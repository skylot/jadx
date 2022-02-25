package jadx.tests.api.compiler;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import static javax.tools.JavaFileObject.Kind;

public class ClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> implements Closeable {

	private final DynamicClassLoader classLoader;

	public ClassFileManager(StandardJavaFileManager standardManager) {
		super(standardManager);
		classLoader = new DynamicClassLoader();
	}

	public List<JavaFileObject> getJavaFileObjectsFromFiles(List<File> sourceFiles) {
		List<JavaFileObject> list = new ArrayList<>();
		for (JavaFileObject javaFileObject : fileManager.getJavaFileObjectsFromFiles(sourceFiles)) {
			list.add(javaFileObject);
		}
		return list;
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) {
		JavaClassObject clsObject = new JavaClassObject(className, kind);
		classLoader.add(className, clsObject);
		return clsObject;
	}

	@Override
	public ClassLoader getClassLoader(Location location) {
		return classLoader;
	}

	public DynamicClassLoader getClassLoader() {
		return classLoader;
	}
}
