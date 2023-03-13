package jadx.core.utils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.deobf.NameMapper;
import jadx.core.deobf.TldHelper;

public class BetterName {
	private static final Logger LOG = LoggerFactory.getLogger(BetterName.class);

	private static final boolean DEBUG = true;

	public static String compareAndGet(String first, String second) {
		if (Objects.equals(first, second)) {
			return first;
		}
		int firstRating = calcRating(first);
		int secondRating = calcRating(second);
		boolean firstBetter = firstRating >= secondRating;
		if (DEBUG) {
			if (firstBetter) {
				LOG.debug("Better name: '{}' > '{}' ({} > {})", first, second, firstRating, secondRating);
			} else {
				LOG.debug("Better name: '{}' > '{}' ({} > {})", second, first, secondRating, firstRating);
			}
		}
		return firstBetter ? first : second;
	}

	public static int calcRating(String str) {
		int rating = str.length() * 3;
		rating += differentCharsCount(str) * 20;

		if (NameMapper.isAllCharsPrintable(str)) {
			rating += 100;
		}
		if (NameMapper.isValidIdentifier(str)) {
			rating += 50;
		}
		if (TldHelper.contains(str)) {
			rating += 20;
		}
		if (str.contains("_")) {
			// rare in obfuscated names
			rating += 100;
		}
		return rating;
	}

	private static int differentCharsCount(String str) {
		String lower = str.toLowerCase(Locale.ROOT);
		Set<Integer> chars = new HashSet<>();
		StringUtils.visitCodePoints(lower, chars::add);
		return chars.size();
	}
}
