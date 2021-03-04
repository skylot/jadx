package jadx.api.data.impl;

import java.util.Comparator;

import org.jetbrains.annotations.NotNull;

import jadx.api.data.ICodeComment;
import jadx.api.data.IJavaNodeRef;

public class JadxCodeComment implements ICodeComment {

	private IJavaNodeRef nodeRef;
	private String comment;
	private int offset;
	private AttachType attachType;

	public JadxCodeComment(IJavaNodeRef nodeRef, String comment) {
		this(nodeRef, comment, -1, null);
	}

	public JadxCodeComment(IJavaNodeRef nodeRef, String comment, int offset) {
		this(nodeRef, comment, offset, null);
	}

	public JadxCodeComment(IJavaNodeRef nodeRef, String comment, int offset, AttachType attachType) {
		this.nodeRef = nodeRef;
		this.comment = comment;
		this.offset = offset;
		this.attachType = attachType;
	}

	public JadxCodeComment() {
		// for json deserialization
	}

	@Override
	public IJavaNodeRef getNodeRef() {
		return nodeRef;
	}

	public void setNodeRef(IJavaNodeRef nodeRef) {
		this.nodeRef = nodeRef;
	}

	@Override
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	@Override
	public AttachType getAttachType() {
		return attachType;
	}

	public void setAttachType(AttachType attachType) {
		this.attachType = attachType;
	}

	private static final Comparator<ICodeComment> COMPARATOR = Comparator
			.comparing(ICodeComment::getNodeRef)
			.thenComparing(ICodeComment::getOffset);

	@Override
	public int compareTo(@NotNull ICodeComment other) {
		return COMPARATOR.compare(this, other);
	}

	@Override
	public String toString() {
		return "JadxCodeComment{" + nodeRef + ", comment='" + comment + '\'' + ", offset=" + offset + '}';
	}
}
