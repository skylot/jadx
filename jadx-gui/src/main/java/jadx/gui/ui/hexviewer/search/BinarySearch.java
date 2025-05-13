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

import org.exbin.auxiliary.binary_data.EditableBinaryData;
import org.exbin.auxiliary.binary_data.array.ByteArrayEditableData;

import jadx.gui.ui.hexviewer.HexSearchBar;
import jadx.gui.ui.hexviewer.search.service.BinarySearchService;
import jadx.gui.utils.NLS;

/**
 * Binary search.
 *
 * @author ExBin Project (https://exbin.org)
 */
public class BinarySearch {

	private static final int DEFAULT_DELAY = 500;

	private InvokeSearchThread invokeSearchThread;
	private SearchThread searchThread;

	private SearchOperation currentSearchOperation = SearchOperation.FIND;
	private SearchParameters.SearchDirection currentSearchDirection = SearchParameters.SearchDirection.FORWARD;
	private final SearchParameters currentSearchParameters = new SearchParameters();
	private BinarySearchService.FoundMatches foundMatches = new BinarySearchService.FoundMatches();

	private BinarySearchService binarySearchService;
	private final BinarySearchService.SearchStatusListener searchStatusListener;
	private HexSearchBar binarySearchPanel;

	public BinarySearch(HexSearchBar binarySearchPanel) {
		this.binarySearchPanel = binarySearchPanel;

		searchStatusListener = new BinarySearchService.SearchStatusListener() {
			@Override
			public void setStatus(BinarySearchService.FoundMatches foundMatches, SearchParameters.MatchMode matchMode) {
				BinarySearch.this.foundMatches = foundMatches;
				switch (foundMatches.getMatchesCount()) {
					case 0:
						binarySearchPanel.setInfoLabel(NLS.str("search.match_not_found"));
						break;
					case 1:
						binarySearchPanel.setInfoLabel(
								matchMode == SearchParameters.MatchMode.MULTIPLE
										? NLS.str("search.single_match")
										: NLS.str("search.match_found"));
						break;
					default:
						binarySearchPanel.setInfoLabel(String.format(NLS.str("search.match_of"),
								foundMatches.getMatchPosition() + 1, foundMatches.getMatchesCount()));
						break;
				}
				updateMatchStatus();
			}

			@Override
			public void clearStatus() {
				binarySearchPanel.setInfoLabel("");
				BinarySearch.this.foundMatches = new BinarySearchService.FoundMatches();
				updateMatchStatus();
			}

			private void updateMatchStatus() {
				int matchesCount = foundMatches.getMatchesCount();
				int matchPosition = foundMatches.getMatchPosition();
				binarySearchPanel.updateMatchCount(matchesCount > 0,
						matchesCount > 1 && matchPosition > 0,
						matchPosition < matchesCount - 1);
			}
		};
		binarySearchPanel.setControl(new HexSearchBar.Control() {
			@Override
			public void prevMatch() {
				foundMatches.prev();
				binarySearchService.setMatchPosition(foundMatches.getMatchPosition());
				searchStatusListener.setStatus(foundMatches, binarySearchService.getLastSearchParameters().getMatchMode());
			}

			@Override
			public void nextMatch() {
				foundMatches.next();
				binarySearchService.setMatchPosition(foundMatches.getMatchPosition());
				searchStatusListener.setStatus(foundMatches, binarySearchService.getLastSearchParameters().getMatchMode());
			}

			@Override
			public void performEscape() {
				cancelSearch();
				close();
				clearSearch();
			}

			@Override
			public void performFind() {
				invokeSearch(SearchOperation.FIND);
			}

			@Override
			public void notifySearchChanged() {
				if (currentSearchOperation == SearchOperation.FIND) {
					invokeSearch(SearchOperation.FIND);
				}
			}

			@Override
			public void notifySearchChanging() {
				if (currentSearchOperation != SearchOperation.FIND) {
					return;
				}

				SearchCondition condition = currentSearchParameters.getCondition();
				SearchCondition updatedSearchCondition = binarySearchPanel.getSearchParameters().getCondition();

				switch (updatedSearchCondition.getSearchMode()) {
					case TEXT: {
						String searchText = updatedSearchCondition.getSearchText();
						if (searchText.isEmpty()) {
							condition.setSearchText(searchText);
							clearSearch();
							return;
						}

						if (searchText.equals(condition.getSearchText())) {
							return;
						}

						condition.setSearchText(searchText);
						break;
					}
					case BINARY: {
						EditableBinaryData searchData = (EditableBinaryData) updatedSearchCondition.getBinaryData();
						if (searchData == null || searchData.isEmpty()) {
							condition.setBinaryData(null);
							clearSearch();
							return;
						}

						if (searchData.equals(condition.getBinaryData())) {
							return;
						}

						ByteArrayEditableData data = new ByteArrayEditableData();
						data.insert(0, searchData);
						condition.setBinaryData(data);
						break;
					}
				}
				BinarySearch.this.invokeSearch(SearchOperation.FIND, DEFAULT_DELAY);
			}

			@Override
			public SearchParameters.SearchDirection getSearchDirection() {
				return currentSearchDirection;
			}

			@Override
			public void close() {
				cancelSearch();
				clearSearch();
			}
		});
	}

	public void setBinarySearchService(BinarySearchService binarySearchService) {
		this.binarySearchService = binarySearchService;
	}

	public void setTargetComponent(HexSearchBar targetComponent) {
		binarySearchPanel = targetComponent;
	}

	public BinarySearchService.SearchStatusListener getSearchStatusListener() {
		return searchStatusListener;
	}

	private void invokeSearch(SearchOperation searchOperation) {
		invokeSearch(searchOperation, binarySearchPanel.getSearchParameters(), 0);
	}

	private void invokeSearch(SearchOperation searchOperation, final int delay) {
		invokeSearch(searchOperation, binarySearchPanel.getSearchParameters(), delay);
	}

	private void invokeSearch(SearchOperation searchOperation, SearchParameters searchParameters) {
		invokeSearch(searchOperation, searchParameters, 0);
	}

	private void invokeSearch(SearchOperation searchOperation, SearchParameters searchParameters, final int delay) {
		if (invokeSearchThread != null) {
			invokeSearchThread.interrupt();
		}
		invokeSearchThread = new InvokeSearchThread();
		invokeSearchThread.delay = delay;
		currentSearchOperation = searchOperation;
		currentSearchParameters.setFromParameters(searchParameters);
		invokeSearchThread.start();
	}

	public void cancelSearch() {
		if (invokeSearchThread != null) {
			invokeSearchThread.interrupt();
		}
		if (searchThread != null) {
			searchThread.interrupt();
		}
	}

	public void clearSearch() {
		SearchCondition condition = currentSearchParameters.getCondition();
		condition.clear();
		binarySearchPanel.clearSearch();
		binarySearchService.clearMatches();
		searchStatusListener.clearStatus();
	}

	public HexSearchBar getPanel() {
		return binarySearchPanel;
	}

	public void dataChanged() {
		binarySearchService.clearMatches();
		invokeSearch(currentSearchOperation, DEFAULT_DELAY);
	}

	private class InvokeSearchThread extends Thread {

		private int delay = DEFAULT_DELAY;

		public InvokeSearchThread() {
			super("InvokeSearchThread");
		}

		@Override
		public void run() {
			try {
				Thread.sleep(delay);
				if (searchThread != null) {
					searchThread.interrupt();
				}
				searchThread = new SearchThread();
				searchThread.start();
			} catch (InterruptedException ex) {
				// don't search
			}
		}
	}

	private class SearchThread extends Thread {

		public SearchThread() {
			super("SearchThread");
		}

		@Override
		public void run() {
			switch (currentSearchOperation) {
				case FIND:
					binarySearchService.performFind(currentSearchParameters, searchStatusListener);
					break;
				case FIND_AGAIN:
					binarySearchService.performFindAgain(searchStatusListener);
					break;
				default:
					throw new UnsupportedOperationException("Not supported yet.");
			}
		}
	}

	private enum SearchOperation {
		FIND,
		FIND_AGAIN
	}
}
