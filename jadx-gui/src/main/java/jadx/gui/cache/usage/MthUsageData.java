package jadx.gui.cache.usage;

import java.util.List;

import jadx.api.plugins.input.data.IMethodRef;

final class MthUsageData {
	private final MthRef mthRef;
	private List<MthRef> usage;
	private List<MthRef> uses;
	private List<IMethodRef> unresolvedUsage;
	private boolean callsSelf;

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

	public List<MthRef> getUses() {
		return uses;
	}

	public void setUses(List<MthRef> uses) {
		this.uses = uses;
	}

	public List<IMethodRef> getUnresolvedUsage() {
		return unresolvedUsage;
	}

	public void setUnresolvedUsage(List<IMethodRef> unresolvedUsage) {
		this.unresolvedUsage = unresolvedUsage;
	}

	public boolean callsSelf() {
		return callsSelf;
	}

	public void setCallsSelf(boolean callsSelf) {
		this.callsSelf = callsSelf;
	}
}
