package jadx.gui.search;

import java.util.regex.Pattern;

public class SearchManager {
	/**
	 * Generate the good pattern according to the different boolean
	 *
	 * @param exp           the searched expression
	 * @param caseSensitive boolean
	 * @param wholeWord     boolean
	 * @param useRegexp     boolean
	 * @return the pattern
	 */
	public static Pattern generatePattern(String exp, boolean caseSensitive, boolean wholeWord, boolean useRegexp) {
		String word = exp;
		if (word != null && !word.isEmpty()) {
			if (!useRegexp) {
				word = word.replace("\\E", "\\E\\\\E\\Q");
				word = "\\Q" + word + "\\E";
				if (wholeWord && exp.matches("\\b.*\\b")) {
					word = "\\b" + word + "\\b";
				}
			}

			if (!caseSensitive) {
				word = "(?i)" + word;
			}

			if (useRegexp) {
				word = "(?m)" + word;
			}

			return Pattern.compile(word);
		} else {
			return Pattern.compile("");
		}
	}
}
