package jadx.tests.api.compiler;

import java.security.SecureClassLoader;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

public class DynamicClassLoader extends SecureClassLoader {
	private final Map<String, JavaClassObject> clsMap = new ConcurrentHashMap<>();
	private final Map<String, Class<?>> clsCache = new ConcurrentHashMap<>();

	public void add(String className, JavaClassObject clsObject) {
		this.clsMap.put(className, clsObject);
	}

	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
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

	@Nullable
	public Class<?> replaceClass(String name) {
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

	public Collection<? extends JavaClassObject> getClassObjects() {
		return clsMap.values();
	}
}
