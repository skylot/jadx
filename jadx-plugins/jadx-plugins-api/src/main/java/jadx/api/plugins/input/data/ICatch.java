package jadx.api.plugins.input.data;

public interface ICatch {
	String[] getTypes();

	int[] getAddresses();

	int getCatchAllAddress();
}
