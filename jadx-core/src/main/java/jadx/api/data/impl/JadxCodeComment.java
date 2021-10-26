package jadx.api.data.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.data.ICodeComment;
import jadx.api.data.IJavaCodeRef;
import jadx.api.data.IJavaNodeRef;

public class JadxCodeComment implements ICodeComment {

	private IJavaNodeRef nodeRef;
	@Nullable
	private IJavaCodeRef codeRef;
	private String comment;

	public JadxCodeComment(IJavaNodeRef nodeRef, String comment) {
		this(nodeRef, null, comment);
	}

	public JadxCodeComment(IJavaNodeRef nodeRef, @Nullable IJavaCodeRef codeRef, String comment) {
		this.nodeRef = nodeRef;
		this.codeRef = codeRef;
		this.comment = comment;
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

	@Nullable
	@Override
	public IJavaCodeRef getCodeRef() {
		return codeRef;
	}

	public void setCodeRef(@Nullable IJavaCodeRef codeRef) {
		this.codeRef = codeRef;
	}

	@Override
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public int compareTo(@NotNull ICodeComment other) {
		int cmpNodeRef = this.getNodeRef().compareTo(other.getNodeRef());
		if (cmpNodeRef != 0) {
			return cmpNodeRef;
		}
		if (this.getCodeRef() != null && other.getCodeRef() != null) {
			return this.getCodeRef().compareTo(other.getCodeRef());
		}
		return this.getComment().compareTo(other.getComment());
	}

	@Override
	public String toString() {
		return "JadxCodeComment{" + nodeRef
				+ ", ref=" + codeRef
				+ ", comment='" + comment + '\''
				+ '}';
	}
}
