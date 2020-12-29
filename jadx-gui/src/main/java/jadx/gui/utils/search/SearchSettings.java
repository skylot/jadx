package jadx.gui.utils.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchSettings {

	private static final Logger LOG = LoggerFactory.getLogger(SearchSettings.class);

	private final String searchString;

	private final boolean useRegex;

	private final boolean ignoreCase;

	private Pattern regexPattern;

	private int startPos = 0;

	public SearchSettings(String searchString, boolean ignoreCase, boolean useRegex) {
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
	public boolean isMatch(StringRef searchArea) {
		return isMatch(searchArea.toString());
	}

	/*
	 * Checks if searchArea matches the searched string found in searchSettings
	 */
	public boolean isMatch(String searchArea) {
		return find(searchArea) != -1;
	}

	/*
	 * Returns the position within searchArea that the searched string found in searchSettings was
	 * identified.
	 * returns -1 if a match is not found
	 */
	public int find(StringRef searchArea) {
		return find(searchArea.toString());
	}

	/*
	 * Returns the position within searchArea that the searched string found in searchSettings was
	 * identified.
	 * returns -1 if a match is not found
	 */
	public int find(String searchArea) {
		int pos;
		if (this.useRegex) {
			Matcher matcher = this.regexPattern.matcher(searchArea);
			if (matcher.find(this.startPos)) {
				pos = matcher.start();
			} else {
				pos = -1;
			}
		} else if (this.ignoreCase) {
			pos = StringUtils.indexOfIgnoreCase(searchArea, this.searchString, this.startPos);
		} else {
			pos = searchArea.indexOf(this.searchString, this.startPos);
		}
		return pos;
	}

}
