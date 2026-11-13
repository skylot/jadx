package jadx.gui.plugins.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import jadx.api.JadxDecompiler;
import jadx.api.gui.IJadxSettings;
import jadx.api.gui.IMainWindow;
import jadx.api.gui.JadxGuiEvents;
import jadx.api.gui.code.IContentStatePersist;
import jadx.api.gui.dock.DockableOptions;
import jadx.api.gui.dock.JadxDockable;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.gui.tree.IJadxTree;
import jadx.api.gui.tree.ITreeNode;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.core.plugins.PluginRuntime;
import jadx.gui.events.types.JadxGuiEventsImpl;
import jadx.gui.settings.TabStateViewAdapter;
import jadx.gui.settings.data.TabViewState;
import jadx.gui.tree.JadxTreeNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.ui.dock.DockingManager;
import jadx.gui.ui.tab.PluginEventsTabStates;
import jadx.gui.ui.tab.TabBlueprint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuiPluginsApiIntegrationTest {

	@Test
	void contextExtExposesTabStatePersistAdapterAndRestoresState() {
		MainWindow mainWindow = TestMainWindowShim.build();
		CommonGuiPluginsContext commonContext = new CommonGuiPluginsContext(mainWindow);
		try (JadxDecompiler decompiler = new JadxDecompiler()) {
			PluginRuntime runtime = decompiler.getPluginManager().register(new DummyPlugin("persist-plugin"));
			assertThat(runtime).isNotNull();
			JadxGuiContextExt contextExt = commonContext.buildForPlugin(runtime, false);

			CustomNode customNode = new CustomNode("custom-id", "Custom Node");
			contextExt.registerTabStatePersistAdapter(new IContentStatePersist() {
				@Override
				public Class<? extends ITreeNode> getNodeClass() {
					return CustomNode.class;
				}

				@Override
				public String save(ITreeNode node) {
					return node.getID();
				}

				@Override
				public @Nullable ITreeNode load(String stateStr) {
					return "custom-id".equals(stateStr) ? customNode : null;
				}
			});

			TabStateViewAdapter adapter = new TabStateViewAdapter();
			adapter.setCustomAdapters(commonContext.getTabStatePersistAdapters());
			TabViewState saved = adapter.build(new EditorViewState(customNode));
			assertThat(saved).isNotNull();
			EditorViewState restored = adapter.load(mainWindow, saved);
			assertThat(restored).isNotNull();
			assertThat(restored.getNode()).isSameAs(customNode);
		}
	}

	@Test
	void addDockableValidatesKeyBindingBeforeRegistering() {
		MainWindow mainWindow = TestMainWindowShim.build();
		attachDockingManager(mainWindow);
		CommonGuiPluginsContext commonContext = new CommonGuiPluginsContext(mainWindow);
		try (JadxDecompiler decompiler = new JadxDecompiler()) {
			PluginRuntime runtime = decompiler.getPluginManager().register(new DummyPlugin("dock-plugin"));
			assertThat(runtime).isNotNull();
			GuiPluginContext context = commonContext.buildForPlugin(runtime, false);
			DockableOptions invalid = new DockableOptions("bad-dock", "Bad")
					.keyBinding("INVALID KEY SPEC");

			assertThatThrownBy(() -> context.addDockable(invalid, () -> new JLabel("x")))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("INVALID KEY SPEC");
			assertThat(context.getRegistry().getDockables()).isEmpty();

			DockableOptions collision = new DockableOptions("good-dock", "Good")
					.keyBinding("F1");
			JadxDockable dockable = context.addDockable(collision, () -> new JLabel("ok"));
			assertThat(dockable.getId()).isEqualTo("good-dock");
			assertThat(context.getRegistry().getDockables()).hasSize(1);
		}
	}

	@Test
	void pluginEventsTabStatesUsesValueEqualityForSelections() throws InterruptedException {
		JadxGuiEventsImpl events = new JadxGuiEventsImpl();
		List<ICodeNodeRef> opened = Collections.synchronizedList(new ArrayList<>());
		List<ICodeNodeRef> selected = Collections.synchronizedList(new ArrayList<>());
		List<ICodeNodeRef> closed = Collections.synchronizedList(new ArrayList<>());
		CountDownLatch eventsLatch = new CountDownLatch(4);
		events.global().addListener(JadxGuiEvents.EDITOR_OPENED, e -> {
			opened.add(e.getNode());
			eventsLatch.countDown();
		});
		events.global().addListener(JadxGuiEvents.EDITOR_SELECTED, e -> {
			selected.add(e.getNode());
			eventsLatch.countDown();
		});
		events.global().addListener(JadxGuiEvents.EDITOR_CLOSED, e -> {
			closed.add(e.getNode());
			eventsLatch.countDown();
		});

		PluginEventsTabStates listener = new PluginEventsTabStates(events);
		ICodeNodeRef firstRef = new CodeNode("com.example.a");
		ICodeNodeRef equalSecondRef = new CodeNode("com.example.a");
		TabBlueprint firstTab = new TabBlueprint((ITreeNode) firstRef);
		TabBlueprint secondTab = new TabBlueprint((ITreeNode) equalSecondRef);

		listener.onTabOpen(firstTab);
		listener.onTabSelect(firstTab);
		listener.onTabSelect(secondTab);
		listener.onTabClose(secondTab);

		assertThat(eventsLatch.await(5, TimeUnit.SECONDS)).isTrue();
		assertThat(opened).containsExactly(firstRef);
		assertThat(selected).containsExactly(firstRef, null);
		assertThat(closed).containsExactly(equalSecondRef);
	}

	@Test
	void treeNodePopupMenuBridgeDelegatesToMainWindowOverload() {
		MainWindow mainWindow = TestMainWindowShim.build();
		JPopupMenu expectedMenu = new JPopupMenu();
		expectedMenu.add(new JMenuItem("Custom Item"));

		JNode node = new JNode() {
			@Override
			public @Nullable JClass getJParent() {
				return null;
			}

			@Override
			public String makeString() {
				return "node";
			}

			@Override
			public @Nullable Icon getIcon() {
				return null;
			}

			@Override
			public @Nullable JPopupMenu onTreePopupMenu(MainWindow mw) {
				return expectedMenu;
			}
		};

		ITreeNode asTreeNode = node;
		assertThat(asTreeNode.onTreePopupMenu(mainWindow)).isSameAs(expectedMenu);
		assertThat(asTreeNode.onTreePopupMenu(new NonMainWindowStub())).isNull();
	}

	private static void attachDockingManager(MainWindow mainWindow) {
		try {
			var field = MainWindow.class.getDeclaredField("docking");
			field.setAccessible(true);
			field.set(mainWindow, new DockingManager(mainWindow, mainWindow.events()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static class CustomNode extends JadxTreeNode {
		private final String id;
		private final String name;

		private CustomNode(String id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public String getID() {
			return id;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	private static final class CodeNode extends CustomNode implements ICodeNodeRef {
		private final String key;
		private int defPos;

		private CodeNode(String key) {
			super("code:" + key, key);
			this.key = key;
		}

		@Override
		public ICodeAnnotation.AnnType getAnnType() {
			return ICodeAnnotation.AnnType.PKG;
		}

		@Override
		public int getDefPosition() {
			return defPos;
		}

		@Override
		public void setDefPosition(int pos) {
			this.defPos = pos;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof CodeNode && key.equals(((CodeNode) obj).key);
		}

		@Override
		public int hashCode() {
			return key.hashCode();
		}
	}

	private static final class NonMainWindowStub implements IMainWindow {
		@Override
		public IJadxSettings getSettings() {
			return null;
		}

		@Override
		public IJadxTree getTree() {
			return null;
		}

		@Override
		public void registerZoomActions(javax.swing.JComponent component, Runnable update) {
		}
	}

	private static final class DummyPlugin implements JadxPlugin {
		private final String id;

		private DummyPlugin(String id) {
			this.id = id;
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId(id).name(id).description("test").build();
		}

		@Override
		public void init(JadxPluginContext context) {
		}
	}
}
