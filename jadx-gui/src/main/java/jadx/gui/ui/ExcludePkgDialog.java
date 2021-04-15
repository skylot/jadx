package jadx.gui.ui;

import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExcludePkgDialog extends JDialog {
	private static final long serialVersionUID = -1111111202104151030L;

	private final transient MainWindow mainWindow;
	private transient DefaultListModel<ListNode> listModel;
	private transient java.util.List<ListNode> packages = Collections.emptyList();

	public ExcludePkgDialog(MainWindow mainWindow) {
		super(mainWindow);
		this.mainWindow = mainWindow;
		initUI();
		UiUtils.addEscapeShortCutToDispose(this);
		initPackageList();
	}

	private void initUI() {
		JTextField pkgNameField = new JTextField();
		JPanel filterPanel = new JPanel(new BorderLayout(20, 5));
		filterPanel.add(new JLabel(NLS.str("exclude_dialog.pkg_name")), BorderLayout.WEST);
		filterPanel.add(pkgNameField, BorderLayout.CENTER);

		JList<ListNode> list = new JList<>();
		listModel = new DefaultListModel<>();
		list.setModel(listModel);
		list.setCellRenderer(new PkgListCellRenderer());
		JScrollPane listPanel = new JScrollPane(list);
		listPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				Point point = e.getPoint();
				int i = list.locationToIndex(point);
				if (i > -1) {
					if (i == listModel.getSize() - 1
							&& !list.getCellBounds(i, i).contains(point)) {
						return;
					}
					listModel.getElementAt(i).toggle();
					repaint();
				}
			}
		});

		JPanel actionPanel = new JPanel();
		BoxLayout boxLayout = new BoxLayout(actionPanel, BoxLayout.LINE_AXIS);
		actionPanel.setLayout(boxLayout);
		actionPanel.add(new Label(" "));
		JButton btnOk = new JButton(NLS.str("exclude_dialog.ok"));
		JButton btnAll = new JButton(NLS.str("exclude_dialog.select_all"));
		JButton btnInvert = new JButton(NLS.str("exclude_dialog.invert"));
		JButton btnDeselect = new JButton(NLS.str("exclude_dialog.deselect"));
		actionPanel.add(btnDeselect);
		actionPanel.add(btnInvert);
		actionPanel.add(btnAll);
		actionPanel.add(btnOk);

		JPanel mainPane = new JPanel(new BorderLayout(5, 5));
		mainPane.add(filterPanel, BorderLayout.NORTH);
		mainPane.add(listPanel, BorderLayout.CENTER);
		mainPane.add(actionPanel, BorderLayout.SOUTH);
		mainPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		getContentPane().add(mainPane);
		pack();
		setSize(600, 700);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.MODELESS);

		btnOk.addActionListener(e -> {
			mainWindow.getWrapper()
					.setExcludedPackages(packages.stream()
							.filter(ListNode::isSelected)
							.map(ListNode::getText)
							.collect(Collectors.toList()));
			mainWindow.reOpenFile();
			dispose();
		});

		btnAll.addActionListener(e -> {
			packages.forEach(p -> p.setSelected(true));
			list.repaint();
		});
		btnDeselect.addActionListener(e -> {
			packages.forEach(p -> p.setSelected(false));
			list.repaint();
		});
		btnInvert.addActionListener(e -> {
			packages.forEach(ListNode::toggle);
			list.repaint();
		});

		pkgNameField.getDocument().addDocumentListener(new DocumentListener() {
			private void update() {
				String text = pkgNameField.getText();
				if (text.isEmpty()) {
					packages.forEach(p->p.filter(false));
				} else {
					packages.forEach(p->p.filter(!p.getText().contains(text)));
				}
				listModel.clear();
				packages.forEach(p->{
					if(!p.isFiltered()){
						listModel.addElement(p);
					}
				});
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				update();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				update();
			}
		});
	}

	private void initPackageList() {
		List<String> excluded = mainWindow.getWrapper().getExcludedPackages();
		packages = mainWindow.getWrapper().getDecompiler().getPackages()
				.stream()
				.map(p -> new ListNode(p.getFullName(), excluded.contains(p.getFullName())))
				.collect(Collectors.toList());
		packages.forEach(listModel::addElement);
	}

	private static class ListNode {
		boolean filtered;
		JCheckBox checkbox;

		ListNode(String text, boolean select) {
			checkbox = new JCheckBox(text, select);
			checkbox.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					super.mousePressed(e);
					System.out.println(e.getPoint());
				}
			});
		}

		boolean toggle() {
			if (!isFiltered()) {
				boolean yes = !checkbox.isSelected();
				checkbox.setSelected(yes);
				return yes;
			}
			return false;
		}

		void setSelected(boolean yes) {
			if (!isFiltered()) {
				checkbox.setSelected(yes);
			}
		}

		boolean isSelected() {
			return checkbox.isSelected();
		}

		String getText() {
			return checkbox.getText();
		}

		void filter(boolean filtered) {
			this.filtered = filtered;
		}

		boolean isFiltered() {
			return this.filtered;
		}

		@Override
		public String toString() {
			return checkbox.getText();
		}
	}

	private static class PkgListCellRenderer implements ListCellRenderer<ListNode> {

		@Override
		public Component getListCellRendererComponent(JList<? extends ListNode> list, ListNode value,
													  int index, boolean isSelected, boolean cellHasFocus) {
			if (value.isSelected()) {
				value.checkbox.setBackground(list.getSelectionBackground());
				value.checkbox.setForeground(Color.white);
			} else {
				value.checkbox.setBackground(list.getBackground());
				value.checkbox.setForeground(Color.black);
			}
			return value.checkbox;
		}
	}

}
