package jadx.gui.ui;

import jadx.gui.treemodel.JNode;

import javax.swing.*;
import java.awt.*;

public class CertificatePanel extends ContentPanel {
    CertificatePanel(TabbedPane panel, JNode jnode) {
        super(panel, jnode);
        setLayout(new BorderLayout());
        JTextArea textArea = new JTextArea(jnode.getContent());
        textArea.setFont(textArea.getFont().deriveFont(12f)); // will only change size to 12pt
        JScrollPane sp = new JScrollPane(textArea);
        add(sp);
    }

    @Override
    public void loadSettings() {


    }
}
