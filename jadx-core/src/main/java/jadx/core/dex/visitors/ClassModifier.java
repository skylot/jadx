package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.FieldReplaceAttr;
import jadx.core.dex.attributes.nodes.MethodReplaceAttr;
import jadx.core.dex.attributes.nodes.SkipMethodArgsAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.InvokeType;
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
import jadx.core.dex.visitors.usage.UsageInfoVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "ClassModifier",
		desc = "Remove synthetic classes, methods and fields",
		runAfter = {
				ModVisitor.class,
				FixAccessModifiers.class,
				ProcessAnonymous.class
		}
)
public class ClassModifier extends AbstractVisitor {

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		for (ClassNode inner : cls.getInnerClasses()) {
			visit(inner);
		}
		if (isEmptySyntheticClass(cls)) {
			cls.add(AFlag.DONT_GENERATE);
			return false;
		}
		removeSyntheticFields(cls);
		cls.getMethods().forEach(ClassModifier::removeSyntheticMethods);
		cls.getMethods().forEach(ClassModifier::removeEmptyMethods);
		cls.getMethods().forEach(ClassModifier::processAnonymousConstructor);
		return false;
	}

	private static boolean isEmptySyntheticClass(ClassNode cls) {
		return cls.getAccessFlags().isSynthetic()
				&& cls.getFields().isEmpty()
				&& cls.getMethods().isEmpty()
				&& cls.getInnerClasses().isEmpty();
	}

	/**
	 * Remove synthetic fields if type is outer class or class will be inlined (anonymous)
	 */
	private static void removeSyntheticFields(ClassNode cls) {
		boolean inline = cls.isAnonymous();
		if (inline || cls.getClassInfo().isInner()) {
			for (FieldNode field : cls.getFields()) {
				ArgType fldType = field.getType();
				if (field.getAccessFlags().isSynthetic() && fldType.isObject() && !fldType.isGenericType()) {
					ClassInfo clsInfo = ClassInfo.fromType(cls.root(), fldType);
					ClassNode fieldsCls = cls.root().resolveClass(clsInfo);
					ClassInfo parentClass = cls.getClassInfo().getParentClass();
					if (fieldsCls != null
							&& (inline || Objects.equals(parentClass, fieldsCls.getClassInfo()))) {
						int found = 0;
						for (MethodNode mth : cls.getMethods()) {
							if (removeFieldUsageFromConstructor(mth, field, fieldsCls)) {
								found++;
							}
						}
						if (found != 0) {
							field.addAttr(new FieldReplaceAttr(fieldsCls.getClassInfo()));
							field.add(AFlag.DONT_GENERATE);
						}
					}
				}
			}
		}
	}

	private static boolean removeFieldUsageFromConstructor(MethodNode mth, FieldNode field, ClassNode fieldsCls) {
		if (mth.isNoCode() || !mth.getAccessFlags().isConstructor()) {
			return false;
		}
		List<RegisterArg> args = mth.getArgRegs();
		if (args.isEmpty() || mth.contains(AFlag.SKIP_FIRST_ARG)) {
			return false;
		}
		RegisterArg arg = args.get(0);
		if (!arg.getType().equals(fieldsCls.getClassInfo().getType())) {
			return false;
		}
		BlockNode block = mth.getEnterBlock().getCleanSuccessors().get(0);
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
		mth.skipFirstArgument();
		InsnRemover.remove(mth, block, insn);
		// other arg usage -> wrap with IGET insn
		if (arg.getSVar().getUseCount() != 0) {
			InsnNode iget = new IndexInsnNode(InsnType.IGET, fieldInfo, 1);
			iget.addArg(insn.getArg(1));
			for (InsnArg insnArg : new ArrayList<>(arg.getSVar().getUseList())) {
				insnArg.wrapInstruction(mth, iget);
			}
		}
		return true;
	}

	private static void removeSyntheticMethods(MethodNode mth) {
		if (mth.isNoCode() || mth.contains(AFlag.DONT_GENERATE)) {
			return;
		}
		AccessInfo af = mth.getAccessFlags();
		if (!af.isSynthetic()) {
			return;
		}
		ClassNode cls = mth.getParentClass();
		if (removeBridgeMethod(cls, mth)) {
			if (Consts.DEBUG) {
				mth.addDebugComment("Removed as synthetic bridge method");
			} else {
				mth.add(AFlag.DONT_GENERATE);
			}
			return;
		}
		// remove synthetic constructor for inner classes
		if (mth.isConstructor() && mth.contains(AFlag.METHOD_CANDIDATE_FOR_INLINE)) {
			InsnNode insn = BlockUtils.getOnlyOneInsnFromMth(mth);
			if (insn != null) {
				List<RegisterArg> args = mth.getArgRegs();
				if (isRemovedClassInArgs(cls, args)) {
					modifySyntheticMethod(cls, mth, insn, args);
				}
			}
		}
	}

	private static boolean isRemovedClassInArgs(ClassNode cls, List<RegisterArg> mthArgs) {
		for (RegisterArg arg : mthArgs) {
			ArgType argType = arg.getType();
			if (!argType.isObject()) {
				continue;
			}
			ClassNode argCls = cls.root().resolveClass(argType);
			if (argCls == null) {
				// check if missing class from current top class
				ClassInfo argClsInfo = ClassInfo.fromType(cls.root(), argType);
				if (argClsInfo.isInner()
						&& cls.getFullName().startsWith(argClsInfo.getParentClass().getFullName())) {
					return true;
				}
			} else {
				if (argCls.contains(AFlag.DONT_GENERATE) || isEmptySyntheticClass(argCls)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Remove synthetic constructor and redirect calls to existing constructor
	 */
	private static void modifySyntheticMethod(ClassNode cls, MethodNode mth, InsnNode insn, List<RegisterArg> args) {
		if (insn.getType() == InsnType.CONSTRUCTOR) {
			ConstructorInsn constr = (ConstructorInsn) insn;
			if (constr.isThis() && !args.isEmpty()) {
				// remove first arg for non-static class (references to outer class)
				RegisterArg firstArg = args.get(0);
				if (firstArg.getType().equals(cls.getParentClass().getClassInfo().getType())) {
					SkipMethodArgsAttr.skipArg(mth, 0);
				}
				// remove unused args
				int argsCount = args.size();
				for (int i = 0; i < argsCount; i++) {
					RegisterArg arg = args.get(i);
					SSAVar sVar = arg.getSVar();
					if (sVar != null && sVar.getUseCount() == 0) {
						SkipMethodArgsAttr.skipArg(mth, i);
					}
				}
				MethodInfo callMth = constr.getCallMth();
				MethodNode callMthNode = cls.root().resolveMethod(callMth);
				if (callMthNode != null) {
					mth.addAttr(new MethodReplaceAttr(callMthNode));
					mth.add(AFlag.DONT_GENERATE);
					// code generation order should be already fixed for marked methods
					UsageInfoVisitor.replaceMethodUsage(callMthNode, mth);
				}
			}
		}
	}

	private static boolean removeBridgeMethod(ClassNode cls, MethodNode mth) {
		if (cls.root().getArgs().isInlineMethods()) { // simple wrapper remove is same as inline
			List<InsnNode> allInsns = BlockUtils.collectAllInsns(mth.getBasicBlocks());
			if (allInsns.size() == 1) {
				InsnNode wrappedInsn = allInsns.get(0);
				if (wrappedInsn.getType() == InsnType.RETURN) {
					InsnArg arg = wrappedInsn.getArg(0);
					if (arg.isInsnWrap()) {
						wrappedInsn = ((InsnWrapArg) arg).getWrapInsn();
					}
				}
				return checkSyntheticWrapper(mth, wrappedInsn);
			}
		}
		return false;
	}

	private static boolean checkSyntheticWrapper(MethodNode mth, InsnNode insn) {
		InsnType insnType = insn.getType();
		if (insnType != InsnType.INVOKE) {
			return false;
		}
		InvokeNode invokeInsn = (InvokeNode) insn;
		if (invokeInsn.getInvokeType() == InvokeType.SUPER) {
			return false;
		}
		MethodInfo callMth = invokeInsn.getCallMth();
		MethodNode wrappedMth = mth.root().resolveMethod(callMth);
		if (wrappedMth == null) {
			return false;
		}
		AccessInfo wrappedAccFlags = wrappedMth.getAccessFlags();
		if (wrappedAccFlags.isStatic()) {
			return false;
		}
		if (callMth.getArgsCount() != mth.getMethodInfo().getArgsCount()) {
			return false;
		}
		// rename method only from current class
		if (!mth.getParentClass().equals(wrappedMth.getParentClass())) {
			return false;
		}
		// all args must be registers passed from method args (allow only casts insns)
		for (InsnArg arg : insn.getArguments()) {
			if (!registersAndCastsOnly(arg)) {
				return false;
			}
		}
		// remove confirmed, change visibility and name if needed
		if (!wrappedAccFlags.isPublic()) {
			// must be public
			FixAccessModifiers.changeVisibility(wrappedMth, AccessFlags.PUBLIC);
		}
		String alias = mth.getAlias();
		if (!Objects.equals(wrappedMth.getAlias(), alias)) {
			wrappedMth.getMethodInfo().setAlias(alias);
		}
		wrappedMth.addAttr(new MethodReplaceAttr(mth));
		wrappedMth.copyAttributeFrom(mth, AType.METHOD_OVERRIDE);
		wrappedMth.addDebugComment("Method merged with bridge method");
		return true;
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

	/**
	 * Remove public empty constructors (static or default)
	 */
	private static void removeEmptyMethods(MethodNode mth) {
		AccessInfo af = mth.getAccessFlags();
		boolean publicConstructor = af.isConstructor() && af.isPublic();
		boolean clsInit = mth.getMethodInfo().isClassInit() && af.isStatic();
		if ((publicConstructor || clsInit) && mth.getArgRegs().isEmpty()) {
			List<BlockNode> bb = mth.getBasicBlocks();
			if (bb == null || bb.isEmpty() || BlockUtils.isAllBlocksEmpty(bb)) {
				if (clsInit) {
					mth.add(AFlag.DONT_GENERATE);
				} else {
					// don't remove default constructor if other constructors exists
					if (mth.isDefaultConstructor() && !isNonDefaultConstructorExists(mth)) {
						mth.add(AFlag.DONT_GENERATE);
					}
				}
			}
		}
	}

	/**
	 * Remove super call and put into removed fields from anonymous constructor
	 */
	private static void processAnonymousConstructor(MethodNode mth) {
		if (!mth.contains(AFlag.ANONYMOUS_CONSTRUCTOR)) {
			return;
		}
		List<InsnNode> usedInsns = new ArrayList<>();
		Map<InsnArg, FieldNode> argsMap = getArgsToFieldsMapping(mth, usedInsns);
		for (Map.Entry<InsnArg, FieldNode> entry : argsMap.entrySet()) {
			FieldNode field = entry.getValue();
			if (field == null) {
				continue;
			}
			InsnArg arg = entry.getKey();
			field.addAttr(new FieldReplaceAttr(arg));
			field.add(AFlag.DONT_GENERATE);
			if (arg.isRegister()) {
				arg.add(AFlag.SKIP_ARG);
				SkipMethodArgsAttr.skipArg(mth, ((RegisterArg) arg));
			}
		}
		for (InsnNode usedInsn : usedInsns) {
			usedInsn.add(AFlag.DONT_GENERATE);
		}
	}

	private static Map<InsnArg, FieldNode> getArgsToFieldsMapping(MethodNode mth, List<InsnNode> usedInsns) {
		MethodInfo callMth = mth.getMethodInfo();
		ClassNode cls = mth.getParentClass();
		List<RegisterArg> argList = mth.getArgRegs();
		ClassNode outerCls = mth.getUseIn().get(0).getParentClass();
		int startArg = 0;
		if (callMth.getArgsCount() != 0 && callMth.getArgumentsTypes().get(0).equals(outerCls.getClassInfo().getType())) {
			startArg = 1;
		}
		Map<InsnArg, FieldNode> map = new LinkedHashMap<>();
		int argsCount = argList.size();
		for (int i = startArg; i < argsCount; i++) {
			RegisterArg arg = argList.get(i);
			InsnNode useInsn = getParentInsnSkipMove(arg);
			if (useInsn == null) {
				return Collections.emptyMap();
			}
			switch (useInsn.getType()) {
				case IPUT:
					FieldNode fieldNode = cls.searchField((FieldInfo) ((IndexInsnNode) useInsn).getIndex());
					if (fieldNode == null || !fieldNode.getAccessFlags().isSynthetic()) {
						return Collections.emptyMap();
					}
					map.put(arg, fieldNode);
					usedInsns.add(useInsn);
					break;

				case CONSTRUCTOR:
					ConstructorInsn superConstr = (ConstructorInsn) useInsn;
					if (!superConstr.isSuper()) {
						return Collections.emptyMap();
					}
					usedInsns.add(useInsn);
					break;

				default:
					return Collections.emptyMap();
			}
		}
		return map;
	}

	private static InsnNode getParentInsnSkipMove(RegisterArg arg) {
		SSAVar sVar = arg.getSVar();
		if (sVar.getUseCount() != 1) {
			return null;
		}
		RegisterArg useArg = sVar.getUseList().get(0);
		InsnNode parentInsn = useArg.getParentInsn();
		if (parentInsn == null) {
			return null;
		}
		if (parentInsn.getType() == InsnType.MOVE) {
			return getParentInsnSkipMove(parentInsn.getResult());
		}
		return parentInsn;
	}

	private static boolean isNonDefaultConstructorExists(MethodNode defCtor) {
		ClassNode parentClass = defCtor.getParentClass();
		for (MethodNode mth : parentClass.getMethods()) {
			if (mth != defCtor
					&& mth.isConstructor()
					&& !mth.isDefaultConstructor()) {
				return true;
			}
		}
		return false;
	}
}
