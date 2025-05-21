package jadx.gui.settings.ui.font;

import java.lang.reflect.Field;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.drjekyll.fontchooser.FontChooser;
import org.drjekyll.fontchooser.panes.FamilyPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FontChooserHack {
	private static final Logger LOG = LoggerFactory.getLogger(FontChooserHack.class);

	public static void setOnlyMonospace(FontChooser fontChooser) {
		try {
			FamilyPane familyPane = (FamilyPane) getPrivateField(fontChooser, "familyPane");
			JCheckBox monospacedCheckBox = (JCheckBox) getPrivateField(familyPane, "monospacedCheckBox");
			monospacedCheckBox.setSelected(true);
			monospacedCheckBox.setEnabled(false);
		} catch (Throwable e) {
			LOG.debug("Failed to set only monospace check box", e);
		}
	}

	public static void hidePreview(FontChooser fontChooser) {
		try {
			JPanel previewPanel = (JPanel) getPrivateField(fontChooser, "previewPanel");
			previewPanel.setVisible(false);
		} catch (Throwable e) {
			LOG.debug("Failed to hide preview panel", e);
		}
	}

	private static Object getPrivateField(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
		Field f = obj.getClass().getDeclaredField(fieldName);
		f.setAccessible(true);
		return f.get(obj);
	}
}
