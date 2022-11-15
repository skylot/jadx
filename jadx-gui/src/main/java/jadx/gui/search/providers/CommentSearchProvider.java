package jadx.gui.search.providers;

import java.util.List;
import java.util.Objects;

import javax.swing.Icon;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.data.ICodeComment;
import jadx.api.data.IJavaCodeRef;
import jadx.api.data.IJavaNodeRef;
import jadx.api.metadata.annotations.InsnCodeOffset;
import jadx.gui.JadxWrapper;
import jadx.gui.jobs.Cancelable;
import jadx.gui.search.ISearchProvider;
import jadx.gui.search.SearchSettings;
import jadx.gui.settings.JadxProject;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.JumpPosition;

public class CommentSearchProvider implements ISearchProvider {
	private static final Logger LOG = LoggerFactory.getLogger(CommentSearchProvider.class);
	private final JadxWrapper wrapper;
	private final CacheObject cacheObject;
	private final JadxProject project;
	private final SearchSettings searchSettings;

	private int progress = 0;

	public CommentSearchProvider(MainWindow mw, SearchSettings searchSettings) {
		this.wrapper = mw.getWrapper();
		this.cacheObject = mw.getCacheObject();
		this.project = mw.getProject();
		this.searchSettings = searchSettings;
	}

	@Override
	public @Nullable JNode next(Cancelable cancelable) {
		while (true) {
			if (cancelable.isCanceled()) {
				return null;
			}
			List<ICodeComment> comments = project.getCodeData().getComments();
			if (progress >= comments.size()) {
				return null;
			}
			ICodeComment comment = comments.get(progress++);
			JNode result = isMatch(searchSettings, comment);
			if (result != null) {
				return result;
			}
		}
	}

	@Nullable
	private JNode isMatch(SearchSettings searchSettings, ICodeComment comment) {
		boolean all = searchSettings.getSearchString().isEmpty();
		if (all || searchSettings.isMatch(comment.getComment())) {
			JNode refNode = getRefNode(comment);
			if (refNode != null) {
				JClass activeCls = searchSettings.getActiveCls();
				if (activeCls == null || Objects.equals(activeCls, refNode.getRootClass())) {
					return getCommentNode(comment, refNode);
				}
			} else {
				LOG.warn("Failed to get ref node for comment: {}", comment);
			}
		}
		return null;
	}

	private @NotNull RefCommentNode getCommentNode(ICodeComment comment, JNode refNode) {
		IJavaNodeRef nodeRef = comment.getNodeRef();
		if (nodeRef.getType() == IJavaNodeRef.RefType.METHOD && comment.getCodeRef() != null) {
			return new CodeCommentNode((JMethod) refNode, comment);
		}
		return new RefCommentNode(refNode, comment.getComment());
	}

	@Nullable
	private JNode getRefNode(ICodeComment comment) {
		IJavaNodeRef nodeRef = comment.getNodeRef();
		JavaClass javaClass = wrapper.searchJavaClassByOrigClassName(nodeRef.getDeclaringClass());
		if (javaClass == null) {
			return null;
		}
		JNodeCache nodeCache = cacheObject.getNodeCache();
		switch (nodeRef.getType()) {
			case CLASS:
				return nodeCache.makeFrom(javaClass);

			case FIELD:
				for (JavaField field : javaClass.getFields()) {
					if (field.getFieldNode().getFieldInfo().getShortId().equals(nodeRef.getShortId())) {
						return nodeCache.makeFrom(field);
					}
				}
				break;

			case METHOD:
				for (JavaMethod mth : javaClass.getMethods()) {
					if (mth.getMethodNode().getMethodInfo().getShortId().equals(nodeRef.getShortId())) {
						return nodeCache.makeFrom(mth);
					}
				}
				break;
		}
		return null;
	}

	private static final class CodeCommentNode extends RefCommentNode {
		private static final long serialVersionUID = 6208192811789176886L;

		private final int offset;
		private JumpPosition pos;

		public CodeCommentNode(JMethod node, ICodeComment comment) {
			super(node, comment.getComment());
			IJavaCodeRef codeRef = Objects.requireNonNull(comment.getCodeRef(), "Null comment code ref");
			this.offset = codeRef.getIndex();
		}

		@Override
		public int getPos() {
			return getCachedPos().getPos();
		}

		private synchronized JumpPosition getCachedPos() {
			if (pos == null) {
				pos = getJumpPos();
			}
			return pos;
		}

		/**
		 * Lazy decompilation to get comment location if requested
		 */
		private JumpPosition getJumpPos() {
			JavaMethod javaMethod = ((JMethod) node).getJavaMethod();
			ICodeInfo codeInfo = javaMethod.getTopParentClass().getCodeInfo();
			int methodDefPos = javaMethod.getDefPos();
			JumpPosition jump = codeInfo.getCodeMetadata().searchDown(methodDefPos,
					(pos, ann) -> ann instanceof InsnCodeOffset && ((InsnCodeOffset) ann).getOffset() == offset
							? new JumpPosition(node, pos)
							: null);
			if (jump != null) {
				return jump;
			}
			return new JumpPosition(node);
		}
	}

	private static class RefCommentNode extends JNode {
		private static final long serialVersionUID = 3887992236082515752L;

		protected final JNode node;
		protected final String comment;

		public RefCommentNode(JNode node, String comment) {
			this.node = node;
			this.comment = comment;
		}

		@Override
		public JClass getRootClass() {
			return node.getRootClass();
		}

		@Override
		public JavaNode getJavaNode() {
			return node.getJavaNode();
		}

		@Override
		public JClass getJParent() {
			return node.getJParent();
		}

		@Override
		public Icon getIcon() {
			return node.getIcon();
		}

		@Override
		public String getSyntaxName() {
			return SyntaxConstants.SYNTAX_STYLE_NONE; // comment is always plain text
		}

		@Override
		public String makeString() {
			return node.makeString();
		}

		@Override
		public String makeLongString() {
			return node.makeLongString();
		}

		@Override
		public String makeStringHtml() {
			return node.makeStringHtml();
		}

		@Override
		public String makeLongStringHtml() {
			return node.makeLongStringHtml();
		}

		@Override
		public boolean disableHtml() {
			return node.disableHtml();
		}

		@Override
		public int getPos() {
			return node.getPos();
		}

		@Override
		public String getTooltip() {
			return node.getTooltip();
		}

		@Override
		public String makeDescString() {
			return comment;
		}

		@Override
		public boolean hasDescString() {
			return true;
		}
	}

	@Override
	public int progress() {
		return progress;
	}

	@Override
	public int total() {
		return project.getCodeData().getComments().size();
	}
}
