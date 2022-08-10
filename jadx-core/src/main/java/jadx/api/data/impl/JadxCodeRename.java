package jadx.api.data.impl;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.data.ICodeRename;
import jadx.api.data.IJavaCodeRef;
import jadx.api.data.IJavaNodeRef;

public class JadxCodeRename implements ICodeRename {
	private IJavaNodeRef nodeRef;
	@Nullable
	private IJavaCodeRef codeRef;
	private String newName;

	public JadxCodeRename(IJavaNodeRef nodeRef, String newName) {
		this(nodeRef, null, newName);
	}

	public JadxCodeRename(IJavaNodeRef nodeRef, @Nullable IJavaCodeRef codeRef, String newName) {
		this.nodeRef = nodeRef;
		this.codeRef = codeRef;
		this.newName = newName;
	}

	public JadxCodeRename() {
		// used in json serialization
	}

	@Override
	public IJavaNodeRef getNodeRef() {
		return nodeRef;
	}

	public void setNodeRef(IJavaNodeRef nodeRef) {
		this.nodeRef = nodeRef;
	}

	@Override
	public IJavaCodeRef getCodeRef() {
		return codeRef;
	}

	public void setCodeRef(IJavaCodeRef codeRef) {
		this.codeRef = codeRef;
	}

	@Override
	public String getNewName() {
		return newName;
	}

	public void setNewName(String newName) {
		this.newName = newName;
	}

	@Override
	public int compareTo(@NotNull ICodeRename other) {
		int cmpNodeRef = this.getNodeRef().compareTo(other.getNodeRef());
		if (cmpNodeRef != 0) {
			return cmpNodeRef;
		}
		if (this.getCodeRef() != null && other.getCodeRef() != null) {
			return this.getCodeRef().compareTo(other.getCodeRef());
		}
		return this.getNewName().compareTo(other.getNewName());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ICodeRename)) {
			return false;
		}
		ICodeRename other = (ICodeRename) o;
		return getNodeRef().equals(other.getNodeRef())
				&& Objects.equals(getCodeRef(), other.getCodeRef());
	}

	@Override
	public int hashCode() {
		return 31 * getNodeRef().hashCode() + Objects.hashCode(getCodeRef());
	}

	@Override
	public String toString() {
		return "JadxCodeRename{" + nodeRef
				+ ", codeRef=" + codeRef
				+ ", newName='" + newName + '\''
				+ '}';
	}
}
