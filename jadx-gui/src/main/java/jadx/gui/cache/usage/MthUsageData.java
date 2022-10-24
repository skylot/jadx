package jadx.gui.cache.usage;

import java.util.List;

final class MthUsageData {
	private final MthRef mthRef;
	private List<MthRef> usage;

	public MthUsageData(MthRef mthRef) {
		this.mthRef = mthRef;
	}

	public MthRef getMthRef() {
		return mthRef;
	}

	public List<MthRef> getUsage() {
		return usage;
	}

	public void setUsage(List<MthRef> usage) {
		this.usage = usage;
	}
}
