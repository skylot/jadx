package jadx.tests.api.compiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompilerOptions {
	private boolean includeDebugInfo = true;
	private boolean useEclipseCompiler = false;
	private int javaVersion = 8;

	List<String> arguments = Collections.emptyList();

	public boolean isIncludeDebugInfo() {
		return includeDebugInfo;
	}

	public void setIncludeDebugInfo(boolean includeDebugInfo) {
		this.includeDebugInfo = includeDebugInfo;
	}

	public boolean isUseEclipseCompiler() {
		return useEclipseCompiler;
	}

	public void setUseEclipseCompiler(boolean useEclipseCompiler) {
		this.useEclipseCompiler = useEclipseCompiler;
	}

	public int getJavaVersion() {
		return javaVersion;
	}

	public void setJavaVersion(int javaVersion) {
		this.javaVersion = javaVersion;
	}

	public List<String> getArguments() {
		return Collections.unmodifiableList(arguments);
	}

	public void addArgument(String argName) {
		if (arguments.isEmpty()) {
			arguments = new ArrayList<>();
		}
		arguments.add(argName);
	}

	public void addArgument(String argName, String argValue) {
		if (arguments.isEmpty()) {
			arguments = new ArrayList<>();
		}
		arguments.add(argName);
		arguments.add(argValue);
	}
}
