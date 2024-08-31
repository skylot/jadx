package jadx.gui.treemodel;

import jadx.gui.jobs.IBackgroundTask;

public abstract class JLoadableNode extends JNode {
	private static final long serialVersionUID = 5543590584166374958L;

	public abstract void loadNode();

	public abstract IBackgroundTask getLoadTask();
}
