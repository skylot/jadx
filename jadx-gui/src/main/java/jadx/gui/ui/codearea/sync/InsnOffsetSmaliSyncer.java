package jadx.gui.ui.codearea.sync;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeMetadata;
import jadx.api.metadata.annotations.InsnCodeOffset;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.core.dex.nodes.MethodNode;
import jadx.gui.device.debugger.DbgUtils;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.SmaliArea;

/*
 * Use insn code offsets to sync smali to code panel area
 * This only works for Smali when the SmaliArea is showing the dalvik bytecode
 */
public class InsnOffsetSmaliSyncer implements IToJavaSyncStrategy {
	private static final Logger LOG = LoggerFactory.getLogger(InsnOffsetSmaliSyncer.class);

	private final SmaliArea from;

	public InsnOffsetSmaliSyncer(SmaliArea area) {
		this.from = area;
	}

	@Override
	public boolean syncTo(CodeArea to) {
		if (!from.isShowingDalvikBytecode()) {
			// This strategy can only be used when the debug model has been used to generate the smali.
			// This populates the code offsets by line as opposed to just text.
			return false;
		}
		// 1. Get the code offset from the Smali caret line number
		// 2. Find the appropriate NodeDeclareRef for the method enclosed in the CodeArea annotations
		// 3. Find all code offset range intervals in the map which contain the code offset
		// 4. Get the CodeArea positions of these intervals and hightlight them in the code area
		// 5. Scroll to the first one.
		JClass jclass = from.getJClass();
		Map.Entry<String, Integer> lineInfo = DbgUtils.getCodeOffsetInfoByLine(jclass, from.getCaretLineNumber());
		if (lineInfo == null) {
			return false;
		}
		Integer lineInfoPos = lineInfo.getValue();
		LOG.debug("lineInfo key {}, lineInfo value {}, caretLineNumber {}", lineInfo.getKey(), lineInfo.getValue(),
				from.getCaretLineNumber());
		ICodeMetadata toMetadata = to.getCodeMetadata();
		NavigableMap<Integer, ICodeAnnotation> codeAreaAnnotationMap =
				(NavigableMap<Integer, ICodeAnnotation>) toMetadata.getAsMap();
		Iterator<NavigableMap.Entry<Integer, ICodeAnnotation>> methodDecl =
				findMethodDeclAnnotation(codeAreaAnnotationMap, lineInfo.getKey());
		if (methodDecl == null) {
			LOG.warn("{} - No NodeDeclareRef exists for {}", LOG.getName(), lineInfo.getKey());
			return false;
		}
		// Looking through the annotations in order from the Method declaration to its end
		// compare every adjacent pair of instruction offsets where the second is greater than the first.
		// Highlight if the smali offset falls between the second and the first.
		Iterator<NavigableMap.Entry<Integer, ICodeAnnotation>> it = methodDecl;
		NavigableMap.Entry<Integer, ICodeAnnotation> prev = null;
		List<CodeMetadataRange> offsetBoundariesToHighlight = new ArrayList<>();
		while (it.hasNext()) {
			NavigableMap.Entry<Integer, ICodeAnnotation> entry = it.next();
			if (entry.getValue().getAnnType() == ICodeAnnotation.AnnType.END) {
				break;
			}
			if (entry.getValue().getAnnType() != ICodeAnnotation.AnnType.OFFSET) {
				continue;
			}
			if (prev != null) {
				InsnCodeOffset currentInsnOffset = (InsnCodeOffset) entry.getValue();
				InsnCodeOffset prevInsnOffset = (InsnCodeOffset) prev.getValue();
				if (prevInsnOffset.getOffset() <= lineInfoPos && lineInfoPos <= currentInsnOffset.getOffset()) {
					offsetBoundariesToHighlight.add(new CodeMetadataRange(prev, entry));
				}
			}
			prev = entry;
		}

		if (offsetBoundariesToHighlight.isEmpty()) {
			return false;
		}

		to.scrollToPos(offsetBoundariesToHighlight.get(0).getStart().getKey());

		try {
			for (CodeMetadataRange cmr : offsetBoundariesToHighlight) {
				LOG.debug("Highlighting {}", cmr);
				CodeSyncHighlighter.defaultHighlighter().highlightRange(to, cmr.getStart().getKey(), cmr.getEnd().getKey());
			}
			LOG.info("{} - successful sync of smali to code", LOG.getName());
			return true;
		} catch (Exception ex) {
			LOG.error("{} - Unable to highlight smali -> code insn offset range: {}", LOG.getName(), ex.getLocalizedMessage());
		}
		return false;
	}

	/**
	 * Find the NodeDeclareRef annotation of the method identified by smaliLineMthFullID
	 *
	 * @param map                the annotation map from the CodeArea
	 * @param smaliLineMthFullID the raw full method ID to look for
	 * @return iterator to the entry in the annotation map
	 */
	@Nullable
	private static Iterator<NavigableMap.Entry<Integer, ICodeAnnotation>> findMethodDeclAnnotation(
			NavigableMap<Integer, ICodeAnnotation> map,
			String smaliLineMthFullID) {
		// Ensure we use NavigableMap here to get ordering guarantee from iterator call
		Iterator<NavigableMap.Entry<Integer, ICodeAnnotation>> it = map.descendingMap().entrySet().iterator();
		while (it.hasNext()) {
			NavigableMap.Entry<Integer, ICodeAnnotation> entry = it.next();
			if (entry.getValue() instanceof NodeDeclareRef) {
				NodeDeclareRef nodeDeclareRef = (NodeDeclareRef) entry.getValue();
				if (nodeDeclareRef.getNode() instanceof MethodNode) {
					MethodNode mth = (MethodNode) nodeDeclareRef.getNode();
					if (mth.getMethodInfo().getRawFullId().equals(smaliLineMthFullID)) {
						return it;
					}
				}
			}
		}
		return null;
	}
}
