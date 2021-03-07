package jadx.api.data.impl;

import java.util.Collections;
import java.util.List;

import jadx.api.data.ICodeComment;
import jadx.api.data.ICodeData;

public class JadxCodeData implements ICodeData {

	private long updateId = System.currentTimeMillis();
	private List<ICodeComment> comments = Collections.emptyList();

	@Override
	public long getUpdateId() {
		return updateId;
	}

	public void markUpdate() {
		updateId = System.currentTimeMillis();
	}

	@Override
	public List<ICodeComment> getComments() {
		return comments;
	}

	public void setComments(List<ICodeComment> comments) {
		markUpdate();
		this.comments = comments;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(updateId);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof JadxCodeData)) {
			return false;
		}
		JadxCodeData that = (JadxCodeData) o;
		return updateId == that.updateId;
	}
}
