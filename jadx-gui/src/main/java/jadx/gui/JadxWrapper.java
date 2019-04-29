package jadx.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.api.ResourceFile;
import jadx.core.utils.files.FileUtils;
import jadx.gui.settings.JadxSettings;

public class JadxWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(JadxWrapper.class);

	private final JadxSettings settings;
	private JadxDecompiler decompiler;
	private File openFile;

	public JadxWrapper(JadxSettings settings) {
		this.settings = settings;
	}

	public void openFile(File file) {
		this.openFile = file;
		try {
			JadxArgs jadxArgs = settings.toJadxArgs();
			jadxArgs.setInputFile(file);
			// output folder not known yet => use input dir as a best choice
			jadxArgs.setFsCaseSensitive(FileUtils.isCaseSensitiveFS(file.getParentFile()));

			this.decompiler = new JadxDecompiler(jadxArgs);
			this.decompiler.load();
		} catch (Exception e) {
			LOG.error("Jadx init error", e);
		}
	}

	public void saveAll(final File dir, final ProgressMonitor progressMonitor) {
		Runnable save = () -> {
			try {
				decompiler.getArgs().setRootDir(dir);
				ThreadPoolExecutor ex = (ThreadPoolExecutor) decompiler.getSaveExecutor();
				ex.shutdown();
				while (ex.isTerminating()) {
					long total = ex.getTaskCount();
					long done = ex.getCompletedTaskCount();
					progressMonitor.setProgress((int) (done * 100.0 / total));
					Thread.sleep(500);
				}
				progressMonitor.close();
				LOG.info("decompilation complete, freeing memory ...");
				decompiler.getClasses().forEach(JavaClass::unload);
				LOG.info("done");
			} catch (InterruptedException e) {
				LOG.error("Save interrupted", e);
				Thread.currentThread().interrupt();
			}
		};
		new Thread(save).start();
	}

	/**
	 * Get the complete list of classes
	 */
	public List<JavaClass> getClasses() {
		return decompiler.getClasses();
	}

	/**
	 * Get all classes that are not excluded by the excluded packages settings
	 */
	public List<JavaClass> getIncludedClasses() {
		List<JavaClass> classList = decompiler.getClasses();
		List<String> excludedPackages = getExcludedPackages();
		if (excludedPackages.isEmpty()) {
			return classList;
		}

		return classList.stream().filter(cls -> {
			for (String exclude : excludedPackages) {
				if (cls.getFullName().equals(exclude)
						|| cls.getFullName().startsWith(exclude + '.')) {
					return false;
				}
			}
			return true;
		}).collect(Collectors.toList());
	}

	// TODO: move to CLI and filter classes in JadxDecompiler
	public List<String> getExcludedPackages() {
		String excludedPackages = settings.getExcludedPackages().trim();
		if (excludedPackages.isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.asList(excludedPackages.split("[ ]+"));
	}

	public void addExcludedPackage(String packageToExclude) {
		String newExclusion = settings.getExcludedPackages() + ' ' + packageToExclude;
		settings.setExcludedPackages(newExclusion.trim());
		settings.sync();
	}

	public void removeExcludedPackage(String packageToRemoveFromExclusion) {
		List<String> list = new ArrayList<>(getExcludedPackages());
		list.remove(packageToRemoveFromExclusion);
		settings.setExcludedPackages(String.join(" ", list));
		settings.sync();
	}

	public List<JavaPackage> getPackages() {
		return decompiler.getPackages();
	}

	public List<ResourceFile> getResources() {
		return decompiler.getResources();
	}

	public File getOpenFile() {
		return openFile;
	}

	public JadxArgs getArgs() {
		return decompiler.getArgs();
	}
}
