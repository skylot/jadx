package jadx.gui.ui.codearea.sync;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.InsnCodeOffset;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.core.dex.nodes.MethodNode;
import jadx.gui.device.debugger.DbgUtils;
import jadx.gui.device.debugger.smali.SmaliMethodNode;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.SmaliArea;

/**
 * Use insn code offsets to sync code panel area to code/smali
 * This only works for Smali when SmaliArea is showing the dalvik bytecode.
 */
public class InsnOffsetJavaSyncer implements IToJavaSyncStrategy, IToSmaliSyncStrategy {
	private static final Logger LOG = LoggerFactory.getLogger(InsnOffsetJavaSyncer.class);

	private final CodeArea from;

	public InsnOffsetJavaSyncer(CodeArea area) {
		this.from = area;
	}

	@Override
	public boolean syncTo(SmaliArea to) {
		if (!to.isShowingDalvikBytecode()) {
			return false;
		}

		// 1. Find the Method start and end boundaries enclosing the caret position in the code metadata
		// 2. Find the closest InsnCodeOffset range within the method boundary corresponding to the caret
		// position
		// 3. Get all of the smali lines which fall within the InsnCodeOffset range.
		// 4. Highlight those found in 3. and scroll to the first one.
		int caretPos = from.getCaretPosition();
		CodeMetadataRange mthRange = findEnclosingMethodRange(caretPos);
		if (mthRange == null) {
			return false;
		}

		Integer mthDefPos = mthRange.getStart().getKey();
		Integer mthEndPos = mthRange.getEnd().getKey();

		LOG.debug("InsnOffsetJavaSyncer caretPos = {}", caretPos);
		LOG.debug("InsnOffsetJavaSyncer mthDefPos = {}", mthDefPos);
		LOG.debug("InsnOffsetJavaSyncer mthEndPos = {}", mthEndPos);

		CodeMetadataRange insnOffsetRange = findOffsetRange(caretPos, mthDefPos, mthEndPos);
		if (insnOffsetRange == null) {
			return false;
		}

		String mthID = getMthRawFullID(mthDefPos);
		SmaliMethodNode smaliMthNode = DbgUtils.getSmaliMethodNode(to.getJClass(), mthID);
		if (smaliMthNode == null) {
			LOG.error("{} - mth ID {} not mapped to a SmaliMethodNode", LOG.getName(), mthID);
			return false;
		}

		List<Integer> smaliLines = getMappedSmaliLines(smaliMthNode, insnOffsetRange);
		if (smaliLines.size() < 2) {
			return false;
		}

		try {
			CodeSyncHighlighter.defaultHighlighter().highlightAndScrollToLine(to, smaliLines.get(0));
			for (int i = 1; i < smaliLines.size(); ++i) {
				CodeSyncHighlighter.defaultHighlighter().highlightLine(to, smaliLines.get(i));
			}
			LOG.info("{} - successful sync of code to smali", LOG.getName());
			return true;
		} catch (Exception ex) {
			LOG.error("{} - Failed to sync code to smali with instruction offsets ", LOG.getName(), ex);
		}

		return false;
	}

	@Override
	public boolean syncTo(CodeArea to) {
		int caretPos = from.getCaretPosition();
		CodeMetadataRange fromMthRange = findEnclosingMethodRange(caretPos);
		if (fromMthRange == null) {
			return false;
		}

		Integer mthDefPos = fromMthRange.getStart().getKey();
		Integer mthEndPos = fromMthRange.getEnd().getKey();

		LOG.debug("InsnOffsetJavaSyncer caretPos = {}", caretPos);
		LOG.debug("InsnOffsetJavaSyncer mthDefPos = {}", mthDefPos);
		LOG.debug("InsnOffsetJavaSyncer mthEndPos = {}", mthEndPos);

		CodeMetadataRange fromInsnOffsetRange = findOffsetRange(caretPos, mthDefPos, mthEndPos);
		if (fromInsnOffsetRange == null) {
			return false;
		}

		String mthID = getMthRawFullID(mthDefPos);

		// now search for this range within the target area
		CodeMetadataRange toMthRange = findMethodRange(mthID, to);
		if (toMthRange == null) {
			return false;
		}

		// search for the first insn offset
		int firstInsnOffset = ((InsnCodeOffset) fromInsnOffsetRange.getStart().getValue()).getOffset();
		Integer highlightPosStart = to.getCodeMetadata().searchDown(toMthRange.getStart().getKey(), (offset, ann) -> {
			if (ann.getAnnType() != ICodeAnnotation.AnnType.OFFSET) {
				return null;
			}
			int pos = ((InsnCodeOffset) ann).getOffset();
			if (pos != firstInsnOffset) {
				return null;
			}
			return offset;
		});

		if (highlightPosStart == null) {
			return false;
		}

		// search for the second insn offset
		int secondInsnOffset = ((InsnCodeOffset) fromInsnOffsetRange.getEnd().getValue()).getOffset();
		Integer highlightPosEnd = to.getCodeMetadata().searchDown(highlightPosStart, (offset, ann) -> {
			if (ann.getAnnType() != ICodeAnnotation.AnnType.OFFSET) {
				return null;
			}
			int pos = ((InsnCodeOffset) ann).getOffset();
			if (pos != secondInsnOffset) {
				return null;
			}
			return offset;
		});

		if (highlightPosEnd == null) {
			return false;
		}

		to.scrollToPos(highlightPosStart);
		try {
			CodeSyncHighlighter.defaultHighlighter().highlightRange(to, highlightPosStart, highlightPosEnd);
			LOG.info("{} - successful sync of code to code", LOG.getName());
			return true;
		} catch (Exception ex) {
			LOG.error("{} - Unable to highlight code area from insn offset mappings {} -> {}", LOG.getName(), highlightPosStart,
					highlightPosEnd);
		}
		return false;
	}

	@Nullable
	private static CodeMetadataRange findMethodRange(String mthFullRawID, CodeArea area) {
		Map.Entry<Integer, ICodeAnnotation> toMthDecl = area.getCodeMetadata().searchDown(0, (offset, ann) -> {
			if (ann.getAnnType() != ICodeAnnotation.AnnType.DECLARATION) {
				return null;
			}
			NodeDeclareRef decl = (NodeDeclareRef) ann;
			ICodeNodeRef node = decl.getNode();
			if (node.getAnnType() != ICodeAnnotation.AnnType.METHOD) {
				return null;
			}
			MethodNode mth = (MethodNode) node;
			if (!mth.getMethodInfo().getRawFullId().equals(mthFullRawID)) {
				return null;
			}
			return new SimpleEntry<>(offset, ann);
		});

		if (toMthDecl == null) {
			return null;
		}

		Map.Entry<Integer, ICodeAnnotation> toMthEnd = area.getCodeMetadata().searchDown(toMthDecl.getKey(), (offset, ann) -> {
			if (ann.getAnnType() != ICodeAnnotation.AnnType.END) {
				return null;
			}
			return new SimpleEntry<>(offset, ann);
		});

		if (toMthEnd == null) {
			return null;
		}

		return new CodeMetadataRange(toMthDecl, toMthEnd);
	}

	@Nullable
	private CodeMetadataRange findEnclosingMethodRange(Integer startPos) {
		Map.Entry<Integer, ICodeAnnotation> mthDef = from.getCodeMetadata().searchUp(startPos, (offset, ann) -> {
			if (ann.getAnnType() != ICodeAnnotation.AnnType.DECLARATION) {
				return null;
			}
			NodeDeclareRef decl = (NodeDeclareRef) ann;
			ICodeNodeRef node = decl.getNode();
			if (node.getAnnType() != ICodeAnnotation.AnnType.METHOD) {
				return null;
			}
			return new SimpleEntry<>(offset, ann);
		});

		if (mthDef == null) {
			return null;
		}

		Map.Entry<Integer, ICodeAnnotation> mthEnd = from.getCodeMetadata().searchDown(startPos, (offset, ann) -> {
			if (ann.getAnnType() != ICodeAnnotation.AnnType.END) {
				return null;
			}
			return new SimpleEntry<>(offset, ann);
		});

		if (mthEnd == null) {
			return null;
		}

		return new CodeMetadataRange(mthDef, mthEnd);
	}

	/**
	 * Gets a CodeMetadataRange for the from CodeArea where start and end
	 * are InsnCodeOffsets whose offsets are monotonically increasing.
	 *
	 * @param - startPos the starting position to start searching from
	 * @param - mthDefPos the method node decl position enclosing the range
	 * @param - mthEndPos the method end position enclosing the range
	 */
	@Nullable
	private CodeMetadataRange findOffsetRange(Integer startPos, Integer mthDefPos, Integer mthEndPos) {
		Map.Entry<Integer, ICodeAnnotation> first = findInsnOffsetBeforePos(startPos, mthDefPos);
		Map.Entry<Integer, ICodeAnnotation> second = findInsnOffsetAfterPos(startPos, mthEndPos);
		if (first == null || second == null) {
			LOG.warn("{} - Unable to find InsnCodeOffsets between {} -> {}", LOG.getName(), mthDefPos, mthEndPos);
			return null;
		}
		int startOffset = ((InsnCodeOffset) first.getValue()).getOffset();
		int endOffset = ((InsnCodeOffset) second.getValue()).getOffset();
		if (startOffset > endOffset) {
			LOG.warn("{} - insn startOffset={} is greater than insn endOffset={} - cannot construct range", LOG.getName(), startOffset,
					endOffset);
			return null;
		}
		return new CodeMetadataRange(first, second);
	}

	@Nullable
	private Map.Entry<Integer, ICodeAnnotation> findInsnOffsetBeforePos(Integer startPos, Integer limit) {
		return from.getCodeMetadata().searchUp(startPos, (offset, ann) -> {
			if (offset <= limit) {
				return null;
			}
			if (ann.getAnnType() != ICodeAnnotation.AnnType.OFFSET) {
				return null;
			}
			return new SimpleEntry<Integer, ICodeAnnotation>(offset, ann);
		});
	}

	@Nullable
	private Map.Entry<Integer, ICodeAnnotation> findInsnOffsetAfterPos(Integer startPos, Integer limit) {
		return from.getCodeMetadata().searchDown(startPos, (offset, ann) -> {
			if (offset >= limit) {
				return null;
			}
			if (ann.getAnnType() != ICodeAnnotation.AnnType.OFFSET) {
				return null;
			}
			return new SimpleEntry<Integer, ICodeAnnotation>(offset, ann);
		});
	}

	/**
	 * Assumes that there is a NodeDeclareRef{MethodNode{}} annotation at mthDefPos in the `from`
	 * CodeInfoMetadata
	 */
	private String getMthRawFullID(Integer mthDefPos) {
		ICodeAnnotation ann = from.getCodeMetadata().getAt(mthDefPos);
		NodeDeclareRef ref = (NodeDeclareRef) ann;
		MethodNode mth = (MethodNode) ref.getNode();
		return mth.getMethodInfo().getRawFullId();
	}

	/**
	 * Gets the mapped smali line indices for the code offsets of interest
	 *
	 * @param smaliMethodNode     - method of interest
	 * @param insnCodeOffsetRange - code offset range from the caret pos
	 * @return
	 */
	private static List<Integer> getMappedSmaliLines(
			SmaliMethodNode smaliMethodNode,
			CodeMetadataRange insnCodeOffsetRange) {
		List<Integer> lines = new ArrayList<>();
		int startInsnCodeOffset = ((InsnCodeOffset) insnCodeOffsetRange.getStart().getValue()).getOffset();
		int endInsnCodeOffset = ((InsnCodeOffset) insnCodeOffsetRange.getEnd().getValue()).getOffset();
		// Line mappings are Line index -> Code offset
		Map<Integer, Integer> smaliLineMapping = smaliMethodNode.getLineMapping();
		LOG.debug("startInsnPos={}, endInsnPos={}", startInsnCodeOffset, endInsnCodeOffset);
		for (Map.Entry<Integer, Integer> lineToCodeOffset : smaliLineMapping.entrySet()) {
			LOG.debug("line={} -> codeOffset={}", lineToCodeOffset.getKey(), lineToCodeOffset.getValue());
			// Asume code offsets from smali debug utils are the same as those in the code metadata
			if (lineToCodeOffset.getValue() == startInsnCodeOffset || lineToCodeOffset.getValue() == endInsnCodeOffset) {
				lines.add(lineToCodeOffset.getKey());
			}
		}
		Collections.sort(lines); // only two elements
		return lines;
	}

}
