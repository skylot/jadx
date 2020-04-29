package jadx.plugins.input.dex.insns.payloads;

import jadx.api.plugins.input.insns.custom.ISwitchPayload;

public class DexSwitchPayload implements ISwitchPayload {

	private final int size;
	private final int[] keys;
	private final int[] targets;

	public DexSwitchPayload(int size, int[] keys, int[] targets) {
		this.size = size;
		this.keys = keys;
		this.targets = targets;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public int[] getKeys() {
		return keys;
	}

	@Override
	public int[] getTargets() {
		return targets;
	}
}
