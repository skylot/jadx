package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;

import jadx.api.JavaMethod;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.cellrenders.MethodsListRenderer;
import jadx.gui.utils.NLS;

public class MethodsDialog extends CommonDialog {
	private JList<JavaMethod> methodList;

	private final Consumer<List<JavaMethod>> listConsumer;

	public MethodsDialog(MainWindow mainWindow, List<JavaMethod> methods, Consumer<List<JavaMethod>> listConsumer) {
		super(mainWindow);
		this.listConsumer = listConsumer;
		initUI(methods);
		setVisible(true);
	}

	private void initUI(List<JavaMethod> methods) {
		setTitle(NLS.str("methods_dialog.title"));

		DefaultListModel<JavaMethod> defaultListModel = new DefaultListModel<>();
		defaultListModel.addAll(methods);

		methodList = new JList<>();
		methodList.setModel(defaultListModel);
		methodList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		methodList.setCellRenderer(new MethodsListRenderer());
		methodList.setSelectionModel(new DefaultListSelectionModel() {
			@Override
			public void setSelectionInterval(int index0, int index1) {
				if (super.isSelectedIndex(index0)) {
					super.removeSelectionInterval(index0, index1);
				} else {
					super.addSelectionInterval(index0, index1);
				}
			}

		});

		JScrollPane scrollPane = new JScrollPane(methodList);
		JPanel buttonPane = initButtonsPanel();

		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BorderLayout(5, 5));
		contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPanel.add(scrollPane, BorderLayout.CENTER);
		contentPanel.add(buttonPane, BorderLayout.PAGE_END);
		getContentPane().add(contentPanel);

		pack();
		setSize(500, 300);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	}

	protected JPanel initButtonsPanel() {
		JButton cancelButton = new JButton(NLS.str("common_dialog.cancel"));
		cancelButton.addActionListener(event -> dispose());

		JButton okBtn = new JButton(NLS.str("common_dialog.ok"));
		okBtn.addActionListener(event -> generateForSelected());
		getRootPane().setDefaultButton(okBtn);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(okBtn);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelButton);
		return buttonPane;
	}

	private void generateForSelected() {
		List<JavaMethod> selectedMethods = methodList.getSelectedValuesList();
		if (!selectedMethods.isEmpty()) {
			this.listConsumer.accept(selectedMethods);
		}
		dispose();
	}

}
