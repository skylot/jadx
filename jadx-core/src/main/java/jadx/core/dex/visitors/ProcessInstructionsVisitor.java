package jadx.core.dex.visitors;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.JumpInfo;
import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.instructions.FillArrayData;
import jadx.core.dex.instructions.FillArrayInsn;
import jadx.core.dex.instructions.FilledNewArrayNode;
import jadx.core.dex.instructions.GotoNode;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.SwitchData;
import jadx.core.dex.instructions.SwitchInsn;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.blocks.BlockSplitter;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

@JadxVisitor(
		name = "Process Instructions Visitor",
		desc = "Init instructions info",
		runBefore = {
				BlockSplitter.class
		}
)
public class ProcessInstructionsVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}

		initJumps(mth, mth.getInstructions());
	}

	private static void initJumps(MethodNode mth, InsnNode[] insnByOffset) {
		for (int offset = 0; offset < insnByOffset.length; offset++) {
			InsnNode insn = insnByOffset[offset];
			if (insn == null) {
				continue;
			}
			switch (insn.getType()) {
				case SWITCH:
					SwitchInsn sw = (SwitchInsn) insn;
					if (sw.needData()) {
						attachSwitchData(insnByOffset, offset, sw);
					}
					int defCaseOffset = sw.getDefaultCaseOffset();
					if (defCaseOffset != -1) {
						addJump(mth, insnByOffset, offset, defCaseOffset);
					}
					for (int target : sw.getTargets()) {
						addJump(mth, insnByOffset, offset, target);
					}
					break;

				case IF:
					int next = getNextInsnOffset(insnByOffset, offset);
					if (next != -1) {
						addJump(mth, insnByOffset, offset, next);
					}
					addJump(mth, insnByOffset, offset, ((IfNode) insn).getTarget());
					break;

				case GOTO:
					addJump(mth, insnByOffset, offset, ((GotoNode) insn).getTarget());
					break;

				case INVOKE:
					if (insn.getResult() == null) {
						ArgType retType = ((BaseInvokeNode) insn).getCallMth().getReturnType();
						mergeMoveResult(insnByOffset, offset, insn, retType);
					}
					break;

				case STR_CONCAT:
					// invoke-custom with string concatenation translated directly to STR_CONCAT, merge next move-result
					if (insn.getResult() == null) {
						mergeMoveResult(insnByOffset, offset, insn, ArgType.STRING);
					}
					break;

				case FILLED_NEW_ARRAY:
					ArgType arrType = ((FilledNewArrayNode) insn).getArrayType();
					mergeMoveResult(insnByOffset, offset, insn, arrType);
					break;

				case FILL_ARRAY:
					FillArrayInsn fillArrayInsn = (FillArrayInsn) insn;
					int target = fillArrayInsn.getTarget();
					InsnNode arrDataInsn = getInsnAtOffset(insnByOffset, target);
					if (arrDataInsn != null && arrDataInsn.getType() == InsnType.FILL_ARRAY_DATA) {
						fillArrayInsn.setArrayData((FillArrayData) arrDataInsn);
						removeInsn(insnByOffset, arrDataInsn);
					} else {
						throw new JadxRuntimeException("Payload for fill-array not found at " + InsnUtils.formatOffset(target));
					}
					break;

				default:
					break;
			}
		}
	}

	private static void attachSwitchData(InsnNode[] insnByOffset, int offset, SwitchInsn sw) {
		int nextInsnOffset = getNextInsnOffset(insnByOffset, offset);
		int dataTarget = sw.getDataTarget();
		InsnNode switchDataInsn = getInsnAtOffset(insnByOffset, dataTarget);
		if (switchDataInsn != null && switchDataInsn.getType() == InsnType.SWITCH_DATA) {
			SwitchData data = (SwitchData) switchDataInsn;
			data.fixTargets(offset);
			sw.attachSwitchData(data, nextInsnOffset);
			removeInsn(insnByOffset, switchDataInsn);
		} else {
			throw new JadxRuntimeException("Payload for switch not found at " + InsnUtils.formatOffset(dataTarget));
		}
	}

	private static void mergeMoveResult(InsnNode[] insnByOffset, int offset, InsnNode insn, ArgType resType) {
		int nextInsnOffset = getNextInsnOffset(insnByOffset, offset);
		if (nextInsnOffset == -1) {
			return;
		}
		InsnNode nextInsn = insnByOffset[nextInsnOffset];
		if (nextInsn.getType() != InsnType.MOVE_RESULT) {
			return;
		}
		RegisterArg moveRes = nextInsn.getResult();
		insn.setResult(moveRes.duplicate(resType));
		insn.copyAttributesFrom(nextInsn);
		removeInsn(insnByOffset, nextInsn);
	}

	private static void addJump(MethodNode mth, InsnNode[] insnByOffset, int offset, int target) {
		try {
			insnByOffset[target].addAttr(AType.JUMP, new JumpInfo(offset, target));
		} catch (Exception e) {
			mth.addError("Failed to set jump: " + InsnUtils.formatOffset(offset) + " -> " + InsnUtils.formatOffset(target), e);
		}
	}

	public static int getNextInsnOffset(InsnNode[] insnByOffset, int offset) {
		int len = insnByOffset.length;
		for (int i = offset + 1; i < len; i++) {
			InsnNode insnNode = insnByOffset[i];
			if (insnNode != null && insnNode.getType() != InsnType.NOP) {
				return i;
			}
		}
		return -1;
	}

	@Nullable
	private static InsnNode getInsnAtOffset(InsnNode[] insnByOffset, int offset) {
		int len = insnByOffset.length;
		for (int i = offset; i < len; i++) {
			InsnNode insnNode = insnByOffset[i];
			if (insnNode != null && insnNode.getType() != InsnType.NOP) {
				return insnNode;
			}
		}
		return null;
	}

	private static void removeInsn(InsnNode[] insnByOffset, InsnNode insn) {
		insnByOffset[insn.getOffset()] = null;
	}
}
