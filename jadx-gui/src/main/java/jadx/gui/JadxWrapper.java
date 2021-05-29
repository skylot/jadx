package jadx.gui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.api.ResourceFile;
import jadx.gui.settings.JadxProject;
import jadx.gui.settings.JadxSettings;

import static jadx.gui.utils.FileUtils.toFiles;

public class JadxWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(JadxWrapper.class);

	private final JadxSettings settings;
	private JadxDecompiler decompiler;
	private JadxProject project;
	private List<Path> openPaths = Collections.emptyList();

	public JadxWrapper(JadxSettings settings) {
		this.settings = settings;
	}

	public void openFile(List<Path> paths) {
		close();
		this.openPaths = paths;
		try {
			JadxArgs jadxArgs = settings.toJadxArgs();
			jadxArgs.setInputFiles(toFiles(paths));
			jadxArgs.setCodeData(project.getCodeData());

			this.decompiler = new JadxDecompiler(jadxArgs);
			this.decompiler.load();
		} catch (Exception e) {
			LOG.error("Jadx init error", e);
			close();
		}
	}

	public void close() {
		if (decompiler != null) {
			try {
				decompiler.close();
			} catch (Exception e) {
				LOG.error("jadx decompiler close error", e);
			}
		}
		this.openPaths = Collections.emptyList();
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

	public void setExcludedPackages(List<String> packagesToExclude) {
		settings.setExcludedPackages(String.join(" ", packagesToExclude).trim());
		settings.sync();
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

	public List<Path> getOpenPaths() {
		return openPaths;
	}

	public JadxDecompiler getDecompiler() {
		return decompiler;
	}

	public JadxArgs getArgs() {
		return decompiler.getArgs();
	}

	public void setProject(JadxProject project) {
		this.project = project;
	}

	/**
	 * @param fullName Full name of an outer class. Inner classes are not supported.
	 */
	public @Nullable JavaClass searchJavaClassByFullAlias(String fullName) {
		return decompiler.getClasses().stream()
				.filter(cls -> cls.getFullName().equals(fullName))
				.findFirst()
				.orElse(null);
	}

	public @Nullable JavaClass searchJavaClassByOrigClassName(String fullName) {
		return decompiler.searchJavaClassByOrigFullName(fullName);
	}

	/**
	 * @param rawName Full raw name of an outer class. Inner classes are not supported.
	 */
	public @Nullable JavaClass searchJavaClassByRawName(String rawName) {
		return decompiler.getClasses().stream()
				.filter(cls -> cls.getRawName().equals(rawName))
				.findFirst()
				.orElse(null);
	}
}
