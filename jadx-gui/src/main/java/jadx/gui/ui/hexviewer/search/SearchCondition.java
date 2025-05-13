package jadx.gui.ui.hexviewer.search;

import java.util.Objects;

import org.exbin.auxiliary.binary_data.BinaryData;
import org.exbin.auxiliary.binary_data.EditableBinaryData;
import org.exbin.auxiliary.binary_data.array.ByteArrayEditableData;
import org.exbin.bined.CodeAreaUtils;

/**
 * Parameters for action to search for occurrences of text or data.
 *
 * @author ExBin Project (https://exbin.org)
 */

public class SearchCondition {

	private SearchMode searchMode = SearchMode.TEXT;
	private String searchText = "";
	private EditableBinaryData binaryData;

	public SearchCondition() {
	}

	/**
	 * This is copy constructor.
	 *
	 * @param source source condition
	 */
	public SearchCondition(SearchCondition source) {
		searchMode = source.getSearchMode();
		searchText = source.getSearchText();
		binaryData = new ByteArrayEditableData();
		if (source.getBinaryData() != null) {
			binaryData.insert(0, source.getBinaryData());
		}
	}

	public SearchMode getSearchMode() {
		return searchMode;
	}

	public void setSearchMode(SearchMode searchMode) {
		this.searchMode = searchMode;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}

	public BinaryData getBinaryData() {
		return binaryData;
	}

	public void setBinaryData(EditableBinaryData binaryData) {
		this.binaryData = binaryData;
	}

	public boolean isEmpty() {
		switch (searchMode) {
			case TEXT: {
				return searchText == null || searchText.isEmpty();
			}
			case BINARY: {
				return binaryData == null || binaryData.isEmpty();
			}
			default:
				throw CodeAreaUtils.getInvalidTypeException(searchMode);
		}
	}

	@Override
	public int hashCode() {
		int hash = 3;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final SearchCondition other = (SearchCondition) obj;
		if (this.searchMode != other.searchMode) {
			return false;
		}
		if (searchMode == SearchMode.TEXT) {
			return Objects.equals(this.searchText, other.searchText);
		} else {
			return Objects.equals(this.binaryData, other.binaryData);
		}
	}

	public void clear() {
		searchText = "";
		if (binaryData != null) {
			binaryData.clear();
		}
	}

	public enum SearchMode {
		TEXT, BINARY
	}
}
