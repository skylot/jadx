package jadx.gui.report;

final class ExceptionData {
	private final Throwable exception;
	private final String githubProject;

	ExceptionData(Throwable exception, String githubProject) {
		this.exception = exception;
		this.githubProject = githubProject;
	}

	public Throwable getException() {
		return exception;
	}

	public String getGithubProject() {
		return githubProject;
	}
}
