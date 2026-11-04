package jadx.gui.settings.ui.plugins;

import java.awt.Component;
import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jadx.gui.utils.NLS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class PluginSettingsGroupTest {

	@BeforeAll
	static void init() {
		NLS.setLocale(NLS.defaultLocale());
	}

	@Test
	public void testOnSelectionWithNullNodeDoesNotThrow() {
		PluginSettingsGroup group = new PluginSettingsGroup(null, null, Collections.emptyList());

		// When detailsPanel is null (before buildComponent)
		assertThatCode(() -> group.onSelection(null)).doesNotThrowAnyException();

		// When TitleNode with hasDetails() == false is selected
		assertThatCode(() -> group.onSelection(new TitleNode("Installed"))).doesNotThrowAnyException();
	}

	@Test
	public void testPluginsListCellRendererWithNullNode() {
		PluginSettingsGroup.PluginsListCellRenderer renderer = new PluginSettingsGroup.PluginsListCellRenderer();
		JList<BasePluginListNode> list = new JList<>();

		// Render with null plugin node
		Component nullComponent = renderer.getListCellRendererComponent(list, null, 0, false, false);
		assertThat(nullComponent).isNotNull();
		assertThat(nullComponent).isInstanceOf(JPanel.class);

		// Render with TitleNode
		TitleNode titleNode = new TitleNode("Available");
		Component titleComponent = renderer.getListCellRendererComponent(list, titleNode, 1, false, false);
		assertThat(titleComponent).isNotNull();
		assertThat(titleComponent).isInstanceOf(JLabel.class);
		assertThat(((JLabel) titleComponent).getText()).isEqualTo("Available");
	}
}
