package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.FieldInitInsnAttr;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "ExtractFieldInit",
		desc = "Move duplicated field initialization from constructors",
		runAfter = ModVisitor.class,
		runBefore = ClassModifier.class
)
public class ExtractFieldInit extends AbstractVisitor {

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		for (ClassNode inner : cls.getInnerClasses()) {
			visit(inner);
		}
		moveStaticFieldsInit(cls);
		moveCommonFieldsInit(cls);
		return false;
	}

	private static final class FieldInitInfo {
		final FieldNode fieldNode;
		final IndexInsnNode putInsn;
		final boolean singlePath;

		public FieldInitInfo(FieldNode fieldNode, IndexInsnNode putInsn, boolean singlePath) {
			this.fieldNode = fieldNode;
			this.putInsn = putInsn;
			this.singlePath = singlePath;
		}
	}

	private static final class ConstructorInitInfo {
		final MethodNode constructorMth;
		final List<FieldInitInfo> fieldInits;

		private ConstructorInitInfo(MethodNode constructorMth, List<FieldInitInfo> fieldInits) {
			this.constructorMth = constructorMth;
			this.fieldInits = fieldInits;
		}
	}

	private static void moveStaticFieldsInit(ClassNode cls) {
		MethodNode classInitMth = cls.getClassInitMth();
		if (classInitMth == null
				|| !classInitMth.getAccessFlags().isStatic()
				|| classInitMth.isNoCode()
				|| classInitMth.getBasicBlocks() == null) {
			return;
		}
		while (processStaticFields(cls, classInitMth)) {
			// sometimes instructions moved to field init prevent from vars inline -> inline and try again
			CodeShrinkVisitor.shrinkMethod(classInitMth);
		}
	}

	private static boolean processStaticFields(ClassNode cls, MethodNode classInitMth) {
		List<FieldInitInfo> inits = collectFieldsInit(cls, classInitMth, InsnType.SPUT);
		if (inits.isEmpty()) {
			return false;
		}
		// ignore field init constant if field initialized in class init method
		for (FieldInitInfo fieldInit : inits) {
			FieldNode field = fieldInit.fieldNode;
			if (field.getAccessFlags().isFinal()) {
				field.remove(JadxAttrType.CONSTANT_VALUE);
			}
		}
		filterFieldsInit(inits);
		if (inits.isEmpty()) {
			return false;
		}
		for (FieldInitInfo fieldInit : inits) {
			IndexInsnNode insn = fieldInit.putInsn;
			InsnArg arg = insn.getArg(0);
			if (arg instanceof InsnWrapArg) {
				((InsnWrapArg) arg).getWrapInsn().add(AFlag.DECLARE_VAR);
			}
			InsnRemover.remove(classInitMth, insn);
			addFieldInitAttr(classInitMth, fieldInit.fieldNode, insn);
		}
		fixFieldsOrder(cls, inits);
		return true;
	}

	private static void moveCommonFieldsInit(ClassNode cls) {
		List<MethodNode> constructors = getConstructorsList(cls);
		if (constructors.isEmpty()) {
			return;
		}
		List<ConstructorInitInfo> infoList = new ArrayList<>(constructors.size());
		for (MethodNode constructorMth : constructors) {
			if (constructorMth.isNoCode()) {
				return;
			}
			List<FieldInitInfo> inits = collectFieldsInit(cls, constructorMth, InsnType.IPUT);
			filterFieldsInit(inits);
			if (inits.isEmpty()) {
				return;
			}
			infoList.add(new ConstructorInitInfo(constructorMth, inits));
		}
		// compare collected instructions
		ConstructorInitInfo common = null;
		for (ConstructorInitInfo info : infoList) {
			if (common == null) {
				common = info;
				continue;
			}
			if (!compareFieldInits(common.fieldInits, info.fieldInits)) {
				return;
			}
		}
		if (common == null) {
			return;
		}
		// all checks passed
		for (ConstructorInitInfo info : infoList) {
			for (FieldInitInfo fieldInit : info.fieldInits) {
				IndexInsnNode putInsn = fieldInit.putInsn;
				InsnArg arg = putInsn.getArg(0);
				if (arg instanceof InsnWrapArg) {
					((InsnWrapArg) arg).getWrapInsn().add(AFlag.DECLARE_VAR);
				}
				InsnRemover.remove(info.constructorMth, putInsn);
			}
		}
		for (FieldInitInfo fieldInit : common.fieldInits) {
			addFieldInitAttr(common.constructorMth, fieldInit.fieldNode, fieldInit.putInsn);
		}
		fixFieldsOrder(cls, common.fieldInits);
	}

	private static List<FieldInitInfo> collectFieldsInit(ClassNode cls, MethodNode mth, InsnType putType) {
		List<FieldInitInfo> fieldsInit = new ArrayList<>();
		Set<BlockNode> singlePathBlocks = new HashSet<>();
		BlockUtils.visitSinglePath(mth.getEnterBlock(), singlePathBlocks::add);

		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn.getType() == putType) {
					IndexInsnNode putInsn = (IndexInsnNode) insn;
					FieldInfo field = (FieldInfo) putInsn.getIndex();
					if (field.getDeclClass().equals(cls.getClassInfo())) {
						FieldNode fn = cls.searchField(field);
						if (fn != null) {
							boolean singlePath = singlePathBlocks.contains(block);
							fieldsInit.add(new FieldInitInfo(fn, putInsn, singlePath));
						}
					}
				}
			}
		}
		return fieldsInit;
	}

	private static void filterFieldsInit(List<FieldInitInfo> inits) {
		// exclude fields initialized several times
		Set<FieldInfo> excludedFields = inits
				.stream()
				.collect(Collectors.toMap(fi -> fi.fieldNode, fi -> 1, Integer::sum))
				.entrySet()
				.stream()
				.filter(v -> v.getValue() > 1)
				.map(v -> v.getKey().getFieldInfo())
				.collect(Collectors.toSet());

		for (FieldInitInfo initInfo : inits) {
			if (!checkInsn(initInfo)) {
				excludedFields.add(initInfo.fieldNode.getFieldInfo());
			}
		}
		if (!excludedFields.isEmpty()) {
			boolean changed;
			do {
				changed = false;
				for (FieldInitInfo initInfo : inits) {
					FieldInfo fieldInfo = initInfo.fieldNode.getFieldInfo();
					if (excludedFields.contains(fieldInfo)) {
						continue;
					}
					if (insnUseExcludedField(initInfo, excludedFields)) {
						excludedFields.add(fieldInfo);
						changed = true;
					}
				}
			} while (changed);
		}

		// apply
		if (!excludedFields.isEmpty()) {
			inits.removeIf(fi -> excludedFields.contains(fi.fieldNode.getFieldInfo()));
		}
	}

	private static boolean checkInsn(FieldInitInfo initInfo) {
		if (!initInfo.singlePath) {
			return false;
		}
		IndexInsnNode insn = initInfo.putInsn;
		InsnArg arg = insn.getArg(0);
		if (arg.isInsnWrap()) {
			InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
			if (!wrapInsn.canReorderRecursive() && insn.contains(AType.EXC_CATCH)) {
				return false;
			}
		} else {
			return arg.isLiteral() || arg.isThis();
		}
		Set<RegisterArg> regs = new HashSet<>();
		insn.getRegisterArgs(regs);
		if (!regs.isEmpty()) {
			for (RegisterArg reg : regs) {
				if (!reg.isThis()) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean insnUseExcludedField(FieldInitInfo initInfo, Set<FieldInfo> excludedFields) {
		if (excludedFields.isEmpty()) {
			return false;
		}
		IndexInsnNode insn = initInfo.putInsn;
		boolean staticField = insn.getType() == InsnType.SPUT;
		InsnType useType = staticField ? InsnType.SGET : InsnType.IGET;
		// exclude if init code use any excluded field
		Boolean exclude = insn.visitInsns(innerInsn -> {
			if (innerInsn.getType() == useType) {
				FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) innerInsn).getIndex();
				if (excludedFields.contains(fieldInfo)) {
					return true;
				}
			}
			return null;
		});
		return Objects.equals(exclude, Boolean.TRUE);
	}

	private static void fixFieldsOrder(ClassNode cls, List<FieldInitInfo> fieldsInit) {
		List<FieldNode> clsFields = cls.getFields();
		List<FieldNode> orderedFields = Utils.collectionMap(fieldsInit, v -> v.fieldNode);
		// check if already ordered
		boolean ordered = Collections.indexOfSubList(clsFields, orderedFields) != -1;
		if (!ordered) {
			clsFields.removeAll(orderedFields);
			clsFields.addAll(orderedFields);
		}
	}

	private static boolean compareFieldInits(List<FieldInitInfo> base, List<FieldInitInfo> other) {
		if (base.size() != other.size()) {
			return false;
		}
		int count = base.size();
		for (int i = 0; i < count; i++) {
			InsnNode baseInsn = base.get(i).putInsn;
			InsnNode otherInsn = other.get(i).putInsn;
			if (!baseInsn.isSame(otherInsn)) {
				return false;
			}
		}
		return true;
	}

	private static List<MethodNode> getConstructorsList(ClassNode cls) {
		List<MethodNode> list = new ArrayList<>();
		for (MethodNode mth : cls.getMethods()) {
			AccessInfo accFlags = mth.getAccessFlags();
			if (!accFlags.isStatic() && accFlags.isConstructor()) {
				list.add(mth);
				if (BlockUtils.isAllBlocksEmpty(mth.getBasicBlocks())) {
					return Collections.emptyList();
				}
			}
		}
		return list;
	}

	private static void addFieldInitAttr(MethodNode mth, FieldNode field, InsnNode insn) {
		InsnNode assignInsn = InsnNode.wrapArg(insn.getArg(0));
		field.addAttr(new FieldInitInsnAttr(mth, assignInsn));
	}
}
