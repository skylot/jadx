package jadx.gui.ui.panel;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import hu.kazocsaba.imageviewer.ImageViewer;

import jadx.api.ICodeWriter;
import jadx.api.ResourceFile;
import jadx.api.ResourcesLoader;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.xmlgen.ResContainer;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.codearea.AbstractCodeArea;

public class ImagePanel extends ContentPanel {
	private static final long serialVersionUID = 4071356367073142688L;

	public ImagePanel(TabbedPane panel, JResource res) {
		super(panel, res);
		setLayout(new BorderLayout());
		try {
			BufferedImage img = loadImage(res);
			ImageViewer imageViewer = new ImageViewer(img);
			add(imageViewer.getComponent());
		} catch (Exception e) {
			RSyntaxTextArea textArea = AbstractCodeArea.getDefaultArea(panel.getMainWindow());
			textArea.setText("Image load error:" + ICodeWriter.NL + Utils.getStackTrace(e));
			add(textArea);
		}
	}

	private BufferedImage loadImage(JResource res) {
		ResourceFile resFile = res.getResFile();
		ResContainer resContainer = resFile.loadContent();
		ResContainer.DataType dataType = resContainer.getDataType();
		if (dataType == ResContainer.DataType.DECODED_DATA) {
			try {
				return ImageIO.read(new ByteArrayInputStream(resContainer.getDecodedData()));
			} catch (Exception e) {
				throw new JadxRuntimeException("Failed to load image", e);
			}
		} else if (dataType == ResContainer.DataType.RES_LINK) {
			try {
				return ResourcesLoader.decodeStream(resFile, (size, is) -> ImageIO.read(is));
			} catch (Exception e) {
				throw new JadxRuntimeException("Failed to load image", e);
			}
		} else {
			throw new JadxRuntimeException("Unsupported resource image data type: " + resFile);
		}
	}

	@Override
	public void loadSettings() {
		// no op
	}
}
