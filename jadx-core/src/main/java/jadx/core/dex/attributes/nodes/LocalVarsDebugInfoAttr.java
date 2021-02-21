package jadx.core.dex.attributes.nodes;

import java.util.List;

import jadx.api.ICodeWriter;
import jadx.api.plugins.input.data.ILocalVar;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.utils.Utils;

public class LocalVarsDebugInfoAttr implements IAttribute {
	private final List<ILocalVar> localVars;

	public LocalVarsDebugInfoAttr(List<ILocalVar> localVars) {
		this.localVars = localVars;
	}

	public List<ILocalVar> getLocalVars() {
		return localVars;
	}

	@Override
	public AType<LocalVarsDebugInfoAttr> getType() {
		return AType.LOCAL_VARS_DEBUG_INFO;
	}

	@Override
	public String toString() {
		return "Debug Info:" + ICodeWriter.NL + "  " + Utils.listToString(localVars, ICodeWriter.NL + "  ");
	}
}
