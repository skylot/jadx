package jadx.plugins.kotlin.metadata.model

import jadx.core.dex.instructions.args.RegisterArg
import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.FieldNode
import jadx.core.dex.nodes.MethodNode

data class ClassAliasRename(
	val pkg: String,
	val name: String,
)

data class MethodArgRename(
	val rArg: RegisterArg,
	val alias: String,
)

data class FieldRename(
	val field: FieldNode,
	val alias: String,
)

data class CompanionRename(
	val field: FieldNode,
	val cls: ClassNode,
	val hide: Boolean,
)

data class ToStringRename(
	val cls: ClassNode,
	val clsAlias: String?,
	val fields: List<FieldRename>,
)

data class MethodRename(
	val mth: MethodNode,
	val alias: String,
)
