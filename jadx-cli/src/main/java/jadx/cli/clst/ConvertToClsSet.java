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
import jadx.api.plugins.input.ICodeLoader;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.core.clsp.ClsSet;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.SignatureProcessor;
import jadx.core.plugins.JadxPluginManager;

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

		JadxArgs jadxArgs = new JadxArgs();
		jadxArgs.setRenameFlags(EnumSet.noneOf(JadxArgs.RenameEnum.class));
		try (JadxDecompiler decompiler = new JadxDecompiler(jadxArgs)) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			pluginManager.load();
			pluginManager.initResolved();
			List<ICodeLoader> loadedInputs = new ArrayList<>();
			for (JadxCodeInput inputPlugin : pluginManager.getCodeInputs()) {
				loadedInputs.add(inputPlugin.loadFiles(inputPaths));
			}
			RootNode root = decompiler.getRoot();
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
		} catch (Exception e) {
			LOG.error("Failed with error", e);
		}
	}
}
