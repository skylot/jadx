package jadx.gui.ui.codearea;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang3.ArrayUtils;

public class HexConfigurationPanel extends JPanel {
	private final List<ValueFormatter> formatters = new ArrayList<>();

	private final HexAreaConfiguration config;

	private byte[] bytes = null;
	private Integer offset = null;

	private int row = 0;

	public HexConfigurationPanel(HexAreaConfiguration configuration) {
		this.config = configuration;

		setLayout(new GridBagLayout());
		addValueFormat("Signed 8 bit", 1, b -> Integer.toString(b.get()));
		addValueFormat("Unsigned 8 bit", 1, b -> Integer.toString(b.get() & 0xFF));
		addValueFormat("Signed 16 bit", 2, b -> Short.toString(b.getShort()));
		addValueFormat("Unsigned 16 bit", 2, b -> Integer.toString(b.getShort() & 0xFFFF));
		addValueFormat("Float 32 bit", 4, b -> Float.toString(b.getFloat()));
		addValueFormat("Signed 32 bit", 4, b -> Integer.toString(b.getInt()));
		addValueFormat("Unsigned 32 bit", 4, b -> Integer.toUnsignedString(b.getInt()));
		addValueFormat("Signed 64 bit", 8, b -> Long.toString(b.getLong()));
		addValueFormat("Float 64 bit", 8, b -> Double.toString(b.getDouble()));
		addValueFormat("Unsigned 64 bit", 8, b -> Long.toUnsignedString(b.getLong()));
		addValueFormat("Hexadecimal", 1, b -> Integer.toString(b.get(), 16));
		addValueFormat("Octal", 1, b -> Integer.toString(b.get(), 8));
		addValueFormat("Binary", 1, b -> Integer.toString(b.get(), 2));

		GridBagConstraints constraints;
		constraints = getConstraints();
		constraints.gridwidth = 2;
		JCheckBox littleEndianCheckBox = new JCheckBox("Little endian", false);
		littleEndianCheckBox.addItemListener(ev -> {
			config.littleEndian = ev.getStateChange() == ItemEvent.SELECTED;
			reloadOffset();
		});
		add(littleEndianCheckBox, constraints);

		// Workaround to force widgets to start from the top (otherwise centered)
		constraints = getConstraints();
		constraints.weighty = 1;
		add(new JLabel(" "), constraints);
	}

	public void setOffset(int offset) {
		this.offset = offset;
		reloadOffset();
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	private void reloadOffset() {
		if (bytes == null || offset == null) {
			return;
		}

		for (int i = 0; i < formatters.size(); i++) {
			ValueFormatter formatter = formatters.get(i);
			if (canDisplay(offset, formatter.dataSize)) {
				ByteBuffer buffer = decodeByteArray(offset, formatter.dataSize);
				String value = formatter.function.apply(buffer);
				((JTextField) getComponent(i * 2 + 1)).setText(value);
			}
		}
	}

	private GridBagConstraints getConstraints() {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.gridy = row;
		row++;
		return constraints;
	}

	private void addValueFormat(String name, int dataSize, Function<ByteBuffer, String> formatter) {
		formatters.add(new ValueFormatter(dataSize, formatter));

		GridBagConstraints constraints = getConstraints();
		constraints.gridx = 0;
		constraints.anchor = GridBagConstraints.WEST;
		add(new JLabel(name), constraints);

		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridx = 1;

		JTextField textField = new JTextField();
		textField.setEditable(false);

		add(textField, constraints);
	}

	private boolean canDisplay(int offset, int size) {
		return offset + size <= bytes.length;
	}

	private ByteBuffer decodeByteArray(int offset, int size) {
		byte[] chunk = sliceBytes(offset, size);
		if (config.littleEndian) {
			ArrayUtils.reverse(chunk);
		}
		return ByteBuffer.wrap(chunk);
	}

	private byte[] sliceBytes(int offset, int size) {
		byte[] slice = new byte[size];
		System.arraycopy(bytes, offset, slice, 0, size);
		return slice;
	}

	private static class ValueFormatter {
		public final int dataSize;
		public final Function<ByteBuffer, String> function;

		public ValueFormatter(int dataSize, Function<ByteBuffer, String> function) {
			this.dataSize = dataSize;
			this.function = function;
		}
	}
}
