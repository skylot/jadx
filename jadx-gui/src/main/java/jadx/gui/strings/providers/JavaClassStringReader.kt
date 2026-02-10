package jadx.gui.strings.providers

class JavaClassStringReader() : Iterator<String> {

	private var matchesIterator: Iterator<MatchResult>? = null
	private var index: Int = 0

	fun registerSmali(smali: String) {
		// Regex pattern will find the string part of the Smali const-string opcode and will output the string to a capture group
		// Expected pattern: const-string someRegister0, "string"
		val regex = Regex("const-string[ \\w]*,.*?\\\"+(?<string>.*)\\\"")
		matchesIterator = regex.findAll(smali).iterator()
		index = 0
	}

	override fun hasNext() = matchesIterator?.hasNext() ?: false

	override fun next(): String {
		val result = matchesIterator!!.next().groups["string"]!!.value.replace("\\'", "'")
		index++
		return result
	}
}
