package jadx.core.dex.instructions;

import java.util.Arrays;

import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

public class SwitchNode extends InsnNode {

	private final Object[] keys;
	private final int[] targets;
	private final int def; // next instruction

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
