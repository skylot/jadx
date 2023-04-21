import jadx.api.CommentsLevel

val jadx = getJadxInstance()
jadx.args.commentsLevel = CommentsLevel.NONE
jadx.args.isDeobfuscationOn = false
jadx.args.renameFlags = emptySet()

jadx.rename.all { name ->
	when (name) {
		"HelloWorld" -> "HelloJadx"
		else -> null
	}
}

jadx.afterLoad {
	println("Loaded classes: ${jadx.classes.size}")
	jadx.classes.forEach {
		println("Class '${it.name}':\n${it.code}")
	}
}
