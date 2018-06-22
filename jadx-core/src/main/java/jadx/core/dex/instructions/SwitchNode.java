package jadx.core.dex.instructions;

import java.util.Arrays;
import java.util.List;

import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

import static jadx.core.utils.BlockUtils.getBlockByOffset;

public class SwitchNode extends TargetInsnNode {

	private final Object[] keys;
	private final int[] targets;
	private final int def; // next instruction

	private BlockNode[] targetBlocks;
	private BlockNode defTargetBlock;

	public SwitchNode(InsnArg arg, Object[] keys, int[] targets, int def) {
		super(InsnType.SWITCH, 1);
		this.keys = keys;
		this.targets = targets;
		this.def = def;
		addArg(arg);
	}

	public int getCasesCount() {
		return keys.length;
	}

	public Object[] getKeys() {
		return keys;
	}

	public int[] getTargets() {
		return targets;
	}

	public int getDefaultCaseOffset() {
		return def;
	}

	public BlockNode[] getTargetBlocks() {
		return targetBlocks;
	}

	public BlockNode getDefTargetBlock() {
		return defTargetBlock;
	}

	@Override
	public void initBlocks(BlockNode curBlock) {
		List<BlockNode> successors = curBlock.getSuccessors();
		int len = targets.length;
		targetBlocks = new BlockNode[len];
		for (int i = 0; i < len; i++) {
			targetBlocks[i] = getBlockByOffset(targets[i], successors);
		}
		defTargetBlock = getBlockByOffset(def, successors);
	}

	@Override
	public boolean replaceTargetBlock(BlockNode origin, BlockNode replace) {
		int count = 0;
		int len = targetBlocks.length;
		for (int i = 0; i < len; i++) {
			if (targetBlocks[i] == origin) {
				targetBlocks[i] = replace;
				count++;
			}
		}
		if (defTargetBlock == origin) {
			defTargetBlock = replace;
			count++;
		}
		return count > 0;
	}

	@Override
	public boolean isSame(InsnNode obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SwitchNode) || !super.isSame(obj)) {
			return false;
		}
		SwitchNode other = (SwitchNode) obj;
		return def == other.def
				&& Arrays.equals(keys, other.keys)
				&& Arrays.equals(targets, other.targets);
	}

	@Override
	public String toString() {
		StringBuilder targ = new StringBuilder();
		targ.append('[');
		for (int i = 0; i < targets.length; i++) {
			targ.append(InsnUtils.formatOffset(targets[i]));
			if (i < targets.length - 1) {
				targ.append(", ");
			}
		}
		targ.append(']');
		return super.toString() + " k:" + Arrays.toString(keys) + " t:" + targ;
	}
}
