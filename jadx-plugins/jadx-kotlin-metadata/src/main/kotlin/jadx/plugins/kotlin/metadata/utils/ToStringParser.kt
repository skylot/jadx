package jadx.plugins.kotlin.metadata.utils

import jadx.core.Consts
import jadx.core.dex.info.FieldInfo
import jadx.core.dex.instructions.ConstStringNode
import jadx.core.dex.instructions.IndexInsnNode
import jadx.core.dex.instructions.InsnType
import jadx.core.dex.instructions.InvokeNode
import jadx.core.dex.instructions.InvokeType
import jadx.core.dex.instructions.args.InsnWrapArg
import jadx.core.dex.instructions.args.RegisterArg
import jadx.core.dex.instructions.mods.ConstructorInsn
import jadx.core.dex.nodes.BlockNode
import jadx.core.dex.nodes.InsnNode
import jadx.core.dex.nodes.MethodNode
import jadx.core.utils.BlockUtils
import jadx.plugins.kotlin.metadata.model.FieldRename
import jadx.plugins.kotlin.metadata.model.ToStringRename

class ToStringParser private constructor(mthToString: MethodNode) {
	private var isStarted = false
	private var isFirstProcessed = false
	private var isFinished = false
	private var pendingAlias: String? = null
	private var clsAlias: String? = null
	private val list: MutableList<Pair<String, FieldInfo>> = mutableListOf()
	val isSuccess: Boolean get() = isStarted && isFinished

	init {
		val blocks: List<BlockNode> = BlockUtils.buildSimplePath(mthToString.enterBlock)
		blocks.forEach { block ->
			block.instructions.forEach { insn ->
				process(insn)
			}
		}
	}

	private fun process(insn: InsnNode) {
		if (!isStarted) {
			isStarted = isStartStringBuilder(insn)
			return
		}
		if (isFinished) {
			return
		}

		if (isAppendInvoke(insn)) {
			val arg = insn.getArg(1)

			// invoke with const string
			if (arg.isInsnWrap && arg is InsnWrapArg && arg.wrapInsn.type == InsnType.CONST_STR) {
				val constStr: String? = (arg.wrapInsn as ConstStringNode).string
				handleString(requireNotNull(constStr) { "Failed to get const String" })
			}

			// invoke with register
			if (arg.isRegister && arg is RegisterArg) {
				val assign = arg.sVar.assignInsn
				// basic argument
				if (assign is IndexInsnNode) {
					val info: FieldInfo? = (arg.sVar.assignInsn as IndexInsnNode).index as? FieldInfo
					handleFieldInfo(requireNotNull(info) { "Failed to get FieldInfo from index" })
				}

				// string formatted argument, for rare cases like Arrays.toString(...)
				if (assign is InvokeNode && assign.invokeType == InvokeType.STATIC && assign.argsCount == 1) {
					val prevArg = assign.getArg(0)
					if (prevArg.isRegister && prevArg is RegisterArg) {
						if (prevArg.sVar.assignInsn is IndexInsnNode) {
							val info: FieldInfo? = (prevArg.sVar.assignInsn as IndexInsnNode).index as? FieldInfo
							handleFieldInfo(requireNotNull(info) { "Failed to get nested FieldInfo from index" })
						}
					}
				}
			}

			return
		}

		isFinished = isToString(insn)
	}

	private fun handleString(string: String) {
		if (pendingAlias != null) {
			LOG.warn("Skipping pending alias: '$pendingAlias'")
		}
		if (!isFirstProcessed) {
			clsAlias = string.substringBefore('(')
			pendingAlias = string
				.substringAfter('(')
				.substringBeforeLast('=')
			isFirstProcessed = true
		} else {
			pendingAlias = string
				.substringAfter(", ")
				.substringBeforeLast('=')
		}
	}

	private fun handleFieldInfo(fieldInfo: FieldInfo) {
		list.add(requireNotNull(pendingAlias) { "No pending alias found" } to fieldInfo)
		pendingAlias = null
	}

	companion object {

		fun parse(mth: MethodNode): ToStringRename? {
			val parser =
				kotlin.runCatching { ToStringParser(mth) }.getOrNull()
			if (parser?.isSuccess != true) return null

			val cls = mth.parentClass
			return ToStringRename(
				cls = cls,
				clsAlias = parser.clsAlias,
				fields = parser.list.mapNotNull { (alias, fieldInfo) ->
					val field = cls.searchField(fieldInfo)
						?: return@mapNotNull null
					FieldRename(
						field = field,
						alias = alias,
					)
				},
			)
		}

		private fun isStartStringBuilder(inst: InsnNode): Boolean {
			return inst is ConstructorInsn &&
				inst.isNewInstance &&
				inst.callMth.declClass.fullName == Consts.CLASS_STRING_BUILDER
		}

		private fun isAppendInvoke(inst: InsnNode): Boolean {
			return inst is InvokeNode &&
				inst.callMth.declClass.fullName == Consts.CLASS_STRING_BUILDER &&
				inst.callMth.name == "append" &&
				inst.argsCount == 2
		}

		private fun isToString(inst: InsnNode): Boolean {
			return inst is InvokeNode &&
				inst.callMth.declClass.fullName == Consts.CLASS_STRING_BUILDER &&
				inst.callMth.shortId == Consts.MTH_TOSTRING_SIGNATURE
		}
	}
}
