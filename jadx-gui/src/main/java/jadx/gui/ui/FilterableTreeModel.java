package jadx.gui.ui;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourceType;
import jadx.gui.jobs.SimpleTask;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.treemodel.JRoot;
import jadx.gui.treemodel.TextNode;
import jadx.gui.utils.UiUtils;

/**
 * A FilterableTreeModel provides dynamic filtering over the decompilation package tree.
 * This filtering is done at the model level, to prevent oddities such as zero-width rows disrupting
 * a keyboard user experience or otherwise causing problems.
 * <p>
 * This class is specialized to filter the main UI pane displaying the decompilation tree. If the
 * fully qualified path of a tree element is obtainable, filtering will check against that, falling
 * back to the standard name if the former is unavailable.
 */
class FilterableTreeModel extends DefaultTreeModel {
	private static final Logger LOG = LoggerFactory.getLogger(FilterableTreeModel.class);

	private final MainWindow mainWindow;

	/**
	 * The filter string
	 */
	private volatile String filter;

	/**
	 * All nodes to filtered paths (including middle nodes)
	 */
	private final Set<TreeNode> filteredTreeNodes;

	/**
	 * Store nodes expanded automatically to prevent re-expand for nodes collapsed by user
	 */
	private final Set<JNode> autoExpandedNodes;

	/**
	 * Very often 'getChild' method called for all children after 'getChildCount' call.
	 * Save children list for fast child return.
	 */
	private final CacheNode cacheNode = new CacheNode();

	public FilterableTreeModel(MainWindow mainWindow, TreeNode root) {
		super(root);
		this.mainWindow = mainWindow;
		this.filter = "";
		this.filteredTreeNodes = new HashSet<>();
		this.autoExpandedNodes = new HashSet<>();
	}

	/**
	 * Sets the filter of the tree by pre-computing tree paths matching the filter and refreshing the
	 * tree's node structure.
	 * <p>
	 * This calls `nodeStructureChanged` on the root of the tree, implying a complete refresh of the
	 * tree data. Unfortunately the declarative nature of the filtering makes it infeasible to give a
	 * more specific refresh, which would potentially preserve open more of the current state of the
	 * tree.
	 *
	 * @param newFilter the new filter string, or "" to unset the filter.
	 */
	public synchronized void setFilter(String newFilter) {
		this.filter = newFilter;
		this.autoExpandedNodes.clear();
		this.cacheNode.clear();
		LOG.debug("New tree filter '{}'", newFilter);
		applyFilterFieldOutline("");
		collectFilteredPaths();
		SwingUtilities.invokeLater(() -> this.nodeStructureChanged((TreeNode) getRoot()));
		SwingUtilities.invokeLater(() -> expandVisibleFilteredNodes(mainWindow.getTree()));
	}

	/**
	 * Filter thread safe nodeStructureChanged event.
	 * This should ensure that all treeListeners get the same filter value per event.
	 */
	@Override
	public void nodeStructureChanged(TreeNode node) {
		super.nodeStructureChanged(node);
	}

	/**
	 * If filter enabled, expand all visible nodes.
	 */
	public void expandVisibleFilteredNodes(JTree tree) {
		if (filter.isEmpty()) {
			return;
		}
		Rectangle rect = tree.getVisibleRect();
		int startRow = tree.getClosestRowForLocation(0, rect.y);
		int bottom = rect.y + rect.height - 1;
		while (true) {
			int lastRow = tree.getClosestRowForLocation(0, bottom);
			if (lastRow <= startRow) {
				break;
			}
			// limit updates for one iteration
			int last = Math.min(startRow + 20, lastRow);
			for (int i = startRow; i <= lastRow; i++) {
				TreePath path = tree.getPathForRow(i);
				JNode node = (JNode) path.getLastPathComponent();
				if (node instanceof JClass) {
					// don't auto expand methods
				} else {
					if (autoExpandedNodes.add(node)) {
						tree.expandPath(path);
					}
				}
			}
			startRow = last;
		}
	}

	private void applyFilterFieldOutline(String outlineType) {
		UiUtils.uiRun(() -> mainWindow.getTreeFilterField().putClientProperty("JComponent.outline", outlineType));
	}

	private void collectFilteredPaths() {
		UiUtils.notUiThreadGuard();
		filteredTreeNodes.clear();
		if (filter.isEmpty()) {
			return;
		}

		Object rootNode = this.getRoot();
		if (!(rootNode instanceof JRoot)) {
			return; // root node is null or of a different type
		}
		int nodesCount = 0;
		int filteredCount = 0;
		Enumeration<TreeNode> en = ((JRoot) rootNode).depthFirstEnumeration();
		while (en.hasMoreElements()) {
			TreeNode node = en.nextElement();
			nodesCount++;
			if (matchesFilter(node)) {
				addPathNodes(node);
				filteredCount++;
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("Total nodes: {}, filtered: {}", nodesCount, filteredCount);
		}
		if (filteredTreeNodes.isEmpty()) {
			applyFilterFieldOutline("error");
		}
	}

	private void addPathNodes(TreeNode node) {
		if (!filteredTreeNodes.add(node)) {
			return;
		}
		TreeNode parent = node.getParent();
		while (parent != null) {
			if (!filteredTreeNodes.add(parent)) {
				break;
			}
			parent = parent.getParent();
		}
	}

	/**
	 * Determines if a given node matches the current filter.
	 *
	 * @param node the node in question
	 * @return true if the filter is considered matched and the node should be displayed in the tree.
	 */
	private boolean matchesFilter(Object node) {
		if (node instanceof TextNode || node instanceof JMethod) {
			return false;
		}
		if (node instanceof JResource) {
			JResource res = (JResource) node;
			if (res.getType() == JResource.JResType.FILE && res.getResFile().getType() == ResourceType.ARSC) {
				loadInnerResources(res);
			}
		}
		if (node instanceof JNode) {
			JNode jNode = (JNode) node;
			String name = jNode.makeString();
			if (name == null) {
				LOG.warn("Node {} has null UI string", node);
				return false;
			}
			return name.toLowerCase().contains(filter.toLowerCase());
		}
		return false;
	}

	private void loadInnerResources(JResource res) {
		// load inner resource of resource.arsc
		SimpleTask loadTask = res.getLoadTask();
		if (loadTask != null) {
			try {
				Future<TaskStatus> load = mainWindow.getBackgroundExecutor().executeWithFuture(loadTask);
				load.get(); // wait for completion
			} catch (Exception e) {
				LOG.warn("Failed to load resource", e);
			}
		}
	}

	private static final class CacheNode {
		private @Nullable TreeNode parent;
		private final List<TreeNode> children = new ArrayList<>();

		public void clear() {
			parent = null;
			children.clear();
		}
	}

	@Override
	public Object getChild(Object parent, int index) {
		if (cacheNode.parent == parent) {
			return cacheNode.children.get(index);
		}
		if (filter.isEmpty() || parent instanceof JClass /* allow to expand and view all methods */) {
			return super.getChild(parent, index);
		}
		int i = 0;
		Enumeration<? extends TreeNode> en = ((TreeNode) parent).children();
		while (en.hasMoreElements()) {
			TreeNode child = en.nextElement();
			if (filteredTreeNodes.contains(child)) {
				if (i == index) {
					return child;
				}
				i++;
			}
		}
		throw new IllegalArgumentException("No child at index " + index);
	}

	@Override
	public int getChildCount(Object parent) {
		if (filter.isEmpty()) {
			return super.getChildCount(parent);
		}
		TreeNode parentNode = (TreeNode) parent;
		if (!filteredTreeNodes.contains(parentNode)) {
			return 0;
		}
		cacheNode.parent = parentNode;
		List<TreeNode> children = cacheNode.children;
		children.clear();

		int count = 0;
		Enumeration<? extends TreeNode> en = parentNode.children();
		while (en.hasMoreElements()) {
			TreeNode child = en.nextElement();
			if (filteredTreeNodes.contains(child)) {
				children.add(child);
				count++;
			}
		}
		return count;
	}
}
