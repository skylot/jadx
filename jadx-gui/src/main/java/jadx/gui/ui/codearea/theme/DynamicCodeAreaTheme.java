package jadx.gui.ui.codearea.theme;

import java.awt.Color;

import javax.swing.UIManager;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxUtilities;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.Gutter;

import jadx.gui.utils.NLS;

/**
 * Mix current UI theme colors and apply to code area theme.
 */
public class DynamicCodeAreaTheme implements IEditorTheme {

	@Override
	public String getId() {
		return "DynamicCodeAreaTheme";
	}

	@Override
	public String getName() {
		return NLS.str("preferences.dynamic_editor_theme");
	}

	public void apply(RSyntaxTextArea textArea) {
		// Get the current colors from UIManager
		Color themeBackground = UIManager.getColor("Panel.background");
		Color themeForeground = UIManager.getColor("Panel.foreground");
		Color separatorForeground = UIManager.getColor("Separator.foreground");
		Color editorSelectionBackground = UIManager.getColor("EditorPane.selectionBackground");
		Color caretForeground = UIManager.getColor("EditorPane.caretForeground");

		SyntaxScheme scheme = textArea.getSyntaxScheme();

		boolean isDarkTheme = isDarkTheme(themeBackground);

		// Background colors based on the theme
		Color editorBackground = isDarkTheme ? themeBackground : Color.WHITE; // Use white for light theme
		Color lineHighlight = isDarkTheme
				? adjustBrightness(themeBackground, 1.2f)
				: Color.decode("#EBECF0"); // Light gray for light theme
		Color lineNumberForeground = UIManager.getColor("Label.foreground");

		// Add these lines after setting the background colors
		Color selectionColor = isDarkTheme
				? new Color(51, 153, 255, 90) // Semi-transparent blue for dark theme
				: new Color(51, 153, 255, 50); // Lighter blue for light theme

		Color markAllHighlightColor = isDarkTheme ? Color.decode("#32593D") : Color.decode("#ffc800");
		Color matchedBracketBackground = isDarkTheme ? adjustBrightness(Color.decode("#3B514D"), 1.2f) : Color.decode("#93D9D9");
		Color markOccurrencesColor = adjustBrightness(editorSelectionBackground, isDarkTheme ? 0.6f : 1.4f);

		// Set the syntax colors for the theme
		if (isDarkTheme) {
			Color dataTypeColor = Color.decode("#4EC9B0");
			scheme.getStyle(Token.COMMENT_EOL).foreground = Color.decode("#57A64A");
			scheme.getStyle(Token.COMMENT_MULTILINE).foreground = Color.decode("#57A64A");
			scheme.getStyle(Token.COMMENT_DOCUMENTATION).foreground = Color.decode("#57A64A");
			scheme.getStyle(Token.COMMENT_KEYWORD).foreground = Color.decode("#57A64A");
			scheme.getStyle(Token.COMMENT_MARKUP).foreground = Color.decode("#57A64A");
			scheme.getStyle(Token.RESERVED_WORD).foreground = Color.decode("#569CD6");
			scheme.getStyle(Token.RESERVED_WORD_2).foreground = dataTypeColor;
			scheme.getStyle(Token.FUNCTION).foreground = Color.decode("#DCDCAA");
			scheme.getStyle(Token.LITERAL_NUMBER_DECIMAL_INT).foreground = Color.decode("#D7BA7D");
			scheme.getStyle(Token.LITERAL_NUMBER_FLOAT).foreground = Color.decode("#D7BA7D");
			scheme.getStyle(Token.LITERAL_NUMBER_HEXADECIMAL).foreground = Color.decode("#D7BA7D");
			scheme.getStyle(Token.LITERAL_BOOLEAN).foreground = Color.decode("#569CD6");
			scheme.getStyle(Token.LITERAL_CHAR).foreground = Color.decode("#CE9178");
			scheme.getStyle(Token.LITERAL_STRING_DOUBLE_QUOTE).foreground = Color.decode("#CE9178");
			scheme.getStyle(Token.DATA_TYPE).foreground = dataTypeColor;
			scheme.getStyle(Token.OPERATOR).foreground = Color.WHITE;
			scheme.getStyle(Token.SEPARATOR).foreground = Color.WHITE;
			scheme.getStyle(Token.IDENTIFIER).foreground = themeForeground;
			// XML-specific colors for dark theme
			scheme.getStyle(Token.MARKUP_TAG_DELIMITER).foreground = Color.decode("#808080"); // Gray for < > /
			scheme.getStyle(Token.MARKUP_TAG_NAME).foreground = Color.decode("#569CD6"); // Blue for tag names
			scheme.getStyle(Token.MARKUP_TAG_ATTRIBUTE).foreground = Color.decode("#9CDCFE"); // Light blue for attributes
			scheme.getStyle(Token.MARKUP_TAG_ATTRIBUTE_VALUE).foreground = Color.decode("#CE9178"); // Orange for values
		} else {
			Color dataTypeColor = Color.decode("#267F99");
			scheme.getStyle(Token.COMMENT_EOL).foreground = Color.decode("#008000");
			scheme.getStyle(Token.COMMENT_MULTILINE).foreground = Color.decode("#008000");
			scheme.getStyle(Token.COMMENT_DOCUMENTATION).foreground = Color.decode("#008000");
			scheme.getStyle(Token.COMMENT_KEYWORD).foreground = Color.decode("#008000");
			scheme.getStyle(Token.COMMENT_MARKUP).foreground = Color.decode("#008000");
			scheme.getStyle(Token.RESERVED_WORD).foreground = Color.decode("#0000FF");
			scheme.getStyle(Token.RESERVED_WORD_2).foreground = dataTypeColor;
			scheme.getStyle(Token.FUNCTION).foreground = Color.decode("#795E26");
			scheme.getStyle(Token.ANNOTATION).foreground = Color.decode("#9E8809");
			scheme.getStyle(Token.LITERAL_NUMBER_DECIMAL_INT).foreground = Color.decode("#098658");
			scheme.getStyle(Token.LITERAL_NUMBER_FLOAT).foreground = Color.decode("#098658");
			scheme.getStyle(Token.LITERAL_NUMBER_HEXADECIMAL).foreground = Color.decode("#098658");
			scheme.getStyle(Token.LITERAL_BOOLEAN).foreground = Color.decode("#0451A5");
			scheme.getStyle(Token.LITERAL_CHAR).foreground = Color.decode("#067d17");
			scheme.getStyle(Token.LITERAL_STRING_DOUBLE_QUOTE).foreground = Color.decode("#067d17"); // Soft blue for values
			scheme.getStyle(Token.DATA_TYPE).foreground = dataTypeColor;
			scheme.getStyle(Token.OPERATOR).foreground = Color.decode("#333333");
			scheme.getStyle(Token.SEPARATOR).foreground = Color.decode("#333333");
			scheme.getStyle(Token.IDENTIFIER).foreground = themeForeground;
			// XML-specific colors for light theme
			scheme.getStyle(Token.MARKUP_TAG_DELIMITER).foreground = Color.decode("#800000"); // Dark red for < > /
			scheme.getStyle(Token.MARKUP_TAG_NAME).foreground = Color.decode("#4A7A4F"); // Soft green for tag names (keys)
			scheme.getStyle(Token.MARKUP_TAG_ATTRIBUTE).foreground = Color.decode("#FF0000"); // Red for attributes
			scheme.getStyle(Token.MARKUP_TAG_ATTRIBUTE_VALUE).foreground = Color.decode("#0000FF"); // Blue for values
		}

		textArea.setBackground(editorBackground);
		textArea.setCaretColor(caretForeground);
		textArea.setSelectionColor(selectionColor);
		textArea.setCurrentLineHighlightColor(lineHighlight);
		textArea.setMarkAllHighlightColor(markAllHighlightColor);
		textArea.setMarkOccurrencesColor(markOccurrencesColor);
		textArea.setHyperlinkForeground(editorSelectionBackground);
		textArea.setMatchedBracketBGColor(matchedBracketBackground);
		textArea.setMatchedBracketBorderColor(lineNumberForeground);

		textArea.setPaintMatchedBracketPair(true);
		textArea.setAnimateBracketMatching(false);
		textArea.setFadeCurrentLineHighlight(true);

		// Reset gutter colors directly to ensure the change applies
		Gutter gutter = RSyntaxUtilities.getGutter(textArea);
		if (gutter != null) {
			gutter.setBackground(editorBackground);
			gutter.setBorderColor(separatorForeground);
			gutter.setLineNumberColor(lineNumberForeground);
		}
	}

	private static boolean isDarkTheme(Color background) {
		double brightness = (background.getRed() * 0.299
				+ background.getGreen() * 0.587
				+ background.getBlue() * 0.114) / 255;
		return brightness < 0.5;
	}

	private static Color adjustBrightness(Color color, float factor) {
		float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
		hsb[2] = Math.min(1.0f, hsb[2] * factor); // Adjust brightness
		return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
	}

}
