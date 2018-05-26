package jadx.core.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.codegen.CodeWriter;
import jadx.core.codegen.InsnGen;
import jadx.core.codegen.MethodGen;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.PhiListAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.PhiInsn;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.DotGraphVisitor;
import jadx.core.dex.visitors.regions.DepthRegionTraversal;
import jadx.core.dex.visitors.regions.TracedRegionVisitor;
import jadx.core.utils.exceptions.CodegenException;
import jadx.core.utils.exceptions.JadxRuntimeException;

@Deprecated
public class DebugUtils {
	private static final Logger LOG = LoggerFactory.getLogger(DebugUtils.class);

	private DebugUtils() {
	}

	public static void dump(MethodNode mth) {
		dump(mth, "");
	}

	public static void dump(MethodNode mth, String desc) {
		File out = new File("test-graph" + desc + "-tmp");
		DotGraphVisitor.dump().save(out, mth);
		DotGraphVisitor.dumpRaw().save(out, mth);
		DotGraphVisitor.dumpRegions().save(out, mth);
	}

	public static void printRegionsWithBlock(MethodNode mth, BlockNode block) {
		Set<IRegion> regions = new LinkedHashSet<>();
		DepthRegionTraversal.traverse(mth, new TracedRegionVisitor() {
			@Override
			public void processBlockTraced(MethodNode mth, IBlock container, IRegion currentRegion) {
				if (block.equals(container)) {
					regions.add(currentRegion);
				}
			}
		});
		LOG.debug(" Found block: {} in regions: {}", block, regions);
	}

	public static void printRegions(MethodNode mth) {
		printRegions(mth, false);
	}

	public static void printRegion(MethodNode mth, IRegion region, boolean printInsn) {
		printRegion(mth, region, "", printInsn);
	}

	public static void printRegions(MethodNode mth, boolean printInsns) {
		LOG.debug("|{}", mth);
		printRegion(mth, mth.getRegion(), "|  ", printInsns);
	}

	private static void printRegion(MethodNode mth, IRegion region, String indent, boolean printInsns) {
		LOG.debug("{}{} {}", indent, region, region.getAttributesString());
		indent += "|  ";
		for (IContainer container : region.getSubBlocks()) {
			if (container instanceof IRegion) {
				printRegion(mth, (IRegion) container, indent, printInsns);
			} else {
				LOG.debug("{}{} {}", indent, container, container.getAttributesString());
				if (printInsns && container instanceof IBlock) {
					IBlock block = (IBlock) container;
					printInsns(mth, indent, block);
				}
			}
		}
	}

	private static void printInsns(MethodNode mth, String indent, IBlock block) {
		for (InsnNode insn : block.getInstructions()) {
			try {
				MethodGen mg = MethodGen.getFallbackMethodGen(mth);
				InsnGen ig = new InsnGen(mg, true);
				CodeWriter code = new CodeWriter();
				ig.makeInsn(insn, code);
				String insnStr = code.toString().substring(CodeWriter.NL.length());
				LOG.debug("{}> {}", indent, insnStr);
			} catch (CodegenException e) {
				LOG.debug("{}>!! {}", indent, insn);
			}
		}
	}

	public static void checkSSA(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn.getResult() != null) {
					checkSSAVar(mth, insn, insn.getResult());
				}
				for (InsnArg arg : insn.getArguments()) {
					if (arg instanceof RegisterArg) {
						checkSSAVar(mth, insn, (RegisterArg) arg);
					}
				}
			}
		}
		checkPHI(mth);
	}

	private static void checkSSAVar(MethodNode mth, InsnNode insn, RegisterArg reg) {
		SSAVar sVar = reg.getSVar();
		if (sVar == null) {
			throw new JadxRuntimeException("Null SSA var in " + insn + ", mth: " + mth);
		}
		for (RegisterArg useArg : sVar.getUseList()) {
			InsnNode parentInsn = useArg.getParentInsn();
			if (parentInsn != null && !parentInsn.containsArg(useArg)) {
				throw new JadxRuntimeException("Incorrect use info in PHI insn");
			}
		}
	}

	private static void checkPHI(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			List<PhiInsn> phis = new ArrayList<>();
			for (InsnNode insn : block.getInstructions()) {
				if (insn.getType() == InsnType.PHI) {
					PhiInsn phi = (PhiInsn) insn;
					phis.add(phi);
					if (phi.getArgsCount() != phi.getBlockBinds().size()) {
						throw new JadxRuntimeException("Incorrect args and binds in PHI");
					}
					if (phi.getArgsCount() == 0) {
						throw new JadxRuntimeException("No args and binds in PHI");
					}
					for (InsnArg arg : insn.getArguments()) {
						if (arg instanceof RegisterArg) {
							BlockNode b = phi.getBlockByArg((RegisterArg) arg);
							if (b == null) {
								throw new JadxRuntimeException("Predecessor block not found");
							}
						} else {
							throw new JadxRuntimeException("Not register in phi insn");
						}
					}
				}
			}
			PhiListAttr phiListAttr = block.get(AType.PHI_LIST);
			if (phiListAttr == null) {
				if (!phis.isEmpty()) {
					throw new JadxRuntimeException("Missing PHI list attribute");
				}
			} else {
				List<PhiInsn> phiList = phiListAttr.getList();
				if (phiList.isEmpty()) {
					throw new JadxRuntimeException("Empty PHI list attribute");
				}
				if (!phis.containsAll(phiList) || !phiList.containsAll(phis)) {
					throw new JadxRuntimeException("Instructions not match");
				}
			}
		}
		for (SSAVar ssaVar : mth.getSVars()) {
			PhiInsn usedInPhi = ssaVar.getUsedInPhi();
			if (usedInPhi != null) {
				boolean found = false;
				for (RegisterArg useArg : ssaVar.getUseList()) {
					InsnNode parentInsn = useArg.getParentInsn();
					if (parentInsn != null && parentInsn == usedInPhi) {
						found = true;
					}
				}
				if (!found) {
					throw new JadxRuntimeException("Used in phi incorrect");
				}
			}
		}
	}
}
