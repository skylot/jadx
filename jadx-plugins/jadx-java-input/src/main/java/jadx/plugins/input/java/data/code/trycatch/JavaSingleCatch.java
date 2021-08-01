package jadx.plugins.input.java.data.code.trycatch;

import org.jetbrains.annotations.Nullable;

public class JavaSingleCatch {
	private final int handler;
	private final @Nullable String type;

	public JavaSingleCatch(int handler, @Nullable String type) {
		this.handler = handler;
		this.type = type;
	}

	public int getHandler() {
		return handler;
	}

	public @Nullable String getType() {
		return type;
	}
}
