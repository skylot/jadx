package jadx.gui.settings;

import com.beust.jcommander.Parameter;

import jadx.cli.JadxCLIArgs;
import jadx.cli.config.JadxConfigExclude;

public class JadxGUIArgs extends JadxCLIArgs {

	@JadxConfigExclude
	@Parameter(
			names = { "-sc", "--select-class" },
			description = "GUI: Open the selected class and show the decompiled code"
	)
	private String cmdSelectClass = null;

	public String getCmdSelectClass() {
		return cmdSelectClass;
	}

	public void setCmdSelectClass(String cmdSelectClass) {
		this.cmdSelectClass = cmdSelectClass;
	}
}
