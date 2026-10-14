package jadx.gui.ui.codearea;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.DecompilationMode;
import jadx.core.utils.Utils;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.codearea.mode.JCodeMode;
import jadx.gui.ui.panel.IViewStateSupport;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

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
	private final JTabbedPane areaTabbedPane;
	private CodePanel javaCodePanel;
	private CodePanel smaliCodePanel;

	public ClassCodeContentPanel(TabbedPane panel, JClass jClass) {
		super(panel, jClass);
		jCls = jClass;
		areaTabbedPane = buildTabbedPane(jClass);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
		add(areaTabbedPane);
		activateCodePanel(javaCodePanel);
	}

	private JTabbedPane buildTabbedPane(JClass jCls) {
		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);
		tabbedPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		javaCodePanel = new CodePanel(new CodeArea(this, jCls));
		smaliCodePanel = new CodePanel(new SmaliArea(this, jCls, false));
		tabbedPane.add(javaCodePanel, NLS.str("tabs.code"));
		tabbedPane.add(smaliCodePanel, NLS.str("tabs.smali"));
		tabbedPane.add(new CodePanel(new SmaliArea(this, jCls, true)), NLS.str("tabs.smali_bytecode"));
		tabbedPane.add(new CodePanel(new CodeArea(this, new JCodeMode(jCls, DecompilationMode.SIMPLE))), "Simple");
		tabbedPane.add(new CodePanel(new CodeArea(this, new JCodeMode(jCls, DecompilationMode.FALLBACK))), "Fallback");
		tabbedPane.setMinimumSize(new Dimension(200, 200));
		tabbedPane.setFocusable(false);
		tabbedPane.addChangeListener(e -> ((CodePanel) tabbedPane.getSelectedComponent()).load());
		return tabbedPane;
	}

	private void activateCodePanel(CodePanel codePanel) {
		if (areaTabbedPane.getSelectedComponent() == codePanel) {
			codePanel.load();
		} else {
			areaTabbedPane.setSelectedComponent(codePanel);
		}
	}

	@Override
	public void loadSettings() {
		for (Component component : areaTabbedPane.getComponents()) {
			if (component instanceof CodePanel) {
				((CodePanel) component).loadSettings();
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
		boolean toSmali = areaTabbedPane.getSelectedComponent() == javaCodePanel;
		activateCodePanel(toSmali ? smaliCodePanel : javaCodePanel);
	}

	public AbstractCodeArea getCurrentCodeArea() {
		return ((CodePanel) areaTabbedPane.getSelectedComponent()).getCodeArea();
	}

	public AbstractCodeArea getSmaliCodeArea() {
		return smaliCodePanel.getCodeArea();
	}

	public void showSmaliPane() {
		activateCodePanel(smaliCodePanel);
	}

	public void showPaneLike(ClassCodeContentPanel other) {
		int index = other.areaTabbedPane.getSelectedIndex();
		if (index >= 0 && index < areaTabbedPane.getTabCount()) {
			activateCodePanel((CodePanel) areaTabbedPane.getComponentAt(index));
		}
	}

	/**
	 * Notified when the inner view (code, smali, ...) changes.
	 */
	public void addViewChangeListener(ChangeListener listener) {
		areaTabbedPane.addChangeListener(listener);
	}

	public void removeViewChangeListener(ChangeListener listener) {
		areaTabbedPane.removeChangeListener(listener);
	}

	@Override
	public void saveEditorViewState(EditorViewState viewState) {
		CodePanel codePanel = (CodePanel) areaTabbedPane.getSelectedComponent();
		int caretPos = codePanel.getCodeArea().getCaretPosition();
		Point viewPoint = codePanel.getCodeScrollPane().getViewport().getViewPosition();
		viewState.setSubPath(String.valueOf(areaTabbedPane.getSelectedIndex()));
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
				activePanel = (CodePanel) areaTabbedPane.getComponentAt(index);
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
		for (Component component : areaTabbedPane.getComponents()) {
			if (component instanceof CodePanel) {
				((CodePanel) component).dispose();
			}
		}
		super.dispose();
	}
}
