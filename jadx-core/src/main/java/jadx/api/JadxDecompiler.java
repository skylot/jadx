package jadx.api;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.api.metadata.annotations.VarNode;
import jadx.api.metadata.annotations.VarRef;
import jadx.api.plugins.CustomResourcesLoader;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.events.IJadxEvents;
import jadx.api.plugins.input.ICodeLoader;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.api.plugins.pass.JadxPass;
import jadx.api.plugins.pass.types.JadxAfterLoadPass;
import jadx.api.plugins.pass.types.JadxPassType;
import jadx.api.utils.tasks.ITaskExecutor;
import jadx.core.Jadx;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.SaveCode;
import jadx.core.export.ExportGradle;
import jadx.core.export.OutDirs;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginContext;
import jadx.core.plugins.events.JadxEventsImpl;
import jadx.core.utils.DecompilerScheduler;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.core.utils.tasks.TaskExecutor;
import jadx.core.xmlgen.ResourcesSaver;
import jadx.zip.ZipReader;

/**
 * Jadx API usage example:
 *
 * <pre>
 * <code>
 *
 * JadxArgs args = new JadxArgs();
 * args.getInputFiles().add(new File("test.apk"));
 * args.setOutDir(new File("jadx-test-output"));
 * try (JadxDecompiler jadx = new JadxDecompiler(args)) {
 *    jadx.load();
 *    jadx.save();
 * }
 * </code>
 * </pre>
 * <p>
 * Instead of 'save()' you can iterate over decompiled classes:
 *
 * <pre>
 * <code>
 *
 *  for(JavaClass cls : jadx.getClasses()) {
 *      System.out.println(cls.getCode());
 *  }
 * </code>
 * </pre>
 */
public final class JadxDecompiler implements Closeable {
	private static final Logger LOG = LoggerFactory.getLogger(JadxDecompiler.class);

	private final JadxArgs args;
	private final JadxPluginManager pluginManager;
	private final List<ICodeLoader> loadedInputs = new ArrayList<>();
	private final ZipReader zipReader;

	private RootNode root;
	private List<JavaClass> classes;
	private List<ResourceFile> resources;

	private final IDecompileScheduler decompileScheduler = new DecompilerScheduler();
	private final ResourcesLoader resourcesLoader;

	private final List<ICodeLoader> customCodeLoaders = new ArrayList<>();
	private final List<CustomResourcesLoader> customResourcesLoaders = new ArrayList<>();
	private final Map<JadxPassType, List<JadxPass>> customPasses = new HashMap<>();
	private final List<Closeable> closeableList = new ArrayList<>();

	private IJadxEvents events = new JadxEventsImpl();

	public JadxDecompiler() {
		this(new JadxArgs());
	}

	public JadxDecompiler(JadxArgs args) {
		this.args = Objects.requireNonNull(args);
		this.pluginManager = new JadxPluginManager(this);
		this.resourcesLoader = new ResourcesLoader(this);
		this.zipReader = new ZipReader(args.getSecurity());
	}

	public void load() {
		reset();
		JadxArgsValidator.validate(this);
		LOG.info("loading ...");
		FileUtils.updateTempRootDir(args.getFilesGetter().getTempDir());
		loadPlugins();
		loadInputFiles();

		root = new RootNode(this);
		root.init();
		// load classes and resources
		root.loadClasses(loadedInputs);
		root.loadResources(resourcesLoader, getResources());
		root.finishClassLoad();
		root.initClassPath();
		// init passes
		root.mergePasses(customPasses);
		root.runPreDecompileStage();
		root.initPasses();
		loadFinished();
	}

	public void reloadPasses() {
		LOG.info("reloading (passes only) ...");
		customPasses.clear();
		root.resetPasses();
		events.reset();
		loadPlugins();
		root.mergePasses(customPasses);
		root.restartVisitors();
		root.initPasses();
		loadFinished();
	}

	private void loadInputFiles() {
		loadedInputs.clear();
		List<Path> inputPaths = Utils.collectionMap(args.getInputFiles(), File::toPath);
		List<Path> inputFiles = FileUtils.expandDirs(inputPaths);
		long start = System.currentTimeMillis();
		for (PluginContext plugin : pluginManager.getResolvedPluginContexts()) {
			for (JadxCodeInput codeLoader : plugin.getCodeInputs()) {
				try {
					ICodeLoader loader = codeLoader.loadFiles(inputFiles);
					if (loader != null && !loader.isEmpty()) {
						loadedInputs.add(loader);
					}
				} catch (Exception e) {
					LOG.warn("Failed to load code for plugin: {}", plugin, e);
				}
			}
		}
		loadedInputs.addAll(customCodeLoaders);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Loaded using {} inputs plugin in {} ms", loadedInputs.size(), System.currentTimeMillis() - start);
		}
	}

	private void reset() {
		unloadPlugins();
		root = null;
		classes = null;
		resources = null;
		events.reset();
	}

	@Override
	public void close() {
		reset();
		closeAll(loadedInputs);
		closeAll(customCodeLoaders);
		closeAll(customResourcesLoaders);
		closeAll(closeableList);
		FileUtils.deleteDirIfExists(args.getFilesGetter().getTempDir());
		args.close();
		FileUtils.clearTempRootDir();
	}

	private void closeAll(List<? extends Closeable> list) {
		try {
			for (Closeable closeable : list) {
				try {
					closeable.close();
				} catch (Exception e) {
					LOG.warn("Fail to close '{}'", closeable, e);
				}
			}
		} finally {
			list.clear();
		}
	}

	private void loadPlugins() {
		pluginManager.providesSuggestion("java-input", args.isUseDxInput() ? "java-convert" : "java-input");
		pluginManager.load(args.getPluginLoader());
		if (LOG.isDebugEnabled()) {
			LOG.debug("Resolved plugins: {}", pluginManager.getResolvedPluginContexts());
		}
		pluginManager.initResolved();
		if (LOG.isDebugEnabled()) {
			List<String> passes = customPasses.values().stream().flatMap(Collection::stream)
					.map(p -> p.getInfo().getName()).collect(Collectors.toList());
			LOG.debug("Loaded custom passes: {} {}", passes.size(), passes);
		}
	}

	private void unloadPlugins() {
		pluginManager.unloadResolved();
	}

	private void loadFinished() {
		LOG.debug("Load finished");
		List<JadxPass> list = customPasses.get(JadxAfterLoadPass.TYPE);
		if (list != null) {
			for (JadxPass pass : list) {
				((JadxAfterLoadPass) pass).init(this);
			}
		}
	}

	@SuppressWarnings("unused")
	public void registerPlugin(JadxPlugin plugin) {
		pluginManager.register(plugin);
	}

	public static String getVersion() {
		return Jadx.getVersion();
	}

	public void save() {
		save(!args.isSkipSources(), !args.isSkipResources());
	}

	public interface ProgressListener {
		void progress(long done, long total);
	}

	@SuppressWarnings("BusyWait")
	public void save(int intervalInMillis, ProgressListener listener) {
		try {
			ITaskExecutor tasks = getSaveTaskExecutor();
			tasks.execute();
			long total = tasks.getTasksCount();
			while (tasks.isRunning()) {
				listener.progress(tasks.getProgress(), total);
				Thread.sleep(intervalInMillis);
			}
		} catch (InterruptedException e) {
			LOG.error("Save interrupted", e);
			Thread.currentThread().interrupt();
		}
	}

	public void saveSources() {
		save(true, false);
	}

	public void saveResources() {
		save(false, true);
	}

	private void save(boolean saveSources, boolean saveResources) {
		ITaskExecutor executor = getSaveTasks(saveSources, saveResources);
		executor.execute();
		executor.awaitTermination();
	}

	public ITaskExecutor getSaveTaskExecutor() {
		return getSaveTasks(!args.isSkipSources(), !args.isSkipResources());
	}

	@Deprecated(forRemoval = true)
	public ExecutorService getSaveExecutor() {
		ITaskExecutor executor = getSaveTaskExecutor();
		executor.execute();
		return executor.getInternalExecutor();
	}

	@Deprecated(forRemoval = true)
	public List<Runnable> getSaveTasks() {
		return Collections.singletonList(this::save);
	}

	private TaskExecutor getSaveTasks(boolean saveSources, boolean saveResources) {
		if (root == null) {
			throw new JadxRuntimeException("No loaded files");
		}
		OutDirs outDirs;
		ExportGradle gradleExport;
		if (args.getExportGradleType() != null) {
			gradleExport = new ExportGradle(root, args.getOutDir(), getResources());
			outDirs = gradleExport.init();
		} else {
			gradleExport = null;
			outDirs = new OutDirs(args.getOutDirSrc(), args.getOutDirRes());
			outDirs.makeDirs();
		}

		TaskExecutor executor = new TaskExecutor();
		executor.setThreadsCount(args.getThreadsCount());
		if (saveResources) {
			// save resources first because decompilation can stop or fail
			appendResourcesSaveTasks(executor, outDirs.getResOutDir());
		}
		if (saveSources) {
			appendSourcesSave(executor, outDirs.getSrcOutDir());
		}
		if (gradleExport != null) {
			executor.addSequentialTask(gradleExport::generateGradleFiles);
		}
		return executor;
	}

	private void appendResourcesSaveTasks(ITaskExecutor executor, File outDir) {
		if (args.isSkipFilesSave()) {
			return;
		}
		// process AndroidManifest.xml first to load complete resource ids table
		for (ResourceFile resourceFile : getResources()) {
			if (resourceFile.getType() == ResourceType.MANIFEST) {
				new ResourcesSaver(this, outDir, resourceFile).run();
				break;
			}
		}
		Set<String> inputFileNames = args.getInputFiles().stream()
				.map(File::getAbsolutePath)
				.collect(Collectors.toSet());
		Set<String> codeSources = collectCodeSources();

		List<Runnable> tasks = new ArrayList<>();
		for (ResourceFile resourceFile : getResources()) {
			ResourceType resType = resourceFile.getType();
			if (resType == ResourceType.MANIFEST) {
				// already processed
				continue;
			}
			String resOriginalName = resourceFile.getOriginalName();
			if (resType != ResourceType.ARSC && inputFileNames.contains(resOriginalName)) {
				// ignore resource made from an input file
				continue;
			}
			if (codeSources.contains(resOriginalName)) {
				// don't output code source resources (.dex, .class, etc)
				// do not trust file extensions, use only sources set as class inputs
				continue;
			}
			tasks.add(new ResourcesSaver(this, outDir, resourceFile));
		}
		executor.addParallelTasks(tasks);
	}

	private Set<String> collectCodeSources() {
		Set<String> set = new HashSet<>();
		for (ClassNode cls : root.getClasses(true)) {
			if (cls.getClsData() == null) {
				// exclude synthetic classes
				continue;
			}
			String inputFileName = cls.getInputFileName();
			if (inputFileName.endsWith(".class")) {
				// cut .class name to get source .jar file
				// current template: "<optional input files>:<.jar>:<full class name>"
				// TODO: add property to set file name or reference to resource name
				int endIdx = inputFileName.lastIndexOf(':');
				if (endIdx != -1) {
					int startIdx = inputFileName.lastIndexOf(':', endIdx - 1) + 1;
					inputFileName = inputFileName.substring(startIdx, endIdx);
				}
			}
			set.add(inputFileName);
		}
		return set;
	}

	private void appendSourcesSave(ITaskExecutor executor, File outDir) {
		List<JavaClass> classes = getClasses();
		List<JavaClass> processQueue = filterClasses(classes);
		List<List<JavaClass>> batches;
		try {
			batches = decompileScheduler.buildBatches(processQueue);
		} catch (Exception e) {
			throw new JadxRuntimeException("Decompilation batches build failed", e);
		}
		List<Runnable> decompileTasks = new ArrayList<>(batches.size());
		for (List<JavaClass> decompileBatch : batches) {
			decompileTasks.add(() -> {
				for (JavaClass cls : decompileBatch) {
					try {
						ClassNode clsNode = cls.getClassNode();
						ICodeInfo code = clsNode.getCode();
						SaveCode.save(outDir, clsNode, code);
					} catch (Exception e) {
						LOG.error("Error saving class: {}", cls, e);
					}
				}

			});
		}
		executor.addParallelTasks(decompileTasks);
	}

	private List<JavaClass> filterClasses(List<JavaClass> classes) {
		Predicate<String> classFilter = args.getClassFilter();
		List<JavaClass> list = new ArrayList<>(classes.size());
		for (JavaClass cls : classes) {
			ClassNode clsNode = cls.getClassNode();
			if (clsNode.contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			if (classFilter != null && !classFilter.test(clsNode.getClassInfo().getFullName())) {
				if (!args.isIncludeDependencies()) {
					clsNode.add(AFlag.DONT_GENERATE);
				}
				continue;
			}
			list.add(cls);
		}
		return list;
	}

	public List<JavaClass> getClasses() {
		if (root == null) {
			return Collections.emptyList();
		}
		if (classes == null) {
			List<ClassNode> classNodeList = root.getClasses();
			List<JavaClass> clsList = new ArrayList<>(classNodeList.size());
			for (ClassNode classNode : classNodeList) {
				if (classNode.contains(AFlag.DONT_GENERATE)) {
					continue;
				}
				if (!classNode.getClassInfo().isInner()) {
					clsList.add(convertClassNode(classNode));
				}
			}
			classes = Collections.unmodifiableList(clsList);
		}
		return classes;
	}

	public List<JavaClass> getClassesWithInners() {
		return Utils.collectionMap(root.getClasses(), this::convertClassNode);
	}

	public synchronized List<ResourceFile> getResources() {
		if (resources == null) {
			if (root == null) {
				return Collections.emptyList();
			}
			resources = resourcesLoader.load(root);
		}
		return resources;
	}

	public List<JavaPackage> getPackages() {
		return Utils.collectionMap(root.getPackages(), this::convertPackageNode);
	}

	public int getErrorsCount() {
		if (root == null) {
			return 0;
		}
		return root.getErrorsCounter().getErrorCount();
	}

	public int getWarnsCount() {
		if (root == null) {
			return 0;
		}
		return root.getErrorsCounter().getWarnsCount();
	}

	public void printErrorsReport() {
		if (root == null) {
			return;
		}
		root.getClsp().printMissingClasses();
		root.getErrorsCounter().printReport();
	}

	/**
	 * Internal API. Not Stable!
	 */
	@ApiStatus.Internal
	public RootNode getRoot() {
		return root;
	}

	/**
	 * Get JavaClass by ClassNode without loading and decompilation
	 */
	@ApiStatus.Internal
	synchronized JavaClass convertClassNode(ClassNode cls) {
		JavaClass javaClass = cls.getJavaNode();
		if (javaClass == null) {
			javaClass = cls.isInner()
					? new JavaClass(cls, convertClassNode(cls.getParentClass()))
					: new JavaClass(cls, this);
			cls.setJavaNode(javaClass);
		}
		return javaClass;
	}

	@ApiStatus.Internal
	synchronized JavaField convertFieldNode(FieldNode fld) {
		JavaField javaField = fld.getJavaNode();
		if (javaField == null) {
			JavaClass parentCls = convertClassNode(fld.getParentClass());
			javaField = new JavaField(parentCls, fld);
			fld.setJavaNode(javaField);
		}
		return javaField;
	}

	@ApiStatus.Internal
	synchronized JavaMethod convertMethodNode(MethodNode mth) {
		JavaMethod javaMethod = mth.getJavaNode();
		if (javaMethod == null) {
			javaMethod = new JavaMethod(convertClassNode(mth.getParentClass()), mth);
			mth.setJavaNode(javaMethod);
		}
		return javaMethod;
	}

	@ApiStatus.Internal
	synchronized JavaPackage convertPackageNode(PackageNode pkg) {
		JavaPackage foundPkg = pkg.getJavaNode();
		if (foundPkg != null) {
			return foundPkg;
		}
		List<JavaClass> clsList = Utils.collectionMap(pkg.getClasses(), this::convertClassNode);
		int subPkgsCount = pkg.getSubPackages().size();
		List<JavaPackage> subPkgs = subPkgsCount == 0 ? Collections.emptyList() : new ArrayList<>(subPkgsCount);
		JavaPackage javaPkg = new JavaPackage(pkg, clsList, subPkgs);
		if (subPkgsCount != 0) {
			// add subpackages after parent to avoid endless recursion
			for (PackageNode subPackage : pkg.getSubPackages()) {
				subPkgs.add(convertPackageNode(subPackage));
			}
		}
		pkg.setJavaNode(javaPkg);
		return javaPkg;
	}

	@Nullable
	public JavaClass searchJavaClassByOrigFullName(String fullName) {
		return getRoot().getClasses().stream()
				.filter(cls -> cls.getClassInfo().getFullName().equals(fullName))
				.findFirst()
				.map(this::convertClassNode)
				.orElse(null);
	}

	@Nullable
	public ClassNode searchClassNodeByOrigFullName(String fullName) {
		return getRoot().getClasses().stream()
				.filter(cls -> cls.getClassInfo().getFullName().equals(fullName))
				.findFirst()
				.orElse(null);
	}

	// returns parent if class contains DONT_GENERATE flag.
	@Nullable
	public JavaClass searchJavaClassOrItsParentByOrigFullName(String fullName) {
		ClassNode node = getRoot().getClasses().stream()
				.filter(cls -> cls.getClassInfo().getFullName().equals(fullName))
				.findFirst()
				.orElse(null);
		if (node != null) {
			if (node.contains(AFlag.DONT_GENERATE)) {
				return convertClassNode(node.getTopParentClass());
			} else {
				return convertClassNode(node);
			}
		}
		return null;
	}

	@Nullable
	public JavaClass searchJavaClassByAliasFullName(String fullName) {
		return getRoot().getClasses().stream()
				.filter(cls -> cls.getClassInfo().getAliasFullName().equals(fullName))
				.findFirst()
				.map(this::convertClassNode)
				.orElse(null);
	}

	@Nullable
	public JavaNode getJavaNodeByRef(ICodeNodeRef ann) {
		return getJavaNodeByCodeAnnotation(null, ann);
	}

	@Nullable
	public JavaNode getJavaNodeByCodeAnnotation(@Nullable ICodeInfo codeInfo, @Nullable ICodeAnnotation ann) {
		if (ann == null) {
			return null;
		}
		switch (ann.getAnnType()) {
			case CLASS:
				return convertClassNode((ClassNode) ann);
			case METHOD:
				return convertMethodNode((MethodNode) ann);
			case FIELD:
				return convertFieldNode((FieldNode) ann);
			case PKG:
				return convertPackageNode((PackageNode) ann);
			case DECLARATION:
				return getJavaNodeByCodeAnnotation(codeInfo, ((NodeDeclareRef) ann).getNode());
			case VAR:
				return resolveVarNode((VarNode) ann);
			case VAR_REF:
				return resolveVarRef(codeInfo, (VarRef) ann);
			case OFFSET:
				// offset annotation don't have java node object
				return null;
			default:
				throw new JadxRuntimeException("Unknown annotation type: " + ann.getAnnType() + ", class: " + ann.getClass());
		}
	}

	private JavaVariable resolveVarNode(VarNode varNode) {
		JavaMethod javaNode = convertMethodNode(varNode.getMth());
		return new JavaVariable(javaNode, varNode);
	}

	@Nullable
	private JavaVariable resolveVarRef(ICodeInfo codeInfo, VarRef varRef) {
		if (codeInfo == null) {
			throw new JadxRuntimeException("Missing code info for resolve VarRef: " + varRef);
		}
		ICodeAnnotation varNodeAnn = codeInfo.getCodeMetadata().getAt(varRef.getRefPos());
		if (varNodeAnn != null && varNodeAnn.getAnnType() == ICodeAnnotation.AnnType.DECLARATION) {
			ICodeNodeRef nodeRef = ((NodeDeclareRef) varNodeAnn).getNode();
			if (nodeRef.getAnnType() == ICodeAnnotation.AnnType.VAR) {
				return resolveVarNode((VarNode) nodeRef);
			}
		}
		return null;
	}

	List<JavaNode> convertNodes(Collection<? extends ICodeNodeRef> nodesList) {
		return nodesList.stream()
				.map(this::getJavaNodeByRef)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	@Nullable
	public JavaNode getJavaNodeAtPosition(ICodeInfo codeInfo, int pos) {
		ICodeAnnotation ann = codeInfo.getCodeMetadata().getAt(pos);
		return getJavaNodeByCodeAnnotation(codeInfo, ann);
	}

	@Nullable
	public JavaNode getClosestJavaNode(ICodeInfo codeInfo, int pos) {
		ICodeAnnotation ann = codeInfo.getCodeMetadata().getClosestUp(pos);
		return getJavaNodeByCodeAnnotation(codeInfo, ann);
	}

	@Nullable
	public JavaNode getEnclosingNode(ICodeInfo codeInfo, int pos) {
		ICodeNodeRef obj = codeInfo.getCodeMetadata().getNodeAt(pos);
		if (obj == null) {
			return null;
		}
		return getJavaNodeByRef(obj);
	}

	public void reloadCodeData() {
		root.notifyCodeDataListeners();
	}

	public JadxArgs getArgs() {
		return args;
	}

	public JadxPluginManager getPluginManager() {
		return pluginManager;
	}

	public IDecompileScheduler getDecompileScheduler() {
		return decompileScheduler;
	}

	public IJadxEvents events() {
		return events;
	}

	public void setEventsImpl(IJadxEvents eventsImpl) {
		this.events = eventsImpl;
	}

	public void addCustomCodeLoader(ICodeLoader customCodeLoader) {
		customCodeLoaders.add(customCodeLoader);
	}

	public List<ICodeLoader> getCustomCodeLoaders() {
		return customCodeLoaders;
	}

	public void addCustomResourcesLoader(CustomResourcesLoader loader) {
		if (customResourcesLoaders.contains(loader)) {
			return;
		}
		customResourcesLoaders.add(loader);
	}

	public List<CustomResourcesLoader> getCustomResourcesLoaders() {
		return customResourcesLoaders;
	}

	public void addCustomPass(JadxPass pass) {
		customPasses.computeIfAbsent(pass.getPassType(), l -> new ArrayList<>()).add(pass);
	}

	public ResourcesLoader getResourcesLoader() {
		return resourcesLoader;
	}

	public ZipReader getZipReader() {
		return zipReader;
	}

	public void addCloseable(Closeable closeable) {
		closeableList.add(closeable);
	}

	@Override
	public String toString() {
		return "jadx decompiler " + getVersion();
	}
}
