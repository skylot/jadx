package jadx.gui.treemodel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import jadx.core.utils.files.FileUtils;
import jadx.gui.JadxWrapper;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class JInputs extends JNode {
	private static final ImageIcon INPUTS_ICON = UiUtils.openSvgIcon("nodes/projectStructure");

	public JInputs(JadxWrapper wrapper) {
		List<Path> inputs = wrapper.getProject().getFilePaths();
		List<Path> files = FileUtils.expandDirs(inputs);
		List<Path> scripts = new ArrayList<>();
		Iterator<Path> it = files.iterator();
		while (it.hasNext()) {
			Path file = it.next();
			if (file.getFileName().toString().endsWith(".jadx.kts")) {
				scripts.add(file);
				it.remove();
			}
		}

		add(new JInputFiles(files));
		add(new JInputScripts(scripts));
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
	public String makeString() {
		return NLS.str("tree.inputs_title");
	}
}
