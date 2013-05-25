package jadx.dex.info;

import jadx.dex.instructions.args.ArgType;
import jadx.dex.instructions.args.RegisterArg;
import jadx.dex.instructions.args.TypedVar;
import jadx.dex.nodes.DexNode;
import jadx.utils.InsnUtils;

public class LocalVarInfo extends RegisterArg {

	private boolean isEnd;

	private int startAddr;
	private int endAddr;

	public LocalVarInfo(DexNode dex, int rn, int nameId, int typeId, int signId) {
		super(rn);
		String name = (nameId == DexNode.NO_INDEX ? null : dex.getString(nameId));
		ArgType type = (typeId == DexNode.NO_INDEX ? null : dex.getType(typeId));
		String sign = (signId == DexNode.NO_INDEX ? null : dex.getString(signId));

		init(name, type, sign);
	}

	public LocalVarInfo(RegisterArg arg) {
		super(arg.getRegNum());
		init(arg.getTypedVar().getName(), arg.getType(), null);
	}

	private void init(String name, ArgType type, String sign) {
		if (sign != null) {
			type = ArgType.generic(sign);
		}
		TypedVar tv = new TypedVar(type);
		tv.setName(name);
		setTypedVar(tv);
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
