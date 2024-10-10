package jadx.core.utils;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.deobf.NameMapper;

public class BetterName {
	private static final Logger LOG = LoggerFactory.getLogger(BetterName.class);

	private static final boolean DEBUG = false;

	private static final double TOLERANCE = 0.001;

	/**
	 * Compares two class names and returns the "better" one.
	 * If both names are equally good, {@code firstName} is returned.
	 */
	public static String getBetterClassName(String firstName, String secondName) {
		return getBetterName(firstName, secondName);
	}

	/**
	 * Compares two resource names and returns the "better" one.
	 * If both names are equally good, {@code firstName} is returned.
	 */
	public static String getBetterResourceName(String firstName, String secondName) {
		return getBetterName(firstName, secondName);
	}

	private static String getBetterName(String firstName, String secondName) {
		if (Objects.equals(firstName, secondName)) {
			return firstName;
		}

		if (StringUtils.isEmpty(firstName) || StringUtils.isEmpty(secondName)) {
			return StringUtils.notEmpty(firstName)
					? firstName
					: secondName;
		}

		final var firstResult = analyze(firstName);
		final var secondResult = analyze(secondName);

		if (firstResult.digitCount != 0 || secondResult.digitCount != 0) {
			final var firstRatio = (float) firstResult.digitCount / firstResult.length;
			final var secondRatio = (float) secondResult.digitCount / secondResult.length;

			if (Math.abs(secondRatio - firstRatio) >= TOLERANCE) {
				return firstRatio <= secondRatio
						? firstName
						: secondName;
			}
		}

		return firstResult.length >= secondResult.length
				? firstName
				: secondName;
	}

	private static AnalyzeResult analyze(String name) {
		final var result = new AnalyzeResult();

		StringUtils.visitCodePoints(name, cp -> {
			if (Character.isDigit(cp)) {
				result.digitCount++;
			}

			result.length++;
		});

		return result;
	}

	private static class AnalyzeResult {
		private int length;
		private int digitCount;
	}

	/**
	 * @deprecated Use {@link #getBetterClassName(String, String)} or
	 *             {@link #getBetterResourceName(String, String)} instead.
	 */
	@Deprecated
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

	/**
	 * @deprecated This function is an implementation detail of deprecated
	 *             {@link #compareAndGet(String, String)} and should not be used outside tests.
	 */
	@Deprecated
	public static int calcRating(String str) {
		int rating = str.length() * 3;
		rating += differentCharsCount(str) * 20;

		if (NameMapper.isAllCharsPrintable(str)) {
			rating += 100;
		}
		if (NameMapper.isValidIdentifier(str)) {
			rating += 50;
		}
		if (str.contains("_")) {
			// rare in obfuscated names
			rating += 100;
		}
		return rating;
	}

	@Deprecated
	private static int differentCharsCount(String str) {
		String lower = str.toLowerCase(Locale.ROOT);
		Set<Integer> chars = new HashSet<>();
		StringUtils.visitCodePoints(lower, chars::add);
		return chars.size();
	}
}
