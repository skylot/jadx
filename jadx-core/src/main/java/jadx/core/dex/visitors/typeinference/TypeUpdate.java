package jadx.core.dex.visitors.typeinference;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.TypeUtils;
import jadx.core.utils.exceptions.JadxOverflowException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.visitors.typeinference.TypeUpdateResult.CHANGED;
import static jadx.core.dex.visitors.typeinference.TypeUpdateResult.REJECT;
import static jadx.core.dex.visitors.typeinference.TypeUpdateResult.SAME;

public final class TypeUpdate {
	private static final Logger LOG = LoggerFactory.getLogger(TypeUpdate.class);

	private final RootNode root;
	private final Map<InsnType, ITypeListener> listenerRegistry;
	private final TypeCompare comparator;

	public TypeUpdate(RootNode root) {
		this.root = root;
		this.listenerRegistry = initListenerRegistry();
		this.comparator = new TypeCompare(root);
	}

	/**
	 * Perform recursive type checking and type propagation for all related variables
	 */
	public TypeUpdateResult apply(SSAVar ssaVar, ArgType candidateType) {
		return apply(ssaVar, candidateType, TypeUpdateFlags.FLAGS_EMPTY);
	}

	/**
	 * Allow wider types for apply from debug info and some special cases
	 */
	public TypeUpdateResult applyWithWiderAllow(SSAVar ssaVar, ArgType candidateType) {
		return apply(ssaVar, candidateType, TypeUpdateFlags.FLAGS_WIDER);
	}

	/**
	 * Force type setting
	 */
	public TypeUpdateResult applyWithWiderIgnSame(SSAVar ssaVar, ArgType candidateType) {
		return apply(ssaVar, candidateType, TypeUpdateFlags.FLAGS_WIDER_IGNSAME);
	}

	private TypeUpdateResult apply(SSAVar ssaVar, ArgType candidateType, TypeUpdateFlags flags) {
		if (candidateType == null || !candidateType.isTypeKnown()) {
			return REJECT;
		}

		TypeUpdateInfo updateInfo = new TypeUpdateInfo(flags);
		TypeUpdateResult result = updateTypeChecked(updateInfo, ssaVar.getAssign(), candidateType);
		if (result == REJECT) {
			return result;
		}
		List<TypeUpdateEntry> updates = updateInfo.getUpdates();
		if (updates.isEmpty()) {
			return SAME;
		}
		if (Consts.DEBUG) {
			LOG.debug("Applying types, init for {} -> {}", ssaVar, candidateType);
			updates.forEach(updateEntry -> LOG.debug("  {} -> {}", updateEntry.getType(), updateEntry.getArg()));
		}
		updateInfo.applyUpdates();
		return CHANGED;
	}

	private TypeUpdateResult updateTypeChecked(TypeUpdateInfo updateInfo, InsnArg arg, ArgType candidateType) {
		if (candidateType == null) {
			throw new JadxRuntimeException("Null type update for arg: " + arg);
		}
		ArgType currentType = arg.getType();
		if (Objects.equals(currentType, candidateType) && !updateInfo.getFlags().isIgnoreSame()) {
			return SAME;
		}
		TypeCompareEnum compareResult = comparator.compareTypes(candidateType, currentType);
		if (arg.isTypeImmutable() && currentType != ArgType.UNKNOWN) {
			// don't changed type
			if (compareResult == TypeCompareEnum.CONFLICT) {
				if (Consts.DEBUG) {
					LOG.debug("Type rejected for {} due to conflict: candidate={}, current={}", arg, candidateType, currentType);
				}
				return REJECT;
			}
			return SAME;
		}
		if (compareResult.isWider() && !updateInfo.getFlags().isAllowWider()) {
			if (Consts.DEBUG) {
				LOG.debug("Type rejected for {}: candidate={} is wider than current={}", arg, candidateType, currentType);
			}
			return REJECT;
		}
		if (arg instanceof RegisterArg) {
			RegisterArg reg = (RegisterArg) arg;
			return updateTypeForSsaVar(updateInfo, reg.getSVar(), candidateType);
		}
		return requestUpdate(updateInfo, arg, candidateType);
	}

	private TypeUpdateResult updateTypeForSsaVar(TypeUpdateInfo updateInfo, SSAVar ssaVar, ArgType candidateType) {
		TypeInfo typeInfo = ssaVar.getTypeInfo();
		ArgType immutableType = ssaVar.getImmutableType();
		if (immutableType != null && !Objects.equals(immutableType, candidateType)) {
			if (Consts.DEBUG) {
				LOG.info("Reject change immutable type {} to {} for {}", immutableType, candidateType, ssaVar);
			}
			return REJECT;
		}
		if (!inBounds(updateInfo, typeInfo.getBounds(), candidateType)) {
			if (Consts.DEBUG) {
				LOG.debug("Reject type '{}' for {} by bounds: {}", candidateType, ssaVar, typeInfo.getBounds());
			}
			return REJECT;
		}
		return requestUpdateForSsaVar(updateInfo, ssaVar, candidateType);
	}

	@NotNull
	private TypeUpdateResult requestUpdateForSsaVar(TypeUpdateInfo updateInfo, SSAVar ssaVar, ArgType candidateType) {
		boolean allSame = true;
		TypeUpdateResult result = requestUpdate(updateInfo, ssaVar.getAssign(), candidateType);
		if (result == REJECT) {
			return result;
		}
		List<RegisterArg> useList = ssaVar.getUseList();
		for (RegisterArg arg : useList) {
			TypeUpdateResult useResult = requestUpdate(updateInfo, arg, candidateType);
			if (useResult == REJECT) {
				return REJECT;
			}
			if (useResult != SAME) {
				allSame = false;
			}
		}
		return allSame ? SAME : CHANGED;
	}

	private TypeUpdateResult requestUpdate(TypeUpdateInfo updateInfo, InsnArg arg, ArgType candidateType) {
		if (updateInfo.isProcessed(arg)) {
			return CHANGED;
		}
		updateInfo.requestUpdate(arg, candidateType);
		if (updateInfo.getUpdates().size() > 500) {
			if (Consts.DEBUG) {
				LOG.error("Type update error: too deep update tree");
			}
			return REJECT;
		}
		try {
			TypeUpdateResult result = runListeners(updateInfo, arg, candidateType);
			if (result == REJECT) {
				updateInfo.rollbackUpdate(arg);
			}
			return result;
		} catch (StackOverflowError | BootstrapMethodError error) {
			throw new JadxOverflowException("Type update terminated with stack overflow, arg: " + arg);
		}
	}

	private TypeUpdateResult runListeners(TypeUpdateInfo updateInfo, InsnArg arg, ArgType candidateType) {
		InsnNode insn = arg.getParentInsn();
		if (insn == null) {
			return SAME;
		}
		ITypeListener listener = listenerRegistry.get(insn.getType());
		if (listener == null) {
			return CHANGED;
		}
		return listener.update(updateInfo, insn, arg, candidateType);
	}

	boolean inBounds(Set<ITypeBound> bounds, ArgType candidateType) {
		return inBounds(null, bounds, candidateType);
	}

	private boolean inBounds(@Nullable TypeUpdateInfo updateInfo, Set<ITypeBound> bounds, ArgType candidateType) {
		for (ITypeBound bound : bounds) {
			ArgType boundType;
			if (updateInfo != null && bound instanceof ITypeBoundDynamic) {
				boundType = ((ITypeBoundDynamic) bound).getType(updateInfo);
			} else {
				boundType = bound.getType();
			}
			if (boundType != null && !checkBound(candidateType, bound, boundType)) {
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
				return false;

			case UNKNOWN:
				LOG.warn("Can't compare types, unknown hierarchy: {} and {}", candidateType, boundType);
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
		registry.put(InsnType.ARITH, this::suggestAllSameListener);
		registry.put(InsnType.NEG, this::suggestAllSameListener);
		registry.put(InsnType.NOT, this::suggestAllSameListener);
		registry.put(InsnType.CHECK_CAST, this::checkCastListener);
		registry.put(InsnType.INVOKE, this::invokeListener);
		return registry;
	}

	private TypeUpdateResult invokeListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		if (insn.getResult() == null) {
			return SAME;
		}
		if (candidateType.isGeneric() || candidateType.isGenericType()) {
			InvokeNode invokeNode = (InvokeNode) insn;
			MethodInfo callMth = invokeNode.getCallMth();
			if (isAssign(insn, arg)) {
				// TODO: implement backward type propagation (from result to instance)
				return SAME;
			} else {
				ArgType returnType = root.getMethodGenericReturnType(callMth);
				if (returnType == null) {
					return SAME;
				}
				ArgType resultGeneric = TypeUtils.replaceClassGenerics(root, candidateType, returnType);
				if (resultGeneric == null) {
					return SAME;
				}
				return updateTypeChecked(updateInfo, insn.getResult(), resultGeneric);
			}
		}
		return SAME;
	}

	private TypeUpdateResult sameFirstArgListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		InsnArg changeArg = isAssign(insn, arg) ? insn.getArg(0) : insn.getResult();
		return updateTypeChecked(updateInfo, changeArg, candidateType);
	}

	private TypeUpdateResult moveListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		boolean assignChanged = isAssign(insn, arg);
		InsnArg changeArg = assignChanged ? insn.getArg(0) : insn.getResult();

		boolean correctType;
		if (changeArg.getType().isTypeKnown()) {
			// allow result to be wider
			TypeCompareEnum cmp = comparator.compareTypes(candidateType, changeArg.getType());
			correctType = cmp.isEqual() || (assignChanged ? cmp.isWider() : cmp.isNarrow());
		} else {
			correctType = true;
		}

		TypeUpdateResult result = updateTypeChecked(updateInfo, changeArg, candidateType);
		if (result == SAME && !correctType) {
			if (Consts.DEBUG) {
				LOG.debug("Move insn types mismatch: {} -> {}, change arg: {}, insn: {}",
						candidateType, changeArg.getType(), changeArg, insn);
			}
			return REJECT;
		}
		if (result == REJECT && correctType) {
			return CHANGED;
		}
		return result;
	}

	/**
	 * All args must have same types
	 */
	private TypeUpdateResult allSameListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		if (!isAssign(insn, arg)) {
			return updateTypeChecked(updateInfo, insn.getResult(), candidateType);
		}
		boolean allSame = true;
		for (InsnArg insnArg : insn.getArguments()) {
			if (insnArg != arg) {
				TypeUpdateResult result = updateTypeChecked(updateInfo, insnArg, candidateType);
				if (result == REJECT) {
					return result;
				}
				if (result != SAME) {
					allSame = false;
				}
			}
		}
		return allSame ? SAME : CHANGED;
	}

	/**
	 * Try to set candidate type to all args, don't fail on reject
	 */
	private TypeUpdateResult suggestAllSameListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		if (!isAssign(insn, arg)) {
			updateTypeChecked(updateInfo, insn.getResult(), candidateType);
		}
		boolean allSame = true;
		for (InsnArg insnArg : insn.getArguments()) {
			if (insnArg != arg) {
				TypeUpdateResult result = updateTypeChecked(updateInfo, insnArg, candidateType);
				if (result == REJECT) {
					// ignore
				} else if (result != SAME) {
					allSame = false;
				}
			}
		}
		return allSame ? SAME : CHANGED;
	}

	private TypeUpdateResult checkCastListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		if (!isAssign(insn, arg)) {
			return SAME;
		}
		InsnArg insnArg = insn.getArg(0);
		TypeUpdateResult result = updateTypeChecked(updateInfo, insnArg, candidateType);
		return result == REJECT ? SAME : result;
	}

	private TypeUpdateResult arrayGetListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		if (isAssign(insn, arg)) {
			return updateTypeChecked(updateInfo, insn.getArg(0), ArgType.array(candidateType));
		}
		InsnArg arrArg = insn.getArg(0);
		if (arrArg == arg) {
			ArgType arrayElement = candidateType.getArrayElement();
			if (arrayElement == null) {
				return REJECT;
			}
			return updateTypeChecked(updateInfo, insn.getResult(), arrayElement);
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
			TypeUpdateResult result = updateTypeChecked(updateInfo, putArg, arrayElement);
			if (result == REJECT) {
				ArgType putType = putArg.getType();
				if (putType.isTypeKnown() && putType.isObject()) {
					TypeCompareEnum compResult = comparator.compareTypes(arrayElement, putType);
					if (compResult == TypeCompareEnum.WIDER || compResult == TypeCompareEnum.WIDER_BY_GENERIC) {
						// allow wider result (i.e allow put in Object[] any objects)
						return CHANGED;
					}
				}
			}
			return result;
		}
		if (arrArg == putArg) {
			return updateTypeChecked(updateInfo, arrArg, ArgType.array(candidateType));
		}
		// index
		return SAME;
	}

	private TypeUpdateResult ifListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		InsnArg firstArg = insn.getArg(0);
		InsnArg secondArg = insn.getArg(1);
		InsnArg updateArg = firstArg == arg ? secondArg : firstArg;
		TypeUpdateResult result = updateTypeChecked(updateInfo, updateArg, candidateType);
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
	}

	private static boolean isAssign(InsnNode insn, InsnArg arg) {
		return insn.getResult() == arg;
	}

	public TypeCompare getTypeCompare() {
		return comparator;
	}
}
