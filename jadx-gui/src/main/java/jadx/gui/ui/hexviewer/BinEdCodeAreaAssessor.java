package jadx.gui.ui.hexviewer;

/*
 * Copyright (C) ExBin Project
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.exbin.bined.CodeAreaSection;
import org.exbin.bined.highlight.swing.NonAsciiCodeAreaColorAssessor;
import org.exbin.bined.highlight.swing.NonprintablesCodeAreaAssessor;
import org.exbin.bined.highlight.swing.SearchCodeAreaColorAssessor;
import org.exbin.bined.swing.CodeAreaCharAssessor;
import org.exbin.bined.swing.CodeAreaColorAssessor;
import org.exbin.bined.swing.CodeAreaPaintState;

/**
 * Color assessor for binary editor with registrable modifiers.
 *
 * @author ExBin Project (https://exbin.org)
 */
public class BinEdCodeAreaAssessor implements CodeAreaColorAssessor, CodeAreaCharAssessor {

	private final List<PositionColorModifier> priorityColorModifiers = new ArrayList<>();
	private final List<PositionColorModifier> colorModifiers = new ArrayList<>();

	private final CodeAreaColorAssessor parentColorAssessor;
	private final CodeAreaCharAssessor parentCharAssessor;

	public BinEdCodeAreaAssessor(CodeAreaColorAssessor parentColorAssessor, CodeAreaCharAssessor parentCharAssessor) {
		NonAsciiCodeAreaColorAssessor nonAsciiCodeAreaColorAssessor = new NonAsciiCodeAreaColorAssessor(parentColorAssessor);
		NonprintablesCodeAreaAssessor nonprintablesCodeAreaAssessor =
				new NonprintablesCodeAreaAssessor(nonAsciiCodeAreaColorAssessor, parentCharAssessor);
		SearchCodeAreaColorAssessor searchCodeAreaColorAssessor = new SearchCodeAreaColorAssessor(nonprintablesCodeAreaAssessor);
		this.parentColorAssessor = searchCodeAreaColorAssessor;
		this.parentCharAssessor = nonprintablesCodeAreaAssessor;
	}

	public void addColorModifier(PositionColorModifier colorModifier) {
		colorModifiers.add(colorModifier);
	}

	public void removeColorModifier(PositionColorModifier colorModifier) {
		colorModifiers.remove(colorModifier);
	}

	public void addPriorityColorModifier(PositionColorModifier colorModifier) {
		priorityColorModifiers.add(colorModifier);
	}

	public void removePriorityColorModifier(PositionColorModifier colorModifier) {
		priorityColorModifiers.remove(colorModifier);
	}

	@Override
	public void startPaint(CodeAreaPaintState codeAreaPaintState) {
		for (PositionColorModifier colorModifier : priorityColorModifiers) {
			colorModifier.resetColors();
		}

		for (PositionColorModifier colorModifier : colorModifiers) {
			colorModifier.resetColors();
		}

		if (parentColorAssessor != null) {
			parentColorAssessor.startPaint(codeAreaPaintState);
		}
	}

	@Override
	public Color getPositionBackgroundColor(long rowDataPosition, int byteOnRow, int charOnRow, CodeAreaSection section,
			boolean inSelection) {
		for (PositionColorModifier colorModifier : priorityColorModifiers) {
			Color positionBackgroundColor =
					colorModifier.getPositionBackgroundColor(rowDataPosition, byteOnRow, charOnRow, section, inSelection);
			if (positionBackgroundColor != null) {
				return positionBackgroundColor;
			}
		}

		if (!inSelection) {
			for (PositionColorModifier colorModifier : colorModifiers) {
				Color positionBackgroundColor =
						colorModifier.getPositionBackgroundColor(rowDataPosition, byteOnRow, charOnRow, section, inSelection);
				if (positionBackgroundColor != null) {
					return positionBackgroundColor;
				}
			}
		}

		if (parentColorAssessor != null) {
			return parentColorAssessor.getPositionBackgroundColor(rowDataPosition, byteOnRow, charOnRow, section, inSelection);
		}

		return null;
	}

	@Override
	public Color getPositionTextColor(long rowDataPosition, int byteOnRow, int charOnRow, CodeAreaSection section, boolean inSelection) {
		for (PositionColorModifier colorModifier : priorityColorModifiers) {
			Color positionTextColor = colorModifier.getPositionTextColor(rowDataPosition, byteOnRow, charOnRow, section, inSelection);
			if (positionTextColor != null) {
				return positionTextColor;
			}
		}

		if (!inSelection) {
			for (PositionColorModifier colorModifier : colorModifiers) {
				Color positionTextColor = colorModifier.getPositionTextColor(rowDataPosition, byteOnRow, charOnRow, section, inSelection);
				if (positionTextColor != null) {
					return positionTextColor;
				}
			}
		}

		if (parentColorAssessor != null) {
			return parentColorAssessor.getPositionTextColor(rowDataPosition, byteOnRow, charOnRow, section, inSelection);
		}

		return null;
	}

	@Override
	public char getPreviewCharacter(long rowDataPosition, int byteOnRow, int charOnRow, CodeAreaSection section) {
		return parentCharAssessor != null ? parentCharAssessor.getPreviewCharacter(rowDataPosition, byteOnRow, charOnRow, section) : ' ';
	}

	@Override
	public char getPreviewCursorCharacter(long rowDataPosition, int byteOnRow, int charOnRow, byte[] cursorData, int cursorDataLength,
			CodeAreaSection section) {
		return parentCharAssessor != null
				? parentCharAssessor.getPreviewCursorCharacter(rowDataPosition, byteOnRow, charOnRow, cursorData, cursorDataLength, section)
				: ' ';
	}

	@Override
	public Optional<CodeAreaCharAssessor> getParentCharAssessor() {
		return Optional.ofNullable(parentCharAssessor);
	}

	@Override
	public Optional<CodeAreaColorAssessor> getParentColorAssessor() {
		return Optional.ofNullable(parentColorAssessor);
	}

	public interface PositionColorModifier {

		Color getPositionBackgroundColor(long rowDataPosition, int byteOnRow, int charOnRow, CodeAreaSection section, boolean inSelection);

		Color getPositionTextColor(long rowDataPosition, int byteOnRow, int charOnRow, CodeAreaSection section, boolean inSelection);

		void resetColors();
	}
}
