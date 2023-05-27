package jadx.gui.settings.ui;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import say.swing.JFontChooser;

public class JPreferredFontChooser extends JFontChooser {
	private static final Logger LOG = LoggerFactory.getLogger(JPreferredFontChooser.class);

	private static final String[] PREFERRED_FONTS = new String[] {
			"Monospaced", "Consolas", "Courier", "Courier New",
			"Lucida Sans Typewriter", "Lucida Console",
			"SimSun", "SimHei",
	};

	private String[] filteredFonts;

	@Override
	protected String[] getFontFamilies() {
		if (filteredFonts == null) {
			GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
			Set<String> fontSet = new HashSet<>();
			Collections.addAll(fontSet, env.getAvailableFontFamilyNames());
			ArrayList<String> found = new ArrayList<>(PREFERRED_FONTS.length);
			for (String font : PREFERRED_FONTS) {
				if (fontSet.contains(font)) {
					found.add(font);
				}
			}
			if (found.size() == PREFERRED_FONTS.length) {
				filteredFonts = PREFERRED_FONTS;
			} else if (found.size() > 0) {
				filteredFonts = new String[found.size()];
				for (int i = 0; i < found.size(); i++) {
					filteredFonts[i] = found.get(i);
				}
			} else {
				LOG.warn("Can't found any preferred fonts for smali, use all available.");
				filteredFonts = env.getAvailableFontFamilyNames();
			}
		}
		return filteredFonts;
	}
}
