// custom deobfuscator example

val jadx = getJadxInstance()
jadx.args.isDeobfuscationOn = false
jadx.args.renameFlags = emptySet()

val regex = """[Oo0]+""".toRegex()
var n = 0
jadx.rename.all { name, node ->
	when {
		name matches regex -> {
			val newName = "${node.typeName()}${n++}"
			println("renaming ${node.typeName()} '$node' to '$newName'")
			newName
		}
		else -> null
	}
}
jadx.afterLoad {
	println("Renames count: $n")
}
