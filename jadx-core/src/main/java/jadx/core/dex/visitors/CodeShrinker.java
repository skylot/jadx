package jadx.core.dex.visitors;

import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnList;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeShrinker extends AbstractVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(CodeShrinker.class);

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode() || mth.getAttributes().contains(AttributeFlag.DONT_SHRINK)) {
			return;
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			shrinkBlock(mth, block);
		}
	}

	private static final class ArgsInfo {
		private final InsnNode insn;
		private final List<ArgsInfo> argsList;
		private final List<RegisterArg> args;
		private final int pos;
		private int inlineBorder;
		private ArgsInfo inlinedInsn;

		public ArgsInfo(InsnNode insn, List<ArgsInfo> argsList, int pos) {
			this.insn = insn;
			this.argsList = argsList;
			this.pos = pos;
			this.inlineBorder = pos;
			this.args = new LinkedList<RegisterArg>();
			addArgs(insn, args);
		}

		private static void addArgs(InsnNode insn, List<RegisterArg> args) {
			if (insn.getType() == InsnType.CONSTRUCTOR) {
				args.add(((ConstructorInsn) insn).getInstanceArg());
			}
			for (InsnArg arg : insn.getArguments()) {
				if (arg.isRegister()) {
					args.add((RegisterArg) arg);
				}
			}
			for (InsnArg arg : insn.getArguments()) {
				if (arg.isInsnWrap()) {
					addArgs(((InsnWrapArg) arg).getWrapInsn(), args);
				}
			}
		}

		public InsnNode getInsn() {
			return insn;
		}

		private List<RegisterArg> getArgs() {
			return args;
		}

		public WrapInfo checkInline(int assignPos, RegisterArg arg) {
			if (assignPos >= inlineBorder || !canMove(assignPos, inlineBorder)) {
				return null;
			}
			inlineBorder = assignPos;
			return inline(assignPos, arg);
		}

		private boolean canMove(int from, int to) {
			from++;
			if (from == to) {
				// previous instruction or on edge of inline border
				return true;
			}
			if (from > to) {
				throw new JadxRuntimeException("Invalid inline insn positions: " + from + " - " + to);
			}
			for (int i = from; i < to - 1; i++) {
				ArgsInfo argsInfo = argsList.get(i);
				if (argsInfo.getInlinedInsn() == this) {
					continue;
				}
				if (!argsInfo.insn.canReorder()) {
					return false;
				}
			}
			return true;
		}

		public WrapInfo inline(int assignInsnPos, RegisterArg arg) {
			ArgsInfo argsInfo = argsList.get(assignInsnPos);
			argsInfo.inlinedInsn = this;
			return new WrapInfo(argsInfo.insn, arg);
		}

		public ArgsInfo getInlinedInsn() {
			if (inlinedInsn != null) {
				ArgsInfo parent = inlinedInsn.getInlinedInsn();
				if (parent != null) {
					inlinedInsn = parent;
				}
			}
			return inlinedInsn;
		}

		@Override
		public String toString() {
			return "ArgsInfo: |" + inlineBorder
					+ " ->" + (inlinedInsn == null ? "-" : inlinedInsn.pos)
					+ " " + args + " : " + insn;
		}
	}

	private static final class WrapInfo {
		private final InsnNode insn;
		private final RegisterArg arg;

		public WrapInfo(InsnNode assignInsn, RegisterArg arg) {
			this.insn = assignInsn;
			this.arg = arg;
		}

		private InsnNode getInsn() {
			return insn;
		}

		private RegisterArg getArg() {
			return arg;
		}

		@Override
		public String toString() {
			return "WrapInfo: " + arg + " -> " + insn;
		}
	}

	private void shrinkBlock(MethodNode mth, BlockNode block) {
		InsnList insnList = new InsnList(block.getInstructions());
		int insnCount = insnList.size();
		List<ArgsInfo> argsList = new ArrayList<ArgsInfo>(insnCount);
		for (int i = 0; i < insnCount; i++) {
			argsList.add(new ArgsInfo(insnList.get(i), argsList, i));
		}
		List<WrapInfo> wrapList = new ArrayList<WrapInfo>();
		for (ArgsInfo argsInfo : argsList) {
			List<RegisterArg> args = argsInfo.getArgs();
			for (ListIterator<RegisterArg> it = args.listIterator(args.size()); it.hasPrevious(); ) {
				RegisterArg arg = it.previous();
				List<InsnArg> useList = arg.getTypedVar().getUseList();
				if (useList.size() != 2) {
					continue;
				}
				InsnNode assignInsn = selectOther(useList, arg).getParentInsn();
				if (assignInsn == null
						|| assignInsn.getResult() == null
						|| assignInsn.getResult().getRegNum() != arg.getRegNum()) {
					continue;
				}
				int assignPos = insnList.getIndex(assignInsn);
				if (assignPos != -1) {
					if (assignInsn.canReorder()) {
						wrapList.add(argsInfo.inline(assignPos, arg));
					} else {
						WrapInfo wrapInfo = argsInfo.checkInline(assignPos, arg);
						if (wrapInfo != null) {
							wrapList.add(wrapInfo);
						}
					}
				} else {
					// another block
					if (block.getPredecessors().size() == 1) {
						BlockNode assignBlock = BlockUtils.getBlockByInsn(mth, assignInsn);
						if (canMoveBetweenBlocks(assignInsn, assignBlock, block, argsInfo.getInsn())) {
							arg.wrapInstruction(assignInsn);
							InsnList.remove(assignBlock, assignInsn);
						}
					}
				}
			}
		}
		for (WrapInfo wrapInfo : wrapList) {
			wrapInfo.getArg().wrapInstruction(wrapInfo.getInsn());
		}
		for (WrapInfo wrapInfo : wrapList) {
			insnList.remove(wrapInfo.getInsn());
		}

	}

	private boolean canMoveBetweenBlocks(InsnNode assignInsn, BlockNode assignBlock,
	                                     BlockNode useBlock, InsnNode useInsn) {
		if (!useBlock.getPredecessors().contains(assignBlock)
				&& !BlockUtils.isOnlyOnePathExists(assignBlock, useBlock)) {
			return false;
		}
		boolean startCheck = false;
		for (InsnNode insn : assignBlock.getInstructions()) {
			if (startCheck) {
				if (!insn.canReorder()) {
					return false;
				}
			}
			if (insn == assignInsn) {
				startCheck = true;
			}
		}
		BlockNode next = assignBlock.getCleanSuccessors().get(0);
		while (next != useBlock) {
			for (InsnNode insn : assignBlock.getInstructions()) {
				if (!insn.canReorder()) {
					return false;
				}
			}
			next = next.getCleanSuccessors().get(0);
		}
		for (InsnNode insn : useBlock.getInstructions()) {
			if (insn == useInsn) {
				return true;
			}
			if (!insn.canReorder()) {
				return false;
			}
		}
		throw new JadxRuntimeException("Can't process instruction move : " + assignBlock);
	}

	public static InsnArg inlineArgument(MethodNode mth, RegisterArg arg) {
		InsnNode assignInsn = arg.getAssignInsn();
		if (assignInsn == null) {
			return null;
		}
		// recursively wrap all instructions
		List<RegisterArg> list = new ArrayList<RegisterArg>();
		List<RegisterArg> args = mth.getArguments(false);
		int i = 0;
		do {
			list.clear();
			assignInsn.getRegisterArgs(list);
			for (RegisterArg rarg : list) {
				InsnNode ai = rarg.getAssignInsn();
				if (ai != assignInsn && ai != null && ai != rarg.getParentInsn()) {
					rarg.wrapInstruction(ai);
				}
			}
			// remove method args
			if (list.size() != 0 && args.size() != 0) {
				list.removeAll(args);
			}
			i++;
			if (i > 1000) {
				throw new JadxRuntimeException("Can't inline arguments for: " + arg + " insn: " + assignInsn);
			}
		} while (!list.isEmpty());

		return arg.wrapInstruction(assignInsn);
	}

	private static InsnArg selectOther(List<InsnArg> list, RegisterArg insn) {
		InsnArg first = list.get(0);
		return insn == first ? list.get(1) : first;
	}
}
