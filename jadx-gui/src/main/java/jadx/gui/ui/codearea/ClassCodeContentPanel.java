package jadx.gui.ui.codearea;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JCheckBox;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.DecompilationMode;
import jadx.gui.jobs.BackgroundExecutor;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.codearea.mode.JCodeMode;
import jadx.gui.ui.panel.IViewStateSupport;
import jadx.gui.utils.NLS;

import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TRAILING_COMPONENT;

/**
 * Displays one class with two different view:
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

	private boolean splitView = false;

	public ClassCodeContentPanel(TabbedPane panel, JClass jCls) {
		super(panel, jCls);

		javaCodePanel = new CodePanel(new CodeArea(this, jCls));
		smaliCodePanel = new CodePanel(new SmaliArea(this, jCls));
		areaTabbedPane = buildTabbedPane(jCls, false);
		addCustomControls(areaTabbedPane);

		initView();
		javaCodePanel.load();
	}

	private void initView() {
		removeAll();
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
		if (splitView) {
			JTabbedPane splitPaneView = buildTabbedPane(((JClass) node), true);
			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, areaTabbedPane, splitPaneView);
			add(splitPane);
			splitPane.setDividerLocation(0.5);
			splitPaneView.setSelectedIndex(1);
		} else {
			add(areaTabbedPane);
		}
		invalidate();
	}

	private JTabbedPane buildTabbedPane(JClass jCls, boolean split) {
		JTabbedPane areaTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
		areaTabbedPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		areaTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		if (split) {
			areaTabbedPane.add(new CodePanel(new CodeArea(this, jCls)), NLS.str("tabs.code"));
			areaTabbedPane.add(new CodePanel(new SmaliArea(this, jCls)), NLS.str("tabs.smali"));
		} else {
			areaTabbedPane.add(javaCodePanel, NLS.str("tabs.code"));
			areaTabbedPane.add(smaliCodePanel, NLS.str("tabs.smali"));
		}
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

	private void execInBackground(Runnable runnable) {
		BackgroundExecutor bgExec = this.tabbedPane.getMainWindow().getBackgroundExecutor();
		bgExec.execute("Loading", runnable);
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
	public EditorViewState getEditorViewState() {
		CodePanel codePanel = (CodePanel) areaTabbedPane.getSelectedComponent();
		int caretPos = codePanel.getCodeArea().getCaretPosition();
		Point viewPoint = codePanel.getCodeScrollPane().getViewport().getViewPosition();
		String subPath = codePanel == javaCodePanel ? "java" : "smali";
		return new EditorViewState(getNode(), subPath, caretPos, viewPoint);
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
}
