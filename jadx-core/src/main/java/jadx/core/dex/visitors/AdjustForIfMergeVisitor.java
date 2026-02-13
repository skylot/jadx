package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.SpecialEdgeAttr;
import jadx.core.dex.attributes.nodes.SpecialEdgeAttr.SpecialEdgeType;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.regions.RegionMakerVisitor;
import jadx.core.dex.visitors.typeinference.FinishTypeInference;
import jadx.core.utils.BlockUtils;

@JadxVisitor(
		name = "AdjustForIfMergeVisitor",
		desc = "Move instructions between if blocks that can't be inlined but are safe to push through the if to allow the ifs to merge",
		runBefore = { RegionMakerVisitor.class },
		runAfter = { FinishTypeInference.class }
)
public class AdjustForIfMergeVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode() || mth.getBasicBlocks() == null) {
			return;
		}
		// Find candidates for adjustment by selecting blocks between two if statements
		List<BlockNode> blocks = mth.getBasicBlocks();

		for (BlockNode blk : blocks) {
			if (areSurroundingsCorrectShape(blk)) {
				BlockNode pred = blk.getPredecessors().get(0);
				BlockNode succ = blk.getCleanSuccessors().get(0);

				if (isSimpleIf(pred) && isSimpleIf(succ)) {
					List<InsnNode> movableInstructions = getMovableInstructions(blk, succ);

					if (!movableInstructions.isEmpty() && couldMerge(mth, pred, blk, succ)) {
						doMove(mth, blk, succ, movableInstructions);
					}
				}
			}
		}

	}

	private boolean areSurroundingsCorrectShape(BlockNode blk) {
		return (blk.getPredecessors().size() == 1 && blk.getCleanSuccessors().size() == 1);
	}

	private boolean isSimpleIf(BlockNode blk) {
		return blk.getInstructions().size() == 1 && blk.getInstructions().get(0).getType() == InsnType.IF;
	}

	private boolean couldMerge(MethodNode mth, BlockNode pred, BlockNode blk, BlockNode succ) {
		// we cannot merge if the edge from blk to succ is a back edge
		// there's a function in BlockUtils that purports to check if something is a back edge but it
		// doesn't so do it by hand here

		List<SpecialEdgeAttr> specialEdges = mth.getAll(AType.SPECIAL_EDGE);
		for (SpecialEdgeAttr edge : specialEdges) {
			if (edge.getStart() == blk && edge.getEnd() == succ && edge.getType() == SpecialEdgeType.BACK_EDGE) {
				mth.addDebugComment("Refusing to push insns through at block " + blk.toString() + " : edge to successor is a back edge.");
				return false;
			}
		}

		return true;
	}

	private List<InsnNode> getMovableInstructions(BlockNode blk, BlockNode succ) {
		// A 'movable instruction' is one that does not impact either codegen or the semantics of the
		// following block, so it can be pushed through into the new synthetics.

		// For now, we just look for nop moves along the same register such that the target variable is not
		// used in the succ block.

		List<InsnNode> movableInstructions = new ArrayList<>();
		for (InsnNode insn : blk.getInstructions()) {
			if (insn.getType() == InsnType.MOVE) {
				if (!(insn.getArg(0) instanceof RegisterArg)) {
					// could be a LiteralArg
					continue;
				}
				RegisterArg source = (RegisterArg) insn.getArg(0);
				RegisterArg target = insn.getResult();

				List<RegisterArg> uses = target.getSVar().getUseList();
				for (RegisterArg use : uses) {
					if (BlockUtils.blockContains(succ, use.getParentInsn())) {
						// the target is used inside the successor, so we can't cleanly do the assignment afterwards
						continue;
					}
				}

				// we don't want to just push everything through, e.g.
				// if (condition) { return; }
				// x = 123456
				// if (condition) { return; }
				// would be a less clean result if the assignment was pushed into the block of the 2nd if.

				if (source.getRegNum() == target.getRegNum()) {
					movableInstructions.add(insn);
				}
			}
		}

		return movableInstructions;
	}

	private void doMove(MethodNode mth, BlockNode target, BlockNode bottomIf, List<InsnNode> movableInstructions) {
		// Move instructions from the list out of blk and into new synthetics on each edge out of succ

		// preserving instruction ordering, although it's unlikely that it would ever matter here
		Collections.reverse(movableInstructions);
		for (InsnNode insn : movableInstructions) {
			target.getInstructions().remove(insn);
			for (BlockNode succ : bottomIf.getCleanSuccessors()) {
				succ.getInstructions().add(0, insn); // add at start

				if (succ.contains(AFlag.LOOP_START)) {
					// if we're merging into a loop condition, silence the warning when there's more than one
					// instruction in the loop header
					succ.add(AFlag.ALLOW_MULTIPLE_INSNS_LOOP_COND);
				}
			}
		}
	}

}
