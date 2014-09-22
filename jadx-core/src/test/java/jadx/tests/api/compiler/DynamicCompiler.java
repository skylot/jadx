package jadx.tests.api.compiler;

import jadx.core.dex.nodes.ClassNode;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static javax.tools.JavaCompiler.CompilationTask;

public class DynamicCompiler {

	private final ClassNode clsNode;

	private JavaFileManager fileManager;

	private Object instance;

	public DynamicCompiler(ClassNode clsNode) {
		this.clsNode = clsNode;
	}

	public boolean compile() throws Exception {
		String fullName = clsNode.getFullName();
		String code = clsNode.getCode().toString();

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));

		List<JavaFileObject> jFiles = new ArrayList<JavaFileObject>(1);
		jFiles.add(new CharSequenceJavaFileObject(fullName, code));

		CompilationTask compilerTask = compiler.getTask(null, fileManager, null, null, null, jFiles);
		return Boolean.TRUE.equals(compilerTask.call());
	}

	private void makeInstance() throws Exception {
		String fullName = clsNode.getFullName();
		instance = fileManager.getClassLoader(null).loadClass(fullName).newInstance();
		if (instance == null) {
			throw new NullPointerException("Instantiation failed");
		}
	}

	private Object getInstance() throws Exception {
		if (instance == null) {
			makeInstance();
		}
		return instance;
	}

	public Method getMethod(String method, Class[] types) throws Exception {
		return getInstance().getClass().getMethod(method, types);
	}

	public Object invoke(Method mth, Object... args) throws Exception {
		return mth.invoke(getInstance(), args);
	}

	public Object invoke(String method) throws Exception {
		return invoke(method, new Class[0]);
	}

	public Object invoke(String method, Class[] types, Object... args) throws Exception {
		Method mth = getMethod(method, types);
		return invoke(mth, args);
	}
}
