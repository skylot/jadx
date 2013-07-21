package jadx.gui.treemodel;

import jadx.api.JavaPackage;
import jadx.gui.JadxWrapper;
import jadx.gui.Utils;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;

public class JRoot extends DefaultMutableTreeNode implements JNode {

	private static final ImageIcon ROOT_ICON = Utils.openIcon("java_model_obj");

	private final JadxWrapper wrapper;

	public JRoot(JadxWrapper wrapper) {
		this.wrapper = wrapper;

		for (JavaPackage pkg : wrapper.getPackages()) {
			add(new JPackage(pkg));
		}
	}

	@Override
	public Icon getIcon() {
		return ROOT_ICON;
	}

	@Override
	public String toString() {
		File file = wrapper.getOpenFile();
		return file != null ? file.getName() : "File not open";
	}
}
