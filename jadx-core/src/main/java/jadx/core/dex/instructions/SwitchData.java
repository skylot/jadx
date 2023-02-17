package jadx.core.dex.instructions;

import jadx.api.plugins.input.insns.custom.ISwitchPayload;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.InsnUtils;

public class SwitchData extends InsnNode {
	private final int size;
	private final int[] keys;
	private final int[] targets;

	public SwitchData(ISwitchPayload payload) {
		super(InsnType.SWITCH_DATA, 0);
		this.size = payload.getSize();
		this.keys = payload.getKeys();
		this.targets = payload.getTargets();
	}

	public void fixTargets(int switchOffset) {
		int size = this.size;
		int[] targets = this.targets;
		for (int i = 0; i < size; i++) {
			targets[i] += switchOffset;
		}
	}

	public int getSize() {
		return size;
	}

	public int[] getKeys() {
		return keys;
	}

	public int[] getTargets() {
		return targets;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("switch-data {");
		for (int i = 0; i < size; i++) {
			sb.append(keys[i]).append("->").append(InsnUtils.formatOffset(targets[i])).append(", ");
		}
		sb.append('}');
		appendAttributes(sb);
		return sb.toString();
	}
}
