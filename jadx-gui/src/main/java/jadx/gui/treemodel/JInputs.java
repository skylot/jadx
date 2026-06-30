package jadx.gui.treemodel;

import java.nio.file.Path;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import jadx.core.utils.files.FileUtils;
import jadx.gui.settings.JadxProject;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.plugins.TreeInputsHelper;

public class JInputs extends JNode {
	private static final ImageIcon INPUTS_ICON = UiUtils.openSvgIcon("nodes/projectStructure");

	public JInputs(MainWindow mainWindow) {
		JadxProject project = mainWindow.getProject();
		List<Path> inputs = project.getFilePaths();
		List<Path> files = FileUtils.expandDirs(inputs);
		TreeInputsHelper inputsHelper = new TreeInputsHelper(mainWindow);
		inputsHelper.processInputs(files);
		add(new JInputFiles(inputsHelper.getSimpleFiles()));
		inputsHelper.getCustomNodes().forEach(this::add);
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return INPUTS_ICON;
	}

	@Override
	public String getID() {
		return "JInputs";
	}

	@Override
	public String makeString() {
		return NLS.str("tree.inputs_title");
	}
}
