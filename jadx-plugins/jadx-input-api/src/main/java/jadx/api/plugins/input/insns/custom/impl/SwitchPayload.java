package jadx.api.plugins.input.insns.custom.impl;

import jadx.api.plugins.input.insns.custom.ISwitchPayload;

public class SwitchPayload implements ISwitchPayload {

	private final int size;
	private final int[] keys;
	private final int[] targets;

	public SwitchPayload(int size, int[] keys, int[] targets) {
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
