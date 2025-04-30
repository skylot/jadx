package jadx.gui.ui.hexeditor.editor;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.hexeditor.buffer.ArrayByteBuffer;
import jadx.gui.ui.hexeditor.buffer.BitTransform;
import jadx.gui.ui.hexeditor.buffer.ByteBuffer;
import jadx.gui.ui.hexeditor.buffer.ByteBufferDocument;
import jadx.gui.ui.hexeditor.buffer.ByteBufferHistory;
import jadx.gui.ui.hexeditor.buffer.ByteBufferListener;
import jadx.gui.ui.hexeditor.buffer.ByteBufferSelectionListener;
import jadx.gui.ui.hexeditor.buffer.ByteBufferSelectionModel;
import jadx.gui.ui.hexeditor.buffer.ByteTransform;
import jadx.gui.ui.hexeditor.buffer.CompositeByteBuffer;
import jadx.gui.ui.hexeditor.buffer.IncrementTransform;
import jadx.gui.ui.hexeditor.buffer.RandomTransform;
import jadx.gui.ui.hexeditor.buffer.ReverseTransform;
import jadx.gui.ui.hexeditor.buffer.RotateTransform;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public class JHexEditor extends JComponent implements Scrollable {
	private static final Logger LOG = LoggerFactory.getLogger(JHexEditor.class);
	private static final long serialVersionUID = 1L;

	private static final Font DEFAULT_FONT = new Font("Monospaced", Font.PLAIN, 12);
	private static final String HEX_ALPHABET = "0123456789ABCDEF";
	private final boolean[] charsetPrintable = new boolean[256];
	private final String[] charsetStrings = new String[256];

	private ByteBufferDocument document;
	private JHexEditorColors colors = JHexEditorColors.AQUA;
	private boolean extendBorders = false;
	private boolean decimalAddresses = false;
	private String charset = "ISO-8859-1";
	private boolean textActive = false;

	private boolean readOnly = false;
	private boolean overtype = false;
	private boolean enableShortcutKeys = true;
	private boolean enableTransformKeys = true;
	private boolean littleEndian = false;
	private long mark = 0;
	private int minimumBytesPerRow = 4;
	private int minimumRowCount = 1;
	private int preferredBytesPerRow = 16;
	private int preferredRowCount = 16;
	private Dimension minimumSize = null;
	private Dimension preferredSize = null;

	private JPopupMenu popupMenu;
	private JMenuItem cutAction;
	private JMenuItem copyAction;
	private JMenuItem copyHexAction;
	private JMenuItem copyStringAction;
	private JMenuItem pasteAction;
	private JMenuItem pasteHexAction;
	private JMenuItem pasteStringAction;
	private JMenuItem deleteAction;
	private JMenuItem selectAllAction;
	private JMenuItem undoAction;
	private JMenuItem redoAction;
	private JMenuItem copyOffsetItem;

	// EVENT HANDLING
	private final List<JHexEditorListener> listeners = new ArrayList<>();
	private final ByteBufferListener bufferListener = new ByteBufferListener() {
		@Override
		public void dataInserted(ByteBuffer buffer, long offset, int length) {
			for (JHexEditorListener l : listeners) {
				l.dataInserted(buffer, offset, length);
			}
			revalidate();
			repaint();
		}

		@Override
		public void dataOverwritten(ByteBuffer buffer, long offset, int length) {
			for (JHexEditorListener l : listeners) {
				l.dataOverwritten(buffer, offset, length);
			}
			repaint();
		}

		@Override
		public void dataRemoved(ByteBuffer buffer, long offset, long length) {
			for (JHexEditorListener l : listeners) {
				l.dataRemoved(buffer, offset, length);
			}
			revalidate();
			repaint();
		}
	};
	private final ByteBufferSelectionListener selectionListener = new ByteBufferSelectionListener() {
		@Override
		public void selectionChanged(ByteBufferSelectionModel sm, long start, long end) {
			if (isShowing()) {
				scrollRectToVisible(getRectForOffset(document.getSelectionEnd()));
			}
			for (JHexEditorListener l : listeners) {
				l.selectionChanged(sm, start, end);
			}
			repaint();
		}
	};
	private final MouseAdapter mouseListener = new MouseAdapter() {
		@Override
		public void mousePressed(MouseEvent e) {
			if (!isFocusOwner()) {
				requestFocusInWindow();
			}
			PointInfo p = getInfoAtPoint(e.getX(), e.getY());
			if (p == null) {
				return;
			}
			if (!e.isPopupTrigger()) {
				if (p.inHexArea || p.inTextArea) {
					if (e.isShiftDown()) {
						document.setSelectionEnd(p.offset);
						// When extending selection, the active area should probably be determined by the END point
						setTextActive(p.inTextArea);
					} else { // Non-Shift click
						long clickOffset = p.offset;
						long selStart = getSelectionMin();
						long selEnd = getSelectionMax();
						boolean clickInsideSelection = (isSelectionExists() && clickOffset >= selStart && clickOffset < selEnd);

						// Condition to KEEP the selection: text is currently active AND click is OUTSIDE the selection.
						boolean keepSelection = isTextActive() && !clickInsideSelection;

						if (!keepSelection && SwingUtilities.isLeftMouseButton(e)) {
							// If we are NOT keeping the selection (either hex is active, or text is active and click is inside
							// selection)
							// Then clear the selection and move the cursor to the click point.
							document.setSelectionRange(p.offset, p.offset);
							document.setMidByte(false); // Reset mid-byte state on regular click

							// Set active area based on the click location ONLY when selection is cleared/moved
							setTextActive(p.inTextArea);
						}
					}
				} else if (p.inAddressArea) {
					long startOfRow = (p.offset / p.rowWidth) * p.rowWidth;
					long endOfRow = Math.min(startOfRow + p.rowWidth, document.length());
					document.setSelectionRange(startOfRow, endOfRow);
					document.setMidByte(false); // Address click resets mid-byte
					setTextActive(false); // Address click implies hex area focus
				}
			}
			showPopupMenuIfTriggered(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			showPopupMenuIfTriggered(e);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			PointInfo p = getInfoAtPoint(e.getX(), e.getY());
			if (p != null && (p.inHexArea || p.inTextArea)) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					document.setSelectionEnd(p.offset);
					Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
					scrollRectToVisible(r);
				}
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			PointInfo p = getInfoAtPoint(e.getX(), e.getY());
			if (p != null && (p.inHexArea || p.inTextArea)) {
				setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
			} else {
				setCursor(Cursor.getDefaultCursor());
			}
		}

		private void showPopupMenuIfTriggered(MouseEvent e) {
			createPopupMenu();
			if (e.isPopupTrigger() && popupMenu != null) {
				PointInfo p = getInfoAtPoint(e.getX(), e.getY());
				if (p != null && (p.inHexArea || p.inTextArea)) {
					updatePopupActionStates();
					popupMenu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		}
	};
	private final FocusListener focusListener = new FocusListener() {
		@Override
		public void focusGained(FocusEvent e) {
			repaint();
		}

		@Override
		public void focusLost(FocusEvent e) {
			repaint();
		}
	};
	private final KeyAdapter keyListener = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent e) {
			if (e.isMetaDown() || e.isControlDown()) {
				if (!enableShortcutKeys) {
					return;
				}
				switch (e.getKeyCode()) {
					case KeyEvent.VK_C:
						if (e.isShiftDown()) {
							copyAsString();
						} else if (e.isAltDown()) {
							copyAsHex();
						} else {
							copy();
						}
						e.consume();
						return;
					case KeyEvent.VK_A:
						selectAll();
						e.consume();
						return;
				}
				if (readOnly) {
					return;
				}

				switch (e.getKeyCode()) {
					case KeyEvent.VK_Z:
						if (e.isShiftDown()) {
							redo();
						} else {
							undo();
						}
						e.consume();
						return;
					case KeyEvent.VK_Y:
						if (e.isShiftDown()) {
							undo();
						} else {
							redo();
						}
						e.consume();
						return;
					case KeyEvent.VK_X:
						if (e.isShiftDown()) {
							cutAsString();
						} else if (e.isAltDown()) {
							cutAsHex();
						} else {
							cut();
						}
						e.consume();
						return;
					case KeyEvent.VK_V:
						if (e.isShiftDown()) {
							pasteAsString();
						} else if (e.isAltDown()) {
							pasteAsHex();
						} else {
							paste();
						}
						e.consume();
						return;
				}
				return;
			}
			switch (e.getKeyCode()) {
				case KeyEvent.VK_ENTER:
				case KeyEvent.VK_ESCAPE:
					setTextActive(!textActive);
					e.consume();
					return;
				case KeyEvent.VK_INSERT:
					setOvertype(!overtype);
					e.consume();
					return;
				case KeyEvent.VK_LEFT:
					arrowKey(-1, e.isShiftDown());
					e.consume();
					return;
				case KeyEvent.VK_RIGHT:
					arrowKey(+1, e.isShiftDown());
					e.consume();
					return;
				case KeyEvent.VK_UP:
					arrowKey(-getRowWidth(), e.isShiftDown());
					e.consume();
					return;
				case KeyEvent.VK_DOWN:
					arrowKey(+getRowWidth(), e.isShiftDown());
					e.consume();
					return;
				case KeyEvent.VK_PAGE_UP:
					arrowKey(-getPageSize(), e.isShiftDown());
					e.consume();
					return;
				case KeyEvent.VK_PAGE_DOWN:
					arrowKey(+getPageSize(), e.isShiftDown());
					e.consume();
					return;
				case KeyEvent.VK_HOME:
					if (e.isShiftDown()) {
						document.setSelectionEnd(0);
					} else {
						document.setSelectionRange(0, 0);
					}
					e.consume();
					return;
				case KeyEvent.VK_END:
					long el = document.length();
					if (e.isShiftDown()) {
						document.setSelectionEnd(el);
					} else {
						document.setSelectionRange(el, el);
					}
					e.consume();
					return;
				case KeyEvent.VK_COPY:
					if (e.isShiftDown()) {
						copyAsString();
					} else if (e.isAltDown()) {
						copyAsHex();
					} else {
						copy();
					}
					e.consume();
					return;
			}
			if (readOnly) {
				return;
			}
			switch (e.getKeyCode()) {
				case KeyEvent.VK_BACK_SPACE:
					document.deleteBackward();
					e.consume();
					return;
				case KeyEvent.VK_DELETE:
					document.deleteForward();
					e.consume();
					return;
				case KeyEvent.VK_UNDO:
					if (e.isShiftDown()) {
						redo();
					} else {
						undo();
					}
					e.consume();
					return;
				case KeyEvent.VK_CUT:
					if (e.isShiftDown()) {
						cutAsString();
					} else if (e.isAltDown()) {
						cutAsHex();
					} else {
						cut();
					}
					e.consume();
					return;
				case KeyEvent.VK_PASTE:
					if (e.isShiftDown()) {
						pasteAsString();
					} else if (e.isAltDown()) {
						pasteAsHex();
					} else {
						paste();
					}
					e.consume();
			}
		}

		@Override
		public void keyTyped(KeyEvent e) {
			if (e.isMetaDown() || e.isControlDown()) {
				return;
			}
			char ch = e.getKeyChar();
			if ((ch >= 0x20 && ch < 0x7F) || (ch >= 0xA0 && ch < 0xFFFD)) {
				if (textActive) {
					if (readOnly) {
						return;
					}
					byte[] data;
					try {
						data = Character.toString(ch).getBytes(charset);
					} catch (Exception ignored) {
						return;
					}
					if (overtype) {
						document.overwrite(data);
					} else {
						document.insert(data);
					}
					e.consume();
				} else {
					int d = Character.digit(ch, 16);
					if (d >= 0) {
						if (readOnly) {
							return;
						}
						if (overtype) {
							document.overwrite(d);
						} else {
							document.insert(d);
						}
						e.consume();
						return;
					}
					if (!enableTransformKeys) {
						return;
					}
					switch (ch) {
						case 'H':
						case 'h':
							setDecimalAddresses(!decimalAddresses);
							e.consume();
							return;
						case 'L':
						case 'l':
							setLittleEndian(!littleEndian);
							e.consume();
							return;
						case 'K':
						case 'k':
							setMark(document.getSelectionMin());
							e.consume();
							return;
						case 'J':
						case 'j':
							long jm = mark;
							byte[] jd = document.getSelection();
							if (jd != null && jd.length > 0) {
								if (littleEndian) {
									ReverseTransform.BYTES.transform(jd, 0, jd.length);
								}
								jm += new BigInteger(jd).longValue();
							}
							long jl = document.length();
							if (jm > jl) {
								jm = jl;
							}
							document.setSelectionRange(jm, jm);
							e.consume();
							return;
					}
					if (readOnly) {
						return;
					}
					switch (ch) {
						case 'Z':
						case 'z':
							document.transformSelection(BitTransform.ZERO);
							e.consume();
							return;
						case 'Y':
						case 'y':
							document.transformSelection(BitTransform.SPACE);
							e.consume();
							return;
						case 'X':
						case 'x':
							document.transformSelection(BitTransform.ONE);
							e.consume();
							return;
						case 'I':
						case 'i':
							document.transformSelection(BitTransform.INVERT);
							e.consume();
							return;
						case 'M':
						case 'm':
							document.transformSelection(BitTransform.INVERT_MSB);
							e.consume();
							return;
						case 'V':
						case 'v':
							document.transformSelection(RandomTransform.RANDOM);
							e.consume();
							return;
						case 'S':
						case 's':
							document.transformSelection(ReverseTransform.BYTES);
							e.consume();
							return;
						case 'N':
						case 'n':
							document.transformSelection(ReverseTransform.NYBBLES);
							e.consume();
							return;
						case 'R':
						case 'r':
							document.transformSelection(ReverseTransform.BITS);
							e.consume();
							return;
						case '[':
						case '{':
							document.transformSelection(littleEndian ? RotateTransform.ROL_LE : RotateTransform.ROL_BE);
							e.consume();
							return;
						case ']':
						case '}':
							document.transformSelection(littleEndian ? RotateTransform.ROR_LE : RotateTransform.ROR_BE);
							e.consume();
							return;
						case ',':
						case '<':
							document.transformSelection(littleEndian ? RotateTransform.ASL_LE : RotateTransform.ASL_BE);
							e.consume();
							return;
						case '.':
						case '>':
							document.transformSelection(littleEndian ? RotateTransform.ASR_LE : RotateTransform.ASR_BE);
							e.consume();
							return;
						case '/':
						case '?':
							document.transformSelection(littleEndian ? RotateTransform.LSR_LE : RotateTransform.LSR_BE);
							e.consume();
							return;
						case '=':
						case '+':
							document.transformSelection(littleEndian ? IncrementTransform.INC_LE : IncrementTransform.INC_BE);
							e.consume();
							return;
						case '-':
						case '_':
							document.transformSelection(littleEndian ? IncrementTransform.DEC_LE : IncrementTransform.DEC_BE);
							e.consume();
					}
				}
			}
		}
	};

	public JHexEditor() {
		this(new ByteBufferDocument(new CompositeByteBuffer()));
	}

	public JHexEditor(byte[] data) {
		this(new ByteBufferDocument(new CompositeByteBuffer(new ArrayByteBuffer(data))));
	}

	public JHexEditor(ByteBuffer buffer) {
		this(new ByteBufferDocument(new CompositeByteBuffer(buffer)));
	}

	public JHexEditor(ByteBufferDocument document) {
		this.document = document;
		this.document.addByteBufferListener(bufferListener);
		this.document.addSelectionListener(selectionListener);
		this.makeCharset();
		this.setFont(DEFAULT_FONT);
		this.setFocusable(true);
		this.setRequestFocusEnabled(true);
		this.addFocusListener(focusListener);
		this.addMouseListener(mouseListener);
		this.addMouseMotionListener(mouseListener);
		this.addKeyListener(keyListener);
		this.createActions();
	}

	// PROPERTY GETTERS AND SETTERS

	public ByteBufferDocument getDocument() {
		return this.document;
	}

	public void setDocument(byte[] data) {
		setDocument(new ByteBufferDocument(new CompositeByteBuffer(new ArrayByteBuffer(data))));
	}

	public void setDocument(ByteBuffer buffer) {
		setDocument(new ByteBufferDocument(new CompositeByteBuffer(buffer)));
	}

	public void setDocument(ByteBufferDocument document) {
		this.document.removeByteBufferListener(bufferListener);
		this.document.removeSelectionListener(selectionListener);
		this.document = document;
		this.document.addByteBufferListener(bufferListener);
		this.document.addSelectionListener(selectionListener);
		for (JHexEditorListener l : listeners) {
			l.documentChanged(this, document);
		}
		revalidate();
		repaint();
	}

	public JHexEditorColors getColors() {
		return this.colors;
	}

	public void setColors(JHexEditorColors newColors) {
		JHexEditorColors effectiveColors = (newColors != null) ? newColors : JHexEditorColors.getThemed();
		JHexEditorColors currentThemedInstance = JHexEditorColors.getThemed();

		if (this.colors != effectiveColors) {
			if (this.colors == currentThemedInstance) {
				LOG.debug("Unregistering JHexEditor from Themed updates (changing scheme)");
				JHexEditorColors.unregisterThemedComponent(this);
			}

			this.colors = effectiveColors;

			if (this.colors == currentThemedInstance) {
				if (isDisplayable()) {
					LOG.debug("Registering JHexEditor for Themed updates (scheme set while visible)");
					JHexEditorColors.registerThemedComponent(this);
				}
			}
			for (JHexEditorListener l : listeners) {
				l.colorsChanged(this, this.colors);
			}
			repaint();
		}
	}

	@Override
	public void addNotify() {
		super.addNotify();
		LOG.debug("JHexEditor addNotify called.");
		if (this.colors == JHexEditorColors.getThemed()) {
			JHexEditorColors.registerThemedComponent(this);
		}
	}

	@Override
	public void removeNotify() {
		JHexEditorColors.unregisterThemedComponent(this);
		super.removeNotify();
	}

	public boolean getExtendBorders() {
		return this.extendBorders;
	}

	public void setExtendBorders(boolean extendBorders) {
		this.extendBorders = extendBorders;
		notifyEditorStatusChanged();
		repaint();
	}

	public boolean getDecimalAddresses() {
		return this.decimalAddresses;
	}

	public void setDecimalAddresses(boolean decimalAddresses) {
		this.decimalAddresses = decimalAddresses;
		notifyEditorStatusChanged();
		repaint();
	}

	public String getCharset() {
		return this.charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
		this.makeCharset();
		notifyEditorStatusChanged();
		repaint();
	}

	public boolean isTextActive() {
		return this.textActive;
	}

	public void setTextActive(boolean textActive) {
		this.textActive = textActive;
		notifyEditorStatusChanged();
		repaint();
	}

	public boolean isReadOnly() {
		return this.readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		notifyEditorStatusChanged();
	}

	public boolean getOvertype() {
		return this.overtype;
	}

	public void setOvertype(boolean overtype) {
		this.overtype = overtype;
		notifyEditorStatusChanged();
	}

	public boolean getEnableShortcutKeys() {
		return this.enableShortcutKeys;
	}

	public void setEnableShortcutKeys(boolean enableShortcutKeys) {
		this.enableShortcutKeys = enableShortcutKeys;
		notifyEditorStatusChanged();
	}

	public boolean getEnableTransformKeys() {
		return this.enableTransformKeys;
	}

	public void setEnableTransformKeys(boolean enableTransformKeys) {
		this.enableTransformKeys = enableTransformKeys;
		notifyEditorStatusChanged();
	}

	public boolean isLittleEndian() {
		return this.littleEndian;
	}

	public void setLittleEndian(boolean littleEndian) {
		this.littleEndian = littleEndian;
		notifyEditorStatusChanged();
	}

	public long getMark() {
		return this.mark;
	}

	public void setMark(long mark) {
		this.mark = mark;
		notifyEditorStatusChanged();
	}

	public void addHexEditorListener(JHexEditorListener listener) {
		listeners.add(listener);
	}

	public void removeHexEditorListener(JHexEditorListener listener) {
		listeners.remove(listener);
	}

	private void notifyEditorStatusChanged() {
		for (JHexEditorListener l : listeners) {
			l.editorStatusChanged(this);
		}
	}
	// CONVENIENCE METHODS

	public ByteBuffer getByteBuffer() {
		return document.getByteBuffer();
	}

	public boolean isEmpty() {
		return document.isEmpty();
	}

	public long length() {
		return document.length();
	}

	public boolean get(long offset, byte[] dst, int dstOffset, int length) {
		return document.get(offset, dst, dstOffset, length);
	}

	public ByteBuffer slice(long offset, long length) {
		return document.slice(offset, length);
	}

	public boolean write(OutputStream out, long offset, long length) throws IOException {
		return document.write(out, offset, length);
	}

	public long indexOf(byte[] pattern) {
		return document.indexOf(pattern);
	}

	public long indexOf(byte[] pattern, long index) {
		return document.indexOf(pattern, index);
	}

	public long lastIndexOf(byte[] pattern) {
		return document.lastIndexOf(pattern);
	}

	public long lastIndexOf(byte[] pattern, long index) {
		return document.lastIndexOf(pattern, index);
	}

	public ByteBufferSelectionModel getSelectionModel() {
		return document.getSelectionModel();
	}

	public long getSelectionStart() {
		return document.getSelectionStart();
	}

	public long getSelectionEnd() {
		return document.getSelectionEnd();
	}

	public long getSelectionMin() {
		return document.getSelectionMin();
	}

	public long getSelectionMax() {
		return document.getSelectionMax();
	}

	public long getSelectionLength() {
		return document.getSelectionLength();
	}

	public void setSelectionStart(long start) {
		document.setSelectionStart(start);
	}

	public void setSelectionEnd(long end) {
		document.setSelectionEnd(end);
	}

	public void setSelectionRange(long start, long end) {
		document.setSelectionRange(start, end);
	}

	public void selectAll() {
		document.selectAll();
	}

	public ByteBufferHistory getHistory() {
		return document.getHistory();
	}

	public boolean canUndo() {
		return !readOnly && document.canUndo();
	}

	public boolean canRedo() {
		return !readOnly && document.canRedo();
	}

	public String getUndoActionName() {
		return readOnly ? null : document.getUndoActionName();
	}

	public String getRedoActionName() {
		return readOnly ? null : document.getRedoActionName();
	}

	public void undo() {
		if (!readOnly) {
			document.undo();
		}
	}

	public void redo() {
		if (!readOnly) {
			document.redo();
		}
	}

	public void clearHistory() {
		document.clearHistory();
	}

	public byte[] getSelection() {
		return document.getSelection();
	}

	public String getSelectionAsHex() {
		return document.getSelectionAsHex();
	}

	public String getSelectionAsString() {
		return document.getSelectionAsString(charset);
	}

	public boolean deleteSelection(String actionName) {
		return !readOnly && document.deleteSelection(actionName);
	}

	public boolean replaceSelection(String actionName, byte[] data, boolean keepSelected) {
		return !readOnly && document.replaceSelection(actionName, data, keepSelected);
	}

	public boolean transformSelection(ByteTransform tx) {
		return !readOnly && document.transformSelection(tx);
	}

	public boolean replaceAll(byte[] pattern, byte[] replacement) {
		return !readOnly && document.replaceAll(pattern, replacement);
	}

	// EDIT COMMANDS
	public boolean cut() {
		return textActive ? cutAsString() : cutAsHex();
	}

	public boolean cutAsHex() {
		return !readOnly && document.cutAsHex();
	}

	public boolean cutAsString() {
		return !readOnly && document.cutAsString(charset);
	}

	public boolean copy() {
		return textActive ? copyAsString() : copyAsHex();
	}

	public boolean copyAsHex() {
		return document.copyAsHex();
	}

	public boolean copyAsString() {
		return document.copyAsString(charset);
	}

	public boolean copyOffset() {
		return document.copyOffset(decimalAddresses);
	}

	public boolean paste() {
		return textActive ? pasteAsString() : pasteAsHex();
	}

	public boolean pasteAsHex() {
		return !readOnly && document.pasteAsHex();
	}

	public boolean pasteAsString() {
		return !readOnly && document.pasteAsString(charset);
	}

	public String getSelectionRange() {
		long ss = document.getSelectionStart();
		long se = document.getSelectionEnd();
		String sss = decimalAddresses ? Long.toString(ss) : Long.toHexString(ss);
		String ses = decimalAddresses ? Long.toString(se) : Long.toHexString(se);
		return (ss == se) ? sss : (sss + ":" + ses);
	}

	public void setSelectionRange(String range) {
		try {
			String[] rangeArray = range.split(":", 2);
			String sss = rangeArray[0].trim();
			String ses = (rangeArray.length > 1) ? rangeArray[1].trim() : sss;
			long ss;
			long se;

			if (sss.startsWith("0x") || sss.startsWith("0X")) {
				ss = Long.parseLong(sss.substring(2), 16);
			} else if (sss.startsWith("$")) {
				ss = Long.parseLong(sss.substring(1), 16);
			} else if (sss.startsWith("#")) {
				ss = Long.parseLong(sss.substring(1), 10);
			} else {
				ss = Long.parseLong(sss, decimalAddresses ? 10 : 16);
			}

			if (ses.startsWith("0x") || ses.startsWith("0X")) {
				se = Long.parseLong(ses.substring(2), 16);
			} else if (ses.startsWith("$")) {
				se = Long.parseLong(ses.substring(1), 16);
			} else if (ses.startsWith("#")) {
				se = Long.parseLong(ses.substring(1), 10);
			} else {
				se = Long.parseLong(ses, decimalAddresses ? 10 : 16);
			}

			document.setSelectionRange(ss, se);
		} catch (Exception ignored) {
		}
	}

	public void showSetSelectionDialog(Component component, String title) {
		Object o = JOptionPane.showInputDialog(
				component, "Enter address range:", title,
				JOptionPane.QUESTION_MESSAGE, null, null,
				getSelectionRange());
		if (o != null) {
			setSelectionRange(o.toString());
		}
	}

	public boolean isSelectionExists() {
		return document.getSelectionLength() > 0;
	}

	private void createActions() {
		cutAction = new JMenuItem("Cut");
		cutAction.addActionListener(e -> cut());

		copyAction = new JMenuItem("Copy");
		copyAction.addActionListener(e -> copy());

		copyHexAction = new JMenuItem("Copy as Hex");
		copyHexAction.addActionListener(e -> copyAsHex());

		copyStringAction = new JMenuItem("Copy as String");
		copyStringAction.addActionListener(e -> copyAsString());

		pasteAction = new JMenuItem("Paste");
		pasteAction.addActionListener(e -> paste());

		pasteHexAction = new JMenuItem("Paste as Hex");
		pasteHexAction.addActionListener(e -> pasteAsHex());

		pasteStringAction = new JMenuItem("Paste as String");
		pasteStringAction.addActionListener(e -> pasteAsString());

		deleteAction = new JMenuItem("Delete");
		deleteAction.addActionListener(e -> {
			if (!isReadOnly()) {
				document.deleteSelection("Delete");
			}
		});

		selectAllAction = new JMenuItem("Select All");
		selectAllAction.addActionListener(e -> selectAll());

		undoAction = new JMenuItem("Undo");
		undoAction.addActionListener(e -> undo());

		redoAction = new JMenuItem("Redo");
		redoAction.addActionListener(e -> redo());

		copyOffsetItem = new JMenuItem("Copy Offset");
		copyOffsetItem.addActionListener(e -> copyOffset());
	}

	private void createPopupMenu() {
		boolean isEditable = !isReadOnly();
		popupMenu = new JPopupMenu();
		popupMenu.add(copyAction);

		if (isEditable) {
			popupMenu.add(cutAction);
			popupMenu.add(pasteAction);
			popupMenu.add(deleteAction);
			popupMenu.addSeparator();
			popupMenu.add(undoAction);
			popupMenu.add(redoAction);
			popupMenu.addSeparator();
		}

		JMenu copyMenu = new JMenu("Copy As...");
		copyMenu.add(copyHexAction);
		copyMenu.add(copyStringAction);
		popupMenu.add(copyMenu);

		popupMenu.add(copyOffsetItem);
		if (isEditable) {
			JMenu pasteMenu = new JMenu("Paste As...");
			pasteMenu.add(pasteHexAction);
			pasteMenu.add(pasteStringAction);
			popupMenu.add(pasteMenu);

			popupMenu.addSeparator();
		}
		popupMenu.add(selectAllAction);
	}

	private void updatePopupActionStates() {

		boolean selectionExists = isSelectionExists();
		boolean isEditable = !isReadOnly();
		boolean canUndo = canUndo();
		boolean canRedo = canRedo();

		cutAction.setEnabled(isEditable && selectionExists);
		copyAction.setEnabled(selectionExists);
		copyHexAction.setEnabled(selectionExists);
		copyStringAction.setEnabled(selectionExists);
		deleteAction.setEnabled(isEditable && selectionExists);
		copyOffsetItem.setEnabled(selectionExists);

		boolean canPaste = false;
		try {
			canPaste = Toolkit.getDefaultToolkit().getSystemClipboard()
					.isDataFlavorAvailable(DataFlavor.stringFlavor);
		} catch (Exception ignored) {

		}
		pasteAction.setEnabled(isEditable && canPaste);
		pasteHexAction.setEnabled(isEditable && canPaste);
		pasteStringAction.setEnabled(isEditable && canPaste);

		selectAllAction.setEnabled(length() > 0);
		undoAction.setEnabled(isEditable && canUndo);

		String undoName = getUndoActionName();
		undoAction.setText(undoName != null ? "Undo " + undoName : "Undo");

		redoAction.setEnabled(isEditable && canRedo);
		String redoName = getRedoActionName();
		redoAction.setText(redoName != null ? "Redo " + redoName : "Redo");
	}

	public PointInfo getInfoAtPoint(int x, int y) {
		Insets i = getInsets();
		int w = getWidth() - i.left - i.right;

		FontMetrics fm = getFontMetrics(getFont());
		int ch = fm.getHeight() + 2;
		int cw = fm.stringWidth(HEX_ALPHABET) / 16;
		int bpr = (w - 11 * cw) / (4 * cw);
		if (bpr < 1) {
			return null;
		}
		if (bpr > 4) {
			bpr = 4 * (bpr / 4);
		}

		int hax = i.left + cw * 9;
		int tax = i.left + cw * (bpr * 3 + 10);

		long length = document.length();
		long offset = ((long) ((y - i.top) / ch) * (long) bpr);
		if (offset < 0) {
			offset = 0;
		}
		if (offset > length) {
			offset = length;
		}
		if (x < hax) {
			return new PointInfo(true, false, false, offset, length, bpr);
		}
		if (y >= i.top) {
			int off = ((x < tax) ? ((x - hax + cw) / (cw * 3)) : ((x - tax - cw / 2) / cw));
			if (off < 0) {
				off = 0;
			}
			if (off > bpr) {
				off = bpr;
			}
			offset += off;
			if (offset > length) {
				offset = length;
			}
		}
		return new PointInfo(false, x < tax, x >= tax, offset, length, bpr);
	}

	public Rectangle getRectForOffset(long offset) {
		Insets i = getInsets();
		int w = getWidth() - i.left - i.right;

		FontMetrics fm = getFontMetrics(getFont());
		int ch = fm.getHeight() + 2;
		int cw = fm.stringWidth(HEX_ALPHABET) / 16;
		int bpr = (w - 11 * cw) / (4 * cw);
		if (bpr < 1) {
			return null;
		}
		if (bpr > 4) {
			bpr = 4 * (bpr / 4);
		}

		int hax = i.left + cw * 9;
		int tax = i.left + cw * (bpr * 3 + 10);

		int y = (int) Math.min(((offset / bpr) * ch) + i.top, Integer.MAX_VALUE);
		int x = (int) ((offset % bpr) * cw);
		if (textActive) {
			return new Rectangle(tax + cw + x, y, 2, ch);
		} else {
			return new Rectangle(hax + cw / 2 + x * 3, y, 2, ch);
		}
	}

	public Rectangle getRectForOffsetLength(long offset, long length) {
		Insets i = getInsets();
		int w = getWidth() - i.left - i.right;

		FontMetrics fm = getFontMetrics(getFont());
		int ch = fm.getHeight() + 2;
		int cw = fm.stringWidth(HEX_ALPHABET) / 16;
		int bpr = (w - 11 * cw) / (4 * cw);
		if (bpr < 1) {
			return null;
		}
		if (bpr > 4) {
			bpr = 4 * (bpr / 4);
		}

		int hax = i.left + cw * 9;
		int tax = i.left + cw * (bpr * 3 + 10);

		int y1 = (int) Math.min(((offset / bpr) * ch) + i.top, Integer.MAX_VALUE);
		int x1 = (int) ((offset % bpr) * cw);
		offset += length;
		int y2 = (int) Math.min(((offset / bpr) * ch) + i.top, Integer.MAX_VALUE);
		int x2 = (int) ((offset % bpr) * cw);

		int y = Math.min(y1, y2);
		int x = Math.min(x1, x2);
		int bh = Math.max(y1, y2) - y;
		int bw = Math.max(x1, x2) - x;
		if (textActive) {
			return new Rectangle(tax + cw + x, y, bw + 2, bh + ch);
		} else {
			return new Rectangle(hax + cw / 2 + x * 3, y, bw + 2, bh + ch);
		}
	}

	// SIZING AND SCROLLING
	public int getMinimumBytesPerRow() {
		return this.minimumBytesPerRow;
	}

	public void setMinimumBytesPerRow(int bpr) {
		this.minimumBytesPerRow = bpr;
	}

	public int getMinimumRowCount() {
		return this.minimumRowCount;
	}

	public void setMinimumRowCount(int rows) {
		this.minimumRowCount = rows;
	}

	public int getPreferredBytesPerRow() {
		return this.preferredBytesPerRow;
	}

	public void setPreferredBytesPerRow(int bpr) {
		this.preferredBytesPerRow = bpr;
	}

	public int getPreferredRowCount() {
		return this.preferredRowCount;
	}

	public void setPreferredRowCount(int rows) {
		this.preferredRowCount = rows;
	}

	@Override
	public Dimension getMinimumSize() {
		if (minimumSize != null) {
			return minimumSize;
		}
		Insets i = getInsets();
		FontMetrics fm = getFontMetrics(getFont());
		int ch = fm.getHeight() + 2;
		int cw = fm.stringWidth(HEX_ALPHABET) / 16;
		int minimumWidth = cw * (minimumBytesPerRow * 4 + 12) + i.left + i.right;
		int minimumHeight = ch * minimumRowCount + i.top + i.bottom;
		return new Dimension(minimumWidth, minimumHeight);
	}

	@Override
	public void setMinimumSize(Dimension minimumSize) {
		this.minimumSize = minimumSize;
	}

	@Override
	public Dimension getPreferredSize() {
		if (preferredSize != null) {
			return preferredSize;
		}
		Insets i = getInsets();
		FontMetrics fm = getFontMetrics(getFont());
		int ch = fm.getHeight() + 2;
		int cw = fm.stringWidth(HEX_ALPHABET) / 16;
		int minimumHeight = ch * minimumRowCount + i.top + i.bottom;
		int preferredWidth;
		int bpr;

		Container parent = getParent();
		if (parent instanceof JViewport) {
			int h = parent.getHeight();
			if (h > minimumHeight) {
				minimumHeight = h;
			}
			preferredWidth = parent.getWidth();
			int w = preferredWidth - i.left - i.right;
			bpr = (w - 11 * cw) / (4 * cw);
			if (bpr < 1) {
				bpr = 1;
			}
			if (bpr > 4) {
				bpr = 4 * (bpr / 4);
			}
		} else {
			preferredWidth = cw * (preferredBytesPerRow * 4 + 12) + i.left + i.right;
			bpr = preferredBytesPerRow;
		}

		long length = document.length();
		long preferredHeight = ch * ((length + bpr - 1) / bpr) + i.top + i.bottom;
		if (preferredHeight < minimumHeight) {
			preferredHeight = minimumHeight;
		}
		if (preferredHeight > Integer.MAX_VALUE) {
			preferredHeight = Integer.MAX_VALUE;
		}
		return new Dimension(preferredWidth, (int) preferredHeight);
	}

	@Override
	public void setFont(Font font) {
		super.setFont(font);
		revalidate();
		repaint();
	}

	@Override
	public void setPreferredSize(Dimension preferredSize) {
		this.preferredSize = preferredSize;
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		Insets i = getInsets();
		FontMetrics fm = getFontMetrics(getFont());
		int ch = fm.getHeight() + 2;
		int cw = fm.stringWidth(HEX_ALPHABET) / 16;
		int preferredWidth = cw * (preferredBytesPerRow * 4 + 12) + i.left + i.right;
		int preferredHeight = ch * preferredRowCount + i.top + i.bottom;
		return new Dimension(preferredWidth, preferredHeight);
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle vr, int or, int dir) {
		FontMetrics fm = getFontMetrics(getFont());
		return fm.getHeight() + 2;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle vr, int or, int dir) {
		FontMetrics fm = getFontMetrics(getFont());
		int ch = fm.getHeight() + 2;
		return ch * (vr.height / ch);
	}

	// RENDERING
	@Override
	protected void paintComponent(Graphics g) {
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}

		Insets i = getInsets();
		int fw = getWidth();
		int fh = getHeight();
		int w = fw - i.left - i.right;
		int h = fh - i.top - i.bottom;
		Rectangle vr = getVisibleRect();

		g.setFont(getFont());
		FontMetrics fm = g.getFontMetrics();
		int ca = fm.getAscent() + 1;
		int ch = fm.getHeight() + 2;
		int cw = fm.stringWidth(HEX_ALPHABET) / 16;

		int bpr = (w - 11 * cw) / (4 * cw);
		if (bpr < 1) {
			return;
		}
		if (bpr > 4) {
			bpr = 4 * (bpr / 4);
		}
		byte[] data = new byte[bpr];

		// Background
		int ay = extendBorders ? 0 : i.top;
		int ah = extendBorders ? fh : h;
		// Address Area
		int aax = extendBorders ? 0 : i.left;
		int aaw = extendBorders ? (cw * 9 + i.left) : (cw * 9);
		g.setColor(colors.addressAreaEven);
		g.fillRect(aax, ay, aaw, ah);
		// Hex Area
		int hax = i.left + cw * 9;
		int haw = cw * (bpr * 3 + 1);
		g.setColor(colors.hexAreaEven);
		g.fillRect(hax, ay, haw, ah);
		// Text Area
		int tax = i.left + cw * (bpr * 3 + 10);
		int taw = extendBorders ? (fw - tax) : (fw - tax - i.right);
		g.setColor(colors.textAreaEven);
		g.fillRect(tax, ay, taw, ah);

		// Body
		long ss = document.getSelectionMin();
		long se = document.getSelectionMax();
		long length = document.length();
		int miny = vr.y - ch;
		int maxy = vr.y + vr.height;
		int startRow = (vr.y - i.top) / ch;
		if (startRow < 0) {
			startRow = 0;
		}
		long offset = (long) startRow * (long) bpr;
		boolean odd = ((startRow & 1) != 0);
		int ry = i.top + startRow * ch;
		int ty = ry + ca;
		while (offset < length && ry >= miny && ry < maxy) {
			// Background
			if (odd) {
				// Address Area
				g.setColor(colors.addressAreaOdd);
				g.fillRect(aax, ry, aaw, ch);
				// Hex Area
				g.setColor(colors.hexAreaOdd);
				g.fillRect(hax, ry, haw, ch);
				// Text Area
				g.setColor(colors.textAreaOdd);
				g.fillRect(tax, ry, taw, ch);
			}
			// Highlight
			if (ss != se && ss < offset + bpr && se > offset) {
				int s = (int) Math.max(ss - offset, 0);
				int e = (int) Math.min(se - offset, bpr);
				int hs = i.left + cw * (s * 3 + 9) + cw / 2;
				int he = i.left + cw * (e * 3 + 9) + cw / 2;
				int ds = i.left + cw * (bpr * 3 + 11 + s);
				int de = i.left + cw * (bpr * 3 + 11 + e);
				g.setColor(hexHighlightColor(odd));
				g.fillRect(hs, ry, he - hs, ch);
				g.setColor(textHighlightColor(odd));
				g.fillRect(ds, ry, de - ds, ch);
			}
			// Address
			String addressString = document.addressString(offset, decimalAddresses) + ":";
			int asx = i.left + cw * 9 - fm.stringWidth(addressString);
			g.setColor(odd ? colors.addressTextOdd : colors.addressTextEven);
			g.drawString(addressString, asx, ty);
			// Data
			document.get(offset, data, 0, (int) Math.min(bpr, length - offset));
			int idx = 0;
			long off = offset;
			int hx = i.left + cw * 10;
			int dx = i.left + cw * (bpr * 3 + 11);
			while (idx < bpr && off < length) {
				boolean sel = (off >= ss && off < se);
				int byteVal = data[idx] & 0xFF;
				g.setColor(hexTextColor(sel, odd));
				g.drawString(String.format("%02X", byteVal), hx, ty);
				g.setColor(textTextColor(sel, odd, charsetPrintable[byteVal]));
				g.drawString(charsetStrings[byteVal], dx, ty);
				idx++;
				off++;
				hx += cw * 3;
				dx += cw;
			}
			offset += bpr;
			odd = !odd;
			ry += ch;
			ty += ch;
		}

		// Address Divider
		g.setColor(colors.addressDivider);
		g.fillRect(hax, ay, 1, ah);
		// Hex Dividers
		g.setColor(colors.hexDivider);
		for (int j = 4; j < bpr; j += 4) {
			g.fillRect(i.left + cw * (j * 3 + 9) + cw / 2, ay, 1, ah);
		}
		// Text Divider
		g.setColor(colors.textDivider);
		g.fillRect(tax, ay, 1, ah);

		// Cursor
		if (ss == se) {
			offset = 0;
			ry = i.top;
			while (offset < length) {
				if (ss == offset) {
					int hs = i.left + cw * 9 + cw * 3 / 4;
					int ds = i.left + cw * (bpr * 3 + 11);
					g.setColor(hexCursorColor());
					g.fillRect(hs, ry, 2, ch);
					g.setColor(textCursorColor());
					g.fillRect(ds, ry, 2, ch);
				} else if (ss == offset + bpr) {
					int hs = i.left + cw * (bpr * 3 + 9) + cw / 4;
					int ds = i.left + cw * (bpr * 4 + 11);
					g.setColor(hexCursorColor());
					g.fillRect(hs, ry, 2, ch);
					g.setColor(textCursorColor());
					g.fillRect(ds, ry, 2, ch);
				} else if (ss > offset && ss < offset + bpr) {
					int s = (int) (ss - offset);
					int hs = i.left + cw * (s * 3 + 9) + cw / 2;
					int ds = i.left + cw * (bpr * 3 + 11 + s);
					g.setColor(hexCursorColor());
					g.fillRect(hs, ry, 2, ch);
					g.setColor(textCursorColor());
					g.fillRect(ds, ry, 2, ch);
				}
				offset += bpr;
				ry += ch;
			}
			if (ss == offset) {
				int hs = i.left + cw * 9 + cw * 3 / 4;
				int ds = i.left + cw * (bpr * 3 + 11);
				g.setColor(hexCursorColor());
				g.fillRect(hs, ry, 2, ch);
				g.setColor(textCursorColor());
				g.fillRect(ds, ry, 2, ch);
			}
		}
	}

	private Color hexCursorColor() {
		if (isFocusOwner() && !textActive) {
			return document.isMidByte() ? colors.activeCursorMidByte : colors.activeCursor;
		}
		return document.isMidByte() ? colors.inactiveCursorMidByte : colors.inactiveCursor;
	}

	private Color hexHighlightColor(boolean odd) {
		if (isFocusOwner() && !textActive) {
			return odd ? colors.hexAreaActiveHighlightOdd : colors.hexAreaActiveHighlightEven;
		}
		return odd ? colors.hexAreaInactiveHighlightOdd : colors.hexAreaInactiveHighlightEven;
	}

	private Color hexTextColor(boolean sel, boolean odd) {
		if (sel && isFocusOwner() && !textActive) {
			return odd ? colors.hexTextActiveHighlightOdd : colors.hexTextActiveHighlightEven;
		}
		if (sel) {
			return odd ? colors.hexTextInactiveHighlightOdd : colors.hexTextInactiveHighlightEven;
		}
		return odd ? colors.hexTextOdd : colors.hexTextEven;
	}

	private Color textCursorColor() {
		if (isFocusOwner() && textActive) {
			return document.isMidByte() ? colors.activeCursorMidByte : colors.activeCursor;
		}
		return document.isMidByte() ? colors.inactiveCursorMidByte : colors.inactiveCursor;
	}

	private Color textHighlightColor(boolean odd) {
		if (isFocusOwner() && textActive) {
			return odd ? colors.textAreaActiveHighlightOdd : colors.textAreaActiveHighlightEven;
		}
		return odd ? colors.textAreaInactiveHighlightOdd : colors.textAreaInactiveHighlightEven;
	}

	private Color textTextColor(boolean sel, boolean odd, boolean printable) {
		if (printable) {
			if (sel && isFocusOwner() && textActive) {
				return odd ? colors.textPrintableActiveHighlightOdd : colors.textPrintableActiveHighlightEven;
			}
			if (sel) {
				return odd ? colors.textPrintableInactiveHighlightOdd : colors.textPrintableInactiveHighlightEven;
			}
			return odd ? colors.textPrintableOdd : colors.textPrintableEven;
		} else {
			if (sel && isFocusOwner() && textActive) {
				return odd ? colors.textUnprintableActiveHighlightOdd : colors.textUnprintableActiveHighlightEven;
			}
			if (sel) {
				return odd ? colors.textUnprintableInactiveHighlightOdd : colors.textUnprintableInactiveHighlightEven;
			}
			return odd ? colors.textUnprintableOdd : colors.textUnprintableEven;
		}
	}

	private void makeCharset() {
		byte[] tmp = new byte[1];
		String replacementCharacterString = Character.toString(0xFFFD);
		for (int i = 0; i < 256; i++) {
			tmp[0] = (byte) i;
			String dataString;
			try {
				dataString = new String(tmp, charset);
			} catch (Exception e) {
				dataString = replacementCharacterString;
			}
			if (dataString.contains(replacementCharacterString)) {
				charsetPrintable[i] = false;
				charsetStrings[i] = ".";
			} else {
				char[] chars = dataString.toCharArray();
				charsetPrintable[i] = !shiftControl(chars);
				charsetStrings[i] = new String(chars);
			}
		}
	}

	private boolean shiftControl(char[] chars) {
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] < 0x20) {
				chars[i] += 0x40;
				return true;
			}
			if (chars[i] == 0x7F) {
				chars[i] = '?';
				return true;
			}
			if (chars[i] >= 0x80 && chars[i] < 0x9F) {
				chars[i] -= 0x20;
				return true;
			}
			if (chars[i] == 0x9F) {
				chars[i] = '#';
				return true;
			}
		}
		return false;
	}

	private void arrowKey(int dir, boolean shiftKey) {
		long length = document.length();
		long offset = document.getSelectionEnd() + dir;
		if (offset < 0) {
			offset = 0;
		}
		if (offset > length) {
			offset = length;
		}
		if (shiftKey) {
			document.setSelectionEnd(offset);
		} else {
			document.setSelectionRange(offset, offset);
		}
	}

	private int getRowWidth() {
		Insets i = getInsets();
		int w = getWidth() - i.left - i.right;
		FontMetrics fm = getFontMetrics(getFont());
		int cw = fm.stringWidth(HEX_ALPHABET) / 16;
		int bpr = (w - 11 * cw) / (4 * cw);
		if (bpr < 1) {
			return 0;
		}
		if (bpr > 4) {
			bpr = 4 * (bpr / 4);
		}
		return bpr;
	}

	private int getPageSize() {
		Insets i = getInsets();
		int w = getWidth() - i.left - i.right;
		FontMetrics fm = getFontMetrics(getFont());
		int ch = fm.getHeight() + 2;
		int cw = fm.stringWidth(HEX_ALPHABET) / 16;
		int bpr = (w - 11 * cw) / (4 * cw);
		if (bpr < 1) {
			return 0;
		}
		if (bpr > 4) {
			bpr = 4 * (bpr / 4);
		}
		int rows = getVisibleRect().height / ch;
		if (rows < 1) {
			rows = 1;
		}
		return bpr * rows;
	}

	// POINT/OFFSET TRANSLATION
	public static final class PointInfo {
		public final boolean inAddressArea;
		public final boolean inHexArea;
		public final boolean inTextArea;
		public final long offset;
		public final long length;
		public final int rowWidth;

		private PointInfo(boolean aa, boolean ha, boolean ta, long offset, long length, int bpr) {
			this.inAddressArea = aa;
			this.inHexArea = ha;
			this.inTextArea = ta;
			this.offset = offset;
			this.length = length;
			this.rowWidth = bpr;
		}
	}
}
