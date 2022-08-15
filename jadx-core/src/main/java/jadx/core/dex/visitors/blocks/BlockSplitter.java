package jadx.core.dex.visitors.blocks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.JumpInfo;
import jadx.core.dex.attributes.nodes.TmpEdgeAttr;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.TargetInsnNode;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class BlockSplitter extends AbstractVisitor {

	// leave these instructions alone in block node
	private static final Set<InsnType> SEPARATE_INSNS = EnumSet.of(
			InsnType.RETURN,
			InsnType.IF,
			InsnType.SWITCH,
			InsnType.MONITOR_ENTER,
			InsnType.MONITOR_EXIT,
			InsnType.THROW,
			InsnType.MOVE_EXCEPTION);

	public static boolean isSeparate(InsnType insnType) {
		return SEPARATE_INSNS.contains(insnType);
	}

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		mth.initBasicBlocks();
		Map<Integer, BlockNode> blocksMap = splitBasicBlocks(mth);
		setupConnectionsFromJumps(mth, blocksMap);
		initBlocksInTargetNodes(mth);
		addTempConnectionsForExcHandlers(mth, blocksMap);

		expandMoveMulti(mth);
		removeJumpAttr(mth);
		removeInsns(mth);
		removeEmptyDetachedBlocks(mth);
		mth.getBasicBlocks().removeIf(BlockSplitter::removeEmptyBlock);
		setupExitConnections(mth);

		mth.unloadInsnArr();
	}

	private static Map<Integer, BlockNode> splitBasicBlocks(MethodNode mth) {
		BlockNode enterBlock = startNewBlock(mth, -1);
		enterBlock.add(AFlag.MTH_ENTER_BLOCK);
		mth.setEnterBlock(enterBlock);

		BlockNode exitBlock = startNewBlock(mth, -1);
		exitBlock.add(AFlag.MTH_EXIT_BLOCK);
		mth.setExitBlock(exitBlock);

		Map<Integer, BlockNode> blocksMap = new HashMap<>();
		BlockNode curBlock = enterBlock;
		InsnNode prevInsn = null;
		for (InsnNode insn : mth.getInstructions()) {
			if (insn == null) {
				continue;
			}
			if (insn.getType() == InsnType.NOP && insn.isAttrStorageEmpty()) {
				continue;
			}
			int insnOffset = insn.getOffset();
			if (prevInsn == null) {
				// first block after method enter block
				curBlock = connectNewBlock(mth, curBlock, insnOffset);
			} else {
				InsnType prevType = prevInsn.getType();
				switch (prevType) {
					case RETURN:
					case THROW:
					case GOTO:
					case IF:
					case SWITCH:
						// split without connect to next block
						curBlock = startNewBlock(mth, insnOffset);
						break;

					default:
						if (isSeparate(prevType)
								|| isSeparate(insn.getType())
								|| insn.contains(AFlag.TRY_ENTER)
								|| prevInsn.contains(AFlag.TRY_LEAVE)
								|| insn.contains(AType.EXC_HANDLER)
								|| isSplitByJump(prevInsn, insn)
								|| isDoWhile(blocksMap, curBlock, insn)) {
							curBlock = connectNewBlock(mth, curBlock, insnOffset);
						}
						break;
				}
			}
			blocksMap.put(insnOffset, curBlock);
			curBlock.getInstructions().add(insn);
			prevInsn = insn;
		}
		return blocksMap;
	}

	/**
	 * Init 'then' and 'else' blocks for 'if' instruction.
	 */
	private static void initBlocksInTargetNodes(MethodNode mth) {
		mth.getBasicBlocks().forEach(block -> {
			InsnNode lastInsn = BlockUtils.getLastInsn(block);
			if (lastInsn instanceof TargetInsnNode) {
				((TargetInsnNode) lastInsn).initBlocks(block);
			}
		});
	}

	static BlockNode connectNewBlock(MethodNode mth, BlockNode block, int offset) {
		BlockNode newBlock = startNewBlock(mth, offset);
		connect(block, newBlock);
		return newBlock;
	}

	static BlockNode startNewBlock(MethodNode mth, int offset) {
		List<BlockNode> blocks = mth.getBasicBlocks();
		BlockNode block = new BlockNode(mth.getNextBlockCId(), blocks.size(), offset);
		blocks.add(block);
		return block;
	}

	public static void connect(BlockNode from, BlockNode to) {
		if (!from.getSuccessors().contains(to)) {
			from.getSuccessors().add(to);
		}
		if (!to.getPredecessors().contains(from)) {
			to.getPredecessors().add(from);
		}
	}

	public static void removeConnection(BlockNode from, BlockNode to) {
		from.getSuccessors().remove(to);
		to.getPredecessors().remove(from);
	}

	public static void removePredecessors(BlockNode block) {
		for (BlockNode pred : block.getPredecessors()) {
			pred.getSuccessors().remove(block);
		}
		block.getPredecessors().clear();
	}

	public static void replaceConnection(BlockNode source, BlockNode oldDest, BlockNode newDest) {
		removeConnection(source, oldDest);
		connect(source, newDest);
		replaceTarget(source, oldDest, newDest);
	}

	static BlockNode insertBlockBetween(MethodNode mth, BlockNode source, BlockNode target) {
		BlockNode newBlock = startNewBlock(mth, target.getStartOffset());
		newBlock.add(AFlag.SYNTHETIC);
		removeConnection(source, target);
		connect(source, newBlock);
		connect(newBlock, target);
		replaceTarget(source, target, newBlock);
		source.updateCleanSuccessors();
		newBlock.updateCleanSuccessors();
		return newBlock;
	}

	static BlockNode blockSplitTop(MethodNode mth, BlockNode block) {
		BlockNode newBlock = startNewBlock(mth, block.getStartOffset());
		for (BlockNode pred : new ArrayList<>(block.getPredecessors())) {
			replaceConnection(pred, block, newBlock);
			pred.updateCleanSuccessors();
		}
		connect(newBlock, block);
		newBlock.updateCleanSuccessors();
		return newBlock;
	}

	static void copyBlockData(BlockNode from, BlockNode to) {
		List<InsnNode> toInsns = to.getInstructions();
		for (InsnNode insn : from.getInstructions()) {
			toInsns.add(insn.copyWithoutSsa());
		}
		to.copyAttributesFrom(from);
	}

	static void replaceTarget(BlockNode source, BlockNode oldTarget, BlockNode newTarget) {
		InsnNode lastInsn = BlockUtils.getLastInsn(source);
		if (lastInsn instanceof TargetInsnNode) {
			((TargetInsnNode) lastInsn).replaceTargetBlock(oldTarget, newTarget);
		}
	}

	private static void setupConnectionsFromJumps(MethodNode mth, Map<Integer, BlockNode> blocksMap) {
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				List<JumpInfo> jumps = insn.getAll(AType.JUMP);
				for (JumpInfo jump : jumps) {
					BlockNode srcBlock = getBlock(jump.getSrc(), blocksMap);
					BlockNode thisBlock = getBlock(jump.getDest(), blocksMap);
					connect(srcBlock, thisBlock);
				}
			}
		}
	}

	/**
	 * Connect exception handlers to the throw block.
	 * This temporary connection needed to build close to final dominators tree.
	 * Will be used and removed in {@code jadx.core.dex.visitors.blocks.BlockExceptionHandler}
	 */
	private static void addTempConnectionsForExcHandlers(MethodNode mth, Map<Integer, BlockNode> blocksMap) {
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				CatchAttr catchAttr = insn.get(AType.EXC_CATCH);
				if (catchAttr == null) {
					continue;
				}
				for (ExceptionHandler handler : catchAttr.getHandlers()) {
					BlockNode handlerBlock = getBlock(handler.getHandlerOffset(), blocksMap);
					if (!handlerBlock.contains(AType.TMP_EDGE)) {
						List<BlockNode> preds = block.getPredecessors();
						if (preds.isEmpty()) {
							throw new JadxRuntimeException("Unexpected missing predecessor for block: " + block);
						}
						BlockNode start = preds.size() == 1 ? preds.get(0) : block;
						if (!start.getSuccessors().contains(handlerBlock)) {
							connect(start, handlerBlock);
							handlerBlock.addAttr(new TmpEdgeAttr(start));
						}
					}
				}
			}
		}
	}

	private static void setupExitConnections(MethodNode mth) {
		BlockNode exitBlock = mth.getExitBlock();
		for (BlockNode block : mth.getBasicBlocks()) {
			if (block.getSuccessors().isEmpty() && block != exitBlock) {
				connect(block, exitBlock);
				if (BlockUtils.checkLastInsnType(block, InsnType.RETURN)) {
					block.add(AFlag.RETURN);
				}
			}
		}
	}

	private static boolean isSplitByJump(InsnNode prevInsn, InsnNode currentInsn) {
		List<JumpInfo> pJumps = prevInsn.getAll(AType.JUMP);
		for (JumpInfo jump : pJumps) {
			if (jump.getSrc() == prevInsn.getOffset()) {
				return true;
			}
		}
		List<JumpInfo> cJumps = currentInsn.getAll(AType.JUMP);
		for (JumpInfo jump : cJumps) {
			if (jump.getDest() == currentInsn.getOffset()) {
				return true;
			}
		}
		return false;
	}

	private static boolean isDoWhile(Map<Integer, BlockNode> blocksMap, BlockNode curBlock, InsnNode insn) {
		// split 'do-while' block (last instruction: 'if', target this block)
		if (insn.getType() != InsnType.IF) {
			return false;
		}
		IfNode ifs = (IfNode) insn;
		BlockNode targetBlock = blocksMap.get(ifs.getTarget());
		return targetBlock == curBlock;
	}

	private static BlockNode getBlock(int offset, Map<Integer, BlockNode> blocksMap) {
		BlockNode block = blocksMap.get(offset);
		if (block == null) {
			throw new JadxRuntimeException("Missing block: " + offset);
		}
		return block;
	}

	private static void expandMoveMulti(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			List<InsnNode> insnsList = block.getInstructions();
			int len = insnsList.size();
			for (int i = 0; i < len; i++) {
				InsnNode insn = insnsList.get(i);
				if (insn.getType() == InsnType.MOVE_MULTI) {
					int mvCount = insn.getArgsCount() / 2;
					for (int j = 0; j < mvCount; j++) {
						InsnNode mv = new InsnNode(InsnType.MOVE, 1);
						int startArg = j * 2;
						mv.setResult((RegisterArg) insn.getArg(startArg));
						mv.addArg(insn.getArg(startArg + 1));
						mv.copyAttributesFrom(insn);
						if (j == 0) {
							mv.setOffset(insn.getOffset());
							insnsList.set(i, mv);
						} else {
							insnsList.add(i + j, mv);
						}
					}
					i += mvCount - 1;
					len = insnsList.size();
				}
			}
		}
	}

	private static void removeJumpAttr(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				insn.remove(AType.JUMP);
			}
		}
	}

	private static void removeInsns(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			block.getInstructions().removeIf(insn -> {
				if (!insn.isAttrStorageEmpty()) {
					return false;
				}
				InsnType insnType = insn.getType();
				return insnType == InsnType.GOTO || insnType == InsnType.NOP;
			});
		}
	}

	public static void detachMarkedBlocks(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			if (block.contains(AFlag.REMOVE)) {
				detachBlock(block);
			}
		}
	}

	static boolean removeEmptyDetachedBlocks(MethodNode mth) {
		return mth.getBasicBlocks().removeIf(block -> block.getInstructions().isEmpty()
				&& block.getPredecessors().isEmpty()
				&& block.getSuccessors().isEmpty()
				&& !block.contains(AFlag.MTH_ENTER_BLOCK)
				&& !block.contains(AFlag.MTH_EXIT_BLOCK));
	}

	static boolean removeEmptyBlock(BlockNode block) {
		if (canRemoveBlock(block)) {
			if (block.getSuccessors().size() == 1) {
				BlockNode successor = block.getSuccessors().get(0);
				block.getPredecessors().forEach(pred -> {
					pred.getSuccessors().remove(block);
					BlockSplitter.connect(pred, successor);
					BlockSplitter.replaceTarget(pred, block, successor);
					pred.updateCleanSuccessors();
				});
				BlockSplitter.removeConnection(block, successor);
			} else {
				block.getPredecessors().forEach(pred -> {
					pred.getSuccessors().remove(block);
					pred.updateCleanSuccessors();
				});
			}
			block.add(AFlag.REMOVE);
			block.getSuccessors().clear();
			block.getPredecessors().clear();
			return true;
		}
		return false;
	}

	private static boolean canRemoveBlock(BlockNode block) {
		return block.getInstructions().isEmpty()
				&& block.isAttrStorageEmpty()
				&& block.getSuccessors().size() <= 1
				&& !block.getPredecessors().isEmpty()
				&& !block.contains(AFlag.MTH_ENTER_BLOCK)
				&& !block.contains(AFlag.MTH_EXIT_BLOCK)
				&& !block.getSuccessors().contains(block); // no self loop
	}

	static void collectSuccessors(BlockNode startBlock, BlockNode methodEnterBlock, Set<BlockNode> toRemove) {
		Deque<BlockNode> stack = new ArrayDeque<>();
		stack.add(startBlock);
		while (!stack.isEmpty()) {
			BlockNode block = stack.pop();
			if (!toRemove.contains(block)) {
				toRemove.add(block);
				for (BlockNode successor : block.getSuccessors()) {
					if (successor != methodEnterBlock && toRemove.containsAll(successor.getPredecessors())) {
						stack.push(successor);
					}
				}
			}
		}
	}

	static void detachBlock(BlockNode block) {
		for (BlockNode pred : block.getPredecessors()) {
			pred.getSuccessors().remove(block);
			pred.updateCleanSuccessors();
		}
		for (BlockNode successor : block.getSuccessors()) {
			successor.getPredecessors().remove(block);
		}
		block.add(AFlag.REMOVE);
		block.getInstructions().clear();
		block.getPredecessors().clear();
		block.getSuccessors().clear();
	}
}
