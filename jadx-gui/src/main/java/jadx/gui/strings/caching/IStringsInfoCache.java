package jadx.gui.strings.caching;

import java.io.Closeable;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.ClassNode;
import jadx.gui.strings.SingleStringResult;

public interface IStringsInfoCache extends Closeable {
	void addResults(final ClassNode clsNode, final List<SingleStringResult> stringResult);

	void remove(final ClassNode clsNode);

	@Nullable
	List<SingleStringResult> getStrings(final ClassNode clsNode);

	boolean contains(final ClassNode clsNode);

	@NotNull
	Set<Integer> getContainedClasses();
}
