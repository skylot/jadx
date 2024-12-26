package jadx.gui.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface ISearchMethod {
	MatchingPositions find(String input, int start);

	static ISearchMethod build(SearchSettings searchSettings) {
		Pattern pattern = searchSettings.getPattern();
		return (input, start) -> {
			Matcher matcher = pattern.matcher(input);
			if (matcher.find(start)) {
				int startIndex = matcher.start();
				int endIndex = matcher.end();
				return new MatchingPositions(/* lineText, */startIndex, endIndex);
			}
			return null;
		};
	}
}
