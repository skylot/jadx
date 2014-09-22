package jadx.tests.api.compiler;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.IOException;
import java.security.SecureClassLoader;

import static javax.tools.JavaFileObject.Kind;

public class ClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

	private JavaClassObject jClsObject;

	public ClassFileManager(StandardJavaFileManager standardManager) {
		super(standardManager);
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className,
	                                           Kind kind, FileObject sibling) throws IOException {
		jClsObject = new JavaClassObject(className, kind);
		return jClsObject;
	}

	@Override
	public ClassLoader getClassLoader(Location location) {
		return new SecureClassLoader() {
			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {
				byte[] clsBytes = jClsObject.getBytes();
				return super.defineClass(name, clsBytes, 0, clsBytes.length);
			}
		};
	}
}
