package jadx.mcp.project;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaNode;

/**
 * Snapshot of GUI editor state taken on UI thread
 */
public final class UiState {
	private final @Nullable String tabTitle;
	private final String tabKind;
	private final @Nullable JavaNode tabNode;
	private final @Nullable String resourceName;
	private final @Nullable String selectedText;
	private final int selectionStart;
	private final int selectionEnd;
	private final int caretPos;
	private final int caretLine;
	private final int selectionStartLine;
	private final int selectionEndLine;
	private final @Nullable JavaNode nodeUnderCaret;
	private final @Nullable JavaNode enclosingNode;

	@SuppressWarnings("checkstyle:ParameterNumber")
	public UiState(@Nullable String tabTitle, String tabKind, @Nullable JavaNode tabNode, @Nullable String resourceName,
			@Nullable String selectedText, int selectionStart, int selectionEnd,
			int caretPos, int caretLine, int selectionStartLine, int selectionEndLine,
			@Nullable JavaNode nodeUnderCaret, @Nullable JavaNode enclosingNode) {
		this.tabTitle = tabTitle;
		this.tabKind = tabKind;
		this.tabNode = tabNode;
		this.resourceName = resourceName;
		this.selectedText = selectedText;
		this.selectionStart = selectionStart;
		this.selectionEnd = selectionEnd;
		this.caretPos = caretPos;
		this.caretLine = caretLine;
		this.selectionStartLine = selectionStartLine;
		this.selectionEndLine = selectionEndLine;
		this.nodeUnderCaret = nodeUnderCaret;
		this.enclosingNode = enclosingNode;
	}

	public static UiState noTab() {
		return new UiState(null, "none", null, null, null, -1, -1, -1, -1, -1, -1, null, null);
	}

	public @Nullable String getTabTitle() {
		return tabTitle;
	}

	/**
	 * 'code' (decompiled class), 'smali', 'resource', 'other' or 'none'
	 */
	public String getTabKind() {
		return tabKind;
	}

	public @Nullable JavaNode getTabNode() {
		return tabNode;
	}

	public @Nullable String getResourceName() {
		return resourceName;
	}

	public @Nullable String getSelectedText() {
		return selectedText;
	}

	public int getSelectionStart() {
		return selectionStart;
	}

	public int getSelectionEnd() {
		return selectionEnd;
	}

	public int getCaretPos() {
		return caretPos;
	}

	/**
	 * 1-based line, -1 if unknown
	 */
	public int getCaretLine() {
		return caretLine;
	}

	public int getSelectionStartLine() {
		return selectionStartLine;
	}

	public int getSelectionEndLine() {
		return selectionEndLine;
	}

	public @Nullable JavaNode getNodeUnderCaret() {
		return nodeUnderCaret;
	}

	public @Nullable JavaNode getEnclosingNode() {
		return enclosingNode;
	}
}
