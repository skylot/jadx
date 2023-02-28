package jadx.api.data.impl;

import java.util.Collections;
import java.util.List;

import jadx.api.data.ICodeComment;
import jadx.api.data.ICodeData;
import jadx.api.data.ICodeRename;

public class JadxCodeData implements ICodeData {
	private List<ICodeComment> comments = Collections.emptyList();
	private List<ICodeRename> renames = Collections.emptyList();

	@Override
	public List<ICodeComment> getComments() {
		return comments;
	}

	public void setComments(List<ICodeComment> comments) {
		this.comments = comments;
	}

	@Override
	public List<ICodeRename> getRenames() {
		return renames;
	}

	public void setRenames(List<ICodeRename> renames) {
		this.renames = renames;
	}

	@Override
	public boolean isEmpty() {
		return comments.isEmpty() && renames.isEmpty();
	}
}
