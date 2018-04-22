package jadx.core.dex.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.android.dex.ClassData.Method;
import com.android.dex.Code;
import com.android.dex.Code.CatchHandler;
import com.android.dex.Code.Try;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.JumpInfo;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.AccessInfo.AFType;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.GotoNode;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnDecoder;
import jadx.core.dex.instructions.SwitchNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.args.TypeImmutableArg;
import jadx.core.dex.nodes.parser.SignatureParser;
import jadx.core.dex.regions.Region;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class MethodNode extends LineAttrNode implements ILoadable, IDexNode {
	private static final Logger LOG = LoggerFactory.getLogger(MethodNode.class);

	private final MethodInfo mthInfo;
	private final ClassNode parentClass;
	private final AccessInfo accFlags;

	private final Method methodData;
	private int regsCount;
	private InsnNode[] instructions;
	private int codeSize;
	private int debugInfoOffset;
	private boolean noCode;
	private boolean methodIsVirtual;

	private ArgType retType;
	private RegisterArg thisArg;
	private List<RegisterArg> argsList;
	private List<SSAVar> sVars = Collections.emptyList();
	private Map<ArgType, List<ArgType>> genericMap;

	private List<BlockNode> blocks;
	private BlockNode enterBlock;
	private List<BlockNode> exitBlocks;

	private Region region;
	private List<ExceptionHandler> exceptionHandlers = Collections.emptyList();
	private List<LoopInfo> loops = Collections.emptyList();

	public MethodNode(ClassNode classNode, Method mthData, boolean isVirtual) {
		this.mthInfo = MethodInfo.fromDex(classNode.dex(), mthData.getMethodIndex());
		this.parentClass = classNode;
		this.accFlags = new AccessInfo(mthData.getAccessFlags(), AFType.METHOD);
		this.noCode = mthData.getCodeOffset() == 0;
		this.methodData = noCode ? null : mthData;
		this.methodIsVirtual = isVirtual;
	}

	@Override
	public void load() throws DecodeException {
		try {
			if (noCode) {
				regsCount = 0;
				codeSize = 0;
				initMethodTypes();
				return;
			}

			DexNode dex = parentClass.dex();
			Code mthCode = dex.readCode(methodData);
			regsCount = mthCode.getRegistersSize();
			initMethodTypes();

			InsnDecoder decoder = new InsnDecoder(this);
			decoder.decodeInsns(mthCode);
			instructions = decoder.process();
			codeSize = instructions.length;

			initTryCatches(mthCode);
			initJumps();

			this.debugInfoOffset = mthCode.getDebugInfoOffset();
		} catch (Exception e) {
			if (!noCode) {
				noCode = true;
				// load without code
				load();
				noCode = false;
			}
			throw new DecodeException(this, "Load method exception: " + e.getMessage(), e);
		}
	}

	public void checkInstructions() {
		List<RegisterArg> list = new ArrayList<>();
		for (InsnNode insnNode : instructions) {
			if (insnNode == null) {
				continue;
			}
			list.clear();
			RegisterArg resultArg = insnNode.getResult();
			if (resultArg != null) {
				list.add(resultArg);
			}
			insnNode.getRegisterArgs(list);
			int argsCount = list.size();
			for (int i = 0; i < argsCount; i++) {
				if (list.get(i).getRegNum() >= regsCount) {
					throw new JadxRuntimeException("Incorrect register number in instruction: " + insnNode
							+ ", expected to be less than " + regsCount);
				}
			}
		}
	}

	private void initMethodTypes() {
		if (!parseSignature()) {
			retType = mthInfo.getReturnType();
			initArguments(mthInfo.getArgumentsTypes());
		}
	}

	@Override
	public void unload() {
		if (noCode) {
			return;
		}
		instructions = null;
		blocks = null;
		exitBlocks = null;
		exceptionHandlers.clear();
	}

	private boolean parseSignature() {
		SignatureParser sp = SignatureParser.fromNode(this);
		if (sp == null) {
			return false;
		}
		try {
			genericMap = sp.consumeGenericMap();
			List<ArgType> argsTypes = sp.consumeMethodArgs();
			retType = sp.consumeType();

			List<ArgType> mthArgs = mthInfo.getArgumentsTypes();
			if (argsTypes.size() != mthArgs.size()) {
				if (argsTypes.isEmpty()) {
					return false;
				}
				if (!mthInfo.isConstructor()) {
					LOG.warn("Wrong signature parse result: {} -> {}, not generic version: {}", sp, argsTypes, mthArgs);
					return false;
				} else if (getParentClass().getAccessFlags().isEnum()) {
					// TODO:
					argsTypes.add(0, mthArgs.get(0));
					argsTypes.add(1, mthArgs.get(1));
				} else {
					// add synthetic arg for outer class
					argsTypes.add(0, mthArgs.get(0));
				}
				if (argsTypes.size() != mthArgs.size()) {
					return false;
				}
			}
			initArguments(argsTypes);
		} catch (JadxRuntimeException e) {
			LOG.error("Method signature parse error: {}", this, e);
			return false;
		}
		return true;
	}

	private void initArguments(List<ArgType> args) {
		int pos;
		if (noCode) {
			pos = 1;
		} else {
			pos = regsCount;
			for (ArgType arg : args) {
				pos -= arg.getRegCount();
			}
		}
		if (accFlags.isStatic()) {
			thisArg = null;
		} else {
			TypeImmutableArg arg = InsnArg.typeImmutableReg(pos - 1, parentClass.getClassInfo().getType());
			arg.markAsThis();
			thisArg = arg;
		}
		if (args.isEmpty()) {
			argsList = Collections.emptyList();
			return;
		}
		argsList = new ArrayList<>(args.size());
		for (ArgType arg : args) {
			argsList.add(InsnArg.typeImmutableReg(pos, arg));
			pos += arg.getRegCount();
		}
	}

	public List<RegisterArg> getArguments(boolean includeThis) {
		if (includeThis && thisArg != null) {
			List<RegisterArg> list = new ArrayList<>(argsList.size() + 1);
			list.add(thisArg);
			list.addAll(argsList);
			return list;
		}
		return argsList;
	}

	public RegisterArg removeFirstArgument() {
		this.add(AFlag.SKIP_FIRST_ARG);
		return argsList.remove(0);
	}

	public RegisterArg getThisArg() {
		return thisArg;
	}

	public ArgType getReturnType() {
		return retType;
	}

	public Map<ArgType, List<ArgType>> getGenericMap() {
		return genericMap;
	}

	private void initTryCatches(Code mthCode) {
		InsnNode[] insnByOffset = instructions;
		CatchHandler[] catchBlocks = mthCode.getCatchHandlers();
		Try[] tries = mthCode.getTries();
		if (catchBlocks.length == 0 && tries.length == 0) {
			return;
		}

		int hc = 0;
		Set<Integer> addrs = new HashSet<>();
		List<TryCatchBlock> catches = new ArrayList<>(catchBlocks.length);

		for (CatchHandler handler : catchBlocks) {
			TryCatchBlock tcBlock = new TryCatchBlock();
			catches.add(tcBlock);
			for (int i = 0; i < handler.getAddresses().length; i++) {
				int addr = handler.getAddresses()[i];
				ClassInfo type = ClassInfo.fromDex(parentClass.dex(), handler.getTypeIndexes()[i]);
				tcBlock.addHandler(this, addr, type);
				addrs.add(addr);
				hc++;
			}
			int addr = handler.getCatchAllAddress();
			if (addr >= 0) {
				tcBlock.addHandler(this, addr, null);
				addrs.add(addr);
				hc++;
			}
		}

		if (hc > 0 && hc != addrs.size()) {
			// resolve nested try blocks:
			// inner block contains all handlers from outer block => remove these handlers from inner block
			// each handler must be only in one try/catch block
			for (TryCatchBlock ct1 : catches) {
				for (TryCatchBlock ct2 : catches) {
					if (ct1 != ct2 && ct2.containsAllHandlers(ct1)) {
						for (ExceptionHandler h : ct1.getHandlers()) {
							ct2.removeHandler(this, h);
							h.setTryBlock(ct1);
						}
					}
				}
			}
		}

		// attach EXC_HANDLER attributes to instructions
		addrs.clear();
		for (TryCatchBlock ct : catches) {
			for (ExceptionHandler eh : ct.getHandlers()) {
				int addr = eh.getHandleOffset();
				ExcHandlerAttr ehAttr = new ExcHandlerAttr(ct, eh);
				insnByOffset[addr].addAttr(ehAttr);
			}
		}

		// attach TRY_ENTER, TRY_LEAVE attributes to instructions
		for (Try aTry : tries) {
			int catchNum = aTry.getCatchHandlerIndex();
			TryCatchBlock catchBlock = catches.get(catchNum);
			int offset = aTry.getStartAddress();
			int end = offset + aTry.getInstructionCount() - 1;

			InsnNode insn = insnByOffset[offset];
			insn.add(AFlag.TRY_ENTER);
			while (offset <= end && offset >= 0) {
				insn = insnByOffset[offset];
				catchBlock.addInsn(insn);
				offset = InsnDecoder.getNextInsnOffset(insnByOffset, offset);
			}
			if (insnByOffset[end] != null) {
				insnByOffset[end].add(AFlag.TRY_LEAVE);
			} else {
				insn.add(AFlag.TRY_LEAVE);
			}
		}
	}

	private void initJumps() {
		InsnNode[] insnByOffset = instructions;
		for (int offset = 0; offset < insnByOffset.length; offset++) {
			InsnNode insn = insnByOffset[offset];
			if (insn == null) {
				continue;
			}
			switch (insn.getType()) {
				case SWITCH:
					SwitchNode sw = (SwitchNode) insn;
					for (int target : sw.getTargets()) {
						addJump(insnByOffset, offset, target);
					}
					// default case
					int nextInsnOffset = InsnDecoder.getNextInsnOffset(insnByOffset, offset);
					if (nextInsnOffset != -1) {
						addJump(insnByOffset, offset, nextInsnOffset);
					}
					break;

				case IF:
					int next = InsnDecoder.getNextInsnOffset(insnByOffset, offset);
					if (next != -1) {
						addJump(insnByOffset, offset, next);
					}
					addJump(insnByOffset, offset, ((IfNode) insn).getTarget());
					break;

				case GOTO:
					addJump(insnByOffset, offset, ((GotoNode) insn).getTarget());
					break;

				default:
					break;
			}
		}
	}

	private static void addJump(InsnNode[] insnByOffset, int offset, int target) {
		insnByOffset[target].addAttr(AType.JUMP, new JumpInfo(offset, target));
	}

	public String getName() {
		return mthInfo.getName();
	}

	public String getAlias() {
		return mthInfo.getAlias();
	}

	public ClassNode getParentClass() {
		return parentClass;
	}

	public boolean isNoCode() {
		return noCode;
	}

	public int getCodeSize() {
		return codeSize;
	}

	public InsnNode[] getInstructions() {
		return instructions;
	}

	public void unloadInsnArr() {
		this.instructions = null;
	}

	public void initBasicBlocks() {
		blocks = new ArrayList<>();
		exitBlocks = new ArrayList<>(1);
	}

	public void finishBasicBlocks() {
		((ArrayList<BlockNode>) blocks).trimToSize();
		((ArrayList<BlockNode>) exitBlocks).trimToSize();

		blocks = Collections.unmodifiableList(blocks);
		exitBlocks = Collections.unmodifiableList(exitBlocks);

		for (BlockNode block : blocks) {
			block.lock();
		}
	}

	public List<BlockNode> getBasicBlocks() {
		return blocks;
	}

	public BlockNode getEnterBlock() {
		return enterBlock;
	}

	public void setEnterBlock(BlockNode enterBlock) {
		this.enterBlock = enterBlock;
	}

	public List<BlockNode> getExitBlocks() {
		return exitBlocks;
	}

	public void addExitBlock(BlockNode exitBlock) {
		this.exitBlocks.add(exitBlock);
	}

	public void registerLoop(LoopInfo loop) {
		if (loops.isEmpty()) {
			loops = new ArrayList<>(5);
		}
		loop.setId(loops.size());
		loops.add(loop);
	}

	@Nullable
	public LoopInfo getLoopForBlock(BlockNode block) {
		if (loops.isEmpty()) {
			return null;
		}
		for (LoopInfo loop : loops) {
			if (loop.getLoopBlocks().contains(block)) {
				return loop;
			}
		}
		return null;
	}

	public List<LoopInfo> getAllLoopsForBlock(BlockNode block) {
		if (loops.isEmpty()) {
			return Collections.emptyList();
		}
		List<LoopInfo> list = new ArrayList<>(loops.size());
		for (LoopInfo loop : loops) {
			if (loop.getLoopBlocks().contains(block)) {
				list.add(loop);
			}
		}
		return list;
	}

	public int getLoopsCount() {
		return loops.size();
	}

	public Iterable<LoopInfo> getLoops() {
		return loops;
	}

	public ExceptionHandler addExceptionHandler(ExceptionHandler handler) {
		if (exceptionHandlers.isEmpty()) {
			exceptionHandlers = new ArrayList<>(2);
		} else {
			for (ExceptionHandler h : exceptionHandlers) {
				if (h == handler || h.getHandleOffset() == handler.getHandleOffset()) {
					return h;
				}
			}
		}
		exceptionHandlers.add(handler);
		return handler;
	}

	public Iterable<ExceptionHandler> getExceptionHandlers() {
		return exceptionHandlers;
	}

	public boolean isNoExceptionHandlers() {
		return exceptionHandlers.isEmpty();
	}

	public int getExceptionHandlersCount() {
		return exceptionHandlers.size();
	}

	/**
	 * Return true if exists method with same name and arguments count
	 */
	public boolean isArgsOverload() {
		int argsCount = mthInfo.getArgumentsTypes().size();
		if (argsCount == 0) {
			return false;
		}

		String name = getName();
		for (MethodNode method : parentClass.getMethods()) {
			MethodInfo otherMthInfo = method.mthInfo;
			if (this != method
					&& otherMthInfo.getArgumentsTypes().size() == argsCount
					&& otherMthInfo.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}

	public boolean isDefaultConstructor() {
		boolean result = false;
		if (accFlags.isConstructor() && mthInfo.isConstructor()) {
			int defaultArgCount = 0;
			// workaround for non-static inner class constructor, that has synthetic argument
			if (parentClass.getClassInfo().isInner()
					&& !parentClass.getAccessFlags().isStatic()) {
				ClassNode outerCls = parentClass.getParentClass();
				if (argsList != null && !argsList.isEmpty()
						&& argsList.get(0).getType().equals(outerCls.getClassInfo().getType())) {
					defaultArgCount = 1;
				}
			}
			result = argsList == null || argsList.size() == defaultArgCount;
		}
		return result;
	}

	public boolean isVirtual() {
		return methodIsVirtual;
	}

	public int getRegsCount() {
		return regsCount;
	}

	public int getDebugInfoOffset() {
		return debugInfoOffset;
	}

	public SSAVar makeNewSVar(int regNum, int version, @NotNull RegisterArg assignArg) {
		SSAVar var = new SSAVar(regNum, version, assignArg);
		if (sVars.isEmpty()) {
			sVars = new ArrayList<>();
		}
		sVars.add(var);
		return var;
	}

	public int getNextSVarVersion(int regNum) {
		int v = -1;
		for (SSAVar sVar : sVars) {
			if (sVar.getRegNum() == regNum) {
				v = Math.max(v, sVar.getVersion());
			}
		}
		v++;
		return v;
	}

	public void removeSVar(SSAVar var) {
		sVars.remove(var);
	}

	public List<SSAVar> getSVars() {
		return sVars;
	}

	public AccessInfo getAccessFlags() {
		return accFlags;
	}

	public Region getRegion() {
		return region;
	}

	public void setRegion(Region region) {
		this.region = region;
	}

	@Override
	public DexNode dex() {
		return parentClass.dex();
	}

	@Override
	public RootNode root() {
		return dex().root();
	}

	public MethodInfo getMethodInfo() {
		return mthInfo;
	}

	@Override
	public int hashCode() {
		return mthInfo.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		MethodNode other = (MethodNode) obj;
		return mthInfo.equals(other.mthInfo);
	}

	@Override
	public String toString() {
		return parentClass + "." + mthInfo.getName()
				+ "(" + Utils.listToString(mthInfo.getArgumentsTypes()) + "):"
				+ retType;
	}
}
