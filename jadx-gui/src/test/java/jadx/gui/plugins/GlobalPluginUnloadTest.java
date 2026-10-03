package jadx.gui.plugins;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import jadx.api.JadxDecompiler;
import jadx.api.gui.plugins.JadxGlobalGuiPlugin;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.core.plugins.PluginRuntime;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.ui.MainWindow;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalPluginUnloadTest {

	@Test
	public void scheduledGlobalPluginUnload() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		TestPlugin removedPlugin = new TestPlugin("removed-plugin");
		TestPlugin keptPlugin = new TestPlugin("kept-plugin");
		manager.initGuiPluginsContextForGlobalScope();
		assertThat(manager.getGlobalPluginManager().register(removedPlugin)).isNotNull();
		assertThat(manager.getGlobalPluginManager().register(keptPlugin)).isNotNull();
		manager.runGlobalInit(manager.getGlobalPlugins());
		assertThat(getMenuItems(mainWindow)).contains("removed-plugin action", "kept-plugin action");
		assertThat(manager.getPluginsContext().getCodePopupActionList()).hasSize(2);

		manager.scheduleGlobalUnload("removed-plugin");
		manager.runScheduledGlobalUnload();

		assertThat(removedPlugin.globalUnloadCount).isEqualTo(1);
		assertThat(keptPlugin.globalUnloadCount).isZero();
		assertThat(manager.getGlobalPlugins())
				.extracting(PluginRuntime::getPluginId)
				.containsExactly("kept-plugin");
		assertThat(getMenuItems(mainWindow))
				.contains("kept-plugin action")
				.doesNotContain("removed-plugin action");
		assertThat(manager.getPluginsContext().getCodePopupActionList()).hasSize(1);

		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			manager.injectGlobalPlugins(projectDecompiler);
			assertThat(projectDecompiler.getPluginManager().getAllPlugins())
					.extracting(PluginRuntime::getPluginId)
					.containsExactly("kept-plugin");
		}

		// unload on exit don't call removed plugin again
		manager.runGlobalUnload();
		assertThat(removedPlugin.globalUnloadCount).isEqualTo(1);
		assertThat(keptPlugin.globalUnloadCount).isEqualTo(1);
	}

	private static List<String> getMenuItems(MainWindow mainWindow) {
		List<String> items = new ArrayList<>();
		for (Component component : mainWindow.getPluginsMenu().getMenuComponents()) {
			if (component instanceof JMenuItem) {
				items.add(((JMenuItem) component).getText());
			}
		}
		return items;
	}

	private static final class TestPlugin extends JadxGlobalGuiPlugin {
		private final String pluginId;
		int globalUnloadCount;

		private TestPlugin(String pluginId) {
			this.pluginId = pluginId;
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId(pluginId).name(pluginId).description("test").build();
		}

		@Override
		public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
			guiContext.addMenuAction(pluginId + " action", () -> {
			});
			guiContext.addPopupMenuAction(pluginId + " popup", null, null, ref -> {
			});
		}

		@Override
		public void globalUnload() {
			globalUnloadCount++;
		}
	}
}
