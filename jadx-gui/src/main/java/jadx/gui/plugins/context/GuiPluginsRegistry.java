package jadx.gui.plugins.context;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;

import jadx.gui.settings.data.ITabStatePersist;

public class GuiPluginsRegistry {

	private final List<Action> menuActions = new ArrayList<>();
	private final List<CodePopupAction> codePopupActions = new ArrayList<>();
	private final List<TreePopupMenuEntry> treePopupMenuEntries = new ArrayList<>();
	private final List<ITreeInputCategory> treeInputCategories = new ArrayList<>();
	private final List<ITabStatePersist> tabStatePersistAdapters = new ArrayList<>();

	public List<Action> getMenuActions() {
		return menuActions;
	}

	public List<CodePopupAction> getCodePopupActions() {
		return codePopupActions;
	}

	public List<TreePopupMenuEntry> getTreePopupMenuEntries() {
		return treePopupMenuEntries;
	}

	public List<ITreeInputCategory> getTreeInputCategories() {
		return treeInputCategories;
	}

	public List<ITabStatePersist> getTabStatePersistAdapters() {
		return tabStatePersistAdapters;
	}

	public void clear() {
		menuActions.clear();
		codePopupActions.clear();
		treePopupMenuEntries.clear();
		treeInputCategories.clear();
		tabStatePersistAdapters.clear();
	}
}
