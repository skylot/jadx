package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.fldinit.FieldInitAttr;
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
		checkStaticFieldsInit(cls);
		moveStaticFieldsInit(cls);
		moveCommonFieldsInit(cls);
		return false;
	}

	private static void checkStaticFieldsInit(ClassNode cls) {
		MethodNode clinit = cls.getClassInitMth();
		if (clinit == null
				|| !clinit.getAccessFlags().isStatic()
				|| clinit.isNoCode()
				|| clinit.getBasicBlocks() == null) {
			return;
		}

		for (BlockNode block : clinit.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn.getType() == InsnType.SPUT) {
					processStaticFieldAssign(cls, (IndexInsnNode) insn);
				}
			}
		}
	}

	/**
	 * Remove a final field in place initialization if it an assign found in class init method
	 */
	private static void processStaticFieldAssign(ClassNode cls, IndexInsnNode insn) {
		FieldInfo field = (FieldInfo) insn.getIndex();
		if (field.getDeclClass().equals(cls.getClassInfo())) {
			FieldNode fn = cls.searchField(field);
			if (fn != null && fn.getAccessFlags().isFinal()) {
				fn.remove(AType.FIELD_INIT);
			}
		}
	}

	private static void moveStaticFieldsInit(ClassNode cls) {
		MethodNode classInitMth = cls.getClassInitMth();
		if (classInitMth == null) {
			return;
		}
		while (processFields(cls, classInitMth)) {
			// sometimes instructions moved to field init prevent from vars inline -> inline and try again
			CodeShrinkVisitor.shrinkMethod(classInitMth);
		}
	}

	private static boolean processFields(ClassNode cls, MethodNode classInitMth) {
		boolean changed = false;
		for (FieldNode field : cls.getFields()) {
			if (field.contains(AFlag.DONT_GENERATE) || field.contains(AType.FIELD_INIT)) {
				continue;
			}
			if (field.getAccessFlags().isStatic()) {
				List<InsnNode> initInsns = getFieldAssigns(classInitMth, field, InsnType.SPUT);
				if (initInsns.size() == 1) {
					InsnNode insn = initInsns.get(0);
					if (checkInsn(cls, insn)) {
						InsnArg arg = insn.getArg(0);
						if (arg instanceof InsnWrapArg) {
							((InsnWrapArg) arg).getWrapInsn().add(AFlag.DECLARE_VAR);
						}
						InsnRemover.remove(classInitMth, insn);
						addFieldInitAttr(classInitMth, field, insn);
						changed = true;
					}
				}
			}
		}
		return changed;
	}

	private static class InitInfo {
		private final MethodNode constrMth;
		private final List<InsnNode> putInsns = new ArrayList<>();

		private InitInfo(MethodNode constrMth) {
			this.constrMth = constrMth;
		}

		public MethodNode getConstrMth() {
			return constrMth;
		}

		public List<InsnNode> getPutInsns() {
			return putInsns;
		}
	}

	private static void moveCommonFieldsInit(ClassNode cls) {
		List<MethodNode> constrList = getConstructorsList(cls);
		if (constrList.isEmpty()) {
			return;
		}
		List<InitInfo> infoList = new ArrayList<>(constrList.size());
		for (MethodNode constrMth : constrList) {
			if (constrMth.isNoCode() || constrMth.getBasicBlocks().isEmpty()) {
				return;
			}
			InitInfo info = new InitInfo(constrMth);
			infoList.add(info);
			// TODO: check not only first block
			BlockNode blockNode = constrMth.getBasicBlocks().get(0);
			for (InsnNode insn : blockNode.getInstructions()) {
				if (insn.getType() == InsnType.IPUT && checkInsn(cls, insn)) {
					info.getPutInsns().add(insn);
				} else if (!info.getPutInsns().isEmpty()) {
					break;
				}
			}
		}
		// compare collected instructions
		InitInfo common = null;
		for (InitInfo info : infoList) {
			if (common == null) {
				common = info;
			} else if (!compareInsns(common.getPutInsns(), info.getPutInsns())) {
				return;
			}
		}
		if (common == null) {
			return;
		}
		Set<FieldInfo> fields = new HashSet<>();
		for (InsnNode insn : common.getPutInsns()) {
			FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) insn).getIndex();
			FieldNode field = cls.root().resolveField(fieldInfo);
			if (field == null) {
				return;
			}
			if (!fields.add(fieldInfo)) {
				return;
			}
		}
		// all checks passed
		for (InitInfo info : infoList) {
			for (InsnNode putInsn : info.getPutInsns()) {
				InsnArg arg = putInsn.getArg(0);
				if (arg instanceof InsnWrapArg) {
					((InsnWrapArg) arg).getWrapInsn().add(AFlag.DECLARE_VAR);
				}
				InsnRemover.remove(info.getConstrMth(), putInsn);
			}
		}
		for (InsnNode insn : common.getPutInsns()) {
			FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) insn).getIndex();
			FieldNode field = cls.root().resolveField(fieldInfo);
			addFieldInitAttr(common.getConstrMth(), field, insn);
		}
	}

	private static boolean compareInsns(List<InsnNode> base, List<InsnNode> other) {
		if (base.size() != other.size()) {
			return false;
		}
		int count = base.size();
		for (int i = 0; i < count; i++) {
			InsnNode baseInsn = base.get(i);
			InsnNode otherInsn = other.get(i);
			if (!baseInsn.isSame(otherInsn)) {
				return false;
			}
		}
		return true;
	}

	private static boolean checkInsn(ClassNode cls, InsnNode insn) {
		if (insn instanceof IndexInsnNode) {
			FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) insn).getIndex();
			if (!fieldInfo.getDeclClass().equals(cls.getClassInfo())) {
				// exclude fields from super classes
				return false;
			}
			FieldNode fieldNode = cls.root().resolveField(fieldInfo);
			if (fieldNode == null) {
				// exclude inherited fields (not declared in this class)
				return false;
			}
		} else {
			return false;
		}

		InsnArg arg = insn.getArg(0);
		if (arg.isInsnWrap()) {
			InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
			if (!wrapInsn.canReorderRecursive() && insn.contains(AType.CATCH_BLOCK)) {
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

	private static List<InsnNode> getFieldAssigns(MethodNode mth, FieldNode field, InsnType putInsn) {
		if (mth.isNoCode() || mth.getBasicBlocks() == null) {
			return Collections.emptyList();
		}
		List<InsnNode> assignInsns = new ArrayList<>();
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn.getType() == putInsn) {
					FieldInfo putNode = (FieldInfo) ((IndexInsnNode) insn).getIndex();
					if (putNode.equals(field.getFieldInfo())) {
						assignInsns.add(insn);
					}
				}
			}
		}
		return assignInsns;
	}

	private static void addFieldInitAttr(MethodNode classInitMth, FieldNode field, InsnNode insn) {
		InsnNode assignInsn = InsnNode.wrapArg(insn.getArg(0));
		field.addAttr(FieldInitAttr.insnValue(classInitMth, assignInsn));
	}
}
