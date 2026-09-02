package jadx.gui.plugins.context;

import java.nio.file.Path;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.junit.jupiter.api.Test;

import jadx.gui.settings.data.ITabStatePersist;
import jadx.gui.treemodel.JNode;

import static org.assertj.core.api.Assertions.assertThat;

public class GuiPluginsRegistryTest {

	@Test
	public void clearRemovesAllContributionTypes() {
		GuiPluginsRegistry registry = new GuiPluginsRegistry();
		fill(registry);
		assertThat(registry.getMenuActions()).hasSize(1);
		assertThat(registry.getCodePopupActions()).hasSize(1);
		assertThat(registry.getTreePopupMenuEntries()).hasSize(1);
		assertThat(registry.getTreeInputCategories()).hasSize(1);
		assertThat(registry.getTabStatePersistAdapters()).hasSize(1);

		registry.clear();

		assertThat(registry.getMenuActions()).isEmpty();
		assertThat(registry.getCodePopupActions()).isEmpty();
		assertThat(registry.getTreePopupMenuEntries()).isEmpty();
		assertThat(registry.getTreeInputCategories()).isEmpty();
		assertThat(registry.getTabStatePersistAdapters()).isEmpty();
	}

	@Test
	public void projectResetKeepsApplicationContributions() {
		GuiPluginsRegistry appScope = new GuiPluginsRegistry();
		GuiPluginsRegistry projectScope = new GuiPluginsRegistry();
		fill(appScope);

		for (int i = 0; i < 3; i++) {
			fill(projectScope);
			projectScope.clear();

			assertThat(appScope.getMenuActions()).hasSize(1);
			assertThat(appScope.getCodePopupActions()).hasSize(1);
			assertThat(appScope.getTreePopupMenuEntries()).hasSize(1);
			assertThat(appScope.getTreeInputCategories()).hasSize(1);
			assertThat(appScope.getTabStatePersistAdapters()).hasSize(1);
		}
	}

	@Test
	public void tabStateAdaptersDoNotAccumulateAcrossProjects() {
		GuiPluginsRegistry projectScope = new GuiPluginsRegistry();
		for (int i = 0; i < 5; i++) {
			fill(projectScope);
			assertThat(projectScope.getTabStatePersistAdapters()).hasSize(1);
			projectScope.clear();
		}
		assertThat(projectScope.getTabStatePersistAdapters()).isEmpty();
	}

	private static void fill(GuiPluginsRegistry registry) {
		registry.getMenuActions().add(newAction());
		registry.getCodePopupActions().add(new CodePopupAction("popup", null, null, ref -> {
		}));
		registry.getTreePopupMenuEntries().add(new TreePopupMenuEntry("tree", node -> true, node -> {
		}));
		registry.getTreeInputCategories().add(new TestTreeInputCategory());
		registry.getTabStatePersistAdapters().add(new TestTabStatePersist());
	}

	private static Action newAction() {
		return new AbstractAction("test") {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				// no-op
			}
		};
	}

	private static class TestTreeInputCategory implements ITreeInputCategory {
		@Override
		public boolean filesFilter(Path file) {
			return false;
		}

		@Override
		public JNode buildInputNode(List<Path> files) {
			return null;
		}
	}

	private static class TestTabStatePersist implements ITabStatePersist {
		@Override
		public Class<? extends JNode> getNodeClass() {
			return JNode.class;
		}

		@Override
		public String save(JNode node) {
			return "";
		}

		@Override
		public JNode load(String stateStr) {
			return null;
		}
	}
}
