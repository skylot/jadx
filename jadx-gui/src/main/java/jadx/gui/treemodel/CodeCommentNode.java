package jadx.gui.treemodel;

import java.util.Objects;

import jadx.api.ICodeInfo;
import jadx.api.JavaMethod;
import jadx.api.data.ICodeComment;
import jadx.api.data.IJavaCodeRef;
import jadx.api.metadata.annotations.InsnCodeOffset;
import jadx.gui.utils.JumpPosition;

public final class CodeCommentNode extends RefCommentNode {
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
