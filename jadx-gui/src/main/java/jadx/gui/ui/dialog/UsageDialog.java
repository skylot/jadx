package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import jadx.api.CodePosition;
import jadx.api.ICodeWriter;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.treemodel.CodeNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.CodeLinesInfo;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.search.StringRef;

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
		mainWindow.getBackgroundExecutor().execute(NLS.str("progress.load"),
				this::collectUsageData,
				(status) -> {
					if (status == TaskStatus.CANCEL_BY_MEMORY) {
						mainWindow.showHeapUsageBar();
						UiUtils.errorMessage(this, NLS.str("message.memoryLow"));
					}
					loadFinishedCommon();
					loadFinished();
				});
	}

	private void collectUsageData() {
		usageList = new ArrayList<>();
		node.getJavaNode().getUseIn()
				.stream()
				.map(JavaNode::getTopParentClass)
				.distinct()
				.sorted(Comparator.comparing(JavaClass::getFullName))
				.forEach(this::processUsageClass);
	}

	private void processUsageClass(JavaClass cls) {
		String code = cls.getCodeInfo().getCodeStr();
		CodeLinesInfo linesInfo = new CodeLinesInfo(cls);
		JavaNode javaNode = node.getJavaNode();
		List<CodePosition> usage = cls.getUsageFor(javaNode);
		for (CodePosition pos : usage) {
			if (javaNode.getTopParentClass().equals(cls) && pos.getPos() == javaNode.getDefPos()) {
				// skip declaration
				continue;
			}
			StringRef line = getLineStrAt(code, pos.getPos());
			if (line.startsWith("import ")) {
				continue;
			}
			JavaNode javaNodeByLine = linesInfo.getJavaNodeByLine(pos.getLine());
			JNode useAtNode = javaNodeByLine == null ? node : getNodeCache().makeFrom(javaNodeByLine);
			usageList.add(new CodeNode(useAtNode, line, pos.getLine(), pos.getPos()));
		}
	}

	private StringRef getLineStrAt(String code, int pos) {
		String newLine = ICodeWriter.NL;
		int start = code.lastIndexOf(newLine, pos);
		int end = code.indexOf(newLine, pos);
		if (start == -1 || end == -1) {
			return StringRef.fromStr("line not found");
		}
		return StringRef.subString(code, start + newLine.length(), end).trim();
	}

	@Override
	protected void loadFinished() {
		resultsTable.setEnabled(true);
		performSearch();
	}

	@Override
	protected void loadStart() {
		resultsTable.setEnabled(false);
	}

	@Override
	protected synchronized void performSearch() {
		resultsModel.clear();
		Collections.sort(usageList);
		resultsModel.addAll(usageList);
		// TODO: highlight only needed node usage
		setHighlightText(null);
		super.performSearch();
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
