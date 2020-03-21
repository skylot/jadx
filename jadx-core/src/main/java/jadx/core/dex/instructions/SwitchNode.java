package jadx.core.dex.instructions;

import java.util.Arrays;
import java.util.List;

import jadx.core.codegen.CodeWriter;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

import static jadx.core.utils.BlockUtils.getBlockByOffset;

public class SwitchNode extends TargetInsnNode {

	private final Object[] keys;
	private final int[] targets;
	private final int def; // next instruction
	private final boolean packed; // type of switch insn, if true can contain filler keys

	private BlockNode[] targetBlocks;
	private BlockNode defTargetBlock;

	public SwitchNode(InsnArg arg, Object[] keys, int[] targets, int def, boolean packed) {
		this(keys, targets, def, packed);
		addArg(arg);
	}

	private SwitchNode(Object[] keys, int[] targets, int def, boolean packed) {
		super(InsnType.SWITCH, 1);
		this.keys = keys;
		this.targets = targets;
		this.def = def;
		this.packed = packed;
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

	public boolean isPacked() {
		return packed;
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
		if (targetBlocks == null) {
			return false;
		}
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
	public InsnNode copy() {
		SwitchNode copy = new SwitchNode(keys, targets, def, packed);
		copy.targetBlocks = targetBlocks;
		copy.defTargetBlock = defTargetBlock;
		return copyCommonParams(copy);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		if (targetBlocks == null) {
			for (int i = 0; i < keys.length; i++) {
				sb.append(CodeWriter.NL);
				sb.append("  case ").append(keys[i]).append(": goto ").append(InsnUtils.formatOffset(targets[i]));
			}
			if (def != -1) {
				sb.append(CodeWriter.NL);
				sb.append("  default: goto ").append(InsnUtils.formatOffset(def));
			}
		} else {
			for (int i = 0; i < keys.length; i++) {
				sb.append(CodeWriter.NL);
				sb.append("  case ").append(keys[i]).append(": goto ").append(targetBlocks[i]);
			}
			if (def != -1) {
				sb.append(CodeWriter.NL).append("  default: goto ").append(defTargetBlock);
			}
		}
		return sb.toString();
	}
}
