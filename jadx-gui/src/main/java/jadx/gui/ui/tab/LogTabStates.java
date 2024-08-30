package jadx.gui.ui.tab;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.utils.JumpPosition;

/**
 * Utility class to log events from TabsController by implementing ITabStatesListener.
 */
public class LogTabStates implements ITabStatesListener {
	private static final Logger LOG = LoggerFactory.getLogger(LogTabStates.class);

	@Override
	public void onTabBookmarkChange(TabBlueprint blueprint) {
		LOG.debug("onTabBookmarkChange: blueprint={}", blueprint);
	}

	@Override
	public void onTabClose(TabBlueprint blueprint) {
		LOG.debug("onTabClose: blueprint={}", blueprint);
	}

	@Override
	public void onTabCodeJump(TabBlueprint blueprint, JumpPosition position) {
		LOG.debug("onTabCodeJump: blueprint={}, position={}", blueprint, position);
	}

	@Override
	public void onTabOpen(TabBlueprint blueprint) {
		LOG.debug("onTabOpen: blueprint={}", blueprint);
	}

	@Override
	public void onTabPinChange(TabBlueprint blueprint) {
		LOG.debug("onTabPinChange: blueprint={}", blueprint);
	}

	@Override
	public void onTabPositionFirst(TabBlueprint blueprint) {
		LOG.debug("onTabPositionFirst: blueprint={}", blueprint);
	}

	@Override
	public void onTabRestore(TabBlueprint blueprint, EditorViewState viewState) {
		LOG.debug("onTabRestore: blueprint={}, viewState={}", blueprint, viewState);
	}

	@Override
	public void onTabSave(TabBlueprint blueprint, EditorViewState viewState) {
		LOG.debug("onTabSave: blueprint={}, viewState={}", blueprint, viewState);
	}

	@Override
	public void onTabSelect(TabBlueprint blueprint) {
		LOG.debug("onTabSelect: blueprint={}", blueprint);
	}

	@Override
	public void onTabSmaliJump(TabBlueprint blueprint, int pos, boolean debugMode) {
		LOG.debug("onTabSmaliJump: blueprint={}, pos={}, debugMode={}", blueprint, pos, debugMode);
	}

	@Override
	public void onTabsReorder(List<TabBlueprint> blueprints) {
		LOG.debug("onTabsReorder: blueprints={}", blueprints);
	}

	@Override
	public void onTabsRestoreDone() {
		LOG.debug("onTabsRestoreDone");
	}

	@Override
	public void onTabVisibilityChange(TabBlueprint blueprint) {
		LOG.debug("onTabVisibilityChange: blueprint={}", blueprint);
	}
}
