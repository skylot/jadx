package jadx.core.xmlgen.entry;

public class RawNamedValue {
	private final int nameRef;
	private final RawValue rawValue;

	public RawNamedValue(int nameRef, RawValue rawValue) {
		this.nameRef = nameRef;
		this.rawValue = rawValue;
	}

	public int getNameRef() {
		return nameRef;
	}

	public RawValue getRawValue() {
		return rawValue;
	}
}
