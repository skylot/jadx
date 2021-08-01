package jadx.api.plugins.input.data.impl;

import jadx.api.plugins.input.data.ICatch;
import jadx.api.plugins.input.data.ITry;

public class TryData implements ITry {

	private final int startAddr;
	private final int endAddr;
	private final ICatch catchHandler;

	public TryData(int startAddr, int endAddr, ICatch catchHandler) {
		this.startAddr = startAddr;
		this.endAddr = endAddr;
		this.catchHandler = catchHandler;
	}

	@Override
	public ICatch getCatch() {
		return catchHandler;
	}

	@Override
	public int getStartAddress() {
		return startAddr;
	}

	@Override
	public int getEndAddress() {
		return endAddr;
	}

	@Override
	public String toString() {
		return "Try{" + startAddr + " - " + endAddr + ": " + catchHandler + '}';
	}
}
