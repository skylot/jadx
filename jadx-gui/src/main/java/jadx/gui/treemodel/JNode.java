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
import jadx.api.gui.tree.ITreeNode;
import jadx.api.metadata.ICodeNodeRef;
import jadx.core.utils.ListUtils;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.tab.TabbedPane;

public abstract class JNode extends DefaultMutableTreeNode implements ITreeNode, Comparable<JNode> {

	private static final long serialVersionUID = -5154479091781041008L;

	public abstract JClass getJParent();

	/**
	 * Return top level JClass or self if already at top.
	 */
	public JClass getRootClass() {
		return null;
	}

	public JavaNode getJavaNode() {
		return null;
	}

	@Override
	public ICodeNodeRef getCodeNodeRef() {
		return null;
	}

	public @Nullable ContentPanel getContentPanel(TabbedPane tabbedPane) {
		return null;
	}

	public String getSyntaxName() {
		return SyntaxConstants.SYNTAX_STYLE_NONE;
	}

	public ICodeInfo getCodeInfo() {
		return ICodeInfo.EMPTY;
	}

	public boolean isEditable() {
		return false;
	}

	@Override
	public String getName() {
		JavaNode javaNode = getJavaNode();
		if (javaNode == null) {
			return null;
		}
		return javaNode.getName();
	}

	public boolean supportsQuickTabs() {
		return true;
	}

	public @Nullable JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		return null;
	}

	@Override
	public String getID() {
		return makeString();
	}

	public abstract String makeString();

	public String makeStringHtml() {
		return makeString();
	}

	public String makeDescString() {
		return null;
	}

	public boolean hasDescString() {
		return false;
	}

	public String makeLongString() {
		return makeString();
	}

	public String makeLongStringHtml() {
		return makeLongString();
	}

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

	public String getTooltip() {
		return makeLongStringHtml();
	}

	public @Nullable JNode searchNode(Predicate<JNode> filter) {
		Enumeration<?> en = this.children();
		while (en.hasMoreElements()) {
			JNode node = (JNode) en.nextElement();
			if (filter.test(node)) {
				return node;
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
			JNode node = (JNode) en.nextElement();
			if (filter.test(node)) {
				this.remove(node);
				return node;
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
