package jadx.gui.device.debugger.smali;

public class SmaliRegister extends RegisterInfo {
	private final int num;
	private String paramName;
	private final int endOffset;
	private int startOffset;
	private boolean isParam;
	private int runtimeNum;

	public SmaliRegister(int num, int insnCount) {
		this.num = num;
		this.endOffset = insnCount;
		this.startOffset = insnCount;
	}

	public int getRuntimeRegNum() {
		return runtimeNum;
	}

	public void setRuntimeRegNum(int runtimeNum) {
		this.runtimeNum = runtimeNum;
	}

	@Override
	public boolean isInitialized(long codeOffset) {
		return codeOffset > getStartOffset() && codeOffset < getEndOffset();
	}

	protected void setParam(String name) {
		paramName = name;
		isParam = true;
	}

	protected void setStartOffset(int off) {
		if (off < startOffset) {
			startOffset = off;
		}
	}

	@Override
	public String getName() {
		return paramName != null ? paramName : "v" + num;
	}

	@Override
	public int getRegNum() {
		return num;
	}

	@Override
	public String getType() {
		return "";
	}

	@Override
	public String getSignature() {
		return null;
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
		return isParam;
	}
}
