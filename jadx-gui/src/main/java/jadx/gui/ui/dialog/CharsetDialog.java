package jadx.gui.ui.dialog;

import java.awt.Component;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.utils.NLS;

public class CharsetDialog {
	private static final Logger LOG = LoggerFactory.getLogger(CharsetDialog.class);

	private static final Comparator<Charset> CHARSET_COMPARATOR = Comparator.comparing(
			Charset::displayName,
			String::compareToIgnoreCase);

	public static String chooseCharset(Component parent, String currentCharsetName) {
		Collection<Charset> availableCharsets = Charset.availableCharsets().values();

		List<Charset> sortedCharsets = availableCharsets.stream()
				.sorted(CHARSET_COMPARATOR)
				.collect(Collectors.toList());

		Charset initialSelection = null;
		try {
			if (currentCharsetName != null && Charset.isSupported(currentCharsetName)) {
				initialSelection = Charset.forName(currentCharsetName);
				if (!sortedCharsets.contains(initialSelection)) {
					initialSelection = null;
				}
			}
		} catch (Exception e) {
			LOG.warn("Failed to find initial charset '{}'", currentCharsetName, e);
		}
		if (initialSelection == null && !sortedCharsets.isEmpty()) {
			initialSelection = sortedCharsets.get(0);
		}
		Charset[] charsetArray = sortedCharsets.toArray(new Charset[0]);

		Object selectedValue = JOptionPane.showInputDialog(
				parent,
				NLS.str("encoding_dialog.message"),
				NLS.str("encoding_dialog.title"),
				JOptionPane.INFORMATION_MESSAGE,
				null,
				charsetArray,
				initialSelection);

		if (selectedValue instanceof Charset) {
			return ((Charset) selectedValue).name();
		} else {
			return null;
		}
	}

}
