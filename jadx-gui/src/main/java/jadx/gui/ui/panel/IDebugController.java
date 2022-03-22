package jadx.gui.ui.panel;

import jadx.core.dex.instructions.args.ArgType;
import jadx.gui.ui.panel.JDebuggerPanel.ValueTreeNode;

public interface IDebugController {
	boolean startDebugger(JDebuggerPanel debuggerPanel, String adbHost, int adbPort, int androidVer);

	boolean run();

	boolean stepOver();

	boolean stepInto();

	boolean stepOut();

	boolean pause();

	boolean stop();

	boolean exit();

	boolean isSuspended();

	boolean isDebugging();

	boolean modifyRegValue(ValueTreeNode node, ArgType type, Object val);

	String getProcessName();

	void setStateListener(StateListener l);

	interface StateListener {
		void onStateChanged(boolean suspended, boolean stopped);
	}
}
