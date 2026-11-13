package jadx.gui.treemodel;

import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Predicate;

import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeInfo;
import jadx.api.JavaNode;
import jadx.api.gui.IMainWindow;
import jadx.api.gui.code.JadxContentPanel;
import jadx.api.gui.tree.ICodeTreeNode;
import jadx.api.gui.tree.ILoadableTreeNode;
import jadx.api.gui.tree.ITreeNodeDetails;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.resources.ResourceContentType;
import jadx.core.utils.ListUtils;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.tab.TabbedPane;

public abstract class JNode extends DefaultMutableTreeNode
		implements ICodeTreeNode, ITreeNodeDetails, ILoadableTreeNode, Comparable<JNode> {

	private static final long serialVersionUID = -5154479091781041008L;

	public abstract @Nullable JClass getJParent();

	/**
	 * Return top level JClass or self if already at top.
	 */
	public JClass getRootClass() {
		return null;
	}

	public @Nullable JavaNode getJavaNode() {
		return null;
	}

	public @Nullable ICodeNodeRef getCodeNodeRef() {
		return null;
	}

	@Override
	public boolean hasContent() {
		return false;
	}

	@Deprecated
	public @Nullable ContentPanel getContentPanel(TabbedPane tabbedPane) {
		return null;
	}

	@Override
	public @Nullable JadxContentPanel getContentPanel(IMainWindow mainWindow) {
		return getContentPanel(((MainWindow) mainWindow).getTabbedPane());
	}

	public String getSyntaxName() {
		return SyntaxConstants.SYNTAX_STYLE_NONE;
	}

	public ICodeInfo getCodeInfo() {
		return ICodeInfo.EMPTY;
	}

	@Override
	public void loadNode() {
		getCodeInfo();
	}

	public ResourceContentType getContentType() {
		return ResourceContentType.CONTENT_TEXT;
	}

	public boolean isEditable() {
		return false;
	}

	@Override
	public String getName() {
		JavaNode javaNode = getJavaNode();
		if (javaNode != null) {
			return javaNode.getName();
		}
		return makeString();
	}

	@Override
	public @Nullable JPopupMenu onTreePopupMenu(IMainWindow mainWindow) {
		if (mainWindow instanceof MainWindow) {
			return onTreePopupMenu((MainWindow) mainWindow);
		}
		return null;
	}

	public @Nullable JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		return null;
	}

	@Override
	public String getID() {
		return makeString();
	}

	public abstract String makeString();

	@Override
	public String makeStringHtml() {
		return makeString();
	}

	@Override
	public String makeDescString() {
		return null;
	}

	@Override
	public boolean hasDescString() {
		return false;
	}

	public String makeLongString() {
		return makeString();
	}

	@Override
	public String makeLongStringHtml() {
		return makeLongString();
	}

	@Override
	public boolean disableHtml() {
		return true;
	}

	public int getPos() {
		JavaNode javaNode = getJavaNode();
		if (javaNode == null) {
			return -1;
		}
		return javaNode.getDefPos();
	}

	@Override
	public String getTooltip() {
		return makeLongStringHtml();
	}

	public @Nullable JNode searchNode(Predicate<JNode> filter) {
		Enumeration<?> en = this.children();
		while (en.hasMoreElements()) {
			Object obj = en.nextElement();
			if (obj instanceof JNode) {
				JNode node = (JNode) obj;
				if (filter.test(node)) {
					return node;
				}
			}
		}
		return null;
	}

	public @Nullable JNode searchDepthNode(Predicate<JNode> filter) {
		Enumeration<?> en = this.breadthFirstEnumeration();
		while (en.hasMoreElements()) {
			Object obj = en.nextElement();
			if (obj instanceof JNode) {
				JNode node = (JNode) obj;
				if (filter.test(node)) {
					return node;
				}
			}
		}
		return null;
	}

	/**
	 * Remove and return first found node
	 */
	public @Nullable JNode removeNode(Predicate<JNode> filter) {
		Enumeration<?> en = this.children();
		while (en.hasMoreElements()) {
			Object obj = en.nextElement();
			if (obj instanceof JNode) {
				JNode node = (JNode) obj;
				if (filter.test(node)) {
					this.remove(node);
					return node;
				}
			}
		}
		return null;
	}

	public List<TreeNode> childrenList() {
		return ListUtils.enumerationToList(this.children());
	}

	private static final Comparator<JNode> COMPARATOR = Comparator
			.comparing(JNode::makeLongString)
			.thenComparingInt(JNode::getPos);

	@Override
	public int compareTo(@NotNull JNode other) {
		return COMPARATOR.compare(this, other);
	}

	@Override
	public String toString() {
		return makeString();
	}
}
