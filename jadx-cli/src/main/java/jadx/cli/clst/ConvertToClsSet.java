package jadx.cli.clst;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.impl.plugins.PluginsContext;
import jadx.api.plugins.JadxPluginManager;
import jadx.api.plugins.input.ICodeLoader;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.core.clsp.ClsSet;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.SignatureProcessor;

/**
 * Utility class for convert dex or jar to jadx classes set (.jcst)
 */
public class ConvertToClsSet {
	private static final Logger LOG = LoggerFactory.getLogger(ConvertToClsSet.class);

	public static void usage() {
		LOG.info("<output .jcst or .jar file> <several input dex or jar files> ");
	}

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			usage();
			System.exit(1);
		}
		List<Path> inputPaths = Stream.of(args).map(Paths::get).collect(Collectors.toList());
		Path output = inputPaths.remove(0);

		PluginsContext pluginsContext = new PluginsContext(new JadxDecompiler());
		JadxPluginManager pluginManager = new JadxPluginManager();
		pluginManager.load();
		pluginManager.initResolved(pluginsContext);
		List<ICodeLoader> loadedInputs = new ArrayList<>();
		for (JadxCodeInput inputPlugin : pluginsContext.getCodeInputs()) {
			loadedInputs.add(inputPlugin.loadFiles(inputPaths));
		}

		JadxArgs jadxArgs = new JadxArgs();
		jadxArgs.setRenameFlags(EnumSet.noneOf(JadxArgs.RenameEnum.class));
		RootNode root = new RootNode(jadxArgs);
		root.loadClasses(loadedInputs);

		// from pre-decompilation stage run only SignatureProcessor
		SignatureProcessor signatureProcessor = new SignatureProcessor();
		signatureProcessor.init(root);
		for (ClassNode classNode : root.getClasses()) {
			signatureProcessor.visit(classNode);
		}

		ClsSet set = new ClsSet(root);
		set.loadFrom(root);
		set.save(output);

		LOG.info("Output: {}", output);
		LOG.info("done");
	}
}
