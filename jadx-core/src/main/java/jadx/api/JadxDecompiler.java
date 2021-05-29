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

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginManager;
import jadx.api.plugins.input.JadxInputPlugin;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.core.Jadx;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.nodes.LineAttrNode;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.nodes.VariableNode;
import jadx.core.dex.visitors.SaveCode;
import jadx.core.export.ExportGradleProject;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.BinaryXMLParser;
import jadx.core.xmlgen.ProtoXMLParser;
import jadx.core.xmlgen.ResContainer;
import jadx.core.xmlgen.ResourcesSaver;

/**
 * Jadx API usage example:
 *
 * <pre>
 * <code>
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

	public JadxDecompiler() {
		this(new JadxArgs());
	}

	public JadxDecompiler(JadxArgs args) {
		this.args = args;
	}

	public void load() {
		reset();
		JadxArgsValidator.validate(args);
		LOG.info("loading ...");
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
		for (JadxInputPlugin inputPlugin : pluginManager.getInputPlugins()) {
			ILoadResult loadResult = inputPlugin.loadFiles(inputPaths);
			if (loadResult != null && !loadResult.isEmpty()) {
				loadedInputs.add(loadResult);
			}
		}
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

		closeInputs();
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

	@Override
	public void close() {
		reset();
	}

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
		if (saveSources) {
			appendSourcesSave(tasks, sourcesOutDir);
		}
		if (saveResources) {
			appendResourcesSaveTasks(tasks, resOutDir);
		}
		return tasks;
	}

	private void appendResourcesSaveTasks(List<Runnable> tasks, File outDir) {
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
		for (JavaClass cls : getClasses()) {
			if (cls.getClassNode().contains(AFlag.DONT_GENERATE)) {
				continue;
			}
			if (classFilter != null && !classFilter.test(cls.getFullName())) {
				continue;
			}
			tasks.add(() -> {
				try {
					ICodeInfo code = cls.getCodeInfo();
					SaveCode.save(outDir, cls.getClassNode(), code);
				} catch (Exception e) {
					LOG.error("Error saving class: {}", cls.getFullName(), e);
				}
			});
		}
	}

	public List<JavaClass> getClasses() {
		if (root == null) {
			return Collections.emptyList();
		}
		if (classes == null) {
			List<ClassNode> classNodeList = root.getClasses(false);
			List<JavaClass> clsList = new ArrayList<>(classNodeList.size());
			classesMap.clear();
			for (ClassNode classNode : classNodeList) {
				if (!classNode.contains(AFlag.DONT_GENERATE)) {
					JavaClass javaClass = new JavaClass(classNode, this);
					clsList.add(javaClass);
					classesMap.put(classNode, javaClass);
				}
			}
			classes = Collections.unmodifiableList(clsList);
		}
		return classes;
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

	private void loadJavaClass(JavaClass javaClass) {
		javaClass.getMethods().forEach(mth -> methodsMap.put(mth.getMethodNode(), mth));
		javaClass.getFields().forEach(fld -> fieldsMap.put(fld.getFieldNode(), fld));

		for (JavaClass innerCls : javaClass.getInnerClasses()) {
			classesMap.put(innerCls.getClassNode(), innerCls);
			loadJavaClass(innerCls);
		}
	}

	@Nullable("For not generated classes")
	private JavaClass getJavaClassByNode(ClassNode cls) {
		JavaClass javaClass = classesMap.get(cls);
		if (javaClass != null) {
			return javaClass;
		}
		// load parent class if inner
		ClassNode parentClass = cls.getTopParentClass();
		if (parentClass.contains(AFlag.DONT_GENERATE)) {
			return null;
		}
		if (parentClass != cls) {
			JavaClass parentJavaClass = classesMap.get(parentClass);
			if (parentJavaClass == null) {
				getClasses();
				parentJavaClass = classesMap.get(parentClass);
			}
			loadJavaClass(parentJavaClass);
			javaClass = classesMap.get(cls);
			if (javaClass != null) {
				return javaClass;
			}
		}
		// class or parent classes can be excluded from generation
		if (cls.hasNotGeneratedParent()) {
			return null;
		}
		throw new JadxRuntimeException("JavaClass not found by ClassNode: " + cls);
	}

	@Nullable
	private JavaMethod getJavaMethodByNode(MethodNode mth) {
		JavaMethod javaMethod = methodsMap.get(mth);
		if (javaMethod != null) {
			return javaMethod;
		}
		// parent class not loaded yet
		JavaClass javaClass = getJavaClassByNode(mth.getParentClass().getTopParentClass());
		if (javaClass == null) {
			return null;
		}
		loadJavaClass(javaClass);
		javaMethod = methodsMap.get(mth);
		if (javaMethod != null) {
			return javaMethod;
		}
		if (mth.getParentClass().hasNotGeneratedParent()) {
			return null;
		}
		throw new JadxRuntimeException("JavaMethod not found by MethodNode: " + mth);
	}

	@Nullable
	private JavaField getJavaFieldByNode(FieldNode fld) {
		JavaField javaField = fieldsMap.get(fld);
		if (javaField != null) {
			return javaField;
		}
		// parent class not loaded yet
		JavaClass javaClass = getJavaClassByNode(fld.getParentClass().getTopParentClass());
		if (javaClass == null) {
			return null;
		}
		loadJavaClass(javaClass);
		javaField = fieldsMap.get(fld);
		if (javaField != null) {
			return javaField;
		}
		if (fld.getParentClass().hasNotGeneratedParent()) {
			return null;
		}
		throw new JadxRuntimeException("JavaField not found by FieldNode: " + fld);
	}

	@Nullable
	public JavaClass searchJavaClassByOrigFullName(String fullName) {
		return getRoot().getClasses().stream()
				.filter(cls -> cls.getClassInfo().getFullName().equals(fullName))
				.findFirst()
				.map(this::getJavaClassByNode)
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
				return getJavaClassByNode(node.getTopParentClass());
			} else {
				return getJavaClassByNode(node);
			}
		}
		return null;
	}

	@Nullable
	public JavaClass searchJavaClassByAliasFullName(String fullName) {
		return getRoot().getClasses().stream()
				.filter(cls -> cls.getClassInfo().getAliasFullName().equals(fullName))
				.findFirst()
				.map(this::getJavaClassByNode)
				.orElse(null);
	}

	@Nullable
	JavaNode convertNode(Object obj) {
		if (!(obj instanceof LineAttrNode)) {
			return null;
		}
		LineAttrNode node = (LineAttrNode) obj;
		if (node.contains(AFlag.DONT_GENERATE)) {
			return null;
		}
		if (obj instanceof ClassNode) {
			return getJavaClassByNode((ClassNode) obj);
		}
		if (obj instanceof MethodNode) {
			return getJavaMethodByNode(((MethodNode) obj));
		}
		if (obj instanceof FieldNode) {
			return getJavaFieldByNode((FieldNode) obj);
		}
		if (obj instanceof VariableNode) {
			VariableNode varNode = (VariableNode) obj;
			return new JavaVariable(getJavaClassByNode(varNode.getClassNode().getTopParentClass()), varNode);
		}
		throw new JadxRuntimeException("Unexpected node type: " + obj);
	}

	List<JavaNode> convertNodes(Collection<?> nodesList) {
		return nodesList.stream()
				.map(this::convertNode)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	@Nullable
	public JavaNode getJavaNodeAtPosition(ICodeInfo codeInfo, int line, int offset) {
		Map<CodePosition, Object> map = codeInfo.getAnnotations();
		if (map.isEmpty()) {
			return null;
		}
		Object obj = map.get(new CodePosition(line, offset));
		if (obj == null) {
			return null;
		}
		return convertNode(obj);
	}

	@Nullable
	public CodePosition getDefinitionPosition(JavaNode javaNode) {
		JavaClass jCls = javaNode.getTopParentClass();
		jCls.decompile();
		int defLine = javaNode.getDecompiledLine();
		if (defLine == 0) {
			return null;
		}
		return new CodePosition(defLine, 0, javaNode.getDefPos());
	}

	public JadxArgs getArgs() {
		return args;
	}

	@Override
	public String toString() {
		return "jadx decompiler " + getVersion();
	}
}
