package jadx.core.xmlgen.entry;

public final class RawValue {
	private final int dataType;
	private final int data;

	public RawValue(int dataType, int data) {
		this.dataType = dataType;
		this.data = data;
	}

	public int getDataType() {
		return dataType;
	}

	public int getData() {
		return data;
	}

	@Override
	public String toString() {
		return "RawValue: type=0x" + Integer.toHexString(dataType) + ", value=" + data;
	}
}
