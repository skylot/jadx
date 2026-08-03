package jadx.gui.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import jadx.api.JavaNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.TextNode;

/**
 * A FilterableTreeModel wraps a backing DefaultTreeModel, providing dynamic filtering over the
 * decompilation package tree. This filtering is done at the model level, to prevent oddities such
 * as zero-width rows disrupting a keyboard user experience or otherwise causing problems.
 *
 * This class is specialised to filter the main UI pane displaying the decompilation tree. If the
 * fullly qualified path of a tree element is obtainable, filtering will check against that, falling
 * back to the standard name if the former is unavailable.
 */
class FilterableTreeModel implements TreeModel {

	// the backing DefaultTreeModel, containing all the elements of the tree that the filtering is done
	// over.
	DefaultTreeModel backing;

	// the filter string, with an empty string implying no filter.
	String filter;

	/**
	 * Constructs a FilterableTreeModel with given root node, to be used as the root of the unfiltered
	 * tree.
	 *
	 * @param root the root node. Passed directly to the constructor for DefaultTreeModel.
	 */
	public FilterableTreeModel(TreeNode root) {
		this.backing = new DefaultTreeModel(root);
		this.filter = "";
	}

	/**
	 * Sets the filter of the tree, refreshing it's node structure.
	 *
	 * This calls `nodeStructureChanged` on the root of the tree, implying a complete refresh of the
	 * tree data. Unfortunately the declarative nature of the filtering makes it infeasible to give a
	 * more specific refresh, which would potentially preserve open more of the current state of the
	 * tree.
	 *
	 * @param newFilter the new filter string, or "" to unset the filter.
	 */
	public void setFilter(String newFilter) {
		this.filter = newFilter;

		// setFilter may be invoked from a Timer thread; this means we may not be in the swing UI thread,
		// but nodeStructureChanged must be run on the swing thread!
		SwingUtilities.invokeLater(() -> nodeStructureChanged((TreeNode) getRoot()));
	}

	public String getFilter() {
		return filter;
	}

	/**
	 * Determines if a given node matches the current filter.
	 *
	 * @param node the node in question
	 * @return true if the filter is considered matched and the node should be displayed in the tree.
	 */
	private boolean matchesFilter(Object node) {

		if (filter.equals("")) {
			return true; // no filter means show everything
		}

		// `JNode`s are elements in the tree that correspond to a Jadx structure e.g. a JClass or JMethod.
		// Most elements in the tree should be these; top level elements such as the root 'source code' node
		// are not, so we unconditionally show non-JNodes.
		if (node instanceof JNode) {
			JavaNode javaNode = ((JNode) node).getJavaNode();

			// if possible, retrieve the fully qualified name (i.e. including full pacakge path) for filtering,
			// else rely on the default name.
			String name = ((JNode) node).getName();
			if (javaNode != null) {
				name = javaNode.getFullName();
			}

			// there are still some cases where the default name is still null. In this case it's better to show
			// these than hide.
			if (name == null) {
				return true;
			}

			// if we match the filter, non-case-sensitively, display the node.
			if (name.toLowerCase().contains(filter.toLowerCase())) {
				return true;
			}

			// if we do not match the filter but any of our children do, display the node.
			for (TreeNode x : (Iterable<TreeNode>) ((JNode) node).children()::asIterator) {
				if (x instanceof TextNode) {
					continue;
				}
				if (matchesFilter(x)) {
					return true;
				}

			}

			// otherwise, hide the node.
			return false;
		}

		return true;
	}

	@Override
	public Object getRoot() {
		return backing.getRoot();
	}

	@Override
	public Object getChild(Object parent, int index) {

		// whilst getFilteredChildren acts the same as just going to the backing directly when no filter is
		// set, it must construct the entire list, which requires N calls to getChild on backing (in
		// getUnfilteredChildren) rather than 1 if we short-circuit it like this.
		if (filter.equals("")) {
			return backing.getChild(parent, index);
		}

		return getFilteredChildren(parent).get(index);
	}

	@Override
	public int getChildCount(Object parent) {

		if (filter.equals("")) {
			return backing.getChildCount(parent);
		}

		return getFilteredChildren(parent).size();
	}

	private List<Object> getUnfilteredChildren(Object parent) {

		int numChildren = backing.getChildCount(parent);
		List<Object> results = new ArrayList<Object>();

		for (int i = 0; i < numChildren; i++) {
			results.add(backing.getChild(parent, i));
		}

		return results;
	}

	private List<Object> getFilteredChildren(Object parent) {
		List<Object> unfiltered = getUnfilteredChildren(parent);

		return unfiltered.stream().filter(obj -> matchesFilter(obj)).collect(Collectors.toList());
	}

	@Override
	public boolean isLeaf(Object node) {
		return backing.isLeaf(node);
	}

	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		backing.valueForPathChanged(path, newValue);
	}

	@Override
	public int getIndexOfChild(Object parent, Object child) {
		return backing.getIndexOfChild(parent, child);
	}

	@Override
	public void addTreeModelListener(TreeModelListener l) {
		backing.addTreeModelListener(l);
	}

	@Override
	public void removeTreeModelListener(TreeModelListener l) {
		backing.removeTreeModelListener(l);
	}

	public void setRoot(TreeNode node) {
		backing.setRoot(node);
	}

	public void reload() {
		backing.reload();
	}

	public void nodeStructureChanged(TreeNode node) {
		backing.nodeStructureChanged(node);
	}

	public TreeNode[] getPathToRoot(TreeNode node) {
		return backing.getPathToRoot(node);
	}

}
