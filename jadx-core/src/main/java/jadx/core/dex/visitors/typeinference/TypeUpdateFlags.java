package jadx.core.dex.visitors.typeinference;

import org.jetbrains.annotations.NotNull;

public class TypeUpdateFlags {
	private static final int ALLOW_WIDER = 1;
	private static final int IGNORE_SAME = 2;
	private static final int IGNORE_UNKNOWN = 4;

	public static final TypeUpdateFlags FLAGS_EMPTY = build(0);
	public static final TypeUpdateFlags FLAGS_WIDER = build(ALLOW_WIDER);
	public static final TypeUpdateFlags FLAGS_WIDER_IGNORE_SAME = build(ALLOW_WIDER | IGNORE_SAME);
	public static final TypeUpdateFlags FLAGS_WIDER_IGNORE_UNKNOWN = build(ALLOW_WIDER | IGNORE_UNKNOWN);

	private final int flags;

	@NotNull
	private static TypeUpdateFlags build(int flags) {
		return new TypeUpdateFlags(flags);
	}

	private TypeUpdateFlags(int flags) {
		this.flags = flags;
	}

	public boolean isAllowWider() {
		return (flags & ALLOW_WIDER) != 0;
	}

	public boolean isIgnoreSame() {
		return (flags & IGNORE_SAME) != 0;
	}

	public boolean isIgnoreUnknown() {
		return (flags & IGNORE_UNKNOWN) != 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (isAllowWider()) {
			sb.append("ALLOW_WIDER");
		}
		if (isIgnoreSame()) {
			if (sb.length() != 0) {
				sb.append('|');
			}
			sb.append("IGNORE_SAME");
		}
		if (isIgnoreUnknown()) {
			if (sb.length() != 0) {
				sb.append('|');
			}
			sb.append("IGNORE_UNKNOWN");
		}
		return sb.toString();
	}
}
