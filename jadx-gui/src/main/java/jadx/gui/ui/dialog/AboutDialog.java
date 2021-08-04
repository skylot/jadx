package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jadx.api.JadxDecompiler;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class AboutDialog extends JDialog {
	private static final long serialVersionUID = 5763493590584039096L;

	public AboutDialog() {
		initUI();
	}

	public final void initUI() {
		Font font = new Font("Serif", Font.BOLD, 13);

		URL logoURL = getClass().getResource("/logos/jadx-logo-48px.png");
		Icon logo = new ImageIcon(logoURL, "jadx logo");

		JLabel name = new JLabel("jadx", logo, SwingConstants.CENTER);
		name.setFont(font);
		name.setAlignmentX(0.5f);

		JLabel desc = new JLabel("Dex to Java decompiler");
		desc.setFont(font);
		desc.setAlignmentX(0.5f);

		JLabel version = new JLabel("jadx version: " + JadxDecompiler.getVersion());
		version.setFont(font);
		version.setAlignmentX(0.5f);

		String javaVm = System.getProperty("java.vm.name");
		String javaVer = System.getProperty("java.version");

		javaVm = javaVm == null ? "" : javaVm;

		JLabel javaVmLabel = new JLabel("Java VM: " + javaVm);
		javaVmLabel.setFont(font);
		javaVmLabel.setAlignmentX(0.5f);

		javaVer = javaVer == null ? "" : javaVer;
		JLabel javaVerLabel = new JLabel("Java version: " + javaVer);
		javaVerLabel.setFont(font);
		javaVerLabel.setAlignmentX(0.5f);

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
		textPane.add(javaVmLabel);
		textPane.add(javaVerLabel);
		textPane.add(Box.createRigidArea(new Dimension(0, 20)));

		JButton close = new JButton(NLS.str("tabs.close"));
		close.addActionListener(event -> dispose());
		close.setAlignmentX(0.5f);

		Container contentPane = getContentPane();
		contentPane.add(textPane, BorderLayout.CENTER);
		contentPane.add(close, BorderLayout.PAGE_END);

		UiUtils.setWindowIcons(this);

		setModalityType(ModalityType.APPLICATION_MODAL);

		setTitle(NLS.str("about_dialog.title"));
		pack();
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);
	}
}
