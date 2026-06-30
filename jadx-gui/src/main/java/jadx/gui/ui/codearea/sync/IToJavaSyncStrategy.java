package jadx.gui.ui.codearea.sync;

import jadx.gui.ui.codearea.CodeArea;

public interface IToJavaSyncStrategy {
	boolean syncTo(CodeArea area);
}
