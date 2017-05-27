package jadx.gui.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import jadx.api.JadxDecompiler;
import jadx.gui.utils.NLS;

class AboutDialog extends JDialog {
	private static final long serialVersionUID = 5763493590584039096L;

	public AboutDialog() {
		initUI();
	}

	public final void initUI() {
		Font font = new Font("Serif", Font.BOLD, 13);

		JLabel name = new JLabel("jadx");
		name.setFont(font);
		name.setAlignmentX(0.5f);

		JLabel desc = new JLabel("Dex to Java decompiler");
		desc.setFont(font);
		desc.setAlignmentX(0.5f);

		JLabel version = new JLabel("version: " + JadxDecompiler.getVersion());
		version.setFont(font);
		version.setAlignmentX(0.5f);

		JPanel textPane = new JPanel();
		textPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		textPane.setLayout(new BoxLayout(textPane, BoxLayout.PAGE_AXIS));
		textPane.add(Box.createRigidArea(new Dimension(0, 10)));
		textPane.add(name);
		textPane.add(Box.createRigidArea(new Dimension(0, 10)));
		textPane.add(desc);
		textPane.add(Box.createRigidArea(new Dimension(0, 10)));
		textPane.add(version);
		textPane.add(Box.createRigidArea(new Dimension(0, 20)));

		JButton close = new JButton(NLS.str("tabs.close"));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				dispose();
			}
		});
		close.setAlignmentX(0.5f);

		Container contentPane = getContentPane();
		contentPane.add(textPane, BorderLayout.CENTER);
		contentPane.add(close, BorderLayout.PAGE_END);

		setModalityType(ModalityType.APPLICATION_MODAL);

		setTitle("About JADX");
		pack();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);
	}
}
