package jadx.core;

import jadx.api.IJadxArgs;
import jadx.core.codegen.CodeGen;
import jadx.core.dex.visitors.BlockMakerVisitor;
import jadx.core.dex.visitors.ClassModifier;
import jadx.core.dex.visitors.CodeShrinker;
import jadx.core.dex.visitors.ConstInlinerVisitor;
import jadx.core.dex.visitors.DotGraphVisitor;
import jadx.core.dex.visitors.EnumVisitor;
import jadx.core.dex.visitors.FallbackModeVisitor;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.dex.visitors.MethodInlinerVisitor;
import jadx.core.dex.visitors.ModVisitor;
import jadx.core.dex.visitors.regions.CheckRegions;
import jadx.core.dex.visitors.regions.CleanRegions;
import jadx.core.dex.visitors.regions.PostRegionVisitor;
import jadx.core.dex.visitors.regions.ProcessVariables;
import jadx.core.dex.visitors.regions.RegionMakerVisitor;
import jadx.core.dex.visitors.typeresolver.FinishTypeResolver;
import jadx.core.dex.visitors.typeresolver.TypeResolver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jadx {
	private static final Logger LOG = LoggerFactory.getLogger(Jadx.class);

	static {
		if (Consts.DEBUG)
			LOG.info("debug enabled");
		if (Jadx.class.desiredAssertionStatus())
			LOG.info("assertions enabled");
	}

	public static List<IDexTreeVisitor> getPassesList(IJadxArgs args, File outDir) {
		List<IDexTreeVisitor> passes = new ArrayList<IDexTreeVisitor>();
		if (args.isFallbackMode()) {
			passes.add(new FallbackModeVisitor());
		} else {
			passes.add(new BlockMakerVisitor());

			passes.add(new TypeResolver());
			passes.add(new ConstInlinerVisitor());
			passes.add(new FinishTypeResolver());

			if (args.isRawCFGOutput())
				passes.add(new DotGraphVisitor(outDir, false, true));

			passes.add(new ModVisitor());
			passes.add(new EnumVisitor());

			if (args.isCFGOutput())
				passes.add(new DotGraphVisitor(outDir, false));

			passes.add(new RegionMakerVisitor());
			passes.add(new PostRegionVisitor());

			passes.add(new CodeShrinker());
			passes.add(new ProcessVariables());
			passes.add(new CheckRegions());
			if (args.isCFGOutput())
				passes.add(new DotGraphVisitor(outDir, true));

			passes.add(new MethodInlinerVisitor());
			passes.add(new ClassModifier());
			passes.add(new CleanRegions());
		}
		passes.add(new CodeGen(args));
		return passes;
	}
}
