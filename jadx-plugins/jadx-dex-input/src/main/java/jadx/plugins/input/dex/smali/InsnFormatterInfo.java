package jadx.plugins.input.dex.smali;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.IMethodData;
import jadx.api.plugins.input.insns.InsnData;

public class InsnFormatterInfo {
	private final SmaliCodeWriter codeWriter;
	@Nullable
	private IMethodData mth;
	@Nullable
	private InsnData insn;

	public InsnFormatterInfo(SmaliCodeWriter codeWriter, IMethodData mth) {
		this.codeWriter = codeWriter;
		this.mth = Objects.requireNonNull(mth);
	}

	public InsnFormatterInfo(SmaliCodeWriter codeWriter, InsnData insn) {
		this.codeWriter = codeWriter;
		this.insn = Objects.requireNonNull(insn);
	}

	public SmaliCodeWriter getCodeWriter() {
		return codeWriter;
	}

	public void setMth(IMethodData mth) {
		this.mth = mth;
	}

	public IMethodData getMth() {
		return mth;
	}

	public InsnData getInsn() {
		if (insn == null) {
			throw new NullPointerException("Instruction not set for formatter");
		}
		return insn;
	}

	public void setInsn(InsnData insn) {
		this.insn = insn;
	}
}
