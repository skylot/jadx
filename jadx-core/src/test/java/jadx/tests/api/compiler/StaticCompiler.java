package jadx.tests.api.compiler;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jadx.core.utils.files.FileUtils;

import static javax.tools.JavaCompiler.CompilationTask;

public class StaticCompiler {

	private static final List<String> COMMON_ARGS = Arrays.asList("-source 1.8 -target 1.8".split(" "));

	public static List<File> compile(List<File> files, File outDir, boolean includeDebugInfo) throws IOException {

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(files);

		StaticFileManager staticFileManager = new StaticFileManager(fileManager, outDir);

		List<String> options = new ArrayList<>();
		options.add(includeDebugInfo ? "-g" : "-g:none");
		options.addAll(COMMON_ARGS);
		CompilationTask task = compiler.getTask(null, staticFileManager, null, options, null, compilationUnits);
		Boolean result = task.call();
		fileManager.close();
		if (Boolean.TRUE.equals(result)) {
			return staticFileManager.outputFiles();
		}
		return Collections.emptyList();
	}

	private static class StaticFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
		private List<File> files = new ArrayList<>();
		private File outDir;

		protected StaticFileManager(StandardJavaFileManager fileManager, File outDir) {
			super(fileManager);
			this.outDir = outDir;
		}

		@Override
		public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind,
				FileObject sibling) throws IOException {
			if (kind == JavaFileObject.Kind.CLASS) {
				File file = new File(outDir, className.replace('.', '/') + ".class");
				files.add(file);
				return new ClassFileObject(file, kind);
			}
			throw new UnsupportedOperationException("Can't save location with kind: " + kind);
		}

		public List<File> outputFiles() {
			return files;
		}
	}

	private static class ClassFileObject extends SimpleJavaFileObject {
		private File file;

		protected ClassFileObject(File file, Kind kind) {
			super(file.toURI(), kind);
			this.file = file;
		}

		@Override
		public OutputStream openOutputStream() throws IOException {
			FileUtils.makeDirsForFile(file);
			return new FileOutputStream(file);
		}
	}
}
