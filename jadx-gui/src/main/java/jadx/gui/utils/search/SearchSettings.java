package jadx.gui.utils.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchSettings {

	private static final Logger LOG = LoggerFactory.getLogger(SearchSettings.class);

	private final String searchString;

	private final boolean useRegex;

	private final boolean ignoreCase;

	private Pattern regexPattern;

	private int startPos = 0;

	
	public SearchSettings(String searchString, boolean useRegex, boolean ignoreCase) {
		this.searchString = searchString;
		this.useRegex = useRegex;
		this.ignoreCase = ignoreCase;
	}
	
	/*
	 * Return whether Regex search should be done
	 */
	public boolean isUseRegex() {
		return this.useRegex;
	}

	/*
	 * Return whether case will be ignored
	 */
	public boolean isIgnoreCase() {
		return this.ignoreCase;
	}

	/*
	 * Return search string
	 */
	public String getSearchString() {
		return this.searchString;
	}

	/*
	 * Return the starting index
	 */
	public int getStartPos() {
		return this.startPos;
	}

	/*
	 * Set Starting Index
	 */
	public void setStartPos(int startPos) {
		this.startPos = startPos;
	}

	/*
	 * get Regex Pattern
	 */
	public Pattern getPattern() {
		return this.regexPattern;
	}

	/*
	 * Runs Pattern.compile if using Regex. If not using Regex return true
	 * return false is invalid Regex
	 */
	public boolean preCompile() {
		try {
			if (this.useRegex && this.ignoreCase) {
				this.regexPattern = Pattern.compile(this.searchString, Pattern.CASE_INSENSITIVE);
			} else if (this.useRegex) {
				this.regexPattern = Pattern.compile(this.searchString);
			}
		} catch (Exception e) {
			LOG.warn("Invalid Regex: {}", this.searchString);
			return false;
		}
		return true;
	}
	
	/*
	 * Checks if searchArea matches the searched string found in searchSettings
	 */
	public static boolean isMatch(StringRef searchArea, final SearchSettings searchSettings) {
		return isMatch(searchArea.toString(), searchSettings);
	}

	/*
	 * Checks if searchArea matches the searched string found in searchSettings
	 */
	public static boolean isMatch(String searchArea, final SearchSettings searchSettings) {
		return find(searchArea, searchSettings) != -1;
	}

	/*
	 * Returns the position within searchArea that the searched string found in searchSettings was
	 * identified.
	 * returns -1 if a match is not found
	 */
	public static int find(StringRef searchArea, final SearchSettings searchSettings) {
		return find(searchArea.toString(), searchSettings);
	}

	/*
	 * Returns the position within searchArea that the searched string found in searchSettings was
	 * identified.
	 * returns -1 if a match is not found
	 */
	public static int find(String searchArea, final SearchSettings searchSettings) {
		int pos;
		if (searchSettings.isUseRegex()) {
			Matcher matcher = searchSettings.getPattern().matcher(searchArea);
			if (matcher.find(searchSettings.getStartPos())) {
				pos = matcher.start();
			} else {
				pos = -1;
			}
		} else if (searchSettings.isIgnoreCase()) {
			pos = StringUtils.indexOfIgnoreCase(searchArea, searchSettings.getSearchString(), searchSettings.getStartPos());
		} else {
			pos = searchArea.indexOf(searchSettings.getSearchString(), searchSettings.getStartPos());
		}
		return pos;
	}

}
