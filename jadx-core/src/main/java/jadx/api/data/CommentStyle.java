package jadx.api.data;

public enum CommentStyle {

	/**
	 * <pre>
	 * // comment
	 * </pre>
	 */
	LINE("// ", "// ", ""),

	// @formatter:off
	/**
	 * <pre>
	 * /*
	 *  * comment
	 *  *&#47;
	 * </pre>
	 */
	// @formatter:on
	BLOCK("/*\n * ", " * ", "\n */"),

	/**
	 * <pre>
	 * /* comment *&#47;
	 * </pre>
	 */
	BLOCK_CONDENSED("/* ", " * ", " */"),

	// @formatter:off
	/**
	 * <pre>
	 * /**
	 *  * comment
	 *  *&#47;
	 * </pre>
	 */
	// @formatter:on
	JAVADOC("/**\n * ", " * ", "\n */"),

	/**
	 * <pre>
	 * /** comment *&#47;
	 * </pre>
	 */
	JAVADOC_CONDENSED("/** ", " * ", " */");

	private final String start;
	private final String onNewLine;
	private final String end;

	CommentStyle(String start, String onNewLine, String end) {
		this.start = start;
		this.onNewLine = onNewLine;
		this.end = end;
	}

	public String getStart() {
		return start;
	}

	public String getOnNewLine() {
		return onNewLine;
	}

	public String getEnd() {
		return end;
	}
}
