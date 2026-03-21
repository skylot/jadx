package jadx.core.dex.visitors.typeinference;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.instructions.BaseInvokeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;

import static jadx.core.dex.visitors.typeinference.TypeUpdateResult.CHANGED;
import static jadx.core.dex.visitors.typeinference.TypeUpdateResult.REJECT;
import static jadx.core.dex.visitors.typeinference.TypeUpdateResult.SAME;

public class InvokeUpdateCallback implements ITypeUpdateCallback {
	private final TypeUpdate typeUpdate;
	private final TypeUpdateInfo updateInfo;
	private final BaseInvokeNode invoke;
	private final int argsCount;
	private final Set<ArgType> knownTypeVars;
	private final Supplier<ArgType> getReturnType;
	private final Function<Integer, ArgType> getArgType;

	private boolean isAssign;
	private boolean allSame = true;
	private int currentArg = -1;

	private InsnArg updateArg;
	private ArgType updateType;
	private boolean firstQueue = false;

	public InvokeUpdateCallback(TypeUpdate typeUpdate, TypeUpdateInfo updateInfo, BaseInvokeNode invoke, int argsCount,
			Set<ArgType> knownTypeVars, Supplier<ArgType> getReturnType, Function<Integer, ArgType> getArgType) {
		this.typeUpdate = typeUpdate;
		this.updateInfo = updateInfo;
		this.invoke = invoke;
		this.argsCount = argsCount;
		this.knownTypeVars = knownTypeVars;
		this.getReturnType = getReturnType;
		this.getArgType = getArgType;
	}

	@Override
	public @Nullable TypeUpdateResult updateCallback(TypeUpdateResult result) {
		while (true) {
			switch (result) {
				case CHANGED:
					allSame = false;
					break;

				case REJECT:
					TypeCompareEnum compare = typeUpdate.getTypeCompare().compareTypes(updateType, updateArg.getType());
					if (isAssign ? compare.isWider() : compare.isNarrow()) {
						return REJECT;
					}
					break;
			}
			if (!getNextArg()) {
				return allSame ? SAME : CHANGED;
			}
			ITypeUpdateCallback cb;
			if (firstQueue) {
				cb = this;
				firstQueue = false;
			} else {
				cb = null;
			}
			result = typeUpdate.queueTypeUpdate(updateInfo, updateArg, updateType, cb);
			if (result == null) {
				return null;
			}
		}
	}

	public TypeUpdateResult runQueue() {
		firstQueue = true;
		TypeUpdateResult result = SAME;
		RegisterArg resultArg = invoke.getResult();
		if (resultArg != null && !resultArg.isTypeImmutable()) {
			ArgType returnType = checkType(knownTypeVars, getReturnType.get());
			if (returnType != null) {
				updateArg = resultArg;
				updateType = returnType;
				isAssign = true;
				firstQueue = false;
				result = typeUpdate.queueTypeUpdate(updateInfo, updateArg, updateType, this);
				if (result == null) {
					return null;
				}
			}
		}
		return updateCallback(result);
	}

	private boolean getNextArg() {
		while (true) {
			currentArg++;
			int i = currentArg;
			if (i >= argsCount) {
				return false;
			}
			int argOffset = invoke.getFirstArgOffset();
			InsnArg invokeArg = invoke.getArg(argOffset + i);
			if (!invokeArg.isTypeImmutable()) {
				ArgType argType = checkType(knownTypeVars, getArgType.apply(i));
				if (argType != null) {
					updateArg = invokeArg;
					updateType = argType;
					isAssign = false;
					return true;
				}
			}
		}
	}

	private @Nullable ArgType checkType(Set<ArgType> knownTypeVars, @Nullable ArgType type) {
		if (type == null) {
			return null;
		}
		if (type.isWildcard()) {
			return null;
		}
		if (type.containsTypeVariable()) {
			if (knownTypeVars.isEmpty()) {
				return null;
			}
			Boolean hasUnknown = type.visitTypes(this::isUnknown);
			if (hasUnknown != null) {
				return null;
			}
		}
		return type;
	}

	private @Nullable Boolean isUnknown(ArgType t) {
		if (t.isGenericType() && !knownTypeVars.contains(t)) {
			return Boolean.TRUE;
		}
		return null;
	}
}
