package jadx.core.dex.visitors;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.IndexInsnNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.InvokeCustomNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.typeinference.TypeInferenceVisitor;

/**
 * Fix CHECK_CAST instructions that wrap InvokeCustomNode (lambda) results.
 * When a lambda uses altMetafactory with marker interfaces (intersection types),
 * the cast should be to the marker interface, not the functional interface.
 * <p>
 * Example: Lambda with type {@code (TestCls<R> & Memoized)} should be cast to
 * {@code Memoized}, not {@code TestCls}.
 * <p>
 * This visitor runs after type inference has updated SSAVar types to marker interfaces,
 * and fixes CHECK_CAST instructions to match the inferred types.
 */
@JadxVisitor(
		name = "Fix Lambda Cast",
		desc = "Fix CHECK_CAST for lambdas with marker interfaces",
		runAfter = { TypeInferenceVisitor.class }
)
public class FixLambdaCastVisitor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(FixLambdaCastVisitor.class);

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}
		
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn.getType() == InsnType.CHECK_CAST) {
					processCheckCast(mth, (IndexInsnNode) insn);
				}
			}
		}
	}

	private void processCheckCast(MethodNode mth, IndexInsnNode checkCast) {
		// Get the argument being cast
		InsnArg arg = checkCast.getArg(0);
		if (!(arg instanceof RegisterArg)) {
			if (Consts.DEBUG_TYPE_INFERENCE) {
				LOG.debug("FixLambdaCast: CHECK_CAST arg is not RegisterArg: {}", arg);
			}
			return;
		}
		
		RegisterArg regArg = (RegisterArg) arg;
		SSAVar ssaVar = regArg.getSVar();
		if (ssaVar == null) {
			if (Consts.DEBUG_TYPE_INFERENCE) {
				LOG.debug("FixLambdaCast: SSAVar is null for CHECK_CAST in {}", mth);
			}
			return;
		}
		
		// Check if this arg comes from an InvokeCustomNode
		RegisterArg assign = ssaVar.getAssign();
		if (assign == null) {
			if (Consts.DEBUG_TYPE_INFERENCE) {
				LOG.debug("FixLambdaCast: assign is null for ssaVar {} in {}", ssaVar, mth);
			}
			return;
		}
		
		InsnNode parentInsn = assign.getParentInsn();
		if (!(parentInsn instanceof InvokeCustomNode)) {
			if (Consts.DEBUG_TYPE_INFERENCE) {
				LOG.debug("FixLambdaCast: parentInsn is not InvokeCustomNode: {} (type={}) in {}", 
						parentInsn, parentInsn != null ? parentInsn.getType() : "null", mth);
			}
			return;
		}
		
		InvokeCustomNode invokeCustom = (InvokeCustomNode) parentInsn;
		List<ArgType> markerInterfaces = invokeCustom.getMarkerInterfaces();
		
		// If no marker interfaces, nothing to fix
		if (markerInterfaces == null || markerInterfaces.isEmpty()) {
			return;
		}
		
		// Get the current cast type
		ArgType currentCastType = (ArgType) checkCast.getIndex();
		
		// Check if current cast is to a marker interface or needs to be updated
		// We prefer the last marker interface (typically the marker like Memoized)
		// over the functional interface (like TestCls)
		ArgType targetMarker = markerInterfaces.get(markerInterfaces.size() - 1);
		
		if (Consts.DEBUG_TYPE_INFERENCE) {
			LOG.debug("FixLambdaCast: Found CHECK_CAST for lambda in {}, currentCastType={}, targetMarker={}, markers={}", 
					mth, currentCastType, targetMarker, markerInterfaces);
		}
		
		if (!currentCastType.equals(targetMarker)) {
			if (Consts.DEBUG_TYPE_INFERENCE) {
				LOG.debug("FixLambdaCast: Updating CHECK_CAST from {} to {} for lambda in {}", 
						currentCastType, targetMarker, mth);
			}
			checkCast.updateIndex(targetMarker);
			
			// Update result type to match
			RegisterArg result = checkCast.getResult();
			if (result != null) {
				result.setType(targetMarker);
			}
		}
		
		// Mark this cast as explicit so SimplifyVisitor doesn't remove it
		// This is important when there are multiple casts (e.g., to marker interface then to functional interface)
		// We want to preserve the marker interface cast for better readability
		checkCast.add(AFlag.EXPLICIT_CAST);
	}

	@Override
	public String getName() {
		return "FixLambdaCastVisitor";
	}
}
