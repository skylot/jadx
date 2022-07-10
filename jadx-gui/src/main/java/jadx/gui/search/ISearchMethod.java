package jadx.gui.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public interface ISearchMethod {
	int find(String input, String subStr, int start);

	static ISearchMethod build(SearchSettings searchSettings) {
		if (searchSettings.isUseRegex()) {
			Pattern pattern = searchSettings.getPattern();
			return (input, subStr, start) -> {
				Matcher matcher = pattern.matcher(input);
				if (matcher.find(start)) {
					return matcher.start();
				}
				return -1;
			};
		}
		if (searchSettings.isIgnoreCase()) {
			return StringUtils::indexOfIgnoreCase;
		}
		return String::indexOf;
	}
}
