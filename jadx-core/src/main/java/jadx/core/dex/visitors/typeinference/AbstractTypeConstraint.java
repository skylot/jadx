package jadx.core.dex.visitors.typeinference;

import java.util.ArrayList;
import java.util.List;

import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.utils.Utils;

public abstract class AbstractTypeConstraint implements ITypeConstraint {

	protected final InsnNode insn;
	protected final List<SSAVar> relatedVars;

	public AbstractTypeConstraint(InsnNode insn, InsnArg arg) {
		this.insn = insn;
		this.relatedVars = collectRelatedVars(insn, arg);
	}

	private List<SSAVar> collectRelatedVars(InsnNode insn, InsnArg arg) {
		List<SSAVar> list = new ArrayList<>(insn.getArgsCount());
		if (insn.getResult() == arg) {
			for (InsnArg insnArg : insn.getArguments()) {
				if (insnArg.isRegister()) {
					list.add(((RegisterArg) insnArg).getSVar());
				}
			}
		} else {
			list.add(insn.getResult().getSVar());
			for (InsnArg insnArg : insn.getArguments()) {
				if (insnArg != arg && insnArg.isRegister()) {
					list.add(((RegisterArg) insnArg).getSVar());
				}
			}
		}
		return list;
	}

	@Override
	public List<SSAVar> getRelatedVars() {
		return relatedVars;
	}

	@Override
	public String toString() {
		return "(" + insn.getType() + ':' + Utils.listToString(relatedVars, SSAVar::toShortString) + ')';
	}
}
