package jadx.gui.ui;

import jadx.gui.utils.NLS;
import jadx.gui.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class HeapUsageBar extends JProgressBar implements ActionListener {

	private static final double TWO_TO_20 = 1048576d; // 1024 * 1024

	private final Color GREEN = new Color(0, 180, 0);
	private final Color RED = new Color(200, 0, 0);

	private final Runtime r;

	private String maxHeapStr;

	private final Timer timer;

	private final double maxGB;

	private final String textFormat;

	public HeapUsageBar() {
		super();
		textFormat = NLS.str("heapUsage.text");
		r = Runtime.getRuntime();
		setBorderPainted(false);
		setStringPainted(true);
		setValue(10);
		int maxKB = (int) (r.maxMemory() / 1024);
		setMaximum(maxKB);
		maxGB = maxKB / TWO_TO_20;
		update();
		timer = new Timer(2000, this);
	}

	public void update() {
		long used = r.totalMemory() - r.freeMemory();
		int usedKB = (int) (used / 1024);
		setValue(usedKB);
		setString(String.format(textFormat, (usedKB / TWO_TO_20), maxGB));

		if ((used + Utils.MIN_FREE_MEMORY) > r.maxMemory()) {
			setForeground(RED);
		} else {
			setForeground(GREEN);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		update();
	}

	@Override
	public void setVisible(boolean aFlag) {
		super.setVisible(aFlag);
		if (aFlag) {
			timer.start();
		} else {
			timer.stop();
		}
	}
}
