package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.data.ICodeComment;
import jadx.api.data.ICodeData;
import jadx.api.data.IJavaNodeRef;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;

@JadxVisitor(
		name = "Attach comments",
		desc = "Attach comments",
		runBefore = {
				ProcessInstructionsVisitor.class
		}
)
public class AttachCommentsVisitor extends AbstractVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(AttachCommentsVisitor.class);

	private final CommentsData cachedCommentsData = new CommentsData();

	@Override
	public boolean visit(ClassNode cls) {
		List<ICodeComment> clsComments = getCommentsData(cls);
		if (!clsComments.isEmpty()) {
			applyComments(cls, clsComments);
		}
		cls.getInnerClasses().forEach(this::visit);
		return false;
	}

	private static void applyComments(ClassNode cls, List<ICodeComment> clsComments) {
		for (ICodeComment comment : clsComments) {
			IJavaNodeRef nodeRef = comment.getNodeRef();
			switch (nodeRef.getType()) {
				case CLASS:
					addComment(cls, comment.getComment());
					break;

				case FIELD:
					FieldNode fieldNode = cls.searchFieldByShortId(nodeRef.getShortId());
					if (fieldNode == null) {
						LOG.warn("Field reference not found: {}", nodeRef);
					} else {
						addComment(fieldNode, comment.getComment());
					}
					break;

				case METHOD:
					MethodNode methodNode = cls.searchMethodByShortId(nodeRef.getShortId());
					if (methodNode == null) {
						LOG.warn("Method reference not found: {}", nodeRef);
					} else {
						int offset = comment.getOffset();
						if (offset < 0) {
							addComment(methodNode, comment.getComment());
						} else {
							try {
								InsnNode insn = methodNode.getInstructions()[offset];
								addComment(insn, comment.getComment());
							} catch (Exception e) {
								LOG.warn("Insn reference not found in: {} node with offset: {}", nodeRef, offset);
							}
						}
					}
					break;
			}
		}
	}

	private static void addComment(IAttributeNode node, String comment) {
		node.remove(AType.CODE_COMMENTS);
		node.addAttr(AType.CODE_COMMENTS, comment);
	}

	private static final class CommentsData {
		long updateId;
		Map<String, List<ICodeComment>> clsCommentsMap;
	}

	private List<ICodeComment> getCommentsData(ClassNode cls) {
		ICodeData additionalData = cls.root().getArgs().getCodeData();
		if (additionalData == null || additionalData.getComments().isEmpty()) {
			return Collections.emptyList();
		}
		synchronized (cachedCommentsData) {
			CommentsData commentsData = this.cachedCommentsData;
			if (commentsData.updateId != additionalData.getUpdateId()) {
				updateCommentsData(additionalData, commentsData);
			}
			List<ICodeComment> clsComments = commentsData.clsCommentsMap.get(cls.getClassInfo().getFullName());
			if (clsComments == null) {
				return Collections.emptyList();
			}
			return clsComments;
		}
	}

	private static void updateCommentsData(ICodeData data, CommentsData commentsData) {
		Map<String, List<ICodeComment>> map = new HashMap<>();
		for (ICodeComment comment : data.getComments()) {
			String declClsId = comment.getNodeRef().getDeclaringClass();
			List<ICodeComment> comments = map.computeIfAbsent(declClsId, s -> new ArrayList<>());
			comments.add(comment);
		}
		commentsData.clsCommentsMap = map;
		commentsData.updateId = data.getUpdateId();
	}
}
