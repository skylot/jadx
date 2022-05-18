package jadx.api.metadata.annotations;

import org.jetbrains.annotations.Nullable;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.MethodNode;

/**
 * Variable info
 */
public class VarNode implements ICodeNodeRef {

	@Nullable
	public static VarNode get(MethodNode mth, RegisterArg reg) {
		SSAVar ssaVar = reg.getSVar();
		if (ssaVar == null) {
			return null;
		}
		return get(mth, ssaVar);
	}

	@Nullable
	public static VarNode get(MethodNode mth, CodeVar codeVar) {
		return get(mth, codeVar.getAnySsaVar());
	}

	@Nullable
	public static VarNode get(MethodNode mth, SSAVar ssaVar) {
		CodeVar codeVar = ssaVar.getCodeVar();
		if (codeVar.isThis()) {
			return null;
		}
		VarNode cachedVarNode = codeVar.getCachedVarNode();
		if (cachedVarNode != null) {
			return cachedVarNode;
		}
		VarNode newVarNode = new VarNode(mth, ssaVar);
		codeVar.setCachedVarNode(newVarNode);
		return newVarNode;
	}

	@Nullable
	public static ICodeAnnotation getRef(MethodNode mth, RegisterArg reg) {
		VarNode varNode = get(mth, reg);
		if (varNode == null) {
			return null;
		}
		return varNode.getVarRef();
	}

	private final MethodNode mth;
	private final int reg;
	private final int ssa;
	private final ArgType type;
	private @Nullable String name;
	private int defPos;

	private final VarRef varRef;

	protected VarNode(MethodNode mth, SSAVar ssaVar) {
		this(mth, ssaVar.getRegNum(), ssaVar.getVersion(),
				ssaVar.getCodeVar().getType(), ssaVar.getCodeVar().getName());
	}

	public VarNode(MethodNode mth, int reg, int ssa, ArgType type, String name) {
		this.mth = mth;
		this.reg = reg;
		this.ssa = ssa;
		this.type = type;
		this.name = name;
		this.varRef = VarRef.fromVarNode(this);
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

	@Nullable
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public VarRef getVarRef() {
		return varRef;
	}

	@Override
	public int getDefPosition() {
		return defPos;
	}

	@Override
	public void setDefPosition(int pos) {
		this.defPos = pos;
	}

	@Override
	public AnnType getAnnType() {
		return AnnType.VAR;
	}

	@Override
	public int hashCode() {
		int h = 31 * getReg() + getSsa();
		return 31 * h + mth.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof VarNode)) {
			return false;
		}
		VarNode other = (VarNode) o;
		return getReg() == other.getReg()
				&& getSsa() == other.getSsa()
				&& getMth().equals(other.getMth());
	}

	@Override
	public String toString() {
		return "VarNode{r" + reg + 'v' + ssa + '}';
	}
}
