package jadx.gui.settings.ui.font;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.lang.reflect.Field;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;

import org.drjekyll.fontchooser.FontChooser;
import org.drjekyll.fontchooser.panes.FamilyPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.util.UIScale;

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

	public static void fixSizePane(FontChooser fontChooser) {
		try {
			JPanel fontPanel = (JPanel) getPrivateField(fontChooser, "fontPanel");
			JPanel sizePane = (JPanel) getPrivateField(fontChooser, "sizePane");
			if (sizePane == null) {
				return;
			}
			FontMetrics fm = fontChooser.getFontMetrics(fontChooser.getFont());
			int fontWidth = fm != null ? fm.stringWidth("128") : 24;
			int minWidth = Math.max(UIScale.scale(85), fontWidth + UIScale.scale(55));

			if (fontPanel != null && fontPanel.getLayout() instanceof GridBagLayout) {
				GridBagLayout gbl = (GridBagLayout) fontPanel.getLayout();
				GridBagConstraints gbc = gbl.getConstraints(sizePane);
				gbc.fill = GridBagConstraints.BOTH;
				gbl.setConstraints(sizePane, gbc);
			}

			if (sizePane.getLayout() instanceof GridBagLayout) {
				GridBagLayout gbl = (GridBagLayout) sizePane.getLayout();
				for (Component comp : sizePane.getComponents()) {
					GridBagConstraints gbc = gbl.getConstraints(comp);
					if (comp instanceof JScrollPane) {
						gbc.fill = GridBagConstraints.BOTH;
						gbc.weightx = 1.0;
						comp.setMinimumSize(new Dimension(minWidth, UIScale.scale(50)));
						comp.setPreferredSize(new Dimension(minWidth, UIScale.scale(100)));
					} else if (comp instanceof JSpinner) {
						gbc.fill = GridBagConstraints.HORIZONTAL;
						gbc.weightx = 1.0;
						int h = comp.getPreferredSize().height;
						comp.setMinimumSize(new Dimension(minWidth, h));
						comp.setPreferredSize(new Dimension(minWidth, h));
					}
					gbl.setConstraints(comp, gbc);
				}
			}

			sizePane.setMinimumSize(new Dimension(minWidth, UIScale.scale(80)));
			sizePane.setPreferredSize(new Dimension(minWidth, UIScale.scale(130)));

			JList<?> sizeList = (JList<?>) getPrivateField(sizePane, "sizeList");
			if (sizeList != null && sizeList.getCellRenderer() instanceof DefaultListCellRenderer) {
				DefaultListCellRenderer renderer = (DefaultListCellRenderer) sizeList.getCellRenderer();
				renderer.setBorder(BorderFactory.createEmptyBorder(0, UIScale.scale(4), 0, UIScale.scale(8)));
			}
		} catch (Throwable e) {
			LOG.debug("Failed to fix size pane layout", e);
		}
	}

	static Object getPrivateField(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
		Field f = obj.getClass().getDeclaredField(fieldName);
		f.setAccessible(true);
		return f.get(obj);
	}
}
