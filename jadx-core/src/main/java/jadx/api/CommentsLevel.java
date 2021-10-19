package jadx.api;

public enum CommentsLevel {
	NONE,
	USER_ONLY,
	ERROR,
	WARN,
	INFO,
	DEBUG;

	public boolean filter(CommentsLevel limit) {
		return this.ordinal() <= limit.ordinal();
	}
}
