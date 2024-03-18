import jadx.api.plugins.options.OptionFlag.PER_PROJECT

/**
 * Custom regexp deobfuscator
 */

val jadx = getJadxInstance()
jadx.args.isDeobfuscationOn = false
jadx.args.renameFlags = emptySet()

val regexOpt = jadx.options.registerString(
	name = "regex",
	desc = "Apply rename for names matches regex",
	defaultValue = "[Oo0]+",
).flags(PER_PROJECT)

val regex = regexOpt.value.toRegex()
var n = 0
jadx.rename.all { name, node ->
	when {
		name matches regex -> {
			val newName = "${node.typeName()}${n++}"
			log.info { "renaming ${node.typeName()} '$node' to '$newName'" }
			newName
		}

		else -> null
	}
}

jadx.afterLoad {
	log.info { "Renames count: $n" }
}
