package jadx.core;

import jadx.api.IJadxArgs;
import jadx.core.dex.visitors.ClassModifier;
import jadx.core.dex.visitors.CodeShrinker;
import jadx.core.dex.visitors.ConstInlineVisitor;
import jadx.core.dex.visitors.DebugInfoVisitor;
import jadx.core.dex.visitors.DependencyCollector;
import jadx.core.dex.visitors.DotGraphVisitor;
import jadx.core.dex.visitors.EnumVisitor;
import jadx.core.dex.visitors.ExtractFieldInit;
import jadx.core.dex.visitors.FallbackModeVisitor;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.dex.visitors.MethodInlineVisitor;
import jadx.core.dex.visitors.ModVisitor;
import jadx.core.dex.visitors.PrepareForCodeGen;
import jadx.core.dex.visitors.ReSugarCode;
import jadx.core.dex.visitors.RenameVisitor;
import jadx.core.dex.visitors.SimplifyVisitor;
import jadx.core.dex.visitors.blocksmaker.BlockExceptionHandler;
import jadx.core.dex.visitors.blocksmaker.BlockFinallyExtract;
import jadx.core.dex.visitors.blocksmaker.BlockFinish;
import jadx.core.dex.visitors.blocksmaker.BlockProcessor;
import jadx.core.dex.visitors.blocksmaker.BlockSplitter;
import jadx.core.dex.visitors.regions.CheckRegions;
import jadx.core.dex.visitors.regions.IfRegionVisitor;
import jadx.core.dex.visitors.regions.LoopRegionVisitor;
import jadx.core.dex.visitors.regions.ProcessVariables;
import jadx.core.dex.visitors.regions.RegionMakerVisitor;
import jadx.core.dex.visitors.regions.ReturnVisitor;
import jadx.core.dex.visitors.ssa.EliminatePhiNodes;
import jadx.core.dex.visitors.ssa.SSATransform;
import jadx.core.dex.visitors.typeinference.FinishTypeInference;
import jadx.core.dex.visitors.typeinference.TypeInference;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jadx {
	private static final Logger LOG = LoggerFactory.getLogger(Jadx.class);

	static {
		if (Consts.DEBUG) {
			LOG.info("debug enabled");
		}
	}

	public static List<IDexTreeVisitor> getPassesList(IJadxArgs args, File outDir) {
		List<IDexTreeVisitor> passes = new ArrayList<IDexTreeVisitor>();
		if (args.isFallbackMode()) {
			passes.add(new FallbackModeVisitor());
		} else {
			passes.add(new BlockSplitter());
			passes.add(new BlockProcessor());
			passes.add(new BlockExceptionHandler());
			passes.add(new BlockFinallyExtract());
			passes.add(new BlockFinish());

			passes.add(new SSATransform());
			passes.add(new DebugInfoVisitor());
			passes.add(new TypeInference());

			if (args.isRawCFGOutput()) {
				passes.add(DotGraphVisitor.dumpRaw(outDir));
			}

			passes.add(new ConstInlineVisitor());
			passes.add(new FinishTypeInference());
			passes.add(new EliminatePhiNodes());

			passes.add(new ModVisitor());

			passes.add(new CodeShrinker());
			passes.add(new ReSugarCode());

			if (args.isCFGOutput()) {
				passes.add(DotGraphVisitor.dump(outDir));
			}

			passes.add(new RegionMakerVisitor());
			passes.add(new IfRegionVisitor());
			passes.add(new ReturnVisitor());

			passes.add(new CodeShrinker());
			passes.add(new SimplifyVisitor());
			passes.add(new CheckRegions());

			if (args.isCFGOutput()) {
				passes.add(DotGraphVisitor.dumpRegions(outDir));
			}

			passes.add(new MethodInlineVisitor());
			passes.add(new ExtractFieldInit());
			passes.add(new ClassModifier());
			passes.add(new EnumVisitor());
			passes.add(new PrepareForCodeGen());
			passes.add(new LoopRegionVisitor());
			passes.add(new ProcessVariables());

			passes.add(new DependencyCollector());

			passes.add(new RenameVisitor());
		}
		return passes;
	}

	public static String getVersion() {
		try {
			ClassLoader classLoader = Jadx.class.getClassLoader();
			if (classLoader != null) {
				Enumeration<URL> resources = classLoader.getResources("META-INF/MANIFEST.MF");
				while (resources.hasMoreElements()) {
					Manifest manifest = new Manifest(resources.nextElement().openStream());
					String ver = manifest.getMainAttributes().getValue("jadx-version");
					if (ver != null) {
						return ver;
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Can't get manifest file", e);
		}
		return "dev";
	}
}
