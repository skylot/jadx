package jadx.gui.ui.hexviewer.search;

/*
 * Copyright (C) ExBin Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Parameters for action to search for occurrences of text or data.
 *
 * @author ExBin Project (https://exbin.org)
 */

public class SearchParameters {

	private SearchCondition condition = new SearchCondition();
	private long startPosition;
	private boolean searchFromCursor;
	private boolean matchCase = true;
	private MatchMode matchMode = MatchMode.MULTIPLE;
	private SearchDirection searchDirection = SearchDirection.FORWARD;

	public SearchParameters() {
	}

	public SearchCondition getCondition() {
		return condition;
	}

	public void setCondition(SearchCondition condition) {
		this.condition = condition;
	}

	public long getStartPosition() {
		return startPosition;
	}

	public void setStartPosition(long startPosition) {
		this.startPosition = startPosition;
	}

	public boolean isSearchFromCursor() {
		return searchFromCursor;
	}

	public void setSearchFromCursor(boolean searchFromCursor) {
		this.searchFromCursor = searchFromCursor;
	}

	public boolean isMatchCase() {
		return matchCase;
	}

	public void setMatchCase(boolean matchCase) {
		this.matchCase = matchCase;
	}

	public MatchMode getMatchMode() {
		return matchMode;
	}

	public void setMatchMode(MatchMode matchMode) {
		this.matchMode = matchMode;
	}

	public SearchDirection getSearchDirection() {
		return searchDirection;
	}

	public void setSearchDirection(SearchDirection searchDirection) {
		this.searchDirection = searchDirection;
	}

	public void setFromParameters(SearchParameters searchParameters) {
		condition = searchParameters.getCondition();
		startPosition = searchParameters.getStartPosition();
		searchFromCursor = searchParameters.isSearchFromCursor();
		matchCase = searchParameters.isMatchCase();
		matchMode = searchParameters.getMatchMode();
		searchDirection = searchParameters.getSearchDirection();
	}

	public enum SearchDirection {
		FORWARD, BACKWARD
	}

	public enum MatchMode {
		SINGLE, MULTIPLE;

		public static MatchMode fromBoolean(boolean multipleMatches) {
			return multipleMatches ? MULTIPLE : SINGLE;
		}
	}
}
