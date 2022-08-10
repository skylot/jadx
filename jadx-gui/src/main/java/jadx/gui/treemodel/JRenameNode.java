package jadx.gui.treemodel;

import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import jadx.api.JavaNode;
import jadx.api.data.ICodeRename;
import jadx.gui.ui.MainWindow;

public interface JRenameNode {

	String getTitle();

	String getName();

	Icon getIcon();

	boolean canRename();

	default JRenameNode replace() {
		return this;
	}

	ICodeRename buildCodeRename(String newName, Set<ICodeRename> renames);

	boolean isValidName(String newName);

	void removeAlias();

	void addUpdateNodes(List<JavaNode> toUpdate);

	void reload(MainWindow mainWindow);
}
