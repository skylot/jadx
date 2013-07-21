package jadx.api;

import jadx.core.Jadx;
import jadx.core.ProcessClass;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.dex.visitors.SaveCode;
import jadx.core.utils.ErrorsCounter;
import jadx.core.utils.exceptions.CodegenException;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.files.InputFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Decompiler {
	private static final Logger LOG = LoggerFactory.getLogger(Decompiler.class);

	private final IJadxArgs args;
	private final List<InputFile> inputFiles = new ArrayList<InputFile>();

	private RootNode root;
	private List<IDexTreeVisitor> passes;
	private int errorsCount;

	public Decompiler(IJadxArgs jadxArgs) {
		this.args = jadxArgs;
		this.passes = Jadx.getPassesList(args);
	}

	public void processAndSaveAll() {
		try {
			loadInput();
			parseDex();
			saveAll();
		} catch (Throwable e) {
			LOG.error("jadx error:", e);
		} finally {
			errorsCount = ErrorsCounter.getErrorCount();
			if (errorsCount != 0)
				ErrorsCounter.printReport();
		}
	}

	public void loadFile(File file) throws IOException, DecodeException {
		setInput(file);
		parseDex();
	}

	public List<JavaClass> getClasses() {
		List<JavaClass> classes = new ArrayList<JavaClass>(root.getClasses().size());
		for (ClassNode classNode : root.getClasses()) {
			classes.add(new JavaClass(this, classNode));
		}
		return classes;
	}

	public List<JavaPackage> getPackages() {
		List<JavaClass> classes = getClasses();
		Map<String, List<JavaClass>> map = new HashMap<String, List<JavaClass>>();
		for (JavaClass javaClass : classes) {
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
		return packages;
	}

	public void saveAll() throws InterruptedException {
		int threadsCount = args.getThreadsCount();
		LOG.debug("processing threads count: {}", threadsCount);

		ArrayList<IDexTreeVisitor> passList = new ArrayList<IDexTreeVisitor>(passes);
		SaveCode savePass = new SaveCode(args);
		passList.add(savePass);

		LOG.info("processing ...");
		ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
		for (ClassNode cls : root.getClasses()) {
			if (cls.getCode() == null) {
				ProcessClass job = new ProcessClass(cls, passList);
				executor.execute(job);
			} else {
				try {
					savePass.visit(cls);
				} catch (CodegenException e) {
					LOG.error("Can't save class {}", cls, e);
				}
			}
		}
		executor.shutdown();
		executor.awaitTermination(100, TimeUnit.DAYS);
	}

	private void loadInput() throws IOException, DecodeException {
		inputFiles.clear();
		for (File file : args.getInput()) {
			inputFiles.add(new InputFile(file));
		}
	}

	private void setInput(File file) throws IOException, DecodeException {
		inputFiles.clear();
		inputFiles.add(new InputFile(file));
	}

	private void parseDex() throws DecodeException {
		ClassInfo.clearCache();
		ErrorsCounter.reset();

		root = new RootNode(args, inputFiles);
		LOG.info("loading ...");
		root.load();
		root.init();
	}

	void processClass(ClassNode cls) {
		try {
			ProcessClass job = new ProcessClass(cls, passes);
			LOG.info("processing class {} ...", cls);
			job.run();
		} catch (Throwable e) {
			LOG.error("Process class error", e);
		}
	}

	public int getErrorsCount() {
		return errorsCount;
	}
}
