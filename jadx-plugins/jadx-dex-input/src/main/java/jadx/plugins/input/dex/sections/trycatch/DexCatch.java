package jadx.plugins.input.dex.sections.trycatch;

import jadx.api.plugins.input.data.ICatch;

public class DexCatch implements ICatch {
	private final int[] addr;
	private final String[] types;
	private final int allAddr;

	public DexCatch(int[] addr, String[] types, int allAddr) {
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
}
