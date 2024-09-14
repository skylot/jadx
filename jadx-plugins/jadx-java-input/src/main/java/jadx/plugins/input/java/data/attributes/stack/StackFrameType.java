package jadx.plugins.input.java.data.attributes.stack;

import org.jetbrains.annotations.Nullable;

public enum StackFrameType {
	SAME_FRAME(0, 63),
	SAME_LOCALS_1_STACK(64, 127),
	SAME_LOCALS_1_STACK_EXTENDED(247, 247),
	CHOP(248, 250),
	SAME_FRAME_EXTENDED(251, 251),
	APPEND(252, 254),
	FULL(255, 255);

	private final int start;
	private final int end;

	StackFrameType(int start, int end) {
		this.start = start;
		this.end = end;
	}

	private static final StackFrameType[] MAPPING = buildMapping();

	private static StackFrameType[] buildMapping() {
		StackFrameType[] mapping = new StackFrameType[256];
		for (StackFrameType value : StackFrameType.values()) {
			for (int i = value.start; i <= value.end; i++) {
				mapping[i] = value;
			}
		}
		return mapping;
	}

	public static @Nullable StackFrameType getType(int data) {
		return MAPPING[data];
	}
}
