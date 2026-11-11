package jadx.gui.settings.ui.font;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;

import org.drjekyll.fontchooser.FontChooser;
import org.drjekyll.fontchooser.panes.FamilyPane;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FontChooserHackTest {

	@Test
	void testSetOnlyMonospace() throws Exception {
		FontChooser chooser = new FontChooser();
		FontChooserHack.setOnlyMonospace(chooser);

		FamilyPane familyPane = (FamilyPane) FontChooserHack.getPrivateField(chooser, "familyPane");
		JCheckBox monospacedCheckBox = (JCheckBox) FontChooserHack.getPrivateField(familyPane, "monospacedCheckBox");
		assertThat(monospacedCheckBox.isSelected()).isTrue();
		assertThat(monospacedCheckBox.isEnabled()).isFalse();
	}

	@Test
	void testHidePreview() throws Exception {
		FontChooser chooser = new FontChooser();
		FontChooserHack.hidePreview(chooser);

		JPanel previewPanel = (JPanel) FontChooserHack.getPrivateField(chooser, "previewPanel");
		assertThat(previewPanel.isVisible()).isFalse();
	}

	@Test
	void testFixSizePane() throws Exception {
		FontChooser chooser = new FontChooser();
		FontChooserHack.fixSizePane(chooser);

		JPanel fontPanel = (JPanel) FontChooserHack.getPrivateField(chooser, "fontPanel");
		JPanel sizePane = (JPanel) FontChooserHack.getPrivateField(chooser, "sizePane");
		assertThat(sizePane).isNotNull();

		GridBagLayout gblFont = (GridBagLayout) fontPanel.getLayout();
		GridBagConstraints gbcSize = gblFont.getConstraints(sizePane);
		assertThat(gbcSize.fill).isEqualTo(GridBagConstraints.BOTH);

		assertThat(sizePane.getMinimumSize().width).isGreaterThanOrEqualTo(85);
		assertThat(sizePane.getPreferredSize().width).isGreaterThanOrEqualTo(85);

		GridBagLayout gblSize = (GridBagLayout) sizePane.getLayout();
		for (Component comp : sizePane.getComponents()) {
			GridBagConstraints gbc = gblSize.getConstraints(comp);
			assertThat(gbc.weightx).isEqualTo(1.0);
			if (comp instanceof JScrollPane) {
				assertThat(gbc.fill).isEqualTo(GridBagConstraints.BOTH);
				assertThat(comp.getMinimumSize().width).isGreaterThanOrEqualTo(85);
				assertThat(comp.getPreferredSize().width).isGreaterThanOrEqualTo(85);
			} else if (comp instanceof JSpinner) {
				assertThat(gbc.fill).isEqualTo(GridBagConstraints.HORIZONTAL);
				assertThat(comp.getMinimumSize().width).isGreaterThanOrEqualTo(85);
				assertThat(comp.getPreferredSize().width).isGreaterThanOrEqualTo(85);
			}
		}
	}
}
