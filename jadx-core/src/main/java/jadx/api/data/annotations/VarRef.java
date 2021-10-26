package jadx.api.data.annotations;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.MethodNode;

public class VarRef {

	@Nullable
	public static VarRef get(MethodNode mth, RegisterArg reg) {
		SSAVar ssaVar = reg.getSVar();
		if (ssaVar == null) {
			return null;
		}
		CodeVar codeVar = ssaVar.getCodeVar();
		VarRef cachedVarRef = codeVar.getCachedVarRef();
		if (cachedVarRef != null) {
			if (cachedVarRef.getName() == null) {
				cachedVarRef.setName(codeVar.getName());
			}
			return cachedVarRef;
		}
		VarRef newVarRef = new VarRef(mth, ssaVar);
		codeVar.setCachedVarRef(newVarRef);
		return newVarRef;
	}

	private final MethodNode mth;
	private final int reg;
	private final int ssa;
	private final ArgType type;
	private String name;

	protected VarRef(MethodNode mth, SSAVar ssaVar) {
		this(mth, ssaVar.getRegNum(), ssaVar.getVersion(),
				ssaVar.getCodeVar().getType(), ssaVar.getCodeVar().getName());
	}

	private VarRef(MethodNode mth, int reg, int ssa, ArgType type, String name) {
		this.mth = mth;
		this.reg = reg;
		this.ssa = ssa;
		this.type = type;
		this.name = name;
	}

	public MethodNode getMth() {
		return mth;
	}

	public int getReg() {
		return reg;
	}

	public int getSsa() {
		return ssa;
	}

	public ArgType getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof VarRef)) {
			return false;
		}
		VarRef other = (VarRef) o;
		return getReg() == other.getReg()
				&& getSsa() == other.getSsa()
				&& getMth().equals(other.getMth());
	}

	@Override
	public int hashCode() {
		return 31 * getReg() + getSsa();
	}

	@Override
	public String toString() {
		return "VarUseRef{r" + reg + 'v' + ssa + '}';
	}
}
