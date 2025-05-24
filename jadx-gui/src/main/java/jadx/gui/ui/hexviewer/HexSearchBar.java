package jadx.gui.ui.hexviewer;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;

import org.exbin.auxiliary.binary_data.array.ByteArrayEditableData;
import org.exbin.bined.CodeAreaUtils;
import org.exbin.bined.swing.section.SectCodeArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatClientProperties;

import jadx.core.utils.StringUtils;
import jadx.gui.ui.hexviewer.search.BinarySearch;
import jadx.gui.ui.hexviewer.search.SearchCondition;
import jadx.gui.ui.hexviewer.search.SearchParameters;
import jadx.gui.ui.hexviewer.search.service.BinarySearchServiceImpl;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;

public class HexSearchBar extends JToolBar {
	private static final long serialVersionUID = 1836871286618633003L;

	private static final Logger LOG = LoggerFactory.getLogger(HexSearchBar.class);
	private final SectCodeArea hexCodeArea;

	private final JTextField searchField;
	private final JLabel resultCountLabel;
	private final JToggleButton markAllCB;
	private final JToggleButton findTypeCB;
	private final JToggleButton matchCaseCB;
	private final JButton nextMatchButton;
	private final JButton prevMatchButton;

	private Control control = null;

	public HexSearchBar(SectCodeArea textArea) {
		hexCodeArea = textArea;

		JLabel findLabel = new JLabel(NLS.str("search.find") + ':');
		add(findLabel);

		searchField = new JTextField(30);
		searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
		searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_ENTER:
						// skip
						break;
					case KeyEvent.VK_ESCAPE:
						toggle();
						break;
					default:
						control.performFind();
						break;
				}
			}
		});
		searchField.addActionListener(e -> control.notifySearchChanging());
		TextStandardActions.attach(searchField);
		add(searchField);

		ActionListener searchSettingListener = e -> control.notifySearchChanged();

		resultCountLabel = new JLabel();
		resultCountLabel.setBorder(new EmptyBorder(0, 10, 0, 10));
		resultCountLabel.setForeground(Color.GRAY);
		add(resultCountLabel);

		matchCaseCB = new JToggleButton();
		matchCaseCB.setIcon(Icons.ICON_MATCH);
		matchCaseCB.setSelectedIcon(Icons.ICON_MATCH_SELECTED);
		matchCaseCB.setToolTipText(NLS.str("search.match_case"));
		matchCaseCB.addActionListener(searchSettingListener);
		add(matchCaseCB);

		findTypeCB = new JToggleButton();
		findTypeCB.setIcon(Icons.ICON_FIND_TYPE_TXT);
		findTypeCB.setSelectedIcon(Icons.ICON_FIND_TYPE_HEX);
		if (findTypeCB.isSelected()) {
			findTypeCB.setToolTipText(NLS.str("search.find_type_hex"));
		} else {
			findTypeCB.setToolTipText(NLS.str("search.find_type_text"));
		}
		findTypeCB.addActionListener(e -> {
			searchField.setText("");
			updateFindStatus();
			control.notifySearchChanged();
		});
		add(findTypeCB);

		prevMatchButton = new JButton();
		prevMatchButton.setIcon(Icons.ICON_UP);
		prevMatchButton.setToolTipText(NLS.str("search.previous"));
		prevMatchButton.addActionListener(e -> control.prevMatch());
		prevMatchButton.setBorderPainted(false);
		add(prevMatchButton);

		nextMatchButton = new JButton();
		nextMatchButton.setIcon(Icons.ICON_DOWN);
		nextMatchButton.setToolTipText(NLS.str("search.next"));
		nextMatchButton.addActionListener(e -> control.nextMatch());
		nextMatchButton.setBorderPainted(false);
		add(nextMatchButton);

		markAllCB = new JToggleButton();
		markAllCB.setIcon(Icons.ICON_MARK);
		markAllCB.setSelectedIcon(Icons.ICON_MARK_SELECTED);
		markAllCB.setToolTipText(NLS.str("search.mark_all"));
		markAllCB.setSelected(true);
		markAllCB.addActionListener(searchSettingListener);
		add(markAllCB);

		JButton closeButton = new JButton();
		closeButton.setIcon(Icons.ICON_CLOSE);
		closeButton.addActionListener(e -> toggle());
		closeButton.setBorderPainted(false);
		add(closeButton);

		BinarySearch binarySearch = new BinarySearch(this);
		binarySearch.setBinarySearchService(new BinarySearchServiceImpl(hexCodeArea));
		setFloatable(false);
		setVisible(false);

	}

	/*
	 * Replicates IntelliJ's search bar behavior
	 * 1.1. If the user has selected text, use that as the search text
	 * 1.2. Otherwise, use the previous search text (or empty if none)
	 * 2. Select all text in the search bar and give it focus
	 */
	public void showAndFocus() {
		setVisible(true);

		if (hexCodeArea.hasSelection()) {
			searchField.setText(hexCodeArea.getActiveSection().toString());
		}
		String selectedText = HexPreviewPanel.getSelectionData(hexCodeArea);
		if (!StringUtils.isEmpty(selectedText)) {
			searchField.setText(selectedText);
			makeFindByHexButton();
		}

		searchField.selectAll();
		searchField.requestFocus();
	}

	public void toggle() {
		boolean visible = !isVisible();
		setVisible(visible);

		if (visible) {
			String preferText = HexPreviewPanel.getSelectionData(hexCodeArea);
			if (!StringUtils.isEmpty(preferText)) {
				searchField.setText(preferText);
				makeFindByHexButton();
			}
			searchField.selectAll();
			searchField.requestFocus();
		} else {
			control.performEscape();
			hexCodeArea.requestFocus();
		}
	}

	public void setInfoLabel(String text) {
		resultCountLabel.setText(text);
	}

	public void updateMatchCount(boolean hasMatches, boolean prevMatchAvailable, boolean nextMatchAvailable) {
		prevMatchButton.setEnabled(prevMatchAvailable);
		nextMatchButton.setEnabled(nextMatchAvailable);
	}

	public void setControl(Control control) {
		this.control = control;
	}

	public void clearSearch() {
		setInfoLabel("");
		searchField.setText("");
	}

	public SearchParameters getSearchParameters() {
		SearchParameters searchParameters = new SearchParameters();
		searchParameters.setMatchCase(matchCaseCB.isSelected());
		searchParameters.setMatchMode(SearchParameters.MatchMode.fromBoolean(markAllCB.isSelected()));
		SearchParameters.SearchDirection searchDirection = control.getSearchDirection();
		searchParameters.setSearchDirection(searchDirection);

		long startPosition;
		if (searchParameters.isSearchFromCursor()) {
			startPosition = hexCodeArea.getActiveCaretPosition().getDataPosition();
		} else {
			switch (searchDirection) {
				case FORWARD: {
					startPosition = 0;
					break;
				}
				case BACKWARD: {
					startPosition = hexCodeArea.getDataSize() - 1;
					break;
				}
				default:
					throw CodeAreaUtils.getInvalidTypeException(searchDirection);
			}
		}
		searchParameters.setStartPosition(startPosition);

		searchParameters.setCondition(new SearchCondition(makeSearchCondition()));
		return searchParameters;
	}

	private SearchCondition makeSearchCondition() {
		SearchCondition condition = new SearchCondition();
		if (findTypeCB.isSelected()) {
			condition.setSearchMode(SearchCondition.SearchMode.BINARY);
		} else {
			condition.setSearchMode(SearchCondition.SearchMode.TEXT);
		}
		if (!StringUtils.isEmpty(searchField.getText())) {
			if (condition.getSearchMode() == SearchCondition.SearchMode.TEXT) {
				condition.setSearchText(searchField.getText());
			} else {
				String hexBytes = searchField.getText();
				if (isValidHexString(hexBytes)) {
					condition.setBinaryData(new ByteArrayEditableData(hexStringToByteArray(hexBytes)));
				}
			}
		}
		return condition;
	}

	public void updateFindStatus() {
		SearchCondition condition = makeSearchCondition();
		if (condition.getSearchMode() == SearchCondition.SearchMode.TEXT) {
			findTypeCB.setSelected(false);
			findTypeCB.setToolTipText(NLS.str("search.find_type_text"));
			matchCaseCB.setEnabled(true);
		} else {
			makeFindByHexButton();
			matchCaseCB.setEnabled(false);
		}
	}

	private void makeFindByHexButton() {
		findTypeCB.setSelected(true);
		findTypeCB.setToolTipText(NLS.str("search.find_type_hex"));
	}

	private boolean isValidHexString(String hexString) {
		String cleanS = hexString.replace(" ", "");
		int len = cleanS.length();
		try {
			boolean isPair = len % 2 == 0;
			if (isPair) {
				Long.parseLong(cleanS, 16);
				return true;
			}
		} catch (NumberFormatException ex) {
			// ignore error
			return false;
		}
		return false;
	}

	public byte[] hexStringToByteArray(String hexString) {
		if (hexString == null || hexString.isEmpty()) {
			return new byte[0];
		}
		String cleanS = hexString.replace(" ", "");
		int len = cleanS.length();
		if (!isValidHexString(hexString)) {
			throw new IllegalArgumentException("Hex string must have even length. Input length: " + len);
		}

		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			String byteString = cleanS.substring(i, i + 2);
			try {
				int intValue = Integer.parseInt(byteString, 16);
				data[i / 2] = (byte) intValue;
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Input string contains non-hex characters at index " + i + ": " + byteString, e);
			}
		}
		return data;
	}

	public interface Control {

		void prevMatch();

		void nextMatch();

		void performEscape();

		void performFind();

		/**
		 * Parameters of search have changed.
		 */
		void notifySearchChanged();

		/**
		 * Parameters of search are changing which might not lead to immediate
		 * search change.
		 * <p>
		 * Typically, text typing.
		 */
		void notifySearchChanging();

		SearchParameters.SearchDirection getSearchDirection();

		void close();
	}
}
