package jadx.api.data.impl;

import jadx.api.data.ICodeComment;
import jadx.api.data.IJavaNodeRef;

public class JadxCodeComment implements ICodeComment {

	private IJavaNodeRef nodeRef;
	private String comment;
	private int offset;

	public JadxCodeComment(IJavaNodeRef nodeRef, String comment) {
		this(nodeRef, comment, -1);
	}

	public JadxCodeComment(IJavaNodeRef nodeRef, String comment, int offset) {
		this.nodeRef = nodeRef;
		this.comment = comment;
		this.offset = offset;
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
	public String toString() {
		return "JadxCodeComment{" + nodeRef + ", comment='" + comment + '\'' + ", offset=" + offset + '}';
	}
}
