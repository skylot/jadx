package jadx.core.dex.visitors;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
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
import jadx.core.dex.nodes.parser.FieldInitAttr;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.exceptions.JadxException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@JadxVisitor(
		name = "ExtractFieldInit",
		desc = "Move duplicated field initialization from constructors",
		runAfter = ModVisitor.class,
		runBefore = ClassModifier.class
)
public class ExtractFieldInit extends AbstractVisitor {

	@Override
	public boolean visit(ClassNode cls) throws JadxException {
		if (cls.isEnum()) {
			return false;
		}
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
				|| clinit.isNoCode()) {
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
	 * Remove final field in place initialization if it assign in class init method
	 */
	private static void processStaticFieldAssign(ClassNode cls, IndexInsnNode insn) {
		FieldInfo field = (FieldInfo) insn.getIndex();
		String thisClass = cls.getClassInfo().getFullName();
		if (field.getDeclClass().getFullName().equals(thisClass)) {
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
		for (FieldNode field : cls.getFields()) {
			if (field.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			if (field.getAccessFlags().isStatic()) {
				List<InsnNode> initInsns = getFieldAssigns(classInitMth, field, InsnType.SPUT);
				if (initInsns.size() == 1) {
					InsnNode insn = initInsns.get(0);
					if (checkInsn(insn)) {
						InstructionRemover.remove(classInitMth, insn);
						addFieldInitAttr(classInitMth, field, insn);
					}
				}
			}
		}
	}

	private static class InitInfo {
		private final MethodNode constrMth;
		private final List<InsnNode> putInsns = new ArrayList<InsnNode>();

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
		List<InitInfo> infoList = new ArrayList<InitInfo>(constrList.size());
		for (MethodNode constrMth : constrList) {
			if (constrMth.isNoCode() || constrMth.getBasicBlocks().isEmpty()) {
				return;
			}
			InitInfo info = new InitInfo(constrMth);
			infoList.add(info);
			// TODO: check not only first block
			BlockNode blockNode = constrMth.getBasicBlocks().get(0);
			for (InsnNode insn : blockNode.getInstructions()) {
				if (insn.getType() == InsnType.IPUT && checkInsn(insn)) {
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
		Set<FieldInfo> fields = new HashSet<FieldInfo>();
		for (InsnNode insn : common.getPutInsns()) {
			FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) insn).getIndex();
			FieldNode field = cls.dex().resolveField(fieldInfo);
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
				InstructionRemover.remove(info.getConstrMth(), putInsn);
			}
		}
		for (InsnNode insn : common.getPutInsns()) {
			FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) insn).getIndex();
			FieldNode field = cls.dex().resolveField(fieldInfo);
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

	private static boolean checkInsn(InsnNode insn) {
		InsnArg arg = insn.getArg(0);
		if (arg.isInsnWrap()) {
			InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
			if (!wrapInsn.canReorderRecursive() && insn.contains(AType.CATCH_BLOCK)) {
				return false;
			}
		} else {
			return arg.isLiteral() || arg.isThis();
		}
		Set<RegisterArg> regs = new HashSet<RegisterArg>();
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
		List<MethodNode> list = new ArrayList<MethodNode>();
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
		if (mth.isNoCode()) {
			return Collections.emptyList();
		}
		List<InsnNode> assignInsns = new ArrayList<InsnNode>();
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
