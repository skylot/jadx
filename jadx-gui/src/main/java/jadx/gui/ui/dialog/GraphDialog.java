package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.layout.WrapLayout;

public abstract class GraphDialog extends JFrame {

	private static final long serialVersionUID = 5840390965763493590L;

	private static final Logger LOG = LoggerFactory.getLogger(GraphDialog.class);

	private final MainWindow mainWindow;
	private GraphPanel panel;

	private static final Dimension MIN_WINDOW_SIZE = new Dimension(800, 500);

	private JMenuBar menuBar = null;

	public static JTextArea graphError() {
		return graphError(NLS.str("graph_viewer.default_error"));
	}

	public static JTextArea graphError(String errorMessage) {
		JTextArea errorText = new JTextArea();
		errorText.setText(errorMessage);
		errorText.setVisible(true);
		errorText.setEditable(false);
		errorText.setLineWrap(false);
		return errorText;
	}

	public static JTextArea graphError(Exception error) {
		JTextArea errorText = new JTextArea();
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		stringWriter.write(NLS.str("graph_viewer.default_error"));
		stringWriter.write(": ");
		error.printStackTrace(printWriter);
		errorText.setText(stringWriter.toString());
		errorText.setVisible(true);
		errorText.setEditable(false);
		errorText.setLineWrap(false);
		return errorText;
	}

	public GraphDialog(MainWindow mainWindow) {
		this(mainWindow, NLS.str("graph_viewer.default_title"));
	}

	public JMenuBar addMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		menuBar.setLayout(new WrapLayout(FlowLayout.LEFT));
		add(menuBar, BorderLayout.PAGE_START);
		this.menuBar = menuBar;

		JFileChooser fileChooser = new JFileChooser();

		JButton saveButton = new JButton(NLS.str("graph_viewer.save_graph"));
		saveButton.setEnabled(false);
		saveButton.addActionListener(e -> {
			try {
				int option = fileChooser.showSaveDialog(this);

				if (option == JFileChooser.APPROVE_OPTION) {
					File file = fileChooser.getSelectedFile();
					getPanel().renderer.render(Format.SVG).toFile(file);

				}
			} catch (Exception ex) {
				LOG.error("Failed to save file: ", ex);
				JOptionPane.showMessageDialog(this, NLS.str("graph_viewer.file_failure"),
						NLS.str("graph_viewer.file_failure"),
						JOptionPane.INFORMATION_MESSAGE);
			}
		});

		// Assemble menubar panel
		JPanel menuBarPanel = new JPanel();
		menuBarPanel.setOpaque(false);
		menuBarPanel.add(saveButton);

		// Add menubar panel to menuBar
		menuBar.add(menuBarPanel);

		return menuBar;
	}

	private void enableMenu() {
		JMenuBar menu = this.menuBar;
		setAllEnabled(true, menu);
	}

	private void disableMenu() {
		JMenuBar menu = this.menuBar;
		setAllEnabled(false, menu);
	}

	private void setAllEnabled(boolean isEnabled, JComponent component) {
		component.setEnabled(isEnabled);

		Component[] components = component.getComponents();
		for (Component subComponent : components) {
			if (subComponent instanceof JComponent) {
				setAllEnabled(isEnabled, (JComponent) subComponent);
			} else {
				subComponent.setEnabled(isEnabled);
			}
		}
	}

	public GraphDialog(MainWindow mainWindow, String title) {
		super(title);
		this.mainWindow = mainWindow;

		setMinimumSize(MIN_WINDOW_SIZE);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		UiUtils.addEscapeShortCutToDispose(this);
		setLocationRelativeTo(null);

		loadWindowPos();

		LOG.debug("Dialog w: {} h: {}", getWidth(), getHeight());

		LOG.debug("cwd: {}", System.getProperty("user.dir"));

		panel = new GraphPanel(this);
		panel.setFocusable(true);
		panel.addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e) {
				requestFocusInWindow();
			}

			public void mouseEntered(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseReleased(MouseEvent e) {
			}
		});

		setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);

	}

	public void loadWindowPos() {
		if (!mainWindow.getSettings().loadWindowPos(this)) {
			setPreferredSize(MIN_WINDOW_SIZE);
		}
	}

	@Override
	public void dispose() {
		try {
			mainWindow.getSettings().saveWindowPos(this);
		} catch (Exception e) {
			LOG.warn("Failed to save window size and position", e);
		}
		super.dispose();
	}

	class GraphPanel extends JPanel {

		private Dimension fullImageSize = new Dimension();
		private double scale = 1.0;
		private double minimumScale = 0.01;
		private double maximumScale = 7.0;
		private double translateX = 0;
		private double translateY = 0;
		private Point lastDragPoint = null;

		private BufferedImage image;

		private Graphviz renderer;

		private final GraphDialog parentDialog;

		public GraphPanel(GraphDialog parentDialog) {

			this.parentDialog = parentDialog;

			MouseAdapter ma = new MouseAdapter() {
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
							translateX = (p.x / scale) - px;
							translateY = (p.y / scale) - py;
							LOG.debug("Rescaling {}%", scale * 100);
							renderGraphScaled();
							if (image == null) {
								return;
							}
							repaint();

						}

					}
				}

			};

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

		public void setGraph(File dotString) {
			try {
				LOG.debug("Parsing DOT file: {} ", dotString.getAbsolutePath());
				setGraph(new Parser().read(dotString));
			} catch (Exception e) {
				LOG.error("Error parsing DOT file", e);
				invalidateImage(graphError(e));
			}
		}

		public void setGraph(String dotString) {
			try {
				setGraph(new Parser().read(dotString));
			} catch (Exception e) {
				LOG.error("Error parsing DOT string", e);
				invalidateImage(graphError(e));
			}
		}

		public void setGraph(MutableGraph g) {

			renderer = Graphviz.fromGraph(g);
			parentDialog.enableMenu();

			scale = 1.0;

			// set initial image scale and posiition
			Runnable doCenter = new Runnable() {
				public void run() {

					renderGraphFullSize();
					if (image == null) {
						return;
					}

					LOG.debug("full image w {} h {}", fullImageSize.width, fullImageSize.height);

					// scale required to fit image to window width or height
					double heightScale = (double) getHeight() / (double) fullImageSize.height;
					double widthScale = (double) getWidth() / (double) fullImageSize.width;
					if (widthScale < heightScale) {
						scale = widthScale;
						LOG.debug("scaling to fit width {}/{} {}", getWidth(), fullImageSize.width, scale);

					} else {
						scale = heightScale;
						LOG.debug("scaling to fit height {}/{} {}", getHeight(), fullImageSize.height, scale);
					}

					scale = scale * 0.95;
					maximumScale = Math.sqrt(Integer.MAX_VALUE / (fullImageSize.width * fullImageSize.height)) / 8;
					minimumScale = Math.min(scale, maximumScale);

					renderGraphScaled();
					if (image == null) {
						return;
					}

					// center image in window
					translateY = (getHeight() / 2 - (fullImageSize.height * scale) / 2) / scale;
					translateX = (getWidth() / 2 - (fullImageSize.width * scale) / 2) / scale;

					repaint();
				}
			};

			SwingUtilities.invokeLater(doCenter);

		}

		private void renderGraphFullSize() {
			try {
				image = null;
				image = renderer.render(Format.SVG).toImage();
				if (image.getWidth() == 0 || image.getHeight() == 0) {
					// If rendered image is too small, calculating the scale would later cause a
					// division by zero
					LOG.error("Graph render failed, image too small");
					invalidateImage(graphError(NLS.str("graph_viewer.image_too_small")));
					return;
				}

				fullImageSize.setSize(image.getWidth(), image.getHeight());

			} catch (IllegalArgumentException illegalArgumentException) {
				// If rendered image is too large, a Dimension object is passed invalid arguments
				LOG.error("Graph render failed, illegal arguments: ", illegalArgumentException);
				invalidateImage(graphError(NLS.str("graph_viewer.image_too_large")));

			} catch (Exception e) {
				// A large image may cause a number of other other exception types caught here along with other
				// failure cases
				LOG.error("Graph render failed: ", e);
				invalidateImage(graphError(e));
			}

		}

		private void renderGraphScaled() {
			try {
				if (fullImageSize.width * scale * fullImageSize.height * scale >= Integer.MAX_VALUE) {
					scale = maximumScale;
				}
				image = renderer.width((int) (fullImageSize.width * scale)).render(Format.SVG).toImage();
			} catch (Exception e) {
				LOG.error("Graph render failed: ", e);
				invalidateImage(graphError(e));
			}

		}

		public void invalidateImage(JTextArea errorMsg) {
			this.add(errorMsg);
			image = null;
			this.parentDialog.disableMenu();
			this.revalidate();
			repaint();
		}

	}

	protected GraphPanel getPanel() {
		return this.panel;
	}
}
