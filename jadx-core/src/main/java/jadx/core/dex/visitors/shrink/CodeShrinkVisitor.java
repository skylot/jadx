package jadx.core.dex.visitors.shrink;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.ModVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnList;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

@JadxVisitor(
		name = "CodeShrinkVisitor",
		desc = "Inline variables to make code smaller",
		runAfter = { ModVisitor.class }
)
public class CodeShrinkVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) {
		shrinkMethod(mth);
	}

	public static void shrinkMethod(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			shrinkBlock(mth, block);
			simplifyMoveInsns(block);
		}
	}

	private static void shrinkBlock(MethodNode mth, BlockNode block) {
		if (block.getInstructions().isEmpty()) {
			return;
		}
		InsnList insnList = new InsnList(block.getInstructions());
		int insnCount = insnList.size();
		List<ArgsInfo> argsList = new ArrayList<>(insnCount);
		for (int i = 0; i < insnCount; i++) {
			argsList.add(new ArgsInfo(insnList.get(i), argsList, i));
		}
		List<WrapInfo> wrapList = new ArrayList<>();
		for (ArgsInfo argsInfo : argsList) {
			List<RegisterArg> args = argsInfo.getArgs();
			if (!args.isEmpty()) {
				ListIterator<RegisterArg> it = args.listIterator(args.size());
				while (it.hasPrevious()) {
					RegisterArg arg = it.previous();
					checkInline(mth, block, insnList, wrapList, argsInfo, arg);
				}
			}
		}
		if (!wrapList.isEmpty()) {
			for (WrapInfo wrapInfo : wrapList) {
				inline(mth, wrapInfo.getArg(), wrapInfo.getInsn(), block);
			}
		}
	}

	private static void checkInline(MethodNode mth, BlockNode block, InsnList insnList,
			List<WrapInfo> wrapList, ArgsInfo argsInfo, RegisterArg arg) {
		if (arg.contains(AFlag.DONT_INLINE)) {
			return;
		}
		SSAVar sVar = arg.getSVar();
		if (sVar == null || sVar.getAssign().contains(AFlag.DONT_INLINE)) {
			return;
		}
		InsnNode assignInsn = sVar.getAssign().getParentInsn();
		if (assignInsn == null
				|| assignInsn.contains(AFlag.DONT_INLINE)
				|| assignInsn.contains(AFlag.WRAPPED)) {
			return;
		}
		// allow inline only one use arg
		boolean assignInline = assignInsn.contains(AFlag.FORCE_ASSIGN_INLINE);
		if (!assignInline && sVar.getVariableUseCount() != 1) {
			return;
		}
		List<RegisterArg> useList = sVar.getUseList();
		if (!useList.isEmpty()) {
			RegisterArg useArg = useList.get(0);
			InsnNode parentInsn = useArg.getParentInsn();
			if (parentInsn != null && parentInsn.contains(AFlag.DONT_GENERATE)) {
				return;
			}
			if (!assignInline && useArg.contains(AFlag.DONT_INLINE_CONST)) {
				return;
			}
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
					&& canMoveBetweenBlocks(mth, assignInsn, assignBlock, block, argsInfo.getInsn())) {
				if (assignInline) {
					assignInline(mth, arg, assignInsn, assignBlock);
				} else {
					inline(mth, arg, assignInsn, assignBlock);
				}
			}
		}
	}

	private static boolean assignInline(MethodNode mth, RegisterArg arg, InsnNode assignInsn, BlockNode assignBlock) {
		RegisterArg useArg = arg.getSVar().getUseList().get(0);
		InsnNode useInsn = useArg.getParentInsn();
		if (useInsn == null || useInsn.contains(AFlag.DONT_GENERATE)) {
			return false;
		}
		if (!InsnRemover.removeWithoutUnbind(mth, assignBlock, assignInsn)) {
			return false;
		}
		InsnArg replaceArg = InsnArg.wrapInsnIntoArg(assignInsn);
		useInsn.replaceArg(useArg, replaceArg);
		return true;
	}

	private static boolean inline(MethodNode mth, RegisterArg arg, InsnNode insn, BlockNode block) {
		if (insn.contains(AFlag.FORCE_ASSIGN_INLINE)) {
			return assignInline(mth, arg, insn, block);
		}
		// just move instruction into arg, don't unbind/copy/duplicate
		InsnArg wrappedArg = arg.wrapInstruction(mth, insn, false);
		boolean replaced = wrappedArg != null;
		if (replaced) {
			processCodeComment(insn, arg.getParentInsn());
			InsnRemover.removeWithoutUnbind(mth, block, insn);
		}
		return replaced;
	}

	private static void processCodeComment(InsnNode insn, @Nullable InsnNode parentInsn) {
		if (parentInsn == null) {
			return;
		}
		if (parentInsn.getType() == InsnType.RETURN) {
			parentInsn.setSourceLine(insn.getSourceLine());
			if (parentInsn.contains(AFlag.SYNTHETIC)) {
				parentInsn.setOffset(insn.getOffset());
				parentInsn.rewriteAttributeFrom(insn, AType.CODE_COMMENTS);
				return;
			}
		}
		parentInsn.copyAttributeFrom(insn, AType.CODE_COMMENTS);
	}

	private static boolean canMoveBetweenBlocks(MethodNode mth, InsnNode assignInsn, BlockNode assignBlock,
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
			if (block.contains(AFlag.DONT_GENERATE)) {
				if (BlockUtils.checkLastInsnType(block, InsnType.MONITOR_EXIT)) {
					if (RegionUtils.isBlocksInSameRegion(mth, assignBlock, useBlock)) {
						// allow move inside same synchronized region
					} else {
						// don't move from synchronized block
						return false;
					}
				}
				// skip checks for not generated blocks
				continue;
			}
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

	private static void simplifyMoveInsns(BlockNode block) {
		List<InsnNode> insns = block.getInstructions();
		int size = insns.size();
		for (int i = 0; i < size; i++) {
			InsnNode insn = insns.get(i);
			if (insn.getType() == InsnType.MOVE) {
				// replace 'move' with wrapped insn
				InsnArg arg = insn.getArg(0);
				if (arg.isInsnWrap()) {
					InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
					wrapInsn.setResult(insn.getResult());
					wrapInsn.copyAttributesFrom(insn);
					wrapInsn.setOffset(insn.getOffset());
					wrapInsn.remove(AFlag.WRAPPED);
					block.getInstructions().set(i, wrapInsn);
				}
			}
		}
	}
}
