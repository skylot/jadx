package jadx.core.dex.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.ICodeReader;
import jadx.api.plugins.input.data.IDebugInfo;
import jadx.api.plugins.input.data.IMethodData;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.core.Consts;
import jadx.core.codegen.NameGen;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.annotations.AnnotationsList;
import jadx.core.dex.attributes.annotations.MethodParameters;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.attributes.nodes.NotificationAttrNode;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.info.AccessInfo.AFType;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.InsnDecoder;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.args.VisibleVar;
import jadx.core.dex.nodes.VariableNode.VarKind;
import jadx.core.dex.nodes.utils.TypeUtils;
import jadx.core.dex.regions.Region;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.utils.Utils.lockList;

public class MethodNode extends NotificationAttrNode implements IMethodDetails, ILoadable, ICodeNode, Comparable<MethodNode> {
	private static final Logger LOG = LoggerFactory.getLogger(MethodNode.class);

	private final MethodInfo mthInfo;
	private final ClassNode parentClass;
	private AccessInfo accFlags;

	private final ICodeReader codeReader;
	private final boolean methodIsVirtual;
	private final int insnsCount;

	private boolean noCode;
	private int regsCount;

	private boolean loaded;

	// additional info available after load, keep on unload
	private ArgType retType;
	private List<ArgType> argTypes;
	private List<ArgType> typeParameters;

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

	private List<MethodNode> useIn = Collections.emptyList();
	private List<VariableNode> variables = new ArrayList<>();

	public static MethodNode build(ClassNode classNode, IMethodData methodData) {
		MethodNode methodNode = new MethodNode(classNode, methodData);
		AnnotationsList.attach(methodNode, methodData.getAnnotations());
		MethodParameters.attach(methodNode, methodData.getParamsAnnotations());
		return methodNode;
	}

	private MethodNode(ClassNode classNode, IMethodData mthData) {
		this.mthInfo = MethodInfo.fromRef(classNode.root(), mthData.getMethodRef());
		this.parentClass = classNode;
		this.accFlags = new AccessInfo(mthData.getAccessFlags(), AFType.METHOD);
		this.methodIsVirtual = !mthData.isDirect();
		ICodeReader codeReader = mthData.getCodeReader();
		this.noCode = codeReader == null;
		if (noCode) {
			this.codeReader = null;
			this.insnsCount = 0;
		} else {
			this.codeReader = codeReader.copy();
			this.insnsCount = codeReader.getInsnsCount();
		}

		this.retType = mthInfo.getReturnType();
		this.argTypes = mthInfo.getArgumentsTypes();
		this.typeParameters = Collections.emptyList();
		unload();
	}

	public List<VariableNode> getVars() {
		return new ArrayList<>(variables);
	}

	public VariableNode getVariable(int index) {
		if (index >= 0 && index < variables.size()) {
			return variables.get(index);
		}
		return null;
	}

	public VariableNode declareVar(VisibleVar var, NameGen nameGen, VarKind varKind) {
		if (var instanceof CodeVar) {
			if (((CodeVar) var).isThis()) {
				return null;
			}
		}
		VariableNode varNode;
		int index = var.getIndex();
		if (index > -1) {
			varNode = getVariable(var.getIndex());
		} else {
			index = variables.size();
			var.setIndex(index);
			String name = mthInfo.getVariableName(VariableNode.makeVarIndex(index, varKind));
			if (name != null) {
				var.setName(name); // set name with user renamed previously.
			}
			if (var instanceof CodeVar) { // let NameGen record this name or gen an valid name.
				name = nameGen.assignArg((CodeVar) var);
			} else if (var instanceof NamedArg) {
				name = nameGen.assignNamedArg((NamedArg) var);
			} else {
				throw new JadxRuntimeException("Unexpected var type: " + var);
			}
			varNode = new VariableNode(this, name, var.getType(), varKind, index);
			this.variables.add(varNode);
		}
		return varNode;
	}

	@Override
	public void unload() {
		loaded = false;
		if (noCode) {
			return;
		}
		// don't unload retType, argTypes, typeParameters
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

	public void updateTypes(List<ArgType> argTypes, ArgType retType) {
		this.argTypes = argTypes;
		this.retType = retType;
	}

	public void updateTypeParameters(List<ArgType> typeParameters) {
		this.typeParameters = typeParameters;
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
				// TODO: registers not needed without code
				initArguments(this.argTypes);
				return;
			}

			this.regsCount = codeReader.getRegistersCount();
			initArguments(this.argTypes);
			InsnDecoder decoder = new InsnDecoder(this);
			this.instructions = decoder.process(codeReader);
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

	public void reload() {
		unload();
		try {
			load();
		} catch (DecodeException e) {
			throw new JadxRuntimeException("Failed to reload method " + getClass().getName() + "." + getName());
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
		TypeUtils typeUtils = root().getTypeUtils();
		if (accFlags.isStatic()) {
			thisArg = null;
		} else {
			ArgType thisClsType = typeUtils.expandTypeVariables(this, parentClass.getType());
			RegisterArg arg = InsnArg.reg(pos - 1, thisClsType);
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
			ArgType expandedType = typeUtils.expandTypeVariables(this, argType);
			RegisterArg regArg = InsnArg.reg(pos, expandedType);
			regArg.add(AFlag.METHOD_ARGUMENT);
			regArg.add(AFlag.IMMUTABLE_TYPE);
			argsList.add(regArg);
			pos += argType.getRegCount();
		}
	}

	@Override
	@NotNull
	public List<ArgType> getArgTypes() {
		if (argTypes == null) {
			throw new JadxRuntimeException("Method generic types not initialized: " + this);
		}
		return argTypes;
	}

	public void updateArgTypes(List<ArgType> newArgTypes, String comment) {
		this.addDebugComment(comment + ", original types: " + getArgTypes());
		this.argTypes = Collections.unmodifiableList(newArgTypes);
		initArguments(newArgTypes);
	}

	public boolean containsGenericArgs() {
		return !Objects.equals(mthInfo.getArgumentsTypes(), getArgTypes());
	}

	@Override
	@NotNull
	public ArgType getReturnType() {
		return retType;
	}

	public void updateReturnType(ArgType type) {
		this.retType = type;
	}

	public boolean isVoidReturn() {
		return mthInfo.getReturnType().equals(ArgType.VOID);
	}

	public List<RegisterArg> getArgRegs() {
		if (argsList == null) {
			throw new JadxRuntimeException("Method arg registers not loaded: " + this
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

	@Override
	public List<ArgType> getTypeParameters() {
		return typeParameters;
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

	@Override
	@SuppressWarnings("unchecked")
	public List<ArgType> getThrows() {
		IAnnotation an = getAnnotation(Consts.DALVIK_THROWS);
		if (an == null) {
			return Collections.emptyList();
		}
		List<EncodedValue> types = (List<EncodedValue>) an.getDefaultValue().getValue();
		return Utils.collectionMap(types, ev -> ArgType.object((String) ev.getValue()));
	}

	/**
	 * Return true if exists method with same name and arguments count
	 */
	public boolean isArgsOverloaded() {
		MethodInfo thisMthInfo = this.mthInfo;
		// quick check in current class
		for (MethodNode method : parentClass.getMethods()) {
			if (method == this) {
				continue;
			}
			if (method.getMethodInfo().isOverloadedBy(thisMthInfo)) {
				return true;
			}
		}
		return root().getMethodUtils().isMethodArgsOverloaded(parentClass.getClassInfo().getType(), thisMthInfo);
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

	public SSAVar makeNewSVar(@NotNull RegisterArg assignArg) {
		int regNum = assignArg.getRegNum();
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
	public int getRawAccessFlags() {
		return accFlags.rawValue();
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
	public RootNode root() {
		return parentClass.root();
	}

	@Override
	public String typeName() {
		return "method";
	}

	@Override
	public String getInputFileName() {
		return parentClass.getInputFileName();
	}

	@Override
	public MethodInfo getMethodInfo() {
		return mthInfo;
	}

	public long getMethodCodeOffset() {
		return noCode ? 0 : codeReader.getCodeOffset();
	}

	@Nullable
	public IDebugInfo getDebugInfo() {
		return noCode ? null : codeReader.getDebugInfo();
	}

	public void ignoreMethod() {
		add(AFlag.DONT_GENERATE);
		noCode = true;
	}

	/**
	 * Calculate instructions count at currect stage
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

	/**
	 * Raw instructions count in method bytecode
	 */
	public int getInsnsCount() {
		return insnsCount;
	}

	@Override
	public boolean isVarArg() {
		return accFlags.isVarArgs();
	}

	public boolean isLoaded() {
		return loaded;
	}

	public ICodeReader getCodeReader() {
		return codeReader;
	}

	public List<MethodNode> getUseIn() {
		return useIn;
	}

	public void setUseIn(List<MethodNode> useIn) {
		this.useIn = useIn;
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
	public int compareTo(@NotNull MethodNode o) {
		return mthInfo.compareTo(o.mthInfo);
	}

	@Override
	public String toAttrString() {
		return IMethodDetails.super.toAttrString() + " (m)";
	}

	@Override
	public String toString() {
		return parentClass + "." + mthInfo.getName()
				+ '(' + Utils.listToString(argTypes) + "):"
				+ retType;
	}
}
