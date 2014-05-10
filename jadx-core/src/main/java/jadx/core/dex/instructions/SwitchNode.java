package jadx.core.dex.instructions;

import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

import java.util.Arrays;

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
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SwitchNode) || !super.equals(obj)) {
			return false;
		}
		SwitchNode that = (SwitchNode) obj;
		return def == that.def
				&& Arrays.equals(keys, that.keys)
				&& Arrays.equals(targets, that.targets);
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + Arrays.hashCode(keys);
		result = 31 * result + Arrays.hashCode(targets);
		result = 31 * result + def;
		return result;
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
