package jadx.plugins.tools.resolvers.github;

import org.jetbrains.annotations.Nullable;

class LocationInfo {
	private final String owner;
	private final String project;
	private final String artifactPrefix;
	private final @Nullable String version;

	public LocationInfo(String owner, String project, String artifactPrefix, @Nullable String version) {
		this.owner = owner;
		this.project = project;
		this.artifactPrefix = artifactPrefix;
		this.version = version;
	}

	public String getOwner() {
		return owner;
	}

	public String getProject() {
		return project;
	}

	public String getArtifactPrefix() {
		return artifactPrefix;
	}

	public @Nullable String getVersion() {
		return version;
	}
}
