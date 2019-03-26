package jadx.core.dex.attributes;

public interface IAttribute {
	AType<? extends IAttribute> getType();
}
