package jadx.core.dex.visitors.typeinference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;

/**
 * Slow and memory consuming multi-variable type search algorithm.
 * Used only if fast type propagation is failed for some variables.
 * <p>
 * Stages description:
 * - find all possible candidate types within bounds
 * - build dynamic constraint list for every variable
 * - run search by checking all candidates
 */
public class TypeSearch {
	private static final Logger LOG = LoggerFactory.getLogger(TypeSearch.class);

	private static final int VARS_PROCESS_LIMIT = 5_000;
	private static final int CANDIDATES_COUNT_LIMIT = 10;
	private static final int SEARCH_ITERATION_LIMIT = 1_000_000;

	private final MethodNode mth;
	private final TypeSearchState state;
	private final TypeCompare typeCompare;
	private final TypeUpdate typeUpdate;

	public TypeSearch(MethodNode mth) {
		this.mth = mth;
		this.state = new TypeSearchState(mth);
		this.typeUpdate = mth.root().getTypeUpdate();
		this.typeCompare = typeUpdate.getTypeCompare();
	}

	public boolean run() {
		if (mth.getSVars().size() > VARS_PROCESS_LIMIT) {
			mth.addWarnComment("Multi-variable search skipped. Vars limit reached: " + mth.getSVars().size()
					+ " (expected less than " + VARS_PROCESS_LIMIT + ")");
			return false;
		}
		mth.getSVars().forEach(this::fillTypeCandidates);
		mth.getSVars().forEach(this::collectConstraints);

		// quick search for variables without dependencies
		state.getUnresolvedVars().forEach(this::resolveIndependentVariables);

		boolean searchSuccess;
		List<TypeSearchVarInfo> vars = state.getUnresolvedVars();
		if (vars.isEmpty()) {
			searchSuccess = true;
		} else {
			search(vars);
			searchSuccess = fullCheck(vars);
			if (Consts.DEBUG_TYPE_INFERENCE && !searchSuccess) {
				LOG.debug("Multi-variable search failed in {}", mth);
			}
		}
		if (searchSuccess) {
			return applyResolvedVars();
		}
		return false;
	}

	private boolean applyResolvedVars() {
		List<TypeSearchVarInfo> resolvedVars = state.getResolvedVars();
		List<TypeSearchVarInfo> updatedVars = new ArrayList<>();
		for (TypeSearchVarInfo var : resolvedVars) {
			SSAVar ssaVar = var.getVar();
			ArgType resolvedType = var.getCurrentType();
			if (!resolvedType.isTypeKnown()) {
				// ignore unknown variables
				continue;
			}
			if (resolvedType.equals(ssaVar.getTypeInfo().getType())) {
				// type already set
				continue;
			}
			ssaVar.setType(resolvedType);
			updatedVars.add(var);
		}
		boolean applySuccess = true;
		for (TypeSearchVarInfo var : updatedVars) {
			TypeUpdateResult res = typeUpdate.applyWithWiderIgnSame(mth, var.getVar(), var.getCurrentType());
			if (res == TypeUpdateResult.REJECT) {
				mth.addDebugComment("Multi-variable search result rejected for " + var);
				applySuccess = false;
			}
		}
		return applySuccess;
	}

	private boolean search(List<TypeSearchVarInfo> vars) {
		int len = vars.size();
		if (Consts.DEBUG_TYPE_INFERENCE) {
			LOG.debug("Run search for {} vars: ", len);
			StringBuilder sb = new StringBuilder();
			long count = 1;
			for (TypeSearchVarInfo var : vars) {
				LOG.debug("  {}", var);
				int size = var.getCandidateTypes().size();
				sb.append(" * ").append(size);
				count *= size;
			}
			sb.append(" = ").append(count);
			LOG.debug(" > max iterations count = {}", sb);
		}

		// prepare vars
		for (TypeSearchVarInfo var : vars) {
			var.reset();
		}
		// check all types combinations
		int n = 0;
		int i = 0;
		while (!fullCheck(vars)) {
			TypeSearchVarInfo first = vars.get(i);
			if (first.nextType()) {
				int k = i + 1;
				if (k >= len) {
					return false;
				}
				TypeSearchVarInfo next = vars.get(k);
				while (true) {
					if (next.nextType()) {
						k++;
						if (k >= len) {
							return false;
						}
						next = vars.get(k);
					} else {
						break;
					}
				}
			}
			n++;
			if (n > SEARCH_ITERATION_LIMIT) {
				if (Consts.DEBUG_TYPE_INFERENCE) {
					LOG.debug(" > iterations limit reached: {}", SEARCH_ITERATION_LIMIT);
				}
				return false;
			}
		}
		if (Consts.DEBUG_TYPE_INFERENCE) {
			LOG.debug(" > done after {} iterations", n);
		}
		// mark all vars as resolved
		for (TypeSearchVarInfo var : vars) {
			var.setTypeResolved(true);
		}
		return true;
	}

	private boolean resolveIndependentVariables(TypeSearchVarInfo varInfo) {
		boolean allRelatedVarsResolved = varInfo.getConstraints().stream()
				.flatMap(c -> c.getRelatedVars().stream())
				.allMatch(v -> state.getVarInfo(v).isTypeResolved());
		if (!allRelatedVarsResolved) {
			return false;
		}
		// variable is independent, run single search
		varInfo.reset();
		do {
			if (singleCheck(varInfo)) {
				varInfo.setTypeResolved(true);
				return true;
			}
		} while (!varInfo.nextType());

		return false;
	}

	private boolean fullCheck(List<TypeSearchVarInfo> vars) {
		for (TypeSearchVarInfo var : vars) {
			if (!singleCheck(var)) {
				return false;
			}
		}
		return true;
	}

	private boolean singleCheck(TypeSearchVarInfo var) {
		if (var.isTypeResolved()) {
			return true;
		}
		for (ITypeConstraint constraint : var.getConstraints()) {
			if (!constraint.check(state)) {
				return false;
			}
		}
		return true;
	}

	private void fillTypeCandidates(SSAVar ssaVar) {
		TypeSearchVarInfo varInfo = state.getVarInfo(ssaVar);
		ArgType immutableType = ssaVar.getImmutableType();
		if (immutableType != null) {
			varInfo.markResolved(immutableType);
			return;
		}
		ArgType currentType = ssaVar.getTypeInfo().getType();
		if (currentType.isTypeKnown()) {
			varInfo.markResolved(currentType);
			return;
		}

		Set<ArgType> assigns = new LinkedHashSet<>();
		Set<ArgType> uses = new LinkedHashSet<>();
		Set<ITypeBound> bounds = ssaVar.getTypeInfo().getBounds();
		for (ITypeBound bound : bounds) {
			if (bound.getBound() == BoundEnum.ASSIGN) {
				assigns.add(bound.getType());
			} else {
				uses.add(bound.getType());
			}
		}

		Set<ArgType> candidateTypes = new LinkedHashSet<>();
		addCandidateTypes(bounds, candidateTypes, assigns);
		addCandidateTypes(bounds, candidateTypes, uses);

		for (ArgType assignType : assigns) {
			addCandidateTypes(bounds, candidateTypes, getWiderTypes(assignType));
		}
		for (ArgType useType : uses) {
			addCandidateTypes(bounds, candidateTypes, getNarrowTypes(useType));
		}

		addUsageTypeCandidates(ssaVar, bounds, candidateTypes);

		int size = candidateTypes.size();
		if (size == 0) {
			varInfo.setTypeResolved(true);
			varInfo.setCurrentType(ArgType.UNKNOWN);
			varInfo.setCandidateTypes(Collections.emptyList());
		} else if (size == 1) {
			varInfo.setTypeResolved(true);
			varInfo.setCurrentType(candidateTypes.iterator().next());
			varInfo.setCandidateTypes(Collections.emptyList());
		} else {
			varInfo.setTypeResolved(false);
			varInfo.setCurrentType(ArgType.UNKNOWN);
			ArrayList<ArgType> types = new ArrayList<>(candidateTypes);
			types.sort(typeCompare.getReversedComparator());
			varInfo.setCandidateTypes(Collections.unmodifiableList(types));
		}
	}

	private void addUsageTypeCandidates(SSAVar ssaVar, Set<ITypeBound> bounds, Set<ArgType> candidateTypes) {
		for (RegisterArg useArg : ssaVar.getUseList()) {
			InsnNode parentInsn = useArg.getParentInsn();
			if (parentInsn != null) {
				InsnType insnType = parentInsn.getType();
				if (insnType == InsnType.APUT) {
					ArgType aputType = parentInsn.getArg(2).getType();
					if (aputType.isTypeKnown()) {
						addCandidateType(bounds, candidateTypes, ArgType.array(aputType));
					}
				}
			}
		}
	}

	private void addCandidateTypes(Set<ITypeBound> bounds, Set<ArgType> collectedTypes, Collection<ArgType> candidateTypes) {
		for (ArgType candidateType : candidateTypes) {
			if (addCandidateType(bounds, collectedTypes, candidateType)) {
				return;
			}
		}
	}

	private boolean addCandidateType(Set<ITypeBound> bounds, Set<ArgType> collectedTypes, ArgType candidateType) {
		if (candidateType.isTypeKnown() && typeUpdate.inBounds(bounds, candidateType)) {
			collectedTypes.add(candidateType);
			if (collectedTypes.size() > CANDIDATES_COUNT_LIMIT) {
				return true;
			}
		}
		return false;
	}

	private List<ArgType> getWiderTypes(ArgType type) {
		if (type.isTypeKnown()) {
			if (type.isObject()) {
				Set<String> ancestors = mth.root().getClsp().getSuperTypes(type.getObject());
				return ancestors.stream().map(ArgType::object).collect(Collectors.toList());
			}
		} else {
			return expandUnknownType(type);
		}
		return Collections.emptyList();
	}

	private List<ArgType> getNarrowTypes(ArgType type) {
		if (type.isTypeKnown()) {
			if (type.isObject()) {
				if (type.equals(ArgType.OBJECT)) {
					// a lot of objects to return
					return Collections.singletonList(ArgType.OBJECT);
				}
				List<String> impList = mth.root().getClsp().getImplementations(type.getObject());
				return impList.stream().map(ArgType::object).collect(Collectors.toList());
			}
		} else {
			return expandUnknownType(type);
		}
		return Collections.emptyList();
	}

	private List<ArgType> expandUnknownType(ArgType type) {
		List<ArgType> list = new ArrayList<>();
		for (PrimitiveType possibleType : type.getPossibleTypes()) {
			list.add(ArgType.convertFromPrimitiveType(possibleType));
		}
		return list;
	}

	private void collectConstraints(SSAVar var) {
		TypeSearchVarInfo varInfo = state.getVarInfo(var);
		if (varInfo.isTypeResolved()) {
			varInfo.setConstraints(Collections.emptyList());
			return;
		}
		varInfo.setConstraints(new ArrayList<>());
		addConstraint(varInfo, makeConstraint(var.getAssign()));
		for (RegisterArg regArg : var.getUseList()) {
			addConstraint(varInfo, makeConstraint(regArg));
		}
	}

	private void addConstraint(TypeSearchVarInfo varInfo, ITypeConstraint constraint) {
		if (constraint != null) {
			varInfo.getConstraints().add(constraint);
		}
	}

	@Nullable
	private ITypeConstraint makeConstraint(RegisterArg arg) {
		InsnNode insn = arg.getParentInsn();
		if (insn == null || arg.isTypeImmutable()) {
			return null;
		}
		switch (insn.getType()) {
			case MOVE:
				return makeMoveConstraint(insn, arg);

			case PHI:
				return makePhiConstraint(insn, arg);

			default:
				return null;
		}
	}

	@Nullable
	private ITypeConstraint makeMoveConstraint(InsnNode insn, RegisterArg arg) {
		if (!insn.getArg(0).isRegister()) {
			return null;
		}
		return new AbstractTypeConstraint(insn, arg) {
			@Override
			public boolean check(TypeSearchState state) {
				ArgType resType = state.getArgType(insn.getResult());
				ArgType argType = state.getArgType(insn.getArg(0));
				TypeCompareEnum res = typeCompare.compareTypes(resType, argType);
				return res.isEqual() || res.isWider();
			}
		};
	}

	private ITypeConstraint makePhiConstraint(InsnNode insn, RegisterArg arg) {
		return new AbstractTypeConstraint(insn, arg) {
			@Override
			public boolean check(TypeSearchState state) {
				ArgType resType = state.getArgType(insn.getResult());
				for (InsnArg insnArg : insn.getArguments()) {
					ArgType argType = state.getArgType(insnArg);
					if (!argType.equals(resType)) {
						return false;
					}
				}
				return true;
			}
		};
	}
}
