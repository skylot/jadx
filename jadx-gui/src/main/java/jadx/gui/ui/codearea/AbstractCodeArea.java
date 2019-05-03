package jadx.gui.ui.codearea;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.ContentPanel;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JumpPosition;

public abstract class AbstractCodeArea extends RSyntaxTextArea {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractCodeArea.class);

	protected final ContentPanel contentPanel;
	protected final JNode node;

	public AbstractCodeArea(ContentPanel contentPanel) {
		this.contentPanel = contentPanel;
		this.node = contentPanel.getNode();
	}

	/**
	 * Implement in this method the code that loads and sets the content to be displayed
	 */
	public abstract void load();

	public void loadSettings() {
		loadCommonSettings(contentPanel.getTabbedPane().getMainWindow(), this);
	}

	public void scrollToLine(int line) {
		int lineNum = line - 1;
		if (lineNum < 0) {
			lineNum = 0;
		}
		setCaretAtLine(lineNum);
		centerCurrentLine();
		forceCurrentLineHighlightRepaint();
	}

	private void setCaretAtLine(int line) {
		try {
			setCaretPosition(getLineStartOffset(line));
		} catch (BadLocationException e) {
			LOG.debug("Can't scroll to {}", line, e);
		}
	}

	public void centerCurrentLine() {
		JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
		if (viewport == null) {
			return;
		}
		try {
			Rectangle r = modelToView(getCaretPosition());
			if (r == null) {
				return;
			}
			int extentHeight = viewport.getExtentSize().height;
			Dimension viewSize = viewport.getViewSize();
			if (viewSize == null) {
				return;
			}
			int viewHeight = viewSize.height;

			int y = Math.max(0, r.y - extentHeight / 2);
			y = Math.min(y, viewHeight - extentHeight);

			viewport.setViewPosition(new Point(0, y));
		} catch (BadLocationException e) {
			LOG.debug("Can't center current line", e);
		}
	}

	public JumpPosition getCurrentPosition() {
		return new JumpPosition(node, getCaretLineNumber() + 1);
	}

	@Nullable
	Integer getSourceLine(int line) {
		return node.getSourceLine(line);
	}

	public ContentPanel getContentPanel() {
		return contentPanel;
	}

	public JNode getNode() {
		return node;
	}

	public static void loadCommonSettings(MainWindow mainWindow, RSyntaxTextArea area) {
		area.setAntiAliasingEnabled(true);
		mainWindow.getEditorTheme().apply(area);

		JadxSettings settings = mainWindow.getSettings();
		area.setFont(settings.getFont());
	}
}
