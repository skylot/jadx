package jadx.core.export.gen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.security.IJadxSecurity;
import jadx.core.dex.nodes.RootNode;
import jadx.core.export.ExportGradleType;
import jadx.core.export.GradleInfoStorage;
import jadx.core.export.OutDirs;
import jadx.core.export.TemplateFile;
import jadx.core.utils.Utils;
import jadx.core.utils.android.AndroidManifestParser;
import jadx.core.utils.android.AppAttribute;
import jadx.core.utils.android.ApplicationParams;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.ResContainer;

public class AndroidGradleGenerator implements IExportGradleGenerator {
	private static final Logger LOG = LoggerFactory.getLogger(AndroidGradleGenerator.class);
	private static final Pattern ILLEGAL_GRADLE_CHARS = Pattern.compile("[/\\\\:>\"?*|]");

	private static final ApplicationParams UNKNOWN_APP_PARAMS =
			new ApplicationParams("UNKNOWN", 0, 0, 0, 0, "UNKNOWN", "UNKNOWN", "UNKNOWN");

	private final RootNode root;
	private final File projectDir;
	private final List<ResourceFile> resources;
	private final boolean exportApp;

	private OutDirs outDirs;
	private File baseDir;
	private ApplicationParams applicationParams;

	public AndroidGradleGenerator(RootNode root, File projectDir, List<ResourceFile> resources, ExportGradleType exportType) {
		this.root = root;
		this.projectDir = projectDir;
		this.resources = resources;
		this.exportApp = exportType == ExportGradleType.ANDROID_APP;
	}

	@Override
	public void init() {
		String moduleDir = exportApp ? "app" : "lib";
		baseDir = new File(projectDir, moduleDir);
		outDirs = new OutDirs(new File(baseDir, "src/main/java"), new File(baseDir, "src/main"));
		applicationParams = parseApplicationParams();
	}

	@Override
	public void generateFiles() {
		try {
			saveProjectBuildGradle();
			if (exportApp) {
				saveApplicationBuildGradle();
			} else {
				saveLibraryBuildGradle();
			}
			saveSettingsGradle();
			saveGradleProperties();
		} catch (Exception e) {
			throw new JadxRuntimeException("Gradle export failed", e);
		}
	}

	@Override
	public OutDirs getOutDirs() {
		return outDirs;
	}

	private ApplicationParams parseApplicationParams() {
		try {
			ResourceFile androidManifest = AndroidManifestParser.getAndroidManifest(resources);
			if (androidManifest == null) {
				LOG.warn("AndroidManifest.xml not found, exported files will contains 'UNKNOWN' fields");
				return UNKNOWN_APP_PARAMS;
			}
			ResContainer strings = null;
			if (exportApp) {
				ResourceFile arscFile = resources.stream()
						.filter(resourceFile -> resourceFile.getType() == ResourceType.ARSC)
						.findFirst().orElse(null);
				if (arscFile != null) {
					List<ResContainer> resContainers = arscFile.loadContent().getSubFiles();
					strings = resContainers
							.stream()
							.filter(resContainer -> resContainer.getName().contains("values/strings.xml"))
							.findFirst()
							.orElseGet(() -> resContainers.stream()
									.filter(resContainer -> resContainer.getName().contains("strings.xml"))
									.findFirst().orElse(null));
				}
			}

			EnumSet<AppAttribute> attrs = EnumSet.noneOf(AppAttribute.class);
			attrs.add(AppAttribute.MIN_SDK_VERSION);
			if (exportApp) {
				attrs.add(AppAttribute.APPLICATION_LABEL);
				attrs.add(AppAttribute.TARGET_SDK_VERSION);
				attrs.add(AppAttribute.COMPILE_SDK_VERSION);
				attrs.add(AppAttribute.VERSION_NAME);
				attrs.add(AppAttribute.VERSION_CODE);
			}

			IJadxSecurity security = root.getArgs().getSecurity();
			AndroidManifestParser parser = new AndroidManifestParser(androidManifest, strings, attrs, security);
			return parser.parse();
		} catch (Exception t) {
			LOG.warn("Failed to parse AndroidManifest.xml", t);
			return UNKNOWN_APP_PARAMS;
		}
	}

	private void saveGradleProperties() throws IOException {
		GradleInfoStorage gradleInfo = root.getGradleInfoStorage();
		/*
		 * For Android Gradle Plugin >=8.0.0 the property "android.nonFinalResIds=false" has to be set in
		 * "gradle.properties" when resource identifiers are used as constant expressions.
		 */
		if (gradleInfo.isNonFinalResIds()) {
			File gradlePropertiesFile = new File(projectDir, "gradle.properties");
			try (FileOutputStream fos = new FileOutputStream(gradlePropertiesFile)) {
				fos.write("android.nonFinalResIds=false".getBytes(StandardCharsets.UTF_8));
			}
		}
	}

	private void saveProjectBuildGradle() throws IOException {
		TemplateFile tmpl = TemplateFile.fromResources("/export/android/build.gradle.tmpl");
		tmpl.save(new File(projectDir, "build.gradle"));
	}

	private void saveSettingsGradle() throws IOException {
		TemplateFile tmpl = TemplateFile.fromResources("/export/android/settings.gradle.tmpl");
		String appName = applicationParams.getApplicationName();
		String projectName;
		if (appName != null) {
			projectName = ILLEGAL_GRADLE_CHARS.matcher(appName).replaceAll("");
		} else {
			projectName = GradleGeneratorTools.guessProjectName(root);
		}
		tmpl.add("projectName", projectName);
		tmpl.add("mainModuleName", baseDir.getName());
		tmpl.save(new File(projectDir, "settings.gradle"));
	}

	private void saveApplicationBuildGradle() throws IOException {
		String appPackage = Utils.getOrElse(root.getAppPackage(), "UNKNOWN");
		int minSdkVersion = Utils.getOrElse(applicationParams.getMinSdkVersion(), 0);

		TemplateFile tmpl = TemplateFile.fromResources("/export/android/app.build.gradle.tmpl");
		tmpl.add("applicationId", appPackage);
		tmpl.add("minSdkVersion", minSdkVersion);
		tmpl.add("compileSdkVersion", applicationParams.getCompileSdkVersion());
		tmpl.add("targetSdkVersion", applicationParams.getTargetSdkVersion());
		tmpl.add("versionCode", applicationParams.getVersionCode());
		tmpl.add("versionName", applicationParams.getVersionName());
		tmpl.add("additionalOptions", genAdditionalAndroidPluginOptions(minSdkVersion));
		tmpl.save(new File(baseDir, "build.gradle"));
	}

	private void saveLibraryBuildGradle() throws IOException {
		String pkg = Utils.getOrElse(root.getAppPackage(), "UNKNOWN");
		int minSdkVersion = Utils.getOrElse(applicationParams.getMinSdkVersion(), 0);

		TemplateFile tmpl = TemplateFile.fromResources("/export/android/lib.build.gradle.tmpl");
		tmpl.add("packageId", pkg);
		tmpl.add("minSdkVersion", minSdkVersion);
		tmpl.add("compileSdkVersion", applicationParams.getCompileSdkVersion());
		tmpl.add("additionalOptions", genAdditionalAndroidPluginOptions(minSdkVersion));

		tmpl.save(new File(baseDir, "build.gradle"));
	}

	private String genAdditionalAndroidPluginOptions(int minSdkVersion) {
		List<String> additionalOptions = new ArrayList<>();
		GradleInfoStorage gradleInfo = root.getGradleInfoStorage();
		if (gradleInfo.isVectorPathData() && minSdkVersion < 21 || gradleInfo.isVectorFillType() && minSdkVersion < 24) {
			additionalOptions.add("vectorDrawables.useSupportLibrary = true");
		}
		if (gradleInfo.isUseApacheHttpLegacy()) {
			additionalOptions.add("useLibrary 'org.apache.http.legacy'");
		}
		StringBuilder sb = new StringBuilder();
		for (String additionalOption : additionalOptions) {
			sb.append("        ").append(additionalOption).append('\n');
		}
		return sb.toString();
	}
}
