package jadx.plugins.tools.resolvers.github;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import jadx.core.utils.ListUtils;
import jadx.plugins.tools.data.JadxPluginMetadata;
import jadx.plugins.tools.resolvers.IJadxPluginResolver;
import jadx.plugins.tools.resolvers.github.data.Asset;
import jadx.plugins.tools.resolvers.github.data.Release;

import static jadx.plugins.tools.utils.PluginUtils.removePrefix;

public class GithubReleaseResolver implements IJadxPluginResolver {
	private static final Pattern VERSION_PATTERN = Pattern.compile("v?\\d+\\.\\d+(\\.\\d+)?");

	@Override
	public Optional<JadxPluginMetadata> resolve(String locationId) {
		LocationInfo info = parseLocation(locationId);
		if (info == null) {
			return Optional.empty();
		}
		Release release = GithubTools.fetchRelease(info);
		List<Asset> assets = release.getAssets();
		String releaseVersion = removePrefix(release.getName(), "v");
		Asset asset = searchPluginAsset(assets, info.getArtifactPrefix(), releaseVersion);

		JadxPluginMetadata metadata = new JadxPluginMetadata();
		metadata.setResolverId(id());
		metadata.setVersion(releaseVersion);
		metadata.setLocationId(buildLocationIdWithoutVersion(info)); // exclude version for later updates
		metadata.setJar(asset.getDownloadUrl());
		return Optional.of(metadata);
	}

	private static LocationInfo parseLocation(String locationId) {
		if (!locationId.startsWith("github:")) {
			return null;
		}
		String[] parts = locationId.split(":");
		if (parts.length < 3) {
			return null;
		}
		String owner = parts[1];
		String project = parts[2];
		String version = null;
		String artifactPrefix = project;
		if (parts.length >= 4) {
			String part = parts[3];
			if (VERSION_PATTERN.matcher(part).matches()) {
				version = part;
				if (parts.length >= 5) {
					artifactPrefix = parts[4];
				}
			} else {
				artifactPrefix = part;
			}
		}
		return new LocationInfo(owner, project, artifactPrefix, version);
	}

	private static Asset searchPluginAsset(List<Asset> assets, String artifactPrefix, String releaseVersion) {
		String artifactName = artifactPrefix + '-' + releaseVersion + ".jar";
		Asset exactAsset = ListUtils.filterOnlyOne(assets, a -> a.getName().equals(artifactName));
		if (exactAsset != null) {
			return exactAsset;
		}
		// search without version filter
		Asset foundAsset = ListUtils.filterOnlyOne(assets, a -> {
			String assetFileName = a.getName();
			return assetFileName.startsWith(artifactPrefix) && assetFileName.endsWith(".jar");
		});
		if (foundAsset != null) {
			return foundAsset;
		}
		throw new RuntimeException("Release artifact with prefix '" + artifactPrefix + "' not found");
	}

	private static String buildLocationIdWithoutVersion(LocationInfo info) {
		String baseLocation = "github:" + info.getOwner() + ':' + info.getProject();
		if (info.getProject().equals(info.getArtifactPrefix())) {
			return baseLocation;
		}
		return baseLocation + ':' + info.getArtifactPrefix();
	}

	@Override
	public String id() {
		return "github-release";
	}

	@Override
	public boolean isUpdateSupported() {
		return true;
	}
}
