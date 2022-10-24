package jadx.gui.cache.usage;

import java.util.List;

final class FldUsageData {
	private final FldRef fldRef;
	private List<MthRef> usage;

	public FldUsageData(FldRef fldRef) {
		this.fldRef = fldRef;
	}

	public FldRef getFldRef() {
		return fldRef;
	}

	public List<MthRef> getUsage() {
		return usage;
	}

	public void setUsage(List<MthRef> usage) {
		this.usage = usage;
	}
}
