package jadx.core.dex.attributes.nodes;

import java.util.List;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;

public class MethodInlineAttr implements IAttribute {

	public static void markForInline(MethodNode mth, InsnNode replaceInsn) {
		List<RegisterArg> allArgRegs = mth.getAllArgRegs();
		int argsCount = allArgRegs.size();
		int[] regNums = new int[argsCount];
		for (int i = 0; i < argsCount; i++) {
			RegisterArg reg = allArgRegs.get(i);
			regNums[i] = reg.getRegNum();
		}
		mth.addAttr(new MethodInlineAttr(replaceInsn, regNums));
	}

	private final InsnNode insn;

	/**
	 * Store method arguments register numbers to allow remap registers
	 */
	private final int[] argsRegNums;

	private MethodInlineAttr(InsnNode insn, int[] argsRegNums) {
		this.insn = insn;
		this.argsRegNums = argsRegNums;
	}

	public InsnNode getInsn() {
		return insn;
	}

	public int[] getArgsRegNums() {
		return argsRegNums;
	}

	@Override
	public AType<MethodInlineAttr> getType() {
		return AType.METHOD_INLINE;
	}

	@Override
	public String toString() {
		return "INLINE: " + insn;
	}
}
