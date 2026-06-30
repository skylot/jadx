package jadx.plugins.tools.resolvers.github;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import jadx.core.utils.ListUtils;
import jadx.plugins.tools.data.JadxPluginMetadata;
import jadx.plugins.tools.resolvers.IJadxPluginResolver;
import jadx.plugins.tools.resolvers.github.data.Asset;
import jadx.plugins.tools.resolvers.github.data.Release;
import jadx.plugins.tools.utils.PluginUtils;

public class GithubReleaseResolver implements IJadxPluginResolver {
	private static final Pattern VERSION_PATTERN = Pattern.compile("v?\\d+\\.\\d+(\\.\\d+)?");

	@Override
	public Optional<JadxPluginMetadata> resolve(String locationId) {
		LocationInfo info = parseLocation(locationId);
		if (info == null) {
			return Optional.empty();
		}
		Release release = GithubTools.fetchRelease(info);
		JadxPluginMetadata metadata = buildMetadata(release, info);
		return Optional.of(metadata);
	}

	@Override
	public List<JadxPluginMetadata> resolveVersions(String locationId, int page, int perPage) {
		LocationInfo info = parseLocation(locationId);
		if (info == null) {
			return List.of();
		}
		return GithubTools.fetchReleases(info, page, perPage)
				.stream()
				.map(r -> buildMetadata(r, info))
				.collect(Collectors.toList());
	}

	@Override
	public boolean hasVersion(String locationId) {
		LocationInfo locationInfo = parseLocation(locationId);
		return locationInfo != null && locationInfo.getVersion() != null;
	}

	private JadxPluginMetadata buildMetadata(Release release, LocationInfo info) {
		List<Asset> assets = release.getAssets();
		String releaseVersion = PluginUtils.removePrefix(release.getName(), "v");
		Asset asset = searchPluginAsset(assets, info.getArtifactPrefix(), releaseVersion);
		if (!asset.getName().contains(releaseVersion)) {
			String assetVersion = PluginUtils.extractVersion(asset.getName());
			if (assetVersion != null) {
				releaseVersion = assetVersion;
			}
		}

		JadxPluginMetadata metadata = new JadxPluginMetadata();
		metadata.setVersion(releaseVersion);
		metadata.setLocationId(buildLocationIdWithoutVersion(info)); // exclude version for later updates
		metadata.setPath(asset.getDownloadUrl());
		return metadata;
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
		Asset assetJar = searchAssetWithExt(assets, artifactPrefix, releaseVersion, ".jar");
		if (assetJar != null) {
			return assetJar;
		}
		Asset assetZip = searchAssetWithExt(assets, artifactPrefix, releaseVersion, ".zip");
		if (assetZip != null) {
			return assetZip;
		}
		throw new RuntimeException("Release artifact with prefix '" + artifactPrefix + "' not found");
	}

	private static @Nullable Asset searchAssetWithExt(List<Asset> assets, String artifactPrefix, String releaseVersion, String ext) {
		String artifactName = artifactPrefix + '-' + releaseVersion + ext;
		Asset exactAsset = ListUtils.filterOnlyOne(assets, a -> a.getName().equals(artifactName));
		if (exactAsset != null) {
			return exactAsset;
		}
		// search without version filter
		return ListUtils.filterOnlyOne(assets, a -> {
			String assetFileName = a.getName();
			return assetFileName.startsWith(artifactPrefix) && assetFileName.endsWith(ext);
		});
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
		return "github";
	}

	@Override
	public boolean isUpdateSupported() {
		return true;
	}
}
