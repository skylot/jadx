package jadx.dex.instructions;

import jadx.dex.instructions.args.InsnArg;
import jadx.dex.nodes.InsnNode;
import jadx.utils.InsnUtils;

import java.util.Arrays;

public class SwitchNode extends InsnNode {

	private final int[] keys;
	private final int[] targets;
	private final int def; // next instruction

	public SwitchNode(InsnArg arg, int[] keys, int[] targets, int def) {
		super(InsnType.SWITCH, 1);
		this.keys = keys;
		this.targets = targets;
		this.def = def;
		addArg(arg);
	}

	public int getCasesCount() {
		return keys.length;
	}

	public int[] getKeys() {
		return keys;
	}

	public int[] getTargets() {
		return targets;
	}

	public int getDefaultCaseOffset() {
		return def;
	}

	@Override
	public String toString() {
		StringBuilder targ = new StringBuilder();
		targ.append('[');
		for (int i = 0; i < targets.length; i++) {
			targ.append(InsnUtils.formatOffset(targets[i]));
			if (i < targets.length - 1)
				targ.append(", ");
		}
		targ.append(']');
		return super.toString() + " k:" + Arrays.toString(keys) + " t:" + targ;
	}
}
