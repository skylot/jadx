package jadx.gui.search;

public class MatchingPositions {
	private final String line;
	private int startMath;
	private int endMath;

	private int lineNumber;

	public MatchingPositions(String line) {
		this.line = line;
	}

	public MatchingPositions(String line, int lineNumber, int startMath, int endMath) {
		this.line = line;
		this.lineNumber = lineNumber;
		this.startMath = startMath;
		this.endMath = endMath;
	}

	public MatchingPositions(String line, int startMath, int endMath) {
		this.line = line;
		this.lineNumber = -1;
		this.startMath = startMath;
		this.endMath = endMath;
	}

	public MatchingPositions(int startMath, int endMath) {
		this.line = null;
		this.lineNumber = -1;
		this.startMath = startMath;
		this.endMath = endMath;
	}

	public int getLineNumber() {
		return lineNumber;
	}

	public String getLine() {
		return line;
	}

	public int getEndMath() {
		return endMath;
	}

	public int getStartMath() {
		return startMath;
	}
}
