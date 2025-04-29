package jadx.gui.ui.hexeditor.editor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jadx.gui.ui.hexeditor.buffer.BitTransform;
import jadx.gui.ui.hexeditor.buffer.ByteBuffer;
import jadx.gui.ui.hexeditor.buffer.ByteBufferDocument;
import jadx.gui.ui.hexeditor.buffer.ByteBufferSelectionModel;
import jadx.gui.ui.hexeditor.buffer.FloatFormat;
import jadx.gui.ui.hexeditor.buffer.IncrementTransform;
import jadx.gui.ui.hexeditor.buffer.ReverseTransform;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public class JHexEditorInspector extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int MAX_LENGTH = 256;

	private final JHexEditor parent;
	private final JTextField binField;
	private final JTextField octField;
	private final JTextField hexField;
	private final JTextField signedField;
	private final JTextField unsignedField;
	private final JTextField fixedField;
	private final JTextField floatField;
	private boolean inputLock = false;

	public JHexEditorInspector(JHexEditor parent) {
		this.parent = parent;
		this.parent.addHexEditorListener(editorListener);
		this.addComponentListener(componentListener);

		JLabel binLabel = new JLabel("Bin:");
		JLabel octLabel = new JLabel("Oct:");
		JLabel hexLabel = new JLabel("Hex:");
		JLabel signedLabel = new JLabel("Signed:");
		JLabel unsignedLabel = new JLabel("Unsigned:");
		JLabel fixedLabel = new JLabel("Fixed:");
		JLabel floatLabel = new JLabel("Float:");

		binField = new JTextField();
		octField = new JTextField();
		hexField = new JTextField();
		signedField = new JTextField();
		unsignedField = new JTextField();
		fixedField = new JTextField();
		floatField = new JTextField();

		JPanel binPanel = labelField(binLabel, binField);
		JPanel octPanel = labelField(octLabel, octField);
		JPanel hexPanel = labelField(hexLabel, hexField);
		JPanel signedPanel = labelField(signedLabel, signedField);
		JPanel unsignedPanel = labelField(unsignedLabel, unsignedField);
		JPanel fixedPanel = labelField(fixedLabel, fixedField);
		JPanel floatPanel = labelField(floatLabel, floatField);

		JPanel leftPanel = gridLayout(0, 1, octPanel, hexPanel);
		JPanel centerPanel = gridLayout(0, 1, signedPanel, unsignedPanel);
		JPanel rightPanel = gridLayout(0, 1, fixedPanel, floatPanel);
		JPanel bottomPanel = gridLayout(1, 0, leftPanel, centerPanel, rightPanel);

		setLayout(new BorderLayout(8, 8));
		add(binPanel, BorderLayout.NORTH);
		add(bottomPanel, BorderLayout.CENTER);
		setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

		fixLabels(binLabel, octLabel, hexLabel);
		fixLabels(signedLabel, unsignedLabel);
		fixLabels(fixedLabel, floatLabel);

		new FieldListener(binField) {
			@Override
			public BigInteger parse(String text, int length) {
				try {
					return new BigInteger(text.trim(), 2);
				} catch (Exception e) {
					return null;
				}
			}
		};
		new FieldListener(octField) {
			@Override
			public BigInteger parse(String text, int length) {
				try {
					return new BigInteger(text.trim(), 8);
				} catch (Exception e) {
					return null;
				}
			}
		};
		new FieldListener(hexField) {
			@Override
			public BigInteger parse(String text, int length) {
				try {
					return new BigInteger(text.trim(), 16);
				} catch (Exception e) {
					return null;
				}
			}
		};
		new FieldListener(signedField) {
			@Override
			public BigInteger parse(String text, int length) {
				try {
					return new BigInteger(text.trim(), 10);
				} catch (Exception e) {
					return null;
				}
			}
		};
		new FieldListener(unsignedField) {
			@Override
			public BigInteger parse(String text, int length) {
				try {
					return new BigInteger(text.trim(), 10);
				} catch (Exception e) {
					return null;
				}
			}
		};
		new FieldListener(fixedField) {
			@Override
			public BigInteger parse(String text, int length) {
				try {
					BigDecimal fixedValue = new BigDecimal(text.trim());
					BigDecimal fixedBase = new BigDecimal(BigInteger.ONE.shiftLeft(length * 4));
					return fixedValue.multiply(fixedBase).toBigInteger();
				} catch (Exception e) {
					return null;
				}
			}
		};
		new FieldListener(floatField) {
			@Override
			public BigInteger parse(String text, int length) {
				try {
					return stringToFloatingPointBits(length, text.trim());
				} catch (Exception e) {
					return null;
				}
			}
		};
	}

	private void setFieldsEnabled(boolean enabled) {
		binField.setEditable(enabled);
		octField.setEditable(enabled);
		hexField.setEditable(enabled);
		signedField.setEditable(enabled);
		unsignedField.setEditable(enabled);
		fixedField.setEditable(enabled);
		floatField.setEditable(enabled);
		binField.setEnabled(enabled);
		octField.setEnabled(enabled);
		hexField.setEnabled(enabled);
		signedField.setEnabled(enabled);
		unsignedField.setEnabled(enabled);
		fixedField.setEnabled(enabled);
		floatField.setEnabled(enabled);
	}

	private void clearFields(JTextField src) {
		inputLock = true;
		if (src != binField)
			binField.setText("");
		if (src != octField)
			octField.setText("");
		if (src != hexField)
			hexField.setText("");
		if (src != signedField)
			signedField.setText("");
		if (src != unsignedField)
			unsignedField.setText("");
		if (src != fixedField)
			fixedField.setText("");
		if (src != floatField)
			floatField.setText("");
		inputLock = false;
	}

	private void setFieldValues(JTextField src, BigInteger value, int length) {
		if (value == null || length <= 0 || length > MAX_LENGTH) {
			clearFields(src);
		} else {
			BigInteger uMask = BigInteger.ONE.shiftLeft(length * 8).subtract(BigInteger.ONE);
			BigInteger sMask = BigInteger.ONE.negate().shiftLeft(length * 8);
			BigInteger uValue = uMask.and(value);
			BigInteger sValue = value.testBit(length * 8 - 1) ? sMask.or(value) : uValue;
			inputLock = true;
			if (src != binField)
				binField.setText(uValue.toString(2));
			if (src != octField)
				octField.setText(uValue.toString(8));
			if (src != hexField)
				hexField.setText(uValue.toString(16).toUpperCase());
			if (src != signedField)
				signedField.setText(sValue.toString());
			if (src != unsignedField)
				unsignedField.setText(uValue.toString());
			if (src != fixedField) {
				BigDecimal fixedBase = new BigDecimal(BigInteger.ONE.shiftLeft(length * 4));
				BigDecimal fixedValue = new BigDecimal(sValue).divide(fixedBase);
				fixedField.setText(fixedValue.toString());
			}
			if (src != floatField) {
				floatField.setText(floatingPointBitsToString(length, value));
			}
			inputLock = false;
		}
	}

	private void pullFromEditor(JTextField src) {
		byte[] data = parent.getSelection();
		if (data == null || data.length == 0 || data.length > MAX_LENGTH) {
			setFieldsEnabled(false);
			clearFields(src);
		} else {
			setFieldsEnabled(true);
			if (parent.isLittleEndian())
				ReverseTransform.BYTES.transform(data, 0, data.length);
			setFieldValues(src, new BigInteger(data), data.length);
		}
	}

	private void pushToEditor(JTextField src, BigInteger value) {
		long length = parent.getSelectionLength();
		if (length <= 0 || length > MAX_LENGTH) {
			clearFields(src);
		} else if (value == null) {
			pullFromEditor(src);
		} else {
			byte[] data = extend(value.toByteArray(), (int) length);
			if (parent.isLittleEndian())
				ReverseTransform.BYTES.transform(data, 0, data.length);
			inputLock = true;
			parent.replaceSelection("Replace Value", data, true);
			setFieldValues(src, value, (int) length);
			inputLock = false;
		}
	}

	private abstract class FieldListener implements DocumentListener, FocusListener, KeyListener {
		private final JTextField field;
		private boolean changed;

		public FieldListener(JTextField field) {
			this.field = field;
			this.field.getDocument().addDocumentListener(this);
			this.field.addFocusListener(this);
			this.field.addKeyListener(this);
			this.changed = false;
		}

		public abstract BigInteger parse(String text, int length);

		@Override
		public void changedUpdate(DocumentEvent e) {
			if (inputLock || !isVisible())
				return;
			long length = parent.getSelectionLength();
			if (length <= 0 || length > MAX_LENGTH)
				return;
			BigInteger value = parse(field.getText(), (int) length);
			if (value != null)
				pushToEditor(field, value);
			changed = true;
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			changedUpdate(e);
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			changedUpdate(e);
		}

		@Override
		public void focusLost(FocusEvent e) {
			if (inputLock || !isVisible() || !changed)
				return;
			long length = parent.getSelectionLength();
			if (length <= 0 || length > MAX_LENGTH)
				return;
			BigInteger value = parse(field.getText(), (int) length);
			if (value == null)
				pullFromEditor(null);
			else
				pushToEditor(null, value);
			changed = false;
		}

		@Override
		public void focusGained(FocusEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
			switch (e.getKeyCode()) {
				case KeyEvent.VK_ENTER:
					focusLost(null);
					e.consume();
					break;
				case KeyEvent.VK_UP:
					parent.transformSelection(
							parent.isLittleEndian()
									? IncrementTransform.INC_LE
									: IncrementTransform.INC_BE);
					e.consume();
					break;
				case KeyEvent.VK_DOWN:
					parent.transformSelection(
							parent.isLittleEndian()
									? IncrementTransform.DEC_LE
									: IncrementTransform.DEC_BE);
					e.consume();
					break;
			}
		}

		@Override
		public void keyTyped(KeyEvent e) {
			switch (e.getKeyChar()) {
				case '~':
				case '`':
					parent.transformSelection(BitTransform.INVERT);
					e.consume();
					break;
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}
	}

	private final ComponentAdapter componentListener = new ComponentAdapter() {
		@Override
		public void componentHidden(ComponentEvent e) {
			setFieldsEnabled(false);
			clearFields(null);
		}

		@Override
		public void componentShown(ComponentEvent e) {
			pullFromEditor(null);
		}
	};

	private final JHexEditorListener editorListener = new JHexEditorListener() {
		@Override
		public void dataInserted(ByteBuffer buffer, long offset, int length) {
			// Not necessary?
		}

		@Override
		public void dataOverwritten(ByteBuffer buffer, long offset, int length) {
			// Not necessary?
		}

		@Override
		public void dataRemoved(ByteBuffer buffer, long offset, long length) {
			// Not necessary?
		}

		@Override
		public void selectionChanged(ByteBufferSelectionModel sm, long start, long end) {
			if (inputLock || !isVisible())
				return;
			pullFromEditor(null);
		}

		@Override
		public void documentChanged(JHexEditor editor, ByteBufferDocument document) {
			// Not necessary?
		}

		@Override
		public void colorsChanged(JHexEditor editor, JHexEditorColors colors) {
			// Not necessary?
		}

		@Override
		public void editorStatusChanged(JHexEditor editor) {
			if (inputLock || !isVisible())
				return;
			pullFromEditor(null);
		}
	};

	private static JPanel labelField(JLabel label, JTextField field) {
		JPanel p = new JPanel(new BorderLayout(8, 8));
		p.add(label, BorderLayout.LINE_START);
		p.add(field, BorderLayout.CENTER);
		return p;
	}

	private static JPanel gridLayout(int rows, int cols, JPanel... panels) {
		JPanel p = new JPanel(new GridLayout(rows, cols, 8, 8));
		for (JPanel panel : panels)
			p.add(panel);
		return p;
	}

	private static void fixLabels(JLabel... labels) {
		int width = 0, height = 0;
		for (JLabel label : labels) {
			label.setHorizontalAlignment(JLabel.TRAILING);
			Dimension d = label.getPreferredSize();
			if (d.width > width)
				width = d.width;
			if (d.height > height)
				height = d.height;
		}
		Dimension d = new Dimension(width, height);
		for (JLabel label : labels) {
			label.setMinimumSize(d);
			label.setPreferredSize(d);
			label.setMaximumSize(d);
		}
	}

	private static String floatingPointBitsToString(int bytes, BigInteger bits) {
		switch (bytes) {
			case 1:
				return Float.toString(FloatFormat.RESPL8.bitsToNumber(bits).floatValue());
			case 2:
				return Float.toString(FloatFormat.HALF.bitsToNumber(bits).floatValue());
			case 3:
				return Float.toString(FloatFormat.FP24.bitsToNumber(bits).floatValue());
			case 4:
				return Float.toString(Float.intBitsToFloat(bits.intValue()));
			case 6:
				return Double.toString(FloatFormat.RESPL48.bitsToNumber(bits).doubleValue());
			case 8:
				return Double.toString(Double.longBitsToDouble(bits.longValue()));
			default:
				return "";
		}
	}

	private static BigInteger stringToFloatingPointBits(int bytes, String s) {
		switch (bytes) {
			case 1:
				return FloatFormat.RESPL8.floatToBits(Float.parseFloat(s));
			case 2:
				return FloatFormat.HALF.floatToBits(Float.parseFloat(s));
			case 3:
				return FloatFormat.FP24.floatToBits(Float.parseFloat(s));
			case 4:
				return BigInteger.valueOf(Float.floatToRawIntBits(Float.parseFloat(s)));
			case 6:
				return FloatFormat.RESPL48.doubleToBits(Double.parseDouble(s));
			case 8:
				return BigInteger.valueOf(Double.doubleToRawLongBits(Double.parseDouble(s)));
			default:
				return null;
		}
	}

	private static byte[] extend(byte[] src, int dstLen) {
		int srcLen = src.length;
		if (srcLen == dstLen)
			return src;
		byte fill = (srcLen > 0 && src[0] < 0) ? (byte) (-1) : (byte) (0);
		byte[] dst = new byte[dstLen];
		while (dstLen > 0) {
			dstLen--;
			srcLen--;
			dst[dstLen] = (srcLen >= 0) ? src[srcLen] : fill;
		}
		return dst;
	}
}
