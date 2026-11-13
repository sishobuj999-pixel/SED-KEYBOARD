extends Node3D
## First playable SED Battle Survival prototype. Primitive meshes only.

const BOT_COUNT := 6
const ARENA_SIZE := 46.0
const PLAYER_SPEED := 7.0
const BOT_SPEED := 2.7
const FIRE_RANGE := 65.0
const FIRE_DAMAGE := 34.0

var player: CharacterBody3D
var camera_pivot: Node3D
var camera: Camera3D
var bots: Array[CharacterBody3D] = []
var health := 100.0
var ammo := 30
var reserve_ammo := 90
var kills := 0
var fire_cooldown := 0.0
var hud: Label
var status_label: Label
var joystick_origin := Vector2.ZERO
var joystick_vector := Vector2.ZERO
var touch_look_id := -1
var fire_pressed := false
var look_sensitivity := 0.004

func _ready() -> void:
	_setup_input()
	_build_world()
	_build_player()
	_spawn_bots()
	_build_hud()
	Input.mouse_mode = Input.MOUSE_MODE_CAPTURED

func _setup_input() -> void:
	var keys := {"move_forward":[KEY_W,KEY_UP],"move_back":[KEY_S,KEY_DOWN],"move_left":[KEY_A,KEY_LEFT],"move_right":[KEY_D,KEY_RIGHT],"jump":[KEY_SPACE],"reload":[KEY_R]}
	for action in keys:
		if not InputMap.has_action(action):
			InputMap.add_action(action)
		for keycode in keys[action]:
			var event := InputEventKey.new()
			event.physical_keycode = keycode
			InputMap.action_add_event(action,event)

func _build_world() -> void:
	var world_env := WorldEnvironment.new()
	var env := Environment.new()
	env.background_mode = Environment.BG_COLOR
	env.background_color = Color(0.42,0.62,0.72)
	env.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	env.ambient_light_color = Color(0.72,0.78,0.82)
	env.ambient_light_energy = 0.8
	world_env.environment = env
	add_child(world_env)
	var sun := DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-52,-28,0)
	sun.light_energy = 1.15
	add_child(sun)
	_add_box(self,Vector3(0,-0.55,0),Vector3(ARENA_SIZE,1,ARENA_SIZE),Color(0.25,0.39,0.24),true)
	for i in range(4):
		var wall := _add_box(self,Vector3(0,1.4,0),Vector3(ARENA_SIZE,2.8,0.7),Color(0.32,0.35,0.34),true)
		wall.rotation_degrees.y = i*90
	for p in [Vector3(-10,1,-8),Vector3(8,1,-12),Vector3(13,1,5),Vector3(-13,1,11),Vector3(0,1,14),Vector3(2,1,-2)]:
		_add_box(self,p,Vector3(3,2,3),Color(0.42,0.39,0.31),true)
	for p in [Vector3(-17,1,-15),Vector3(17,1,-17),Vector3(-18,1,16),Vector3(17,1,17)]:
		_add_box(self,p,Vector3(2,2,2),Color(0.19,0.29,0.18),true)

func _add_box(parent: Node3D,pos: Vector3,size: Vector3,color: Color,collision: bool) -> MeshInstance3D:
	var holder: Node3D = parent
	var body: StaticBody3D
	if collision:
		body = StaticBody3D.new()
		parent.add_child(body)
		body.position = pos
		holder = body
	var mesh := MeshInstance3D.new()
	var box := BoxMesh.new()
	box.size = size
	mesh.mesh = box
	var mat := StandardMaterial3D.new()
	mat.albedo_color = color
	mat.roughness = 0.95
	mesh.material_override = mat
	holder.add_child(mesh)
	if collision:
		var shape := CollisionShape3D.new()
		var box_shape := BoxShape3D.new()
		box_shape.size = size
		shape.shape = box_shape
		body.add_child(shape)
	else:
		mesh.position = pos
	return mesh

func _build_player() -> void:
	player = CharacterBody3D.new()
	player.name = "Player"
	player.position = Vector3(0,1,8)
	add_child(player)
	var capsule := CapsuleMesh.new()
	capsule.radius = 0.42
	capsule.height = 1.8
	var body_mesh := MeshInstance3D.new()
	body_mesh.mesh = capsule
	var mat := StandardMaterial3D.new()
	mat.albedo_color = Color(0.12,0.35,0.82)
	body_mesh.material_override = mat
	player.add_child(body_mesh)
	var shape := CollisionShape3D.new()
	var capsule_shape := CapsuleShape3D.new()
	capsule_shape.radius = 0.42
	capsule_shape.height = 1.8
	shape.shape = capsule_shape
	player.add_child(shape)
	camera_pivot = Node3D.new()
	camera_pivot.position = Vector3(0,1.25,0)
	player.add_child(camera_pivot)
	camera = Camera3D.new()
	camera.position = Vector3(0,3.2,6.5)
	camera.rotation_degrees.x = -18
	camera.current = true
	camera_pivot.add_child(camera)

func _spawn_bots() -> void:
	var positions := [Vector3(-14,1,-12),Vector3(12,1,-15),Vector3(16,1,12),Vector3(-15,1,14),Vector3(2,1,-18),Vector3(0,1,0)]
	for i in range(BOT_COUNT):
		var bot := CharacterBody3D.new()
		bot.name = "Enemy_%d" % i
		bot.position = positions[i]
		add_child(bot)
		var mesh := MeshInstance3D.new()
		var capsule := CapsuleMesh.new()
		capsule.radius = 0.43
		capsule.height = 1.8
		mesh.mesh = capsule
		var mat := StandardMaterial3D.new()
		mat.albedo_color = Color(0.78,0.16,0.12)
		mesh.material_override = mat
		bot.add_child(mesh)
		var shape := CollisionShape3D.new()
		var cs := CapsuleShape3D.new()
		cs.radius = 0.43
		cs.height = 1.8
		shape.shape = cs
		bot.add_child(shape)
		bot.set_meta("health",100.0)
		bot.set_meta("attack_timer",randf_range(1.0,2.8))
		bots.append(bot)

func _build_hud() -> void:
	var layer := CanvasLayer.new()
	add_child(layer)
	var panel := ColorRect.new()
	panel.color = Color(0.02,0.04,0.05,0.72)
	panel.position = Vector2(18,18)
	panel.size = Vector2(310,92)
	layer.add_child(panel)
	hud = Label.new()
	hud.position = Vector2(32,27)
	hud.add_theme_font_size_override("font_size",22)
	layer.add_child(hud)
	status_label = Label.new()
	status_label.position = Vector2(18,118)
	status_label.add_theme_font_size_override("font_size",18)
	layer.add_child(status_label)
	var crosshair := Label.new()
	crosshair.text = "+"
	crosshair.set_anchors_and_offsets_preset(Control.PRESET_CENTER)
	crosshair.position = Vector2(-10,-18)
	crosshair.add_theme_font_size_override("font_size",34)
	layer.add_child(crosshair)
	var fire := Button.new()
	fire.text = "FIRE"
	fire.size = Vector2(112,76)
	fire.anchor_left = 1
	fire.anchor_top = 1
	fire.anchor_right = 1
	fire.anchor_bottom = 1
	fire.offset_left = -142
	fire.offset_top = -112
	fire.offset_right = -24
	fire.offset_bottom = -28
	fire.button_down.connect(func(): fire_pressed = true)
	fire.button_up.connect(func(): fire_pressed = false)
	layer.add_child(fire)
	var reload_btn := Button.new()
	reload_btn.text = "RELOAD"
	reload_btn.anchor_left = 1
	reload_btn.anchor_top = 1
	reload_btn.anchor_right = 1
	reload_btn.anchor_bottom = 1
	reload_btn.offset_left = -142
	reload_btn.offset_top = -194
	reload_btn.offset_right = -24
	reload_btn.offset_bottom = -130
	reload_btn.pressed.connect(_reload)
	layer.add_child(reload_btn)
	var help := Label.new()
	help.text = "WASD / touch: move   |   drag right side: look   |   FIRE: shoot"
	help.position = Vector2(18,0)
	help.anchor_top = 1
	help.anchor_bottom = 1
	help.offset_top = -32
	help.offset_bottom = -8
	help.add_theme_font_size_override("font_size",14)
	layer.add_child(help)

func _unhandled_input(event: InputEvent) -> void:
	if event is InputEventMouseMotion and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED:
		camera_pivot.rotate_y(-event.relative.x*look_sensitivity)
		camera.rotation.x = clampf(camera.rotation.x-event.relative.y*look_sensitivity,-0.9,0.25)
	elif event is InputEventMouseButton and event.pressed:
		if event.button_index == MOUSE_BUTTON_LEFT:
			_shoot()
		elif event.button_index == MOUSE_BUTTON_RIGHT:
			Input.mouse_mode = Input.MOUSE_MODE_CAPTURED
	elif event is InputEventScreenTouch:
		if event.pressed:
			if event.position.x > get_viewport().size.x*0.55:
				touch_look_id = event.index
			else:
				joystick_origin = event.position
				joystick_vector = Vector2.ZERO
		else:
			if event.index == touch_look_id:
				touch_look_id = -1
			joystick_vector = Vector2.ZERO
	elif event is InputEventScreenDrag:
		if event.index == touch_look_id:
			camera_pivot.rotate_y(-event.relative.x*0.006)
			camera.rotation.x = clampf(camera.rotation.x-event.relative.y*0.004,-0.9,0.25)
		elif event.position.x < get_viewport().size.x*0.55:
			joystick_vector = (event.position-joystick_origin).limit_length(75.0)/75.0

func _physics_process(delta: float) -> void:
	if health <= 0:
		status_label.text = "YOU WERE ELIMINATED"
		return
	fire_cooldown = maxf(0.0,fire_cooldown-delta)
	var input_vec := Input.get_vector("move_left","move_right","move_forward","move_back")
	if joystick_vector.length() > 0.08:
		input_vec = joystick_vector
	var direction := (camera_pivot.global_basis*Vector3(input_vec.x,0,input_vec.y)).normalized()
	direction.y = 0
	if direction.length() > 0.05:
		player.velocity.x = direction.x*PLAYER_SPEED
		player.velocity.z = direction.z*PLAYER_SPEED
		player.rotation.y = lerp_angle(player.rotation.y,atan2(-direction.x,-direction.z),delta*9.0)
	else:
		player.velocity.x = move_toward(player.velocity.x,0,PLAYER_SPEED*5*delta)
		player.velocity.z = move_toward(player.velocity.z,0,PLAYER_SPEED*5*delta)
	player.velocity.y -= 22.0*delta
	if Input.is_action_just_pressed("jump") and player.is_on_floor():
		player.velocity.y = 7.0
	player.move_and_slide()
	for bot in bots.duplicate():
		if not is_instance_valid(bot):
			bots.erase(bot)
			continue
		_bot_tick(bot,delta)
	if fire_pressed and fire_cooldown <= 0:
		_shoot()
	_update_hud()

func _bot_tick(bot: CharacterBody3D,delta: float) -> void:
	var to_player := player.global_position-bot.global_position
	var distance := to_player.length()
	if distance > 2.2:
		var dir := to_player.normalized()
		bot.velocity.x = dir.x*BOT_SPEED
		bot.velocity.z = dir.z*BOT_SPEED
		bot.rotation.y = atan2(-dir.x,-dir.z)
	else:
		bot.velocity.x = 0
		bot.velocity.z = 0
	bot.velocity.y -= 22.0*delta
	bot.move_and_slide()
	var timer: float = bot.get_meta("attack_timer")
	timer -= delta
	if timer <= 0 and distance < 22:
		health = maxf(0.0,health-5.0)
		timer = 1.7
	bot.set_meta("attack_timer",timer)

func _shoot() -> void:
	if fire_cooldown > 0:
		return
	if ammo <= 0:
		status_label.text = "Magazine empty — press RELOAD"
		fire_cooldown = 0.2
		return
	ammo -= 1
	fire_cooldown = 0.16
	var from := camera.global_position
	var to := from-camera.global_basis.z*FIRE_RANGE
	var query := PhysicsRayQueryParameters3D.create(from,to)
	query.exclude = [player]
	var hit := get_world_3d().direct_space_state.intersect_ray(query)
	if hit and hit.collider is CharacterBody3D and hit.collider != player:
		var target := hit.collider as CharacterBody3D
		if target.has_meta("health"):
			var hp: float = target.get_meta("health")-FIRE_DAMAGE
			target.set_meta("health",hp)
			if hp <= 0:
				bots.erase(target)
				target.queue_free()
				kills += 1
				if bots.is_empty():
					status_label.text = "ARENA CLEARED!"
	_update_hud()

func _reload() -> void:
	if reserve_ammo <= 0 or ammo == 30:
		return
	var loaded := mini(30-ammo,reserve_ammo)
	ammo += loaded
	reserve_ammo -= loaded
	_update_hud()

func _update_hud() -> void:
	hud.text = "HP: %d   AMMO: %d/%d\nENEMIES: %d   KILLS: %d" % [int(health),ammo,reserve_ammo,bots.size(),kills]
	if health > 0 and not bots.is_empty():
		status_label.text = "Eliminate all enemies and survive!"
