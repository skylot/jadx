package jadx.gui.ui;

import jadx.core.dex.instructions.args.ArgType;
import jadx.gui.ui.JDebuggerPanel.ValueTreeNode;

public interface IDebugController {
	boolean startDebugger(JDebuggerPanel panel, String[] args);

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
