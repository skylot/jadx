package jadx.core.dex.visitors.typeinference;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jadx.api.JadxArgs;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxOverflowException;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class TypeUpdateInfo {
	private final MethodNode mth;
	private final TypeUpdateFlags flags;
	private final Map<InsnArg, TypeUpdateEntry> updateMap = new IdentityHashMap<>();
	private final int updatesLimitCount;
	private int updateSeq = 0;

	public TypeUpdateInfo(MethodNode mth, TypeUpdateFlags flags, JadxArgs args) {
		this.mth = mth;
		this.flags = flags;
		this.updatesLimitCount = mth.getInsnsCount() * args.getTypeUpdatesLimitCount();
	}

	public void requestUpdate(InsnArg arg, ArgType changeType) {
		TypeUpdateEntry prev = updateMap.put(arg, new TypeUpdateEntry(updateSeq++, arg, changeType));
		if (prev != null) {
			throw new JadxRuntimeException("Unexpected type update override for arg: " + arg
					+ " types: prev=" + prev.getType() + ", new=" + changeType
					+ ", insn: " + arg.getParentInsn());
		}
		if (updateSeq > updatesLimitCount) {
			throw new JadxOverflowException("Type inference error: updates count limit reached"
					+ " with updateSeq = " + updateSeq + ". Try increasing the type limit count on preferences.");
		}
	}

	public void rollbackUpdate(InsnArg arg) {
		TypeUpdateEntry removed = updateMap.remove(arg);
		if (removed != null) {
			int seq = removed.getSeq();
			updateMap.values().removeIf(upd -> upd.getSeq() > seq);
		}
	}

	public void applyUpdates() {
		updateMap.values().stream().sorted()
				.forEach(upd -> upd.getArg().setType(upd.getType()));
	}

	public boolean isProcessed(InsnArg arg) {
		return updateMap.containsKey(arg);
	}

	public boolean hasUpdateWithType(InsnArg arg, ArgType type) {
		TypeUpdateEntry updateEntry = updateMap.get(arg);
		if (updateEntry != null) {
			return updateEntry.getType().equals(type);
		}
		return false;
	}

	public ArgType getType(InsnArg arg) {
		TypeUpdateEntry updateEntry = updateMap.get(arg);
		if (updateEntry != null) {
			return updateEntry.getType();
		}
		return arg.getType();
	}

	public MethodNode getMth() {
		return mth;
	}

	public boolean isEmpty() {
		return updateMap.isEmpty();
	}

	public List<TypeUpdateEntry> getSortedUpdates() {
		return updateMap.values().stream().sorted().collect(Collectors.toList());
	}

	public TypeUpdateFlags getFlags() {
		return flags;
	}

	@Override
	public String toString() {
		return "TypeUpdateInfo{" + flags + ' ' + getSortedUpdates() + '}';
	}
}
