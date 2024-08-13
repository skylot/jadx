package jadx.gui.ui.tab;

import java.util.List;

import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.utils.JumpManager;
import jadx.gui.utils.JumpPosition;

public class NavigationController implements ITabStatesListener {
	private final transient MainWindow mainWindow;

	private final transient JumpManager jumps = new JumpManager();

	public NavigationController(MainWindow mainWindow) {
		this.mainWindow = mainWindow;

		mainWindow.getTabsController().addListener(this);
	}

	public void saveJump(JumpPosition pos) {
		JumpPosition curPos = mainWindow.getTabbedPane().getCurrentPosition();
		if (curPos != null) {
			jumps.addPosition(curPos);
			jumps.addPosition(pos);
		}
	}

	public void navBack() {
		if (jumps.size() > 1) {
			jumps.updateCurPosition(mainWindow.getTabbedPane().getCurrentPosition());
		}
		JumpPosition pos = jumps.getPrev();
		if (pos != null) {
			mainWindow.getTabsController().codeJump(pos);
		}
	}

	public void navForward() {
		if (jumps.size() > 1) {
			jumps.updateCurPosition(mainWindow.getTabbedPane().getCurrentPosition());
		}
		JumpPosition pos = jumps.getNext();
		if (pos != null) {
			mainWindow.getTabsController().codeJump(pos);
		}
	}

	@Override
	public void onTabOpen(TabBlueprint blueprint) {

	}

	@Override
	public void onTabSelect(TabBlueprint blueprint) {

	}

	@Override
	public void onTabCodeJump(TabBlueprint blueprint, JumpPosition position) {

	}

	@Override
	public void onTabSmaliJump(TabBlueprint blueprint, int pos, boolean debugMode) {

	}

	@Override
	public void onTabClose(TabBlueprint blueprint) {

	}

	@Override
	public void onTabPositionFirst(TabBlueprint blueprint) {

	}

	@Override
	public void onTabPinChange(TabBlueprint blueprint) {

	}

	@Override
	public void onTabBookmarkChange(TabBlueprint blueprint) {

	}

	@Override
	public void onTabVisibilityChange(TabBlueprint blueprint) {

	}

	@Override
	public void onTabRestore(TabBlueprint blueprint, EditorViewState viewState) {

	}

	@Override
	public void onTabsRestoreDone() {

	}

	@Override
	public void onTabsReorder(List<TabBlueprint> blueprints) {

	}

	@Override
	public void onTabSave(TabBlueprint blueprint, EditorViewState viewState) {

	}

	public void reset() {
		jumps.reset();
	}

	public void dispose() {
		reset();
		mainWindow.getTabsController().removeListener(this);
	}
}
