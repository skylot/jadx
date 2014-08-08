package jadx.core;

import jadx.api.IJadxArgs;
import jadx.core.codegen.CodeGen;
import jadx.core.dex.visitors.BlockMakerVisitor;
import jadx.core.dex.visitors.ClassModifier;
import jadx.core.dex.visitors.CodeShrinker;
import jadx.core.dex.visitors.ConstInlinerVisitor;
import jadx.core.dex.visitors.DebugInfoVisitor;
import jadx.core.dex.visitors.DotGraphVisitor;
import jadx.core.dex.visitors.EnumVisitor;
import jadx.core.dex.visitors.FallbackModeVisitor;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.dex.visitors.MethodInlineVisitor;
import jadx.core.dex.visitors.ModVisitor;
import jadx.core.dex.visitors.PrepareForCodeGen;
import jadx.core.dex.visitors.SimplifyVisitor;
import jadx.core.dex.visitors.regions.CheckRegions;
import jadx.core.dex.visitors.regions.IfRegionVisitor;
import jadx.core.dex.visitors.regions.ProcessVariables;
import jadx.core.dex.visitors.regions.RegionMakerVisitor;
import jadx.core.dex.visitors.regions.ReturnVisitor;
import jadx.core.dex.visitors.ssa.EliminatePhiNodes;
import jadx.core.dex.visitors.ssa.SSATransform;
import jadx.core.dex.visitors.typeinference.FinishTypeInference;
import jadx.core.dex.visitors.typeinference.TypeInference;
import jadx.core.utils.Utils;

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
		if (Jadx.class.desiredAssertionStatus()) {
			LOG.info("assertions enabled");
		}
	}

	public static List<IDexTreeVisitor> getPassesList(IJadxArgs args, File outDir) {
		List<IDexTreeVisitor> passes = new ArrayList<IDexTreeVisitor>();
		if (args.isFallbackMode()) {
			passes.add(new FallbackModeVisitor());
		} else {
			passes.add(new BlockMakerVisitor());
			passes.add(new SSATransform());
			passes.add(new DebugInfoVisitor());
			passes.add(new TypeInference());
			if (args.isRawCFGOutput()) {
				passes.add(new DotGraphVisitor(outDir, false, true));
			}

			passes.add(new ConstInlinerVisitor());
			passes.add(new FinishTypeInference());
			passes.add(new EliminatePhiNodes());

			passes.add(new ModVisitor());
			passes.add(new EnumVisitor());

			passes.add(new CodeShrinker());
			if (args.isCFGOutput()) {
				passes.add(new DotGraphVisitor(outDir, false));
			}

			passes.add(new RegionMakerVisitor());
			passes.add(new IfRegionVisitor());
			passes.add(new ReturnVisitor());

			passes.add(new CodeShrinker());
			passes.add(new SimplifyVisitor());
			passes.add(new ProcessVariables());
			passes.add(new CheckRegions());

			if (args.isCFGOutput()) {
				passes.add(new DotGraphVisitor(outDir, true));
			}

			passes.add(new MethodInlineVisitor());
			passes.add(new ClassModifier());
			passes.add(new PrepareForCodeGen());
		}
		passes.add(new CodeGen(args));
		return passes;
	}

	public static String getVersion() {
		try {
			ClassLoader classLoader = Utils.class.getClassLoader();
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
