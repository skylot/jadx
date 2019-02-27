package jadx.core.dex.visitors.typeinference;

import java.util.ArrayList;
import java.util.List;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;

public class TypeUpdateInfo {
	private final List<TypeUpdateEntry> updates = new ArrayList<>();

	public void requestUpdate(InsnArg arg, ArgType changeType) {
		updates.add(new TypeUpdateEntry(arg, changeType));
	}

	public boolean isProcessed(InsnArg arg) {
		if (updates.isEmpty()) {
			return false;
		}
		for (TypeUpdateEntry entry : updates) {
			if (entry.getArg() == arg) {
				return true;
			}
		}
		return false;
	}

	public void rollbackUpdate(InsnArg arg) {
		updates.removeIf(updateEntry -> updateEntry.getArg() == arg);
	}

	public List<TypeUpdateEntry> getUpdates() {
		return updates;
	}
}
