package jadx.core.dex.visitors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.nodes.GenericInfoAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.dex.visitors.typeinference.TypeInferenceVisitor;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "GenericTypesVisitor",
		desc = "Fix and apply generic type info",
		runAfter = TypeInferenceVisitor.class,
		runBefore = { CodeShrinkVisitor.class, MethodInvokeVisitor.class }
)
public class GenericTypesVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(GenericTypesVisitor.class);

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn.getType() == InsnType.CONSTRUCTOR) {
					attachGenericTypesInfo(mth, (ConstructorInsn) insn);
				}
			}
		}
	}

	private void attachGenericTypesInfo(MethodNode mth, ConstructorInsn insn) {
		try {
			RegisterArg resultArg = insn.getResult();
			if (resultArg == null) {
				return;
			}
			ArgType argType = resultArg.getSVar().getCodeVar().getType();
			if (argType == null || argType.getGenericTypes() == null) {
				return;
			}
			ClassNode cls = mth.root().resolveClass(insn.getClassType());
			if (cls != null && cls.getGenericTypeParameters().isEmpty()) {
				return;
			}
			insn.addAttr(new GenericInfoAttr(argType.getGenericTypes()));
		} catch (Exception e) {
			LOG.error("Failed to attach constructor generic info", e);
		}
	}
}
