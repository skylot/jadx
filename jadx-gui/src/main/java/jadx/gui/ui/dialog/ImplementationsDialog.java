package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.core.dex.nodes.MethodNode;
import jadx.gui.jobs.SimpleTask;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.jobs.TaskWithExtraOnFinish;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.NodeLabel;

public class ImplementationsDialog extends CommonSearchDialog {
	private static final long serialVersionUID = -3765193640217824513L;

	private final transient JMethod node;

	private transient List<JNode> implList;

	public static void open(MainWindow mainWindow, JMethod node) {
		if (node == null) {
			return;
		}
		ImplementationsDialog dialog = new ImplementationsDialog(mainWindow, node);
		mainWindow.addLoadListener(loaded -> {
			if (!loaded) {
				dialog.dispose();
				return true;
			}
			return false;
		});
		dialog.setVisible(true);
	}

	private ImplementationsDialog(MainWindow mainWindow, JMethod node) {
		super(mainWindow, NLS.str("implementations_dialog.title"));
		this.node = node;

		initUI();
		registerInitOnOpen();
		loadWindowPos();
	}

	@Override
	protected void openInit() {
		loadStart();
		progressStartCommon();
		mainWindow.getBackgroundExecutor().execute(NLS.str("progress.load"),
				this::collectImplementations,
				status -> {
					if (status == TaskStatus.CANCEL_BY_MEMORY) {
						mainWindow.showHeapUsageBar();
						UiUtils.errorMessage(this, NLS.str("message.memoryLow"));
					}
					progressFinishedCommon();
					loadFinished();
				});
	}

	@Override
	protected void openItem(JNode item) {
		if (item instanceof JMethod) {
			JMethod jMethod = (JMethod) item;
			JClass jumpRootCls = resolveCodeRootClass(jMethod);
			if (jumpRootCls != null) {
				SimpleTask loadTask = jumpRootCls.getLoadTask();
				if (loadTask != null) {
					if (!mainWindow.getSettings().isKeepCommonDialogOpen()) {
						dispose();
					}
					mainWindow.getBackgroundExecutor().execute(
							new TaskWithExtraOnFinish(loadTask, () -> tabsController.codeJump(resolveJumpNode(jMethod))));
					return;
				}
			}
			super.openItem(resolveJumpNode(jMethod));
			return;
		}
		super.openItem(item);
	}

	private JNode resolveJumpNode(JMethod jMethod) {
		MethodNode mth = jMethod.getJavaMethod().getMethodNode();
		MethodNode jumpMth = MethodImplementationsFinder.resolveJumpMethod(mth);
		if (jumpMth != mth) {
			JNode jumpNode = getNodeCache().makeFrom(jumpMth);
			if (jumpNode != null) {
				return jumpNode;
			}
		}
		return jMethod;
	}

	private @Nullable JClass resolveCodeRootClass(JMethod jMethod) {
		JClass parentCls = jMethod.getJParent();
		if (parentCls != null) {
			JavaClass cls = parentCls.getCls();
			JavaClass origTopCls = cls.getOriginalTopParentClass();
			JavaClass codeParent = cls.getTopParentClass();
			if (!Objects.equals(codeParent, origTopCls)) {
				return getNodeCache().makeFrom(codeParent);
			}
		}
		return jMethod.getRootClass();
	}

	private void collectImplementations() {
		List<JavaMethod> methods = MethodImplementationsFinder.collectImplementations(node.getJavaMethod());
		implList = new ArrayList<>(methods.size());
		JNodeCache nodeCache = getNodeCache();
		for (JavaMethod method : methods) {
			JNode jNode = nodeCache.makeFrom(method);
			if (jNode != null) {
				implList.add(jNode);
			}
		}
	}

	@Override
	protected void loadFinished() {
		resultsTable.setEnabled(true);
		resultsModel.clear();

		if (implList != null) {
			Collections.sort(implList);
			resultsModel.addAll(implList);
		}
		updateHighlightContext(node.getName(), true, false, true);
		resultsTable.initColumnWidth();
		resultsTable.updateTable();
		if (resultsModel.getRowCount() > 0) {
			resultsTable.setRowSelectionInterval(0, 0);
			resultsTable.requestFocusInWindow();
		}
		updateProgressLabel(true);
	}

	@Override
	protected void loadStart() {
		resultsTable.setEnabled(false);
	}

	private void initUI() {
		JadxSettings settings = mainWindow.getSettings();
		Font codeFont = settings.getCodeFont();
		JLabel lbl = new JLabel(NLS.str("implementations_dialog.label"));
		lbl.setFont(codeFont);
		JLabel nodeLabel = NodeLabel.longName(node);
		nodeLabel.setFont(codeFont);
		lbl.setLabelFor(nodeLabel);

		JPanel searchPane = new JPanel();
		searchPane.setLayout(new FlowLayout(FlowLayout.LEFT));
		searchPane.add(lbl);
		searchPane.add(nodeLabel);
		searchPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		initCommon();
		JPanel resultsPanel = initResultsTable();
		JPanel buttonPane = initButtonsPanel();

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout(5, 5));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPanel.add(searchPane, BorderLayout.PAGE_START);
		contentPanel.add(resultsPanel, BorderLayout.CENTER);
		contentPanel.add(buttonPane, BorderLayout.PAGE_END);
		getContentPane().add(contentPanel);

		pack();
		setSize(800, 500);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}
}
