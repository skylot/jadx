package jadx.core.dex.visitors.typeinference;

import java.util.ArrayList;
import java.util.List;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxOverflowException;

public class TypeUpdateInfo {
	private final MethodNode mth;
	private final TypeUpdateFlags flags;
	private final List<TypeUpdateEntry> updates = new ArrayList<>();
	private final int updatesLimitCount;

	public TypeUpdateInfo(MethodNode mth, TypeUpdateFlags flags) {
		this.mth = mth;
		this.flags = flags;
		this.updatesLimitCount = mth.getInsnsCount() * 5; // maximum registers count to update at once
	}

	public void requestUpdate(InsnArg arg, ArgType changeType) {
		updates.add(new TypeUpdateEntry(arg, changeType));
	}

	public void applyUpdates() {
		for (TypeUpdateEntry updateEntry : updates) {
			InsnArg arg = updateEntry.getArg();
			arg.setType(updateEntry.getType());
		}
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

	public ArgType getType(InsnArg arg) {
		for (TypeUpdateEntry update : updates) {
			if (update.getArg() == arg) {
				return update.getType();
			}
		}
		return arg.getType();
	}

	public void rollbackUpdate(InsnArg arg) {
		updates.removeIf(updateEntry -> updateEntry.getArg() == arg);
	}

	public void checkUpdatesCount() {
		if (updates.size() > updatesLimitCount) {
			throw new JadxOverflowException("Type inference error: update tree size limit reached");
		}
	}

	public MethodNode getMth() {
		return mth;
	}

	public List<TypeUpdateEntry> getUpdates() {
		return updates;
	}

	public TypeUpdateFlags getFlags() {
		return flags;
	}

	@Override
	public String toString() {
		return "TypeUpdateInfo{" + flags + ", updates=" + updates + '}';
	}
}
