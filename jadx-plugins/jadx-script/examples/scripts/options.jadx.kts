val jadx = getJadxInstance()

val testOpt = jadx.options.registerString(
	"test",
	"Simple string option",
	values = listOf("first", "second"),
	defaultValue = "first"
)

val numOpt = jadx.options.registerInt("number", "Number option").validate { it >= 0 }

val boolOpt = jadx.options.registerYesNo("bool", "Boolean option")

val allOptions = listOf(testOpt, numOpt, boolOpt)

jadx.afterLoad {
	printOptions()
}

jadx.gui.ifAvailable {
	addMenuAction("Print options") {
		printOptions()
	}
}

fun printOptions() {
	allOptions.forEach { opt ->
		println("Option: '${opt.name}', id: '${opt.id}', value: '${opt.value}'")
	}
}
