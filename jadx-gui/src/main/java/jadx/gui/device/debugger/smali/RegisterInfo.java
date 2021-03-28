package jadx.gui.device.debugger.smali;

import jadx.api.plugins.input.data.ILocalVar;

public abstract class RegisterInfo implements ILocalVar {

	public boolean isInitialized(long codeOffset) {
		return codeOffset >= getStartOffset() && codeOffset < getEndOffset();
	}

	public boolean isUnInitialized(long codeOffset) {
		return codeOffset < getStartOffset() || codeOffset >= getEndOffset();
	}
}
