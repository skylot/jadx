package jadx.core.dex.trycatch;

import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.core.dex.attributes.AType;

public class CatchAttr implements IJadxAttribute {

	private final TryCatchBlock tryBlock;

	public CatchAttr(TryCatchBlock block) {
		this.tryBlock = block;
	}

	@Override
	public AType<CatchAttr> getAttrType() {
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
