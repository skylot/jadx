package jadx.core.dex.visitors;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.data.CodeRefType;
import jadx.api.data.ICodeComment;
import jadx.api.data.ICodeData;
import jadx.api.data.IJavaCodeRef;
import jadx.api.data.IJavaNodeRef;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

@JadxVisitor(
		name = "AttachComments",
		desc = "Attach user code comments",
		runBefore = {
				ProcessInstructionsVisitor.class
		}
)
public class AttachCommentsVisitor extends AbstractVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(AttachCommentsVisitor.class);

	private Map<String, List<ICodeComment>> clsCommentsMap;

	@Override
	public void init(RootNode root) throws JadxException {
		updateCommentsData(root.getArgs().getCodeData());
		root.registerCodeDataUpdateListener(this::updateCommentsData);
	}

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
						IJavaCodeRef codeRef = comment.getCodeRef();
						if (codeRef == null) {
							addComment(methodNode, comment.getComment());
						} else {
							processCustomAttach(methodNode, codeRef, comment);
						}
					}
					break;
			}
		}
	}

	private static InsnNode getInsnByOffset(MethodNode mth, int offset) {
		try {
			return mth.getInstructions()[offset];
		} catch (Exception e) {
			LOG.warn("Insn reference not found in: {} with offset: {}", mth, offset);
			return null;
		}
	}

	private static void processCustomAttach(MethodNode mth, IJavaCodeRef codeRef, ICodeComment comment) {
		CodeRefType attachType = codeRef.getAttachType();
		switch (attachType) {
			case INSN: {
				InsnNode insn = getInsnByOffset(mth, codeRef.getIndex());
				addComment(insn, comment.getComment());
				break;
			}
			default:
				throw new JadxRuntimeException("Unexpected attach type: " + attachType);
		}
	}

	private static void addComment(@Nullable IAttributeNode node, String comment) {
		if (node == null) {
			return;
		}
		node.addAttr(AType.CODE_COMMENTS, comment);
	}

	private List<ICodeComment> getCommentsData(ClassNode cls) {
		if (clsCommentsMap == null) {
			return Collections.emptyList();
		}
		List<ICodeComment> clsComments = clsCommentsMap.get(cls.getClassInfo().getRawName());
		if (clsComments == null) {
			return Collections.emptyList();
		}
		return clsComments;
	}

	private void updateCommentsData(@Nullable ICodeData data) {
		if (data == null) {
			this.clsCommentsMap = Collections.emptyMap();
		} else {
			this.clsCommentsMap = data.getComments().stream()
					.collect(Collectors.groupingBy(c -> c.getNodeRef().getDeclaringClass()));
		}
	}
}
