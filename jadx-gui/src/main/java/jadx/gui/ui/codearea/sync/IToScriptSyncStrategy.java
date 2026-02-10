package jadx.gui.ui.codearea.sync;

import jadx.gui.plugins.script.ScriptCodeArea;

public interface IToScriptSyncStrategy {
	boolean syncTo(ScriptCodeArea area);
}
