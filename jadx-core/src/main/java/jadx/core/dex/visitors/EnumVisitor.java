package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.AccessFlags;
import jadx.core.codegen.TypeGen;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.EnumClassAttr;
import jadx.core.dex.attributes.nodes.EnumClassAttr.EnumField;
import jadx.core.dex.attributes.nodes.SkipMethodArgsAttr;
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
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.BlockInsnPair;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxException;

import static jadx.core.utils.InsnUtils.checkInsnType;
import static jadx.core.utils.InsnUtils.getSingleArg;
import static jadx.core.utils.InsnUtils.getWrappedInsn;

@JadxVisitor(
		name = "EnumVisitor",
		desc = "Restore enum classes",
		runAfter = { CodeShrinkVisitor.class, ModVisitor.class, ReSugarCode.class },
		runBefore = { ExtractFieldInit.class }
)
public class EnumVisitor extends AbstractVisitor {

	private MethodInfo enumValueOfMth;

	@Override
	public void init(RootNode root) {
		enumValueOfMth = MethodInfo.fromDetails(
				root,
				ClassInfo.fromType(root, ArgType.ENUM),
				"valueOf",
				Arrays.asList(ArgType.CLASS, ArgType.STRING),
				ArgType.ENUM);
	}

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		if (!convertToEnum(cls)) {
			AccessInfo accessFlags = cls.getAccessFlags();
			if (accessFlags.isEnum()) {
				cls.setAccessFlags(accessFlags.remove(AccessFlags.ENUM));
				cls.addAttr(AType.COMMENTS, "JADX INFO: Failed to restore enum class, 'enum' modifier removed");
			}
		}
		return true;
	}

	private boolean convertToEnum(ClassNode cls) {
		if (!cls.isEnum()) {
			return false;
		}
		MethodNode classInitMth = cls.getClassInitMth();
		if (classInitMth == null) {
			cls.addAttr(AType.COMMENTS, "JADX INFO: Enum class init method not found");
			return false;
		}
		if (classInitMth.getBasicBlocks().isEmpty()) {
			return false;
		}
		ArgType clsType = cls.getClassInfo().getType();

		// search "$VALUES" field (holds all enum values)
		List<FieldNode> valuesCandidates = cls.getFields().stream()
				.filter(f -> f.getAccessFlags().isStatic())
				.filter(f -> f.getType().isArray())
				.filter(f -> Objects.equals(f.getType().getArrayRootElement(), clsType))
				.collect(Collectors.toList());

		if (valuesCandidates.isEmpty()) {
			return false;
		}
		if (valuesCandidates.size() > 1) {
			valuesCandidates.removeIf(f -> !f.getAccessFlags().isSynthetic());
		}
		if (valuesCandidates.size() > 1) {
			Optional<FieldNode> valuesOpt = valuesCandidates.stream().filter(f -> f.getName().equals("$VALUES")).findAny();
			if (valuesOpt.isPresent()) {
				valuesCandidates.clear();
				valuesCandidates.add(valuesOpt.get());
			}
		}
		if (valuesCandidates.size() != 1) {
			cls.addAttr(AType.COMMENTS, "JADX INFO: found several \"values\" enum fields: " + valuesCandidates);
			return false;
		}
		FieldNode valuesField = valuesCandidates.get(0);
		List<InsnNode> toRemove = new ArrayList<>();

		// search "$VALUES" array init and collect enum fields
		BlockInsnPair valuesInitPair = getValuesInitInsn(classInitMth, valuesField);
		if (valuesInitPair == null) {
			return false;
		}
		BlockNode staticBlock = valuesInitPair.getBlock();
		InsnNode valuesInitInsn = valuesInitPair.getInsn();

		List<EnumField> enumFields = null;
		InsnArg arrArg = valuesInitInsn.getArg(0);
		if (arrArg.isInsnWrap()) {
			InsnNode wrappedInsn = ((InsnWrapArg) arrArg).getWrapInsn();
			enumFields = extractEnumFieldsFromInsn(cls, staticBlock, wrappedInsn, toRemove);
		}
		if (enumFields == null) {
			return false;
		}
		toRemove.add(valuesInitInsn);

		// all checks complete, perform transform
		EnumClassAttr attr = new EnumClassAttr(enumFields);
		attr.setStaticMethod(classInitMth);
		cls.addAttr(attr);

		for (EnumField enumField : attr.getFields()) {
			ConstructorInsn co = enumField.getConstrInsn();
			FieldNode fieldNode = enumField.getField();

			// use string arg from the constructor as enum field name
			String name = getConstString(cls.root(), co.getArg(0));
			if (name != null
					&& !fieldNode.getAlias().equals(name)
					&& NameMapper.isValidAndPrintable(name)
					&& cls.root().getArgs().isRenameValid()) {
				fieldNode.getFieldInfo().setAlias(name);
			}
			fieldNode.add(AFlag.DONT_GENERATE);
			processConstructorInsn(cls, enumField, classInitMth, staticBlock);
		}
		valuesField.add(AFlag.DONT_GENERATE);
		InsnRemover.removeAllAndUnbind(classInitMth, staticBlock, toRemove);
		if (classInitMth.countInsns() == 0) {
			classInitMth.add(AFlag.DONT_GENERATE);
		} else if (!toRemove.isEmpty()) {
			CodeShrinkVisitor.shrinkMethod(classInitMth);
		}
		removeEnumMethods(cls, clsType, valuesField);
		return true;
	}

	private void processConstructorInsn(ClassNode cls, EnumField enumField, MethodNode classInitMth, BlockNode staticBlock) {
		ConstructorInsn co = enumField.getConstrInsn();
		ClassInfo enumClsInfo = co.getClassType();
		if (!enumClsInfo.equals(cls.getClassInfo())) {
			ClassNode enumCls = cls.root().resolveClass(enumClsInfo);
			if (enumCls != null) {
				processEnumCls(enumField, enumCls);
				cls.addInlinedClass(enumCls);
			}
		}
		List<RegisterArg> regs = new ArrayList<>();
		co.getRegisterArgs(regs);
		if (!regs.isEmpty()) {
			cls.addWarnComment("Init of enum " + enumField.getField().getName() + " can be incorrect");
		}
		MethodNode ctrMth = cls.root().resolveMethod(co.getCallMth());
		if (ctrMth != null) {
			markArgsForSkip(ctrMth);
		}
		RegisterArg coResArg = co.getResult();
		if (coResArg == null || coResArg.getSVar().getUseList().size() <= 2) {
			InsnRemover.removeWithoutUnbind(classInitMth, staticBlock, co);
		} else {
			// constructor result used in other places -> replace constructor with enum field get (SGET)
			IndexInsnNode enumGet = new IndexInsnNode(InsnType.SGET, enumField.getField().getFieldInfo(), 0);
			enumGet.setResult(coResArg.duplicate());
			BlockUtils.replaceInsn(classInitMth, staticBlock, co, enumGet);
		}
	}

	@Nullable
	private List<EnumField> extractEnumFieldsFromInsn(ClassNode cls, BlockNode staticBlock,
			InsnNode wrappedInsn, List<InsnNode> toRemove) {
		switch (wrappedInsn.getType()) {
			case FILLED_NEW_ARRAY:
				return extractEnumFieldsFromFilledArray(cls, wrappedInsn, staticBlock, toRemove);

			case INVOKE:
				// handle redirection of values array fill (added in java 15)
				return extractEnumFieldsFromInvoke(cls, staticBlock, (InvokeNode) wrappedInsn, toRemove);

			case NEW_ARRAY:
				InsnArg arg = wrappedInsn.getArg(0);
				if (arg.isZeroLiteral()) {
					// empty enum
					return Collections.emptyList();
				}
				return null;

			default:
				return null;
		}
	}

	private List<EnumField> extractEnumFieldsFromInvoke(ClassNode cls, BlockNode staticBlock,
			InvokeNode invokeNode, List<InsnNode> toRemove) {
		MethodInfo callMth = invokeNode.getCallMth();
		MethodNode valuesMth = cls.root().resolveMethod(callMth);
		if (valuesMth == null || valuesMth.isVoidReturn()) {
			return null;
		}
		BlockNode returnBlock = Utils.getOne(valuesMth.getExitBlocks());
		InsnNode returnInsn = BlockUtils.getLastInsn(returnBlock);
		InsnNode wrappedInsn = getWrappedInsn(getSingleArg(returnInsn));
		if (wrappedInsn == null) {
			return null;
		}
		List<EnumField> enumFields = extractEnumFieldsFromInsn(cls, staticBlock, wrappedInsn, toRemove);
		if (enumFields != null) {
			valuesMth.add(AFlag.DONT_GENERATE);
		}
		return enumFields;
	}

	private BlockInsnPair getValuesInitInsn(MethodNode classInitMth, FieldNode valuesField) {
		FieldInfo searchField = valuesField.getFieldInfo();
		for (BlockNode blockNode : classInitMth.getBasicBlocks()) {
			for (InsnNode insn : blockNode.getInstructions()) {
				if (insn.getType() == InsnType.SPUT) {
					IndexInsnNode indexInsnNode = (IndexInsnNode) insn;
					FieldInfo f = (FieldInfo) indexInsnNode.getIndex();
					if (f.equals(searchField)) {
						return new BlockInsnPair(blockNode, indexInsnNode);
					}
				}
			}
		}
		return null;
	}

	private List<EnumField> extractEnumFieldsFromFilledArray(ClassNode cls, InsnNode arrFillInsn, BlockNode staticBlock,
			List<InsnNode> toRemove) {
		List<EnumField> enumFields = new ArrayList<>();
		for (InsnArg arg : arrFillInsn.getArguments()) {
			EnumField field = null;
			if (arg.isInsnWrap()) {
				InsnNode wrappedInsn = ((InsnWrapArg) arg).getWrapInsn();
				field = processEnumFieldByWrappedInsn(cls, wrappedInsn, staticBlock, toRemove);
			} else if (arg.isRegister()) {
				field = processEnumFieldByRegister(cls, (RegisterArg) arg, staticBlock, toRemove);
			}
			if (field == null) {
				return null;
			}
			enumFields.add(field);
		}
		toRemove.add(arrFillInsn);
		return enumFields;
	}

	private EnumField processEnumFieldByWrappedInsn(ClassNode cls, InsnNode wrappedInsn, BlockNode staticBlock, List<InsnNode> toRemove) {
		if (wrappedInsn.getType() == InsnType.SGET) {
			return processEnumFieldByField(cls, wrappedInsn, staticBlock, toRemove);
		}
		ConstructorInsn constructorInsn = castConstructorInsn(wrappedInsn);
		if (constructorInsn != null) {
			FieldNode enumFieldNode = createFakeField(cls, "EF" + constructorInsn.getOffset());
			cls.addField(enumFieldNode);
			return createEnumFieldByConstructor(cls, enumFieldNode, constructorInsn);
		}
		return null;
	}

	@Nullable
	private EnumField processEnumFieldByField(ClassNode cls, InsnNode sgetInsn, BlockNode staticBlock, List<InsnNode> toRemove) {
		if (sgetInsn.getType() != InsnType.SGET) {
			return null;
		}
		FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) sgetInsn).getIndex();
		FieldNode enumFieldNode = cls.searchField(fieldInfo);
		if (enumFieldNode == null) {
			return null;
		}
		InsnNode sputInsn = searchFieldPutInsn(cls, staticBlock, enumFieldNode);
		if (sputInsn == null) {
			return null;
		}

		ConstructorInsn co = getConstructorInsn(sputInsn);
		if (co == null) {
			return null;
		}
		RegisterArg sgetResult = sgetInsn.getResult();
		if (sgetResult == null || sgetResult.getSVar().getUseCount() == 1) {
			toRemove.add(sgetInsn);
		}
		toRemove.add(sputInsn);
		return createEnumFieldByConstructor(cls, enumFieldNode, co);
	}

	@Nullable
	private EnumField processEnumFieldByRegister(ClassNode cls, RegisterArg arg, BlockNode staticBlock, List<InsnNode> toRemove) {
		InsnNode assignInsn = arg.getAssignInsn();
		if (assignInsn != null && assignInsn.getType() == InsnType.SGET) {
			return processEnumFieldByField(cls, assignInsn, staticBlock, toRemove);
		}

		SSAVar ssaVar = arg.getSVar();
		if (ssaVar.getUseCount() == 0) {
			return null;
		}
		InsnNode constrInsn = ssaVar.getAssign().getParentInsn();
		if (constrInsn == null || constrInsn.getType() != InsnType.CONSTRUCTOR) {
			return null;
		}
		FieldNode enumFieldNode = searchEnumField(cls, ssaVar, toRemove);
		if (enumFieldNode == null) {
			enumFieldNode = createFakeField(cls, "EF" + arg.getRegNum());
			cls.addField(enumFieldNode);
		}
		return createEnumFieldByConstructor(cls, enumFieldNode, (ConstructorInsn) constrInsn);
	}

	private FieldNode createFakeField(ClassNode cls, String name) {
		FieldNode enumFieldNode;
		FieldInfo fldInfo = FieldInfo.from(cls.root(), cls.getClassInfo(), name, cls.getType());
		enumFieldNode = new FieldNode(cls, fldInfo, 0);
		enumFieldNode.add(AFlag.SYNTHETIC);
		enumFieldNode.addAttr(AType.COMMENTS, "Fake field, exist only in values array");
		return enumFieldNode;
	}

	@Nullable
	private FieldNode searchEnumField(ClassNode cls, SSAVar ssaVar, List<InsnNode> toRemove) {
		InsnNode sputInsn = ssaVar.getUseList().get(0).getParentInsn();
		if (sputInsn == null || sputInsn.getType() != InsnType.SPUT) {
			return null;
		}
		FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) sputInsn).getIndex();
		FieldNode enumFieldNode = cls.searchField(fieldInfo);
		if (enumFieldNode == null) {
			return null;
		}
		toRemove.add(sputInsn);
		return enumFieldNode;
	}

	private EnumField createEnumFieldByConstructor(ClassNode cls, FieldNode enumFieldNode, ConstructorInsn co) {
		// usually constructor signature is '<init>(Ljava/lang/String;I)V'.
		// sometimes for one field enum second arg can be omitted
		if (co.getArgsCount() < 1) {
			return null;
		}
		ClassInfo clsInfo = co.getClassType();
		ClassNode constrCls = cls.root().resolveClass(clsInfo);
		if (constrCls == null) {
			return null;
		}
		if (!clsInfo.equals(cls.getClassInfo()) && !constrCls.getAccessFlags().isEnum()) {
			return null;
		}
		MethodNode ctrMth = cls.root().resolveMethod(co.getCallMth());
		if (ctrMth == null) {
			return null;
		}
		return new EnumField(enumFieldNode, co);
	}

	@Nullable
	private InsnNode searchFieldPutInsn(ClassNode cls, BlockNode staticBlock, FieldNode enumFieldNode) {
		for (InsnNode sputInsn : staticBlock.getInstructions()) {
			if (sputInsn != null && sputInsn.getType() == InsnType.SPUT) {
				FieldInfo f = (FieldInfo) ((IndexInsnNode) sputInsn).getIndex();
				FieldNode fieldNode = cls.searchField(f);
				if (Objects.equals(fieldNode, enumFieldNode)) {
					return sputInsn;
				}
			}
		}
		return null;
	}

	private void removeEnumMethods(ClassNode cls, ArgType clsType, FieldNode valuesField) {
		String valuesMethod = "values()" + TypeGen.signature(ArgType.array(clsType));
		FieldInfo valuesFieldInfo = valuesField.getFieldInfo();

		// remove compiler generated methods
		for (MethodNode mth : cls.getMethods()) {
			MethodInfo mi = mth.getMethodInfo();
			if (mi.isClassInit()) {
				continue;
			}
			String shortId = mi.getShortId();
			if (mi.isConstructor()) {
				if (isDefaultConstructor(mth, shortId)) {
					mth.add(AFlag.DONT_GENERATE);
				}
				markArgsForSkip(mth);
			} else if (shortId.equals(valuesMethod)
					|| usesValuesField(mth, valuesFieldInfo)
					|| simpleValueOfMth(mth, clsType)) {
				mth.add(AFlag.DONT_GENERATE);
			}
		}
	}

	private void markArgsForSkip(MethodNode mth) {
		// skip first and second args
		SkipMethodArgsAttr.skipArg(mth, 0);
		if (mth.getMethodInfo().getArgsCount() > 1) {
			SkipMethodArgsAttr.skipArg(mth, 1);
		}
	}

	private boolean isDefaultConstructor(MethodNode mth, String shortId) {
		boolean defaultId = shortId.equals("<init>(Ljava/lang/String;I)V")
				|| shortId.equals("<init>(Ljava/lang/String;)V");
		if (defaultId) {
			// check content
			return mth.countInsns() == 0;
		}
		return false;
	}

	private boolean simpleValueOfMth(MethodNode mth, ArgType clsType) {
		InsnNode returnInsn = InsnUtils.searchSingleReturnInsn(mth, insn -> insn.getArgsCount() == 1);
		if (returnInsn == null) {
			return false;
		}
		InsnNode wrappedInsn = getWrappedInsn(getSingleArg(returnInsn));
		IndexInsnNode castInsn = (IndexInsnNode) checkInsnType(wrappedInsn, InsnType.CHECK_CAST);
		if (castInsn != null && Objects.equals(castInsn.getIndex(), clsType)) {
			InvokeNode invokeInsn = (InvokeNode) checkInsnType(getWrappedInsn(getSingleArg(castInsn)), InsnType.INVOKE);
			return invokeInsn != null && invokeInsn.getCallMth().equals(enumValueOfMth);
		}
		return false;
	}

	private boolean usesValuesField(MethodNode mth, FieldInfo valuesFieldInfo) {
		Predicate<InsnNode> insnTest = insn -> Objects.equals(((IndexInsnNode) insn).getIndex(), valuesFieldInfo);
		return InsnUtils.searchInsn(mth, InsnType.SGET, insnTest) != null;
	}

	private static void processEnumCls(EnumField field, ClassNode innerCls) {
		// remove constructor, because it is anonymous class
		for (MethodNode innerMth : innerCls.getMethods()) {
			if (innerMth.getAccessFlags().isConstructor()) {
				innerMth.add(AFlag.DONT_GENERATE);
			}
		}
		field.setCls(innerCls);
		innerCls.add(AFlag.DONT_GENERATE);
	}

	private ConstructorInsn getConstructorInsn(InsnNode insn) {
		if (insn.getArgsCount() != 1) {
			return null;
		}
		InsnArg arg = insn.getArg(0);
		if (arg.isInsnWrap()) {
			return castConstructorInsn(((InsnWrapArg) arg).getWrapInsn());
		}
		if (arg.isRegister()) {
			return castConstructorInsn(((RegisterArg) arg).getAssignInsn());
		}
		return null;
	}

	@Nullable
	private ConstructorInsn castConstructorInsn(InsnNode coCandidate) {
		if (coCandidate != null && coCandidate.getType() == InsnType.CONSTRUCTOR) {
			return (ConstructorInsn) coCandidate;
		}
		return null;
	}

	private String getConstString(RootNode root, InsnArg arg) {
		if (arg.isInsnWrap()) {
			InsnNode constInsn = ((InsnWrapArg) arg).getWrapInsn();
			Object constValue = InsnUtils.getConstValueByInsn(root, constInsn);
			if (constValue instanceof String) {
				return (String) constValue;
			}
		}
		return null;
	}
}
