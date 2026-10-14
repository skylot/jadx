package jadx.gui.ui.dock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.event.CaretListener;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.codearea.sync.CodeAreaSyncee;
import jadx.gui.ui.codearea.sync.CodeAreaSyncer;
import jadx.gui.ui.codearea.sync.CodeAreaSyncerAbstractFactory;
import jadx.gui.ui.codearea.sync.fallback.FallbackSyncer;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.ui.ListenersHelper;

/**
 * Keeps the carets of split views of the same class in sync.
 */
public class SplitSync {
	private static final Logger LOG = LoggerFactory.getLogger(SplitSync.class);

	private final List<Link> links = new ArrayList<>();

	private static final class Link {
		final ClassCodeContentPanel a;
		final ClassCodeContentPanel b;
		final ListenersHelper<JTextComponent, CaretListener> carets = ListenersHelper.buildForCaretListener();
		final AtomicBoolean inProgress = new AtomicBoolean(false);
		final ChangeListener viewChange;

		Link(ClassCodeContentPanel a, ClassCodeContentPanel b) {
			this.a = a;
			this.b = b;
			this.viewChange = e -> rebind();
			a.addViewChangeListener(viewChange);
			b.addViewChangeListener(viewChange);
			rebind();
		}

		void rebind() {
			carets.removeAll();
			AbstractCodeArea left = a.getCurrentCodeArea();
			AbstractCodeArea right = b.getCurrentCodeArea();
			if (!(left instanceof CodeAreaSyncee) || !(right instanceof CodeAreaSyncee)) {
				return;
			}
			CodeAreaSyncer leftSyncer = syncerOf(left);
			CodeAreaSyncer rightSyncer = syncerOf(right);
			if (leftSyncer == null || rightSyncer == null) {
				return;
			}
			carets.add(left, e -> sync(left, right, leftSyncer));
			carets.add(right, e -> sync(right, left, rightSyncer));
		}

		void sync(AbstractCodeArea from, AbstractCodeArea to, CodeAreaSyncer syncer) {
			if (inProgress.get()) {
				return;
			}
			try {
				inProgress.set(true);
				if (!((CodeAreaSyncee) to).sync(syncer) && !FallbackSyncer.sync(from, to)) {
					LOG.debug("Code pane area sync not possible");
				}
			} catch (Exception ex) {
				LOG.debug("Failed to sync method/class across views: {}", ex.getLocalizedMessage());
			} finally {
				inProgress.set(false);
			}
		}

		void dispose() {
			carets.removeAll();
			a.removeViewChangeListener(viewChange);
			b.removeViewChangeListener(viewChange);
		}

		boolean has(ContentPanel panel) {
			return a == panel || b == panel;
		}

		private static @Nullable CodeAreaSyncer syncerOf(AbstractCodeArea codeArea) {
			if (codeArea instanceof CodeAreaSyncerAbstractFactory) {
				return ((CodeAreaSyncerAbstractFactory) codeArea).createCodeAreaSyncer();
			}
			return null;
		}
	}

	public void link(ContentPanel a, ContentPanel b) {
		if (a instanceof ClassCodeContentPanel && b instanceof ClassCodeContentPanel) {
			links.add(new Link((ClassCodeContentPanel) a, (ClassCodeContentPanel) b));
		}
	}

	public void unlink(ContentPanel panel) {
		links.removeIf(link -> {
			if (link.has(panel)) {
				link.dispose();
				return true;
			}
			return false;
		});
	}
}
