package jadx.gui.utils.layout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * FlowLayout subclass that fully supports wrapping of components.
 */
public class WrapLayout extends FlowLayout {
	private static final long serialVersionUID = 6109752116520941346L;

	private Dimension preferredLayoutSize;

	/**
	 * Constructs a new <code>WrapLayout</code> with a left
	 * alignment and a default 5-unit horizontal and vertical gap.
	 */
	public WrapLayout() {
		super();
	}

	/**
	 * Constructs a new <code>FlowLayout</code> with the specified
	 * alignment and a default 5-unit horizontal and vertical gap.
	 * The value of the alignment argument must be one of
	 * <code>WrapLayout</code>, <code>WrapLayout</code>,
	 * or <code>WrapLayout</code>.
	 *
	 * @param align the alignment value
	 */
	public WrapLayout(int align) {
		super(align);
	}

	/**
	 * Creates a new flow layout manager with the indicated alignment
	 * and the indicated horizontal and vertical gaps.
	 * <p>
	 * The value of the alignment argument must be one of
	 * <code>WrapLayout</code>, <code>WrapLayout</code>,
	 * or <code>WrapLayout</code>.
	 *
	 * @param align the alignment value
	 * @param hgap  the horizontal gap between components
	 * @param vgap  the vertical gap between components
	 */
	public WrapLayout(int align, int hgap, int vgap) {
		super(align, hgap, vgap);
	}

	/**
	 * Returns the preferred dimensions for this layout given the
	 * <i>visible</i> components in the specified target container.
	 *
	 * @param target the component which needs to be laid out
	 * @return the preferred dimensions to lay out the
	 *         subcomponents of the specified container
	 */
	@Override
	public Dimension preferredLayoutSize(Container target) {
		return layoutSize(target, true);
	}

	/**
	 * Returns the minimum dimensions needed to layout the <i>visible</i>
	 * components contained in the specified target container.
	 *
	 * @param target the component which needs to be laid out
	 * @return the minimum dimensions to lay out the
	 *         subcomponents of the specified container
	 */
	@Override
	public Dimension minimumLayoutSize(Container target) {
		Dimension minimum = layoutSize(target, false);
		minimum.width -= (getHgap() + 1);
		return minimum;
	}

	/**
	 * Returns the minimum or preferred dimension needed to layout the target
	 * container.
	 *
	 * @param target    target to get layout size for
	 * @param preferred should preferred size be calculated
	 * @return the dimension to layout the target container
	 */
	private Dimension layoutSize(Container target, boolean preferred) {
		synchronized (target.getTreeLock()) {
			// Each row must fit with the width allocated to the containter.
			// When the container width = 0, the preferred width of the container
			// has not yet been calculated so lets ask for the maximum.

			int targetWidth = target.getSize().width;
			Container container = target;

			while (container.getSize().width == 0 && container.getParent() != null) {
				container = container.getParent();
			}

			targetWidth = container.getSize().width;

			if (targetWidth == 0) {
				targetWidth = Integer.MAX_VALUE;
			}

			int hgap = getHgap();
			int vgap = getVgap();
			Insets insets = target.getInsets();
			int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
			int maxWidth = targetWidth - horizontalInsetsAndGap;

			// Fit components into the allowed width

			Dimension dim = new Dimension(0, 0);
			int rowWidth = 0;
			int rowHeight = 0;

			int nmembers = target.getComponentCount();

			for (int i = 0; i < nmembers; i++) {
				Component m = target.getComponent(i);

				if (m.isVisible()) {
					Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
					int width = d.width;

					// Can't add the component to current row. Start a new row.
					if (rowWidth + width >= maxWidth) {
						addRow(dim, rowWidth, rowHeight);
						rowWidth = 0;
						rowHeight = 0;
					}

					// Add a horizontal gap for all components after the first

					if (rowWidth != 0) {
						rowWidth += hgap;
					}

					rowWidth += width;
					rowHeight = Math.max(rowHeight, d.height);
				}
			}

			addRow(dim, rowWidth, rowHeight);

			dim.width += horizontalInsetsAndGap;
			dim.height += insets.top + insets.bottom + vgap * 2;

			// When using a scroll pane or the DecoratedLookAndFeel we need to
			// make sure the preferred size is less than the size of the
			// target containter so shrinking the container size works
			// correctly. Removing the horizontal gap is an easy way to do this.

			Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);

			if (scrollPane != null && target.isValid()) {
				dim.width -= (hgap + 1);
			}

			return dim;
		}
	}

	/*
	 * A new row has been completed. Use the dimensions of this row
	 * to update the preferred size for the container.
	 * @param dim update the width and height when appropriate
	 * @param rowWidth the width of the row to add
	 * @param rowHeight the height of the row to add
	 */
	private void addRow(Dimension dim, int rowWidth, int rowHeight) {
		dim.width = Math.max(dim.width, rowWidth);

		if (dim.height > 0) {
			dim.height += getVgap();
		}

		dim.height += rowHeight;
	}
}
