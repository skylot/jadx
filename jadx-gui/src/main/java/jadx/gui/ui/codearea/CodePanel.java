package jadx.gui.ui.codearea;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JPopupMenu.Separator;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;

import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.core.utils.StringUtils;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.LineNumbersMode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.dialog.SearchDialog;
import jadx.gui.utils.CaretPositionFix;
import jadx.gui.utils.DefaultPopupMenuListener;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

/**
 * A panel combining a {@link SearchBar and a scollable {@link CodeArea}
 */
public class CodePanel extends JPanel {
	private static final Logger LOG = LoggerFactory.getLogger(CodePanel.class);
	private static final long serialVersionUID = 1117721869391885865L;

	private final SearchBar searchBar;
	private final AbstractCodeArea codeArea;
	private final JScrollPane codeScrollPane;

	public CodePanel(AbstractCodeArea codeArea) {
		this.codeArea = codeArea;
		this.searchBar = new SearchBar(codeArea);
		this.codeScrollPane = buildCodeScrollPane(codeArea);

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
		add(searchBar, BorderLayout.NORTH);
		add(codeScrollPane, BorderLayout.CENTER);

		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F, UiUtils.ctrlButton());
		UiUtils.addKeyBinding(codeArea, key, "SearchAction", new AbstractAction() {
			private static final long serialVersionUID = 71338030532869694L;

			@Override
			public void actionPerformed(ActionEvent e) {
				searchBar.toggle();
			}
		});
		JMenuItem searchItem = new JMenuItem();
		JMenuItem globalSearchItem = new JMenuItem();
		AbstractAction searchAction = new AbstractAction(NLS.str("popup.search", "")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				searchBar.toggle();
			}
		};
		AbstractAction globalSearchAction = new AbstractAction(NLS.str("popup.search_global", "")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				MainWindow mainWindow = codeArea.getContentPanel().getTabbedPane().getMainWindow();
				SearchDialog.searchText(mainWindow, codeArea.getSelectedText());
			}
		};
		searchItem.setAction(searchAction);
		globalSearchItem.setAction(globalSearchAction);
		Separator separator = new Separator();
		JPopupMenu popupMenu = codeArea.getPopupMenu();
		popupMenu.addPopupMenuListener(new DefaultPopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				String preferText = codeArea.getSelectedText();
				if (!StringUtils.isEmpty(preferText)) {
					if (preferText.length() >= 23) {
						preferText = preferText.substring(0, 20) + " ...";
					}
					searchAction.putValue(Action.NAME, NLS.str("popup.search", preferText));
					globalSearchAction.putValue(Action.NAME, NLS.str("popup.search_global", preferText));
					popupMenu.add(separator);
					popupMenu.add(globalSearchItem);
					popupMenu.add(searchItem);
				} else {
					popupMenu.remove(separator);
					popupMenu.remove(globalSearchItem);
					popupMenu.remove(searchItem);
				}
			}
		});
	}

	public void loadSettings() {
		codeArea.loadSettings();
		initLineNumbers();
	}

	public void load() {
		codeArea.load();
		initLineNumbers();
	}

	private JScrollPane buildCodeScrollPane(AbstractCodeArea codeArea) {
		if (codeArea instanceof SmaliArea) {
			return new RTextScrollPane(codeArea);
		}
		return new JScrollPane(codeArea);
	}

	private void initLineNumbers() {
		if (codeArea instanceof SmaliArea) {
			return;
		}
		LineNumbersMode mode = getSettings().getLineNumbersMode();
		boolean canShowDebugLines = canShowDebugLines();
		if (mode == LineNumbersMode.AUTO) {
			mode = canShowDebugLines ? LineNumbersMode.DEBUG : LineNumbersMode.NORMAL;
		} else if (mode == LineNumbersMode.DEBUG && !canShowDebugLines) {
			// nothing to show => hide lines view
			mode = LineNumbersMode.DISABLE;
		}
		switch (mode) {
			case DISABLE:
				codeScrollPane.setRowHeaderView(null);
				break;
			case NORMAL:
				Gutter gutter = new Gutter(codeArea);
				gutter.setLineNumberFont(getSettings().getFont());
				codeScrollPane.setRowHeaderView(gutter);
				break;
			case DEBUG:
				LineNumbers jadxGutter = new LineNumbers(codeArea);
				jadxGutter.setUseSourceLines(true);
				codeScrollPane.setRowHeaderView(jadxGutter);
				break;
		}
	}

	private boolean canShowDebugLines() {
		ICodeInfo codeInfo = codeArea.getCodeInfo();
		if (!codeInfo.hasMetadata()) {
			return false;
		}
		Map<Integer, Integer> lineMapping = codeInfo.getCodeMetadata().getLineMapping();
		if (lineMapping.isEmpty()) {
			return false;
		}
		Set<Integer> uniqueDebugLines = new HashSet<>(lineMapping.values());
		return uniqueDebugLines.size() > 3;
	}

	public SearchBar getSearchBar() {
		return searchBar;
	}

	public AbstractCodeArea getCodeArea() {
		return codeArea;
	}

	public JScrollPane getCodeScrollPane() {
		return codeScrollPane;
	}

	public void refresh(CaretPositionFix caretFix) {
		JViewport viewport = getCodeScrollPane().getViewport();
		Point viewPosition = viewport.getViewPosition();
		codeArea.refresh();
		initLineNumbers();

		SwingUtilities.invokeLater(() -> {
			viewport.setViewPosition(viewPosition);
			caretFix.restore();
		});
	}

	private JadxSettings getSettings() {
		return this.codeArea.getContentPanel().getTabbedPane()
				.getMainWindow().getSettings();
	}

	public void dispose() {
		codeArea.dispose();
	}
}
