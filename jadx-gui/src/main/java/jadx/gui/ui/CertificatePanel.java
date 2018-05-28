package jadx.gui.ui;

import jadx.gui.treemodel.JNode;

import javax.swing.*;
import java.awt.*;

public class CertificatePanel extends ContentPanel {
    CertificatePanel(TabbedPane panel, JNode jnode) {
        super(panel, jnode);
        setLayout(new BorderLayout());
        add(new JLabel("Hello"));
    }

    @Override
    public void loadSettings() {

    }
}
