package jadx.api.metadata.annotations;

import org.jetbrains.annotations.Nullable;

import jadx.api.ICodeWriter;
import jadx.api.metadata.ICodeAnnotation;
import jadx.core.dex.nodes.InsnNode;

public class InsnCodeOffset implements ICodeAnnotation {

	public static void attach(ICodeWriter code, InsnNode insn) {
		if (insn == null) {
			return;
		}
		if (code.isMetadataSupported()) {
			InsnCodeOffset ann = from(insn);
			if (ann != null) {
				code.attachLineAnnotation(ann);
			}
		}
	}

	public static void attach(ICodeWriter code, int offset) {
		if (offset >= 0 && code.isMetadataSupported()) {
			code.attachLineAnnotation(new InsnCodeOffset(offset));
		}
	}

	@Nullable
	public static InsnCodeOffset from(InsnNode insn) {
		int offset = insn.getOffset();
		if (offset < 0) {
			return null;
		}
		return new InsnCodeOffset(offset);
	}

	private final int offset;

	public InsnCodeOffset(int offset) {
		this.offset = offset;
	}

	public int getOffset() {
		return offset;
	}

	@Override
	public AnnType getAnnType() {
		return AnnType.OFFSET;
	}

	@Override
	public String toString() {
		return "offset=" + offset;
	}
}
