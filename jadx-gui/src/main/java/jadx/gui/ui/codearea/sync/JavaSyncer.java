package jadx.gui.ui.codearea.sync;

import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.SmaliArea;

/**
 * Syncs a Java code panel area (Java/Simple/Fallback) to another area
 */
public class JavaSyncer implements CodeAreaSyncer {
	private final DebugLineJavaSyncer debugLineSyncer;
	private final InsnOffsetJavaSyncer insnOffsetSyncer;

	public JavaSyncer(CodeArea area) {
		this.debugLineSyncer = new DebugLineJavaSyncer(area);
		this.insnOffsetSyncer = new InsnOffsetJavaSyncer(area);
	}

	@Override
	public boolean syncTo(CodeArea to) {
		return debugLineSyncer.syncTo(to) || insnOffsetSyncer.syncTo(to);
	}

	@Override
	public boolean syncTo(SmaliArea to) {
		return debugLineSyncer.syncTo(to) || insnOffsetSyncer.syncTo(to);
	}
}
