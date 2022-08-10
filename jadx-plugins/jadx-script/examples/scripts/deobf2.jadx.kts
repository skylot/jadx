// animal deobfuscator ^_^
@file:DependsOn("com.github.javafaker:javafaker:1.0.2")

import com.github.javafaker.Faker
import jadx.core.deobf.NameMapper
import java.util.Random

val jadx = getJadxInstance()
jadx.args.isDeobfuscationOn = false
jadx.args.renameFlags = emptySet()

val regex = """[Oo0]+""".toRegex()
val usedNames = mutableSetOf<String>()
val faker = Faker(Random(1))
var dups = 1

jadx.rename.all { name, node ->
	when {
		name matches regex -> {
			val prefix = node.typeName().first()
			val alias = faker.name().firstName().cap() + faker.animal().name().cap()
			makeUnique(prefix, alias)
		}
		else -> null
	}
}

fun makeUnique(prefix: Char, name: String): String {
	while (true) {
		val resName = prefix + NameMapper.removeInvalidCharsMiddle(name)
		return if (usedNames.add(resName)) resName else "$resName${dups++}"
	}
}

jadx.afterLoad {
	println("Renames count: ${usedNames.size + dups}, names: ${usedNames.size}, dups: $dups")
}

fun String.cap() = this.replaceFirstChar(Char::uppercaseChar)
