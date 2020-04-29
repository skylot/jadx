package jadx.plugins.input.dex.sections.debuginfo;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.ILocalVar;
import jadx.plugins.input.dex.sections.SectionReader;
import jadx.plugins.input.dex.utils.Utils;

public class LocalVar implements ILocalVar {
	private final int regNum;
	private final String name;
	private final String type;
	@Nullable
	private final String sign;

	private boolean isEnd;
	private int startOffset;
	private int endOffset;

	public LocalVar(SectionReader dex, int regNum, int nameId, int typeId, int signId) {
		this(regNum, dex.getString(nameId), dex.getType(typeId), dex.getString(signId));
	}

	public LocalVar(int regNum, String name, String type) {
		this(regNum, name, type, null);
	}

	public LocalVar(int regNum, String name, String type, @Nullable String sign) {
		this.regNum = regNum;
		this.name = name;
		this.type = type;
		this.sign = sign;
	}

	public void start(int addr) {
		this.isEnd = false;
		this.startOffset = addr;
	}

	/**
	 * Sets end address of local variable
	 *
	 * @param addr address
	 * @return <b>true</b> if local variable was active, else <b>false</b>
	 */
	public boolean end(int addr) {
		if (isEnd) {
			return false;
		}
		this.isEnd = true;
		this.endOffset = addr;
		return true;
	}

	@Override
	public int getRegNum() {
		return regNum;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getType() {
		return type;
	}

	@Nullable
	@Override
	public String getSignature() {
		return sign;
	}

	@Override
	public int getStartOffset() {
		return startOffset;
	}

	@Override
	public int getEndOffset() {
		return endOffset;
	}

	public boolean isEnd() {
		return isEnd;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public String toString() {
		return Utils.formatOffset(startOffset)
				+ '-' + (isEnd ? Utils.formatOffset(endOffset) : "      ")
				+ ": r" + regNum + " '" + name + "' " + type
				+ (sign != null ? ", signature: " + sign : "");
	}
}
