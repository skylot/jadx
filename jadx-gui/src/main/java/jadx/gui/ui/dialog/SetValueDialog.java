package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.panel.JDebuggerPanel.ValueTreeNode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.UiUtils;

public class SetValueDialog extends JDialog {
	private static final long serialVersionUID = -1111111202103121002L;

	private final transient MainWindow mainWindow;
	private final transient ValueTreeNode valNode;

	public SetValueDialog(MainWindow mainWindow, ValueTreeNode valNode) {
		super(mainWindow);
		this.mainWindow = mainWindow;
		this.valNode = valNode;
		initUI();
		UiUtils.addEscapeShortCutToDispose(this);
		setTitle(valNode.toString());
	}

	private void initUI() {
		JTextField valField = new JTextField();
		TextStandardActions.attach(valField);
		JPanel valPane = new JPanel(new BorderLayout(5, 5));
		valPane.add(new JLabel(NLS.str("set_value_dialog.label_value")), BorderLayout.WEST);
		valPane.add(valField, BorderLayout.CENTER);

		JPanel btnPane = new JPanel();
		btnPane.setLayout(new BoxLayout(btnPane, BoxLayout.LINE_AXIS));
		JButton setValueBtn = new JButton(NLS.str("set_value_dialog.btn_set"));
		btnPane.add(new Label());
		btnPane.add(setValueBtn);

		UiUtils.addKeyBinding(valField, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "set value",
				new AbstractAction() {
					@Override
					public void actionPerformed(ActionEvent e) {
						setValueBtn.doClick();
					}
				});

		JPanel typePane = new JPanel();
		typePane.setLayout(new BoxLayout(typePane, BoxLayout.LINE_AXIS));
		java.util.List<JRadioButton> rbs = new ArrayList<>(6);
		rbs.add(new JRadioButton("int"));
		rbs.add(new JRadioButton("String"));
		rbs.add(new JRadioButton("long"));
		rbs.add(new JRadioButton("float"));
		rbs.add(new JRadioButton("double"));
		rbs.add(new JRadioButton("Object id"));
		rbs.get(0).setSelected(true); // select int radio

		ButtonGroup rbGroup = new ButtonGroup();
		rbs.forEach(rbGroup::add);
		rbs.forEach(typePane::add);

		JPanel mainPane = new JPanel(new BorderLayout(5, 5));
		mainPane.add(typePane, BorderLayout.NORTH);
		mainPane.add(valPane, BorderLayout.CENTER);
		mainPane.add(btnPane, BorderLayout.SOUTH);
		mainPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		getContentPane().add(mainPane);

		this.setTitle(NLS.str("set_value_dialog.title"));

		pack();
		setSize(480, 160);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.MODELESS);
		UiUtils.addEscapeShortCutToDispose(this);

		setValueBtn.addActionListener(new AbstractAction() {
			private static final long serialVersionUID = -1111111202103260220L;

			@Override
			public void actionPerformed(ActionEvent e) {
				boolean ok;
				try {
					Entry<ArgType, Object> type = getType();
					if (type != null) {
						ok = mainWindow
								.getDebuggerPanel()
								.getDbgController()
								.modifyRegValue(valNode, type.getKey(), type.getValue());
					} else {
						UiUtils.showMessageBox(mainWindow, NLS.str("set_value_dialog.sel_type"));
						return;
					}
				} catch (JadxRuntimeException except) {
					UiUtils.showMessageBox(mainWindow, except.getMessage());
					return;
				}
				if (ok) {
					dispose();
				} else {
					UiUtils.showMessageBox(mainWindow, NLS.str("set_value_dialog.neg_msg"));
				}
			}

			private Entry<ArgType, Object> getType() {
				String val = valField.getText();
				for (JRadioButton rb : rbs) {
					if (rb.isSelected()) {
						switch (rb.getText()) {
							case "int":
								return new SimpleEntry<>(ArgType.INT, Integer.valueOf(val));
							case "String":
								return new SimpleEntry<>(ArgType.STRING, val);
							case "long":
								return new SimpleEntry<>(ArgType.LONG, Long.valueOf(val));
							case "float":
								return new SimpleEntry<>(ArgType.FLOAT, Float.valueOf(val));
							case "double":
								return new SimpleEntry<>(ArgType.DOUBLE, Double.valueOf(val));
							case "Object id":
								return new SimpleEntry<>(ArgType.OBJECT, Long.valueOf(val));
							default:
								throw new JadxRuntimeException("Unexpected type: " + rb.getText());
						}
					}
				}
				return null;
			}
		});
	}
}
