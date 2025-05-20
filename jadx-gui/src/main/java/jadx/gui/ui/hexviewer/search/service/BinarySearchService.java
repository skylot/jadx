package jadx.gui.ui.hexviewer.search.service;

import jadx.gui.ui.hexviewer.search.SearchParameters;

public interface BinarySearchService {

	void performFind(SearchParameters dialogSearchParameters, SearchStatusListener searchStatusListener);

	void setMatchPosition(int matchPosition);

	void performFindAgain(SearchStatusListener searchStatusListener);

	SearchParameters getLastSearchParameters();

	void clearMatches();

	public interface SearchStatusListener {

		void setStatus(FoundMatches foundMatches, SearchParameters.MatchMode matchMode);

		void clearStatus();
	}

	public static class FoundMatches {

		private int matchesCount;
		private int matchPosition;

		public FoundMatches() {
			matchesCount = 0;
			matchPosition = -1;
		}

		public FoundMatches(int matchesCount, int matchPosition) {
			if (matchPosition >= matchesCount) {
				throw new IllegalStateException("Match position is out of range");
			}

			this.matchesCount = matchesCount;
			this.matchPosition = matchPosition;
		}

		public int getMatchesCount() {
			return matchesCount;
		}

		public int getMatchPosition() {
			return matchPosition;
		}

		public void setMatchesCount(int matchesCount) {
			this.matchesCount = matchesCount;
		}

		public void setMatchPosition(int matchPosition) {
			this.matchPosition = matchPosition;
		}

		public void next() {
			if (matchPosition == matchesCount - 1) {
				throw new IllegalStateException("Cannot find next on last match");
			}

			matchPosition++;
		}

		public void prev() {
			if (matchPosition == 0) {
				throw new IllegalStateException("Cannot find previous on first match");
			}

			matchPosition--;
		}
	}
}
