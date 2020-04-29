package jadx.plugins.input.dex.sections.trycatch;

import jadx.api.plugins.input.data.ICatch;
import jadx.api.plugins.input.data.ITry;

public class DexTryData implements ITry {

	private final ICatch catchHandler;
	private final int startAddr;
	private final int insnsCount;

	public DexTryData(ICatch catchHandler, int startAddr, int insnsCount) {
		this.catchHandler = catchHandler;
		this.startAddr = startAddr;
		this.insnsCount = insnsCount;
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
	public int getInstructionCount() {
		return insnsCount;
	}
}
