package jadx.gui.ui.hexviewer.search.service;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.exbin.auxiliary.binary_data.BinaryData;
import org.exbin.bined.CharsetStreamTranslator;
import org.exbin.bined.CodeAreaUtils;
import org.exbin.bined.highlight.swing.SearchCodeAreaColorAssessor;
import org.exbin.bined.highlight.swing.SearchMatch;
import org.exbin.bined.swing.CodeAreaSwingUtils;
import org.exbin.bined.swing.capability.ColorAssessorPainterCapable;
import org.exbin.bined.swing.section.SectCodeArea;

import jadx.gui.ui.hexviewer.search.SearchCondition;
import jadx.gui.ui.hexviewer.search.SearchParameters;

/**
 * Binary search service.
 *
 * @author ExBin Project (https://exbin.org)
 */

public class BinarySearchServiceImpl implements BinarySearchService {

	private static final int MAX_MATCHES_COUNT = 100;
	private final SectCodeArea codeArea;
	private final SearchParameters lastSearchParameters = new SearchParameters();

	public BinarySearchServiceImpl(SectCodeArea codeArea) {
		this.codeArea = codeArea;
	}

	@Override
	public void performFind(SearchParameters searchParameters, SearchStatusListener searchStatusListener) {
		SearchCodeAreaColorAssessor searchAssessor = CodeAreaSwingUtils
				.findColorAssessor((ColorAssessorPainterCapable) codeArea.getPainter(), SearchCodeAreaColorAssessor.class);
		SearchCondition condition = searchParameters.getCondition();
		searchStatusListener.clearStatus();
		if (condition.isEmpty()) {
			searchAssessor.clearMatches();
			codeArea.repaint();
			return;
		}

		long position;
		switch (searchParameters.getSearchDirection()) {
			case FORWARD: {
				if (searchParameters.isSearchFromCursor()) {
					position = codeArea.getActiveCaretPosition().getDataPosition();
				} else {
					position = 0;
				}
				break;
			}
			case BACKWARD: {
				if (searchParameters.isSearchFromCursor()) {
					position = codeArea.getActiveCaretPosition().getDataPosition() - 1;
				} else {
					long searchDataSize;
					switch (condition.getSearchMode()) {
						case TEXT: {
							searchDataSize = condition.getSearchText().length();
							break;
						}
						case BINARY: {
							searchDataSize = condition.getBinaryData().getDataSize();
							break;
						}
						default:
							throw CodeAreaUtils.getInvalidTypeException(condition.getSearchMode());
					}
					position = codeArea.getDataSize() - searchDataSize;
				}
				break;
			}
			default:
				throw CodeAreaUtils.getInvalidTypeException(searchParameters.getSearchDirection());
		}
		searchParameters.setStartPosition(position);

		switch (condition.getSearchMode()) {
			case TEXT: {
				searchForText(searchParameters, searchStatusListener);
				break;
			}
			case BINARY: {
				searchForBinaryData(searchParameters, searchStatusListener);
				break;
			}
			default:
				throw CodeAreaUtils.getInvalidTypeException(condition.getSearchMode());
		}
	}

	/**
	 * Performs search by binary data.
	 */
	private void searchForBinaryData(SearchParameters searchParameters, SearchStatusListener searchStatusListener) {
		SearchCodeAreaColorAssessor searchAssessor = CodeAreaSwingUtils
				.findColorAssessor((ColorAssessorPainterCapable) codeArea.getPainter(), SearchCodeAreaColorAssessor.class);
		SearchCondition condition = searchParameters.getCondition();
		long position = searchParameters.getStartPosition();

		BinaryData searchData = condition.getBinaryData();
		long searchDataSize = searchData.getDataSize();
		BinaryData data = codeArea.getContentData();

		List<SearchMatch> foundMatches = new ArrayList<>();

		long dataSize = data.getDataSize();
		while (position >= 0 && position <= dataSize - searchDataSize) {
			long matchLength = 0;
			while (matchLength < searchDataSize) {
				if (data.getByte(position + matchLength) != searchData.getByte(matchLength)) {
					break;
				}
				matchLength++;
			}

			if (matchLength == searchDataSize) {
				SearchMatch match = new SearchMatch();
				match.setPosition(position);
				match.setLength(searchDataSize);
				if (searchParameters.getSearchDirection() == SearchParameters.SearchDirection.BACKWARD) {
					foundMatches.add(0, match);
				} else {
					foundMatches.add(match);
				}

				if (foundMatches.size() == MAX_MATCHES_COUNT || searchParameters.getMatchMode() == SearchParameters.MatchMode.SINGLE) {
					break;
				}
			}

			position++;
		}

		searchAssessor.setMatches(foundMatches);
		if (!foundMatches.isEmpty()) {
			if (searchParameters.getSearchDirection() == SearchParameters.SearchDirection.BACKWARD) {
				searchAssessor.setCurrentMatchIndex(foundMatches.size() - 1);
			} else {
				searchAssessor.setCurrentMatchIndex(0);
			}
			SearchMatch firstMatch = Objects.requireNonNull(searchAssessor.getCurrentMatch());
			codeArea.revealPosition(firstMatch.getPosition(), 0, codeArea.getActiveSection());
		}
		lastSearchParameters.setFromParameters(searchParameters);
		searchStatusListener.setStatus(
				new FoundMatches(foundMatches.size(), foundMatches.isEmpty() ? -1 : searchAssessor.getCurrentMatchIndex()),
				searchParameters.getMatchMode());
		codeArea.repaint();
	}

	/**
	 * Performs search by text/characters.
	 */
	private void searchForText(SearchParameters searchParameters, SearchStatusListener searchStatusListener) {
		SearchCodeAreaColorAssessor searchAssessor = CodeAreaSwingUtils
				.findColorAssessor((ColorAssessorPainterCapable) codeArea.getPainter(), SearchCodeAreaColorAssessor.class);
		SearchCondition condition = searchParameters.getCondition();

		long position = searchParameters.getStartPosition();
		String findText;
		if (searchParameters.isMatchCase()) {
			findText = condition.getSearchText();
		} else {
			findText = condition.getSearchText().toLowerCase();
		}
		long searchDataSize = findText.length();
		BinaryData data = codeArea.getContentData();

		List<SearchMatch> foundMatches = new ArrayList<>();

		Charset charset = codeArea.getCharset();
		int maxBytesPerChar;
		try {
			CharsetEncoder encoder = charset.newEncoder();
			maxBytesPerChar = (int) encoder.maxBytesPerChar();
		} catch (UnsupportedOperationException ex) {
			maxBytesPerChar = CharsetStreamTranslator.DEFAULT_MAX_BYTES_PER_CHAR;
		}
		byte[] charData = new byte[maxBytesPerChar];
		long dataSize = data.getDataSize();
		long lastPosition = position;
		while (position >= 0 && position <= dataSize - searchDataSize) {
			int matchCharLength = 0;
			int matchLength = 0;
			while (matchCharLength < (int) searchDataSize) {
				if (Thread.interrupted()) {
					return;
				}

				long searchPosition = position + (long) matchLength;
				int bytesToUse = maxBytesPerChar;
				if (searchPosition + bytesToUse > dataSize) {
					bytesToUse = (int) (dataSize - searchPosition);
				}

				if (searchPosition == lastPosition + 1) {
					System.arraycopy(charData, 1, charData, 0, maxBytesPerChar - 1);
					charData[bytesToUse - 1] = data.getByte(searchPosition + bytesToUse - 1);
				} else if (searchPosition == lastPosition - 1) {
					System.arraycopy(charData, 0, charData, 1, maxBytesPerChar - 1);
					charData[0] = data.getByte(searchPosition);
				} else {
					data.copyToArray(searchPosition, charData, 0, bytesToUse);
				}
				if (bytesToUse < maxBytesPerChar) {
					Arrays.fill(charData, bytesToUse, maxBytesPerChar, (byte) 0);
				}
				lastPosition = searchPosition;
				char singleChar = new String(charData, charset).charAt(0);

				if (searchParameters.isMatchCase()) {
					if (singleChar != findText.charAt(matchCharLength)) {
						break;
					}
				} else if (Character.toLowerCase(singleChar) != findText.charAt(matchCharLength)) {
					break;
				}
				int characterLength = String.valueOf(singleChar).getBytes(charset).length;
				matchCharLength++;
				matchLength += characterLength;
			}

			if (matchCharLength == findText.length()) {
				SearchMatch match = new SearchMatch();
				match.setPosition(position);
				match.setLength(matchLength);
				if (searchParameters.getSearchDirection() == SearchParameters.SearchDirection.BACKWARD) {
					foundMatches.add(0, match);
				} else {
					foundMatches.add(match);
				}

				if (foundMatches.size() == MAX_MATCHES_COUNT || searchParameters.getMatchMode() == SearchParameters.MatchMode.SINGLE) {
					break;
				}
			}

			switch (searchParameters.getSearchDirection()) {
				case FORWARD: {
					position++;
					break;
				}
				case BACKWARD: {
					position--;
					break;
				}
				default:
					throw CodeAreaUtils.getInvalidTypeException(searchParameters.getSearchDirection());
			}
		}

		if (Thread.interrupted()) {
			return;
		}

		searchAssessor.setMatches(foundMatches);
		if (!foundMatches.isEmpty()) {
			if (searchParameters.getSearchDirection() == SearchParameters.SearchDirection.BACKWARD) {
				searchAssessor.setCurrentMatchIndex(foundMatches.size() - 1);
			} else {
				searchAssessor.setCurrentMatchIndex(0);
			}
			SearchMatch firstMatch = searchAssessor.getCurrentMatch();
			codeArea.revealPosition(firstMatch.getPosition(), 0, codeArea.getActiveSection());
		}
		lastSearchParameters.setFromParameters(searchParameters);
		searchStatusListener.setStatus(
				new FoundMatches(foundMatches.size(), foundMatches.isEmpty() ? -1 : searchAssessor.getCurrentMatchIndex()),
				searchParameters.getMatchMode());
		codeArea.repaint();
	}

	@Override
	public void setMatchPosition(int matchPosition) {
		SearchCodeAreaColorAssessor searchAssessor = CodeAreaSwingUtils
				.findColorAssessor((ColorAssessorPainterCapable) codeArea.getPainter(), SearchCodeAreaColorAssessor.class);
		searchAssessor.setCurrentMatchIndex(matchPosition);
		SearchMatch currentMatch = searchAssessor.getCurrentMatch();
		codeArea.revealPosition(currentMatch.getPosition(), 0, codeArea.getActiveSection());
		codeArea.repaint();
	}

	@Override
	public void performFindAgain(SearchStatusListener searchStatusListener) {
		SearchCodeAreaColorAssessor searchAssessor = CodeAreaSwingUtils
				.findColorAssessor((ColorAssessorPainterCapable) codeArea.getPainter(), SearchCodeAreaColorAssessor.class);
		List<SearchMatch> foundMatches = searchAssessor.getMatches();
		int matchesCount = foundMatches.size();
		if (matchesCount > 0) {
			switch (lastSearchParameters.getMatchMode()) {
				case MULTIPLE:
					if (matchesCount > 1) {
						int currentMatchIndex = searchAssessor.getCurrentMatchIndex();
						setMatchPosition(currentMatchIndex < matchesCount - 1 ? currentMatchIndex + 1 : 0);
						searchStatusListener.setStatus(new FoundMatches(foundMatches.size(), searchAssessor.getCurrentMatchIndex()),
								lastSearchParameters.getMatchMode());
					}

					break;
				case SINGLE:
					switch (lastSearchParameters.getSearchDirection()) {
						case FORWARD:
							lastSearchParameters.setStartPosition(foundMatches.get(0).getPosition() + 1);
							break;
						case BACKWARD:
							SearchMatch match = foundMatches.get(0);
							lastSearchParameters.setStartPosition(match.getPosition() - 1);
							break;
					}

					SearchCondition condition = lastSearchParameters.getCondition();
					switch (condition.getSearchMode()) {
						case TEXT: {
							searchForText(lastSearchParameters, searchStatusListener);
							break;
						}
						case BINARY: {
							searchForBinaryData(lastSearchParameters, searchStatusListener);
							break;
						}
						default:
							throw CodeAreaUtils.getInvalidTypeException(condition.getSearchMode());
					}
					break;
			}
		}
	}

	@Override
	public SearchParameters getLastSearchParameters() {
		return lastSearchParameters;
	}

	@Override
	public void clearMatches() {
		SearchCodeAreaColorAssessor searchAssessor = CodeAreaSwingUtils
				.findColorAssessor((ColorAssessorPainterCapable) codeArea.getPainter(), SearchCodeAreaColorAssessor.class);
		searchAssessor.clearMatches();
	}
}
