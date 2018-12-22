package jadx.core.dex.visitors.typeinference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.ConstInlineVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.ssa.SSATransform;

@JadxVisitor(
		name = "Type Inference",
		desc = "Calculate best types for registers",
		runAfter = {
				SSATransform.class,
				ConstInlineVisitor.class
		}
)
public final class TypeInferenceVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(TypeInferenceVisitor.class);

	private TypeUpdate typeUpdate;

	@Override
	public void init(RootNode root) {
		typeUpdate = root.getTypeUpdate();
	}

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		// collect initial types from assign and usages
		mth.getSVars().forEach(this::attachBounds);
		// start initial type changing
		mth.getSVars().forEach(this::setBestType);

		// try all possible types if var type is still unknown
		mth.getSVars().forEach(var -> {
			TypeInfo typeInfo = var.getTypeInfo();
			ArgType type = typeInfo.getType();
			if (type != null && !type.isTypeKnown()) {
				boolean changed = tryAllTypes(var, type);
				if (!changed) {
					mth.addComment("JADX WARNING: type inference failed for: " + var.getDetailedVarInfo(mth));
				}
			}
		});
	}

	private void setBestType(SSAVar ssaVar) {
		try {
			RegisterArg assignArg = ssaVar.getAssign();
			if (assignArg.isTypeImmutable()) {
				ArgType initType = assignArg.getInitType();
				TypeUpdateResult result = typeUpdate.apply(ssaVar, initType);
				if (Consts.DEBUG && result == TypeUpdateResult.REJECT && LOG.isDebugEnabled()) {
					LOG.debug("Initial immutable type set rejected: {} -> {}", ssaVar, initType);
				}
			} else {
				calculateFromBounds(ssaVar);
			}
		} catch (Exception e) {
			LOG.error("Failed to calculate best type for var: {}", ssaVar);
		}
	}

	private void calculateFromBounds(SSAVar ssaVar) {
		TypeInfo typeInfo = ssaVar.getTypeInfo();
		Set<ITypeBound> bounds = typeInfo.getBounds();
		Optional<ArgType> bestTypeOpt = selectBestTypeFromBounds(bounds);
		if (bestTypeOpt.isPresent()) {
			ArgType candidateType = bestTypeOpt.get();
			TypeUpdateResult result = typeUpdate.apply(ssaVar, candidateType);
			if (Consts.DEBUG && result == TypeUpdateResult.REJECT && LOG.isDebugEnabled()) {
				if (ssaVar.getTypeInfo().getType().equals(candidateType)) {
					LOG.warn("Same type rejected: {} -> {}, bounds: {}", ssaVar, candidateType, bounds);
				} else {
					LOG.debug("Type set rejected: {} -> {}, bounds: {}", ssaVar, candidateType, bounds);
				}
			}
		} else if (!bounds.isEmpty()) {
			LOG.warn("Failed to select best type from bounds: ");
			for (ITypeBound bound : bounds) {
				LOG.warn("  {}", bound);
			}
		}
	}

	private Optional<ArgType> selectBestTypeFromBounds(Set<ITypeBound> bounds) {
		return bounds.stream()
				.map(ITypeBound::getType)
				.filter(Objects::nonNull)
				.max(typeUpdate.getArgTypeComparator());
	}

	private void attachBounds(SSAVar var) {
		TypeInfo typeInfo = var.getTypeInfo();
		RegisterArg assign = var.getAssign();
		addBound(typeInfo, makeAssignBound(assign));

		for (RegisterArg regArg : var.getUseList()) {
			addBound(typeInfo, makeUseBound(regArg));
		}
	}

	private void addBound(TypeInfo typeInfo, ITypeBound bound) {
		if (bound != null && bound.getType() != ArgType.UNKNOWN) {
			typeInfo.getBounds().add(bound);
		}
	}

	private ITypeBound makeAssignBound(RegisterArg assign) {
		InsnNode insn = assign.getParentInsn();
		if (insn == null || assign.isTypeImmutable()) {
			return new TypeBoundConst(BoundEnum.ASSIGN, assign.getInitType());
		}
		switch (insn.getType()) {
			case NEW_INSTANCE:
				ArgType clsType = (ArgType) ((IndexInsnNode) insn).getIndex();
				return new TypeBoundConst(BoundEnum.ASSIGN, clsType);

			case CONST:
				LiteralArg constLit = (LiteralArg) insn.getArg(0);
				return new TypeBoundConst(BoundEnum.ASSIGN, constLit.getType());

			default:
				ArgType type = insn.getResult().getInitType();
				return new TypeBoundConst(BoundEnum.ASSIGN, type);
		}
	}

	@Nullable
	private ITypeBound makeUseBound(RegisterArg regArg) {
		InsnNode insn = regArg.getParentInsn();
		if (insn == null) {
			return null;
		}
		return new TypeBoundConst(BoundEnum.USE, regArg.getInitType());
	}

	private boolean tryAllTypes(SSAVar var, ArgType type) {
		List<ArgType> types = makePossibleTypesList(type);
		for (ArgType candidateType : types) {
			TypeUpdateResult result = typeUpdate.apply(var, candidateType);
			if (result == TypeUpdateResult.CHANGED) {
				return true;
			}
		}
		return false;
	}

	private List<ArgType> makePossibleTypesList(ArgType type) {
		List<ArgType> list = new ArrayList<>();
		if (type.isArray()) {
			for (ArgType arrElemType : makePossibleTypesList(type.getArrayElement())) {
				list.add(ArgType.array(arrElemType));
			}
		}
		for (PrimitiveType possibleType : type.getPossibleTypes()) {
			list.add(ArgType.convertFromPrimitiveType(possibleType));
		}
		return list;
	}
}
