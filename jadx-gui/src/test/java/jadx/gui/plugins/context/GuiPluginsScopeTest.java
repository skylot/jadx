package jadx.gui.plugins.context;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.core.plugins.PluginRuntime;
import jadx.gui.settings.data.ITabStatePersist;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;

import static org.assertj.core.api.Assertions.assertThat;

public class GuiPluginsScopeTest {

	@Test
	public void globalEntriesSurviveProjectClose() {
		MainWindow mainWindow = TestMainWindowShim.build();
		CommonGuiPluginsContext context = new CommonGuiPluginsContext(mainWindow);
		try (JadxDecompiler globalDecompiler = new JadxDecompiler()) {
			registerEntries(buildContext(context, globalDecompiler, "global-plugin", true), "global");
			assertEntriesCount(context, 1);

			try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
				registerEntries(buildContext(context, projectDecompiler, "project-plugin", false), "project");
				assertEntriesCount(context, 2);
			}
			context.resetProjectScope();
			assertEntriesCount(context, 1);
			assertThat(context.getCodePopupActionList()).hasSize(1);
		}
	}

	@Test
	public void severalProjectsDontDuplicateOrAccumulateEntries() {
		MainWindow mainWindow = TestMainWindowShim.build();
		CommonGuiPluginsContext context = new CommonGuiPluginsContext(mainWindow);
		try (JadxDecompiler globalDecompiler = new JadxDecompiler()) {
			registerEntries(buildContext(context, globalDecompiler, "global-plugin", true), "global");

			for (int i = 0; i < 3; i++) {
				try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
					registerEntries(buildContext(context, projectDecompiler, "project-plugin-" + i, false), "project");
					assertEntriesCount(context, 2);
				}
				context.resetProjectScope();
				assertEntriesCount(context, 1);
			}
		}
	}

	@Test
	public void projectScopeResetKeepsGlobalMenuActions() {
		MainWindow mainWindow = TestMainWindowShim.build();
		CommonGuiPluginsContext context = new CommonGuiPluginsContext(mainWindow);
		try (JadxDecompiler globalDecompiler = new JadxDecompiler()) {
			buildContext(context, globalDecompiler, "global-plugin", true)
					.addMenuAction("global-menu", () -> {
					});
			context.resetProjectScope();
			TestMainWindowShim.waitForUiThread();
			assertThat(mainWindow.getPluginsMenu().getMenuComponentCount()).isEqualTo(3);
			context.resetProjectScope();
			TestMainWindowShim.waitForUiThread();
			assertThat(mainWindow.getPluginsMenu().getMenuComponentCount()).isEqualTo(3);
		}
	}

	@Test
	public void pluginsMenuChangedInUiThread() {
		UiThreadCheckMenu pluginsMenu = new UiThreadCheckMenu();
		MainWindow mainWindow = TestMainWindowShim.build(pluginsMenu);
		CommonGuiPluginsContext context = new CommonGuiPluginsContext(mainWindow);
		try (JadxDecompiler globalDecompiler = new JadxDecompiler()) {
			buildContext(context, globalDecompiler, "global-plugin", true)
					.addMenuAction("global-menu", () -> {
					});
			context.resetProjectScope();
			TestMainWindowShim.waitForUiThread();
			assertThat(pluginsMenu.getMenuComponentCount()).isEqualTo(3);
			assertThat(pluginsMenu.getNonUiThreads()).isEmpty();
		}
	}

	@Test
	public void pluginKeyBindingsFollowPluginScope() {
		MainWindow mainWindow = TestMainWindowShim.build();
		InputMap keys = ((JComponent) mainWindow.getContentPane()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		KeyStroke globalKey = KeyStroke.getKeyStroke("ctrl alt shift G");
		KeyStroke projectKey = KeyStroke.getKeyStroke("ctrl alt shift P");
		CommonGuiPluginsContext context = new CommonGuiPluginsContext(mainWindow);
		try (JadxDecompiler globalDecompiler = new JadxDecompiler()) {
			PluginRuntime globalPlugin = globalDecompiler.getPluginManager().register(new TestPlugin("global-plugin"));
			assertThat(globalPlugin).isNotNull();
			GuiPluginContext globalContext = context.buildForPlugin(globalPlugin, true);
			assertThat(globalContext.registerGlobalKeyBinding("global-key", "ctrl alt shift G", () -> {
			})).isTrue();
			for (int i = 0; i < 2; i++) {
				try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
					GuiPluginContext projectContext = buildContext(context, projectDecompiler, "project-plugin", false);
					assertThat(projectContext.registerGlobalKeyBinding("project-key", "ctrl alt shift P", () -> {
					})).isTrue();
					assertThat(projectContext.registerGlobalKeyBinding("other-key", "ctrl alt shift G", () -> {
					})).isFalse();
					TestMainWindowShim.waitForUiThread();
					assertThat(keys.get(globalKey)).isEqualTo("global-key");
					assertThat(keys.get(projectKey)).isEqualTo("project-key");
				}
				context.resetProjectScope();
				TestMainWindowShim.waitForUiThread();
				assertThat(keys.get(globalKey)).isEqualTo("global-key");
				assertThat(keys.get(projectKey)).isNull();
			}
			context.removeGlobalPlugin(globalPlugin);
			TestMainWindowShim.waitForUiThread();
			assertThat(keys.get(globalKey)).isNull();
		}
	}

	private static GuiPluginContext buildContext(CommonGuiPluginsContext context,
			JadxDecompiler decompiler, String pluginId, boolean global) {
		PluginRuntime pluginRuntime = decompiler.getPluginManager().register(new TestPlugin(pluginId));
		assertThat(pluginRuntime).isNotNull();
		return context.buildForPlugin(pluginRuntime, global);
	}

	private static void registerEntries(GuiPluginContext guiContext, String name) {
		guiContext.addPopupMenuAction(name, null, null, ref -> {
		});
		guiContext.addTreePopupMenuEntry(name, node -> true, node -> {
		});
		guiContext.registerTreeInputCategory(new TestInputCategory());
		guiContext.registerTabStatePersistAdapter(new TestTabState());
	}

	private static void assertEntriesCount(CommonGuiPluginsContext context, int expected) {
		assertThat(context.getCodePopupActionList()).hasSize(expected);
		assertThat(context.getTreePopupMenuEntries()).hasSize(expected);
		assertThat(context.getTreeInputCategories()).hasSize(expected);
		assertThat(context.getTabStatePersistAdapters()).hasSize(expected);
	}

	private static final class TestPlugin implements JadxPlugin {
		private final String pluginId;

		private TestPlugin(String pluginId) {
			this.pluginId = pluginId;
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId(pluginId).name(pluginId).description("test").build();
		}

		@Override
		public void init(JadxPluginContext context) {
		}
	}

	private static final class UiThreadCheckMenu extends JMenu {
		private final List<String> nonUiThreads = new CopyOnWriteArrayList<>();

		@Override
		public JMenuItem add(JMenuItem menuItem) {
			checkThread();
			return super.add(menuItem);
		}

		@Override
		public void addSeparator() {
			checkThread();
			super.addSeparator();
		}

		@Override
		public void removeAll() {
			checkThread();
			super.removeAll();
		}

		private void checkThread() {
			if (!SwingUtilities.isEventDispatchThread()) {
				nonUiThreads.add(Thread.currentThread().getName());
			}
		}

		public List<String> getNonUiThreads() {
			return nonUiThreads;
		}
	}

	private static final class TestInputCategory implements ITreeInputCategory {
		@Override
		public boolean filesFilter(Path file) {
			return false;
		}

		@Override
		public JNode buildInputNode(List<Path> files) {
			return null;
		}
	}

	private static final class TestTabState implements ITabStatePersist {
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
