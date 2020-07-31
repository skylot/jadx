package jadx.core.dex.visitors.typeinference;

import java.util.Collections;
import java.util.List;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.SSAVar;

public class TypeSearchVarInfo {
	private final SSAVar var;
	private boolean typeResolved;
	private ArgType currentType;
	private List<ArgType> candidateTypes;
	private int currentIndex = -1;
	private List<ITypeConstraint> constraints;

	public TypeSearchVarInfo(SSAVar var) {
		this.var = var;
	}

	public void markResolved(ArgType type) {
		this.currentType = type;
		this.typeResolved = true;
		this.candidateTypes = Collections.emptyList();
	}

	public void reset() {
		if (typeResolved) {
			return;
		}
		currentIndex = 0;
		currentType = candidateTypes.get(0);
	}

	/**
	 * Switch {@code currentType} to next candidate
	 *
	 * @return true - if this is the first candidate
	 */
	public boolean nextType() {
		if (typeResolved) {
			return false;
		}
		int len = candidateTypes.size();
		currentIndex = (currentIndex + 1) % len;
		currentType = candidateTypes.get(currentIndex);
		return currentIndex == 0;
	}

	public SSAVar getVar() {
		return var;
	}

	public boolean isTypeResolved() {
		return typeResolved;
	}

	public void setTypeResolved(boolean typeResolved) {
		this.typeResolved = typeResolved;
	}

	public ArgType getCurrentType() {
		return currentType;
	}

	public void setCurrentType(ArgType currentType) {
		this.currentType = currentType;
	}

	public List<ArgType> getCandidateTypes() {
		return candidateTypes;
	}

	public void setCandidateTypes(List<ArgType> candidateTypes) {
		this.candidateTypes = candidateTypes;
	}

	public List<ITypeConstraint> getConstraints() {
		return constraints;
	}

	public void setConstraints(List<ITypeConstraint> constraints) {
		this.constraints = constraints;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(var.toShortString());
		if (typeResolved) {
			sb.append(", resolved type: ").append(currentType);
		} else {
			sb.append(", currentType=").append(currentType);
			sb.append(", candidateTypes=").append(candidateTypes);
			sb.append(", constraints=").append(constraints);
		}
		return sb.toString();
	}
}
