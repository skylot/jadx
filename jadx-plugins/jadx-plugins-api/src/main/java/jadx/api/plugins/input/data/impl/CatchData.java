package jadx.api.plugins.input.data.impl;

import jadx.api.plugins.input.data.ICatch;

public class CatchData implements ICatch {
	private final int[] addr;
	private final String[] types;
	private final int allAddr;

	public CatchData(int[] addr, String[] types, int allAddr) {
		this.addr = addr;
		this.types = types;
		this.allAddr = allAddr;
	}

	@Override
	public int[] getAddresses() {
		return addr;
	}

	@Override
	public String[] getTypes() {
		return types;
	}

	@Override
	public int getCatchAllAddress() {
		return allAddr;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Catch:");
		int size = types.length;
		for (int i = 0; i < size; i++) {
			sb.append(' ').append(types[i]).append("->").append(addr[i]);
		}
		if (allAddr != -1) {
			sb.append(" all->").append(allAddr);
		}
		return sb.toString();
	}
}
