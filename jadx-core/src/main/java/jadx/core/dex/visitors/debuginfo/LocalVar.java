package jadx.core.dex.visitors.debuginfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.DexNode;
import jadx.core.utils.InsnUtils;

public final class LocalVar {
	private static final Logger LOG = LoggerFactory.getLogger(LocalVar.class);

	private final int regNum;
	private final String name;
	private final ArgType type;

	private boolean isEnd;
	private int startAddr;
	private int endAddr;

	public LocalVar(DexNode dex, int rn, int nameId, int typeId, int signId) {
		this(rn, dex.getString(nameId), dex.getType(typeId), dex.getString(signId));
	}

	public LocalVar(int regNum, String name, ArgType type) {
		this(regNum, name, type, null);
	}

	public LocalVar(int regNum, String name, ArgType type, String sign) {
		this.regNum = regNum;
		this.name = name;
		if (sign != null) {
			try {
				ArgType gType = ArgType.generic(sign);
				if (checkSignature(type, gType)) {
					type = gType;
				}
			} catch (Exception e) {
				LOG.error("Can't parse signature for local variable: {}", sign, e);
			}
		}
		this.type = type;
	}

	private boolean checkSignature(ArgType type, ArgType gType) {
		boolean apply;
		ArgType el = gType.getArrayRootElement();
		if (el.isGeneric()) {
			if (!type.getArrayRootElement().getObject().equals(el.getObject())) {
				LOG.warn("Generic type in debug info not equals: {} != {}", type, gType);
			}
			apply = true;
		} else {
			apply = el.isGenericType();
		}
		return apply;
	}

	public void start(int addr) {
		this.isEnd = false;
		this.startAddr = addr;
	}

	/**
	 * Sets end address of local variable
	 *
	 * @param addr address
	 * @return <b>true</b> if local variable was active, else <b>false</b>
	 */
	public boolean end(int addr) {
		if (isEnd) {
			return false;
		}
		this.isEnd = true;
		this.endAddr = addr;
		return true;
	}

	public int getRegNum() {
		return regNum;
	}

	public String getName() {
		return name;
	}

	public ArgType getType() {
		return type;
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
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public String toString() {
		return InsnUtils.formatOffset(startAddr)
				+ "-" + (isEnd ? InsnUtils.formatOffset(endAddr) : "     ")
				+ ": r" + regNum + " '" + name + "' " + type;
	}
}
