package jadx.gui.ui.codearea.sync;

/**
 * Accepts a code panel syncer for syncing code areas
 */
public interface CodePanelSyncee {
	public boolean sync(CodePanelSyncer syncer);
}
