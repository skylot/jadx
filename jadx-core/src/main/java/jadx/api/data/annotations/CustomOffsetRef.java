package jadx.api.data.annotations;

import jadx.api.data.ICodeComment;

public class CustomOffsetRef implements ICodeRawOffset {
	private final int offset;
	private final ICodeComment.AttachType attachType;

	public CustomOffsetRef(int offset, ICodeComment.AttachType attachType) {
		this.offset = offset;
		this.attachType = attachType;
	}

	@Override
	public int getOffset() {
		return offset;
	}

	public ICodeComment.AttachType getAttachType() {
		return attachType;
	}

	@Override
	public String toString() {
		return "CustomOffsetRef{offset=" + offset + ", attachType=" + attachType + '}';
	}
}
