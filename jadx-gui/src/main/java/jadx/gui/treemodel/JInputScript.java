package jadx.gui.treemodel;

import java.nio.file.Path;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.impl.SimpleCodeInfo;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.gui.plugins.script.ScriptContentPanel;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.SimpleMenuItem;

public class JInputScript extends JEditableNode {
	private static final Logger LOG = LoggerFactory.getLogger(JInputScript.class);

	private static final ImageIcon SCRIPT_ICON = UiUtils.openSvgIcon("nodes/kotlin_script");

	private final Path scriptPath;
	private final String name;

	public JInputScript(Path scriptPath) {
		this.scriptPath = scriptPath;
		this.name = scriptPath.getFileName().toString().replace(".jadx.kts", "");
	}

	@Override
	public ContentPanel getContentPanel(TabbedPane tabbedPane) {
		return new ScriptContentPanel(tabbedPane, this);
	}

	@Override
	public @NotNull ICodeInfo getCodeInfo() {
		try {
			return new SimpleCodeInfo(FileUtils.readFile(scriptPath));
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to read script file: " + scriptPath.toAbsolutePath(), e);
		}
	}

	@Override
	public void save(String newContent) {
		try {
			FileUtils.writeFile(scriptPath, newContent);
			LOG.debug("Script saved: {}", scriptPath.toAbsolutePath());
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to write script file: " + scriptPath.toAbsolutePath(), e);
		}
	}

	@Override
	public JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(new SimpleMenuItem(NLS.str("popup.add_scripts"), mainWindow::addFiles));
		menu.add(new SimpleMenuItem(NLS.str("popup.new_script"), mainWindow::addNewScript));
		menu.add(new SimpleMenuItem(NLS.str("popup.remove"), () -> mainWindow.removeInput(scriptPath)));
		return menu;
	}

	@Override
	public String getSyntaxName() {
		return SyntaxConstants.SYNTAX_STYLE_KOTLIN;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return SCRIPT_ICON;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String makeString() {
		return name;
	}
}
