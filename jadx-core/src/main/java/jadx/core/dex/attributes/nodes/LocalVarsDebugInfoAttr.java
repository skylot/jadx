package jadx.core.dex.attributes.nodes;

import java.util.List;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.visitors.debuginfo.LocalVar;
import jadx.core.utils.Utils;

public class LocalVarsDebugInfoAttr implements IAttribute {
	private final List<LocalVar> localVars;

	public LocalVarsDebugInfoAttr(List<LocalVar> localVars) {
		this.localVars = localVars;
	}

	public List<LocalVar> getLocalVars() {
		return localVars;
	}

	@Override
	public AType<LocalVarsDebugInfoAttr> getType() {
		return AType.LOCAL_VARS_DEBUG_INFO;
	}

	@Override
	public String toString() {
		return "Debug Info:\n  " + Utils.listToString(localVars, "\n  ");
	}
}
