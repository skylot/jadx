package jadx.core.dex.nodes.parser;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.TypedVar;
import jadx.core.dex.nodes.DexNode;
import jadx.core.utils.InsnUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LocalVar extends RegisterArg {

	private static final Logger LOG = LoggerFactory.getLogger(LocalVar.class);

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
			try {
				ArgType gType = ArgType.generic(sign);
				if (checkSignature(type, sign, gType)) {
					type = gType;
				}
			} catch (Exception e) {
				LOG.error("Can't parse signature for local variable: " + sign, e);
			}
		}
		TypedVar tv = new TypedVar(type);
		tv.setName(name);
		forceSetTypedVar(tv);
	}

	private boolean checkSignature(ArgType type, String sign, ArgType gType) {
		boolean apply;
		ArgType el = gType.getArrayRootElement();
		if (el.isGeneric()) {
			if (!type.getArrayRootElement().getObject().equals(el.getObject())) {
				LOG.warn("Generic type in debug info not equals: {} != {}", type, gType);
			}
			apply = true;
		} else if (el.isGenericType()) {
			apply = true;
		} else {
			LOG.debug("Local var signature from debug info not generic: {}, parsed: {}", sign, gType);
			apply = false;
		}
		return apply;
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
