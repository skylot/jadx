package jadx.core.utils;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.ConstClassNode;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

public class InsnUtils {

	private static final Logger LOG = LoggerFactory.getLogger(InsnUtils.class);

	private InsnUtils() {
	}

	public static String formatOffset(int offset) {
		if (offset < 0) {
			return "?";
		}
		return String.format("0x%04x", offset);
	}

	public static String insnTypeToString(InsnType type) {
		return type + "  ";
	}

	public static String indexToString(Object index) {
		if (index == null) {
			return "";
		}
		if (index instanceof String) {
			return "\"" + index + '"';
		}
		return index.toString();
	}

	/**
	 * Search constant assigned to provided arg.
	 *
	 * @return LiteralArg, String, ArgType or null
	 */
	public static Object getConstValueByArg(RootNode root, InsnArg arg) {
		if (arg.isLiteral()) {
			return arg;
		}
		if (arg.isRegister()) {
			RegisterArg reg = (RegisterArg) arg;
			InsnNode parInsn = reg.getAssignInsn();
			if (parInsn == null) {
				return null;
			}
			if (parInsn.getType() == InsnType.MOVE) {
				return getConstValueByArg(root, parInsn.getArg(0));
			}
			return getConstValueByInsn(root, parInsn);
		}
		if (arg.isInsnWrap()) {
			InsnNode insn = ((InsnWrapArg) arg).getWrapInsn();
			return getConstValueByInsn(root, insn);
		}
		return null;
	}

	/**
	 * Return constant value from insn or null if not constant.
	 *
	 * @return LiteralArg, String, ArgType or null
	 */
	@Nullable
	public static Object getConstValueByInsn(RootNode root, InsnNode insn) {
		switch (insn.getType()) {
			case CONST:
				return insn.getArg(0);
			case CONST_STR:
				return ((ConstStringNode) insn).getString();
			case CONST_CLASS:
				return ((ConstClassNode) insn).getClsType();
			case SGET:
				FieldInfo f = (FieldInfo) ((IndexInsnNode) insn).getIndex();
				FieldNode fieldNode = root.resolveField(f);
				if (fieldNode == null) {
					LOG.warn("Field {} not found", f);
					return null;
				}
				EncodedValue constVal = fieldNode.get(JadxAttrType.CONSTANT_VALUE);
				if (constVal != null) {
					return EncodedValueUtils.convertToConstValue(constVal);
				}
				return null;

			default:
				return null;
		}
	}

	@Nullable
	public static InsnNode searchSingleReturnInsn(MethodNode mth, Predicate<InsnNode> test) {
		if (!mth.isNoCode() && mth.getPreExitBlocks().size() == 1) {
			return searchInsn(mth, InsnType.RETURN, test);
		}
		return null;
	}

	/**
	 * Search instruction of specific type and condition in method.
	 * This method support inlined instructions.
	 */
	@Nullable
	public static InsnNode searchInsn(MethodNode mth, InsnType insnType, Predicate<InsnNode> test) {
		if (mth.isNoCode()) {
			return null;
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				InsnNode foundInsn = recursiveInsnCheck(insn, insnType, test);
				if (foundInsn != null) {
					return foundInsn;
				}
			}
		}
		return null;
	}

	public static void replaceInsns(MethodNode mth, Function<InsnNode, InsnNode> replaceFunction) {
		for (BlockNode block : mth.getBasicBlocks()) {
			List<InsnNode> insns = block.getInstructions();
			int insnsCount = insns.size();
			for (int i = 0; i < insnsCount; i++) {
				InsnNode insn = insns.get(i);
				replaceInsnsInInsn(mth, insn, replaceFunction);
				InsnNode replace = replaceFunction.apply(insn);
				if (replace != null) {
					BlockUtils.replaceInsn(mth, block, i, replace);
				}
			}
		}
	}

	public static void replaceInsnsInInsn(MethodNode mth, InsnNode insn, Function<InsnNode, InsnNode> replaceFunction) {
		int argsCount = insn.getArgsCount();
		for (int i = 0; i < argsCount; i++) {
			InsnArg arg = insn.getArg(i);
			if (arg.isInsnWrap()) {
				InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
				replaceInsnsInInsn(mth, wrapInsn, replaceFunction);
				InsnNode replace = replaceFunction.apply(wrapInsn);
				if (replace != null) {
					InsnRemover.unbindArgUsage(mth, arg);
					insn.setArg(i, InsnArg.wrapInsnIntoArg(replace));
				}
			}
		}
	}

	@Nullable
	public static RegisterArg getRegFromInsn(List<RegisterArg> regs, InsnType insnType) {
		for (RegisterArg reg : regs) {
			InsnNode parentInsn = reg.getParentInsn();
			if (parentInsn != null && parentInsn.getType() == insnType) {
				return reg;
			}
		}
		return null;
	}

	private static InsnNode recursiveInsnCheck(InsnNode insn, InsnType insnType, Predicate<InsnNode> test) {
		if (insn.getType() == insnType && test.test(insn)) {
			return insn;
		}
		for (InsnArg arg : insn.getArguments()) {
			if (arg.isInsnWrap()) {
				InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
				InsnNode foundInsn = recursiveInsnCheck(wrapInsn, insnType, test);
				if (foundInsn != null) {
					return foundInsn;
				}
			}
		}
		return null;
	}

	@Nullable
	public static InsnArg getSingleArg(InsnNode insn) {
		if (insn != null && insn.getArgsCount() == 1) {
			return insn.getArg(0);
		}
		return null;
	}

	@Nullable
	public static InsnNode checkInsnType(@Nullable InsnNode insn, InsnType insnType) {
		if (insn != null && insn.getType() == insnType) {
			return insn;
		}
		return null;
	}

	public static boolean isInsnType(@Nullable InsnNode insn, InsnType insnType) {
		return insn != null && insn.getType() == insnType;
	}

	@Nullable
	public static InsnNode getWrappedInsn(InsnArg arg) {
		if (arg != null && arg.isInsnWrap()) {
			return ((InsnWrapArg) arg).getWrapInsn();
		}
		return null;
	}

	public static boolean dontGenerateIfNotUsed(InsnNode insn) {
		RegisterArg resArg = insn.getResult();
		if (resArg != null) {
			SSAVar ssaVar = resArg.getSVar();
			for (RegisterArg arg : ssaVar.getUseList()) {
				InsnNode parentInsn = arg.getParentInsn();
				if (parentInsn != null
						&& !parentInsn.contains(AFlag.DONT_GENERATE)) {
					return false;
				}
			}
		}
		insn.add(AFlag.DONT_GENERATE);
		return true;
	}

	public static <T extends InsnArg> boolean containsVar(List<T> list, RegisterArg arg) {
		if (list == null || list.isEmpty()) {
			return false;
		}
		for (InsnArg insnArg : list) {
			if (insnArg == arg || arg.sameRegAndSVar(insnArg)) {
				return true;
			}
		}
		return false;
	}

	public static boolean contains(InsnNode insn, AFlag flag) {
		return insn != null && insn.contains(flag);
	}
}
