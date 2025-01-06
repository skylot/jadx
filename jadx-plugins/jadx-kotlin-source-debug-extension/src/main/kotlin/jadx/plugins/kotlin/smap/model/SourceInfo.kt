package jadx.plugins.kotlin.smap.model

data class SourceInfo(
	val sourceFileName: String?,
	val pathOrCleanFQN: String,
	val linesInFile: Int,
)
