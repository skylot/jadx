package jadx.gui.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.gui.tree.ILoadableTreeNode;
import jadx.api.gui.tree.ITreeNode;
import jadx.api.metadata.ICodeNodeRef;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.jobs.LoadTask;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JPackage;
import jadx.gui.treemodel.JRoot;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.UiUtils;

public class TreeExpansionService {
	private static final Logger LOG = LoggerFactory.getLogger(TreeExpansionService.class);
	private static final boolean DEBUG = false;

	private static final Comparator<TreePath> PATH_LENGTH_REVERSE = Comparator.comparingInt(p -> -p.getPathCount());

	private final MainWindow mainWindow;
	private final JTree tree;
	private final JNodeCache nodeCache;

	public TreeExpansionService(MainWindow mainWindow, JTree tree) {
		this.mainWindow = mainWindow;
		this.tree = tree;
		this.nodeCache = mainWindow.getCacheObject().getNodeCache();
	}

	public List<String> save() {
		if (tree.getRowCount() == 0 || mainWindow.getWrapper().getCurrentDecompiler().isEmpty()) {
			return Collections.emptyList();
		}
		List<TreePath> expandedPaths = collectExpandedPaths(tree);
		List<String> list = new ArrayList<>();
		for (TreePath expandedPath : expandedPaths) {
			list.add(savePath(expandedPath));
		}
		if (DEBUG) {
			LOG.debug("Saving tree expansions:\n {}", Utils.listToString(list, "\n "));
		}
		return list;
	}

	public void load(List<String> treeExpansions) {
		mainWindow.getBackgroundExecutor().execute(new LoadTask<>(
				() -> {
					List<TreePath> expandedPaths = new ArrayList<>();
					loadPaths(treeExpansions, expandedPaths);
					// send expand event to load sub-nodes and wait for completion
					UiUtils.uiRunAndWait(() -> expandedPaths.forEach(path -> {
						try {
							tree.fireTreeWillExpand(path);
						} catch (Exception e) {
							throw new JadxRuntimeException("Tree expand error", e);
						}
					}));
					return expandedPaths;
				},
				expandedPaths -> {
					// expand paths after a loading task is finished
					expandedPaths.forEach(tree::expandPath);
				}));
	}

	private void loadPaths(List<String> treeExpansions, List<TreePath> expandedPaths) {
		if (DEBUG) {
			LOG.debug("Restoring tree expansions:\n {}", Utils.listToString(treeExpansions, "\n "));
		}
		for (String treeExpansion : treeExpansions) {
			try {
				TreePath treePath = loadPath(treeExpansion);
				if (treePath != null) {
					expandedPaths.add(treePath);
				}
			} catch (Exception e) {
				LOG.warn("Failed to load tree expansion entry: {}", treeExpansion, e);
			}
		}
		if (DEBUG) {
			LOG.debug("Restored expanded tree paths:\n {}", Utils.listToString(expandedPaths, "\n "));
		}
	}

	String savePath(TreePath path) {
		Object node = path.getLastPathComponent();
		if (node instanceof JPackage) {
			return "p:" + ((JPackage) node).getPkg().getRawFullName();
		}
		if (node instanceof JClass) {
			return "c:" + ((JClass) node).getCls().getRawName();
		}
		return Arrays.stream(path.getPath())
				.map(p -> p instanceof ITreeNode ? ((ITreeNode) p).getID() : Objects.toString(p))
				.skip(1) // skip root
				.collect(Collectors.joining("//", "t:", ""));
	}

	@Nullable
	TreePath loadPath(String pathStr) {
		String pathData = pathStr.substring(2);
		switch (pathStr.charAt(0)) {
			case 'c':
				return getTreePathForRef(getRoot().resolveRawClass(pathData));
			case 'p':
				return getTreePathForRef(getRoot().resolvePackage(pathData));
			case 't':
				return resolveTreePath(pathData.split("//"));

			default:
				throw new JadxRuntimeException("Unknown tree expansion path type: " + pathStr);
		}
	}

	private @Nullable TreePath resolveTreePath(String[] pathArr) {
		Object root = tree.getModel().getRoot();
		if (!(root instanceof TreeNode)) {
			return null;
		}
		TreeNode current = (TreeNode) root;
		List<TreeNode> resolved = new ArrayList<>();
		resolved.add(current);
		for (String nodeStr : pathArr) {
			TreeNode match = findChildById(current, nodeStr);
			if (match == null) {
				if (DEBUG) {
					List<String> children = collectChildIds(current);
					LOG.warn("Failed to restore path: {}, node '{}' not found in '{}' children: {}",
							Arrays.toString(pathArr), nodeStr, current, children);
				}
				return null;
			}
			resolved.add(match);
			current = match;
		}
		return new TreePath(resolved.toArray());
	}

	private static @Nullable TreeNode findChildById(TreeNode parent, String id) {
		if (parent instanceof ILoadableTreeNode) {
			((ILoadableTreeNode) parent).loadNode();
		}
		Enumeration<? extends TreeNode> en = parent.children();
		while (en.hasMoreElements()) {
			TreeNode child = en.nextElement();
			if (child instanceof ITreeNode && ((ITreeNode) child).getID().equals(id)) {
				return child;
			}
		}
		return null;
	}

	private static List<String> collectChildIds(TreeNode parent) {
		List<String> ids = new ArrayList<>();
		Enumeration<? extends TreeNode> en = parent.children();
		while (en.hasMoreElements()) {
			TreeNode child = en.nextElement();
			ids.add(child instanceof ITreeNode ? ((ITreeNode) child).getID() : Objects.toString(child));
		}
		return ids;
	}

	private @Nullable TreePath getTreePathForRef(@Nullable ICodeNodeRef ref) {
		if (ref == null) {
			return null;
		}
		ITreeNode node = nodeCache.makeFrom(ref);
		if (node.getParent() == null) {
			if (DEBUG) {
				LOG.warn("Resolving node not from tree: {}", node);
			}
			ITreeNode treeNode = ((JRoot) tree.getModel().getRoot()).searchNode(node);
			if (treeNode == null) {
				if (DEBUG) {
					LOG.error("Node not found in tree: {}", node);
				}
				return null;
			}
			node = treeNode;
		}
		TreeNode[] pathNodes = ((DefaultTreeModel) tree.getModel()).getPathToRoot(node);
		if (pathNodes == null) {
			return null;
		}
		return new TreePath(pathNodes);
	}

	private static List<TreePath> collectExpandedPaths(JTree tree) {
		TreePath root = tree.getPathForRow(0);
		Enumeration<TreePath> expandedDescendants = tree.getExpandedDescendants(root);
		if (expandedDescendants == null) {
			return Collections.emptyList();
		}
		List<TreePath> expandedPaths = new ArrayList<>();
		while (expandedDescendants.hasMoreElements()) {
			TreePath path = expandedDescendants.nextElement();
			if (path.getPathCount() > 1) {
				expandedPaths.add(path);
			}
		}
		// filter out sub-paths
		expandedPaths.sort(PATH_LENGTH_REVERSE); // put the longest paths before sub-paths
		List<TreePath> result = new ArrayList<>();
		for (TreePath path : expandedPaths) {
			if (!isSubPath(result, path)) {
				result.add(path);
			}
		}
		return result;
	}

	private static boolean isSubPath(List<TreePath> paths, TreePath path) {
		for (TreePath addedPath : paths) {
			if (path.isDescendant(addedPath)) {
				return true;
			}
		}
		return false;
	}

	private RootNode getRoot() {
		return mainWindow.getWrapper().getDecompiler().getRoot();
	}
}
