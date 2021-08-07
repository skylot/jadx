package jadx.gui.ui;

import java.awt.*;

import javax.swing.*;

class GhostGlassPane extends JComponent {
	private DnDTabbedPane tabbedPane;

	protected GhostGlassPane(DnDTabbedPane tabbedPane) {
		super();
		this.tabbedPane = tabbedPane;
		setOpaque(false);
	}

	public void setTargetTabbedPane(DnDTabbedPane tab) {
		tabbedPane = tab;
	}

	@Override
	protected void paintComponent(Graphics g) {
		tabbedPane.getDropLineRect().ifPresent(rect -> {
			Graphics2D g2 = (Graphics2D) g.create();
			Rectangle r = SwingUtilities.convertRectangle(tabbedPane, rect, this);
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f));
			g2.setPaint(Color.RED);
			g2.fill(r);
			g2.dispose();
		});
	}
}
