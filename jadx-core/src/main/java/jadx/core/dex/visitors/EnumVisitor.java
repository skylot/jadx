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
import jadx.core.dex.attributes.nodes.RenameReasonAttr;
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
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.visitors.regions.CheckRegions;
import jadx.core.dex.visitors.regions.IfRegionVisitor;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.BlockInsnPair;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.utils.InsnUtils.checkInsnType;
import static jadx.core.utils.InsnUtils.getSingleArg;
import static jadx.core.utils.InsnUtils.getWrappedInsn;

@JadxVisitor(
		name = "EnumVisitor",
		desc = "Restore enum classes",
		runAfter = {
				CodeShrinkVisitor.class, // all possible instructions already inlined
				ModVisitor.class,
				ReSugarCode.class,
				IfRegionVisitor.class, // ternary operator inlined
				CheckRegions.class // regions processing finished
		},
		runBefore = {
				ExtractFieldInit.class
		}
)
public class EnumVisitor extends AbstractVisitor {

	private MethodInfo enumValueOfMth;
	private MethodInfo cloneMth;

	@Override
	public void init(RootNode root) {
		enumValueOfMth = MethodInfo.fromDetails(
				root,
				ClassInfo.fromType(root, ArgType.ENUM),
				"valueOf",
				Arrays.asList(ArgType.CLASS, ArgType.STRING),
				ArgType.ENUM);

		cloneMth = MethodInfo.fromDetails(root,
				ClassInfo.fromType(root, ArgType.OBJECT),
				"clone",
				Collections.emptyList(),
				ArgType.OBJECT);
	}

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		if (cls.isEnum()) {
			boolean converted;
			try {
				converted = convertToEnum(cls);
			} catch (Exception e) {
				cls.addWarnComment("Enum visitor error", e);
				converted = false;
			}
			if (!converted) {
				AccessInfo accessFlags = cls.getAccessFlags();
				if (accessFlags.isEnum()) {
					cls.setAccessFlags(accessFlags.remove(AccessFlags.ENUM));
					cls.addWarnComment("Failed to restore enum class, 'enum' modifier and super class removed");
				}
			}
		}
		return true;
	}

	private boolean convertToEnum(ClassNode cls) {
		ArgType superType = cls.getSuperClass();
		if (superType != null && superType.getObject().equals(ArgType.ENUM.getObject())) {
			cls.add(AFlag.REMOVE_SUPER_CLASS);
		}
		MethodNode classInitMth = cls.getClassInitMth();
		if (classInitMth == null) {
			cls.addWarnComment("Enum class init method not found");
			return false;
		}
		Region staticRegion = classInitMth.getRegion();
		if (staticRegion == null || classInitMth.getBasicBlocks().isEmpty()) {
			return false;
		}
		// collect blocks on linear part of static method (ignore branching on method end)
		List<BlockNode> staticBlocks = new ArrayList<>();
		for (IContainer subBlock : staticRegion.getSubBlocks()) {
			if (subBlock instanceof BlockNode) {
				staticBlocks.add((BlockNode) subBlock);
			} else {
				break;
			}
		}
		if (staticBlocks.isEmpty()) {
			cls.addWarnComment("Unexpected branching in enum static init block");
			return false;
		}
		EnumData data = new EnumData(cls, classInitMth, staticBlocks);
		if (!searchValuesField(data)) {
			return false;
		}
		List<EnumField> enumFields = null;
		InsnArg arrArg = data.valuesInitInsn.getArg(0);
		if (arrArg.isInsnWrap()) {
			InsnNode wrappedInsn = ((InsnWrapArg) arrArg).getWrapInsn();
			enumFields = extractEnumFieldsFromInsn(data, wrappedInsn);
		}
		if (enumFields == null) {
			cls.addWarnComment("Unknown enum class pattern. Please report as an issue!");
			return false;
		}
		data.toRemove.add(data.valuesInitInsn);

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
			processConstructorInsn(data, enumField, classInitMth);
		}
		data.valuesField.add(AFlag.DONT_GENERATE);
		InsnRemover.removeAllAndUnbind(classInitMth, data.toRemove);
		if (classInitMth.countInsns() == 0) {
			classInitMth.add(AFlag.DONT_GENERATE);
		} else if (!data.toRemove.isEmpty()) {
			CodeShrinkVisitor.shrinkMethod(classInitMth);
		}
		removeEnumMethods(cls, data.valuesField);
		return true;
	}

	/**
	 * Search "$VALUES" field (holds all enum values)
	 */
	private boolean searchValuesField(EnumData data) {
		ArgType clsType = data.cls.getClassInfo().getType();
		List<FieldNode> valuesCandidates = data.cls.getFields().stream()
				.filter(f -> f.getAccessFlags().isStatic())
				.filter(f -> f.getType().isArray())
				.filter(f -> Objects.equals(f.getType().getArrayRootElement(), clsType))
				.collect(Collectors.toList());

		if (valuesCandidates.isEmpty()) {
			data.cls.addWarnComment("$VALUES field not found");
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
			data.cls.addWarnComment("Found several \"values\" enum fields: " + valuesCandidates);
			return false;
		}
		data.valuesField = valuesCandidates.get(0);

		// search "$VALUES" array init and collect enum fields
		BlockInsnPair valuesInitPair = getValuesInitInsn(data);
		if (valuesInitPair == null) {
			return false;
		}
		data.valuesInitInsn = valuesInitPair.getInsn();
		return true;
	}

	private void processConstructorInsn(EnumData data, EnumField enumField, MethodNode classInitMth) {
		ConstructorInsn co = enumField.getConstrInsn();
		ClassInfo enumClsInfo = co.getClassType();
		if (!enumClsInfo.equals(data.cls.getClassInfo())) {
			ClassNode enumCls = data.cls.root().resolveClass(enumClsInfo);
			if (enumCls != null) {
				processEnumCls(data.cls, enumField, enumCls);
			}
		}
		MethodNode ctrMth = data.cls.root().resolveMethod(co.getCallMth());
		if (ctrMth != null) {
			markArgsForSkip(ctrMth);
		}
		RegisterArg coResArg = co.getResult();
		if (coResArg == null || coResArg.getSVar().getUseList().size() <= 2) {
			data.toRemove.add(co);
		} else {
			// constructor result used in other places -> replace constructor with enum field get (SGET)
			IndexInsnNode enumGet = new IndexInsnNode(InsnType.SGET, enumField.getField().getFieldInfo(), 0);
			enumGet.setResult(coResArg.duplicate());
			BlockUtils.replaceInsn(classInitMth, co, enumGet);
		}
	}

	@Nullable
	private List<EnumField> extractEnumFieldsFromInsn(EnumData enumData, InsnNode wrappedInsn) {
		switch (wrappedInsn.getType()) {
			case FILLED_NEW_ARRAY:
				return extractEnumFieldsFromFilledArray(enumData, wrappedInsn);

			case INVOKE:
				// handle redirection of values array fill (added in java 15)
				return extractEnumFieldsFromInvoke(enumData, (InvokeNode) wrappedInsn);

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

	private List<EnumField> extractEnumFieldsFromInvoke(EnumData enumData, InvokeNode invokeNode) {
		MethodInfo callMth = invokeNode.getCallMth();
		MethodNode valuesMth = enumData.cls.root().resolveMethod(callMth);
		if (valuesMth == null || valuesMth.isVoidReturn()) {
			return null;
		}
		BlockNode returnBlock = Utils.getOne(valuesMth.getPreExitBlocks());
		InsnNode returnInsn = BlockUtils.getLastInsn(returnBlock);
		InsnNode wrappedInsn = getWrappedInsn(getSingleArg(returnInsn));
		if (wrappedInsn == null) {
			return null;
		}
		List<EnumField> enumFields = extractEnumFieldsFromInsn(enumData, wrappedInsn);
		if (enumFields != null) {
			valuesMth.add(AFlag.DONT_GENERATE);
		}
		return enumFields;
	}

	private BlockInsnPair getValuesInitInsn(EnumData data) {
		FieldInfo searchField = data.valuesField.getFieldInfo();
		for (BlockNode blockNode : data.staticBlocks) {
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

	private List<EnumField> extractEnumFieldsFromFilledArray(EnumData enumData, InsnNode arrFillInsn) {
		List<EnumField> enumFields = new ArrayList<>();
		for (InsnArg arg : arrFillInsn.getArguments()) {
			EnumField field = null;
			if (arg.isInsnWrap()) {
				InsnNode wrappedInsn = ((InsnWrapArg) arg).getWrapInsn();
				field = processEnumFieldByWrappedInsn(enumData, wrappedInsn);
			} else if (arg.isRegister()) {
				field = processEnumFieldByRegister(enumData, (RegisterArg) arg);
			}
			if (field == null) {
				return null;
			}
			enumFields.add(field);
		}
		enumData.toRemove.add(arrFillInsn);
		return enumFields;
	}

	private EnumField processEnumFieldByWrappedInsn(EnumData data, InsnNode wrappedInsn) {
		if (wrappedInsn.getType() == InsnType.SGET) {
			return processEnumFieldByField(data, wrappedInsn);
		}
		ConstructorInsn constructorInsn = castConstructorInsn(wrappedInsn);
		if (constructorInsn != null) {
			FieldNode enumFieldNode = createFakeField(data.cls, "EF" + constructorInsn.getOffset());
			data.cls.addField(enumFieldNode);
			return createEnumFieldByConstructor(data.cls, enumFieldNode, constructorInsn);
		}
		return null;
	}

	@Nullable
	private EnumField processEnumFieldByField(EnumData data, InsnNode sgetInsn) {
		if (sgetInsn.getType() != InsnType.SGET) {
			return null;
		}
		FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) sgetInsn).getIndex();
		FieldNode enumFieldNode = data.cls.searchField(fieldInfo);
		if (enumFieldNode == null) {
			return null;
		}
		InsnNode sputInsn = searchFieldPutInsn(data, enumFieldNode);
		if (sputInsn == null) {
			return null;
		}

		ConstructorInsn co = getConstructorInsn(sputInsn);
		if (co == null) {
			return null;
		}
		RegisterArg sgetResult = sgetInsn.getResult();
		if (sgetResult == null || sgetResult.getSVar().getUseCount() == 1) {
			data.toRemove.add(sgetInsn);
		}
		data.toRemove.add(sputInsn);
		return createEnumFieldByConstructor(data.cls, enumFieldNode, co);
	}

	@Nullable
	private EnumField processEnumFieldByRegister(EnumData data, RegisterArg arg) {
		InsnNode assignInsn = arg.getAssignInsn();
		if (assignInsn != null && assignInsn.getType() == InsnType.SGET) {
			return processEnumFieldByField(data, assignInsn);
		}

		SSAVar ssaVar = arg.getSVar();
		if (ssaVar.getUseCount() == 0) {
			return null;
		}
		InsnNode constrInsn = ssaVar.getAssign().getParentInsn();
		if (constrInsn == null || constrInsn.getType() != InsnType.CONSTRUCTOR) {
			return null;
		}
		FieldNode enumFieldNode = searchEnumField(data, ssaVar);
		if (enumFieldNode == null) {
			enumFieldNode = createFakeField(data.cls, "EF" + arg.getRegNum());
			data.cls.addField(enumFieldNode);
		}
		return createEnumFieldByConstructor(data.cls, enumFieldNode, (ConstructorInsn) constrInsn);
	}

	private FieldNode createFakeField(ClassNode cls, String name) {
		FieldNode enumFieldNode;
		FieldInfo fldInfo = FieldInfo.from(cls.root(), cls.getClassInfo(), name, cls.getType());
		enumFieldNode = new FieldNode(cls, fldInfo, 0);
		enumFieldNode.add(AFlag.SYNTHETIC);
		enumFieldNode.addInfoComment("Fake field, exist only in values array");
		return enumFieldNode;
	}

	@Nullable
	private FieldNode searchEnumField(EnumData data, SSAVar ssaVar) {
		InsnNode sputInsn = ssaVar.getUseList().get(0).getParentInsn();
		if (sputInsn == null || sputInsn.getType() != InsnType.SPUT) {
			return null;
		}
		FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) sputInsn).getIndex();
		FieldNode enumFieldNode = data.cls.searchField(fieldInfo);
		if (enumFieldNode == null) {
			return null;
		}
		data.toRemove.add(sputInsn);
		return enumFieldNode;
	}

	@SuppressWarnings("StatementWithEmptyBody")
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
		if (constrCls.equals(cls)) {
			// allow same class
		} else if (constrCls.contains(AType.ANONYMOUS_CLASS)) {
			// allow external class already marked as anonymous
		} else {
			return null;
		}
		MethodNode ctrMth = cls.root().resolveMethod(co.getCallMth());
		if (ctrMth == null) {
			return null;
		}
		List<RegisterArg> regs = new ArrayList<>();
		co.getRegisterArgs(regs);
		if (!regs.isEmpty()) {
			throw new JadxRuntimeException("Init of enum " + enumFieldNode.getName() + " uses external variables");
		}
		return new EnumField(enumFieldNode, co);
	}

	@Nullable
	private InsnNode searchFieldPutInsn(EnumData data, FieldNode enumFieldNode) {
		for (BlockNode block : data.staticBlocks) {
			for (InsnNode sputInsn : block.getInstructions()) {
				if (sputInsn != null && sputInsn.getType() == InsnType.SPUT) {
					FieldInfo f = (FieldInfo) ((IndexInsnNode) sputInsn).getIndex();
					FieldNode fieldNode = data.cls.searchField(f);
					if (Objects.equals(fieldNode, enumFieldNode)) {
						return sputInsn;
					}
				}
			}
		}
		return null;
	}

	private void removeEnumMethods(ClassNode cls, FieldNode valuesField) {
		ArgType clsType = cls.getClassInfo().getType();
		String valuesMethodShortId = "values()" + TypeGen.signature(ArgType.array(clsType));
		MethodNode valuesMethod = null;
		// remove compiler generated methods
		for (MethodNode mth : cls.getMethods()) {
			MethodInfo mi = mth.getMethodInfo();
			if (mi.isClassInit() || mth.isNoCode()) {
				continue;
			}
			String shortId = mi.getShortId();
			if (mi.isConstructor()) {
				if (isDefaultConstructor(mth, shortId)) {
					mth.add(AFlag.DONT_GENERATE);
				}
				markArgsForSkip(mth);
			} else if (mi.getShortId().equals(valuesMethodShortId)) {
				if (isValuesMethod(mth, clsType)) {
					valuesMethod = mth;
					mth.add(AFlag.DONT_GENERATE);
				} else {
					// custom values method => rename to resolve conflict with enum method
					mth.getMethodInfo().setAlias("valuesCustom");
					mth.addAttr(new RenameReasonAttr(mth).append("to resolve conflict with enum method"));
				}
			} else if (isValuesMethod(mth, clsType)) {
				if (!mth.getMethodInfo().getAlias().equals("values") && !mth.getUseIn().isEmpty()) {
					// rename to use default values method
					mth.getMethodInfo().setAlias("values");
					mth.addAttr(new RenameReasonAttr(mth).append("to match enum method name"));
					mth.add(AFlag.DONT_RENAME);
				}
				valuesMethod = mth;
				mth.add(AFlag.DONT_GENERATE);
			} else if (simpleValueOfMth(mth, clsType)) {
				mth.add(AFlag.DONT_GENERATE);
			}
		}
		FieldInfo valuesFieldInfo = valuesField.getFieldInfo();
		for (MethodNode mth : cls.getMethods()) {
			// fix access to 'values' field and 'values()' method
			fixValuesAccess(mth, valuesFieldInfo, clsType, valuesMethod);
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

	// TODO: support other method patterns ???
	private boolean isValuesMethod(MethodNode mth, ArgType clsType) {
		ArgType retType = mth.getReturnType();
		if (!retType.isArray() || !retType.getArrayElement().equals(clsType)) {
			return false;
		}
		InsnNode returnInsn = BlockUtils.getOnlyOneInsnFromMth(mth);
		if (returnInsn == null || returnInsn.getType() != InsnType.RETURN || returnInsn.getArgsCount() != 1) {
			return false;
		}
		InsnNode wrappedInsn = getWrappedInsn(getSingleArg(returnInsn));
		IndexInsnNode castInsn = (IndexInsnNode) checkInsnType(wrappedInsn, InsnType.CHECK_CAST);
		if (castInsn != null && Objects.equals(castInsn.getIndex(), ArgType.array(clsType))) {
			InvokeNode invokeInsn = (InvokeNode) checkInsnType(getWrappedInsn(getSingleArg(castInsn)), InsnType.INVOKE);
			return invokeInsn != null && invokeInsn.getCallMth().equals(cloneMth);
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

	private void fixValuesAccess(MethodNode mth, FieldInfo valuesFieldInfo, ArgType clsType, @Nullable MethodNode valuesMethod) {
		MethodInfo mi = mth.getMethodInfo();
		if (mi.isConstructor() || mi.isClassInit() || mth.isNoCode() || mth == valuesMethod) {
			return;
		}
		// search value field usage
		Predicate<InsnNode> insnTest = insn -> Objects.equals(((IndexInsnNode) insn).getIndex(), valuesFieldInfo);
		InsnNode useInsn = InsnUtils.searchInsn(mth, InsnType.SGET, insnTest);
		if (useInsn == null) {
			return;
		}
		// replace 'values' field with 'values()' method
		InsnUtils.replaceInsns(mth, insn -> {
			if (insn.getType() == InsnType.SGET && insnTest.test(insn)) {
				MethodInfo valueMth = valuesMethod == null
						? getValueMthInfo(mth.root(), clsType)
						: valuesMethod.getMethodInfo();
				InvokeNode invokeNode = new InvokeNode(valueMth, InvokeType.STATIC, 0);
				invokeNode.setResult(insn.getResult());
				if (valuesMethod == null) {
					// forcing enum method (can overlap and get renamed by custom method)
					invokeNode.add(AFlag.FORCE_RAW_NAME);
				}
				mth.addDebugComment("Replace access to removed values field (" + valuesFieldInfo.getName() + ") with 'values()' method");
				return invokeNode;
			}
			return null;
		});
	}

	private MethodInfo getValueMthInfo(RootNode root, ArgType clsType) {
		return MethodInfo.fromDetails(root,
				ClassInfo.fromType(root, clsType),
				"values",
				Collections.emptyList(), ArgType.array(clsType));
	}

	private static void processEnumCls(ClassNode cls, EnumField field, ClassNode innerCls) {
		// remove constructor, because it is anonymous class
		for (MethodNode innerMth : innerCls.getMethods()) {
			if (innerMth.getAccessFlags().isConstructor()) {
				innerMth.add(AFlag.DONT_GENERATE);
			}
		}
		field.setCls(innerCls);
		if (!innerCls.getParentClass().equals(cls)) {
			// not inner
			cls.addInlinedClass(innerCls);
			innerCls.add(AFlag.DONT_GENERATE);
		}
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

	private static class EnumData {
		final ClassNode cls;
		final MethodNode classInitMth;
		final List<BlockNode> staticBlocks;
		final List<InsnNode> toRemove = new ArrayList<>();
		FieldNode valuesField;
		InsnNode valuesInitInsn;

		public EnumData(ClassNode cls, MethodNode classInitMth, List<BlockNode> staticBlocks) {
			this.cls = cls;
			this.classInitMth = classInitMth;
			this.staticBlocks = staticBlocks;
		}
	}
}
