package jadx.gui.ui;

import hu.kazocsaba.imageviewer.ImageViewer;
import jadx.api.ResourceFile;
import jadx.gui.treemodel.JResource;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;

public class ImagePanel extends ContentPanel {

	ImagePanel(TabbedPane panel, JResource res) {
		super(panel, res);

		ResourceFile resFile = res.getResFile();
		BufferedImage img = resFile.loadContent().getImage();
		ImageViewer imageViewer = new ImageViewer(img);
		imageViewer.setZoomFactor(2.);

		setLayout(new BorderLayout());
		add(imageViewer.getComponent());
	}

	@Override
	public void loadSettings() {
	}
}
