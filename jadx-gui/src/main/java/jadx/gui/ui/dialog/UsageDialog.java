package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import jadx.api.ICodeInfo;
import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.utils.CodeUtils;
import jadx.core.dex.info.ConstStorage;
import jadx.core.dex.nodes.FieldNode;
import jadx.gui.JadxWrapper;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JField;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.NodeLabel;

public class UsageDialog extends CommonSearchDialog {
	private static final long serialVersionUID = -5105405789969134105L;

	private final transient JNode node;

	private transient List<CodeNode> usageList;

	public UsageDialog(MainWindow mainWindow, JNode node) {
		super(mainWindow, NLS.str("usage_dialog.title"));
		this.node = node;

		initUI();
		registerInitOnOpen();
		loadWindowPos();
	}

	@Override
	protected void openInit() {
		progressStartCommon();
		prepareUsageData();
		mainWindow.getBackgroundExecutor().execute(NLS.str("progress.load"),
				this::collectUsageData,
				(status) -> {
					if (status == TaskStatus.CANCEL_BY_MEMORY) {
						mainWindow.showHeapUsageBar();
						UiUtils.errorMessage(this, NLS.str("message.memoryLow"));
					}
					progressFinishedCommon();
					loadFinished();
				});
	}

	private void prepareUsageData() {
		if (mainWindow.getSettings().isReplaceConsts() && node instanceof JField) {
			FieldNode fld = ((JField) node).getJavaField().getFieldNode();
			boolean constField = ConstStorage.getFieldConstValue(fld) != null;
			if (constField && !fld.getAccessFlags().isPrivate()) {
				// run full decompilation to prepare for full code scan
				mainWindow.requestFullDecompilation();
			}
		}
	}

	private void collectUsageData() {
		usageList = new ArrayList<>();
		buildUsageQuery().forEach(
				(searchNode, useNodes) -> useNodes.stream()
						.map(JavaNode::getTopParentClass)
						.distinct()
						.forEach(u -> processUsage(searchNode, u)));
	}

	/**
	 * Return mapping of 'node to search' to 'use places'
	 */
	private Map<JavaNode, List<? extends JavaNode>> buildUsageQuery() {
		Map<JavaNode, List<? extends JavaNode>> map = new HashMap<>();
		if (node instanceof JMethod) {
			JavaMethod javaMethod = ((JMethod) node).getJavaMethod();
			for (JavaMethod mth : getMethodWithOverrides(javaMethod)) {
				map.put(mth, mth.getUseIn());
			}
			return map;
		}
		if (node instanceof JClass) {
			JavaClass javaCls = ((JClass) node).getCls();
			map.put(javaCls, javaCls.getUseIn());
			// add constructors usage into class usage
			for (JavaMethod javaMth : javaCls.getMethods()) {
				if (javaMth.isConstructor()) {
					map.put(javaMth, javaMth.getUseIn());
				}
			}
			return map;
		}
		if (node instanceof JField && mainWindow.getSettings().isReplaceConsts()) {
			FieldNode fld = ((JField) node).getJavaField().getFieldNode();
			boolean constField = ConstStorage.getFieldConstValue(fld) != null;
			if (constField && !fld.getAccessFlags().isPrivate()) {
				// search all classes to collect usage of replaced constants
				map.put(fld.getJavaNode(), mainWindow.getWrapper().getIncludedClasses());
				return map;
			}
		}
		JavaNode javaNode = node.getJavaNode();
		map.put(javaNode, javaNode.getUseIn());
		return map;
	}

	private List<JavaMethod> getMethodWithOverrides(JavaMethod javaMethod) {
		List<JavaMethod> relatedMethods = javaMethod.getOverrideRelatedMethods();
		if (!relatedMethods.isEmpty()) {
			return relatedMethods;
		}
		return Collections.singletonList(javaMethod);
	}

	private void processUsage(JavaNode searchNode, JavaClass topUseClass) {
		ICodeInfo codeInfo = topUseClass.getCodeInfo();
		List<Integer> usePositions = topUseClass.getUsePlacesFor(codeInfo, searchNode);
		if (usePositions.isEmpty()) {
			return;
		}
		String code = codeInfo.getCodeStr();
		JadxWrapper wrapper = mainWindow.getWrapper();
		for (int pos : usePositions) {
			String line = CodeUtils.getLineForPos(code, pos);
			if (line.startsWith("import ")) {
				continue;
			}
			JNodeCache nodeCache = getNodeCache();
			JavaNode enclosingNode = wrapper.getEnclosingNode(codeInfo, pos);
			JClass rootJCls = nodeCache.makeFrom(topUseClass);
			JNode usageJNode = enclosingNode == null ? rootJCls : nodeCache.makeFrom(enclosingNode);
			usageList.add(new CodeNode(rootJCls, usageJNode, line.trim(), pos));
		}
	}

	@Override
	protected void loadFinished() {
		resultsTable.setEnabled(true);
		resultsModel.clear();

		Collections.sort(usageList);
		resultsModel.addAll(usageList);
		updateHighlightContext(node.getName(), true, false);
		resultsTable.initColumnWidth();
		resultsTable.updateTable();
		updateProgressLabel(true);
	}

	@Override
	protected void loadStart() {
		resultsTable.setEnabled(false);
	}

	private void initUI() {
		JadxSettings settings = mainWindow.getSettings();
		Font codeFont = settings.getFont();
		JLabel lbl = new JLabel(NLS.str("usage_dialog.label"));
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
