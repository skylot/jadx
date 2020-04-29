package jadx.plugins.input.dex.insns.payloads;

import jadx.api.plugins.input.insns.custom.IArrayPayload;

public class DexArrayPayload implements IArrayPayload {

	private final int size;
	private final int elemSize;
	private final Object data;

	public DexArrayPayload(int size, int elemSize, Object data) {
		this.size = size;
		this.elemSize = elemSize;
		this.data = data;
	}

	@Override
	public int getSize() {
		return size;
	}

	@Override
	public int getElementSize() {
		return elemSize;
	}

	@Override
	public Object getData() {
		return data;
	}
}
