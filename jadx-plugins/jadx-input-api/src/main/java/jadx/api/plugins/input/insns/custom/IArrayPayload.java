package jadx.api.plugins.input.insns.custom;

public interface IArrayPayload extends ICustomPayload {
	int getSize();

	int getElementSize();

	Object getData();
}
