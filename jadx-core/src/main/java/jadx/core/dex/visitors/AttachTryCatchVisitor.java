package jadx.core.dex.visitors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jadx.api.plugins.input.data.ICatch;
import jadx.api.plugins.input.data.ICodeReader;
import jadx.api.plugins.input.data.ITry;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.utils.exceptions.JadxException;

import static jadx.core.dex.visitors.ProcessInstructionsVisitor.getNextInsnOffset;

@JadxVisitor(
		name = "Attach Try/Catch Visitor",
		desc = "Attach try/catch info to instructions",
		runBefore = {
				ProcessInstructionsVisitor.class
		}
)
public class AttachTryCatchVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isNoCode()) {
			return;
		}
		initTryCatches(mth, mth.getCodeReader(), mth.getInstructions());
	}

	private static void initTryCatches(MethodNode mth, ICodeReader codeReader, InsnNode[] insnByOffset) {
		List<ITry> tries = codeReader.getTries();
		if (tries.isEmpty()) {
			return;
		}
		int handlersCount = 0;
		Set<Integer> addrs = new HashSet<>();
		List<TryCatchBlock> catches = new ArrayList<>(tries.size());
		for (ITry tryData : tries) {
			TryCatchBlock catchBlock = processHandlers(mth, addrs, tryData.getCatch());
			catches.add(catchBlock);
			handlersCount += catchBlock.getHandlersCount();
		}

		// TODO: run modify in later passes
		if (handlersCount > 0 && handlersCount != addrs.size()) {
			// resolve nested try blocks:
			// inner block contains all handlers from outer block => remove these handlers from inner block
			// each handler must be only in one try/catch block
			for (TryCatchBlock outerTry : catches) {
				for (TryCatchBlock innerTry : catches) {
					if (outerTry != innerTry
							&& innerTry.containsAllHandlers(outerTry)) {
						innerTry.removeSameHandlers(outerTry);
					}
				}
			}
		}
		addrs.clear();

		for (TryCatchBlock tryCatchBlock : catches) {
			if (tryCatchBlock.getHandlersCount() == 0) {
				continue;
			}
			for (ExceptionHandler handler : tryCatchBlock.getHandlers()) {
				int addr = handler.getHandleOffset();
				ExcHandlerAttr ehAttr = new ExcHandlerAttr(tryCatchBlock, handler);
				// TODO: don't override existing attribute
				insnByOffset[addr].addAttr(ehAttr);
			}
		}

		int k = 0;
		for (ITry tryData : tries) {
			TryCatchBlock catchBlock = catches.get(k++);
			if (catchBlock.getHandlersCount() != 0) {
				markTryBounds(insnByOffset, tryData, catchBlock);
			}
		}

	}

	private static void markTryBounds(InsnNode[] insnByOffset, ITry aTry, TryCatchBlock catchBlock) {
		int offset = aTry.getStartAddress();
		int end = offset + aTry.getInstructionCount() - 1;

		boolean tryBlockStarted = false;
		InsnNode insn = null;
		while (offset <= end && offset >= 0) {
			insn = insnByOffset[offset];
			if (insn != null && insn.getType() != InsnType.NOP) {
				if (tryBlockStarted) {
					catchBlock.addInsn(insn);
				} else if (insn.canThrowException()) {
					insn.add(AFlag.TRY_ENTER);
					catchBlock.addInsn(insn);
					tryBlockStarted = true;
				}
			}
			offset = getNextInsnOffset(insnByOffset, offset);
		}
		if (tryBlockStarted && insn != null) {
			insn.add(AFlag.TRY_LEAVE);
		}
	}

	private static TryCatchBlock processHandlers(MethodNode mth, Set<Integer> addrs, ICatch catchBlock) {
		int[] handlerAddrArr = catchBlock.getAddresses();
		String[] handlerTypes = catchBlock.getTypes();

		int handlersCount = handlerAddrArr.length;
		TryCatchBlock tcBlock = new TryCatchBlock(handlersCount);
		for (int i = 0; i < handlersCount; i++) {
			int addr = handlerAddrArr[i];
			ClassInfo type = ClassInfo.fromName(mth.root(), handlerTypes[i]);
			tcBlock.addHandler(mth, addr, type);
			addrs.add(addr);
		}
		int addr = catchBlock.getCatchAllAddress();
		if (addr >= 0) {
			tcBlock.addHandler(mth, addr, null);
			addrs.add(addr);
		}
		return tcBlock;
	}
}
