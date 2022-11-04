package jadx.gui.plugins.context;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.JNodePopupBuilder;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.ActionHandler;

public class GuiPluginsContext implements JadxGuiContext {
	private static final Logger LOG = LoggerFactory.getLogger(GuiPluginsContext.class);

	private final MainWindow mainWindow;

	private final List<CodePopupAction> codePopupActionList = new ArrayList<>();

	public GuiPluginsContext(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public void reset() {
		codePopupActionList.clear();
		JMenu pluginsMenu = mainWindow.getPluginsMenu();
		pluginsMenu.removeAll();
		pluginsMenu.setVisible(false);
	}

	@Override
	public void uiRun(Runnable runnable) {
		UiUtils.uiRun(runnable);
	}

	@Override
	public void addMenuAction(String name, Runnable action) {
		ActionHandler item = new ActionHandler(ev -> {
			try {
				mainWindow.getBackgroundExecutor().execute(name, action);
			} catch (Exception e) {
				LOG.error("Error running action for menu item: {}", name, e);
			}
		});
		item.setNameAndDesc(name);
		JMenu pluginsMenu = mainWindow.getPluginsMenu();
		pluginsMenu.add(item);
		pluginsMenu.setVisible(true);
	}

	@Override
	public void addPopupMenuAction(String name, @Nullable Function<ICodeNodeRef, Boolean> enabled,
			@Nullable String keyBinding, Consumer<ICodeNodeRef> action) {
		codePopupActionList.add(new CodePopupAction(name, enabled, keyBinding, action));
	}

	public void appendPopupMenus(CodeArea codeArea, JNodePopupBuilder popup) {
		if (codePopupActionList.isEmpty()) {
			return;
		}
		popup.addSeparator();
		for (CodePopupAction codePopupAction : codePopupActionList) {
			popup.add(codePopupAction.buildAction(codeArea));
		}
	}

	@Override
	public boolean registerGlobalKeyBinding(String id, String keyBinding, Runnable action) {
		KeyStroke keyStroke = KeyStroke.getKeyStroke(keyBinding);
		if (keyStroke == null) {
			throw new IllegalArgumentException("Failed to parse key binding: " + keyBinding);
		}
		JPanel mainPanel = (JPanel) mainWindow.getContentPane();
		Object prevBinding = mainPanel.getInputMap().get(keyStroke);
		if (prevBinding != null) {
			return false;
		}
		UiUtils.addKeyBinding(mainPanel, keyStroke, id, action);
		return true;
	}

	@Override
	public void copyToClipboard(String str) {
		UiUtils.copyToClipboard(str);
	}
}
