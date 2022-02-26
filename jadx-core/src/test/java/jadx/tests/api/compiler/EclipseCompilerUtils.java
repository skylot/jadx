package jadx.tests.api.compiler;

import javax.tools.JavaCompiler;

public class EclipseCompilerUtils {

	public static JavaCompiler newInstance() {
		if (!JavaUtils.checkJavaVersion(11)) {
			throw new IllegalArgumentException("Eclipse compiler build with Java 11");
		}
		try {
			Class<?> ecjCls = Class.forName("org.eclipse.jdt.internal.compiler.tool.EclipseCompiler");
			return (JavaCompiler) ecjCls.getConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Failed to init Eclipse compiler", e);
		}
	}
}
