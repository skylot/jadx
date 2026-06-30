package jadx.gui.ui.codearea.sync;

import jadx.gui.ui.codearea.SmaliArea;

public interface IToSmaliSyncStrategy {
	boolean syncTo(SmaliArea area);
}
