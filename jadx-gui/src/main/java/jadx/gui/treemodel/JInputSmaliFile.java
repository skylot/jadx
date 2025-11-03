package jadx.gui.treemodel;

import java.nio.file.Path;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.impl.SimpleCodeInfo;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.CodeContentPanel;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.Icons;

public class JInputSmaliFile extends JEditableNode {
	private static final Logger LOG = LoggerFactory.getLogger(JInputSmaliFile.class);

	private final Path filePath;

	public JInputSmaliFile(Path filePath) {
		this.filePath = Objects.requireNonNull(filePath);
	}

	@Override
	public JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		return JInputFile.buildInputFilePopupMenu(mainWindow, filePath);
	}

	@Override
	public boolean hasContent() {
		return true;
	}

	@Override
	public @Nullable ContentPanel getContentPanel(TabbedPane tabbedPane) {
		return new CodeContentPanel(tabbedPane, this);
	}

	@Override
	public String getSyntaxName() {
		return AbstractCodeArea.SYNTAX_STYLE_SMALI;
	}

	@Override
	public ICodeInfo getCodeInfo() {
		try {
			return new SimpleCodeInfo(FileUtils.readFile(filePath));
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to read file: " + filePath.toAbsolutePath(), e);
		}
	}

	@Override
	public void save(String newContent) {
		try {
			FileUtils.writeFile(filePath, newContent);
			LOG.debug("File saved: {}", filePath.toAbsolutePath());
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to write file: " + filePath.toAbsolutePath(), e);
		}
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return Icons.FILE;
	}

	@Override
	public String makeString() {
		return filePath.getFileName().toString();
	}

	@Override
	public String getTooltip() {
		return filePath.normalize().toAbsolutePath().toString();
	}

	@Override
	public int hashCode() {
		return filePath.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		return ((JInputSmaliFile) o).filePath.equals(filePath);
	}
}
