package jadx.tests.api.compiler;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.IOException;
import java.security.SecureClassLoader;
import java.util.HashMap;
import java.util.Map;

import static javax.tools.JavaFileObject.Kind;

public class ClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

	private DynamicClassLoader classLoader;

	public ClassFileManager(StandardJavaFileManager standardManager) {
		super(standardManager);
		classLoader = new DynamicClassLoader();
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className,
			Kind kind, FileObject sibling) throws IOException {
		JavaClassObject clsObject = new JavaClassObject(className, kind);
		classLoader.getClsMap().put(className, clsObject);
		return clsObject;
	}

	@Override
	public ClassLoader getClassLoader(Location location) {
		return classLoader;
	}

	private class DynamicClassLoader extends SecureClassLoader {
		private final Map<String, JavaClassObject> clsMap = new HashMap<>();
		private final Map<String, Class<?>> clsCache = new HashMap<>();

		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			Class<?> cls = replaceClass(name);
			if (cls != null) {
				return cls;
			}
			return super.findClass(name);
		}

		public Class<?> loadClass(String name) throws ClassNotFoundException {
			Class<?> cls = replaceClass(name);
			if (cls != null) {
				return cls;
			}
			return super.loadClass(name);
		}

		public Class<?> replaceClass(String name) throws ClassNotFoundException {
			Class<?> cacheCls = clsCache.get(name);
			if (cacheCls != null) {
				return cacheCls;
			}
			JavaClassObject clsObject = clsMap.get(name);
			if (clsObject == null) {
				return null;
			}
			byte[] clsBytes = clsObject.getBytes();
			Class<?> cls = super.defineClass(name, clsBytes, 0, clsBytes.length);
			clsCache.put(name, cls);
			return cls;
		}

		public Map<String, JavaClassObject> getClsMap() {
			return clsMap;
		}
	}
}
