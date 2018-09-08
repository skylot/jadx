package jadx.core.dex.trycatch;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;

public class CatchAttr implements IAttribute {

	private final TryCatchBlock tryBlock;

	public CatchAttr(TryCatchBlock block) {
		this.tryBlock = block;
	}

	@Override
	public AType<CatchAttr> getType() {
		return AType.CATCH_BLOCK;
	}

	public TryCatchBlock getTryBlock() {
		return tryBlock;
	}

	@Override
	public String toString() {
		return tryBlock.toString();
	}
}
