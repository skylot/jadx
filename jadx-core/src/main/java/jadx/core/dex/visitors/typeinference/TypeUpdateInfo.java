package jadx.core.dex.visitors.typeinference;

import java.util.IdentityHashMap;
import java.util.Map;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;

public class TypeUpdateInfo {

	private final Map<InsnArg, ArgType> updates = new IdentityHashMap<>();

	public void requestUpdate(InsnArg arg, ArgType changeType) {
		updates.put(arg, changeType);
	}

	public boolean isProcessed(InsnArg arg) {
		return updates.containsKey(arg);
	}

	public Map<InsnArg, ArgType> getUpdates() {
		return updates;
	}
}
