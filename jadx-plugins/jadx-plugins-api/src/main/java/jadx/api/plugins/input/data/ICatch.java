package jadx.api.plugins.input.data;

public interface ICatch {
	String[] getTypes();

	int[] getHandlers();

	int getCatchAllHandler();
}
