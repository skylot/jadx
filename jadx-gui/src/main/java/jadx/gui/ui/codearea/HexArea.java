package jadx.gui.ui.codearea;

import java.awt.BorderLayout;
import java.awt.Font;
import java.nio.charset.StandardCharsets;

import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import org.apache.commons.lang3.StringUtils;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.ResourcesLoader;
import jadx.core.utils.exceptions.JadxException;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.UiUtils;

public class HexArea extends AbstractCodeArea {
	private static final Logger LOG = LoggerFactory.getLogger(HexArea.class);

	private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

	private final HexAreaConfiguration config;
	private final JNode binaryNode;
	private final HexPreviewPanel hexPreviewPanel;
	private HexConfigurationPanel hexConfigurationPanel;

	private byte[] bytes;

	public HexArea(ContentPanel contentPanel, JNode node) {
		super(contentPanel, node);
		binaryNode = node;
		config = new HexAreaConfiguration();
		hexPreviewPanel = new HexPreviewPanel(config);

		initView();
		applyTheme();
	}

	@Override
	public @NotNull ICodeInfo getCodeInfo() {
		return ICodeInfo.EMPTY;
	}

	@Override
	public void load() {
		byte[] bytes = null;
		if (binaryNode instanceof JResource) {
			JResource jResource = (JResource) binaryNode;
			try {
				bytes = ResourcesLoader.decodeStream(jResource.getResFile(), (size, is) -> is.readAllBytes());
			} catch (JadxException e) {
				LOG.error("Failed to directly load resource binary data {}: {}", jResource.getName(), e.getMessage());
			}
		}
		if (bytes == null) {
			bytes = binaryNode.getCodeInfo().getCodeStr().getBytes(StandardCharsets.UTF_8);
		}
		setBytes(bytes);
		if (getBytes().length > 0) {
			// We set the caret after the first byte to prevent it from being highlighted
			setCaretPosition(2);
		} else {
			setCaretPosition(0);
		}
		setLoaded();
	}

	@Override
	public void refresh() {

	}

	@Override
	public void loadSettings() {
		super.loadSettings();
		applyTheme();
	}

	private void applyTheme() {
		Font font = getContentPanel().getMainWindow().getSettings().getSmaliFont();
		setFont(font);

		Theme theme = contentPanel.getMainWindow().getEditorTheme();
		if (hexPreviewPanel != null) {
			hexPreviewPanel.applyTheme(theme, font);
		}
	}

	private void initView() {
		addCaretListener(new HexCaretListener());
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
		hexPreviewPanel.setFont(getFont());
		add(hexPreviewPanel, BorderLayout.EAST);
	}

	private void setBytes(byte[] bytes) {
		this.bytes = bytes;

		String text;
		if (bytes.length > 0) {
			byte[] hexChars = new byte[bytes.length * 4 - 2];

			for (int j = 0; j < bytes.length; j++) {
				int v = bytes[j] & 0xFF;
				hexChars[j * 4] = HEX_ARRAY[v >>> 4];
				hexChars[j * 4 + 1] = HEX_ARRAY[v & 0x0F];
				if (j != bytes.length - 1) {
					hexChars[j * 4 + 2] = ' ';
					hexChars[j * 4 + 3] = (byte) ((j % config.bytesPerLine == config.bytesPerLine - 1) ? '\n' : ' ');
				}
			}
			text = new String(hexChars, StandardCharsets.UTF_8);
		} else {
			text = "";
		}
		setText(text);
		hexPreviewPanel.setBytes(bytes);
		hexConfigurationPanel.setBytes(bytes);
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setConfigurationPanel(HexConfigurationPanel hexConfigurationPanel) {
		this.hexConfigurationPanel = hexConfigurationPanel;
	}

	@Override
	public void copyAsStyledText() {
		String text = getSelectedText();
		if (text != null && !StringUtils.isEmpty(text)) {
			text = text
					.replace(" ", "")
					.replace("\n", "");
			UiUtils.copyToClipboard(text);
		}
	}

	public HexAreaConfiguration getConfiguration() {
		return config;
	}

	private class HexCaretListener implements CaretListener {
		private boolean isListening = true;
		private int previousCaretDot = -1;

		@Override
		public void caretUpdate(CaretEvent caretEvent) {
			int dot = caretEvent.getDot();
			int mark = caretEvent.getMark();

			if (!isListening) {
				return;
			}

			if (dot % 2 == 1) {
				if (previousCaretDot > dot) {
					if (mark == dot) {
						mark--;
					}
					dot--;
				} else {
					if (mark == dot) {
						mark++;
					}
					dot++;
				}

				isListening = false;
				HexArea.this.setCaretPosition(mark);
				HexArea.this.moveCaretPosition(dot);
				isListening = true;
			}

			if (previousCaretDot != dot) {
				onTextCursorMoved(dot, mark);
			}

			previousCaretDot = dot;
		}

		private void onTextCursorMoved(int dot, int mark) {
			hexConfigurationPanel.setOffset(dot / 4);

			int startIndex = Math.min(dot, mark);
			int endIndex = Math.max(dot, mark);
			int startOffset = startIndex / 4;
			int endOffset = endIndex / 4;
			if (startIndex % 4 == 2 && endIndex == startIndex + 2) {
				// Highlighted an empty space
				hexPreviewPanel.clearHighlights();
				return;
			}
			if (startOffset < endOffset && startIndex % 4 == 2) {
				startOffset++;
			}
			if (endOffset > startOffset && endIndex % 4 == 0) {
				endOffset--;
			}
			hexPreviewPanel.highlightBytes(startOffset, endOffset);
		}
	}
}
