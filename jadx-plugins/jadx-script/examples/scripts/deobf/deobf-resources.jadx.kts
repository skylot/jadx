import jadx.api.plugins.options.OptionFlag.PER_PROJECT

/**
 * Custom resources regexp deobfuscator
 */

val jadx = getJadxInstance()

val regexOpt = jadx.options.registerString(
	name = "regex",
	desc = "Apply resources rename for file names matches regex",
	defaultValue = """[Oo0]+\.xml""",
).flags(PER_PROJECT)

val regex = regexOpt.value.toRegex()
var n = 0

jadx.stages.prepare {
	for (resFile in jadx.internalDecompiler.resources) {
		val fullName = resFile.originalName
		val name = fullName.substringAfterLast('/')
		if (name matches regex) {
			val path = fullName.substringBeforeLast('/') // TODO: path also may be obfuscated
			val ext = name.substringAfterLast('.')
			val newName = "$path/res-${n++}.$ext"
			log.info { "renaming resource: '$fullName' to '$newName'" }
			resFile.deobfName = newName
		}
	}
}

jadx.afterLoad {
	log.info { "Renames count: $n" }
}
