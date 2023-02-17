package jadx.core.dex.visitors;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.annotations.AnnotationVisibility;
import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.AnnotationsAttr;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.AttrNode;
import jadx.core.dex.attributes.nodes.SkipMethodArgsAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ConstClassNode;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.FillArrayInsn;
import jadx.core.dex.instructions.FilledNewArrayNode;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IfOp;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.NewArrayNode;
import jadx.core.dex.instructions.SwitchInsn;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.visitors.regions.variables.ProcessVariables;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.dex.visitors.typeinference.TypeCompareEnum;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.utils.BlockUtils.replaceInsn;

/**
 * Visitor for modify method instructions
 * (remove, replace, process exception handlers)
 */
@JadxVisitor(
		name = "ModVisitor",
		desc = "Modify method instructions",
		runBefore = {
				CodeShrinkVisitor.class,
				ProcessVariables.class
		}
)
public class ModVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(ModVisitor.class);

	private static final long DOUBLE_TO_BITS = Double.doubleToLongBits(1);
	private static final long FLOAT_TO_BITS = Float.floatToIntBits(1);

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		replaceConstInAnnotations(cls);
		return true;
	}

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		InsnRemover remover = new InsnRemover(mth);
		replaceStep(mth, remover);
		removeStep(mth, remover);
		iterativeRemoveStep(mth);
	}

	private static void replaceStep(MethodNode mth, InsnRemover remover) {
		ClassNode parentClass = mth.getParentClass();
		for (BlockNode block : mth.getBasicBlocks()) {
			remover.setBlock(block);
			List<InsnNode> insnsList = block.getInstructions();
			int size = insnsList.size();
			for (int i = 0; i < size; i++) {
				InsnNode insn = insnsList.get(i);
				switch (insn.getType()) {
					case CONSTRUCTOR:
						processAnonymousConstructor(mth, ((ConstructorInsn) insn));
						break;

					case CONST:
					case CONST_STR:
					case CONST_CLASS:
						replaceConst(mth, parentClass, block, i, insn);
						break;

					case SWITCH:
						replaceConstKeys(mth, parentClass, (SwitchInsn) insn);
						break;

					case NEW_ARRAY:
						// replace with filled array if 'fill-array' is next instruction
						NewArrayNode newArrInsn = (NewArrayNode) insn;
						InsnNode nextInsn = getFirstUseSkipMove(insn.getResult());
						if (nextInsn != null && nextInsn.getType() == InsnType.FILL_ARRAY) {
							FillArrayInsn fillArrInsn = (FillArrayInsn) nextInsn;
							if (checkArrSizes(mth, newArrInsn, fillArrInsn)) {
								InsnNode filledArr = makeFilledArrayInsn(mth, newArrInsn, fillArrInsn);
								replaceInsn(mth, block, i, filledArr);
								remover.addAndUnbind(nextInsn);
							}
						}
						break;

					case MOVE_EXCEPTION:
						processMoveException(mth, block, insn, remover);
						break;

					case ARITH:
						processArith(mth, parentClass, (ArithNode) insn);
						break;

					case CHECK_CAST:
						removeCheckCast(mth, block, i, (IndexInsnNode) insn);
						break;

					case CAST:
						fixPrimitiveCast(mth, block, i, insn);
						break;

					case IPUT:
					case IGET:
						fixFieldUsage(mth, (IndexInsnNode) insn);
						break;

					default:
						break;
				}
			}
			remover.perform();
		}
	}

	/**
	 * If field is not visible from use site => cast to origin class
	 */
	private static void fixFieldUsage(MethodNode mth, IndexInsnNode insn) {
		InsnArg instanceArg = insn.getArg(insn.getType() == InsnType.IGET ? 0 : 1);
		if (instanceArg.contains(AFlag.SUPER)) {
			return;
		}
		if (instanceArg.isInsnWrap() && ((InsnWrapArg) instanceArg).getWrapInsn().getType() == InsnType.CAST) {
			return;
		}
		FieldInfo fieldInfo = (FieldInfo) insn.getIndex();
		ArgType clsType = fieldInfo.getDeclClass().getType();
		ArgType instanceType = instanceArg.getType();
		if (Objects.equals(clsType, instanceType)) {
			// cast not needed
			return;
		}

		FieldNode fieldNode = mth.root().resolveField(fieldInfo);
		if (fieldNode == null) {
			// unknown field
			TypeCompareEnum result = mth.root().getTypeCompare().compareTypes(instanceType, clsType);
			if (result.isEqual() || (result == TypeCompareEnum.NARROW_BY_GENERIC && !instanceType.isGenericType())) {
				return;
			}
		} else if (isFieldVisibleInMethod(fieldNode, mth)) {
			return;
		}
		// insert cast
		IndexInsnNode castInsn = new IndexInsnNode(InsnType.CAST, clsType, 1);
		castInsn.addArg(instanceArg.duplicate());
		castInsn.add(AFlag.SYNTHETIC);
		castInsn.add(AFlag.EXPLICIT_CAST);

		InsnArg castArg = InsnArg.wrapInsnIntoArg(castInsn);
		castArg.setType(clsType);
		insn.replaceArg(instanceArg, castArg);
		InsnRemover.unbindArgUsage(mth, instanceArg);
	}

	private static boolean isFieldVisibleInMethod(FieldNode field, MethodNode mth) {
		AccessInfo accessFlags = field.getAccessFlags();
		if (accessFlags.isPublic()) {
			return true;
		}
		ClassNode useCls = mth.getParentClass();
		ClassNode fieldCls = field.getParentClass();
		boolean sameScope = Objects.equals(useCls, fieldCls) && !mth.getAccessFlags().isStatic();
		if (sameScope) {
			return true;
		}
		if (accessFlags.isPrivate()) {
			return false;
		}
		// package-private or protected
		if (Objects.equals(useCls.getClassInfo().getPackage(), fieldCls.getClassInfo().getPackage())) {
			// same package
			return true;
		}
		if (accessFlags.isPackagePrivate()) {
			return false;
		}
		// protected
		TypeCompareEnum result = mth.root().getTypeCompare().compareTypes(useCls, fieldCls);
		return result == TypeCompareEnum.NARROW; // true if use class is subclass of field class
	}

	private static void replaceConstKeys(MethodNode mth, ClassNode parentClass, SwitchInsn insn) {
		int[] keys = insn.getKeys();
		int len = keys.length;
		for (int k = 0; k < len; k++) {
			FieldNode f = parentClass.getConstField(keys[k]);
			if (f != null) {
				insn.modifyKey(k, f);
				f.addUseIn(mth);
			}
		}
	}

	private static void fixPrimitiveCast(MethodNode mth, BlockNode block, int i, InsnNode insn) {
		// replace boolean to (byte/char/short/long/double/float) cast with ternary
		InsnArg castArg = insn.getArg(0);
		if (castArg.getType() == ArgType.BOOLEAN) {
			ArgType type = insn.getResult().getType();
			if (type.isPrimitive()) {
				TernaryInsn ternary = makeBooleanConvertInsn(insn.getResult(), castArg, type);
				replaceInsn(mth, block, i, ternary);
			}
		}
	}

	public static TernaryInsn makeBooleanConvertInsn(RegisterArg result, InsnArg castArg, ArgType type) {
		InsnArg zero = LiteralArg.make(0, type);
		long litVal = 1;
		if (type == ArgType.DOUBLE) {
			litVal = DOUBLE_TO_BITS;
		} else if (type == ArgType.FLOAT) {
			litVal = FLOAT_TO_BITS;
		}
		InsnArg one = LiteralArg.make(litVal, type);

		IfNode ifNode = new IfNode(IfOp.EQ, -1, castArg, LiteralArg.litTrue());
		IfCondition condition = IfCondition.fromIfNode(ifNode);
		return new TernaryInsn(condition, result, one, zero);
	}

	private void replaceConstInAnnotations(ClassNode cls) {
		if (cls.root().getArgs().isReplaceConsts()) {
			replaceConstsInAnnotationForAttrNode(cls, cls);
			cls.getFields().forEach(f -> replaceConstsInAnnotationForAttrNode(cls, f));
			cls.getMethods().forEach(m -> replaceConstsInAnnotationForAttrNode(cls, m));
		}
	}

	private void replaceConstsInAnnotationForAttrNode(ClassNode parentCls, AttrNode attrNode) {
		AnnotationsAttr annotationsList = attrNode.get(JadxAttrType.ANNOTATION_LIST);
		if (annotationsList == null) {
			return;
		}
		for (IAnnotation annotation : annotationsList.getAll()) {
			if (annotation.getVisibility() == AnnotationVisibility.SYSTEM) {
				continue;
			}
			for (Map.Entry<String, EncodedValue> entry : annotation.getValues().entrySet()) {
				entry.setValue(replaceConstValue(parentCls, entry.getValue()));
			}
		}
	}

	@SuppressWarnings("unchecked")
	private EncodedValue replaceConstValue(ClassNode parentCls, EncodedValue encodedValue) {
		if (encodedValue.getType() == EncodedType.ENCODED_ANNOTATION) {
			IAnnotation annotation = (IAnnotation) encodedValue.getValue();
			for (Map.Entry<String, EncodedValue> entry : annotation.getValues().entrySet()) {
				entry.setValue(replaceConstValue(parentCls, entry.getValue()));
			}
			return encodedValue;
		}
		if (encodedValue.getType() == EncodedType.ENCODED_ARRAY) {
			List<EncodedValue> listVal = (List<EncodedValue>) encodedValue.getValue();
			if (!listVal.isEmpty()) {
				listVal.replaceAll(v -> replaceConstValue(parentCls, v));
			}
			return new EncodedValue(EncodedType.ENCODED_ARRAY, listVal);
		}
		FieldNode constField = parentCls.getConstField(encodedValue.getValue());
		if (constField != null) {
			return new EncodedValue(EncodedType.ENCODED_FIELD, constField.getFieldInfo());
		}
		return encodedValue;
	}

	private static void replaceConst(MethodNode mth, ClassNode parentClass, BlockNode block, int i, InsnNode insn) {
		FieldNode f;
		if (insn.getType() == InsnType.CONST_STR) {
			String s = ((ConstStringNode) insn).getString();
			f = parentClass.getConstField(s);
		} else if (insn.getType() == InsnType.CONST_CLASS) {
			ArgType t = ((ConstClassNode) insn).getClsType();
			f = parentClass.getConstField(t);
		} else {
			f = parentClass.getConstFieldByLiteralArg((LiteralArg) insn.getArg(0));
		}
		if (f != null) {
			InsnNode inode = new IndexInsnNode(InsnType.SGET, f.getFieldInfo(), 0);
			inode.setResult(insn.getResult());
			replaceInsn(mth, block, i, inode);
			f.addUseIn(mth);
		}
	}

	private static void processArith(MethodNode mth, ClassNode parentClass, ArithNode arithNode) {
		if (arithNode.getArgsCount() != 2) {
			throw new JadxRuntimeException("Invalid args count in insn: " + arithNode);
		}
		InsnArg litArg = arithNode.getArg(1);
		if (litArg.isLiteral()) {
			FieldNode f = parentClass.getConstFieldByLiteralArg((LiteralArg) litArg);
			if (f != null) {
				InsnNode fGet = new IndexInsnNode(InsnType.SGET, f.getFieldInfo(), 0);
				if (arithNode.replaceArg(litArg, InsnArg.wrapArg(fGet))) {
					f.addUseIn(mth);
				}
			}
		}
	}

	private static boolean checkArrSizes(MethodNode mth, NewArrayNode newArrInsn, FillArrayInsn fillArrInsn) {
		int dataSize = fillArrInsn.getSize();
		InsnArg arrSizeArg = newArrInsn.getArg(0);
		Object value = InsnUtils.getConstValueByArg(mth.root(), arrSizeArg);
		if (value instanceof LiteralArg) {
			long literal = ((LiteralArg) value).getLiteral();
			return dataSize == (int) literal;
		}
		return false;
	}

	private static void removeCheckCast(MethodNode mth, BlockNode block, int i, IndexInsnNode insn) {
		InsnArg castArg = insn.getArg(0);
		ArgType castType = (ArgType) insn.getIndex();
		if (!ArgType.isCastNeeded(mth.root(), castArg.getType(), castType)) {
			RegisterArg result = insn.getResult();
			result.setType(castArg.getType());

			InsnNode move = new InsnNode(InsnType.MOVE, 1);
			move.setResult(result);
			move.addArg(castArg);
			replaceInsn(mth, block, i, move);
			return;
		}
		InsnNode prevCast = isCastDuplicate(insn);
		if (prevCast != null) {
			// replace previous cast with move
			InsnNode move = new InsnNode(InsnType.MOVE, 1);
			move.setResult(prevCast.getResult());
			move.addArg(prevCast.getArg(0));
			replaceInsn(mth, block, prevCast, move);
		}
	}

	private static @Nullable InsnNode isCastDuplicate(IndexInsnNode castInsn) {
		InsnArg arg = castInsn.getArg(0);
		if (arg.isRegister()) {
			SSAVar sVar = ((RegisterArg) arg).getSVar();
			if (sVar != null && sVar.getUseCount() == 1 && !sVar.isUsedInPhi()) {
				InsnNode assignInsn = sVar.getAssign().getParentInsn();
				if (assignInsn != null && assignInsn.getType() == InsnType.CHECK_CAST) {
					ArgType assignCastType = (ArgType) ((IndexInsnNode) assignInsn).getIndex();
					if (assignCastType.equals(castInsn.getIndex())) {
						return assignInsn;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Remove unnecessary instructions
	 */
	private static void removeStep(MethodNode mth, InsnRemover remover) {
		for (BlockNode block : mth.getBasicBlocks()) {
			remover.setBlock(block);
			for (InsnNode insn : block.getInstructions()) {
				switch (insn.getType()) {
					case NOP:
					case GOTO:
					case NEW_INSTANCE:
						remover.addAndUnbind(insn);
						break;

					default:
						if (insn.contains(AFlag.REMOVE)) {
							remover.addAndUnbind(insn);
						}
						break;
				}
			}
			remover.perform();
		}
	}

	private static void iterativeRemoveStep(MethodNode mth) {
		boolean changed;
		do {
			changed = false;
			for (BlockNode block : mth.getBasicBlocks()) {
				for (InsnNode insn : block.getInstructions()) {
					if (insn.getType() == InsnType.MOVE
							&& insn.isAttrStorageEmpty()
							&& isResultArgNotUsed(insn)) {
						InsnRemover.remove(mth, block, insn);
						changed = true;
						break;
					}
				}
			}
		} while (changed);
	}

	private static boolean isResultArgNotUsed(InsnNode insn) {
		RegisterArg result = insn.getResult();
		if (result != null) {
			SSAVar ssaVar = result.getSVar();
			return ssaVar.getUseCount() == 0;
		}
		return false;
	}

	/**
	 * For args in anonymous constructor invoke apply:
	 * - forbid inline into constructor call
	 * - make variables final (compiler require this implicitly)
	 */
	private static void processAnonymousConstructor(MethodNode mth, ConstructorInsn co) {
		IMethodDetails callMthDetails = mth.root().getMethodUtils().getMethodDetails(co);
		if (!(callMthDetails instanceof MethodNode)) {
			return;
		}
		MethodNode callMth = (MethodNode) callMthDetails;
		if (!callMth.contains(AFlag.ANONYMOUS_CONSTRUCTOR) || callMth.contains(AFlag.NO_SKIP_ARGS)) {
			return;
		}
		SkipMethodArgsAttr attr = callMth.get(AType.SKIP_MTH_ARGS);
		if (attr != null) {
			int argsCount = Math.min(callMth.getMethodInfo().getArgsCount(), co.getArgsCount());
			for (int i = 0; i < argsCount; i++) {
				if (attr.isSkip(i)) {
					anonymousCallArgMod(co.getArg(i));
				}
			}
		} else {
			// additional info not available apply mods to all args (the safest solution)
			co.getArguments().forEach(ModVisitor::anonymousCallArgMod);
		}
	}

	private static void anonymousCallArgMod(InsnArg arg) {
		arg.add(AFlag.DONT_INLINE);
		if (arg.isRegister()) {
			((RegisterArg) arg).getSVar().getCodeVar().setFinal(true);
		}
	}

	/**
	 * Return first usage instruction for arg.
	 * If used only once try to follow move chain
	 */
	@Nullable
	private static InsnNode getFirstUseSkipMove(RegisterArg arg) {
		SSAVar sVar = arg.getSVar();
		int useCount = sVar.getUseCount();
		if (useCount == 0) {
			return null;
		}
		RegisterArg useArg = sVar.getUseList().get(0);
		InsnNode parentInsn = useArg.getParentInsn();
		if (parentInsn == null) {
			return null;
		}
		if (useCount == 1 && parentInsn.getType() == InsnType.MOVE) {
			return getFirstUseSkipMove(parentInsn.getResult());
		}
		return parentInsn;
	}

	private static InsnNode makeFilledArrayInsn(MethodNode mth, NewArrayNode newArrayNode, FillArrayInsn insn) {
		ArgType insnArrayType = newArrayNode.getArrayType();
		ArgType insnElementType = insnArrayType.getArrayElement();
		ArgType elType = insn.getElementType();
		if (!elType.isTypeKnown()
				&& insnElementType.isPrimitive()
				&& elType.contains(insnElementType.getPrimitiveType())) {
			elType = insnElementType;
		}
		if (!elType.equals(insnElementType) && !insnArrayType.equals(ArgType.OBJECT)) {
			mth.addWarn("Incorrect type for fill-array insn " + InsnUtils.formatOffset(insn.getOffset())
					+ ", element type: " + elType + ", insn element type: " + insnElementType);
		}
		if (!elType.isTypeKnown()) {
			LOG.warn("Unknown array element type: {} in mth: {}", elType, mth);
			elType = insnElementType.isTypeKnown() ? insnElementType : elType.selectFirst();
			if (elType == null) {
				throw new JadxRuntimeException("Null array element type");
			}
		}

		List<LiteralArg> list = insn.getLiteralArgs(elType);
		InsnNode filledArr = new FilledNewArrayNode(elType, list.size());
		filledArr.setResult(newArrayNode.getResult().duplicate());
		for (LiteralArg arg : list) {
			FieldNode f = mth.getParentClass().getConstFieldByLiteralArg(arg);
			if (f != null) {
				InsnNode fGet = new IndexInsnNode(InsnType.SGET, f.getFieldInfo(), 0);
				filledArr.addArg(InsnArg.wrapArg(fGet));
				f.addUseIn(mth);
			} else {
				filledArr.addArg(arg.duplicate());
			}
		}
		return filledArr;
	}

	private static void processMoveException(MethodNode mth, BlockNode block, InsnNode insn, InsnRemover remover) {
		ExcHandlerAttr excHandlerAttr = block.get(AType.EXC_HANDLER);
		if (excHandlerAttr == null) {
			return;
		}
		ExceptionHandler excHandler = excHandlerAttr.getHandler();

		// result arg used both in this insn and exception handler,
		RegisterArg resArg = insn.getResult();
		ArgType type = excHandler.getArgType();
		String name = excHandler.isCatchAll() ? "th" : "e";
		if (resArg.getName() == null) {
			resArg.setName(name);
		}
		SSAVar sVar = insn.getResult().getSVar();
		if (sVar.getUseCount() == 0) {
			excHandler.setArg(new NamedArg(name, type));
			remover.addAndUnbind(insn);
		} else if (sVar.isUsedInPhi()) {
			// exception var moved to external variable => replace with 'move' insn
			InsnNode moveInsn = new InsnNode(InsnType.MOVE, 1);
			moveInsn.setResult(insn.getResult());
			NamedArg namedArg = new NamedArg(name, type);
			moveInsn.addArg(namedArg);
			excHandler.setArg(namedArg);
			replaceInsn(mth, block, 0, moveInsn);
		}
		block.copyAttributeFrom(insn, AType.CODE_COMMENTS); // save comment
	}
}
