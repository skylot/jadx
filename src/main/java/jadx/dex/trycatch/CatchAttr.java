package jadx.dex.trycatch;

import jadx.dex.attributes.AttributeType;
import jadx.dex.attributes.IAttribute;

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
