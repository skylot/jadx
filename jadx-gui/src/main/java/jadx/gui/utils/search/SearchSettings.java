package jadx.gui.utils.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.treemodel.JClass;

public class SearchSettings {

	private static final Logger LOG = LoggerFactory.getLogger(SearchSettings.class);

	private final String searchString;
	private final boolean useRegex;
	private final boolean ignoreCase;

	private JClass activeCls;

	private Pattern regexPattern;
	private int startPos = 0;

	public SearchSettings(String searchString, boolean ignoreCase, boolean useRegex) {
		this.searchString = searchString;
		this.useRegex = useRegex;
		this.ignoreCase = ignoreCase;
	}

	public boolean isUseRegex() {
		return this.useRegex;
	}

	public boolean isIgnoreCase() {
		return this.ignoreCase;
	}

	public String getSearchString() {
		return this.searchString;
	}

	public int getStartPos() {
		return this.startPos;
	}

	public void setStartPos(int startPos) {
		this.startPos = startPos;
	}

	public Pattern getPattern() {
		return this.regexPattern;
	}

	public boolean preCompile() throws InvalidSearchTermException {
		if (useRegex) {
			try {
				int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
				this.regexPattern = Pattern.compile(searchString, flags);
			} catch (Exception e) {
				throw new InvalidSearchTermException("Invalid Regex: " + this.searchString, e);
			}
		}
		return true;
	}

	public boolean isMatch(StringRef searchArea) {
		return find(searchArea) != -1;
	}

	public boolean isMatch(String searchArea) {
		return find(searchArea) != -1;
	}

	public int find(StringRef searchArea) {
		if (useRegex) {
			return findWithRegex(searchArea.toString());
		}
		return searchArea.indexOf(this.searchString, this.startPos, this.ignoreCase);
	}

	public int find(String searchArea) {
		if (useRegex) {
			return findWithRegex(searchArea);
		}
		if (ignoreCase) {
			return StringUtils.indexOfIgnoreCase(searchArea, searchString, startPos);
		}
		return searchArea.indexOf(searchString, startPos);
	}

	private int findWithRegex(String searchArea) {
		Matcher matcher = regexPattern.matcher(searchArea);
		if (matcher.find(startPos)) {
			return matcher.start();
		}
		return -1;
	}

	public JClass getActiveCls() {
		return activeCls;
	}

	public void setActiveCls(JClass activeCls) {
		this.activeCls = activeCls;
	}

	public static class InvalidSearchTermException extends Exception {

		public InvalidSearchTermException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
