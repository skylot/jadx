package jadx.plugins.input.java.data.attributes.debuginfo;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.ILocalVar;

public class JavaLocalVar implements ILocalVar {
	private int regNum;
	private final String name;
	private final String type;
	@Nullable
	private String sign;

	private final int startOffset;
	private final int endOffset;

	public JavaLocalVar(int regNum, String name, @Nullable String type, @Nullable String sign, int startOffset, int endOffset) {
		this.regNum = regNum;
		this.name = name;
		this.type = type;
		this.sign = sign;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
	}

	public void shiftRegNum(int maxStack) {
		this.regNum += maxStack; // convert local var to register
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getRegNum() {
		return regNum;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public @Nullable String getSignature() {
		return sign;
	}

	public void setSignature(String sign) {
		this.sign = sign;
	}

	@Override
	public int getStartOffset() {
		return startOffset;
	}

	@Override
	public int getEndOffset() {
		return endOffset;
	}

	@Override
	public boolean isMarkedAsParameter() {
		return false;
	}

	@Override
	public int hashCode() {
		int result = regNum;
		result = 31 * result + name.hashCode();
		result = 31 * result + startOffset;
		result = 31 * result + endOffset;
		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof JavaLocalVar)) {
			return false;
		}
		JavaLocalVar other = (JavaLocalVar) o;
		return regNum == other.regNum
				&& startOffset == other.startOffset
				&& endOffset == other.endOffset
				&& name.equals(other.name);
	}

	private static String formatOffset(int offset) {
		return String.format("0x%04x", offset);
	}

	@Override
	public String toString() {
		return formatOffset(startOffset) + '-' + formatOffset(endOffset)
				+ ": r" + regNum + " '" + name + "' " + type
				+ (sign != null ? ", signature: " + sign : "");
	}
}
