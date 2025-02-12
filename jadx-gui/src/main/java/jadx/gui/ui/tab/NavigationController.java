package jadx.gui.ui.tab;

import org.jetbrains.annotations.Nullable;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JumpManager;
import jadx.gui.utils.JumpPosition;

/**
 * TODO: Save jumps history into project file to restore after reload or reopen
 */
public class NavigationController implements ITabStatesListener {
	private final transient MainWindow mainWindow;

	private final transient JumpManager jumps = new JumpManager();

	public NavigationController(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		mainWindow.getTabsController().addListener(this);
	}

	public void navBack() {
		jump(jumps.getPrev());
	}

	public void navForward() {
		jump(jumps.getNext());
	}

	private void jump(@Nullable JumpPosition pos) {
		if (pos != null) {
			mainWindow.getTabsController().codeJump(pos);
		}
	}

	@Override
	public void onTabCodeJump(TabBlueprint blueprint, @Nullable JumpPosition prevPos, JumpPosition newPos) {
		if (newPos.equals(jumps.getCurrent())) {
			// ignore self-initiated jumps
			return;
		}
		jumps.addPosition(prevPos);
		jumps.addPosition(newPos);
	}

	@Override
	public void onTabSmaliJump(TabBlueprint blueprint, int pos, boolean debugMode) {
		// TODO: save smali jump
	}

	public void reset() {
		jumps.reset();
	}

	public void dispose() {
		reset();
		mainWindow.getTabsController().removeListener(this);
	}
}
