package jadx.gui.tree;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.gui.tree.BaseTreeNodeEnum;
import jadx.api.gui.tree.ILoadableTreeNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.JadxWrapper;
import jadx.gui.plugins.GuiPluginsManager;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.settings.JadxProject;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JRoot;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.CacheObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JadxTreeApiTest {

	@Test
	void filterAndRootFollowPathHandleCustomTreeNodesSafely() throws Exception {
		MainWindow mainWindow = buildWindowWithTree();
		JRoot root = new JRoot(mainWindow);
		PluginTreeNode customParent = new PluginTreeNode("plugin-root", "PluginRoot");
		PluginTreeNode customChild = new PluginTreeNode("plugin-child", "TargetItem");
		customParent.add(customChild);
		root.add(customParent);
		root.add(new PlainJNode("OtherJNode"));

		assertThatThrownBy(() -> root.followStaticPath("JResource"))
				.isInstanceOf(JadxRuntimeException.class)
				.hasMessageContaining("JResource");

		FilterableTreeModel model = new FilterableTreeModel(mainWindow, root);
		model.setFilter("target");
		TestMainWindowShim.waitForUiThread();

		assertThat(model.getChildCount(root)).isEqualTo(1);
		assertThat(model.getChild(root, 0)).isSameAs(customParent);
		assertThat(model.getChildCount(customParent)).isEqualTo(1);
		assertThat(model.getChild(customParent, 0)).isSameAs(customChild);
	}

	@Test
	void addNodeReplacesDuplicateIdAndUpdatesInitializedTree(@TempDir Path tempDir) throws Exception {
		MainWindow mainWindow = buildWindowWithTree();
		Path dummyInput = Files.writeString(tempDir.resolve("sample.txt"), "hello");
		JadxArgs args = new JadxArgs();
		args.setInputFile(dummyInput.toFile());
		try (JadxDecompiler decompiler = new JadxDecompiler(args)) {
			decompiler.load();
			var decompilerField = JadxWrapper.class.getDeclaredField("decompiler");
			decompilerField.setAccessible(true);
			decompilerField.set(mainWindow.getWrapper(), decompiler);

			JadxTree jadxTree = mainWindow.getTree();
			PluginTreeNode preInit = new PluginTreeNode("pre.init", "PreInit");
			jadxTree.addNode(BaseTreeNodeEnum.ROOT, preInit);
			jadxTree.initTree();

			PluginTreeNode first = new PluginTreeNode("custom.report", "Report v1");
			PluginTreeNode replacement = new PluginTreeNode("custom.report", "Report v2");
			jadxTree.addNode(BaseTreeNodeEnum.ROOT, first);
			assertThat(jadxTree.getTreeRoot().searchNode(first)).isSameAs(first);

			jadxTree.addNode(BaseTreeNodeEnum.ROOT, replacement);
			assertThat(jadxTree.getTreeRoot().searchNode(replacement)).isSameAs(replacement);
			assertThat(jadxTree.getTreeRoot().childrenList())
					.filteredOn(n -> n instanceof PluginTreeNode)
					.containsExactly(preInit, replacement);
		}
	}

	@Test
	void treeExpansionServiceSavesAndRestoresLoadableCustomNodes() throws Exception {
		MainWindow mainWindow = buildWindowWithTree();
		JRoot root = new JRoot(mainWindow);
		LoadablePluginNode pluginNode = new LoadablePluginNode("plugin-root", "LazyPluginNode");
		root.add(pluginNode);

		FilterableTreeModel model = new FilterableTreeModel(mainWindow, root);
		JTree tree = new JTree(model);
		TreeExpansionService service = new TreeExpansionService(mainWindow, tree);

		TreePath resolvedBeforeLoad = service.loadPath("t:plugin-root//lazy-child");
		assertThat(pluginNode.loaded).isTrue();
		assertThat(resolvedBeforeLoad).isNotNull();
		assertThat(resolvedBeforeLoad.getLastPathComponent()).isInstanceOf(PluginTreeNode.class);
		assertThat(service.savePath(resolvedBeforeLoad)).isEqualTo("t:plugin-root//lazy-child");
	}

	private static MainWindow buildWindowWithTree() throws Exception {
		MainWindow mainWindow = TestMainWindowShim.build();
		TestMainWindowShim.setPluginsManager(mainWindow, new GuiPluginsManager(mainWindow));

		var projectField = MainWindow.class.getDeclaredField("project");
		projectField.setAccessible(true);
		projectField.set(mainWindow, new JadxProject(mainWindow));

		var cacheField = MainWindow.class.getDeclaredField("cacheObject");
		cacheField.setAccessible(true);
		cacheField.set(mainWindow, new CacheObject(mainWindow.getWrapper()));

		var treeField = MainWindow.class.getDeclaredField("jadxTree");
		treeField.setAccessible(true);
		treeField.set(mainWindow, new JadxTree(mainWindow));
		return mainWindow;
	}

	private static class PluginTreeNode extends JadxTreeNode {
		private final String id;
		private final String name;

		PluginTreeNode(String id, String name) {
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

	private static final class LoadablePluginNode extends PluginTreeNode implements ILoadableTreeNode {
		private boolean loaded;

		LoadablePluginNode(String id, String name) {
			super(id, name);
			add(new DefaultMutableTreeNode("placeholder"));
		}

		@Override
		public void loadNode() {
			if (!loaded) {
				loaded = true;
				removeAllChildren();
				add(new PluginTreeNode("lazy-child", "LazyChild"));
			}
		}
	}

	private static final class PlainJNode extends JNode {
		private final String name;

		private PlainJNode(String name) {
			this.name = name;
		}

		@Override
		public JClass getJParent() {
			return null;
		}

		@Override
		public Icon getIcon() {
			return null;
		}

		@Override
		public String makeString() {
			return name;
		}
	}
}
