package jadx.core.export;

import java.io.File;
import java.util.List;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.TaskBarrier;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.android.AndroidManifestParser;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.core.xmlgen.ResContainer;

public class ExportGradleTask implements Runnable {

	private final List<ResourceFile> resources;

	private final RootNode root;
	private final File projectDir;
	private final File appDir;
	private final File srcOutDir;
	private final File resOutDir;

	private final TaskBarrier barrier;

	public ExportGradleTask(List<ResourceFile> resources, RootNode root, File projectDir, TaskBarrier barrier) {
		this.resources = resources;
		this.projectDir = projectDir;
		this.root = root;
		this.appDir = new File(projectDir, "app");
		this.srcOutDir = new File(appDir, "src/main/java");
		this.resOutDir = new File(appDir, "src/main");
		this.barrier = barrier;
	}

	@Override
	public void run() {
		ResourceFile androidManifest = AndroidManifestParser.getAndroidManifest(resources);
		if (androidManifest == null) {
			throw new IllegalStateException("Could not find AndroidManifest.xml");
		}

		List<ResContainer> resContainers = resources.stream()
				.filter(resourceFile -> resourceFile.getType() == ResourceType.ARSC)
				.findFirst()
				.orElseThrow(IllegalStateException::new)
				.loadContent()
				.getSubFiles();

		ResContainer strings = resContainers
				.stream()
				.filter(resContainer -> resContainer.getName().contains("values/strings.xml"))
				.findFirst()
				.orElseGet(() -> resContainers.stream()
						.filter(resContainer -> resContainer.getFileName().contains("strings.xml"))
						.findFirst()
						.orElseThrow(IllegalStateException::new));

		ExportGradleProject export = new ExportGradleProject(root, projectDir, androidManifest, strings);

		// wait until all sources and resources are exported and all necessary info for gradle export are
		// collected
		try {
			barrier.getTaskCountDown().await();
		} catch (InterruptedException e) {
			throw new JadxRuntimeException("Gradle export failed", e);
		}

		export.generateGradleFiles();
	}

	public void init() {
		FileUtils.makeDirs(srcOutDir);
		FileUtils.makeDirs(resOutDir);
	}

	public File getSrcOutDir() {
		return srcOutDir;
	}

	public File getResOutDir() {
		return resOutDir;
	}
}
