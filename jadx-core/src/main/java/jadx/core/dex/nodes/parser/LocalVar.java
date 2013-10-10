package jadx.core.dex.nodes.parser;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.TypedVar;
import jadx.core.dex.nodes.DexNode;
import jadx.core.utils.InsnUtils;

final class LocalVar extends RegisterArg {

	private boolean isEnd;

	private int startAddr;
	private int endAddr;

	public LocalVar(DexNode dex, int rn, int nameId, int typeId, int signId) {
		super(rn);
		String name = (nameId == DexNode.NO_INDEX ? null : dex.getString(nameId));
		ArgType type = (typeId == DexNode.NO_INDEX ? null : dex.getType(typeId));
		String sign = (signId == DexNode.NO_INDEX ? null : dex.getString(signId));

		init(name, type, sign);
	}

	public LocalVar(RegisterArg arg) {
		super(arg.getRegNum());
		init(arg.getTypedVar().getName(), arg.getType(), null);
	}

	private void init(String name, ArgType type, String sign) {
		if (sign != null) {
			type = ArgType.generic(sign);
		}
		TypedVar tv = new TypedVar(type);
		tv.setName(name);
		forceSetTypedVar(tv);
	}

	public void start(int addr, int line) {
		this.isEnd = false;
		this.startAddr = addr;
	}

	public void end(int addr, int line) {
		this.isEnd = true;
		this.endAddr = addr;
	}

	public boolean isEnd() {
		return isEnd;
	}

	public int getStartAddr() {
		return startAddr;
	}

	public int getEndAddr() {
		return endAddr;
	}

	@Override
	public String toString() {
		return super.toString() + " " + (isEnd
				? "end: " + InsnUtils.formatOffset(startAddr) + "-" + InsnUtils.formatOffset(endAddr)
				: "active: " + InsnUtils.formatOffset(startAddr));
	}
}
