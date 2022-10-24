package jadx.gui.cache.usage;

final class MthRef {
	private final String cls;
	private final String shortId;

	MthRef(String cls, String shortId) {
		this.cls = cls;
		this.shortId = shortId;
	}

	public String getCls() {
		return cls;
	}

	public String getShortId() {
		return shortId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof MthRef)) {
			return false;
		}
		MthRef other = (MthRef) o;
		return cls.equals(other.cls)
				&& shortId.equals(other.shortId);
	}

	@Override
	public int hashCode() {
		return 31 * cls.hashCode() + shortId.hashCode();
	}
}
