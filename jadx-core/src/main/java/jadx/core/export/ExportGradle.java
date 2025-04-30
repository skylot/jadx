package jadx.core.export;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.core.dex.nodes.RootNode;
import jadx.core.export.gen.AndroidGradleGenerator;
import jadx.core.export.gen.IExportGradleGenerator;
import jadx.core.export.gen.SimpleJavaGradleGenerator;
import jadx.core.utils.android.AndroidManifestParser;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class ExportGradle {

	private static final Logger LOG = LoggerFactory.getLogger(ExportGradle.class);
	private final RootNode root;
	private final File projectDir;
	private final List<ResourceFile> resources;
	private IExportGradleGenerator generator;

	public ExportGradle(RootNode root, File projectDir, List<ResourceFile> resources) {
		this.root = root;
		this.projectDir = projectDir;
		this.resources = resources;
	}

	public OutDirs init() {
		ExportGradleType exportType = getExportGradleType();
		LOG.info("Export Gradle project using '{}' template", exportType);
		switch (exportType) {
			case ANDROID_APP:
			case ANDROID_LIBRARY:
				generator = new AndroidGradleGenerator(root, projectDir, resources, exportType);
				break;
			case SIMPLE_JAVA:
				generator = new SimpleJavaGradleGenerator(root, projectDir, resources);
				break;
			default:
				throw new JadxRuntimeException("Unexpected export type: " + exportType);
		}
		generator.init();
		OutDirs outDirs = generator.getOutDirs();
		outDirs.makeDirs();
		return outDirs;
	}

	private ExportGradleType getExportGradleType() {
		ExportGradleType argsExportType = root.getArgs().getExportGradleType();
		ExportGradleType detectedType = detectExportType(root, resources);
		if (argsExportType == null
				|| argsExportType == ExportGradleType.AUTO
				|| argsExportType == detectedType) {
			return detectedType;
		}
		return argsExportType;
	}

	public static ExportGradleType detectExportType(RootNode root, List<ResourceFile> resources) {
		ResourceFile androidManifest = AndroidManifestParser.getAndroidManifest(resources);
		if (androidManifest != null) {
			if (resources.stream().anyMatch(r -> r.getOriginalName().equals("classes.jar"))) {
				return ExportGradleType.ANDROID_LIBRARY;
			}
			if (resources.stream().anyMatch(r -> r.getType() == ResourceType.ARSC)) {
				return ExportGradleType.ANDROID_APP;
			}
		}
		return ExportGradleType.SIMPLE_JAVA;
	}

	public void generateGradleFiles() {
		generator.generateFiles();
	}

}
