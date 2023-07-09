package jadx.core.xmlgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
 * This class contains source code form https://github.com/iBotPeaches/Apktool/
 * see:
 * https://github.com/iBotPeaches/Apktool/blob/master/brut.apktool/apktool-lib/src/main/java/brut/
 * androlib/res/xml/ResXmlEncoders.java
 */
public class StringFormattedCheck {

	public static boolean hasMultipleNonPositionalSubstitutions(String str) {
		Duo<List<Integer>, List<Integer>> tuple = findSubstitutions(str, 4);
		return !tuple.m1.isEmpty() && tuple.m1.size() + tuple.m2.size() > 1;
	}

	@SuppressWarnings("checkstyle:ClassTypeParameterName")
	private static class Duo<T1, T2> {
		public final T1 m1;
		public final T2 m2;

		public Duo(T1 t1, T2 t2) {
			this.m1 = t1;
			this.m2 = t2;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			@SuppressWarnings("unchecked")
			final Duo<T1, T2> other = (Duo<T1, T2>) obj;
			if (!Objects.equals(this.m1, other.m1)) {
				return false;
			}
			return Objects.equals(this.m2, other.m2);
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 71 * hash + (this.m1 != null ? this.m1.hashCode() : 0);
			hash = 71 * hash + (this.m2 != null ? this.m2.hashCode() : 0);
			return hash;
		}
	}

	/**
	 * It returns a tuple of:
	 * - a list of offsets of non positional substitutions. non-pos is defined as any "%" which isn't
	 * "%%" nor "%\d+\$"
	 * - a list of offsets of positional substitutions
	 */
	@SuppressWarnings({ "checkstyle:NeedBraces", "checkstyle:EmptyStatement" })
	private static Duo<List<Integer>, List<Integer>> findSubstitutions(String str, int nonPosMax) {
		if (nonPosMax == -1) {
			nonPosMax = Integer.MAX_VALUE;
		}
		int pos;
		int pos2 = 0;
		List<Integer> nonPositional = new ArrayList<>();
		List<Integer> positional = new ArrayList<>();

		if (str == null) {
			return new Duo<>(nonPositional, positional);
		}

		int length = str.length();

		while ((pos = str.indexOf('%', pos2)) != -1) {
			pos2 = pos + 1;
			if (pos2 == length) {
				nonPositional.add(pos);
				break;
			}
			char c = str.charAt(pos2++);
			if (c == '%') {
				continue;
			}
			if (c >= '0' && c <= '9' && pos2 < length) {
				while ((c = str.charAt(pos2++)) >= '0' && c <= '9' && pos2 < length)
					;
				if (c == '$') {
					positional.add(pos);
					continue;
				}
			}

			nonPositional.add(pos);
			if (nonPositional.size() >= nonPosMax) {
				break;
			}
		}

		return new Duo<>(nonPositional, positional);
	}
}
