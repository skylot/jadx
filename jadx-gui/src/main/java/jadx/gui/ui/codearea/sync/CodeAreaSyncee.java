package jadx.gui.ui.codearea.sync;

/**
 * Accepts syncer for syncing code areas
 */
public interface CodeAreaSyncee {
	boolean sync(CodeAreaSyncer syncer);
}
