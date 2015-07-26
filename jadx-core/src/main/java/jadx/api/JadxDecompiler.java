package jadx.api;

import jadx.core.Jadx;
import jadx.core.ProcessClass;
import jadx.core.codegen.CodeGen;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.dex.visitors.SaveCode;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.InputFile;
import jadx.core.xmlgen.BinaryXMLParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Jadx API usage example:
 * <pre><code>
 *  JadxDecompiler jadx = new JadxDecompiler();
 *  jadx.loadFile(new File("classes.dex"));
 *  jadx.setOutputDir(new File("out"));
 *  jadx.save();
 * </code></pre>
 * <p/>
 * Instead of 'save()' you can get list of decompiled classes:
 * <pre><code>
 *  for(JavaClass cls : jadx.getClasses()) {
 *      System.out.println(cls.getCode());
 *  }
 * </code></pre>
 */
public final class JadxDecompiler {
	private static final Logger LOG = LoggerFactory.getLogger(JadxDecompiler.class);

	private final IJadxArgs args;
	private final List<InputFile> inputFiles = new ArrayList<InputFile>();

	private File outDir;

	private RootNode root;
	private List<IDexTreeVisitor> passes;
	private CodeGen codeGen;

	private List<JavaClass> classes;
	private List<ResourceFile> resources;

	private BinaryXMLParser xmlParser;

	private Map<ClassNode, JavaClass> classesMap = new HashMap<ClassNode, JavaClass>();
	private Map<MethodNode, JavaMethod> methodsMap = new HashMap<MethodNode, JavaMethod>();
	private Map<FieldNode, JavaField> fieldsMap = new HashMap<FieldNode, JavaField>();

	public JadxDecompiler() {
		this(new DefaultJadxArgs());
	}

	public JadxDecompiler(IJadxArgs jadxArgs) {
		this.args = jadxArgs;
		this.outDir = jadxArgs.getOutDir();
		reset();
		init();
	}

	public void setOutputDir(File outDir) {
		this.outDir = outDir;
		init();
	}

	void init() {
		if (outDir == null) {
			outDir = new DefaultJadxArgs().getOutDir();
		}
		this.passes = Jadx.getPassesList(args, outDir);
		this.codeGen = new CodeGen(args);
	}

	void reset() {
		classes = null;
		resources = null;
		xmlParser = null;
		root = null;
		passes = null;
		codeGen = null;
	}

	public static String getVersion() {
		return Jadx.getVersion();
	}

	public void loadFile(File file) throws JadxException {
		loadFiles(Collections.singletonList(file));
	}

	public void loadFiles(List<File> files) throws JadxException {
		if (files.isEmpty()) {
			throw new JadxException("Empty file list");
		}
		inputFiles.clear();
		for (File file : files) {
			try {
				inputFiles.add(new InputFile(file));
			} catch (IOException e) {
				throw new JadxException("Error load file: " + file, e);
			}
		}
		parse();
	}

	public void save() {
		save(!args.isSkipSources(), !args.isSkipResources());
	}

	public void saveSources() {
		save(true, false);
	}

	public void saveResources() {
		save(false, true);
	}

	private void save(boolean saveSources, boolean saveResources) {
		try {
			ExecutorService ex = getSaveExecutor(saveSources, saveResources);
			ex.shutdown();
			ex.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			throw new JadxRuntimeException("Save interrupted", e);
		}
	}

	public ExecutorService getSaveExecutor() {
		return getSaveExecutor(!args.isSkipSources(), !args.isSkipResources());
	}

	private ExecutorService getSaveExecutor(boolean saveSources, boolean saveResources) {
		if (root == null) {
			throw new JadxRuntimeException("No loaded files");
		}
		int threadsCount = args.getThreadsCount();
		LOG.debug("processing threads count: {}", threadsCount);

		LOG.info("processing ...");
		ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
		if (saveSources) {
			for (final JavaClass cls : getClasses()) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						cls.decompile();
						SaveCode.save(outDir, args, cls.getClassNode());
					}
				});
			}
		}
		if (saveResources) {
			for (final ResourceFile resourceFile : getResources()) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						if (ResourceType.isSupportedForUnpack(resourceFile.getType())) {
							CodeWriter cw = resourceFile.getContent();
							if (cw != null) {
								cw.save(new File(outDir, resourceFile.getName()));
							}
						}
					}
				});
			}
		}
		return executor;
	}

	public List<JavaClass> getClasses() {
		if (root == null) {
			return Collections.emptyList();
		}
		if (classes == null) {
			List<ClassNode> classNodeList = root.getClasses(false);
			List<JavaClass> clsList = new ArrayList<JavaClass>(classNodeList.size());
			classesMap.clear();
			for (ClassNode classNode : classNodeList) {
				JavaClass javaClass = new JavaClass(classNode, this);
				clsList.add(javaClass);
				classesMap.put(classNode, javaClass);
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
			resources = new ResourcesLoader(this).load(inputFiles);
		}
		return resources;
	}

	public List<JavaPackage> getPackages() {
		List<JavaClass> classList = getClasses();
		if (classList.isEmpty()) {
			return Collections.emptyList();
		}
		Map<String, List<JavaClass>> map = new HashMap<String, List<JavaClass>>();
		for (JavaClass javaClass : classList) {
			String pkg = javaClass.getPackage();
			List<JavaClass> clsList = map.get(pkg);
			if (clsList == null) {
				clsList = new ArrayList<JavaClass>();
				map.put(pkg, clsList);
			}
			clsList.add(javaClass);
		}
		List<JavaPackage> packages = new ArrayList<JavaPackage>(map.size());
		for (Map.Entry<String, List<JavaClass>> entry : map.entrySet()) {
			packages.add(new JavaPackage(entry.getKey(), entry.getValue()));
		}
		Collections.sort(packages);
		for (JavaPackage pkg : packages) {
			Collections.sort(pkg.getClasses(), new Comparator<JavaClass>() {
				@Override
				public int compare(JavaClass o1, JavaClass o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
		}
		return Collections.unmodifiableList(packages);
	}

	public int getErrorsCount() {
		if (root == null) {
			return 0;
		}
		return root.getErrorsCounter().getErrorCount();
	}

	public void printErrorsReport() {
		if (root == null) {
			return;
		}
		root.getErrorsCounter().printReport();
	}

	void parse() throws DecodeException {
		reset();
		init();

		root = new RootNode(args);
		LOG.info("loading ...");
		root.load(inputFiles);

		root.initClassPath();
		root.loadResources(getResources());
		root.initAppResClass();

		initVisitors();
	}

	private void initVisitors() {
		for (IDexTreeVisitor pass : passes) {
			try {
				pass.init(root);
			} catch (Exception e) {
				LOG.error("Visitor init failed: {}", pass.getClass().getSimpleName(), e);
			}
		}
	}

	void processClass(ClassNode cls) {
		ProcessClass.process(cls, passes, codeGen);
	}

	RootNode getRoot() {
		return root;
	}

	BinaryXMLParser getXmlParser() {
		if (xmlParser == null) {
			xmlParser = new BinaryXMLParser(root);
		}
		return xmlParser;
	}

	Map<ClassNode, JavaClass> getClassesMap() {
		return classesMap;
	}

	Map<MethodNode, JavaMethod> getMethodsMap() {
		return methodsMap;
	}

	Map<FieldNode, JavaField> getFieldsMap() {
		return fieldsMap;
	}

	public IJadxArgs getArgs() {
		return args;
	}

	@Override
	public String toString() {
		return "jadx decompiler " + getVersion();
	}
}
