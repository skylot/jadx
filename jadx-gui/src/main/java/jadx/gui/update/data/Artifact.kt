package jadx.gui.update.data

import java.util.Date

data class Artifact(
	val name: String,
	val sizeInBytes: Long,
	val createdAt: Date,
)
