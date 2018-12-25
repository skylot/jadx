package jadx.core.dex.visitors.typeinference;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.instructions.args.Typed;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxOverflowException;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.visitors.typeinference.TypeUpdateResult.CHANGED;
import static jadx.core.dex.visitors.typeinference.TypeUpdateResult.REJECT;
import static jadx.core.dex.visitors.typeinference.TypeUpdateResult.SAME;

public final class TypeUpdate {
	private static final Logger LOG = LoggerFactory.getLogger(TypeUpdate.class);

	private final TypeUpdateRegistry listenerRegistry;
	private final TypeCompare comparator;

	private ThreadLocal<Boolean> applyDebug = new ThreadLocal<>();

	public TypeUpdate(RootNode root) {
		this.listenerRegistry = initListenerRegistry();
		this.comparator = new TypeCompare(root);
	}

	public TypeUpdateResult applyDebug(SSAVar ssaVar, ArgType candidateType) {
		try {
			applyDebug.set(true);
			return apply(ssaVar, candidateType);
		} finally {
			applyDebug.set(false);
		}
	}

	public TypeUpdateResult apply(SSAVar ssaVar, ArgType candidateType) {
		if (candidateType == null) {
			return REJECT;
		}
		if (!candidateType.isTypeKnown() && ssaVar.getTypeInfo().getType().isTypeKnown()) {
			return REJECT;
		}

		TypeUpdateInfo updateInfo = new TypeUpdateInfo();
		TypeUpdateResult result = updateTypeChecked(updateInfo, ssaVar.getAssign(), candidateType);
		if (result == REJECT) {
			return result;
		}
		Map<InsnArg, ArgType> updates = updateInfo.getUpdates();
		if (updates.isEmpty()) {
			return SAME;
		}
		updates.forEach(Typed::setType);
		return CHANGED;
	}

	private TypeUpdateResult updateTypeChecked(TypeUpdateInfo updateInfo, InsnArg arg, ArgType candidateType) {
		if (candidateType == null) {
			LOG.warn("Reject null type update, arg: {}, info: {}", arg, updateInfo, new RuntimeException());
			return REJECT;
		}
		ArgType currentType = arg.getType();
		if (Objects.equals(currentType, candidateType)) {
			return SAME;
		}
		if (arg.isTypeImmutable() && currentType != ArgType.UNKNOWN) {
			return REJECT;
		}
		TypeCompareEnum compareResult = comparator.compareTypes(candidateType, currentType);
		if (compareResult == TypeCompareEnum.CONFLICT) {
			return REJECT;
		}
		if (compareResult == TypeCompareEnum.WIDER || compareResult == TypeCompareEnum.WIDER_BY_GENERIC) {
			// allow wider types for apply from debug info
			if (applyDebug.get() != Boolean.TRUE) {
				return REJECT;
			}
		}
		if (arg instanceof RegisterArg) {
			RegisterArg reg = (RegisterArg) arg;
			return updateTypeForSsaVar(updateInfo, reg.getSVar(), candidateType);
		}
		return requestUpdate(updateInfo, arg, candidateType);
	}

	private TypeUpdateResult updateTypeForSsaVar(TypeUpdateInfo updateInfo, SSAVar ssaVar, ArgType candidateType) {
		TypeInfo typeInfo = ssaVar.getTypeInfo();
		if (!inBounds(typeInfo.getBounds(), candidateType)) {
			if (Consts.DEBUG && LOG.isDebugEnabled()) {
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
		try {
			return runListeners(updateInfo, arg, candidateType);
		} catch (StackOverflowError overflow) {
			throw new JadxOverflowException("Type update terminated with stack overflow, arg: " + arg);
		}
	}

	private TypeUpdateResult runListeners(TypeUpdateInfo updateInfo, InsnArg arg, ArgType candidateType) {
		InsnNode insn = arg.getParentInsn();
		if (insn == null) {
			return SAME;
		}
		List<ITypeListener> listeners = listenerRegistry.getListenersForInsn(insn.getType());
		if (listeners.isEmpty()) {
			return CHANGED;
		}
		boolean allSame = true;
		for (ITypeListener listener : listeners) {
			TypeUpdateResult updateResult = listener.update(updateInfo, insn, arg, candidateType);
			if (updateResult == REJECT) {
				return REJECT;
			}
			if (updateResult != SAME) {
				allSame = false;
			}
		}
		return allSame ? SAME : CHANGED;
	}

	private boolean inBounds(Set<ITypeBound> bounds, ArgType candidateType) {
		for (ITypeBound bound : bounds) {
			ArgType boundType = bound.getType();
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

	private TypeUpdateRegistry initListenerRegistry() {
		TypeUpdateRegistry registry = new TypeUpdateRegistry();
		registry.add(InsnType.CONST, this::sameFirstArgListener);
		registry.add(InsnType.MOVE, this::sameFirstArgListener);
		registry.add(InsnType.PHI, this::allSameListener);
		registry.add(InsnType.MERGE, this::allSameListener);
		registry.add(InsnType.AGET, this::arrayGetListener);
		registry.add(InsnType.APUT, this::arrayPutListener);
		registry.add(InsnType.IF, this::ifListener);
		registry.add(InsnType.ARITH, this::suggestAllSameListener);
		registry.add(InsnType.NEG, this::suggestAllSameListener);
		registry.add(InsnType.NOT, this::suggestAllSameListener);
		return registry;
	}

	private TypeUpdateResult sameFirstArgListener(TypeUpdateInfo updateInfo, InsnNode insn, InsnArg arg, ArgType candidateType) {
		InsnArg changeArg = isAssign(insn, arg) ? insn.getArg(0) : insn.getResult();
		return updateTypeChecked(updateInfo, changeArg, candidateType);
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
			return updateTypeChecked(updateInfo, putArg, arrayElement);
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

	public Comparator<ArgType> getArgTypeComparator() {
		return comparator.getComparator();
	}
}
