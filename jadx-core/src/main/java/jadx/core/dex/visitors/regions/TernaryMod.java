package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.IfRegion;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.TernaryRegion;
import jadx.core.dex.visitors.CodeShrinker;
import jadx.core.utils.InsnList;

import java.util.List;

public class TernaryMod {

	private TernaryMod() {
	}

	static void makeTernaryInsn(MethodNode mth, IfRegion ifRegion) {
		if (ifRegion.getAttributes().contains(AttributeFlag.ELSE_IF_CHAIN)) {
			return;
		}
		IContainer thenRegion = ifRegion.getThenRegion();
		IContainer elseRegion = ifRegion.getElseRegion();
		if (thenRegion == null || elseRegion == null) {
			return;
		}
		BlockNode tb = getTernaryInsnBlock(thenRegion);
		BlockNode eb = getTernaryInsnBlock(elseRegion);
		if (tb == null || eb == null) {
			return;
		}
		BlockNode header = ifRegion.getHeader();
		InsnNode t = tb.getInstructions().get(0);
		InsnNode e = eb.getInstructions().get(0);

		if (t.getResult() != null && e.getResult() != null
				&& t.getResult().getTypedVar() == e.getResult().getTypedVar()) {
			InsnList.remove(tb, t);
			InsnList.remove(eb, e);

			TernaryInsn ternInsn = new TernaryInsn(ifRegion.getCondition(),
					t.getResult(), InsnArg.wrapArg(t), InsnArg.wrapArg(e));
			TernaryRegion tern = new TernaryRegion(ifRegion, header);
			// TODO: add api for replace regions
			ifRegion.setTernRegion(tern);

			// remove 'if' instruction
			header.getInstructions().clear();
			header.getInstructions().add(ternInsn);

			// unbind result args
			List<InsnArg> useList = ternInsn.getResult().getTypedVar().getUseList();
			useList.remove(t.getResult());
			useList.remove(e.getResult());
			useList.add(ternInsn.getResult());

			// shrink method again
			CodeShrinker.shrinkMethod(mth);
			return;
		}

		if (!mth.getReturnType().equals(ArgType.VOID)
				&& t.getType() == InsnType.RETURN && e.getType() == InsnType.RETURN) {
			InsnList.remove(tb, t);
			InsnList.remove(eb, e);
			tb.getAttributes().remove(AttributeFlag.RETURN);
			eb.getAttributes().remove(AttributeFlag.RETURN);

			TernaryInsn ternInsn = new TernaryInsn(ifRegion.getCondition(), null, t.getArg(0), e.getArg(0));
			InsnNode retInsn = new InsnNode(InsnType.RETURN, 1);
			retInsn.addArg(InsnArg.wrapArg(ternInsn));

			header.getInstructions().clear();
			header.getInstructions().add(retInsn);
			header.getAttributes().add(AttributeFlag.RETURN);

			ifRegion.setTernRegion(new TernaryRegion(ifRegion, header));

			CodeShrinker.shrinkMethod(mth);
		}
	}

	private static BlockNode getTernaryInsnBlock(IContainer thenRegion) {
		if (thenRegion instanceof Region) {
			Region r = (Region) thenRegion;
			if (r.getSubBlocks().size() == 1) {
				IContainer container = r.getSubBlocks().get(0);
				if (container instanceof BlockNode) {
					BlockNode block = (BlockNode) container;
					if (block.getInstructions().size() == 1) {
						return block;
					}
				}
			}
		}
		return null;
	}
}
