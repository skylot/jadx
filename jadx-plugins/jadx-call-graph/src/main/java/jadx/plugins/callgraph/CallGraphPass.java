package jadx.plugins.callgraph;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.analysis.callgraph.JadxCallGraph;
import jadx.analysis.callgraph.api.ICallGraph;
import jadx.analysis.callgraph.api.ICallGraphBuilder;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.SimpleJadxPassInfo;
import jadx.api.plugins.pass.types.JadxAfterLoadPass;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class CallGraphPass implements JadxAfterLoadPass {
	private static final Logger LOG = LoggerFactory.getLogger(CallGraphPass.class);

	private final CallGraphOptions options;

	public CallGraphPass(CallGraphOptions options) {
		this.options = options;
	}

	@Override
	public JadxPassInfo getInfo() {
		return new SimpleJadxPassInfo("CallGraph", "Build and export call graph");
	}

	@Override
	public void init(JadxDecompiler decompiler) {
		CallGraphOptions.Format format = options.getFormat();
		if (format == null || format == CallGraphOptions.Format.NONE) {
			return;
		}
		if (decompiler.getRoot().getClasses().isEmpty()) {
			return;
		}
		File outDir = decompiler.getArgs().getOutDir();
		if (outDir == null) {
			LOG.warn("Call graph export skipped: output directory is not set");
			return;
		}
		ICallGraphBuilder builder = JadxCallGraph.builder(decompiler)
				.resolvedOnly(!options.isUnresolved())
				.useAlias(options.isUseAlias());
		String includePackages = options.getIncludePackages();
		if (includePackages != null && !includePackages.isEmpty()) {
			builder.includePackages(Arrays.asList(includePackages.split(",")));
		}
		String excludePackages = options.getExcludePackages();
		if (excludePackages != null && !excludePackages.isEmpty()) {
			builder.excludePackages(Arrays.asList(excludePackages.split(",")));
		}
		ICallGraph callGraph = builder.build();
		Path outPath = outDir.toPath();
		Path cgPath;
		switch (format) {
			case JSON:
				cgPath = outPath.resolve("callgraph.json");
				callGraph.writeJson(cgPath);
				break;
			case DOT:
				cgPath = outPath.resolve("callgraph.dot");
				callGraph.writeDot(cgPath);
				break;
			default:
				throw new JadxRuntimeException("Unexpected call graph format: " + format);
		}
		LOG.info("Call graph saved: {}", cgPath.toAbsolutePath());
	}
}
