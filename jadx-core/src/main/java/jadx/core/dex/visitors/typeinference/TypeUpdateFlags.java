package jadx.core.dex.visitors.typeinference;

public class TypeUpdateFlags {

	public static final TypeUpdateFlags FLAGS_EMPTY = new TypeUpdateFlags(false, false);
	public static final TypeUpdateFlags FLAGS_WIDER = new TypeUpdateFlags(true, false);
	public static final TypeUpdateFlags FLAGS_WIDER_IGNSAME = new TypeUpdateFlags(true, true);

	private final boolean allowWider;
	private final boolean ignoreSame;

	private TypeUpdateFlags(boolean allowWider, boolean ignoreSame) {
		this.allowWider = allowWider;
		this.ignoreSame = ignoreSame;
	}

	public boolean isAllowWider() {
		return allowWider;
	}

	public boolean isIgnoreSame() {
		return ignoreSame;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (allowWider) {
			sb.append("ALLOW_WIDER");
		}
		if (ignoreSame) {
			if (sb.length() != 0) {
				sb.append('|');
			}
			sb.append("IGNORE_SAME");
		}
		return sb.toString();
	}
}
