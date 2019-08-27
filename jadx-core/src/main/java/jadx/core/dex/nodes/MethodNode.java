package jadx.core.dex.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.dex.ClassData.Method;
import com.android.dex.Code;
import com.android.dex.Code.CatchHandler;
import com.android.dex.Code.Try;

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
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.SwitchNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.parser.SignatureParser;
import jadx.core.dex.regions.Region;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.utils.Utils.lockList;

public class MethodNode extends LineAttrNode implements ILoadable, ICodeNode {
	private static final Logger LOG = LoggerFactory.getLogger(MethodNode.class);

	private final MethodInfo mthInfo;
	private final ClassNode parentClass;
	private AccessInfo accFlags;

	private final Method methodData;
	private final boolean methodIsVirtual;

	private boolean noCode;
	private int regsCount;
	private int codeSize;
	private int debugInfoOffset;

	private boolean loaded;

	// additional info available after load, keep on unload
	private ArgType retType;
	private List<ArgType> argTypes;
	private List<GenericInfo> generics;

	// decompilation data, reset on unload
	private RegisterArg thisArg;
	private List<RegisterArg> argsList;
	private InsnNode[] instructions;
	private List<BlockNode> blocks;
	private BlockNode enterBlock;
	private List<BlockNode> exitBlocks;
	private List<SSAVar> sVars;
	private List<ExceptionHandler> exceptionHandlers;
	private List<LoopInfo> loops;
	private Region region;

	public MethodNode(ClassNode classNode, Method mthData, boolean isVirtual) {
		this.mthInfo = MethodInfo.fromDex(classNode.dex(), mthData.getMethodIndex());
		this.parentClass = classNode;
		this.accFlags = new AccessInfo(mthData.getAccessFlags(), AFType.METHOD);
		this.noCode = mthData.getCodeOffset() == 0;
		this.methodData = noCode ? null : mthData;
		this.methodIsVirtual = isVirtual;
		unload();
	}

	@Override
	public void unload() {
		loaded = false;
		if (noCode) {
			return;
		}
		// don't unload retType, argTypes, generics
		thisArg = null;
		argsList = null;
		sVars = Collections.emptyList();
		instructions = null;
		blocks = null;
		enterBlock = null;
		exitBlocks = null;
		region = null;
		exceptionHandlers = Collections.emptyList();
		loops = Collections.emptyList();
		unloadAttributes();
	}

	@Override
	public void load() throws DecodeException {
		if (loaded) {
			// method already loaded
			return;
		}
		try {
			loaded = true;
			if (noCode) {
				regsCount = 0;
				codeSize = 0;
				// TODO: registers not needed without code
				initArguments(this.argTypes);
				return;
			}

			DexNode dex = parentClass.dex();
			Code mthCode = dex.readCode(methodData);
			this.regsCount = mthCode.getRegistersSize();
			initArguments(this.argTypes);

			InsnDecoder decoder = new InsnDecoder(this);
			decoder.decodeInsns(mthCode);
			this.instructions = decoder.process();
			this.codeSize = instructions.length;

			initTryCatches(this, mthCode, instructions);
			initJumps(instructions);

			this.debugInfoOffset = mthCode.getDebugInfoOffset();
		} catch (Exception e) {
			if (!noCode) {
				unload();
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
			for (RegisterArg arg : list) {
				if (arg.getRegNum() >= regsCount) {
					throw new JadxRuntimeException("Incorrect register number in instruction: " + insnNode
							+ ", expected to be less than " + regsCount);
				}
			}
		}
	}

	public void initMethodTypes() {
		List<ArgType> types = parseSignature();
		if (types == null) {
			this.retType = mthInfo.getReturnType();
			this.argTypes = mthInfo.getArgumentsTypes();
		} else {
			this.argTypes = types;
		}
	}

	@Nullable
	private List<ArgType> parseSignature() {
		SignatureParser sp = SignatureParser.fromNode(this);
		if (sp == null) {
			return null;
		}
		try {
			this.generics = sp.consumeGenericMap();
			List<ArgType> argsTypes = sp.consumeMethodArgs();
			this.retType = sp.consumeType();

			List<ArgType> mthArgs = mthInfo.getArgumentsTypes();
			if (argsTypes.size() != mthArgs.size()) {
				if (argsTypes.isEmpty()) {
					return null;
				}
				if (!mthInfo.isConstructor()) {
					LOG.warn("Wrong signature parse result: {} -> {}, not generic version: {}", sp, argsTypes, mthArgs);
					return null;
				} else if (getParentClass().getAccessFlags().isEnum()) {
					// TODO:
					argsTypes.add(0, mthArgs.get(0));
					argsTypes.add(1, mthArgs.get(1));
				} else {
					// add synthetic arg for outer class
					argsTypes.add(0, mthArgs.get(0));
				}
				if (argsTypes.size() != mthArgs.size()) {
					return null;
				}
			}
			return argsTypes;
		} catch (JadxRuntimeException e) {
			LOG.error("Method signature parse error: {}", this, e);
			return null;
		}
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
			RegisterArg arg = InsnArg.reg(pos - 1, parentClass.getClassInfo().getType());
			arg.add(AFlag.THIS);
			arg.add(AFlag.IMMUTABLE_TYPE);
			thisArg = arg;
		}
		if (args.isEmpty()) {
			argsList = Collections.emptyList();
			return;
		}
		argsList = new ArrayList<>(args.size());
		for (ArgType argType : args) {
			RegisterArg regArg = InsnArg.reg(pos, argType);
			regArg.add(AFlag.METHOD_ARGUMENT);
			regArg.add(AFlag.IMMUTABLE_TYPE);
			argsList.add(regArg);
			pos += argType.getRegCount();
		}
	}

	@NotNull
	public List<ArgType> getArgTypes() {
		if (argTypes == null) {
			throw new JadxRuntimeException("Method types not initialized: " + this);
		}
		return argTypes;
	}

	public List<RegisterArg> getArgRegs() {
		if (argsList == null) {
			throw new JadxRuntimeException("Method args not loaded: " + this
					+ ", class status: " + parentClass.getTopParentClass().getState());
		}
		return argsList;
	}

	public List<RegisterArg> getAllArgRegs() {
		List<RegisterArg> argRegs = getArgRegs();
		if (thisArg != null) {
			List<RegisterArg> list = new ArrayList<>(argRegs.size() + 1);
			list.add(thisArg);
			list.addAll(argRegs);
			return list;
		}
		return argRegs;
	}

	@Nullable
	public RegisterArg getThisArg() {
		return thisArg;
	}

	public void skipFirstArgument() {
		this.add(AFlag.SKIP_FIRST_ARG);
	}

	public ArgType getReturnType() {
		return retType;
	}

	public List<GenericInfo> getGenerics() {
		return generics;
	}

	private static void initTryCatches(MethodNode mth, Code mthCode, InsnNode[] insnByOffset) {
		CatchHandler[] catchBlocks = mthCode.getCatchHandlers();
		Try[] tries = mthCode.getTries();
		if (catchBlocks.length == 0 && tries.length == 0) {
			return;
		}

		int handlersCount = 0;
		Set<Integer> addrs = new HashSet<>();
		List<TryCatchBlock> catches = new ArrayList<>(catchBlocks.length);

		for (CatchHandler handler : catchBlocks) {
			TryCatchBlock tcBlock = new TryCatchBlock();
			catches.add(tcBlock);
			int[] handlerAddrArr = handler.getAddresses();
			for (int i = 0; i < handlerAddrArr.length; i++) {
				int addr = handlerAddrArr[i];
				ClassInfo type = ClassInfo.fromDex(mth.dex(), handler.getTypeIndexes()[i]);
				tcBlock.addHandler(mth, addr, type);
				addrs.add(addr);
				handlersCount++;
			}
			int addr = handler.getCatchAllAddress();
			if (addr >= 0) {
				tcBlock.addHandler(mth, addr, null);
				addrs.add(addr);
				handlersCount++;
			}
		}

		if (handlersCount > 0 && handlersCount != addrs.size()) {
			// resolve nested try blocks:
			// inner block contains all handlers from outer block => remove these handlers from inner block
			// each handler must be only in one try/catch block
			for (TryCatchBlock outerTry : catches) {
				for (TryCatchBlock innerTry : catches) {
					if (outerTry != innerTry
							&& innerTry.containsAllHandlers(outerTry)) {
						innerTry.removeSameHandlers(outerTry);
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
				// TODO: don't override existing attribute
				insnByOffset[addr].addAttr(ehAttr);
			}
		}

		// attach TRY_ENTER, TRY_LEAVE attributes to instructions
		for (Try aTry : tries) {
			int catchNum = aTry.getCatchHandlerIndex();
			TryCatchBlock catchBlock = catches.get(catchNum);
			int offset = aTry.getStartAddress();
			int end = offset + aTry.getInstructionCount() - 1;

			boolean tryBlockStarted = false;
			InsnNode insn = null;
			while (offset <= end && offset >= 0) {
				insn = insnByOffset[offset];
				if (insn != null && insn.getType() != InsnType.NOP) {
					if (tryBlockStarted) {
						catchBlock.addInsn(insn);
					} else if (insn.canThrowException()) {
						insn.add(AFlag.TRY_ENTER);
						catchBlock.addInsn(insn);
						tryBlockStarted = true;
					}
				}
				offset = InsnDecoder.getNextInsnOffset(insnByOffset, offset);
			}
			if (tryBlockStarted && insn != null) {
				insn.add(AFlag.TRY_LEAVE);
			}
		}
	}

	private static void initJumps(InsnNode[] insnByOffset) {
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
		blocks = lockList(blocks);
		exitBlocks = lockList(exitBlocks);
		loops = lockList(loops);
		blocks.forEach(BlockNode::lock);
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
				if (h.equals(handler)) {
					return h;
				}
				if (h.getHandleOffset() == handler.getHandleOffset()) {
					if (h.getTryBlock() == handler.getTryBlock()) {
						for (ClassInfo catchType : handler.getCatchTypes()) {
							h.addCatchType(catchType);
						}
					} else {
						// same handlers from different try blocks
						// will merge later
					}
					return h;
				}
			}
		}
		exceptionHandlers.add(handler);
		return handler;
	}

	public boolean clearExceptionHandlers() {
		return exceptionHandlers.removeIf(ExceptionHandler::isRemoved);
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

	public boolean isConstructor() {
		return accFlags.isConstructor() && mthInfo.isConstructor();
	}

	public boolean isDefaultConstructor() {
		if (isConstructor()) {
			int defaultArgCount = 0;
			// workaround for non-static inner class constructor, that has synthetic argument
			if (parentClass.getClassInfo().isInner()
					&& !parentClass.getAccessFlags().isStatic()) {
				ClassNode outerCls = parentClass.getParentClass();
				if (argsList != null && !argsList.isEmpty()
						&& argsList.get(0).getInitType().equals(outerCls.getClassInfo().getType())) {
					defaultArgCount = 1;
				}
			}
			return argsList == null || argsList.size() == defaultArgCount;
		}
		return false;
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

	public SSAVar makeNewSVar(int regNum, @NotNull RegisterArg assignArg) {
		return makeNewSVar(regNum, getNextSVarVersion(regNum), assignArg);
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

	@Override
	public AccessInfo getAccessFlags() {
		return accFlags;
	}

	@Override
	public void setAccessFlags(AccessInfo newAccessFlags) {
		this.accFlags = newAccessFlags;
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

	@Override
	public String typeName() {
		return "method";
	}

	public void addWarn(String warnStr) {
		ErrorsCounter.methodWarn(this, warnStr);
	}

	public void addComment(String commentStr) {
		addAttr(AType.COMMENTS, commentStr);
		LOG.info("{} in {}", commentStr, this);
	}

	public void addError(String errStr, Throwable e) {
		ErrorsCounter.methodError(this, errStr, e);
	}

	public MethodInfo getMethodInfo() {
		return mthInfo;
	}

	public long getMethodCodeOffset() {
		return noCode ? 0 : methodData.getCodeOffset();
	}

	/**
	 * Stat method.
	 * Calculate instructions count as a measure of method size
	 */
	public long countInsns() {
		if (instructions != null) {
			return instructions.length;
		}
		if (blocks != null) {
			return blocks.stream().mapToLong(block -> block.getInstructions().size()).sum();
		}
		return -1;
	}

	public boolean isLoaded() {
		return loaded;
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
				+ '(' + Utils.listToString(mthInfo.getArgumentsTypes()) + "):"
				+ retType;
	}
}
