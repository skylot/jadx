package jadx.core.dex.visitors;

import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.regions.CheckRegions;
import jadx.core.dex.visitors.regions.DepthRegionTraversal;
import jadx.core.dex.visitors.regions.IRegionVisitor;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "FixInstructionsVisitor",
		desc = "Verify and fix instructions (args, casts, compiler inference)",
		runAfter = {
				SimplifyVisitor.class, // run STR_CONCAT fix after conversion from StringBuilder
				CheckRegions.class // work with Regions IR
		}
)
public class FixInstructionsVisitor extends AbstractVisitor implements IRegionVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		DepthRegionTraversal.traverse(mth, this);
	}

	@Override
	public void processBlock(MethodNode mth, IBlock container) {
		for (InsnNode insn : container.getInstructions()) {
			insn.visitInsns(this::fixInsn);
		}
	}

	private void fixInsn(InsnNode insn) {
		if (insn.getType() == InsnType.STR_CONCAT) {
			// check if we need to insert empty string arg to force '+' to use string concatenation
			if (forceStringConcat(insn)) {
				insn.getArgList().add(0, InsnArg.wrapInsnIntoArg(new ConstStringNode("")));
			}
		}
	}

	private static boolean forceStringConcat(InsnNode insn) {
		if (insn.getArgsCount() < 2) {
			return false;
		}
		return !insn.getArg(0).getType().equals(ArgType.STRING)
				&& !insn.getArg(1).getType().equals(ArgType.STRING);
	}

	@Override
	public boolean enterRegion(MethodNode mth, IRegion region) {
		return true;
	}

	@Override
	public void leaveRegion(MethodNode mth, IRegion region) {
	}
}
