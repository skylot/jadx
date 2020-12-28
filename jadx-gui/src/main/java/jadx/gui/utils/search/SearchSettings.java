package jadx.gui.utils.search;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchSettings {

	private static final Logger LOG = LoggerFactory.getLogger(SearchSettings.class);

	private String searchString;

	private boolean useRegex;

	private boolean ignoreCase;

	private Pattern regexPattern;

	private int startPos = 0;

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
	 * Set whether Regex search should be done
	 */
	public void setRegex(boolean useRegex) {
		this.useRegex = useRegex;
	}

	/*
	 * Set whether case should be ignored
	 */
	public void setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
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
	 * Set search string
	 */
	public void setSearchString(String searchString) {
		this.searchString = searchString;
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

}
