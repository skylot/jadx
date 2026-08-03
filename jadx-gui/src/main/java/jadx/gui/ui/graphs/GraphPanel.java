package jadx.gui.ui.graphs;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.Renderer;

import jadx.core.utils.exceptions.JadxRuntimeException;

import static java.awt.RenderingHints.*;

public class GraphPanel extends JPanel {
	private static final Logger LOG = LoggerFactory.getLogger(GraphPanel.class);

	private final GraphDialog parentDialog;
	private final Dimension fullImageSize = new Dimension();
	private final double minimumScale = 0.1;
	private final double maximumScale = 10.0;
	private double scale = 1.0;
	private double translateX = 0;
	private double translateY = 0;
	private Point lastDragPoint = null;
	private @Nullable BufferedImage image;
	private Renderer renderer;
	private SVGDiagram svgDiagram;

	public GraphPanel(GraphDialog parentDialog) {
		this.parentDialog = parentDialog;
		MouseAdapter ma = new GraphPanelMouseAdapter();
		addMouseListener(ma);
		addMouseMotionListener(ma);
		addMouseWheelListener(ma);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (image != null) {
			Graphics2D g2d = (Graphics2D) g;
			AffineTransform transform = new AffineTransform();
			transform.translate(translateX * scale, translateY * scale);
			g2d.drawImage(image, transform, null);
		}
	}

	public void setGraph(String dotString) {
		try {
			init(dotString);
		} catch (Exception e) {
			LOG.error("Error parsing DOT string", e);
			invalidateImage(GraphDialog.graphError(e));
		}
	}

	public void exportSVG(File svgImage) {
		try {
			renderer.toFile(svgImage);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void init(String dotString) {
		image = null;
		byte[] bytes;
		try (ByteArrayOutputStream bout = new ByteArrayOutputStream()) {
			renderer = Graphviz.fromString(dotString)
					.width(getWidth())
					.postProcessor((result, options, processOptions) -> result.mapString(svg -> {
						// Graphviz <5.0.1 erroniously outputs 'stroke="transparent"' attributes in it's generated svg,
						// which is not valid SVG 1.1. To fix this, we parse the SVG as XML, use XPath to locate bad
						// attributes, correct them, and reserialize.
						// Unfortunately, the versions of Graphviz in both Ubuntu <26.04 and guru.nidi.graphviz are far too
						// old for this to be fixed upstream.
						try {
							// Parse the generated XML
							DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
							DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
							Document doc = docBuilder.parse(new ByteArrayInputStream(svg.getBytes()));

							// Find polygon nodes with attribute 'stroke="transparent"'
							XPath xpath = XPathFactory.newInstance().newXPath();
							NodeList nodes =
									(NodeList) xpath.compile("//polygon[@stroke=\"transparent\"]").evaluate(doc, XPathConstants.NODESET);

							// Rewrite the stroke attribute on found nodes
							for (int i = 0; i < nodes.getLength(); i++) {
								Node node = nodes.item(i);
								NamedNodeMap attrs = node.getAttributes();
								attrs.getNamedItem("stroke").setNodeValue("none");
							}

							// Reserialize the adjusted XML document
							TransformerFactory tff = TransformerFactory.newInstance();
							Transformer tf = tff.newTransformer();
							StringWriter writer = new StringWriter();
							tf.transform(new DOMSource(doc), new StreamResult(writer));
							return writer.toString();

						} catch (Exception e) {
							// If something goes wrong in this process, just give up and continue with the original SVG source.
							// This will cause large amounts of exceptions to be printed to the console but should otherwise
							// work fine.
							return svg;
						}

					}))
					.render(Format.SVG);
			renderer.toOutputStream(bout);
			bytes = bout.toByteArray();
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to render graph", e);
		}
		try (InputStream in = new ByteArrayInputStream(bytes)) {
			SVGUniverse universe = new SVGUniverse();
			URI uri = universe.loadSVG(in, "//graph/");
			svgDiagram = universe.getDiagram(uri);
			svgDiagram.setIgnoringClipHeuristic(true);
			fullImageSize.setSize(svgDiagram.getWidth(), svgDiagram.getHeight());
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to process SVG image", e);
		}
		parentDialog.enableMenu();
		removeAll();
		SwingUtilities.invokeLater(() -> {
			double width = getWidth();
			double height = getHeight();
			if (scale == 1.0) {
				// fit in window on dialog open (keep on graph changes)
				scale = Math.min(width / (double) fullImageSize.width, height / (double) fullImageSize.height);
			}
			renderGraphScaled();
			if (image == null) {
				return;
			}
			// center image in window
			translateX = (width / 2. - fullImageSize.width * scale / 2.) / scale;
			translateY = (height / 2. - fullImageSize.height * scale / 2.) / scale;
			repaint();
		});
	}

	private void renderGraphScaled() {
		try {
			if (fullImageSize.width * scale * fullImageSize.height * scale >= Integer.MAX_VALUE) {
				scale = maximumScale;
			}
			image = new BufferedImage(
					(int) (fullImageSize.width * scale),
					(int) (fullImageSize.height * scale),
					BufferedImage.TYPE_INT_ARGB);
			Graphics2D imgG2d = image.createGraphics();
			configGraphics(imgG2d);
			AffineTransform transform = new AffineTransform();
			transform.scale(scale, scale);
			imgG2d.setTransform(transform);
			svgDiagram.render(imgG2d);
		} catch (Exception e) {
			LOG.error("Graph render failed: ", e);
			invalidateImage(GraphDialog.graphError(e));
		}
	}

	private void configGraphics(Graphics2D graphics) {
		graphics.setRenderingHint(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY);
		graphics.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(KEY_COLOR_RENDERING, VALUE_COLOR_RENDER_QUALITY);
		graphics.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BICUBIC);
		graphics.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY);
		graphics.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_ON);
	}

	public void invalidateImage(JTextArea errorMsg) {
		removeAll();
		add(errorMsg);
		image = null;
		parentDialog.disableMenu();
		revalidate();
		repaint();
	}

	private class GraphPanelMouseAdapter extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {
			lastDragPoint = e.getPoint();
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			if (image != null) {
				Point p = e.getPoint();
				translateX += (p.x - lastDragPoint.x) / scale;
				translateY += (p.y - lastDragPoint.y) / scale;
				lastDragPoint = p;
				repaint();
			}
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if (image != null) {
				double prevScale = scale;
				scale *= Math.pow(1.1, -e.getWheelRotation());
				if (scale > maximumScale) {
					scale = maximumScale;
				}
				if (scale < minimumScale) {
					scale = minimumScale;
				}
				if (scale != prevScale) {
					Point p = e.getPoint();
					double px = (p.x - translateX * prevScale) / prevScale;
					double py = (p.y - translateY * prevScale) / prevScale;
					translateX = p.x / scale - px;
					translateY = p.y / scale - py;
					renderGraphScaled();
					if (image == null) {
						return;
					}
					repaint();
				}
			}
		}
	}
}
