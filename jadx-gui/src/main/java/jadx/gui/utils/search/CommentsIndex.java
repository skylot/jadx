package jadx.gui.utils.search;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Icon;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

import jadx.api.CodePosition;
import jadx.api.ICodeInfo;
import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.data.ICodeComment;
import jadx.api.data.IJavaNodeRef;
import jadx.api.data.annotations.ICodeRawOffset;
import jadx.gui.JadxWrapper;
import jadx.gui.settings.JadxProject;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.JumpPosition;

public class CommentsIndex {

	private static final Logger LOG = LoggerFactory.getLogger(CommentsIndex.class);
	private final JadxWrapper wrapper;
	private final CacheObject cacheObject;
	private final JadxProject project;

	public CommentsIndex(JadxWrapper wrapper, CacheObject cacheObject, JadxProject project) {
		this.wrapper = wrapper;
		this.cacheObject = cacheObject;
		this.project = project;
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

	public Flowable<JNode> search(SearchSettings searchSettings) {
		List<ICodeComment> comments = project.getCodeData().getComments();
		if (comments == null || comments.isEmpty()) {
			return Flowable.empty();
		}
		LOG.debug("Total comments count: {}", comments.size());
		return Flowable.create(emitter -> {
			for (ICodeComment comment : comments) {
				JNode foundNode = isMatch(searchSettings, comment);
				if (foundNode != null) {
					emitter.onNext(foundNode);
				}
				if (emitter.isCancelled()) {
					return;
				}
			}
			emitter.onComplete();
		}, BackpressureStrategy.BUFFER);
	}

	private @NotNull RefCommentNode getCommentNode(ICodeComment comment, JNode refNode) {
		IJavaNodeRef nodeRef = comment.getNodeRef();
		if (nodeRef.getType() == IJavaNodeRef.RefType.METHOD && comment.getOffset() > 0) {
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
			this.offset = comment.getOffset();
		}

		@Override
		public int getLine() {
			return getCachedPos().getLine();
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
			int methodLine = javaMethod.getDecompiledLine();
			ICodeInfo codeInfo = javaMethod.getTopParentClass().getCodeInfo();
			for (Map.Entry<CodePosition, Object> entry : codeInfo.getAnnotations().entrySet()) {
				CodePosition codePos = entry.getKey();
				if (codePos.getOffset() == 0 && codePos.getLine() > methodLine) {
					Object ann = entry.getValue();
					if (ann instanceof ICodeRawOffset) {
						if (((ICodeRawOffset) ann).getOffset() == offset) {
							return new JumpPosition(node, codePos);
						}
					}
				}
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
			return node.getSyntaxName();
		}

		@Override
		public String makeString() {
			return node.makeString();
		}

		@Override
		public int getLine() {
			return node.getLine();
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
}
