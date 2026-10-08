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
		manager.runScheduledGlobalChanges();

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
			projectDecompiler.getPluginManager().load(new GuiPluginsLoader(manager.getGlobalPlugins(), new TestPluginLoader()));
			assertThat(projectDecompiler.getPluginManager().getAllPlugins())
					.extracting(PluginRuntime::getPluginId)
					.containsExactly("kept-plugin");
		}

		// unload on exit don't call removed plugin again
		manager.runGlobalUnload();
		assertThat(removedPlugin.globalUnloadCount).isEqualTo(1);
		assertThat(keptPlugin.globalUnloadCount).isEqualTo(1);
	}

	@Test
	public void failedGlobalPluginEntriesRemoved() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		TestPlugin failedPlugin = new TestPlugin("failed-plugin", true);
		TestPlugin goodPlugin = new TestPlugin("good-plugin");
		manager.initGuiPluginsContextForGlobalScope();
		assertThat(manager.getGlobalPluginManager().register(failedPlugin)).isNotNull();
		assertThat(manager.getGlobalPluginManager().register(goodPlugin)).isNotNull();
		manager.runGlobalInit(manager.getGlobalPlugins());

		assertThat(getMenuItems(mainWindow))
				.contains("good-plugin action")
				.doesNotContain("failed-plugin action");
		assertThat(manager.getPluginsContext().getCodePopupActionList()).hasSize(1);
	}

	@Test
	public void globalPluginLoadedWithoutRestart() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		TestPlugin startPlugin = new TestPlugin("start-plugin");
		manager.initGuiPluginsContextForGlobalScope();
		assertThat(manager.getGlobalPluginManager().register(startPlugin)).isNotNull();
		manager.runGlobalInit(manager.getGlobalPlugins());

		TestPlugin addedPlugin = new TestPlugin("added-plugin");
		manager.addGlobalPlugin(addedPlugin);

		assertThat(startPlugin.globalInitCount).isEqualTo(1);
		assertThat(addedPlugin.globalInitCount).isEqualTo(1);
		assertThat(getMenuItems(mainWindow)).contains("start-plugin action", "added-plugin action");
		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			projectDecompiler.getPluginManager().load(new GuiPluginsLoader(manager.getGlobalPlugins(), new TestPluginLoader()));
			assertThat(projectDecompiler.getPluginManager().getAllPlugins())
					.extracting(PluginRuntime::getPluginId)
					.containsExactly("added-plugin", "start-plugin");
		}
		manager.runGlobalUnload();
		assertThat(addedPlugin.globalUnloadCount).isEqualTo(1);
	}

	@Test
	public void failedAddedGlobalPluginNotKept() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		manager.initGuiPluginsContextForGlobalScope();

		manager.addGlobalPlugin(new TestPlugin("failed-plugin", true));

		assertThat(manager.getGlobalPlugins()).isEmpty();
		assertThat(getMenuItems(mainWindow)).doesNotContain("failed-plugin action");
	}

	private static List<String> getMenuItems(MainWindow mainWindow) {
		TestMainWindowShim.waitForUiThread();
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
		private final boolean failGlobalInit;
		int globalInitCount;
		int globalUnloadCount;

		private TestPlugin(String pluginId) {
			this(pluginId, false);
		}

		private TestPlugin(String pluginId, boolean failGlobalInit) {
			this.pluginId = pluginId;
			this.failGlobalInit = failGlobalInit;
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId(pluginId).name(pluginId).description("test").build();
		}

		@Override
		public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
			globalInitCount++;
			guiContext.addMenuAction(pluginId + " action", () -> {
			});
			guiContext.addPopupMenuAction(pluginId + " popup", null, null, ref -> {
			});
			if (failGlobalInit) {
				throw new RuntimeException("test");
			}
		}

		@Override
		public void globalUnload() {
			globalUnloadCount++;
		}
	}
}
