package jadx.gui.ui;

import javax.swing.*;
import java.awt.*;

import jadx.gui.treemodel.JNode;
import jadx.gui.utils.CodeUsageInfo;
import jadx.gui.utils.NLS;

public class UsageDialog extends CommonSearchDialog {

	private static final long serialVersionUID = -5105405789969134105L;

	private final transient JNode node;

	public UsageDialog(MainWindow mainWindow, JNode node) {
		super(mainWindow);
		this.node = node;

		initUI();
		registerInitOnOpen();
		loadWindowPos();
	}

	@Override
	protected void openInit() {
		prepare();
	}

	@Override
	protected void loadFinished() {
		performSearch();
	}

	@Override
	protected void loadStart() {
		// no op
	}

	@Override
	protected synchronized void performSearch() {
		resultsModel.clear();

		CodeUsageInfo usageInfo = cache.getUsageInfo();
		if (usageInfo == null) {
			return;
		}
		resultsModel.addAll(usageInfo.getUsageList(node));
		// TODO: highlight only needed node usage
		highlightText = null;
		super.performSearch();
	}

	private void initUI() {
		JLabel lbl = new JLabel(NLS.str("usage_dialog.label"));
		JLabel nodeLabel = new JLabel(this.node.makeLongString(), this.node.getIcon(), SwingConstants.LEFT);
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

		setTitle(NLS.str("usage_dialog.title"));
		pack();
		setSize(800, 500);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.MODELESS);
	}
}
