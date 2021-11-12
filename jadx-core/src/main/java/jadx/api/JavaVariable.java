package jadx.api;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.ApiStatus;

import jadx.api.data.annotations.VarDeclareRef;
import jadx.api.data.annotations.VarRef;

public class JavaVariable implements JavaNode {
	private final JavaMethod mth;
	private final VarRef varRef;

	public JavaVariable(JavaMethod mth, VarRef varRef) {
		this.mth = mth;
		this.varRef = varRef;
	}

	public JavaMethod getMth() {
		return mth;
	}

	public int getReg() {
		return varRef.getReg();
	}

	public int getSsa() {
		return varRef.getSsa();
	}

	@Override
	public String getName() {
		return varRef.getName();
	}

	@ApiStatus.Internal
	public VarRef getVarRef() {
		return varRef;
	}

	@Override
	public String getFullName() {
		return varRef.getType() + " " + varRef.getName() + " (r" + varRef.getReg() + "v" + varRef.getSsa() + ")";
	}

	@Override
	public JavaClass getDeclaringClass() {
		return mth.getDeclaringClass();
	}

	@Override
	public JavaClass getTopParentClass() {
		return mth.getTopParentClass();
	}

	@Override
	public int getDecompiledLine() {
		if (varRef instanceof VarDeclareRef) {
			return ((VarDeclareRef) varRef).getDecompiledLine();
		}
		return 0;
	}

	@Override
	public int getDefPos() {
		if (varRef instanceof VarDeclareRef) {
			return ((VarDeclareRef) varRef).getDefPosition();
		}
		return 0;
	}

	@Override
	public List<JavaNode> getUseIn() {
		return Collections.singletonList(mth);
	}

	@Override
	public int hashCode() {
		return varRef.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof JavaVariable)) {
			return false;
		}
		return varRef.equals(((JavaVariable) o).varRef);
	}
}
