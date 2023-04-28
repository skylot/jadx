package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.codegen.TypeGen;
import jadx.core.deobf.NameMapper;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.ArithOp;
import jadx.core.dex.instructions.ConstStringNode;
import jadx.core.dex.instructions.FilledNewArrayNode;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.InvokeType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.InsnWrapArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.mods.ConstructorInsn;
import jadx.core.dex.instructions.mods.TernaryInsn;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.regions.conditions.IfCondition;
import jadx.core.dex.visitors.shrink.CodeShrinkVisitor;
import jadx.core.dex.visitors.typeinference.TypeCompareEnum;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnList;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class SimplifyVisitor extends AbstractVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(SimplifyVisitor.class);

	private MethodInfo stringGetBytesMth;

	@Override
	public void init(RootNode root) {
		stringGetBytesMth = MethodInfo.fromDetails(
				root,
				ClassInfo.fromType(root, ArgType.STRING),
				"getBytes",
				Collections.emptyList(),
				ArgType.array(ArgType.BYTE));
	}

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		boolean changed = false;
		for (BlockNode block : mth.getBasicBlocks()) {
			if (simplifyBlock(mth, block)) {
				changed = true;
			}
		}
		if (changed || mth.contains(AFlag.REQUEST_CODE_SHRINK)) {
			CodeShrinkVisitor.shrinkMethod(mth);
		}
	}

	private boolean simplifyBlock(MethodNode mth, BlockNode block) {
		boolean changed = false;
		List<InsnNode> list = block.getInstructions();
		for (int i = 0; i < list.size(); i++) {
			InsnNode insn = list.get(i);
			int insnCount = list.size();
			InsnNode modInsn = simplifyInsn(mth, insn, null);
			if (modInsn != null) {
				modInsn.rebindArgs();
				if (i < list.size() && list.get(i) == insn) {
					list.set(i, modInsn);
				} else {
					int idx = InsnList.getIndex(list, insn);
					if (idx == -1) {
						throw new JadxRuntimeException("Failed to replace insn");
					}
					list.set(idx, modInsn);
				}
				if (list.size() < insnCount) {
					// some insns removed => restart block processing
					simplifyBlock(mth, block);
					return true;
				}
				changed = true;
			}
		}
		return changed;
	}

	private void simplifyArgs(MethodNode mth, InsnNode insn) {
		boolean changed = false;
		for (InsnArg arg : insn.getArguments()) {
			if (arg.isInsnWrap()) {
				InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
				InsnNode replaceInsn = simplifyInsn(mth, wrapInsn, insn);
				if (replaceInsn != null) {
					arg.wrapInstruction(mth, replaceInsn, false);
					InsnRemover.unbindInsn(mth, wrapInsn);
					changed = true;
				}
			}
		}
		if (changed) {
			insn.rebindArgs();
			mth.add(AFlag.REQUEST_CODE_SHRINK);
		}
	}

	private InsnNode simplifyInsn(MethodNode mth, InsnNode insn, @Nullable InsnNode parentInsn) {
		if (insn.contains(AFlag.DONT_GENERATE)) {
			return null;
		}
		simplifyArgs(mth, insn);
		switch (insn.getType()) {
			case ARITH:
				return simplifyArith((ArithNode) insn);

			case IF:
				simplifyIf(mth, (IfNode) insn);
				break;
			case TERNARY:
				simplifyTernary(mth, (TernaryInsn) insn);
				break;

			case INVOKE:
				return convertInvoke(mth, (InvokeNode) insn);

			case IPUT:
			case SPUT:
				return convertFieldArith(mth, insn);

			case CAST:
			case CHECK_CAST:
				return processCast(mth, (IndexInsnNode) insn, parentInsn);

			case MOVE:
				InsnArg firstArg = insn.getArg(0);
				if (firstArg.isLiteral()) {
					InsnNode constInsn = new InsnNode(InsnType.CONST, 1);
					constInsn.setResult(insn.getResult());
					constInsn.addArg(firstArg);
					constInsn.copyAttributesFrom(insn);
					return constInsn;
				}
				break;

			case CONSTRUCTOR:
				return simplifyStringConstructor(mth, (ConstructorInsn) insn);

			default:
				break;
		}
		return null;
	}

	private InsnNode simplifyStringConstructor(MethodNode mth, ConstructorInsn insn) {
		if (insn.getCallMth().getDeclClass().getType().equals(ArgType.STRING)
				&& insn.getArgsCount() != 0
				&& insn.getArg(0).isInsnWrap()) {
			InsnNode arrInsn = ((InsnWrapArg) insn.getArg(0)).getWrapInsn();
			if (arrInsn.getType() == InsnType.FILLED_NEW_ARRAY
					&& arrInsn.getArgsCount() != 0) {
				ArgType elemType = ((FilledNewArrayNode) arrInsn).getElemType();
				if (elemType == ArgType.BYTE || elemType == ArgType.CHAR) {
					int printable = 0;
					byte[] arr = new byte[arrInsn.getArgsCount()];
					for (int i = 0; i < arr.length; i++) {
						InsnArg arrArg = arrInsn.getArg(i);
						if (!arrArg.isLiteral()) {
							return null;
						}
						arr[i] = (byte) ((LiteralArg) arrArg).getLiteral();
						if (NameMapper.isPrintableChar((char) arr[i])) {
							printable++;
						}
					}
					if (printable >= arr.length - printable) {
						InsnNode constStr = new ConstStringNode(new String(arr));
						if (insn.getArgsCount() == 1) {
							constStr.setResult(insn.getResult());
							constStr.copyAttributesFrom(insn);
							InsnRemover.unbindArgUsage(mth, insn.getArg(0));
							return constStr;
						} else {
							InvokeNode in = new InvokeNode(stringGetBytesMth, InvokeType.VIRTUAL, 1);
							in.addArg(InsnArg.wrapArg(constStr));
							InsnArg bytesArg = InsnArg.wrapArg(in);
							bytesArg.setType(stringGetBytesMth.getReturnType());
							insn.setArg(0, bytesArg);
							return null;
						}
					}
				}
			}
		}
		return null;
	}

	private static InsnNode processCast(MethodNode mth, IndexInsnNode castInsn, @Nullable InsnNode parentInsn) {
		if (castInsn.contains(AFlag.EXPLICIT_CAST)) {
			return null;
		}
		InsnArg castArg = castInsn.getArg(0);
		ArgType argType = castArg.getType();

		// Don't removes CHECK_CAST for wrapped INVOKE if invoked method returns different type
		if (castArg.isInsnWrap()) {
			InsnNode wrapInsn = ((InsnWrapArg) castArg).getWrapInsn();
			if (wrapInsn.getType() == InsnType.INVOKE) {
				argType = ((InvokeNode) wrapInsn).getCallMth().getReturnType();
			}
		}

		ArgType castToType = (ArgType) castInsn.getIndex();
		if (!ArgType.isCastNeeded(mth.root(), argType, castToType)
				|| isCastDuplicate(castInsn)
				|| shadowedByOuterCast(mth.root(), castToType, parentInsn)) {
			InsnNode insnNode = new InsnNode(InsnType.MOVE, 1);
			insnNode.setOffset(castInsn.getOffset());
			insnNode.setResult(castInsn.getResult());
			insnNode.addArg(castArg);
			return insnNode;
		}
		return null;
	}

	private static boolean isCastDuplicate(IndexInsnNode castInsn) {
		InsnArg arg = castInsn.getArg(0);
		if (arg.isRegister()) {
			SSAVar sVar = ((RegisterArg) arg).getSVar();
			if (sVar != null && sVar.getUseCount() == 1 && !sVar.isUsedInPhi()) {
				InsnNode assignInsn = sVar.getAssign().getParentInsn();
				if (assignInsn != null && assignInsn.getType() == InsnType.CHECK_CAST) {
					ArgType assignCastType = (ArgType) ((IndexInsnNode) assignInsn).getIndex();
					return assignCastType.equals(castInsn.getIndex());
				}
			}
		}
		return false;
	}

	private static boolean shadowedByOuterCast(RootNode root, ArgType castType, @Nullable InsnNode parentInsn) {
		if (parentInsn != null && parentInsn.getType() == InsnType.CAST) {
			ArgType parentCastType = (ArgType) ((IndexInsnNode) parentInsn).getIndex();
			TypeCompareEnum result = root.getTypeCompare().compareTypes(parentCastType, castType);
			return result.isNarrow();
		}
		return false;
	}

	/**
	 * Simplify 'cmp' instruction in if condition
	 */
	private static void simplifyIf(MethodNode mth, IfNode insn) {
		InsnArg f = insn.getArg(0);
		if (f.isInsnWrap()) {
			InsnNode wi = ((InsnWrapArg) f).getWrapInsn();
			if (wi.getType() == InsnType.CMP_L || wi.getType() == InsnType.CMP_G) {
				if (insn.getArg(1).isZeroLiteral()) {
					insn.changeCondition(insn.getOp(), wi.getArg(0).duplicate(), wi.getArg(1).duplicate());
					InsnRemover.unbindInsn(mth, wi);
				} else {
					LOG.warn("TODO: cmp {}", insn);
				}
			}
		}
	}

	/**
	 * Simplify condition in ternary operation
	 */
	private static void simplifyTernary(MethodNode mth, TernaryInsn insn) {
		IfCondition condition = insn.getCondition();
		if (condition.isCompare()) {
			simplifyIf(mth, condition.getCompare().getInsn());
		} else {
			insn.simplifyCondition();
		}
	}

	/**
	 * Simplify chains of calls to StringBuilder#append() plus constructor of StringBuilder.
	 * Those chains are usually automatically generated by the Java compiler when you create String
	 * concatenations like <code>"text " + 1 + " text"</code>.
	 */
	private static InsnNode convertInvoke(MethodNode mth, InvokeNode insn) {
		MethodInfo callMth = insn.getCallMth();

		if (callMth.getDeclClass().getFullName().equals(Consts.CLASS_STRING_BUILDER)
				&& callMth.getShortId().equals(Consts.MTH_TOSTRING_SIGNATURE)) {
			InsnArg instanceArg = insn.getArg(0);
			if (instanceArg.isInsnWrap()) {
				// Convert 'new StringBuilder(xxx).append(yyy).append(zzz).toString() to STRING_CONCAT insn
				List<InsnNode> callChain = flattenInsnChainUntil(insn, InsnType.CONSTRUCTOR);
				return convertStringBuilderChain(mth, insn, callChain);
			}
			if (instanceArg.isRegister()) {
				// Convert 'StringBuilder sb = new StringBuilder(xxx); sb.append(yyy); String str = sb.toString();'
				List<InsnNode> useChain = collectUseChain(mth, insn, (RegisterArg) instanceArg);
				return convertStringBuilderChain(mth, insn, useChain);
			}
		}
		return null;
	}

	private static List<InsnNode> collectUseChain(MethodNode mth, InvokeNode insn, RegisterArg instanceArg) {
		SSAVar sVar = instanceArg.getSVar();
		if (sVar.isUsedInPhi() || sVar.getUseCount() == 0) {
			return Collections.emptyList();
		}
		List<InsnNode> useChain = new ArrayList<>(sVar.getUseCount() + 1);
		InsnNode assignInsn = sVar.getAssign().getParentInsn();
		if (assignInsn == null) {
			return Collections.emptyList();
		}
		useChain.add(assignInsn);
		for (RegisterArg reg : sVar.getUseList()) {
			InsnNode parentInsn = reg.getParentInsn();
			if (parentInsn == null) {
				return Collections.emptyList();
			}
			useChain.add(parentInsn);
		}
		int toStrIdx = InsnList.getIndex(useChain, insn);
		if (useChain.size() - 1 != toStrIdx) {
			return Collections.emptyList();
		}
		useChain.remove(toStrIdx);

		// all insns must be in one block and sequential
		BlockNode assignBlock = BlockUtils.getBlockByInsn(mth, assignInsn);
		if (assignBlock == null) {
			return Collections.emptyList();
		}
		List<InsnNode> blockInsns = assignBlock.getInstructions();
		int assignIdx = InsnList.getIndex(blockInsns, assignInsn);
		int chainSize = useChain.size();
		int lastInsn = blockInsns.size() - assignIdx;
		if (lastInsn < chainSize) {
			return Collections.emptyList();
		}
		for (int i = 1; i < chainSize; i++) {
			if (blockInsns.get(assignIdx + i) != useChain.get(i)) {
				return Collections.emptyList();
			}
		}
		return useChain;
	}

	private static InsnNode convertStringBuilderChain(MethodNode mth, InvokeNode toStrInsn, List<InsnNode> chain) {
		try {
			int chainSize = chain.size();
			if (chainSize < 2) {
				return null;
			}
			List<InsnArg> args = new ArrayList<>(chainSize);
			InsnNode firstInsn = chain.get(0);
			if (firstInsn.getType() != InsnType.CONSTRUCTOR) {
				return null;
			}
			ConstructorInsn constrInsn = (ConstructorInsn) firstInsn;
			if (constrInsn.getArgsCount() == 1) {
				ArgType argType = constrInsn.getCallMth().getArgumentsTypes().get(0);
				if (!argType.isObject()) {
					return null;
				}
				args.add(constrInsn.getArg(0));
			}
			for (int i = 1; i < chainSize; i++) {
				InsnNode chainInsn = chain.get(i);
				InsnArg arg = getArgFromAppend(chainInsn);
				if (arg == null) {
					return null;
				}
				args.add(arg);
			}

			boolean stringArgFound = false;
			for (InsnArg arg : args) {
				if (arg.getType().equals(ArgType.STRING)) {
					stringArgFound = true;
					break;
				}
			}
			if (!stringArgFound) {
				mth.addDebugComment("TODO: convert one arg to string using `String.valueOf()`, args: " + args);
				return null;
			}

			// all check passed
			List<InsnArg> dupArgs = Utils.collectionMap(args, InsnArg::duplicate);
			List<InsnArg> simplifiedArgs = concatConstArgs(dupArgs);
			InsnNode concatInsn = new InsnNode(InsnType.STR_CONCAT, simplifiedArgs);
			concatInsn.add(AFlag.SYNTHETIC);
			if (toStrInsn.getResult() == null && !toStrInsn.contains(AFlag.WRAPPED)) {
				// string concat without assign to variable will cause compilation error
				concatInsn.setResult(mth.makeSyntheticRegArg(ArgType.STRING));
			} else {
				concatInsn.setResult(toStrInsn.getResult());
			}
			concatInsn.copyAttributesFrom(toStrInsn);
			removeStringBuilderInsns(mth, toStrInsn, chain);
			return concatInsn;
		} catch (Exception e) {
			mth.addWarnComment("String concatenation convert failed", e);
		}
		return null;
	}

	private static boolean isConstConcatNeeded(List<InsnArg> args) {
		boolean prevConst = false;
		for (InsnArg arg : args) {
			boolean curConst = arg.isConst();
			if (curConst && prevConst) {
				// found 2 consecutive constants
				return true;
			}
			prevConst = curConst;
		}
		return false;
	}

	private static List<InsnArg> concatConstArgs(List<InsnArg> args) {
		if (!isConstConcatNeeded(args)) {
			return args;
		}
		int size = args.size();
		List<InsnArg> newArgs = new ArrayList<>(size);
		List<String> concatList = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			InsnArg arg = args.get(i);
			String constStr = getConstString(arg);
			if (constStr != null) {
				concatList.add(constStr);
			} else {
				if (!concatList.isEmpty()) {
					newArgs.add(getConcatArg(concatList, args, i));
					concatList.clear();
				}
				newArgs.add(arg);
			}
		}
		if (!concatList.isEmpty()) {
			newArgs.add(getConcatArg(concatList, args, size));
		}
		return newArgs;
	}

	private static InsnArg getConcatArg(List<String> concatList, List<InsnArg> args, int idx) {
		if (concatList.size() == 1) {
			return args.get(idx - 1);
		}
		String str = Utils.concatStrings(concatList);
		return InsnArg.wrapArg(new ConstStringNode(str));
	}

	@Nullable
	private static String getConstString(InsnArg arg) {
		if (arg.isLiteral()) {
			return TypeGen.literalToRawString((LiteralArg) arg);
		}
		if (arg.isInsnWrap()) {
			InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
			if (wrapInsn instanceof ConstStringNode) {
				return ((ConstStringNode) wrapInsn).getString();
			}
		}
		return null;
	}

	/**
	 * Remove and unbind all instructions with StringBuilder
	 */
	private static void removeStringBuilderInsns(MethodNode mth, InvokeNode toStrInsn, List<InsnNode> chain) {
		InsnRemover.unbindAllArgs(mth, toStrInsn);
		for (InsnNode insnNode : chain) {
			InsnRemover.unbindAllArgs(mth, insnNode);
		}
		InsnRemover insnRemover = new InsnRemover(mth);
		for (InsnNode insnNode : chain) {
			if (insnNode != toStrInsn) {
				insnRemover.addAndUnbind(insnNode);
			}
		}
		insnRemover.perform();
	}

	private static List<InsnNode> flattenInsnChainUntil(InsnNode insn, InsnType insnType) {
		List<InsnNode> chain = new ArrayList<>();
		InsnArg arg = insn.getArg(0);
		while (arg.isInsnWrap()) {
			InsnNode wrapInsn = ((InsnWrapArg) arg).getWrapInsn();
			chain.add(wrapInsn);
			if (wrapInsn.getType() == insnType
					|| wrapInsn.getArgsCount() == 0) {
				break;
			}
			arg = wrapInsn.getArg(0);
		}
		Collections.reverse(chain);
		return chain;
	}

	private static InsnArg getArgFromAppend(InsnNode chainInsn) {
		if (chainInsn.getType() == InsnType.INVOKE && chainInsn.getArgsCount() == 2) {
			MethodInfo callMth = ((InvokeNode) chainInsn).getCallMth();
			if (callMth.getDeclClass().getFullName().equals(Consts.CLASS_STRING_BUILDER)
					&& callMth.getName().equals("append")) {
				return chainInsn.getArg(1);
			}
		}
		return null;
	}

	private static InsnNode simplifyArith(ArithNode arith) {
		if (arith.getArgsCount() != 2) {
			return null;
		}
		LiteralArg litArg = null;
		InsnArg secondArg = arith.getArg(1);
		if (secondArg.isInsnWrap()) {
			InsnNode wr = ((InsnWrapArg) secondArg).getWrapInsn();
			if (wr.getType() == InsnType.CONST) {
				InsnArg arg = wr.getArg(0);
				if (arg.isLiteral()) {
					litArg = (LiteralArg) arg;
				}
			}
		} else if (secondArg.isLiteral()) {
			litArg = (LiteralArg) secondArg;
		}
		if (litArg == null) {
			return null;
		}
		switch (arith.getOp()) {
			case ADD:
				// fix 'c + (-1)' to 'c - (1)'
				if (litArg.isNegative()) {
					LiteralArg negLitArg = litArg.negate();
					if (negLitArg != null) {
						return new ArithNode(ArithOp.SUB, arith.getResult(), arith.getArg(0), negLitArg);
					}
				}
				break;

			case XOR:
				// simplify xor on boolean
				InsnArg firstArg = arith.getArg(0);
				long lit = litArg.getLiteral();
				if (firstArg.getType() == ArgType.BOOLEAN && (lit == 0 || lit == 1)) {
					InsnNode node = new InsnNode(lit == 0 ? InsnType.MOVE : InsnType.NOT, 1);
					node.setResult(arith.getResult());
					node.addArg(firstArg);
					return node;
				}
				break;
		}
		return null;
	}

	/**
	 * Convert field arith operation to arith instruction
	 * (IPUT (ARITH (IGET, lit)) -> ARITH ((IGET)) <op>= lit))
	 */
	private static ArithNode convertFieldArith(MethodNode mth, InsnNode insn) {
		InsnArg arg = insn.getArg(0);
		if (!arg.isInsnWrap()) {
			return null;
		}
		InsnNode wrap = ((InsnWrapArg) arg).getWrapInsn();
		InsnType wrapType = wrap.getType();
		if (wrapType != InsnType.ARITH && wrapType != InsnType.STR_CONCAT
				|| !wrap.getArg(0).isInsnWrap()) {
			return null;
		}
		InsnArg getWrap = wrap.getArg(0);
		InsnNode get = ((InsnWrapArg) getWrap).getWrapInsn();
		InsnType getType = get.getType();
		if (getType != InsnType.IGET && getType != InsnType.SGET) {
			return null;
		}
		FieldInfo field = (FieldInfo) ((IndexInsnNode) insn).getIndex();
		FieldInfo innerField = (FieldInfo) ((IndexInsnNode) get).getIndex();
		if (!field.equals(innerField)) {
			return null;
		}
		try {
			if (getType == InsnType.IGET && insn.getType() == InsnType.IPUT) {
				InsnArg reg = get.getArg(0);
				InsnArg putReg = insn.getArg(1);
				if (!reg.equals(putReg)) {
					return null;
				}
			}
			InsnArg fArg = getWrap.duplicate();
			InsnRemover.unbindInsn(mth, get);
			if (insn.getType() == InsnType.IPUT) {
				InsnRemover.unbindArgUsage(mth, insn.getArg(1));
			}
			if (wrapType == InsnType.ARITH) {
				ArithNode ar = (ArithNode) wrap;
				return ArithNode.oneArgOp(ar.getOp(), fArg, ar.getArg(1));
			}
			int argsCount = wrap.getArgsCount();
			InsnNode concat = new InsnNode(InsnType.STR_CONCAT, argsCount - 1);
			for (int i = 1; i < argsCount; i++) {
				concat.addArg(wrap.getArg(i));
			}
			return ArithNode.oneArgOp(ArithOp.ADD, fArg, InsnArg.wrapArg(concat));
		} catch (Exception e) {
			LOG.debug("Can't convert field arith insn: {}, mth: {}", insn, mth, e);
		}
		return null;
	}
}
