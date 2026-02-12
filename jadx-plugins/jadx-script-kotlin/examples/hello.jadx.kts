// logger is preferred for output
log.info { "Hello from jadx script!" }

// println will also work (will be redirected to logger)
println("println from script '$scriptName'")

// get jadx decompiler script instance
val jadx = getJadxInstance()

// adjust options if needed
jadx.args.isDeobfuscationOn = false

// change names
jadx.rename.all { name ->
	when (name) {
		"HelloWorld" -> "HelloJadx"
		else -> null
	}
}

// run some code after loading is finished
jadx.afterLoad {
	log.info { "Loaded classes: ${jadx.classes.size}" }
	// print first class code
	jadx.classes.firstOrNull()?.let { cls ->
		log.info { "Class: '${cls.name}'" }
		log.info { cls.code }
	}
}
