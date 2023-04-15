package jadx.api.plugins.input.data.impl;

import jadx.api.plugins.input.data.ICatch;

import static jadx.api.plugins.input.data.impl.InputUtils.formatOffset;

public class CatchData implements ICatch {
	private final int[] handlers;
	private final String[] types;
	private final int allHandler;

	public CatchData(int[] handlers, String[] types, int allHandler) {
		this.handlers = handlers;
		this.types = types;
		this.allHandler = allHandler;
	}

	@Override
	public int[] getHandlers() {
		return handlers;
	}

	@Override
	public String[] getTypes() {
		return types;
	}

	@Override
	public int getCatchAllHandler() {
		return allHandler;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Catch:");
		int size = types.length;
		for (int i = 0; i < size; i++) {
			sb.append(' ').append(types[i]).append("->").append(formatOffset(handlers[i]));
		}
		if (allHandler != -1) {
			sb.append(" all->").append(formatOffset(allHandler));
		}
		return sb.toString();
	}
}
