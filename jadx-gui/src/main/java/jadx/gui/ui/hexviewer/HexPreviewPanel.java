package jadx.gui.ui.hexviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import org.exbin.auxiliary.binary_data.BinaryData;
import org.exbin.auxiliary.binary_data.array.ByteArrayEditableData;
import org.exbin.bined.CodeAreaCaretListener;
import org.exbin.bined.CodeAreaCaretPosition;
import org.exbin.bined.CodeAreaUtils;
import org.exbin.bined.CodeCharactersCase;
import org.exbin.bined.CodeType;
import org.exbin.bined.EditMode;
import org.exbin.bined.SelectionRange;
import org.exbin.bined.basic.BasicCodeAreaZone;
import org.exbin.bined.color.CodeAreaBasicColors;
import org.exbin.bined.highlight.swing.color.CodeAreaMatchColorType;
import org.exbin.bined.swing.CodeAreaPainter;
import org.exbin.bined.swing.basic.DefaultCodeAreaCommandHandler;
import org.exbin.bined.swing.capability.CharAssessorPainterCapable;
import org.exbin.bined.swing.capability.ColorAssessorPainterCapable;
import org.exbin.bined.swing.section.SectCodeArea;
import org.exbin.bined.swing.section.color.SectionCodeAreaColorProfile;

import jadx.gui.settings.JadxSettings;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class HexPreviewPanel extends JPanel {
	private static final long serialVersionUID = 3261685857479120073L;
	private static final int CACHE_SIZE = 250;

	private final byte[] valuesCache = new byte[CACHE_SIZE];
	private final SectCodeArea hexCodeArea;
	private final SectionCodeAreaColorProfile defaultColors;
	private final HexEditorHeader header;
	private final HexSearchBar searchBar;
	private final HexInspectorPanel inspector;

	private JPopupMenu popupMenu;
	private JMenuItem cutAction;
	private JMenuItem copyAction;
	private JMenuItem copyHexAction;
	private JMenuItem copyStringAction;
	private JMenuItem pasteAction;
	private JMenuItem deleteAction;
	private JMenuItem selectAllAction;
	private JMenuItem copyOffsetItem;
	private BasicCodeAreaZone popupMenuPositionZone = BasicCodeAreaZone.UNKNOWN;

	public HexPreviewPanel(JadxSettings settings) {
		hexCodeArea = new SectCodeArea();
		hexCodeArea.setCodeFont(settings.getFont());
		hexCodeArea.setEditMode(EditMode.READ_ONLY);
		hexCodeArea.setCharset(StandardCharsets.UTF_8);
		hexCodeArea.setComponentPopupMenu(new JPopupMenu() {
			@Override
			public void show(Component invoker, int x, int y) {
				popupMenuPositionZone = hexCodeArea.getPainter().getPositionZone(x, y);
				createPopupMenu();
				if (popupMenu != null && popupMenuPositionZone != BasicCodeAreaZone.HEADER
						&& popupMenuPositionZone != BasicCodeAreaZone.ROW_POSITIONS) {
					updatePopupActionStates();
					popupMenu.show(invoker, x, y);
				}
			}
		});

		inspector = new HexInspectorPanel();
		searchBar = new HexSearchBar(hexCodeArea);
		header = new HexEditorHeader(hexCodeArea);
		header.setFont(settings.getFont());

		CodeAreaPainter painter = hexCodeArea.getPainter();
		defaultColors = (SectionCodeAreaColorProfile) hexCodeArea.getColorsProfile();

		hexCodeArea.setColorsProfile(getColorsProfile());

		BinEdCodeAreaAssessor codeAreaAssessor = new BinEdCodeAreaAssessor(((ColorAssessorPainterCapable) painter).getColorAssessor(),
				((CharAssessorPainterCapable) painter).getCharAssessor());
		((ColorAssessorPainterCapable) painter).setColorAssessor(codeAreaAssessor);
		((CharAssessorPainterCapable) painter).setCharAssessor(codeAreaAssessor);

		setLayout(new BorderLayout());
		add(searchBar, BorderLayout.PAGE_START);
		add(hexCodeArea, BorderLayout.CENTER);
		add(header, BorderLayout.PAGE_END);
		add(inspector, BorderLayout.EAST);

		setFocusable(true);
		addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				hexCodeArea.requestFocusInWindow();
			}

			@Override
			public void focusLost(FocusEvent e) {

			}
		});
		createActions();
		enableUpdate();
	}

	public SectionCodeAreaColorProfile getColorsProfile() {
		boolean isDarkTheme = UiUtils.isDarkTheme(Objects.requireNonNull(defaultColors.getColor(CodeAreaBasicColors.TEXT_BACKGROUND)));
		Color markAllHighlightColor = isDarkTheme ? Color.decode("#32593D") : Color.decode("#ffc800");
		Color editorSelectionBackground = Objects.requireNonNull(defaultColors.getColor(CodeAreaBasicColors.SELECTION_BACKGROUND));
		Color currentMatchColor = UiUtils.adjustBrightness(editorSelectionBackground, isDarkTheme ? 0.6f : 1.4f);
		defaultColors.setColor(CodeAreaMatchColorType.MATCH_BACKGROUND, markAllHighlightColor);
		defaultColors.setColor(CodeAreaMatchColorType.CURRENT_MATCH_BACKGROUND, currentMatchColor);
		return defaultColors;
	}

	public boolean isDataLoaded() {
		return !hexCodeArea.getContentData().isEmpty();
	}

	public void setData(byte[] data) {
		if (data != null) {
			hexCodeArea.setContentData(new ByteArrayEditableData(data));
			inspector.setBytes(data);
		}
	}

	public void enableUpdate() {
		CodeAreaCaretListener caretMovedListener = (CodeAreaCaretPosition caretPosition) -> updateValues();
		hexCodeArea.addCaretMovedListener(caretMovedListener);
	}

	private void updateValues() {
		CodeAreaCaretPosition caretPosition = hexCodeArea.getActiveCaretPosition();
		long dataPosition = caretPosition.getDataPosition();
		long dataSize = hexCodeArea.getDataSize();

		if (dataPosition < dataSize) {
			int availableData = dataSize - dataPosition >= CACHE_SIZE ? CACHE_SIZE : (int) (dataSize - dataPosition);
			BinaryData contentData = hexCodeArea.getContentData();
			contentData.copyToArray(dataPosition, valuesCache, 0, availableData);
			if (availableData < CACHE_SIZE) {
				Arrays.fill(valuesCache, availableData, CACHE_SIZE, (byte) 0);
			}
		}

		inspector.setOffset((int) dataPosition);
	}

	private void createActions() {
		cutAction = new JMenuItem(NLS.str("popup.cut"));
		cutAction.addActionListener(e -> performCut());

		copyAction = new JMenuItem(NLS.str("popup.copy"));
		copyAction.addActionListener(e -> performCopy());

		copyHexAction = new JMenuItem(NLS.str("popup.copy_as_hex"));
		copyHexAction.addActionListener(e -> performCopyAsCode());

		copyStringAction = new JMenuItem(NLS.str("popup.copy_as_string"));
		copyStringAction.addActionListener(e -> performCopy());

		pasteAction = new JMenuItem(NLS.str("popup.paste"));
		pasteAction.addActionListener(e -> performPaste());

		deleteAction = new JMenuItem(NLS.str("popup.delete"));
		deleteAction.addActionListener(e -> {
			if (!isEditable()) {
				performDelete();
			}
		});

		selectAllAction = new JMenuItem(NLS.str("popup.select_all"));
		selectAllAction.addActionListener(e -> performSelectAll());

		copyOffsetItem = new JMenuItem(NLS.str("popup.copy_offset"));
		copyOffsetItem.addActionListener(e -> copyOffset());
	}

	private void createPopupMenu() {
		boolean isEditable = isEditable();
		popupMenu = new JPopupMenu();
		popupMenu.add(copyAction);

		if (isEditable) {
			popupMenu.add(cutAction);
			popupMenu.add(pasteAction);
			popupMenu.add(deleteAction);
			popupMenu.addSeparator();
		}

		JMenu copyMenu = new JMenu(NLS.str("popup.copy_as"));
		copyMenu.add(copyHexAction);
		copyMenu.add(copyStringAction);
		popupMenu.add(copyMenu);
		popupMenu.add(copyOffsetItem);
		popupMenu.add(selectAllAction);
	}

	private void updatePopupActionStates() {

		boolean selectionExists = isSelection();
		boolean isEditable = !isEditable();

		cutAction.setEnabled(isEditable && selectionExists);
		copyAction.setEnabled(selectionExists);
		copyHexAction.setEnabled(selectionExists);
		copyStringAction.setEnabled(selectionExists);
		deleteAction.setEnabled(isEditable && selectionExists);

		selectAllAction.setEnabled(hexCodeArea.getDataSize() > 0);
	}

	public SectCodeArea getEditor() {
		return this.hexCodeArea;
	}

	public HexEditorHeader getHeader() {
		return this.header;
	}

	public HexInspectorPanel getInspector() {
		return this.inspector;
	}

	public HexSearchBar getSearchBar() {
		return this.searchBar;
	}

	public void showSearchBar() {
		searchBar.showAndFocus();
	}

	public void performCut() {
		hexCodeArea.cut();
	}

	public void performCopy() {
		hexCodeArea.copy();
	}

	public void performCopyAsCode() {
		((DefaultCodeAreaCommandHandler) hexCodeArea.getCommandHandler()).copyAsCode();
	}

	public void performPaste() {
		hexCodeArea.paste();
	}

	public void performDelete() {
		hexCodeArea.delete();
	}

	public void performSelectAll() {
		hexCodeArea.selectAll();
	}

	public boolean isSelection() {
		return hexCodeArea.hasSelection();
	}

	public boolean isEditable() {
		return hexCodeArea.isEditable();
	}

	public boolean canPaste() {
		return hexCodeArea.canPaste();
	}

	public static String getSelectionData(SectCodeArea core) {
		SelectionRange selection = core.getSelection();
		if (!selection.isEmpty()) {
			long first = selection.getFirst();
			long last = selection.getLast();

			BinaryData copy = core.getContentData().copy(first, last - first + 1);

			CodeType codeType = core.getCodeType();
			CodeCharactersCase charactersCase = core.getCodeCharactersCase();

			int charsPerByte = codeType.getMaxDigitsForByte() + 1;
			int textLength = (int) (copy.getDataSize() * charsPerByte);
			if (textLength > 0) {
				textLength--;
			}

			char[] targetData = new char[textLength];
			Arrays.fill(targetData, ' ');
			for (int i = 0; i < (int) copy.getDataSize(); i++) {
				CodeAreaUtils.byteToCharsCode(copy.getByte(i), codeType, targetData, i * charsPerByte, charactersCase);
			}
			return new String(targetData);
		}
		return null;
	}

	public void copyOffset() {
		String str = header.addressString(hexCodeArea.getSelection().getStart());
		UiUtils.copyToClipboard(str);
	}

}
