package jadx.api.data.annotations;

import jadx.core.dex.attributes.ILineAttributeNode;
import jadx.core.dex.instructions.args.CodeVar;
import jadx.core.dex.nodes.MethodNode;

public class VarDeclareRef extends VarRef implements ILineAttributeNode {

	public static VarDeclareRef get(MethodNode mth, CodeVar codeVar) {
		VarDeclareRef ref = new VarDeclareRef(mth, codeVar);
		codeVar.setCachedVarRef(ref);
		return ref;
	}

	private int sourceLine;
	private int decompiledLine;
	private int defPosition;

	private VarDeclareRef(MethodNode mth, CodeVar codeVar) {
		super(mth, codeVar.getAnySsaVar());
	}

	@Override
	public int getSourceLine() {
		return sourceLine;
	}

	@Override
	public void setSourceLine(int sourceLine) {
		this.sourceLine = sourceLine;
	}

	@Override
	public int getDecompiledLine() {
		return decompiledLine;
	}

	@Override
	public void setDecompiledLine(int decompiledLine) {
		this.decompiledLine = decompiledLine;
	}

	@Override
	public int getDefPosition() {
		return defPosition;
	}

	@Override
	public void setDefPosition(int pos) {
		this.defPosition = pos;
	}

	@Override
	public String toString() {
		return "VarDeclareRef{r" + getReg() + 'v' + getSsa() + '}';
	}
}
