package jadx.api;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginManager;
import jadx.api.plugins.input.JadxInputPlugin;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.core.Jadx;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.SaveCode;
import jadx.core.export.ExportGradleProject;
import jadx.core.utils.DecompilerScheduler;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.core.xmlgen.BinaryXMLParser;
import jadx.core.xmlgen.ProtoXMLParser;
import jadx.core.xmlgen.ResContainer;
import jadx.core.xmlgen.ResourcesSaver;

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
	private final JadxPluginManager pluginManager = new JadxPluginManager();
	private final List<ILoadResult> loadedInputs = new ArrayList<>();

	private RootNode root;
	private List<JavaClass> classes;
	private List<ResourceFile> resources;

	private BinaryXMLParser binaryXmlParser;
	private ProtoXMLParser protoXmlParser;

	private final Map<ClassNode, JavaClass> classesMap = new ConcurrentHashMap<>();
	private final Map<MethodNode, JavaMethod> methodsMap = new ConcurrentHashMap<>();
	private final Map<FieldNode, JavaField> fieldsMap = new ConcurrentHashMap<>();

	private final IDecompileScheduler decompileScheduler = new DecompilerScheduler();

	private final List<ILoadResult> customLoads = new ArrayList<>();

	public JadxDecompiler() {
		this(new JadxArgs());
	}

	public JadxDecompiler(JadxArgs args) {
		this.args = args;
	}

	public void load() {
		reset();
		JadxArgsValidator.validate(this);
		LOG.info("loading ...");
		loadPlugins(args);
		loadInputFiles();

		root = new RootNode(args);
		root.loadClasses(loadedInputs);
		root.initClassPath();
		root.loadResources(getResources());
		root.runPreDecompileStage();
		root.initPasses();
	}

	private void loadInputFiles() {
		loadedInputs.clear();
		List<Path> inputPaths = Utils.collectionMap(args.getInputFiles(), File::toPath);
		List<Path> inputFiles = FileUtils.expandDirs(inputPaths);
		long start = System.currentTimeMillis();
		for (JadxInputPlugin inputPlugin : pluginManager.getInputPlugins()) {
			ILoadResult loadResult = inputPlugin.loadFiles(inputFiles);
			if (loadResult != null && !loadResult.isEmpty()) {
				loadedInputs.add(loadResult);
			}
		}
		loadedInputs.addAll(customLoads);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Loaded using {} inputs plugin in {} ms", loadedInputs.size(), System.currentTimeMillis() - start);
		}
	}

	public void addCustomLoad(ILoadResult customLoad) {
		customLoads.add(customLoad);
	}

	public List<ILoadResult> getCustomLoads() {
		return customLoads;
	}

	private void reset() {
		root = null;
		classes = null;
		resources = null;
		binaryXmlParser = null;
		protoXmlParser = null;

		classesMap.clear();
		methodsMap.clear();
		fieldsMap.clear();
	}

	@Override
	public void close() {
		reset();
		closeInputs();
		args.close();
	}

	private void closeInputs() {
		loadedInputs.forEach(load -> {
			try {
				load.close();
			} catch (Exception e) {
				LOG.error("Failed to close input", e);
			}
		});
		loadedInputs.clear();
	}

	private void loadPlugins(JadxArgs args) {
		pluginManager.providesSuggestion("java-input", args.isUseDxInput() ? "java-convert" : "java-input");
		pluginManager.load();
		if (LOG.isDebugEnabled()) {
			LOG.debug("Resolved plugins: {}", Utils.collectionMap(pluginManager.getResolvedPlugins(),
					p -> p.getPluginInfo().getPluginId()));
		}
		Map<String, String> pluginOptions = args.getPluginOptions();
		if (!pluginOptions.isEmpty()) {
			LOG.debug("Applying plugin options: {}", pluginOptions);
			for (JadxPluginOptions plugin : pluginManager.getPluginsWithOptions()) {
				try {
					plugin.setOptions(pluginOptions);
				} catch (Exception e) {
					String pluginId = plugin.getPluginInfo().getPluginId();
					throw new JadxRuntimeException("Failed to apply options for plugin: " + pluginId, e);
				}
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
		ThreadPoolExecutor ex = (ThreadPoolExecutor) getSaveExecutor();
		ex.shutdown();
		try {
			long total = ex.getTaskCount();
			while (ex.isTerminating()) {
				long done = ex.getCompletedTaskCount();
				listener.progress(done, total);
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

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private void save(boolean saveSources, boolean saveResources) {
		ExecutorService ex = getSaveExecutor(saveSources, saveResources);
		ex.shutdown();
		try {
			ex.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			LOG.error("Save interrupted", e);
			Thread.currentThread().interrupt();
		}
	}

	public ExecutorService getSaveExecutor() {
		return getSaveExecutor(!args.isSkipSources(), !args.isSkipResources());
	}

	public List<Runnable> getSaveTasks() {
		return getSaveTasks(!args.isSkipSources(), !args.isSkipResources());
	}

	private ExecutorService getSaveExecutor(boolean saveSources, boolean saveResources) {
		int threadsCount = args.getThreadsCount();
		LOG.debug("processing threads count: {}", threadsCount);
		LOG.info("processing ...");
		ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
		List<Runnable> tasks = getSaveTasks(saveSources, saveResources);
		tasks.forEach(executor::execute);
		return executor;
	}

	private List<Runnable> getSaveTasks(boolean saveSources, boolean saveResources) {
		if (root == null) {
			throw new JadxRuntimeException("No loaded files");
		}
		File sourcesOutDir;
		File resOutDir;
		if (args.isExportAsGradleProject()) {
			ResourceFile androidManifest = resources.stream()
					.filter(resourceFile -> resourceFile.getType() == ResourceType.MANIFEST)
					.findFirst()
					.orElseThrow(IllegalStateException::new);

			ResContainer strings = resources.stream()
					.filter(resourceFile -> resourceFile.getType() == ResourceType.ARSC)
					.findFirst()
					.orElseThrow(IllegalStateException::new)
					.loadContent()
					.getSubFiles()
					.stream()
					.filter(resContainer -> resContainer.getFileName().contains("strings.xml"))
					.findFirst()
					.orElseThrow(IllegalStateException::new);

			ExportGradleProject export = new ExportGradleProject(root, args.getOutDir(), androidManifest, strings);
			export.init();
			sourcesOutDir = export.getSrcOutDir();
			resOutDir = export.getResOutDir();
		} else {
			sourcesOutDir = args.getOutDirSrc();
			resOutDir = args.getOutDirRes();
		}
		List<Runnable> tasks = new ArrayList<>();
		// save resources first because decompilation can hang or fail
		if (saveResources) {
			appendResourcesSaveTasks(tasks, resOutDir);
		}
		if (saveSources) {
			appendSourcesSave(tasks, sourcesOutDir);
		}
		return tasks;
	}

	private void appendResourcesSaveTasks(List<Runnable> tasks, File outDir) {
		if (args.isSkipFilesSave()) {
			return;
		}
		Set<String> inputFileNames = args.getInputFiles().stream().map(File::getAbsolutePath).collect(Collectors.toSet());
		for (ResourceFile resourceFile : getResources()) {
			if (resourceFile.getType() != ResourceType.ARSC
					&& inputFileNames.contains(resourceFile.getOriginalName())) {
				// ignore resource made from input file
				continue;
			}
			tasks.add(new ResourcesSaver(outDir, resourceFile));
		}
	}

	private void appendSourcesSave(List<Runnable> tasks, File outDir) {
		Predicate<String> classFilter = args.getClassFilter();
		List<JavaClass> classes = getClasses();
		List<JavaClass> processQueue = new ArrayList<>(classes.size());
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
			processQueue.add(cls);
		}
		List<List<JavaClass>> batches;
		try {
			batches = decompileScheduler.buildBatches(processQueue);
		} catch (Exception e) {
			throw new JadxRuntimeException("Decompilation batches build failed", e);
		}
		for (List<JavaClass> decompileBatch : batches) {
			tasks.add(() -> {
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

	public List<ResourceFile> getResources() {
		if (resources == null) {
			if (root == null) {
				return Collections.emptyList();
			}
			resources = new ResourcesLoader(this).load();
		}
		return resources;
	}

	public List<JavaPackage> getPackages() {
		List<JavaClass> classList = getClasses();
		if (classList.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String, List<JavaClass>> map = new HashMap<>();
		for (JavaClass javaClass : classList) {
			String pkg = javaClass.getPackage();
			List<JavaClass> clsList = map.computeIfAbsent(pkg, k -> new ArrayList<>());
			clsList.add(javaClass);
		}
		List<JavaPackage> packages = new ArrayList<>(map.size());
		for (Map.Entry<String, List<JavaClass>> entry : map.entrySet()) {
			packages.add(new JavaPackage(entry.getKey(), entry.getValue()));
		}
		Collections.sort(packages);
		for (JavaPackage pkg : packages) {
			pkg.getClasses().sort(Comparator.comparing(JavaClass::getName, String.CASE_INSENSITIVE_ORDER));
		}
		return Collections.unmodifiableList(packages);
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

	synchronized BinaryXMLParser getBinaryXmlParser() {
		if (binaryXmlParser == null) {
			binaryXmlParser = new BinaryXMLParser(root);
		}
		return binaryXmlParser;
	}

	synchronized ProtoXMLParser getProtoXmlParser() {
		if (protoXmlParser == null) {
			protoXmlParser = new ProtoXMLParser(root);
		}
		return protoXmlParser;
	}

	/**
	 * Get JavaClass by ClassNode without loading and decompilation
	 */
	@ApiStatus.Internal
	JavaClass convertClassNode(ClassNode cls) {
		return classesMap.compute(cls, (node, prevJavaCls) -> {
			if (prevJavaCls != null && prevJavaCls.getClassNode() == cls) {
				// keep previous variable
				return prevJavaCls;
			}
			if (cls.isInner()) {
				return new JavaClass(cls, convertClassNode(cls.getParentClass()));
			}
			return new JavaClass(cls, this);
		});
	}

	@ApiStatus.Internal
	JavaField convertFieldNode(FieldNode field) {
		return fieldsMap.computeIfAbsent(field, fldNode -> {
			JavaClass parentCls = convertClassNode(fldNode.getParentClass());
			return new JavaField(parentCls, fldNode);
		});
	}

	@ApiStatus.Internal
	JavaMethod convertMethodNode(MethodNode method) {
		return methodsMap.computeIfAbsent(method, mthNode -> {
			ClassNode parentCls = mthNode.getParentClass();
			return new JavaMethod(convertClassNode(parentCls), mthNode);
		});
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

	@Nullable
	private JavaVariable resolveVarNode(VarNode varNode) {
		MethodNode mthNode = varNode.getMth();
		JavaMethod mth = convertMethodNode(mthNode);
		if (mth == null) {
			return null;
		}
		return new JavaVariable(mth, varNode);
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

	@Override
	public String toString() {
		return "jadx decompiler " + getVersion();
	}
}
