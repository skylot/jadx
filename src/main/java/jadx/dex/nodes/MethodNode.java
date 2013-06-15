package jadx.dex.nodes;

import jadx.Consts;
import jadx.dex.attributes.AttrNode;
import jadx.dex.attributes.AttributeFlag;
import jadx.dex.attributes.JumpAttribute;
import jadx.dex.attributes.annotations.Annotation;
import jadx.dex.info.AccessInfo;
import jadx.dex.info.AccessInfo.AFType;
import jadx.dex.info.ClassInfo;
import jadx.dex.info.MethodInfo;
import jadx.dex.instructions.GotoNode;
import jadx.dex.instructions.IfNode;
import jadx.dex.instructions.InsnDecoder;
import jadx.dex.instructions.SwitchNode;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.instructions.args.InsnArg;
import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.instructions.mods.ConstructorInsn;
import jadx.dex.nodes.parser.DebugInfoParser;
import jadx.dex.trycatch.ExcHandlerAttr;
import jadx.dex.trycatch.ExceptionHandler;
import jadx.dex.trycatch.TryCatchBlock;
import jadx.utils.Utils;
import jadx.utils.exceptions.DecodeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.android.dx.io.ClassData.Method;
import com.android.dx.io.Code;
import com.android.dx.io.Code.CatchHandler;
import com.android.dx.io.Code.Try;

public class MethodNode extends AttrNode implements ILoadable {
	private static final Logger LOG = LoggerFactory.getLogger(MethodNode.class);

	private final MethodInfo mthInfo;
	private final ClassNode parentClass;
	private final AccessInfo accFlags;

	private final Method methodData;
	private int regsCount;
	private List<InsnNode> instructions;
	private boolean noCode;

	private ArgType retType;
	private RegisterArg thisArg;
	private List<RegisterArg> argsList;
	private Map<ArgType, List<ArgType>> genericMap;

	private List<BlockNode> blocks;
	private BlockNode enterBlock;
	private List<BlockNode> exitBlocks;

	private ConstructorInsn superCall;

	private IContainer region;
	private List<ExceptionHandler> exceptionHandlers;

	public MethodNode(ClassNode classNode, Method mthData) {
		this.mthInfo = MethodInfo.fromDex(classNode.dex(), mthData.getMethodIndex());
		this.parentClass = classNode;
		this.accFlags = new AccessInfo(mthData.getAccessFlags(), AFType.METHOD);
		this.noCode = (mthData.getCodeOffset() == 0);
		this.methodData = (noCode ? null : mthData);
	}

	@Override
	public void load() throws DecodeException {
		try {
			if (noCode) {
				regsCount = 0;
				initMethodTypes();
				return;
			}

			DexNode dex = parentClass.dex();
			Code mthCode = dex.readCode(methodData);
			regsCount = mthCode.getRegistersSize();
			initMethodTypes();

			InsnDecoder decoder = new InsnDecoder(this, mthCode);
			InsnNode[] insnByOffset = decoder.run();
			instructions = new ArrayList<InsnNode>();
			for (InsnNode insn : insnByOffset) {
				if (insn != null)
					instructions.add(insn);
			}
			((ArrayList<InsnNode>) instructions).trimToSize();

			initTryCatches(mthCode, insnByOffset);
			initJumps(insnByOffset);

			if (mthCode.getDebugInfoOffset() > 0) {
				(new DebugInfoParser(this, mthCode.getDebugInfoOffset(), insnByOffset)).process();
			}
		} catch (Exception e) {
			throw new DecodeException(this, "Load method exception", e);
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
		if (noCode)
			return;

		if (instructions != null) instructions.clear();
		blocks = null;
		exitBlocks = null;
		if (exceptionHandlers != null) exceptionHandlers.clear();
		getAttributes().clear();
		noCode = true;
	}

	@SuppressWarnings("unchecked")
	private boolean parseSignature() {
		Annotation a = getAttributes().getAnnotation(Consts.DALVIK_SIGNATURE);
		if (a == null)
			return false;

		String sign = Utils.mergeSignature((List<String>) a.getDefaultValue());

		// parse generic map
		int end = Utils.getGenericEnd(sign);
		if (end != -1) {
			String gen = sign.substring(1, end);
			genericMap = ArgType.parseGenericMap(gen);
			sign = sign.substring(end + 1);
		}

		int firstBracket = sign.indexOf('(');
		int lastBracket = sign.lastIndexOf(')');
		String argsTypesStr = sign.substring(firstBracket + 1, lastBracket);
		String returnType = sign.substring(lastBracket + 1);

		retType = ArgType.parseSignature(returnType);
		if (retType == null) {
			LOG.warn("Signature parse error: {}", returnType);
			return false;
		}

		List<ArgType> argsTypes = ArgType.parseSignatureList(argsTypesStr);
		if (argsTypes == null)
			return false;

		if (argsTypes.size() != mthInfo.getArgumentsTypes().size()) {
			if (argsTypes.isEmpty()) {
				return false;
			}
			if (!mthInfo.isConstructor()) {
				LOG.warn("Wrong signature parse result: " + sign + " -> " + argsTypes
						+ ", not generic version: " + mthInfo.getArgumentsTypes());
				return false;
			} else if (getParentClass().getAccessFlags().isEnum()) {
				// TODO:
				argsTypes.add(0, mthInfo.getArgumentsTypes().get(0));
				argsTypes.add(1, mthInfo.getArgumentsTypes().get(1));
			} else {
				// add synthetic arg for outer class
				argsTypes.add(0, mthInfo.getArgumentsTypes().get(0));
			}
			if (argsTypes.size() != mthInfo.getArgumentsTypes().size()) {
				return false;
			}
		}

		initArguments(argsTypes);
		return true;
	}

	private void initArguments(List<ArgType> args) {
		int pos;
		if (noCode) {
			pos = 1;
		} else {
			pos = regsCount;
			for (ArgType arg : args)
				pos -= arg.getRegCount();
		}

		if (accFlags.isStatic()) {
			thisArg = null;
		} else {
			thisArg = InsnArg.reg(pos - 1, parentClass.getClassInfo().getType());
			thisArg.getTypedVar().setName("this");
		}

		if (args.isEmpty()) {
			argsList = Collections.emptyList();
			return;
		}

		argsList = new ArrayList<RegisterArg>(args.size());
		for (ArgType arg : args) {
			argsList.add(InsnArg.reg(pos, arg));
			pos += arg.getRegCount();
		}
	}

	public List<RegisterArg> getArguments(boolean includeThis) {
		if (includeThis && thisArg != null) {
			List<RegisterArg> list = new ArrayList<RegisterArg>(argsList.size() + 1);
			list.add(thisArg);
			list.addAll(argsList);
			return list;
		} else {
			return argsList;
		}
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

	// TODO: move to external class
	private void initTryCatches(Code mthCode, InsnNode[] insnByOffset) {
		CatchHandler[] catchBlocks = mthCode.getCatchHandlers();
		Try[] tries = mthCode.getTries();

		// Bug in dx library already fixed (Try.getHandlerOffset() replaced by Try.getCatchHandlerIndex())
		// and we don't need this mapping anymore,
		// but in maven repository still old version
		Set<Integer> handlerSet = new HashSet<Integer>(tries.length);
		for (Try try_ : tries) {
			handlerSet.add(try_.getHandlerOffset());
		}
		List<Integer> handlerList = new ArrayList<Integer>(catchBlocks.length);
		handlerList.addAll(handlerSet);
		Collections.sort(handlerList);
		handlerSet = null;
		// -------------------

		int hc = 0;
		Set<Integer> addrs = new HashSet<Integer>();
		List<TryCatchBlock> catches = new ArrayList<TryCatchBlock>(catchBlocks.length);

		for (CatchHandler catch_ : catchBlocks) {
			TryCatchBlock tcBlock = new TryCatchBlock();
			catches.add(tcBlock);
			for (int i = 0; i < catch_.getAddresses().length; i++) {
				int addr = catch_.getAddresses()[i];
				ClassInfo type = ClassInfo.fromDex(parentClass.dex(), catch_.getTypeIndexes()[i]);
				tcBlock.addHandler(this, addr, type);
				addrs.add(addr);
				hc++;
			}
			int addr = catch_.getCatchAllAddress();
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
			for (TryCatchBlock ct1 : catches)
				for (TryCatchBlock ct2 : catches)
					if (ct1 != ct2 && ct2.getHandlers().containsAll(ct1.getHandlers()))
						for (ExceptionHandler h : ct1.getHandlers())
							ct2.removeHandler(this, h);
		}

		// attach EXC_HANDLER attributes to instructions
		addrs.clear();
		for (TryCatchBlock ct : catches) {
			for (ExceptionHandler eh : ct.getHandlers()) {
				int addr = eh.getHandleOffset();
				// assert addrs.add(addr) : "Instruction already contains EXC_HANDLER attribute";
				ExcHandlerAttr ehAttr = new ExcHandlerAttr(ct, eh);
				insnByOffset[addr].getAttributes().add(ehAttr);
			}
		}

		// attach TRY_ENTER, TRY_LEAVE attributes to instructions
		for (Try try_ : tries) {
			int catchNum = handlerList.indexOf(try_.getHandlerOffset());
			TryCatchBlock block = catches.get(catchNum);
			int offset = try_.getStartAddress();
			int end = offset + try_.getInstructionCount() - 1;

			insnByOffset[offset].getAttributes().add(AttributeFlag.TRY_ENTER);
			while (offset <= end && offset >= 0) {
				block.addInsn(insnByOffset[offset]);
				offset = InsnDecoder.getNextInsnOffset(insnByOffset, offset);
			}
			if (insnByOffset[end] != null)
				insnByOffset[end].getAttributes().add(AttributeFlag.TRY_LEAVE);
		}
	}

	private void initJumps(InsnNode[] insnByOffset) {
		for (InsnNode insn : getInstructions()) {
			int offset = insn.getOffset();
			switch (insn.getType()) {
				case SWITCH: {
					SwitchNode sw = (SwitchNode) insn;
					for (int target : sw.getTargets()) {
						addJump(insnByOffset, offset, target);
					}
					// default case
					int next = InsnDecoder.getNextInsnOffset(insnByOffset, offset);
					if (next != -1)
						addJump(insnByOffset, offset, next);
					break;
				}

				case IF:
					int next = InsnDecoder.getNextInsnOffset(insnByOffset, offset);
					if (next != -1)
						addJump(insnByOffset, offset, next);
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
		insnByOffset[target].getAttributes().add(new JumpAttribute(offset, target));
	}

	public String getName() {
		String name = mthInfo.getName();
		if (name.equals(parentClass.getShortName()))
			return name + "_";
		else
			return name;
	}

	public ClassNode getParentClass() {
		return parentClass;
	}

	public boolean isNoCode() {
		return noCode;
	}

	public List<InsnNode> getInstructions() {
		return instructions;
	}

	public void initBasicBlocks() {
		blocks = new ArrayList<BlockNode>();
		exitBlocks = new ArrayList<BlockNode>(1);
	}

	public void finishBasicBlocks() {
		// after filling basic blocks we don't need instructions list anymore
		instructions.clear();
		instructions = null;

		((ArrayList<BlockNode>) blocks).trimToSize();
		((ArrayList<BlockNode>) exitBlocks).trimToSize();

		blocks = Collections.unmodifiableList(blocks);
		exitBlocks = Collections.unmodifiableList(exitBlocks);

		for (BlockNode block : blocks)
			block.lock();
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

	public ExceptionHandler addExceptionHandler(ExceptionHandler handler) {
		if (exceptionHandlers == null) {
			exceptionHandlers = new ArrayList<ExceptionHandler>(2);
		} else {
			for (ExceptionHandler h : exceptionHandlers) {
				if (h == handler || h.getHandleOffset() == handler.getHandleOffset())
					return h;
			}
		}
		exceptionHandlers.add(handler);
		return handler;
	}

	public List<ExceptionHandler> getExceptionHandlers() {
		return exceptionHandlers;
	}

	public int getRegsCount() {
		return regsCount;
	}

	public AccessInfo getAccessFlags() {
		return accFlags;
	}

	public void setSuperCall(ConstructorInsn insn) {
		this.superCall = insn;
	}

	public ConstructorInsn getSuperCall() {
		return this.superCall;
	}

	public IContainer getRegion() {
		return region;
	}

	public void setRegion(IContainer region) {
		this.region = region;
	}

	public DexNode dex() {
		return parentClass.dex();
	}

	public MethodInfo getMethodInfo() {
		return mthInfo;
	}

	@Override
	public String toString() {
		return retType
				+ " " + parentClass.getFullName() + "." + mthInfo.getName()
				+ "(" + Utils.listToString(mthInfo.getArgumentsTypes()) + ")";
	}

}
