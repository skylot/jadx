package jadx.gui.treemodel;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import jadx.gui.jobs.IBackgroundTask;

public abstract class JLoadableNode extends JNode {
	private static final long serialVersionUID = 5543590584166374958L;

	public abstract void loadNode();

	public abstract @Nullable IBackgroundTask getLoadTask();

	@Override
	public @Nullable JNode searchNode(Predicate<JNode> filter) {
		loadNode();
		return super.searchNode(filter);
	}

	@Override
	public @Nullable JNode removeNode(Predicate<JNode> filter) {
		loadNode();
		return super.removeNode(filter);
	}
}
