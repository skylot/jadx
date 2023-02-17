package jadx.core.dex.instructions;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeWriter;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.utils.BlockUtils.getBlockByOffset;

public class SwitchInsn extends TargetInsnNode {
	private final int dataTarget;
	private final boolean packed; // type of switch insn, if true can contain filler keys
	@Nullable
	private SwitchData switchData;

	private int def; // next instruction

	private Object[] modifiedKeys;
	private BlockNode[] targetBlocks;
	private BlockNode defTargetBlock;

	public SwitchInsn(InsnArg arg, int dataTarget, boolean packed) {
		super(InsnType.SWITCH, 1);
		addArg(arg);
		this.dataTarget = dataTarget;
		this.packed = packed;
	}

	public boolean needData() {
		return this.switchData == null;
	}

	public void attachSwitchData(SwitchData data, int def) {
		this.switchData = data;
		this.def = def;
	}

	@Override
	public void initBlocks(BlockNode curBlock) {
		if (switchData == null) {
			throw new JadxRuntimeException("Switch data not yet attached");
		}
		List<BlockNode> successors = curBlock.getSuccessors();
		int[] targets = switchData.getTargets();
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
		if (!(obj instanceof SwitchInsn) || !super.isSame(obj)) {
			return false;
		}
		SwitchInsn other = (SwitchInsn) obj;
		return dataTarget == other.dataTarget
				&& packed == other.packed;
	}

	@Override
	public InsnNode copy() {
		SwitchInsn copy = new SwitchInsn(getArg(0), dataTarget, packed);
		copy.switchData = switchData;
		copy.def = def;
		copy.targetBlocks = targetBlocks;
		copy.defTargetBlock = defTargetBlock;
		return copyCommonParams(copy);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(baseString());
		if (switchData == null) {
			sb.append("no payload");
		} else {
			int size = switchData.getSize();
			int[] keys = switchData.getKeys();
			if (targetBlocks != null) {
				for (int i = 0; i < size; i++) {
					sb.append(ICodeWriter.NL);
					sb.append(" case ").append(keys[i]).append(": goto ").append(targetBlocks[i]);
				}
				if (def != -1) {
					sb.append(ICodeWriter.NL).append(" default: goto ").append(defTargetBlock);
				}
			} else {
				int[] targets = switchData.getTargets();
				for (int i = 0; i < size; i++) {
					sb.append(ICodeWriter.NL);
					sb.append(" case ").append(keys[i]).append(": goto ").append(InsnUtils.formatOffset(targets[i]));
				}
				if (def != -1) {
					sb.append(ICodeWriter.NL);
					sb.append(" default: goto ").append(InsnUtils.formatOffset(def));
				}
			}
		}
		appendAttributes(sb);
		return sb.toString();
	}

	public int getDataTarget() {
		return dataTarget;
	}

	public boolean isPacked() {
		return packed;
	}

	public int getDefaultCaseOffset() {
		return def;
	}

	@NotNull
	private SwitchData getSwitchData() {
		if (switchData == null) {
			throw new JadxRuntimeException("Switch data not yet attached");
		}
		return switchData;
	}

	public int[] getTargets() {
		return getSwitchData().getTargets();
	}

	public int[] getKeys() {
		return getSwitchData().getKeys();
	}

	public Object getKey(int i) {
		if (modifiedKeys != null) {
			return modifiedKeys[i];
		}
		return getSwitchData().getKeys()[i];
	}

	public void modifyKey(int i, Object newKey) {
		if (modifiedKeys == null) {
			int[] keys = getKeys();
			int caseCount = keys.length;
			Object[] newKeys = new Object[caseCount];
			for (int j = 0; j < caseCount; j++) {
				newKeys[j] = keys[j];
			}
			modifiedKeys = newKeys;
		}
		modifiedKeys[i] = newKey;
	}

	public BlockNode[] getTargetBlocks() {
		return targetBlocks;
	}

	public BlockNode getDefTargetBlock() {
		return defTargetBlock;
	}
}
