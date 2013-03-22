package jadx.dex.info;

import jadx.dex.instructions.args.ArgType;
import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.instructions.args.TypedVar;
import jadx.dex.nodes.DexNode;

public class LocalVarInfo extends RegisterArg {

	private boolean isEnd;

	public LocalVarInfo(DexNode dex, int rn, int nameId, int typeId, int signId) {
		super(rn);
		String name = (nameId == DexNode.NO_INDEX ? null : dex.getString(nameId));
		ArgType type = (typeId == DexNode.NO_INDEX ? null : dex.getType(typeId));
		String sign = (signId == DexNode.NO_INDEX ? null : dex.getString(signId));

		init(name, type, sign);
	}

	public LocalVarInfo(DexNode dex, int rn, String name, ArgType type, String sign) {
		super(rn);
		init(name, type, sign);
	}

	private void init(String name, ArgType type, String sign) {
		TypedVar tv = new TypedVar(type);
		tv.setName(name);
		setTypedVar(tv);
	}

	public void start(int addr, int line) {
		this.isEnd = false;
	}

	public void end(int addr, int line) {
		this.isEnd = true;
	}

	public boolean isEnd() {
		return isEnd;
	}
}
