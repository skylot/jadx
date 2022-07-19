package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import jadx.api.ICodeInfo;
import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.utils.CodeUtils;
import jadx.gui.JadxWrapper;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

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

	private void collectUsageData() {
		usageList = new ArrayList<>();
		Map<JavaNode, List<JavaNode>> usageQuery = buildUsageQuery();
		usageQuery.forEach((searchNode, useNodes) -> useNodes.stream()
				.map(JavaNode::getTopParentClass)
				.distinct()
				.forEach(u -> processUsage(searchNode, u)));
	}

	/**
	 * Return mapping of 'node to search' to 'use places'
	 */
	private Map<JavaNode, List<JavaNode>> buildUsageQuery() {
		Map<JavaNode, List<JavaNode>> map = new HashMap<>();
		if (node instanceof JMethod) {
			JavaMethod javaMethod = ((JMethod) node).getJavaMethod();
			for (JavaMethod mth : getMethodWithOverrides(javaMethod)) {
				map.put(mth, mth.getUseIn());
			}
		} else {
			JavaNode javaNode = node.getJavaNode();
			map.put(javaNode, javaNode.getUseIn());
		}
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
		String code = codeInfo.getCodeStr();
		JadxWrapper wrapper = mainWindow.getWrapper();
		List<Integer> usePositions = topUseClass.getUsePlacesFor(codeInfo, searchNode);
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
		// TODO: highlight only needed node usage
		setHighlightText(null);
		resultsTable.initColumnWidth();
		resultsTable.updateTable();
		updateProgressLabel(true);
	}

	@Override
	protected void loadStart() {
		resultsTable.setEnabled(false);
	}

	private void initUI() {
		JLabel lbl = new JLabel(NLS.str("usage_dialog.label"));
		JLabel nodeLabel = new JLabel(this.node.makeLongStringHtml(), this.node.getIcon(), SwingConstants.LEFT);
		lbl.setLabelFor(nodeLabel);

		JPanel searchPane = new JPanel();
		searchPane.setLayout(new FlowLayout(FlowLayout.LEFT));
		searchPane.add(lbl);
		searchPane.add(nodeLabel);
		searchPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		initCommon();
		JPanel resultsPanel = initResultsTable();
		JPanel buttonPane = initButtonsPanel();

		Container contentPane = getContentPane();
		contentPane.add(searchPane, BorderLayout.PAGE_START);
		contentPane.add(resultsPanel, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);

		pack();
		setSize(800, 500);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}
}
