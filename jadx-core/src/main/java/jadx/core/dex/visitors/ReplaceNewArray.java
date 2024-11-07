package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.CodeFeaturesAttr;
import jadx.core.dex.attributes.nodes.CodeFeaturesAttr.CodeFeature;
import jadx.core.dex.instructions.FilledNewArrayNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.NewArrayNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IFieldInfoRef;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.utils.InsnList;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.InsnUtils;
import jadx.core.utils.exceptions.JadxException;

@JadxVisitor(
		name = "ReplaceNewArray",
		desc = "Replace new-array and sequence of array-put to new filled-array instruction",
		runAfter = CodeShrinkVisitor.class
)
public class ReplaceNewArray extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (!CodeFeaturesAttr.contains(mth, CodeFeature.NEW_ARRAY)) {
			return;
		}
		InsnRemover remover = new InsnRemover(mth);
		int k = 0;
		while (true) {
			boolean changed = false;
			for (BlockNode block : mth.getBasicBlocks()) {
				List<InsnNode> insnList = block.getInstructions();
				int size = insnList.size();
				for (int i = 0; i < size; i++) {
					changed |= processInsn(mth, insnList, i, remover);
				}
				remover.performForBlock(block);
			}
			if (changed) {
				CodeShrinkVisitor.shrinkMethod(mth);
			} else {
				break;
			}
			if (k++ > 100) {
				mth.addWarnComment("Reached limit for ReplaceNewArray iterations");
				break;
			}
		}
	}

	private static boolean processInsn(MethodNode mth, List<InsnNode> instructions, int i, InsnRemover remover) {
		InsnNode insn = instructions.get(i);
		if (insn.getType() == InsnType.NEW_ARRAY && !insn.contains(AFlag.REMOVE)) {
			return processNewArray(mth, (NewArrayNode) insn, instructions, remover);
		}
		return false;
	}

	private static boolean processNewArray(MethodNode mth,
			NewArrayNode newArrayInsn, List<InsnNode> instructions, InsnRemover remover) {
		Object arrayLenConst = InsnUtils.getConstValueByArg(mth.root(), newArrayInsn.getArg(0));
		if (!(arrayLenConst instanceof LiteralArg)) {
			return false;
		}
		int len = (int) ((LiteralArg) arrayLenConst).getLiteral();
		if (len == 0) {
			return false;
		}
		ArgType arrType = newArrayInsn.getArrayType();
		ArgType elemType = arrType.getArrayElement();
		boolean allowMissingKeys = arrType.getArrayDimension() == 1 && elemType.isPrimitive();
		int minLen = allowMissingKeys ? len / 2 : len;

		RegisterArg arrArg = newArrayInsn.getResult();
		List<RegisterArg> useList = arrArg.getSVar().getUseList();
		if (useList.size() < minLen) {
			return false;
		}
		// quick check if APUT is used
		boolean foundPut = false;
		for (RegisterArg registerArg : useList) {
			InsnNode parentInsn = registerArg.getParentInsn();
			if (parentInsn != null && parentInsn.getType() == InsnType.APUT) {
				foundPut = true;
				break;
			}
		}
		if (!foundPut) {
			return false;
		}
		// collect put instructions sorted by array index
		SortedMap<Long, InsnNode> arrPuts = new TreeMap<>();
		InsnNode firstNotAPutUsage = null;
		for (RegisterArg registerArg : useList) {
			InsnNode parentInsn = registerArg.getParentInsn();
			if (parentInsn == null
					|| parentInsn.getType() != InsnType.APUT
					|| !arrArg.sameRegAndSVar(parentInsn.getArg(0))) {
				if (firstNotAPutUsage == null) {
					firstNotAPutUsage = parentInsn;
				}
				continue;
			}
			Object constVal = InsnUtils.getConstValueByArg(mth.root(), parentInsn.getArg(1));
			if (!(constVal instanceof LiteralArg)) {
				return false;
			}
			long index = ((LiteralArg) constVal).getLiteral();
			if (index >= len) {
				return false;
			}
			if (arrPuts.containsKey(index)) {
				// stop on index rewrite
				break;
			}
			arrPuts.put(index, parentInsn);
		}
		if (arrPuts.size() < minLen) {
			return false;
		}
		if (!verifyPutInsns(arrArg, instructions, arrPuts)) {
			return false;
		}

		// checks complete, apply
		InsnNode filledArr = new FilledNewArrayNode(elemType, len);
		filledArr.setResult(arrArg.duplicate());
		filledArr.copyAttributesFrom(newArrayInsn);
		filledArr.inheritMetadata(newArrayInsn);
		filledArr.setOffset(newArrayInsn.getOffset());

		long prevIndex = -1;
		for (Map.Entry<Long, InsnNode> entry : arrPuts.entrySet()) {
			long index = entry.getKey();
			if (index != prevIndex) {
				// use zero for missing keys
				for (long i = prevIndex + 1; i < index; i++) {
					filledArr.addArg(InsnArg.lit(0, elemType));
				}
			}
			InsnNode put = entry.getValue();
			filledArr.addArg(replaceConstInArg(mth, put.getArg(2)));
			remover.addAndUnbind(put);
			prevIndex = index;
		}
		// add missing trailing zeros
		for (long i = prevIndex + 1; i < len; i++) {
			filledArr.addArg(InsnArg.lit(0, elemType));
		}
		remover.addAndUnbind(newArrayInsn);

		// place new insn at last array put or before first usage
		InsnNode lastPut = arrPuts.get(arrPuts.lastKey());
		int newInsnPos = InsnList.getIndex(instructions, lastPut);
		if (firstNotAPutUsage != null) {
			int idx = InsnList.getIndex(instructions, firstNotAPutUsage);
			if (idx != -1) {
				// TODO: check that all args already assigned
				newInsnPos = Math.min(idx, newInsnPos);
			}
		}
		instructions.add(newInsnPos, filledArr);
		return true;
	}

	private static boolean verifyPutInsns(RegisterArg arrReg, List<InsnNode> insnList, SortedMap<Long, InsnNode> arrPuts) {
		List<InsnNode> puts = new ArrayList<>(arrPuts.values());
		int putsCount = puts.size();
		// expect all puts to be in the same block
		if (insnList.size() < putsCount) {
			return false;
		}
		Set<InsnNode> insnSet = Collections.newSetFromMap(new IdentityHashMap<>());
		insnSet.addAll(insnList);
		if (!insnSet.containsAll(puts)) {
			return false;
		}
		// array arg shouldn't be used in puts insns
		for (InsnNode put : puts) {
			InsnArg putArg = put.getArg(2);
			if (putArg.isUseVar(arrReg)) {
				return false;
			}
		}
		return true;
	}

	private static InsnArg replaceConstInArg(MethodNode mth, InsnArg valueArg) {
		if (valueArg.isLiteral()) {
			IFieldInfoRef f = mth.getParentClass().getConstFieldByLiteralArg((LiteralArg) valueArg);
			if (f != null) {
				InsnNode fGet = new IndexInsnNode(InsnType.SGET, f.getFieldInfo(), 0);
				InsnArg arg = InsnArg.wrapArg(fGet);
				ModVisitor.addFieldUsage(f, mth);
				return arg;
			}
		}
		return valueArg.duplicate();
	}
}
