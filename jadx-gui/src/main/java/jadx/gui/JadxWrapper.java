package jadx.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.api.JavaPackage;
import jadx.api.ResourceFile;
import jadx.api.impl.InMemoryCodeCache;
import jadx.api.impl.plugins.PluginsContext;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.usage.impl.EmptyUsageInfoCache;
import jadx.api.usage.impl.InMemoryUsageInfoCache;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.ProcessState;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.gui.cache.code.CodeStringCache;
import jadx.gui.cache.code.disk.BufferCodeCache;
import jadx.gui.cache.code.disk.DiskCodeCache;
import jadx.gui.cache.usage.UsageInfoCache;
import jadx.gui.plugins.context.GuiPluginsContext;
import jadx.gui.settings.JadxProject;
import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.CacheObject;

import static jadx.core.dex.nodes.ProcessState.GENERATED_AND_UNLOADED;
import static jadx.core.dex.nodes.ProcessState.NOT_LOADED;
import static jadx.core.dex.nodes.ProcessState.PROCESS_COMPLETE;

@SuppressWarnings("ConstantConditions")
public class JadxWrapper {
	private static final Logger LOG = LoggerFactory.getLogger(JadxWrapper.class);

	private static final Object DECOMPILER_UPDATE_SYNC = new Object();

	private final MainWindow mainWindow;
	private volatile @Nullable JadxDecompiler decompiler;
	private GuiPluginsContext guiPluginsContext;

	public JadxWrapper(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public void open() {
		close();
		try {
			synchronized (DECOMPILER_UPDATE_SYNC) {
				JadxProject project = getProject();
				JadxArgs jadxArgs = getSettings().toJadxArgs();
				project.fillJadxArgs(jadxArgs);

				decompiler = new JadxDecompiler(jadxArgs);
				initGuiPluginsContext();
				initUsageCache(jadxArgs);
				decompiler.load();
				initCodeCache();
			}
		} catch (Exception e) {
			LOG.error("Jadx decompiler wrapper init error", e);
			close();
		}
	}

	// TODO: check and move into core package
	public void unloadClasses() {
		for (ClassNode cls : getDecompiler().getRoot().getClasses()) {
			ProcessState clsState = cls.getState();
			cls.unload();
			cls.setState(clsState == PROCESS_COMPLETE ? GENERATED_AND_UNLOADED : NOT_LOADED);
		}
	}

	public void close() {
		try {
			synchronized (DECOMPILER_UPDATE_SYNC) {
				if (decompiler != null) {
					decompiler.close();
					decompiler = null;
				}
				if (guiPluginsContext != null) {
					guiPluginsContext.reset();
					guiPluginsContext = null;
				}
			}
		} catch (Exception e) {
			LOG.error("Jadx decompiler close error", e);
		} finally {
			mainWindow.getCacheObject().reset();
		}
	}

	private void initCodeCache() {
		switch (getSettings().getCodeCacheMode()) {
			case MEMORY:
				getArgs().setCodeCache(new InMemoryCodeCache());
				break;
			case DISK_WITH_CACHE:
				getArgs().setCodeCache(new CodeStringCache(buildBufferedDiskCache()));
				break;
			case DISK:
				getArgs().setCodeCache(buildBufferedDiskCache());
				break;
		}
	}

	private BufferCodeCache buildBufferedDiskCache() {
		DiskCodeCache diskCache = new DiskCodeCache(getDecompiler().getRoot(), getProject().getCacheDir());
		return new BufferCodeCache(diskCache);
	}

	private void initUsageCache(JadxArgs jadxArgs) {
		switch (getSettings().getUsageCacheMode()) {
			case NONE:
				jadxArgs.setUsageInfoCache(new EmptyUsageInfoCache());
				break;
			case MEMORY:
				jadxArgs.setUsageInfoCache(new InMemoryUsageInfoCache());
				break;
			case DISK:
				jadxArgs.setUsageInfoCache(new UsageInfoCache(getProject().getCacheDir(), jadxArgs.getInputFiles()));
				break;
		}
	}

	private void initGuiPluginsContext() {
		guiPluginsContext = new GuiPluginsContext(mainWindow);
		decompiler.getPluginsContext().setGuiContext(guiPluginsContext);
	}

	/**
	 * Get the complete list of classes
	 */
	public List<JavaClass> getClasses() {
		return getDecompiler().getClasses();
	}

	/**
	 * Get all classes that are not excluded by the excluded packages settings
	 */
	public List<JavaClass> getIncludedClasses() {
		List<JavaClass> classList = getDecompiler().getClasses();
		List<String> excludedPackages = getExcludedPackages();
		if (excludedPackages.isEmpty()) {
			return classList;
		}
		return classList.stream()
				.filter(cls -> isClassIncluded(excludedPackages, cls))
				.collect(Collectors.toList());
	}

	/**
	 * Get all classes that are not excluded by the excluded packages settings including inner classes
	 */
	public List<JavaClass> getIncludedClassesWithInners() {
		List<JavaClass> classes = getDecompiler().getClassesWithInners();
		List<String> excludedPackages = getExcludedPackages();
		if (excludedPackages.isEmpty()) {
			return classes;
		}
		return classes.stream()
				.filter(cls -> isClassIncluded(excludedPackages, cls))
				.collect(Collectors.toList());
	}

	private static boolean isClassIncluded(List<String> excludedPackages, JavaClass cls) {
		for (String exclude : excludedPackages) {
			String clsFullName = cls.getFullName();
			if (clsFullName.equals(exclude)
					|| clsFullName.startsWith(exclude + '.')) {
				return false;
			}
		}
		return true;
	}

	public List<List<JavaClass>> buildDecompileBatches(List<JavaClass> classes) {
		return getDecompiler().getDecompileScheduler().buildBatches(classes);
	}

	// TODO: move to CLI and filter classes in JadxDecompiler
	public List<String> getExcludedPackages() {
		String excludedPackages = getSettings().getExcludedPackages().trim();
		if (excludedPackages.isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.asList(excludedPackages.split(" +"));
	}

	public void setExcludedPackages(List<String> packagesToExclude) {
		getSettings().setExcludedPackages(String.join(" ", packagesToExclude).trim());
		getSettings().sync();
	}

	public void addExcludedPackage(String packageToExclude) {
		String newExclusion = getSettings().getExcludedPackages() + ' ' + packageToExclude;
		getSettings().setExcludedPackages(newExclusion.trim());
		getSettings().sync();
	}

	public void removeExcludedPackage(String packageToRemoveFromExclusion) {
		List<String> list = new ArrayList<>(getExcludedPackages());
		list.remove(packageToRemoveFromExclusion);
		getSettings().setExcludedPackages(String.join(" ", list));
		getSettings().sync();
	}

	public PluginsContext getPluginsContext() {
		if (decompiler != null) {
			return decompiler.getPluginsContext();
		}
		try (JadxDecompiler tmpDecompiler = new JadxDecompiler()) {
			// TODO: override input file checks
			tmpDecompiler.getArgs().setInputFile(FileUtils.createTempFile("tmp.txt").toFile());
			tmpDecompiler.load();
			return tmpDecompiler.getPluginsContext();
		}
	}

	/**
	 * TODO: make method private
	 * Do not store JadxDecompiler in fields to not leak old instances
	 */
	public @NotNull JadxDecompiler getDecompiler() {
		if (decompiler == null || decompiler.getRoot() == null) {
			throw new JadxRuntimeException("Decompiler not yet loaded");
		}
		return decompiler;
	}

	// TODO: forbid usage of this method
	public RootNode getRootNode() {
		return getDecompiler().getRoot();
	}

	public void reloadCodeData() {
		getDecompiler().reloadCodeData();
	}

	public JavaNode getJavaNodeByRef(ICodeNodeRef nodeRef) {
		return getDecompiler().getJavaNodeByRef(nodeRef);
	}

	public @Nullable JavaNode getEnclosingNode(ICodeInfo codeInfo, int pos) {
		return getDecompiler().getEnclosingNode(codeInfo, pos);
	}

	public List<Runnable> getSaveTasks() {
		return getDecompiler().getSaveTasks();
	}

	public List<JavaPackage> getPackages() {
		return getDecompiler().getPackages();
	}

	public List<ResourceFile> getResources() {
		return getDecompiler().getResources();
	}

	public JadxArgs getArgs() {
		return getDecompiler().getArgs();
	}

	public JadxProject getProject() {
		return mainWindow.getProject();
	}

	public JadxSettings getSettings() {
		return mainWindow.getSettings();
	}

	public CacheObject getCache() {
		return mainWindow.getCacheObject();
	}

	/**
	 * @param fullName
	 *                 Full name of an outer class. Inner classes are not supported.
	 */
	public @Nullable JavaClass searchJavaClassByFullAlias(String fullName) {
		return getDecompiler().getClasses().stream()
				.filter(cls -> cls.getFullName().equals(fullName))
				.findFirst()
				.orElse(null);
	}

	public @Nullable JavaClass searchJavaClassByOrigClassName(String fullName) {
		return getDecompiler().searchJavaClassByOrigFullName(fullName);
	}

	/**
	 * @param rawName
	 *                Full raw name of an outer class. Inner classes are not supported.
	 */
	public @Nullable JavaClass searchJavaClassByRawName(String rawName) {
		return getDecompiler().getClasses().stream()
				.filter(cls -> cls.getRawName().equals(rawName))
				.findFirst()
				.orElse(null);
	}
}
