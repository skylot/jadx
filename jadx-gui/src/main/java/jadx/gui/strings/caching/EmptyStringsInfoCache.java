package jadx.gui.strings.caching;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.ClassNode;
import jadx.gui.strings.SingleStringResult;

public final class EmptyStringsInfoCache implements IStringsInfoCache {

	@Override
	public final void close() throws IOException {
		// do nothing
	}

	@Override
	public final void addResults(final ClassNode clsNode, final List<SingleStringResult> stringResult) {
		// do nothing
	}

	@Override
	public final void remove(ClassNode clsNode) {
		// do nothing
	}

	@Override
	public final @Nullable List<SingleStringResult> getStrings(final ClassNode clsNode) {
		return null;
	}

	@Override
	public final boolean contains(final ClassNode clsNode) {
		return false;
	}

	@Override
	public @NotNull Set<Integer> getContainedClasses() {
		return Set.of();
	}
}
