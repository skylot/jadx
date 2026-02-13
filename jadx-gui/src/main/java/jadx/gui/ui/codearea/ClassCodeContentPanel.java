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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.DecompilationMode;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.codearea.mode.JCodeMode;
import jadx.gui.ui.codearea.sync.CodePanelSyncee;
import jadx.gui.ui.codearea.sync.CodePanelSyncer;
import jadx.gui.ui.codearea.sync.CodePanelSyncerAbstractFactory;
import jadx.gui.ui.codearea.sync.fallback.FallbackSyncer;
import jadx.gui.ui.panel.IViewStateSupport;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.NLS;

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

	private final transient CodePanel javaCodePanel;
	private final transient CodePanel smaliCodePanel;
	private final transient JTabbedPane areaTabbedPane;
	private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

	private boolean splitView = false;

	public ClassCodeContentPanel(TabbedPane panel, JClass jCls) {
		super(panel, jCls);

		javaCodePanel = new CodePanel(new CodeArea(this, jCls));
		smaliCodePanel = new CodePanel(new SmaliArea(this, jCls, false));
		areaTabbedPane = buildTabbedPane(jCls);
		addCustomControls(areaTabbedPane);

		javaCodePanel.load();
		initView();
	}

	private void initView() {
		removeAll();
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
		if (splitView) {
			setupSplitPane();
		} else {
			javaCodePanel.load();
			smaliCodePanel.load();
			attachSyncListeners(javaCodePanel, smaliCodePanel);
			areaTabbedPane.setSelectedIndex(0); // default to Java
			add(areaTabbedPane);
		}
		revalidate();
		repaint();
	}

	private void attachSyncListeners(CodePanel javaPanel, CodePanel smaliPanel) {
		javaPanel.getCodeArea().addCaretListener(e -> {
			if (syncInProgress.get()) {
				return;
			}
			syncInProgress.set(true);
			syncToMethod(javaPanel, smaliPanel);
			syncInProgress.set(false);
		});

		smaliPanel.getCodeArea().addCaretListener(e -> {
			if (syncInProgress.get()) {
				return;
			}
			syncInProgress.set(true);
			syncToMethod(smaliPanel, javaPanel);
			syncInProgress.set(false);
		});
	}

	private void setupSplitPane() {
		JTabbedPane leftTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
		JTabbedPane rightTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);

		CodePanel[] leftPanels = {
				new CodePanel(new CodeArea(this, (JClass) node)), // Java
				new CodePanel(new SmaliArea(this, (JClass) node, false)), // Smali
				new CodePanel(new SmaliArea(this, (JClass) node, true)), // Smali with Dalvik
				new CodePanel(new CodeArea(this, new JCodeMode((JClass) node, DecompilationMode.SIMPLE))), // Simple
				new CodePanel(new CodeArea(this, new JCodeMode((JClass) node, DecompilationMode.FALLBACK))) // Fallback
		};

		CodePanel[] rightPanels = {
				new CodePanel(new SmaliArea(this, (JClass) node, false)), // Smali
				new CodePanel(new SmaliArea(this, (JClass) node, true)), // Smali with Dalvik
				new CodePanel(new CodeArea(this, (JClass) node)), // Java
				new CodePanel(new CodeArea(this, new JCodeMode((JClass) node, DecompilationMode.SIMPLE))), // Simple
				new CodePanel(new CodeArea(this, new JCodeMode((JClass) node, DecompilationMode.FALLBACK))) // Fallback
		};

		leftTabbedPane.add(leftPanels[0], NLS.str("tabs.code"));
		leftTabbedPane.add(leftPanels[1], NLS.str("tabs.smali"));
		leftTabbedPane.add(leftPanels[2], NLS.str("tabs.smali_bytecode"));
		leftTabbedPane.add(leftPanels[3], "Simple");
		leftTabbedPane.add(leftPanels[4], "Fallback");

		rightTabbedPane.add(rightPanels[0], NLS.str("tabs.smali"));
		rightTabbedPane.add(rightPanels[1], NLS.str("tabs.smali_bytecode"));
		rightTabbedPane.add(rightPanels[2], NLS.str("tabs.code"));
		rightTabbedPane.add(rightPanels[3], "Simple");
		rightTabbedPane.add(rightPanels[4], "Fallback");

		for (CodePanel p : leftPanels) {
			p.load();
		}
		for (CodePanel p : rightPanels) {
			p.load();
		}

		leftTabbedPane.addChangeListener(e -> ((CodePanel) leftTabbedPane.getSelectedComponent()).load());
		rightTabbedPane.addChangeListener(e -> ((CodePanel) rightTabbedPane.getSelectedComponent()).load());

		// Attach caret sync between all combinations
		for (CodePanel leftPanel : leftPanels) {
			for (CodePanel rightPanel : rightPanels) {
				attachSyncListeners(leftPanel, rightPanel);
			}
		}

		// Create and configure split pane
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftTabbedPane, rightTabbedPane);
		splitPane.setResizeWeight(0.5);
		leftTabbedPane.setMinimumSize(new Dimension(200, 200));
		rightTabbedPane.setMinimumSize(new Dimension(200, 200));
		add(splitPane);

		// Set divider location after layout
		SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.5));

		rightTabbedPane.setSelectedIndex(0);
		addCustomControls(leftTabbedPane);
	}

	private JTabbedPane buildTabbedPane(JClass jCls) {
		JTabbedPane areaTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
		areaTabbedPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		areaTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		areaTabbedPane.add(javaCodePanel, NLS.str("tabs.code"));
		areaTabbedPane.add(smaliCodePanel, NLS.str("tabs.smali"));
		areaTabbedPane.add(new CodePanel(new SmaliArea(this, jCls, true)), NLS.str("tabs.smali_bytecode"));
		areaTabbedPane.add(new CodePanel(new CodeArea(this, new JCodeMode(jCls, DecompilationMode.SIMPLE))), "Simple");
		areaTabbedPane.add(new CodePanel(new CodeArea(this, new JCodeMode(jCls, DecompilationMode.FALLBACK))), "Fallback");
		areaTabbedPane.addChangeListener(e -> {
			CodePanel selectedPanel = (CodePanel) areaTabbedPane.getSelectedComponent();
			// TODO: to run background load extract ui update to other method
			selectedPanel.load();
			// execInBackground(selectedPanel::load);
		});
		return areaTabbedPane;
	}

	private void addCustomControls(JTabbedPane tabbedPane) {
		JCheckBox splitCheckBox = new JCheckBox("Split view", splitView);
		splitCheckBox.addItemListener(e -> {
			splitView = splitCheckBox.isSelected();
			this.initView();
		});

		JToolBar trailing = new JToolBar();
		trailing.setFloatable(false);
		trailing.setBorder(null);
		// trailing.add(Box.createHorizontalGlue());
		trailing.addSeparator(new Dimension(50, 1));
		trailing.add(splitCheckBox);
		tabbedPane.putClientProperty(TABBED_PANE_TRAILING_COMPONENT, trailing);
	}

	@Override
	public void loadSettings() {
		javaCodePanel.loadSettings();
		smaliCodePanel.loadSettings();
		updateUI();
	}

	@Override
	public AbstractCodeArea getCodeArea() {
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
		boolean toSmali = areaTabbedPane.getSelectedComponent() == javaCodePanel;
		areaTabbedPane.setSelectedComponent(toSmali ? smaliCodePanel : javaCodePanel);
	}

	public AbstractCodeArea getCurrentCodeArea() {
		return ((CodePanel) areaTabbedPane.getSelectedComponent()).getCodeArea();
	}

	public AbstractCodeArea getSmaliCodeArea() {
		return smaliCodePanel.getCodeArea();
	}

	public void showSmaliPane() {
		areaTabbedPane.setSelectedComponent(smaliCodePanel);
	}

	@Override
	public void saveEditorViewState(EditorViewState viewState) {
		CodePanel codePanel = (CodePanel) areaTabbedPane.getSelectedComponent();
		int caretPos = codePanel.getCodeArea().getCaretPosition();
		Point viewPoint = codePanel.getCodeScrollPane().getViewport().getViewPosition();
		String subPath = codePanel == javaCodePanel ? "java" : "smali";
		viewState.setSubPath(subPath);
		viewState.setCaretPos(caretPos);
		viewState.setViewPoint(viewPoint);
	}

	@Override
	public void restoreEditorViewState(EditorViewState viewState) {
		boolean isJava = viewState.getSubPath().equals("java");
		CodePanel activePanel = isJava ? javaCodePanel : smaliCodePanel;
		areaTabbedPane.setSelectedComponent(activePanel);
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
		javaCodePanel.dispose();
		smaliCodePanel.dispose();
		for (Component component : areaTabbedPane.getComponents()) {
			if (component instanceof CodePanel) {
				((CodePanel) component).dispose();
			}
		}
		super.dispose();
	}

	private void syncToMethod(CodePanel fromPanel, CodePanel toPanel) {
		if (!fromPanel.isShowing() || !toPanel.isShowing()) {
			return;
		}
		try {
			AbstractCodeArea from = fromPanel.getCodeArea();
			AbstractCodeArea to = toPanel.getCodeArea();
			toPanel.load();

			if (from instanceof CodePanelSyncerAbstractFactory && to instanceof CodePanelSyncee) {
				CodePanelSyncer syncer = ((CodePanelSyncerAbstractFactory) from).createCodePanelSyncer();
				if (((CodePanelSyncee) to).sync(syncer)) {
					return;
				}
			}
			if (!FallbackSyncer.sync(fromPanel, toPanel)) {
				LOG.warn("Code pane area sync not possible");
			}
		} catch (Exception ex) {
			LOG.warn("Failed to sync method/class across views: {}", ex.getLocalizedMessage());
		}
	}
}
