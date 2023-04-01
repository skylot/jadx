package jadx.api;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import jadx.api.plugins.input.ICodeLoader;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.api.plugins.pass.JadxPass;
import jadx.api.plugins.pass.types.JadxAfterLoadPass;
import jadx.api.plugins.pass.types.JadxPassType;
import jadx.core.Jadx;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.SaveCode;
import jadx.core.export.ExportGradleProject;
import jadx.core.plugins.JadxPluginManager;
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
	private final JadxPluginManager pluginManager = new JadxPluginManager(this);
	private final List<ICodeLoader> loadedInputs = new ArrayList<>();

	private RootNode root;
	private List<JavaClass> classes;
	private List<ResourceFile> resources;

	private BinaryXMLParser binaryXmlParser;
	private ProtoXMLParser protoXmlParser;

	private final IDecompileScheduler decompileScheduler = new DecompilerScheduler();

	private final List<ICodeLoader> customCodeLoaders = new ArrayList<>();
	private final Map<JadxPassType, List<JadxPass>> customPasses = new HashMap<>();

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
		loadPlugins();
		loadInputFiles();

		root = new RootNode(args);
		root.init();
		root.setDecompilerRef(this);
		root.mergePasses(customPasses);
		root.loadClasses(loadedInputs);
		root.initClassPath();
		root.loadResources(getResources());
		root.runPreDecompileStage();
		root.initPasses();
		loadFinished();
	}

	public void reloadPasses() {
		LOG.info("reloading (passes only) ...");
		customPasses.clear();
		root.resetPasses();
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
		for (JadxCodeInput codeLoader : pluginManager.getCodeInputs()) {
			ICodeLoader loader = codeLoader.loadFiles(inputFiles);
			if (loader != null && !loader.isEmpty()) {
				loadedInputs.add(loader);
			}
		}
		loadedInputs.addAll(customCodeLoaders);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Loaded using {} inputs plugin in {} ms", loadedInputs.size(), System.currentTimeMillis() - start);
		}
	}

	private void reset() {
		root = null;
		classes = null;
		resources = null;
		binaryXmlParser = null;
		protoXmlParser = null;
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

	private void loadPlugins() {
		pluginManager.providesSuggestion("java-input", args.isUseDxInput() ? "java-convert" : "java-input");
		pluginManager.load();
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

	private void loadFinished() {
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
		// process AndroidManifest.xml first to load complete resource ids table
		for (ResourceFile resourceFile : getResources()) {
			if (resourceFile.getType() == ResourceType.MANIFEST) {
				new ResourcesSaver(outDir, resourceFile).run();
			}
		}

		Set<String> inputFileNames = args.getInputFiles().stream().map(File::getAbsolutePath).collect(Collectors.toSet());
		for (ResourceFile resourceFile : getResources()) {
			ResourceType resType = resourceFile.getType();
			if (resType == ResourceType.MANIFEST) {
				// already processed
				continue;
			}
			if (resType != ResourceType.ARSC
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

	public synchronized List<ResourceFile> getResources() {
		if (resources == null) {
			if (root == null) {
				return Collections.emptyList();
			}
			resources = new ResourcesLoader(this).load();
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

	public void addCustomCodeLoader(ICodeLoader customCodeLoader) {
		customCodeLoaders.add(customCodeLoader);
	}

	public List<ICodeLoader> getCustomCodeLoaders() {
		return customCodeLoaders;
	}

	public void addCustomPass(JadxPass pass) {
		customPasses.computeIfAbsent(pass.getPassType(), l -> new ArrayList<>()).add(pass);
	}

	@Override
	public String toString() {
		return "jadx decompiler " + getVersion();
	}
}
