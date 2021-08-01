package jadx.plugins.input.java.data.code.trycatch;

import jadx.api.plugins.input.data.ICatch;
import jadx.api.plugins.input.data.ITry;

public class JavaTryData implements ITry {

	private final int startAddr;
	private final int endAddr;
	private ICatch catchHandler;

	public JavaTryData(int startAddr, int endAddr) {
		this.startAddr = startAddr;
		this.endAddr = endAddr;
	}

	@Override
	public ICatch getCatch() {
		return catchHandler;
	}

	public void setCatch(ICatch catchHandler) {
		this.catchHandler = catchHandler;
	}

	@Override
	public int getStartAddress() {
		return startAddr;
	}

	@Override
	public int getEndAddress() {
		return endAddr;
	}

	@Override
	public int hashCode() {
		return startAddr + 31 * endAddr;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof JavaTryData)) {
			return false;
		}
		JavaTryData that = (JavaTryData) o;
		return startAddr == that.startAddr && endAddr == that.endAddr;
	}

	@Override
	public String toString() {
		return "Try{" + startAddr + " - " + endAddr + ": " + catchHandler + '}';
	}
}
