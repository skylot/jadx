package jadx.core.dex.visitors.regions;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.MethodNode;

public interface IRegionPartialVisitor<R> {
	/**
	 * Visit all containers in region until stopped
	 *
	 * @return non-null value to stop visiting
	 */
	@Nullable
	R visit(MethodNode mth, IContainer container);
}
