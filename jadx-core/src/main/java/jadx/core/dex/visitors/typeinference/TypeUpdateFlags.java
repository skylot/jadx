package jadx.core.dex.visitors.typeinference;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static jadx.core.dex.visitors.typeinference.TypeUpdateFlags.FlagsEnum.ALLOW_WIDER;
import static jadx.core.dex.visitors.typeinference.TypeUpdateFlags.FlagsEnum.IGNORE_SAME;
import static jadx.core.dex.visitors.typeinference.TypeUpdateFlags.FlagsEnum.IGNORE_UNKNOWN;
import static jadx.core.dex.visitors.typeinference.TypeUpdateFlags.FlagsEnum.KEEP_GENERICS;

public class TypeUpdateFlags {
	enum FlagsEnum {
		ALLOW_WIDER,
		IGNORE_SAME,
		IGNORE_UNKNOWN,
		KEEP_GENERICS,
	}

	static final TypeUpdateFlags FLAGS_EMPTY = build();
	static final TypeUpdateFlags FLAGS_WIDER = build(ALLOW_WIDER);
	static final TypeUpdateFlags FLAGS_WIDER_IGNORE_SAME = build(ALLOW_WIDER, IGNORE_SAME);
	static final TypeUpdateFlags FLAGS_APPLY_DEBUG = build(ALLOW_WIDER, KEEP_GENERICS, IGNORE_UNKNOWN);

	private final Set<FlagsEnum> flags;

	private static TypeUpdateFlags build(FlagsEnum... flags) {
		EnumSet<FlagsEnum> set;
		if (flags.length == 0) {
			set = EnumSet.noneOf(FlagsEnum.class);
		} else {
			set = EnumSet.copyOf(List.of(flags));
		}
		return new TypeUpdateFlags(set);
	}

	private TypeUpdateFlags(Set<FlagsEnum> flags) {
		this.flags = flags;
	}

	public boolean isAllowWider() {
		return flags.contains(ALLOW_WIDER);
	}

	public boolean isIgnoreSame() {
		return flags.contains(IGNORE_SAME);
	}

	public boolean isIgnoreUnknown() {
		return flags.contains(IGNORE_UNKNOWN);
	}

	public boolean isKeepGenerics() {
		return flags.contains(KEEP_GENERICS);
	}

	@Override
	public String toString() {
		return flags.toString();
	}
}
