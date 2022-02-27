package jadx.gui.device.debugger;

import jadx.gui.device.protocol.ADB;
import jadx.gui.ui.panel.JDebuggerPanel;

import java.io.IOException;

public class LogcatController {
	private ADB.Device adbDevice;
	private JDebuggerPanel debugPanel;

	public LogcatController(JDebuggerPanel debugPanel, ADB.Device adbDevice) throws IOException, InterruptedException {
		this.adbDevice = adbDevice;
		this.debugPanel = debugPanel;
		debugPanel.logcatUpdate(adbDevice.getLogcat());


	}


}
