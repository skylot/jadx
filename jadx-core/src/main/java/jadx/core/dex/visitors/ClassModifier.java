package jadx.core.dex.visitors;

import java.util.List;
import java.util.Objects;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.FieldReplaceAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "ClassModifier",
		desc = "Remove synthetic classes, methods and fields",
		runAfter = ModVisitor.class
)
public class ClassModifier extends AbstractVisitor {

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		for (ClassNode inner : cls.getInnerClasses()) {
			visit(inner);
		}
		if (cls.getAccessFlags().isSynthetic()
				&& cls.getFields().isEmpty()
				&& cls.getMethods().isEmpty()
				&& cls.getInnerClasses().isEmpty()) {
			cls.add(AFlag.DONT_GENERATE);
			return false;
		}
		removeSyntheticFields(cls);
		cls.getMethods().forEach(mth -> removeSyntheticMethods(cls, mth));
		cls.getMethods().forEach(ClassModifier::removeEmptyMethods);

		markAnonymousClass(cls);
		return false;
	}

	private void markAnonymousClass(ClassNode cls) {
		if (cls.isAnonymous()) {
			cls.add(AFlag.ANONYMOUS_CLASS);
		}
	}

	private static void removeSyntheticFields(ClassNode cls) {
		if (!cls.getClassInfo().isInner() || cls.getAccessFlags().isStatic()) {
			return;
		}
		// remove fields if it is synthetic and type is a outer class
		for (FieldNode field : cls.getFields()) {
			if (field.getAccessFlags().isSynthetic() && field.getType().isObject()) {
				ClassInfo clsInfo = ClassInfo.fromType(cls.root(), field.getType());
				ClassNode fieldsCls = cls.dex().resolveClass(clsInfo);
				ClassInfo parentClass = cls.getClassInfo().getParentClass();
				if (fieldsCls != null && parentClass.equals(fieldsCls.getClassInfo())) {
					int found = 0;
					for (MethodNode mth : cls.getMethods()) {
						if (removeFieldUsageFromConstructor(mth, field, fieldsCls)) {
							found++;
						}
					}
					if (found != 0) {
						field.addAttr(new FieldReplaceAttr(parentClass));
						field.add(AFlag.DONT_GENERATE);
					}
				}
			}
		}
	}

	private static boolean removeFieldUsageFromConstructor(MethodNode mth, FieldNode field, ClassNode fieldsCls) {
		if (mth.isNoCode() || !mth.getAccessFlags().isConstructor()) {
			return false;
		}
		List<RegisterArg> args = mth.getArguments(false);
		if (args.isEmpty() || mth.contains(AFlag.SKIP_FIRST_ARG)) {
			return false;
		}
		RegisterArg arg = args.get(0);
		if (!arg.getType().equals(fieldsCls.getClassInfo().getType())) {
			return false;
		}
		BlockNode block = mth.getBasicBlocks().get(0);
		List<InsnNode> instructions = block.getInstructions();
		if (instructions.isEmpty()) {
			return false;
		}
		InsnNode insn = instructions.get(0);
		if (insn.getType() != InsnType.IPUT) {
			return false;
		}
		IndexInsnNode putInsn = (IndexInsnNode) insn;
		FieldInfo fieldInfo = (FieldInfo) putInsn.getIndex();
		if (!fieldInfo.equals(field.getFieldInfo()) || !putInsn.getArg(0).equals(arg)) {
			return false;
		}
		mth.removeFirstArgument();
		InstructionRemover.remove(mth, block, insn);
		// other arg usage -> wrap with IGET insn
		if (arg.getSVar().getUseCount() != 0) {
			InsnNode iget = new IndexInsnNode(InsnType.IGET, fieldInfo, 1);
			iget.addArg(insn.getArg(1));
			for (InsnArg insnArg : arg.getSVar().getUseList()) {
				insnArg.wrapInstruction(iget);
			}
		}
		return true;
	}

	private static void removeSyntheticMethods(ClassNode cls, MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		AccessInfo af = mth.getAccessFlags();
		if (!af.isSynthetic()) {
			return;
		}
		if (removeBridgeMethod(cls, mth)) {
			mth.add(AFlag.DONT_GENERATE);
			return;
		}
		// remove synthetic constructor for inner classes
		if (af.isConstructor() && mth.getBasicBlocks().size() == 2) {
			List<RegisterArg> args = mth.getArguments(false);
			if (isRemovedClassInArgs(cls, args)) {
				modifySyntheticMethod(cls, mth, args);
			}
		}
	}

	private static boolean isRemovedClassInArgs(ClassNode cls, List<RegisterArg> mthArgs) {
		for (RegisterArg arg : mthArgs) {
			ArgType argType = arg.getType();
			if (!argType.isObject()) {
				continue;
			}
			ClassNode argCls = cls.dex().resolveClass(argType);
			if (argCls == null) {
				// check if missing class from current top class
				ClassInfo argClsInfo = ClassInfo.fromType(cls.root(), argType);
				if (argClsInfo.isInner()
						&& cls.getFullName().startsWith(argClsInfo.getParentClass().getFullName())) {
					return true;
				}
			} else {
				if (argCls.contains(AFlag.DONT_GENERATE)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Remove synthetic constructor and redirect calls to existing constructor
	 */
	private static void modifySyntheticMethod(ClassNode cls, MethodNode mth, List<RegisterArg> args) {
		List<InsnNode> insns = mth.getBasicBlocks().get(0).getInstructions();
		if (insns.size() == 1 && insns.get(0).getType() == InsnType.CONSTRUCTOR) {
			ConstructorInsn constr = (ConstructorInsn) insns.get(0);
			if (constr.isThis() && !args.isEmpty()) {
				// remove first arg for non-static class (references to outer class)
				RegisterArg firstArg = args.get(0);
				if (firstArg.getType().equals(cls.getParentClass().getClassInfo().getType())) {
					firstArg.add(AFlag.SKIP_ARG);
				}
				// remove unused args
				for (RegisterArg arg : args) {
					SSAVar sVar = arg.getSVar();
					if (sVar != null && sVar.getUseCount() == 0) {
						arg.add(AFlag.SKIP_ARG);
					}
				}
				mth.add(AFlag.DONT_GENERATE);
			}
		}
	}

	private static boolean removeBridgeMethod(ClassNode cls, MethodNode mth) {
		List<InsnNode> allInsns = BlockUtils.collectAllInsns(mth.getBasicBlocks());
		if (allInsns.size() == 1) {
			InsnNode wrappedInsn = allInsns.get(0);
			if (wrappedInsn.getType() == InsnType.RETURN) {
				InsnArg arg = wrappedInsn.getArg(0);
				if (arg.isInsnWrap()) {
					wrappedInsn = ((InsnWrapArg) arg).getWrapInsn();
				}
			}
			if (checkSyntheticWrapper(mth, wrappedInsn)) {
				return true;
			}
		}
		return !isMethodUnique(cls, mth);
	}

	private static boolean checkSyntheticWrapper(MethodNode mth, InsnNode insn) {
		InsnType insnType = insn.getType();
		if (insnType == InsnType.INVOKE) {
			MethodInfo callMth = ((InvokeNode) insn).getCallMth();
			MethodNode wrappedMth = mth.root().deepResolveMethod(callMth);
			if (wrappedMth != null) {
				if (callMth.getArgsCount() != mth.getMethodInfo().getArgsCount()) {
					return false;
				}
				// all args must be registers passed from method args (allow only casts insns)
				for (InsnArg arg : insn.getArguments()) {
					if (!registersAndCastsOnly(arg)) {
						return false;
					}
				}
				String alias = mth.getAlias();
				if (Objects.equals(wrappedMth.getAlias(), alias)) {
					return true;
				}
				if (!wrappedMth.isVirtual()) {
					return false;
				}
				wrappedMth.getMethodInfo().setAlias(alias);
				return true;
			}
		}
		return false;
	}

	private static boolean registersAndCastsOnly(InsnArg arg) {
		if (arg.isRegister()) {
			return true;
		}
		if (arg.isInsnWrap()) {
			InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
			if (wrapInsn.getType() == InsnType.CHECK_CAST) {
				return registersAndCastsOnly(wrapInsn.getArg(0));
			}
		}
		return false;
	}

	private static boolean isMethodUnique(ClassNode cls, MethodNode mth) {
		MethodInfo mi = mth.getMethodInfo();
		for (MethodNode otherMth : cls.getMethods()) {
			if (otherMth != mth) {
				MethodInfo omi = otherMth.getMethodInfo();
				if (omi.getName().equals(mi.getName())
						&& omi.getArgumentsTypes().size() == mi.getArgumentsTypes().size()) {
					// TODO: check objects types
					return false;
				}
			}
		}
		return true;
	}

	private static void removeEmptyMethods(MethodNode mth) {
		AccessInfo af = mth.getAccessFlags();
		// remove public empty constructors
		if (af.isConstructor()
				&& (af.isPublic() || af.isStatic())
				&& mth.getArguments(false).isEmpty()
				&& !mth.contains(AType.JADX_ERROR)) {
			List<BlockNode> bb = mth.getBasicBlocks();
			if (bb == null || bb.isEmpty() || BlockUtils.isAllBlocksEmpty(bb)) {
				mth.add(AFlag.DONT_GENERATE);
			}
		}
	}
}
