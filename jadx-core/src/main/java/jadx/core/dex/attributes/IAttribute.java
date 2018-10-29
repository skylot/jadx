package jadx.core.dex.attributes;

public interface IAttribute {
	<T extends IAttribute> AType<T> getType();
}
