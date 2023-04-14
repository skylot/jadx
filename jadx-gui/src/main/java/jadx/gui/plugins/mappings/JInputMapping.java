package jadx.gui.plugins.mappings;

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
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JEditableNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.codearea.CodeContentPanel;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.SimpleMenuItem;

public class JInputMapping extends JEditableNode {
	private static final Logger LOG = LoggerFactory.getLogger(JInputMapping.class);

	private static final ImageIcon MAPPING_ICON = UiUtils.openSvgIcon("nodes/abbreviatePackageNames");

	private final Path mappingPath;
	private final String name;

	public JInputMapping(Path mappingPath) {
		this.mappingPath = mappingPath;
		this.name = mappingPath.getFileName().toString();
	}

	@Override
	public ContentPanel getContentPanel(TabbedPane tabbedPane) {
		return new CodeContentPanel(tabbedPane, this);
	}

	@Override
	public @NotNull ICodeInfo getCodeInfo() {
		try {
			return new SimpleCodeInfo(FileUtils.readFile(mappingPath));
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to read mapping file: " + mappingPath.toAbsolutePath(), e);
		}
	}

	@Override
	public void save(String newContent) {
		try {
			FileUtils.writeFile(mappingPath, newContent);
			LOG.debug("Mapping saved: {}", mappingPath.toAbsolutePath());
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to write mapping file: " + mappingPath.toAbsolutePath(), e);
		}
	}

	@Override
	public JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(new SimpleMenuItem(NLS.str("popup.remove"),
				() -> mainWindow.getRenameMappings().closeMappingsAndRemoveFromProject()));
		return menu;
	}

	@Override
	public String getSyntaxName() {
		return SyntaxConstants.SYNTAX_STYLE_NONE;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return MAPPING_ICON;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String makeString() {
		return name;
	}

	@Override
	public String getTooltip() {
		return mappingPath.normalize().toAbsolutePath().toString();
	}
}
