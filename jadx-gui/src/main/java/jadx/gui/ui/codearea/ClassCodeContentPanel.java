package jadx.gui.ui.codearea;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JCheckBox;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretListener;
import javax.swing.text.JTextComponent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.DecompilationMode;
import jadx.core.utils.Utils;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.codearea.mode.JCodeMode;
import jadx.gui.ui.codearea.sync.CodeAreaSyncee;
import jadx.gui.ui.codearea.sync.CodeAreaSyncer;
import jadx.gui.ui.codearea.sync.CodeAreaSyncerAbstractFactory;
import jadx.gui.ui.codearea.sync.fallback.FallbackSyncer;
import jadx.gui.ui.panel.IViewStateSupport;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.ListenersHelper;

import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TRAILING_COMPONENT;

/**
 * Displays one class with two different views:
 *
 * <ul>
 * <li>Java source code of the selected class (default)</li>
 * <li>Smali source code of the selected class</li>
 * </ul>
 */
public final class ClassCodeContentPanel extends AbstractCodeContentPanel implements IViewStateSupport {
	private static final Logger LOG = LoggerFactory.getLogger(ClassCodeContentPanel.class);
	private static final long serialVersionUID = -7229931102504634591L;

	private final JClass jCls;
	private final ListenersHelper<JTextComponent, CaretListener> caretListeners = ListenersHelper.buildForCaretListener();
	private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

	private final JTabbedPane leftTabbedPane;
	private @Nullable JTabbedPane rightTabbedPane;
	private CodePanel javaCodePanel;
	private CodePanel smaliCodePanel;

	private boolean isSplitViewActivated = false;

	public ClassCodeContentPanel(TabbedPane panel, JClass jClass) {
		super(panel, jClass);
		jCls = jClass;
		leftTabbedPane = buildTabbedPane(jClass, true);
		addCustomControls(leftTabbedPane);
		initView();
		activateCodePanel(javaCodePanel);
	}

	private void initView() {
		removeAll();
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
		if (isSplitViewActivated) {
			rightTabbedPane = buildTabbedPane(jCls, false);
			rightTabbedPane.setSelectedIndex(1); // default to Smali

			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftTabbedPane, rightTabbedPane);
			splitPane.setResizeWeight(0.5);
			add(splitPane);
			revalidate();
			repaint();

			// set divider location after layout
			SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.5));
		} else {
			disposeTabbedPane(rightTabbedPane);
			rightTabbedPane = null;
			add(leftTabbedPane);
			revalidate();
			repaint();
		}
	}

	private JTabbedPane buildTabbedPane(JClass jCls, boolean leftPanel) {
		JTabbedPane areaTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
		areaTabbedPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		areaTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		CodePanel javaPanel = new CodePanel(new CodeArea(this, jCls));
		CodePanel smaliPanel = new CodePanel(new SmaliArea(this, jCls, false));
		if (leftPanel) {
			this.javaCodePanel = javaPanel;
			this.smaliCodePanel = smaliPanel;
		}
		areaTabbedPane.add(javaPanel, NLS.str("tabs.code"));
		areaTabbedPane.add(smaliPanel, NLS.str("tabs.smali"));
		areaTabbedPane.add(new CodePanel(new SmaliArea(this, jCls, true)), NLS.str("tabs.smali_bytecode"));
		areaTabbedPane.add(new CodePanel(new CodeArea(this, new JCodeMode(jCls, DecompilationMode.SIMPLE))), "Simple");
		areaTabbedPane.add(new CodePanel(new CodeArea(this, new JCodeMode(jCls, DecompilationMode.FALLBACK))), "Fallback");
		areaTabbedPane.setMinimumSize(new Dimension(200, 200));
		areaTabbedPane.addChangeListener(e -> onCodePanelActivation((CodePanel) areaTabbedPane.getSelectedComponent()));
		return areaTabbedPane;
	}

	private void onCodePanelActivation(CodePanel selectedPanel) {
		selectedPanel.load();
		updateSync();
	}

	private void activateCodePanel(CodePanel javaCodePanel) {
		if (leftTabbedPane.getSelectedComponent() == javaCodePanel) {
			// already selected, change listener will not be called, run update manually
			onCodePanelActivation(javaCodePanel);
		} else {
			leftTabbedPane.setSelectedComponent(javaCodePanel);
		}
	}

	private void addCustomControls(JTabbedPane tabbedPane) {
		JCheckBox splitCheckBox = new JCheckBox("Split view", false);
		splitCheckBox.addItemListener(e -> {
			boolean newSplitView = splitCheckBox.isSelected();
			if (isSplitViewActivated != newSplitView) {
				isSplitViewActivated = newSplitView;
				initView();
			}
		});

		JToolBar trailing = new JToolBar();
		trailing.setFloatable(false);
		trailing.setBorder(null);
		// trailing.add(Box.createHorizontalGlue());
		trailing.addSeparator(new Dimension(50, 1));
		trailing.add(splitCheckBox);
		tabbedPane.putClientProperty(TABBED_PANE_TRAILING_COMPONENT, trailing);
	}

	private void updateSync() {
		caretListeners.removeAll();
		if (!isSplitViewActivated) {
			return;
		}
		AbstractCodeArea leftArea = getCodePanel(leftTabbedPane).getCodeArea();
		AbstractCodeArea rightArea = getCodePanel(rightTabbedPane).getCodeArea();
		if (leftArea instanceof CodeAreaSyncee && rightArea instanceof CodeAreaSyncee) {
			CodeAreaSyncer leftSyncer = buildCodeAreaSyncer(leftArea);
			CodeAreaSyncer rightSyncer = buildCodeAreaSyncer(rightArea);
			if (leftSyncer != null && rightSyncer != null) {
				caretListeners.add(leftArea, e -> syncCodeArea(leftArea, rightArea, leftSyncer));
				caretListeners.add(rightArea, e -> syncCodeArea(rightArea, leftArea, rightSyncer));
			}
		}
	}

	private void syncCodeArea(AbstractCodeArea fromArea, AbstractCodeArea toArea, CodeAreaSyncer syncer) {
		if (syncInProgress.get()) {
			return;
		}
		try {
			syncInProgress.set(true);
			boolean synced = ((CodeAreaSyncee) toArea).sync(syncer);
			if (!synced) {
				if (!FallbackSyncer.sync(fromArea, toArea)) {
					LOG.warn("Code pane area sync not possible");
				}
			}
		} catch (Exception ex) {
			LOG.warn("Failed to sync method/class across views: {}", ex.getLocalizedMessage());
		} finally {
			syncInProgress.set(false);
		}
	}

	private static CodePanel getCodePanel(@Nullable JTabbedPane tabbedPane) {
		if (tabbedPane == null) {
			throw new IllegalStateException("tabbedPane is null");
		}
		return (CodePanel) tabbedPane.getSelectedComponent();
	}

	private static @Nullable CodeAreaSyncer buildCodeAreaSyncer(AbstractCodeArea codeArea) {
		if (codeArea instanceof CodeAreaSyncerAbstractFactory) {
			return ((CodeAreaSyncerAbstractFactory) codeArea).createCodeAreaSyncer();
		}
		return null;
	}

	@Override
	public void loadSettings() {
		for (Component component : leftTabbedPane.getComponents()) {
			if (component instanceof CodePanel) {
				((CodePanel) component).loadSettings();
			}
		}
		if (rightTabbedPane != null) {
			for (Component component : rightTabbedPane.getComponents()) {
				if (component instanceof CodePanel) {
					((CodePanel) component).loadSettings();
				}
			}
		}
		updateUI();
	}

	@Override
	public @NotNull AbstractCodeArea getCodeArea() {
		return javaCodePanel.getCodeArea();
	}

	@Override
	public Component getChildrenComponent() {
		return getCodeArea();
	}

	public CodePanel getJavaCodePanel() {
		return javaCodePanel;
	}

	public void switchPanel() {
		boolean toSmali = leftTabbedPane.getSelectedComponent() == javaCodePanel;
		activateCodePanel(toSmali ? smaliCodePanel : javaCodePanel);
	}

	public AbstractCodeArea getCurrentCodeArea() {
		return ((CodePanel) leftTabbedPane.getSelectedComponent()).getCodeArea();
	}

	public AbstractCodeArea getSmaliCodeArea() {
		return smaliCodePanel.getCodeArea();
	}

	public void showSmaliPane() {
		activateCodePanel(smaliCodePanel);
	}

	@Override
	public void saveEditorViewState(EditorViewState viewState) {
		CodePanel codePanel = (CodePanel) leftTabbedPane.getSelectedComponent();
		int caretPos = codePanel.getCodeArea().getCaretPosition();
		Point viewPoint = codePanel.getCodeScrollPane().getViewport().getViewPosition();
		viewState.setSubPath(String.valueOf(leftTabbedPane.getSelectedIndex()));
		viewState.setCaretPos(caretPos);
		viewState.setViewPoint(viewPoint);
	}

	@Override
	public void restoreEditorViewState(EditorViewState viewState) {
		UiUtils.uiThreadGuard();
		String subPath = viewState.getSubPath();
		CodePanel activePanel = null;
		if (subPath.equals("java")) {
			activePanel = javaCodePanel;
		} else if (subPath.equals("smali")) {
			activePanel = smaliCodePanel;
		} else {
			try {
				int index = Utils.safeParseInt(subPath, 0);
				activePanel = (CodePanel) leftTabbedPane.getComponentAt(index);
			} catch (Exception e) {
				LOG.debug("Failed to restore active code panel: {}", subPath, e);
			}
		}
		if (activePanel == null) {
			return;
		}
		activateCodePanel(activePanel);
		try {
			activePanel.getCodeScrollPane().getViewport().setViewPosition(viewState.getViewPoint());
		} catch (Exception e) {
			LOG.debug("Failed to restore view position: {}", viewState.getViewPoint(), e);
		}
		int caretPos = viewState.getCaretPos();
		try {
			AbstractCodeArea codeArea = activePanel.getCodeArea();
			int codeLen = codeArea.getDocument().getLength();
			if (caretPos >= 0 && caretPos < codeLen) {
				codeArea.setCaretPosition(caretPos);
			}
		} catch (Exception e) {
			LOG.debug("Failed to restore caret position: {}", caretPos, e);
		}
	}

	@Override
	public void dispose() {
		caretListeners.removeAll();
		disposeTabbedPane(leftTabbedPane);
		disposeTabbedPane(rightTabbedPane);
		super.dispose();
	}

	private void disposeTabbedPane(@Nullable JTabbedPane tabbedPane) {
		if (tabbedPane != null) {
			for (Component component : tabbedPane.getComponents()) {
				if (component instanceof CodePanel) {
					((CodePanel) component).dispose();
				}
			}
		}
	}
}
