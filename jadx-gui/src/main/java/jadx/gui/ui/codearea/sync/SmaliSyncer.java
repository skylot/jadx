package jadx.gui.ui.codearea.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.SmaliArea;

/**
 * Syncs a Smali code panel area to another area
 */
public class SmaliSyncer implements CodePanelSyncer {
	private static final Logger LOG = LoggerFactory.getLogger(SmaliSyncer.class);

	private final SmaliArea from;
	private final InsnOffsetSmaliSyncer insnOffsetSyncer;
	private final DebugLineSmaliSyncer debugLineSyncer;

	public SmaliSyncer(SmaliArea area) {
		this.from = area;
		this.insnOffsetSyncer = new InsnOffsetSmaliSyncer(area);
		this.debugLineSyncer = new DebugLineSmaliSyncer(area);
	}

	@Override
	public boolean syncTo(CodeArea to) {
		// first try debug lines then insn offsets
		return debugLineSyncer.syncTo(to) || insnOffsetSyncer.syncTo(to);
	}

	@Override
	public boolean syncTo(SmaliArea to) {
		if (from.isShowingDalvikBytecode() == to.isShowingDalvikBytecode()) {
			// smali -> smali just highlight the current line but only if content is the same
			to.scrollToPos(from.getLineStartOffsetOfCurrentLine());
		}
		return true; // Prevent fallback syncing
	}
}
