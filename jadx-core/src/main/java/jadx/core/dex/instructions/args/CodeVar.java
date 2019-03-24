package jadx.core.dex.instructions.args;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodeVar {
	private String name;
	private ArgType type; // before type inference can be null and set only for immutable types
	private List<SSAVar> ssaVars = new ArrayList<>(3);

	private boolean isFinal;
	private boolean isThis;
	private boolean isDeclared;

	public static CodeVar fromMthArg(RegisterArg mthArg) {
		CodeVar var = new CodeVar();
		var.setType(mthArg.getInitType());
		var.setName(mthArg.getName());
		var.setDeclared(true);
		var.setThis(mthArg.isThis());
		var.setSsaVars(Collections.singletonList(new SSAVar(mthArg.getRegNum(), 0, mthArg)));
		return var;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArgType getType() {
		return type;
	}

	public void setType(ArgType type) {
		this.type = type;
	}

	public List<SSAVar> getSsaVars() {
		return ssaVars;
	}

	public void addSsaVar(SSAVar ssaVar) {
		if (!ssaVars.contains(ssaVar)) {
			ssaVars.add(ssaVar);
		}
	}

	public void setSsaVars(List<SSAVar> ssaVars) {
		this.ssaVars = ssaVars;
	}

	public boolean isFinal() {
		return isFinal;
	}

	public void setFinal(boolean aFinal) {
		isFinal = aFinal;
	}

	public boolean isThis() {
		return isThis;
	}

	public void setThis(boolean aThis) {
		isThis = aThis;
	}

	public boolean isDeclared() {
		return isDeclared;
	}

	public void setDeclared(boolean declared) {
		isDeclared = declared;
	}

	/**
	 * Merge flags with OR operator
	 */
	public void mergeFlagsFrom(CodeVar other) {
		if (other.isDeclared()) {
			setDeclared(true);
		}
		if (other.isThis()) {
			setThis(true);
		}
		if (other.isFinal()) {
			setFinal(true);
		}
	}

	@Override
	public String toString() {
		return (isFinal ? "final " : "") + type + ' ' + name;
	}
}
