package jadx.core.dex.trycatch;

import jadx.core.dex.attributes.AttributeType;
import jadx.core.dex.attributes.IAttribute;

public class CatchAttr implements IAttribute {

	private final TryCatchBlock tryBlock;

	public CatchAttr(TryCatchBlock block) {
		this.tryBlock = block;
	}

	@Override
	public AttributeType getType() {
		return AttributeType.CATCH_BLOCK;
	}

	public TryCatchBlock getTryBlock() {
		return tryBlock;
	}

	@Override
	public String toString() {
		return tryBlock.toString();
	}

}
