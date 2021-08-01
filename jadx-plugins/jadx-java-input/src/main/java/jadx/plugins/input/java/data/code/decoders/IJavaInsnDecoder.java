package jadx.plugins.input.java.data.code.decoders;

import jadx.plugins.input.java.data.code.CodeDecodeState;

public interface IJavaInsnDecoder {
	void decode(CodeDecodeState state);

	default void skip(CodeDecodeState state) {
	}
}
