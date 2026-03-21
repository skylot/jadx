package jadx.core.dex.visitors.typeinference;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.core.Consts;
import jadx.core.clsp.ClspClass;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.ArithNode;
import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.IMethodDetails;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.nodes.utils.TypeUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.visitors.typeinference.TypeUpdateResult.CHANGED;
import static jadx.core.dex.visitors.typeinference.TypeUpdateResult.REJECT;
import static jadx.core.dex.visitors.typeinference.TypeUpdateResult.SAME;

public final class TypeUpdate {
	private static final Logger LOG = LoggerFactory.getLogger(TypeUpdate.class);

	private final RootNode root;
	private final Map<InsnType, ITypeListener> listenerRegistry;
	private final TypeCompare comparator;
	private final JadxArgs args;

	public TypeUpdate(RootNode root) {
		this.root = root;
		this.args = root.getArgs();
		this.listenerRegistry = initListenerRegistry();
		this.comparator = new TypeCompare(root);
	}

	/**
	 * Perform type checking and type propagation for all related variables
	 */
	public TypeUpdateResult apply(MethodNode mth, SSAVar ssaVar, ArgType candidateType) {
		return apply(mth, ssaVar, candidateType, TypeUpdateFlags.FLAGS_EMPTY);
	}

	/**
	 * Allow wider types for apply from debug info and some special cases
	 */
	public TypeUpdateResult applyWithWiderAllow(MethodNode mth, SSAVar ssaVar, ArgType candidateType) {
		return apply(mth, ssaVar, candidateType, TypeUpdateFlags.FLAGS_WIDER);
	}

	/**
	 * Force type setting
	 */
	public TypeUpdateResult applyWithWiderIgnSame(MethodNode mth, SSAVar ssaVar, ArgType candidateType) {
		return apply(mth, ssaVar, candidateType, TypeUpdateFlags.FLAGS_WIDER_IGNORE_SAME);
	}

	public TypeUpdateResult applyDebugInfo(MethodNode mth, SSAVar ssaVar, ArgType candidateType) {
		return apply(mth, ssaVar, candidateType, TypeUpdateFlags.FLAGS_APPLY_DEBUG);
	}

	private TypeUpdateResult apply(MethodNode mth, SSAVar ssaVar, ArgType candidateType, TypeUpdateFlags flags) {
		try {
			if (candidateType == null || !candidateType.isTypeKnown()) {
				return REJECT;
			}
			if (Consts.DEBUG_TYPE_INFERENCE) {
				LOG.debug("Start type update for {} to {}", ssaVar.toShortString(), candidateType);
			}
			TypeUpdateInfo updateInfo = new TypeUpdateInfo(mth, flags, args);
			TypeUpdateResult result = queueTypeUpdate(updateInfo, ssaVar.getAssign(), candidateType, null);
			if (result == null) {
				result = runUpdate(updateInfo);
			}
			if (result == REJECT) {
				return result;
			}
			if (updateInfo.isEmpty()) {
				return SAME;
			}
			if (Consts.DEBUG_TYPE_INFERENCE) {
				LOG.debug("Applying type {} to {}:", candidateType, ssaVar.toShortString());
				updateInfo.getSortedUpdates().forEach(upd -> LOG.debug("  {} -> {} in {}",
						upd.getType(), upd.getArg().toShortString(), upd.getArg().getParentInsn()));
			}
			updateInfo.applyUpdates();
			return CHANGED;
		} catch (Exception e) {
			mth.addWarnComment("Type update failed for variable: " + ssaVar + ", new type: " + candidateType, e);
			return REJECT;
		}
	}

	private TypeUpdateResult runUpdate(TypeUpdateInfo updateInfo) {
		TypeUpdateResult result = REJECT;
		while (true) {
			TypeUpdateRequest request = updateInfo.pollNextRequest();
			if (request == null) {
				return result;
			}
			InsnArg updateArg = request.getArg();
			ArgType updateType = request.getCandidateType();
			TypeUpdateResult newResult;
			if (request.isDirect()) {
				newResult = requestUpdate(updateInfo, updateArg, updateType);
			} else {
				newResult = updateTypeForArg(updateInfo, updateArg, updateType);
			}
			updateInfo.saveCallback(request);
			if (newResult == null) {
				// no result: continue
			} else {
				// propagate result back through callbacks
				result = processCallbacks(updateInfo, newResult);
			}
		}
	}

	private static TypeUpdateResult processCallbacks(TypeUpdateInfo updateInfo, TypeUpdateResult result) {
		TypeUpdateResult current = result;
		while (true) {
			TypeUpdateRequest cbReq = updateInfo.pollNextCallback();
			if (cbReq == null) {
				return current;
			}
			ITypeUpdateCallback callback = Objects.requireNonNull(cbReq.getCallback());
			current = callback.updateCallback(current);
			if (current == null) {
				// no result, put callback back into queue
				// so it can be executed once result is calculated
				updateInfo.saveCallback(cbReq);
				return null;
			}
			if (current == REJECT) {
				updateInfo.rollbackUpdate(cbReq.getArg());
			}
			// proceed to next callback
		}
	}

	/**
	 * Queue type update for InsnArg.
	 *
	 * @param callback - will be executed when result for this update is calculated,
	 *                 can be null - callback will pass result without change
	 * @return null if update added into queue, non-null result if not queued (verify failed)
	 */
	public @Nullable TypeUpdateResult queueTypeUpdate(TypeUpdateInfo updateInfo,
			InsnArg arg, ArgType candidateType, @Nullable ITypeUpdateCallback callback) {
		// verify can be done in queue processing before request run, but kept here for faster processing
		// this might increase code complexity because result should be checked for null every time
		TypeUpdateResult res = verifyType(updateInfo, arg, candidateType);
		if (res != null) {
			if (callback == null) {
				return res;
			}
			TypeUpdateResult result = callback.updateCallback(res);
			if (result == null) {
				updateInfo.saveCallback(new TypeUpdateRequest(arg, candidateType, false, callback));
			}
			return result;
		}
		updateInfo.queueRequest(new TypeUpdateRequest(arg, candidateType, false, callback));
		return null;
	}

	public @Nullable TypeUpdateResult queueDirectTypeUpdate(TypeUpdateInfo updateInfo, InsnArg arg, ArgType candidateType,
			@Nullable ITypeUpdateCallback callback) {
		updateInfo.queueRequest(new TypeUpdateRequest(arg, candidateType, true, callback));
		return null;
	}

	private TypeUpdateResult updateTypeForArg(TypeUpdateInfo updateInfo, InsnArg arg, ArgType candidateType) {
		if (Consts.DEBUG_TYPE_INFERENCE) {
			LOG.debug("-> update type for: {} to {}", arg, candidateType);
		}
		if (arg instanceof RegisterArg) {
			RegisterArg reg = (RegisterArg) arg;
			return updateTypeForSsaVar(updateInfo, reg.getSVar(), candidateType);
		}
		return requestUpdate(updateInfo, arg, candidateType);
	}

	private @Nullable TypeUpdateResult verifyType(TypeUpdateInfo updateInfo, InsnArg arg, ArgType candidateType) {
		if (candidateType == null) {
			throw new JadxRuntimeException("Null type update for arg: " + arg);
		}
		if (updateInfo.isProcessed(arg)) {
			return CHANGED;
		}
		ArgType currentType = arg.getType();
		TypeUpdateFlags typeUpdateFlags = updateInfo.getFlags();
		if (Objects.equals(currentType, candidateType)) {
			if (!typeUpdateFlags.isIgnoreSame()) {
				return SAME;
			}
		} else {
			if (candidateType.isWildcard()) {
				if (Consts.DEBUG_TYPE_INFERENCE) {
					LOG.debug("Wildcard type rejected for {}: candidate={}, current={}", arg, candidateType, currentType);
				}
				return REJECT;
			}

			TypeCompareEnum compareResult = comparator.compareTypes(candidateType, currentType);
			if (compareResult.isConflict()) {
				if (Consts.DEBUG_TYPE_INFERENCE) {
					LOG.debug("Type rejected for {}: candidate={} in conflict with current={}", arg, candidateType, currentType);
				}
				return REJECT;
			}
			if (compareResult == TypeCompareEnum.UNKNOWN && typeUpdateFlags.isIgnoreUnknown()) {
				return REJECT;
			}
			if (arg.isTypeImmutable() && currentType != ArgType.UNKNOWN) {
				// don't changed type
				if (compareResult == TypeCompareEnum.EQUAL) {
					return SAME;
				}
				if (Consts.DEBUG_TYPE_INFERENCE) {
					LOG.debug("Type rejected for {} due to conflict: candidate={}, current={}", arg, candidateType, currentType);
				}
				return REJECT;
			}
			if (compareResult == TypeCompareEnum.WIDER_BY_GENERIC && typeUpdateFlags.isKeepGenerics()) {
				if (Consts.DEBUG_TYPE_INFERENCE) {
					LOG.debug("Type rejected for {}: candidate={} is removing generic from current={}", arg, candidateType, currentType);
				}
				return REJECT;
			}
			if (compareResult.isWider() && !typeUpdateFlags.isAllowWider()) {
				if (Consts.DEBUG_TYPE_INFERENCE) {
					LOG.debug("Type rejected for {}: candidate={} is wider than current={}", arg, candidateType, currentType);
				}
				return REJECT;
			}
			if (candidateType.containsTypeVariable()) {
				// reject unknown type vars
				ArgType unknownTypeVar = root.getTypeUtils().checkForUnknownTypeVars(updateInfo.getMth(), candidateType);
				if (unknownTypeVar != null) {
					if (Consts.DEBUG_TYPE_INFERENCE) {
						LOG.debug("Type rejected for {}: candidate: '{}' has unknown type var: '{}'", arg, candidateType, unknownTypeVar);
					}
					return REJECT;
				}
			}
		}
		return null;
	}

	private TypeUpdateResult updateTypeForSsaVar(TypeUpdateInfo updateInfo, SSAVar ssaVar, ArgType candidateType) {
		TypeInfo typeInfo = ssaVar.getTypeInfo();
		ArgType immutableType = ssaVar.getImmutableType();
		if (immutableType != null && !Objects.equals(immutableType, candidateType)) {
			if (Consts.DEBUG_TYPE_INFERENCE) {
				LOG.info("Reject change immutable type {} to {} for {}", immutableType, candidateType, ssaVar);
			}
			return REJECT;
		}
		if (!inBounds(updateInfo, ssaVar, typeInfo.getBounds(), candidateType)) {
			return REJECT;
		}
		var updateCallback = new ArgsListUpdateCallback<>(this, updateInfo, ssaVar.getUseList(), candidateType, true);
		updateCallback.setFinalResultCallback(result -> {
			if (result == REJECT) {
				// rollback update for all registers in current SSA var
				updateInfo.rollbackUpdate(ssaVar.getAssign());
				ssaVar.getUseList().forEach(updateInfo::rollbackUpdate);
			}
			return result;
		});
		return queueDirectTypeUpdate(updateInfo, ssaVar.getAssign(), candidateType, updateCallback);
	}

	private TypeUpdateResult requestUpdate(TypeUpdateInfo updateInfo, InsnArg arg, ArgType candidateType) {
		if (updateInfo.isProcessed(arg)) {
			return CHANGED;
		}
		updateInfo.requestUpdate(arg, candidateType);
		InsnNode insn = arg.getParentInsn();
		if (insn == null) {
			return SAME;
		}
		ITypeListener listener = listenerRegistry.get(insn.getType());
		if (listener == null) {
			return CHANGED;
		}
		if (Consts.DEBUG_TYPE_INFERENCE) {
			LOG.debug("Run listener for insn: {}, arg: {}, type: {}", insn.getType(), arg, candidateType);
		}
		return listener.update(updateInfo, insn, arg, candidateType);
	}

	boolean inBounds(Set<ITypeBound> bounds, ArgType candidateType) {
		for (ITypeBound bound : bounds) {
			ArgType boundType = bound.getType();
			if (boundType != null && !checkBound(candidateType, bound, boundType)) {
				return false;
			}
		}
		return true;
	}

	private boolean inBounds(TypeUpdateInfo updateInfo, SSAVar ssaVar, Set<ITypeBound> bounds, ArgType candidateType) {
		for (ITypeBound bound : bounds) {
			ArgType boundType;
			if (updateInfo != null && bound instanceof ITypeBoundDynamic) {
				boundType = ((ITypeBoundDynamic) bound).getType(updateInfo);
			} else {
				boundType = bound.getType();
			}
			if (boundType != null && !checkBound(candidateType, bound, boundType)) {
				if (Consts.DEBUG_TYPE_INFERENCE) {
					LOG.debug("Reject type '{}' for {} by bound: {} from {}", candidateType, ssaVar, boundType, bound);
				}
				return false;
			}
		}
		return true;
	}

	private boolean checkBound(ArgType candidateType, ITypeBound bound, ArgType boundType) {
		TypeCompareEnum compareResult = comparator.compareTypes(candidateType, boundType);
		switch (compareResult) {
			case EQUAL:
				return true;

			case WIDER:
				return bound.getBound() != BoundEnum.USE;

			case NARROW:
				if (bound.getBound() == BoundEnum.ASSIGN) {
					return !boundType.isTypeKnown() && checkAssignForUnknown(boundType, candidateType);
				}
				return true;

			case WIDER_BY_GENERIC:
			case NARROW_BY_GENERIC:
				// allow replace object to same object with known generic type
				// due to incomplete information about external methods and fields
				return true;

			case CONFLICT:
			case CONFLICT_BY_GENERIC:
				return false;

			case UNKNOWN:
				LOG.warn("Can't compare types, unknown hierarchy: {} and {}", candidateType, boundType);
				comparator.compareTypes(candidateType, boundType);
				return true;

			default:
				throw new JadxRuntimeException("Not processed type compare enum: " + compareResult);
		}
	}

	private boolean checkAssignForUnknown(ArgType boundType, ArgType candidateType) {
		if (boundType == ArgType.UNKNOWN) {
			return true;
		}
		boolean candidateArray = candidateType.isArray();
		if (boundType.isArray() && candidateArray) {
			return checkAssignForUnknown(boundType.getArrayElement(), candidateType.getArrayElement());
		}
		if (candidateArray && boundType.contains(PrimitiveType.ARRAY)) {
			return true;
		}
		if (candidateType.isObject() && boundType.contains(PrimitiveType.OBJECT)) {
			return true;
		}
		if (candidateType.isPrimitive() && boundType.contains(candidateType.getPrimitiveType())) {
			return true;
		}
		return false;
	}

	private Map<InsnType, ITypeListener> initListenerRegistry() {
		Map<InsnType, ITypeListener> registry = new EnumMap<>(InsnType.class);
		registry.put(InsnType.CONST, this::sameFirstArgListener);
		registry.put(InsnType.MOVE, this::moveListener);
		registry.put(InsnType.PHI, this::allSameListener);
		registry.put(InsnType.AGET, this::arrayGetListener);
		registry.put(InsnType.APUT, this::arrayPutListener);
		registry.put(InsnType.IF, this::ifListener);
		registry.put(InsnType.ARITH, this::arithListener);
		registry.put(InsnType.NEG, this::suggestAllSameListener);
		registry.put(InsnType.NOT, this::suggestAllSameListener);
		registry.put(InsnType.CHECK_CAST, this::checkCastListener);
		registry.put(InsnType.INVOKE, this::invokeListener);
		registry.put(InsnType.CONSTRUCTOR, this::invokeListener);
		return registry;
	}

	private TypeUpdateResult invokeListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		BaseInvokeNode invoke = (BaseInvokeNode) insn;
		if (isAssign(invoke, arg)) {
			// TODO: implement backward type propagation (from result to instance)
			return SAME;
		}
		if (invoke.getInstanceArg() == arg) {
			IMethodDetails methodDetails = root.getMethodUtils().getMethodDetails(invoke);
			if (methodDetails == null) {
				return SAME;
			}
			TypeUtils typeUtils = root.getTypeUtils();
			Set<ArgType> knownTypeVars = typeUtils.getKnownTypeVarsAtMethod(updateInfo.getMth());
			Map<ArgType, ArgType> typeVarsMap = typeUtils.getTypeVariablesMapping(candidateType);

			ArgType returnType = methodDetails.getReturnType();
			List<ArgType> argTypes = methodDetails.getArgTypes();
			int argsCount = argTypes.size();

			Supplier<ArgType> getReturnType;
			Function<Integer, ArgType> getArgType;
			if (typeVarsMap.isEmpty()) {
				// generics can't be resolved => use as is
				getReturnType = () -> returnType;
				getArgType = argTypes::get;
			} else {
				// resolve types before apply
				getReturnType = () -> typeUtils.replaceTypeVariablesUsingMap(returnType, typeVarsMap);
				getArgType = argNum -> typeUtils.replaceClassGenerics(candidateType, argTypes.get(argNum));
			}
			return new InvokeUpdateCallback(this, updateInfo, invoke, argsCount, knownTypeVars, getReturnType, getArgType)
					.runQueue();
		}
		return SAME;
	}

	private TypeUpdateResult sameFirstArgListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		InsnArg changeArg = isAssign(insn, arg) ? insn.getArg(0) : insn.getResult();
		if (updateInfo.hasUpdateWithType(changeArg, candidateType)) {
			return CHANGED;
		}
		return queueTypeUpdate(updateInfo, changeArg, candidateType, null);
	}

	private TypeUpdateResult moveListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		if (insn.getResult() == null) {
			return CHANGED;
		}
		boolean assignChanged = isAssign(insn, arg);
		InsnArg changeArg = assignChanged ? insn.getArg(0) : insn.getResult();

		// allow result to be wider
		TypeCompareEnum cmp = comparator.compareTypes(candidateType, changeArg.getType());
		boolean correctType = cmp.isEqual() || (assignChanged ? cmp.isWider() : cmp.isNarrow());

		return queueTypeUpdate(updateInfo, changeArg, candidateType, result -> {
			if (result == SAME && !correctType) {
				if (Consts.DEBUG_TYPE_INFERENCE) {
					LOG.debug("Move insn types mismatch: {} -> {}, change arg: {}, insn: {}",
							candidateType, changeArg.getType(), changeArg, insn);
				}
				return REJECT;
			}
			if (result == REJECT && correctType) {
				return CHANGED;
			}
			return result;
		});
	}

	/**
	 * All args must have same types
	 */
	private TypeUpdateResult allSameListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		if (!isAssign(insn, arg)) {
			return queueTypeUpdate(updateInfo, insn.getResult(), candidateType, null);
		}
		// update args with same type
		var updateCallback = new ArgsListUpdateCallback<>(this, updateInfo, insn.getArgList(), candidateType, false);
		updateCallback.setArgsFilter(a -> a != arg);
		return updateCallback.runFirstQueue();
	}

	private TypeUpdateResult arithListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		ArithNode arithInsn = (ArithNode) insn;
		if (candidateType == ArgType.BOOLEAN && arithInsn.getOp().isBitOp()) {
			// force all args to boolean
			return allSameListener(updateInfo, insn, arg, candidateType);
		}
		return suggestAllSameListener(updateInfo, insn, arg, candidateType);
	}

	/**
	 * Try to set candidate type to all args, don't fail on reject
	 */
	private TypeUpdateResult suggestAllSameListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		var updateCallback = new ArgsListUpdateCallback<>(this, updateInfo, insn.getArgList(), candidateType, false);
		updateCallback.setArgsFilter(a -> a != arg);
		updateCallback.setIgnoreReject(true);
		if (!isAssign(insn, arg)) {
			RegisterArg resultArg = insn.getResult();
			if (resultArg != null) {
				// start with result
				return queueTypeUpdate(updateInfo, resultArg, candidateType, updateCallback);
			}
		}
		// start with first arg
		return updateCallback.runFirstQueue();
	}

	private TypeUpdateResult checkCastListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		IndexInsnNode checkCast = (IndexInsnNode) insn;
		if (isAssign(insn, arg)) {
			InsnArg insnArg = insn.getArg(0);
			return queueTypeUpdate(updateInfo, insnArg, candidateType,
					r -> r == REJECT ? SAME : r);
		}
		ArgType castType = (ArgType) checkCast.getIndex();
		TypeCompareEnum res = comparator.compareTypes(candidateType, castType);
		if (res == TypeCompareEnum.CONFLICT) {
			// allow casting one interface to another
			if (!isInterfaces(candidateType, castType)) {
				return REJECT;
			}
		}
		if (res == TypeCompareEnum.CONFLICT_BY_GENERIC) {
			if (!insn.contains(AFlag.SOFT_CAST)) {
				return REJECT;
			}
		}
		if (res == TypeCompareEnum.NARROW_BY_GENERIC && candidateType.containsGeneric()) {
			// propagate generic type to result
			return queueTypeUpdate(updateInfo, checkCast.getResult(), candidateType, null);
		}
		ArgType currentType = checkCast.getArg(0).getType();
		return candidateType.equals(currentType) ? SAME : CHANGED;
	}

	private boolean isInterfaces(ArgType firstType, ArgType secondType) {
		if (!firstType.isObject() || !secondType.isObject()) {
			return false;
		}
		ClspClass firstCls = root.getClsp().getClsDetails(firstType);
		ClspClass secondCls = root.getClsp().getClsDetails(secondType);
		if (firstCls != null && !firstCls.isInterface()) {
			return false;
		}
		if (secondCls != null && !secondCls.isInterface()) {
			return false;
		}
		if (firstCls == null || secondCls == null) {
			return true;
		}
		return secondCls.isInterface() && firstCls.isInterface();
	}

	private TypeUpdateResult arrayGetListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		if (isAssign(insn, arg)) {
			return queueTypeUpdate(updateInfo, insn.getArg(0), ArgType.array(candidateType), result -> {
				if (result == REJECT) {
					ArgType arrType = insn.getArg(0).getType();
					if (arrType.isTypeKnown() && arrType.isArray() && arrType.getArrayElement().isPrimitive()) {
						TypeCompareEnum compResult = comparator.compareTypes(candidateType, arrType.getArrayElement());
						if (compResult == TypeCompareEnum.WIDER) {
							// allow implicit upcast for primitive types (int a = byteArr[n])
							return CHANGED;
						}
					}
				}
				return result;
			});
		}
		InsnArg arrArg = insn.getArg(0);
		if (arrArg == arg) {
			ArgType arrayElement = candidateType.getArrayElement();
			if (arrayElement == null) {
				return REJECT;
			}
			return queueTypeUpdate(updateInfo, insn.getResult(), arrayElement, result -> {
				if (result == REJECT) {
					ArgType resType = insn.getResult().getType();
					if (resType.isTypeKnown() && resType.isPrimitive()) {
						TypeCompareEnum compResult = comparator.compareTypes(resType, arrayElement);
						if (compResult == TypeCompareEnum.WIDER) {
							// allow implicit upcast for primitive types (int a = byteArr[n])
							return CHANGED;
						}
					}
				}
				return result;
			});
		}
		// index argument
		return SAME;
	}

	private TypeUpdateResult arrayPutListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		InsnArg arrArg = insn.getArg(0);
		InsnArg putArg = insn.getArg(2);
		if (arrArg == arg) {
			ArgType arrayElement = candidateType.getArrayElement();
			if (arrayElement == null) {
				return REJECT;
			}
			return queueTypeUpdate(updateInfo, putArg, arrayElement, result -> {
				if (result == REJECT) {
					ArgType putType = putArg.getType();
					if (putType.isTypeKnown()) {
						TypeCompareEnum compResult = comparator.compareTypes(arrayElement, putType);
						if (compResult == TypeCompareEnum.WIDER || compResult == TypeCompareEnum.WIDER_BY_GENERIC) {
							// allow wider result (i.e. allow put any objects in Object[] or byte in int[])
							return CHANGED;
						}
					}
				}
				return result;
			});
		}
		if (arrArg == putArg) {
			return queueTypeUpdate(updateInfo, arrArg, ArgType.array(candidateType), null);
		}
		// index
		return SAME;
	}

	private TypeUpdateResult ifListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		InsnArg firstArg = insn.getArg(0);
		InsnArg secondArg = insn.getArg(1);
		InsnArg updateArg = firstArg == arg ? secondArg : firstArg;
		return queueTypeUpdate(updateInfo, updateArg, candidateType, result -> {
			if (result == REJECT) {
				// soft checks for objects and array - exact type not compared
				ArgType updateArgType = updateArg.getType();
				if (candidateType.isObject() && updateArgType.canBeObject()) {
					return SAME;
				}
				if (candidateType.isArray() && updateArgType.canBeArray()) {
					return SAME;
				}
				if (candidateType.isPrimitive()) {
					if (updateArgType.canBePrimitive(candidateType.getPrimitiveType())) {
						return SAME;
					}
					if (updateArgType.isTypeKnown() && candidateType.getRegCount() == updateArgType.getRegCount()) {
						return SAME;
					}
				}
			}
			return result;
		});
	}

	private static boolean isAssign(InsnNode insn, InsnArg arg) {
		return insn.getResult() == arg;
	}

	public TypeCompare getTypeCompare() {
		return comparator;
	}
}
