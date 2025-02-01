package jadx.gui.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.metadata.ICodeNodeRef;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;
import jadx.gui.treemodel.JRoot;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class TreeExpansionService {
	private static final Logger LOG = LoggerFactory.getLogger(TreeExpansionService.class);
	private static final boolean DEBUG = UiUtils.JADX_GUI_DEBUG;

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
		List<TreePath> expandedPaths = new ArrayList<>();
		mainWindow.getBackgroundExecutor().execute(NLS.str("progress.load"),
				() -> {
					loadPaths(treeExpansions, expandedPaths);
					// send expand event to load sub-nodes and wait for completion
					UiUtils.uiRunAndWait(() -> expandedPaths.forEach(path -> {
						try {
							tree.fireTreeWillExpand(path);
						} catch (Exception e) {
							throw new JadxRuntimeException("Tree expand error", e);
						}
					}));
				},
				s -> {
					// expand paths after a loading task is finished
					expandedPaths.forEach(tree::expandPath);
				});
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

	private String savePath(TreePath path) {
		JNode node = (JNode) path.getLastPathComponent();
		if (node instanceof JPackage) {
			return "p:" + ((JPackage) node).getPkg().getRawFullName();
		}
		if (node instanceof JClass) {
			return "c:" + ((JClass) node).getCls().getRawName();
		}
		return Arrays.stream(path.getPath())
				.map(p -> ((JNode) p).getID())
				.skip(1) // skip root
				.collect(Collectors.joining("//", "t:", ""));
	}

	private @Nullable TreePath loadPath(String pathStr) {
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
		JNode current = (JNode) tree.getModel().getRoot();
		for (String nodeStr : pathArr) {
			JNode node = current.searchNode(n -> n.getID().equals(nodeStr));
			if (node == null) {
				if (DEBUG) {
					List<String> children = current.childrenList().stream()
							.map(n -> ((JNode) n).getID())
							.collect(Collectors.toList());
					LOG.warn("Failed to restore path: {}, node '{}' not found in '{}' children: {}",
							Arrays.toString(pathArr), nodeStr, current, children);
				}
				return null;
			}
			current = node;
		}
		return new TreePath(current.getPath());
	}

	private @Nullable TreePath getTreePathForRef(@Nullable ICodeNodeRef ref) {
		if (ref == null) {
			return null;
		}
		JNode node = nodeCache.makeFrom(ref);
		if (node.getParent() == null) {
			if (DEBUG) {
				LOG.warn("Resolving node not from tree: {}", node);
			}
			JNode treeNode = ((JRoot) tree.getModel().getRoot()).searchNode(node);
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
