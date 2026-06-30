package jadx.core.dex.visitors;

import java.io.File;
import java.util.Optional;
import java.util.regex.Matcher;

import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.DotGraphUtils;

public class DotGraphVisitor extends AbstractVisitor {

	private static final String NL = "\\l";
	private static final String NLQR = Matcher.quoteReplacement(NL);
	private static final boolean PRINT_DOMINATORS = false;
	private static final boolean PRINT_DOMINATORS_INFO = false;

	private final boolean useRegions;
	private final boolean rawInsn;

	// if present, this region and it's children will still be drawn when not in regions mode.
	private Optional<IRegion> highlightRegion;

	public static DotGraphVisitor dump() {
		return new DotGraphVisitor(false, false);
	}

	public static DotGraphVisitor dumpRaw() {
		return new DotGraphVisitor(false, true);
	}

	public static DotGraphVisitor dumpRegions() {
		return new DotGraphVisitor(true, false);
	}

	public static DotGraphVisitor dumpRawRegions() {
		return new DotGraphVisitor(true, true);
	}

	/**
	 * Helper function to generate a cfg at a given point showing only one of the regions in the graph.
	 * Intended to be called during a debugging session to produce a CFG with only a region of interest,
	 * with DotGraphVisitor.debugDumpWithRegionHiglight(region).visit(mth);
	 *
	 * @param region the region to show
	 * @return the visitor, to be invoked with `.visit(mth);`
	 */
	public static DotGraphVisitor debugDumpWithRegionHighlight(IRegion region) {
		return new DotGraphVisitor(false, false, Optional.of(region));
	}

	private DotGraphVisitor(boolean useRegions, boolean rawInsn) {
		this(useRegions, rawInsn, Optional.empty());
	}

	private DotGraphVisitor(boolean useRegions, boolean rawInsn, Optional<IRegion> highlightRegion) {
		this.useRegions = useRegions;
		this.rawInsn = rawInsn;
		this.highlightRegion = highlightRegion;
	}

	@Override
	public String getName() {
		return "DotGraphVisitor";
	}

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		new DotGraphUtils(useRegions, rawInsn, highlightRegion).dumpToFile(mth);
	}

	public void save(File dir, MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		new DotGraphUtils(useRegions, rawInsn, highlightRegion).dumpToFile(mth, dir);
	}
}
