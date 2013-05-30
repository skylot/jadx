package jadx;

import jadx.codegen.CodeGen;
import jadx.dex.info.ClassInfo;
import jadx.dex.nodes.ClassNode;
import jadx.dex.nodes.RootNode;
import jadx.dex.visitors.BlockMakerVisitor;
import jadx.dex.visitors.ClassModifier;
import jadx.dex.visitors.CodeShrinker;
import jadx.dex.visitors.ConstInlinerVisitor;
import jadx.dex.visitors.DotGraphVisitor;
import jadx.dex.visitors.EnumVisitor;
import jadx.dex.visitors.FallbackModeVisitor;
import jadx.dex.visitors.IDexTreeVisitor;
import jadx.dex.visitors.MethodInlinerVisitor;
import jadx.dex.visitors.ModVisitor;
import jadx.dex.visitors.regions.CheckRegions;
import jadx.dex.visitors.regions.CleanRegions;
import jadx.dex.visitors.regions.PostRegionVisitor;
import jadx.dex.visitors.regions.ProcessVariables;
import jadx.dex.visitors.regions.RegionMakerVisitor;
import jadx.dex.visitors.typeresolver.FinishTypeResolver;
import jadx.dex.visitors.typeresolver.TypeResolver;
import jadx.utils.ErrorsCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Jadx implements Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(Jadx.class);

	static {
		if (Consts.DEBUG)
			LOG.info("debug enabled");
		if (Jadx.class.desiredAssertionStatus())
			LOG.info("assertions enabled");
	}

	private final IJadxArgs args;
	private int errorsCount;

	public Jadx(IJadxArgs args) {
		this.args = args;
	}

	public void run() {
		try {
			RootNode root = new RootNode(args);
			LOG.info("loading ...");
			root.load();
			LOG.info("processing ...");
			root.init();

			int threadsCount = args.getThreadsCount();
			LOG.debug("processing threads count: {}", threadsCount);

			List<IDexTreeVisitor> passes = getPassesList(args);
			if (threadsCount == 1) {
				for (ClassNode cls : root.getClasses()) {
					ProcessClass job = new ProcessClass(cls, passes);
					job.run();
				}
			} else {
				ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
				for (ClassNode cls : root.getClasses()) {
					ProcessClass job = new ProcessClass(cls, passes);
					executor.execute(job);
				}
				executor.shutdown();
				executor.awaitTermination(100, TimeUnit.DAYS);
			}
		} catch (Throwable e) {
			LOG.error("jadx error:", e);
		} finally {
			errorsCount = ErrorsCounter.getErrorCount();
			if (errorsCount != 0)
				ErrorsCounter.printReport();

			// clear resources if we use jadx as a library
			ClassInfo.clearCache();
			ErrorsCounter.reset();
		}
		LOG.info("done");
	}

	private static List<IDexTreeVisitor> getPassesList(IJadxArgs args) {
		List<IDexTreeVisitor> passes = new ArrayList<IDexTreeVisitor>();
		if (args.isFallbackMode()) {
			passes.add(new FallbackModeVisitor());
		} else {
			passes.add(new BlockMakerVisitor());

			passes.add(new TypeResolver());
			passes.add(new ConstInlinerVisitor());
			passes.add(new FinishTypeResolver());

			if (args.isRawCFGOutput())
				passes.add(new DotGraphVisitor(args.getOutDir(), false, true));

			passes.add(new ModVisitor());
			passes.add(new EnumVisitor());

			if (args.isCFGOutput())
				passes.add(new DotGraphVisitor(args.getOutDir(), false));

			passes.add(new RegionMakerVisitor());
			passes.add(new PostRegionVisitor());

			passes.add(new CodeShrinker());
			passes.add(new ProcessVariables());
			passes.add(new CheckRegions());
			if (args.isCFGOutput())
				passes.add(new DotGraphVisitor(args.getOutDir(), true));

			passes.add(new MethodInlinerVisitor());
			passes.add(new ClassModifier());
			passes.add(new CleanRegions());
		}
		passes.add(new CodeGen(args));
		return passes;
	}

	public int getErrorsCount() {
		return errorsCount;
	}
}
