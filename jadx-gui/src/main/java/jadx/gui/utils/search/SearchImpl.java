package jadx.gui.utils.search;

import java.util.regex.Matcher;

import org.apache.commons.lang3.StringUtils;

public class SearchImpl {
	
	public static boolean isMatch(StringRef searchArea, final SearchSettings searchSettings) {
		return isMatch(searchArea.toString(), searchSettings);
	}	
	
	public static boolean isMatch(String searchArea, final SearchSettings searchSettings) {
		int pos = find(searchArea, searchSettings);
		if (pos == -1) {
			return false;
		} else {
			return true;
		}
	}
	
	public static int find(StringRef searchArea, final SearchSettings searchSettings) {
		return find(searchArea.toString(), searchSettings);
	}
	
	public static int find(String searchArea, final SearchSettings searchSettings) {
		int pos;
		if (searchSettings.isUseRegex()) {
				Matcher matcher = searchSettings.getPattern().matcher(searchArea);
				if (matcher.find(searchSettings.getStartPos())) {
					pos = matcher.start();
				} else {
					pos = -1;
				}
		} else if(searchSettings.isIgnoreCase()){
			pos = StringUtils.indexOfIgnoreCase(searchArea, searchSettings.getSearchString(), searchSettings.getStartPos());
		} else {
			pos = searchArea.indexOf(searchSettings.getSearchString(), searchSettings.getStartPos());
		}
		return pos;
	}

}
