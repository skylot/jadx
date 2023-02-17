package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodInlineAttr;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.typeinference.TypeInferenceVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.ListUtils;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

@JadxVisitor(
		name = "InlineMethods",
		desc = "Inline methods (previously marked in MarkMethodsForInline)",
		runAfter = TypeInferenceVisitor.class,
		runBefore = ModVisitor.class
)
public class InlineMethods extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(InlineMethods.class);

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn.getType() == InsnType.INVOKE) {
					processInvokeInsn(mth, block, ((InvokeNode) insn));
				}
			}
		}
	}

	private void processInvokeInsn(MethodNode mth, BlockNode block, InvokeNode insn) {
		IMethodDetails callMthDetails = insn.get(AType.METHOD_DETAILS);
		if (!(callMthDetails instanceof MethodNode)) {
			return;
		}
		MethodNode callMth = (MethodNode) callMthDetails;
		try {
			// TODO: sort inner classes process order by dependencies!
			MethodInlineAttr mia = MarkMethodsForInline.process(callMth);
			if (mia == null) {
				// method not yet loaded => will retry at codegen stage
				callMth.getParentClass().reloadAtCodegenStage();
				return;
			}
			if (mia.notNeeded()) {
				return;
			}
			inlineMethod(mth, callMth, mia, block, insn);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to process method for inline: " + callMth, e);
		}
	}

	private void inlineMethod(MethodNode mth, MethodNode callMth, MethodInlineAttr mia, BlockNode block, InvokeNode insn) {
		InsnNode inlCopy = mia.getInsn().copyWithoutResult();
		RegisterArg resultArg = insn.getResult();
		if (resultArg != null) {
			inlCopy.setResult(resultArg.duplicate());
		} else if (isAssignNeeded(mia.getInsn(), insn, callMth)) {
			// add fake result to make correct java expression (see test TestGetterInlineNegative)
			inlCopy.setResult(mth.makeSyntheticRegArg(callMth.getReturnType(), "unused"));
		}
		if (!callMth.getMethodInfo().getArgumentsTypes().isEmpty()) {
			// remap args
			InsnArg[] regs = new InsnArg[callMth.getRegsCount()];
			int[] regNums = mia.getArgsRegNums();
			for (int i = 0; i < regNums.length; i++) {
				InsnArg arg = insn.getArg(i);
				regs[regNums[i]] = arg;
			}
			// replace args
			List<RegisterArg> inlArgs = new ArrayList<>();
			inlCopy.getRegisterArgs(inlArgs);
			for (RegisterArg r : inlArgs) {
				int regNum = r.getRegNum();
				if (regNum >= regs.length) {
					LOG.warn("Unknown register number {} in method call: {} from {}", r, callMth, mth);
				} else {
					InsnArg repl = regs[regNum];
					if (repl == null) {
						LOG.warn("Not passed register {} in method call: {} from {}", r, callMth, mth);
					} else {
						inlCopy.replaceArg(r, repl);
					}
				}
			}
		}
		IMethodDetails methodDetailsAttr = inlCopy.get(AType.METHOD_DETAILS);
		if (!BlockUtils.replaceInsn(mth, block, insn, inlCopy)) {
			mth.addWarnComment("Failed to inline method: " + callMth);
		}
		// replaceInsn replaces the attributes as well, make sure to preserve METHOD_DETAILS
		if (methodDetailsAttr != null) {
			inlCopy.addAttr(methodDetailsAttr);
		}
		updateUsageInfo(mth, callMth, mia.getInsn());
	}

	private boolean isAssignNeeded(InsnNode inlineInsn, InvokeNode parentInsn, MethodNode callMthNode) {
		if (parentInsn.getResult() != null) {
			return false;
		}
		if (parentInsn.contains(AFlag.WRAPPED)) {
			return false;
		}
		if (inlineInsn.getType() == InsnType.IPUT) {
			return false;
		}
		return !callMthNode.isVoidReturn();
	}

	private void updateUsageInfo(MethodNode mth, MethodNode inlinedMth, InsnNode insn) {
		inlinedMth.getUseIn().remove(mth);
		insn.visitInsns(innerInsn -> {
			// TODO: share code with UsageInfoVisitor
			switch (innerInsn.getType()) {
				case INVOKE:
				case CONSTRUCTOR:
					MethodInfo callMth = ((BaseInvokeNode) innerInsn).getCallMth();
					MethodNode callMthNode = mth.root().resolveMethod(callMth);
					if (callMthNode != null) {
						callMthNode.setUseIn(ListUtils.safeReplace(callMthNode.getUseIn(), inlinedMth, mth));
						replaceClsUsage(mth, inlinedMth, callMthNode.getParentClass());
					}
					break;

				case IGET:
				case IPUT:
				case SPUT:
				case SGET:
					FieldInfo fieldInfo = (FieldInfo) ((IndexInsnNode) innerInsn).getIndex();
					FieldNode fieldNode = mth.root().resolveField(fieldInfo);
					if (fieldNode != null) {
						fieldNode.setUseIn(ListUtils.safeReplace(fieldNode.getUseIn(), inlinedMth, mth));
						replaceClsUsage(mth, inlinedMth, fieldNode.getParentClass());
					}
					break;
			}
		});
	}

	private void replaceClsUsage(MethodNode mth, MethodNode inlinedMth, ClassNode parentClass) {
		parentClass.setUseInMth(ListUtils.safeReplace(parentClass.getUseInMth(), inlinedMth, mth));
		parentClass.setUseIn(ListUtils.safeReplace(parentClass.getUseIn(), inlinedMth.getParentClass(), mth.getParentClass()));
	}
}
