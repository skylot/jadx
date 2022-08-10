package jadx.gui.treemodel;

import java.util.Comparator;
import java.util.Enumeration;
import java.util.function.Predicate;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeInfo;
import jadx.api.JavaNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.panel.ContentPanel;

public abstract class JNode extends DefaultMutableTreeNode implements Comparable<JNode> {

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

	@Nullable
	public ContentPanel getContentPanel(TabbedPane tabbedPane) {
		return null;
	}

	public String getSyntaxName() {
		return SyntaxConstants.SYNTAX_STYLE_NONE;
	}

	@NotNull
	public ICodeInfo getCodeInfo() {
		return ICodeInfo.EMPTY;
	}

	public boolean isEditable() {
		return false;
	}

	public abstract Icon getIcon();

	public String getName() {
		JavaNode javaNode = getJavaNode();
		if (javaNode == null) {
			return null;
		}
		return javaNode.getName();
	}

	public @Nullable JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		return null;
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
		Enumeration<?> en = this.breadthFirstEnumeration();
		while (en.hasMoreElements()) {
			JNode node = (JNode) en.nextElement();
			if (filter.test(node)) {
				return node;
			}
		}
		return null;
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
