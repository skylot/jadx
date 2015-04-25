package jadx.core.dex.visitors;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.EmptyBitSet;
import jadx.core.utils.InsnList;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

public class CodeShrinker extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) {
		shrinkMethod(mth);
	}

	public static void shrinkMethod(MethodNode mth) {
		if (mth.isNoCode() || mth.contains(AFlag.DONT_SHRINK)) {
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
			this.args = getArgs(insn);
		}

		public static List<RegisterArg> getArgs(InsnNode insn) {
			List<RegisterArg> args = new LinkedList<RegisterArg>();
			addArgs(insn, args);
			return args;
		}

		private static void addArgs(InsnNode insn, List<RegisterArg> args) {
			if (insn.getType() == InsnType.CONSTRUCTOR) {
				args.add(((ConstructorInsn) insn).getInstanceArg());
			} else if (insn.getType() == InsnType.TERNARY) {
				args.addAll(((TernaryInsn) insn).getCondition().getRegisterArgs());
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
			if (!arg.isThis()
					&& (assignPos >= inlineBorder || !canMove(assignPos, inlineBorder))) {
				return null;
			}
			inlineBorder = assignPos;
			return inline(assignPos, arg);
		}

		private boolean canMove(int from, int to) {
			ArgsInfo startInfo = argsList.get(from);
			List<RegisterArg> movedArgs = startInfo.getArgs();
			int start = from + 1;
			if (start == to) {
				// previous instruction or on edge of inline border
				return true;
			}
			if (start > to) {
				throw new JadxRuntimeException("Invalid inline insn positions: " + start + " - " + to);
			}
			BitSet movedSet;
			if (movedArgs.isEmpty()) {
				if (startInfo.insn.isConstInsn()) {
					return true;
				}
				movedSet = EmptyBitSet.EMPTY;
			} else {
				movedSet = new BitSet();
				for (RegisterArg arg : movedArgs) {
					movedSet.set(arg.getRegNum());
				}
			}
			for (int i = start; i < to; i++) {
				ArgsInfo argsInfo = argsList.get(i);
				if (argsInfo.getInlinedInsn() == this) {
					continue;
				}
				InsnNode curInsn = argsInfo.insn;
				if (!curInsn.canReorder() || usedArgAssign(curInsn, movedSet)) {
					return false;
				}
			}
			return true;
		}

		private static boolean usedArgAssign(InsnNode insn, BitSet args) {
			RegisterArg result = insn.getResult();
			return result != null && args.get(result.getRegNum());
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

	private static void shrinkBlock(MethodNode mth, BlockNode block) {
		if (block.getInstructions().isEmpty()) {
			return;
		}
		InsnList insnList = new InsnList(block.getInstructions());
		int insnCount = insnList.size();
		List<ArgsInfo> argsList = new ArrayList<ArgsInfo>(insnCount);
		for (int i = 0; i < insnCount; i++) {
			argsList.add(new ArgsInfo(insnList.get(i), argsList, i));
		}
		List<WrapInfo> wrapList = new ArrayList<WrapInfo>();
		for (ArgsInfo argsInfo : argsList) {
			List<RegisterArg> args = argsInfo.getArgs();
			if (args.isEmpty()) {
				continue;
			}
			ListIterator<RegisterArg> it = args.listIterator(args.size());
			while (it.hasPrevious()) {
				RegisterArg arg = it.previous();
//				if (arg.getName() != null) {
//					continue;
//				}
				SSAVar sVar = arg.getSVar();
				// allow inline only one use arg or 'this'
				if (sVar == null
						|| sVar.getVariableUseCount() != 1 && !arg.isThis()
						|| sVar.contains(AFlag.DONT_INLINE)) {
					continue;
				}
				InsnNode assignInsn = sVar.getAssign().getParentInsn();
				if (assignInsn == null || assignInsn.contains(AFlag.DONT_INLINE)) {
					continue;
				}
				int assignPos = insnList.getIndex(assignInsn);
				if (assignPos != -1) {
					WrapInfo wrapInfo = argsInfo.checkInline(assignPos, arg);
					if (wrapInfo != null) {
						wrapList.add(wrapInfo);
					}
				} else {
					// another block
					BlockNode assignBlock = BlockUtils.getBlockByInsn(mth, assignInsn);
					if (assignBlock != null
							&& assignInsn != arg.getParentInsn()
							&& canMoveBetweenBlocks(assignInsn, assignBlock, block, argsInfo.getInsn())) {
						if (inline(arg, assignInsn, assignBlock, mth)) {
							InsnList.remove(assignBlock, assignInsn);
						}
					}
				}
			}
		}
		if (!wrapList.isEmpty()) {
			for (WrapInfo wrapInfo : wrapList) {
				inline(wrapInfo.getArg(), wrapInfo.getInsn(), block, mth);
			}
			for (WrapInfo wrapInfo : wrapList) {
				insnList.remove(wrapInfo.getInsn());
			}
		}
	}

	private static boolean inline(RegisterArg arg, InsnNode insn, @Nullable BlockNode block, MethodNode mth) {
		InsnNode parentInsn = arg.getParentInsn();
		// replace move instruction if needed
		if (parentInsn != null) {
			switch (parentInsn.getType()) {
				case MOVE: {
					if (block == null) {
						block = BlockUtils.getBlockByInsn(mth, parentInsn);
					}
					if (block != null) {
						int index = InsnList.getIndex(block.getInstructions(), parentInsn);
						if (index != -1) {
							insn.setResult(parentInsn.getResult());
							insn.copyAttributesFrom(parentInsn);
							insn.setOffset(parentInsn.getOffset());

							block.getInstructions().set(index, insn);
							return true;
						}
					}
					break;
				}
				case RETURN: {
					parentInsn.setSourceLine(insn.getSourceLine());
					break;
				}
			}
		}
		// simple case
		return arg.wrapInstruction(insn) != null;
	}

	private static boolean canMoveBetweenBlocks(InsnNode assignInsn, BlockNode assignBlock,
			BlockNode useBlock, InsnNode useInsn) {
		if (!BlockUtils.isPathExists(assignBlock, useBlock)) {
			return false;
		}

		List<RegisterArg> argsList = ArgsInfo.getArgs(assignInsn);
		BitSet args = new BitSet();
		for (RegisterArg arg : argsList) {
			args.set(arg.getRegNum());
		}
		boolean startCheck = false;
		for (InsnNode insn : assignBlock.getInstructions()) {
			if (startCheck && (!insn.canReorder() || ArgsInfo.usedArgAssign(insn, args))) {
				return false;
			}
			if (insn == assignInsn) {
				startCheck = true;
			}
		}
		Set<BlockNode> pathsBlocks = BlockUtils.getAllPathsBlocks(assignBlock, useBlock);
		pathsBlocks.remove(assignBlock);
		pathsBlocks.remove(useBlock);
		for (BlockNode block : pathsBlocks) {
			for (InsnNode insn : block.getInstructions()) {
				if (!insn.canReorder() || ArgsInfo.usedArgAssign(insn, args)) {
					return false;
				}
			}
		}
		for (InsnNode insn : useBlock.getInstructions()) {
			if (insn == useInsn) {
				return true;
			}
			if (!insn.canReorder() || ArgsInfo.usedArgAssign(insn, args)) {
				return false;
			}
		}
		throw new JadxRuntimeException("Can't process instruction move : " + assignBlock);
	}
}
