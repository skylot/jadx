package jadx.core.utils.kotlin

import jadx.core.dex.instructions.args.RegisterArg
import jadx.core.dex.nodes.FieldNode
import jadx.core.dex.nodes.MethodNode

data class ClsMetadataResult(
	val methodArgs: Map<MethodNode, List<MethodArgRename>>,
	val fields: Map<FieldNode, String>,
)

data class MethodArgRename(
	val rArg: RegisterArg,
	val alias: String,
)
