package jadx.tests.api.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;

import jadx.core.utils.files.FileUtils;

public class StaticCompiler {

	public static List<File> compile(List<File> files, File outDir, CompilerOptions options) throws IOException {
		int javaVersion = options.getJavaVersion();
		if (!JavaUtils.checkJavaVersion(javaVersion)) {
			throw new IllegalArgumentException("Current java version not meet requirement: "
					+ "current: " + JavaUtils.JAVA_VERSION_INT + ", required: " + javaVersion);
		}

		JavaCompiler compiler;
		if (options.isUseEclipseCompiler()) {
			compiler = new EclipseCompiler();
		} else {
			compiler = ToolProvider.getSystemJavaCompiler();
			if (compiler == null) {
				throw new IllegalStateException("Can not find compiler, please use JDK instead");
			}
		}
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
		Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(files);

		StaticFileManager staticFileManager = new StaticFileManager(fileManager, outDir);

		List<String> arguments = new ArrayList<>();
		arguments.add(options.isIncludeDebugInfo() ? "-g" : "-g:none");
		String javaVerStr = javaVersion <= 8 ? "1." + javaVersion : Integer.toString(javaVersion);
		arguments.add("-source");
		arguments.add(javaVerStr);
		arguments.add("-target");
		arguments.add(javaVerStr);
		arguments.addAll(options.getArguments());

		DiagnosticListener<? super JavaFileObject> diag = new DiagnosticListener<JavaFileObject>() {
			@Override
			public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
				System.out.println(diagnostic);
			}
		};
		CompilationTask task = compiler.getTask(null, staticFileManager, diag, arguments, null, compilationUnits);
		Boolean result = task.call();
		fileManager.close();
		if (Boolean.TRUE.equals(result)) {
			return staticFileManager.outputFiles();
		}
		return Collections.emptyList();
	}

	private static class StaticFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {
		private final List<File> files = new ArrayList<>();
		private final File outDir;

		protected StaticFileManager(StandardJavaFileManager fileManager, File outDir) {
			super(fileManager);
			this.outDir = outDir;
		}

		@Override
		public JavaFileObject getJavaFileForOutput(Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
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
		private final File file;

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
