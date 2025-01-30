package jadx.gui.search.providers;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.data.ICodeComment;
import jadx.api.data.IJavaNodeRef;
import jadx.gui.JadxWrapper;
import jadx.gui.jobs.Cancelable;
import jadx.gui.search.ISearchProvider;
import jadx.gui.search.SearchSettings;
import jadx.gui.settings.JadxProject;
import jadx.gui.treemodel.CodeCommentNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.RefCommentNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.JNodeCache;

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
		while (!cancelable.isCanceled()) {
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
		return null;
	}

	@Nullable
	private JNode isMatch(SearchSettings searchSettings, ICodeComment comment) {
		boolean all = searchSettings.getSearchString().isEmpty();
		if (all || searchSettings.isMatch(comment.getComment())) {
			JNode refNode = getRefNode(comment);
			if (refNode != null) {
				if (searchSettings.getSearchPackage() != null
						&& !refNode.getRootClass().getCls().getJavaPackage().isDescendantOf(searchSettings.getSearchPackage())) {
					return null;
				}
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

	@Override
	public int progress() {
		return progress;
	}

	@Override
	public int total() {
		return project.getCodeData().getComments().size();
	}
}
