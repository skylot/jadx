package jadx.gui.ui.hexeditor.editor;

import java.awt.Color;
import java.awt.SystemColor;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Defines color schemes for the JHexEditor.
 * Includes logic for a theme-dependent scheme ("Themed") that updates
 * dynamically when the LookAndFeel changes during runtime.
 */
public class JHexEditorColors {

	private static final JHexEditorColors THEMED_INSTANCE = new JHexEditorColors("Themed");
	private static volatile boolean themedInitialized = false;
	private static final List<JHexEditor> themedComponents = new CopyOnWriteArrayList<>();
	private static final Object themedLock = new Object();

	public String name;
	public Color addressAreaEven, addressAreaOdd;
	public Color addressTextEven, addressTextOdd;
	public Color addressDivider;
	public Color hexAreaEven, hexAreaOdd;
	public Color hexTextEven, hexTextOdd;
	public Color hexDivider;
	public Color textAreaEven, textAreaOdd;
	public Color textPrintableEven, textPrintableOdd;
	public Color textUnprintableEven, textUnprintableOdd;
	public Color textDivider;
	public Color activeCursor, activeCursorMidByte;
	public Color inactiveCursor, inactiveCursorMidByte;
	public Color hexAreaActiveHighlightEven, hexAreaActiveHighlightOdd;
	public Color hexAreaInactiveHighlightEven, hexAreaInactiveHighlightOdd;
	public Color hexTextActiveHighlightEven, hexTextActiveHighlightOdd;
	public Color hexTextInactiveHighlightEven, hexTextInactiveHighlightOdd;
	public Color textAreaActiveHighlightEven, textAreaActiveHighlightOdd;
	public Color textAreaInactiveHighlightEven, textAreaInactiveHighlightOdd;
	public Color textPrintableActiveHighlightEven, textPrintableActiveHighlightOdd;
	public Color textPrintableInactiveHighlightEven, textPrintableInactiveHighlightOdd;
	public Color textUnprintableActiveHighlightEven, textUnprintableActiveHighlightOdd;
	public Color textUnprintableInactiveHighlightEven, textUnprintableInactiveHighlightOdd;
	public Color headerArea, headerText, headerDivider;

	public JHexEditorColors(String name, Color... colors) {
		this.name = name;
		int i = 0;
		addressAreaEven = safeGetColor(colors, i++, Color.WHITE);
		addressTextEven = safeGetColor(colors, i++, Color.BLACK);
		addressDivider = safeGetColor(colors, i++, Color.GRAY);
		hexAreaEven = safeGetColor(colors, i++, Color.WHITE);
		hexAreaOdd = safeGetColor(colors, i++, Color.LIGHT_GRAY);
		hexTextEven = safeGetColor(colors, i++, Color.BLACK);
		hexTextOdd = safeGetColor(colors, i++, Color.BLACK);
		textUnprintableEven = safeGetColor(colors, i++, Color.GRAY);
		textUnprintableOdd = safeGetColor(colors, i++, Color.GRAY);
		hexDivider = safeGetColor(colors, i++, Color.LIGHT_GRAY);
		activeCursor = safeGetColor(colors, i++, Color.BLUE);
		activeCursorMidByte = safeGetColor(colors, i++, Color.RED);
		inactiveCursor = safeGetColor(colors, i++, Color.GRAY);
		inactiveCursorMidByte = safeGetColor(colors, i++, Color.DARK_GRAY);
		hexAreaActiveHighlightEven = safeGetColor(colors, i++, SystemColor.textHighlight);
		hexAreaActiveHighlightOdd = safeGetColor(colors, i++, SystemColor.textHighlight);
		hexAreaInactiveHighlightEven = safeGetColor(colors, i++, Color.LIGHT_GRAY);
		hexAreaInactiveHighlightOdd = safeGetColor(colors, i++, Color.LIGHT_GRAY);
		hexTextActiveHighlightEven = safeGetColor(colors, i++, SystemColor.textHighlight);
		hexTextActiveHighlightOdd = safeGetColor(colors, i++, SystemColor.textHighlight);
		hexTextInactiveHighlightEven = safeGetColor(colors, i++, Color.DARK_GRAY);
		hexTextInactiveHighlightOdd = safeGetColor(colors, i++, Color.DARK_GRAY);
		textUnprintableActiveHighlightEven = safeGetColor(colors, i++, Color.DARK_GRAY);
		textUnprintableActiveHighlightOdd = safeGetColor(colors, i++, Color.DARK_GRAY);
		textUnprintableInactiveHighlightEven = safeGetColor(colors, i++, Color.GRAY);
		textUnprintableInactiveHighlightOdd = safeGetColor(colors, i++, Color.GRAY);
		headerArea = safeGetColor(colors, i++, addressAreaEven);
		headerText = safeGetColor(colors, i++, addressTextEven);
		headerDivider = safeGetColor(colors, i++, addressDivider);
		addressAreaOdd = safeGetColor(colors, i++, addressAreaEven);
		addressTextOdd = safeGetColor(colors, i++, addressTextEven);
		textAreaEven = safeGetColor(colors, i++, hexAreaEven);
		textAreaOdd = safeGetColor(colors, i++, hexAreaOdd);
		textPrintableEven = safeGetColor(colors, i++, hexTextEven);
		textPrintableOdd = safeGetColor(colors, i++, hexTextOdd);
		textDivider = safeGetColor(colors, i++, hexDivider);
		textAreaActiveHighlightEven = safeGetColor(colors, i++, hexAreaActiveHighlightEven);
		textAreaActiveHighlightOdd = safeGetColor(colors, i++, hexAreaActiveHighlightOdd);
		textAreaInactiveHighlightEven = safeGetColor(colors, i++, hexAreaInactiveHighlightEven);
		textAreaInactiveHighlightOdd = safeGetColor(colors, i++, hexAreaInactiveHighlightOdd);
		textPrintableActiveHighlightEven = safeGetColor(colors, i++, hexTextActiveHighlightEven);
		textPrintableActiveHighlightOdd = safeGetColor(colors, i++, hexTextActiveHighlightOdd);
		textPrintableInactiveHighlightEven = safeGetColor(colors, i++, hexTextInactiveHighlightEven);
		textPrintableInactiveHighlightOdd = safeGetColor(colors, i++, hexTextInactiveHighlightOdd);
	}

	public JHexEditorColors(String name, int... colors) {
		this(name, convertIntsToColors(colors));
	}

	private JHexEditorColors(String name) {
		this.name = name;
	}

	/**
	 * Returns an instance of the “Themed” color scheme, which is updated
	 * when the LookAndFeel is changed.
	 */
	public static JHexEditorColors getThemed() {
		if (!themedInitialized) {
			initializeThemed();
		}
		return THEMED_INSTANCE;
	}

	/**
	 * Registers the JHexEditor component to receive notifications about theme changes.
	 * Called from the JHexEditor.
	 */
	protected static void registerThemedComponent(JHexEditor component) {
		themedComponents.add(component);
		if (!themedInitialized) {
			initializeThemed();
		}
	}

	/**
	 * De-registers the JHexEditor component.
	 * Called from the JHexEditor.
	 */
	protected static void unregisterThemedComponent(JHexEditor component) {
		themedComponents.remove(component);
	}

	private static void initializeThemed() {
		if (!themedInitialized) {
			synchronized (themedLock) {
				if (!themedInitialized) {
					updateThemedColorsFromUIManager();

					PropertyChangeListener lafListener = evt -> {
						if ("lookAndFeel".equals(evt.getPropertyName())) {
							SwingUtilities.invokeLater(() -> {
								updateThemedColorsFromUIManager();
								notifyThemedComponents();
							});
						}
					};
					UIManager.addPropertyChangeListener(lafListener);
					themedInitialized = true;
				}
			}
		}
	}

	private static void updateThemedColorsFromUIManager() {
		JHexEditorColors instance = THEMED_INSTANCE;
		JHexEditorColors fallback = SYSTEM;

		instance.addressAreaEven = getColor("Panel.background", fallback.addressAreaEven);
		instance.addressTextEven = getColor("Label.foreground", fallback.addressTextEven);
		instance.addressDivider = getColor("Component.borderColor", getColor("Separator.foreground", fallback.addressDivider));
		instance.hexAreaEven = getColor("EditorPane.background", getColor("Panel.background", fallback.hexAreaEven));
		instance.hexAreaOdd =
				getColor("Table.alternateRowColor", blend(instance.hexAreaEven, getColor("EditorPane.foreground", Color.BLACK), 0.03f)); // Розрахунок
																																			// парних/непарних
		instance.hexTextEven = getColor("EditorPane.foreground", fallback.hexTextEven);
		instance.hexTextOdd = instance.hexTextEven;
		instance.textUnprintableEven = getColor("Label.disabledForeground", fallback.textUnprintableEven);
		instance.textUnprintableOdd = instance.textUnprintableEven;
		instance.hexDivider = getColor("Component.borderColor", getColor("Separator.foreground", fallback.hexDivider));
		instance.activeCursor = getColor("EditorPane.caretForeground", fallback.activeCursor);
		instance.activeCursorMidByte = getColor("Component.error.focusedBorderColor", fallback.activeCursorMidByte); // Колір помилки для
																														// виділення
		instance.inactiveCursor = getColor("Label.disabledForeground", fallback.inactiveCursor);
		instance.inactiveCursorMidByte = instance.inactiveCursor;
		instance.hexAreaActiveHighlightEven = getColor("EditorPane.selectionBackground", fallback.hexAreaActiveHighlightEven);
		instance.hexAreaActiveHighlightOdd = instance.hexAreaActiveHighlightEven;
		instance.hexAreaInactiveHighlightEven = getColor("EditorPane.inactiveSelectionBackground", fallback.hexAreaInactiveHighlightEven);
		instance.hexAreaInactiveHighlightOdd = instance.hexAreaInactiveHighlightEven;
		instance.hexTextActiveHighlightEven = getColor("EditorPane.selectionForeground", fallback.hexTextActiveHighlightEven);
		instance.hexTextActiveHighlightOdd = instance.hexTextActiveHighlightEven;
		instance.hexTextInactiveHighlightEven = getColor("EditorPane.inactiveSelectionForeground", fallback.hexTextInactiveHighlightEven);
		instance.hexTextInactiveHighlightOdd = instance.hexTextInactiveHighlightEven;
		instance.textUnprintableActiveHighlightEven = blend(instance.hexTextActiveHighlightEven, instance.hexAreaActiveHighlightEven, 0.6f);
		instance.textUnprintableActiveHighlightOdd = instance.textUnprintableActiveHighlightEven;
		instance.textUnprintableInactiveHighlightEven =
				blend(instance.hexTextInactiveHighlightEven, instance.hexAreaInactiveHighlightEven, 0.6f);
		instance.textUnprintableInactiveHighlightOdd = instance.textUnprintableInactiveHighlightEven;
		instance.headerArea = getColor("Panel.background", fallback.headerArea);
		instance.headerText = getColor("Label.foreground", fallback.headerText);
		instance.headerDivider = getColor("Component.borderColor", getColor("Separator.foreground", fallback.headerDivider));
		instance.addressAreaOdd = instance.addressAreaEven;
		instance.addressTextOdd = instance.addressTextEven;
		instance.textAreaEven = instance.hexAreaEven;
		instance.textAreaOdd = instance.hexAreaOdd;
		instance.textPrintableEven = instance.hexTextEven;
		instance.textPrintableOdd = instance.hexTextOdd;
		instance.textDivider = instance.hexDivider;
		instance.textAreaActiveHighlightEven = instance.hexAreaActiveHighlightEven;
		instance.textAreaActiveHighlightOdd = instance.hexAreaActiveHighlightOdd;
		instance.textAreaInactiveHighlightEven = instance.hexAreaInactiveHighlightEven;
		instance.textAreaInactiveHighlightOdd = instance.hexAreaInactiveHighlightOdd;
		instance.textPrintableActiveHighlightEven = instance.hexTextActiveHighlightEven;
		instance.textPrintableActiveHighlightOdd = instance.hexTextActiveHighlightOdd;
		instance.textPrintableInactiveHighlightEven = instance.hexTextInactiveHighlightEven;
		instance.textPrintableInactiveHighlightOdd = instance.hexTextInactiveHighlightOdd;
	}

	private static void notifyThemedComponents() {
		for (JHexEditor component : themedComponents) {
			if (component.getColors() == THEMED_INSTANCE && component.isShowing()) {
				SwingUtilities.invokeLater(component::repaint);
			}
		}
	}

	public static final JHexEditorColors SYSTEM = new JHexEditorColors(
			"System",
			SystemColor.text, // 0 address background
			SystemColor.textText, // 1 address text
			SystemColor.gray, // 2 divider between address and hex
			SystemColor.text, // 3 hex background even rows
			SystemColor.text, // 4 hex background odd rows
			SystemColor.textText, // 5 hex text even rows
			SystemColor.textText, // 6 hex text odd rows
			SystemColor.textInactiveText, // 7 hex text unprintable chars even rows
			SystemColor.textInactiveText, // 8 hex text unprintable chars odd rows
			SystemColor.gray, // 9 divider between words
			SystemColor.black, // 10 cursor
			SystemColor.red, // 11 cursor in mid-byte
			SystemColor.gray, // 12 cursor on inactive side
			SystemColor.gray, // 13 cursor in mid-byte on inactive side
			SystemColor.textHighlight, // 14 highlight background even rows
			SystemColor.textHighlight, // 15 highlight background odd rows
			SystemColor.textHighlight, // 16 highlight background on inactive side even rows
			SystemColor.textHighlight, // 17 highlight background on inactive side odd rows
			SystemColor.textHighlightText, // 18 hex text selected even rows
			SystemColor.textHighlightText, // 19 hex text selected odd rows
			SystemColor.textHighlightText, // 20 hex text selected on inactive side even rows
			SystemColor.textHighlightText, // 21 hex text selected on inactive side odd rows
			SystemColor.textInactiveText, // 22 hex text selected unprintable chars even rows
			SystemColor.textInactiveText, // 23 hex text selected unprintable chars odd rows
			SystemColor.textInactiveText, // 24 hex text selected unprintable chars on inactive side even rows
			SystemColor.textInactiveText, // 25 hex text selected unprintable chars on inactive side odd rows
			SystemColor.control, // 26 header background
			SystemColor.controlText, // 27 header text
			SystemColor.gray // 28 header divider
	);

	public static final JHexEditorColors MONO = new JHexEditorColors(
			"Mono",
			Color.white, // 0 address background
			Color.black, // 1 address text
			Color.black, // 2 divider between address and hex
			Color.white, // 3 hex background even rows
			Color.white, // 4 hex background odd rows
			Color.black, // 5 hex text even rows
			Color.black, // 6 hex text odd rows
			Color.lightGray, // 7 hex text unprintable chars even rows
			Color.lightGray, // 8 hex text unprintable chars odd rows
			Color.black, // 9 divider between words
			Color.black, // 10 cursor
			Color.red, // 11 cursor in mid-byte
			Color.gray, // 12 cursor on inactive side
			Color.gray, // 13 cursor in mid-byte on inactive side
			Color.black, // 14 highlight background even rows
			Color.black, // 15 highlight background odd rows
			Color.gray, // 16 highlight background on inactive side even rows
			Color.gray, // 17 highlight background on inactive side odd rows
			Color.white, // 18 hex text selected even rows
			Color.white, // 19 hex text selected odd rows
			Color.white, // 20 hex text selected on inactive side even rows
			Color.white, // 21 hex text selected on inactive side odd rows
			Color.darkGray, // 22 hex text selected unprintable chars even rows
			Color.darkGray, // 23 hex text selected unprintable chars odd rows
			Color.darkGray, // 24 hex text selected unprintable chars on inactive side even rows
			Color.darkGray, // 25 hex text selected unprintable chars on inactive side odd rows
			Color.white, // 26 header background
			Color.black, // 27 header text
			Color.black // 28 header divider
	);

	public static final JHexEditorColors AMBER = new JHexEditorColors("Amber", 0xFF443300, 0xFFFFEE00, 0xFF554400, 0xFF000000, 0xFF332200,
			0xFFFFEE00, 0xFFFFEE00, 0x40FFEE00, 0x40FFEE00, 0xFF554400, 0xFFFFEE00, 0xFFFFFF99, 0xFF807700, 0xFF80804C, 0xFFFFEE00,
			0xFFFFEE00, 0x66FFEE00, 0x66FFEE00, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0x40000000,
			0x40000000, 0xFF443300, 0xFFFFEE00, 0xFF554400);
	public static final JHexEditorColors AQUA = new JHexEditorColors("Aqua", 0xFFE6E6E6, 0xFF000000, 0xFFDEDEDE, 0xFFFFFFFF, 0xFFE6E6FF,
			0xFF32323D, 0xFF32323D, 0x4032323D, 0x4032323D, 0xFFBFBFBF, 0xFF000000, 0xFFCC0000, 0xFF808080, 0xFFE68080, 0xFF89B8FC,
			0xFF89B8FC, 0x6689B8FC, 0x6689B8FC, 0xFF32323D, 0xFF32323D, 0xFF32323D, 0xFF32323D, 0x4032323D, 0x4032323D, 0x4032323D,
			0x4032323D, 0xFFE6E6E6, 0xFF000000, 0xFF32323D);
	public static final JHexEditorColors AVOCADO = new JHexEditorColors("Avocado", 0xFFBFE658, 0xFF5B8B23, 0xFF8ACA7A, 0xFFD6FFAF,
			0xFFD6EAAF, 0xFF546020, 0xFF546020, 0x40546020, 0x40546020, 0xFF90BF03, 0xFF290050, 0xFFE00050, 0xFF9580A8, 0xFFF080A8,
			0xFF8DC83D, 0xFF8DC83D, 0x668DC83D, 0x668DC83D, 0xFF546020, 0xFF546020, 0xFF546020, 0xFF546020, 0x40546020, 0x40546020,
			0x40546020, 0x40546020, 0xFFD7FE58, 0xFF0B421E, 0xFFB5CA2F);
	public static final JHexEditorColors BLACK_AND_WHITE = new JHexEditorColors("Black & White", 0xFFEEEEEE, 0xFF000000, 0xFFCBCBCB,
			0xFFFFFFFF, 0xFFF2F2F2, 0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0xFF000000, 0xFF000000, 0xFFCC0000, 0xFF808080,
			0xFFE68080, 0xFFC6C6C6, 0xFFC6C6C6, 0x99C6C6C6, 0x99C6C6C6, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0x40000000,
			0x40000000, 0x40000000, 0x40000000, 0xFFEEEEEE, 0xFF000000, 0xFF555555);
	public static final JHexEditorColors BLUE = new JHexEditorColors("Blue", 0xFFCAEBFF, 0xFF2A0E78, 0xFFA8D6E6, 0xFFD6F8FF, 0xFFCFF2FF,
			0xFF230B63, 0xFF230B63, 0x40230B63, 0x40230B63, 0xFF7575FF, 0xFF300D00, 0xFFD00D00, 0xFF988680, 0xFFCC8680, 0xFF89B8FC,
			0xFF89B8FC, 0x6689B8FC, 0x6689B8FC, 0xFF230B63, 0xFF230B63, 0xFF230B63, 0xFF230B63, 0x40230B63, 0x40230B63, 0x40230B63,
			0x40230B63, 0xFFADDDFA, 0xFF270D6E, 0xFF7AADC5);
	public static final JHexEditorColors DARK_BLUE = new JHexEditorColors("Dark Blue", 0xFF6C18B0, 0xFFFFC7D2, 0xFF8154D1, 0xFF1822CD,
			0xFF1116AE, 0xFFFAFA11, 0xFFFAFA11, 0x40FAFA11, 0x40FAFA11, 0xFF02007D, 0xFFE7DD32, 0xFFFF6600, 0xFF736E19, 0xFF803B06,
			0xFFFAFA11, 0xFFFAFA11, 0x66FAFA11, 0x66FAFA11, 0xFF1822CD, 0xFF1116AE, 0xFF1822CD, 0xFF1116AE, 0x401822CD, 0x401116AE,
			0x401822CD, 0x401116AE, 0xFF4B45A6, 0xFFEEEEEE, 0xFF402ECC);
	public static final JHexEditorColors GREEN = new JHexEditorColors("Green", 0xFF43D66F, 0xFF000000, 0xFF00BF00, 0xFFDCFFDC, 0xFFC7FFC7,
			0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0xFF7AFF9C, 0xFF230023, 0xFFD00023, 0xFF928092, 0xFFF08092, 0xFF5DF124,
			0xFF5DF124, 0x665DF124, 0x665DF124, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0x40000000,
			0x40000000, 0xFF7CEE8D, 0xFF000000, 0xFF00A100);
	public static final JHexEditorColors GREENSCREEN = new JHexEditorColors("Greenscreen", 0xFF003300, 0xFF00FF00, 0xFF004400, 0xFF000000,
			0xFF002200, 0xFF00FF00, 0xFF00FF00, 0x4000FF00, 0x4000FF00, 0xFF004400, 0xFF00EE00, 0xFF99FF99, 0xFF007700, 0xFF4C804C,
			0xFF00FF00, 0xFF00FF00, 0x6600FF00, 0x6600FF00, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0x40000000, 0x40000000,
			0x40000000, 0x40000000, 0xFF003300, 0xFF00FF00, 0xFF004400);
	public static final JHexEditorColors GREY_1 = new JHexEditorColors("Grey 1", 0xFFCCCCCC, 0xFF000000, 0xFF888888, 0xFFEEEEEE, 0xFFD9D9D9,
			0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0xFFBFBFBF, 0xFF111111, 0xFFDD1111, 0xFF888888, 0xFFEE8888, 0xFF9E9E9E,
			0xFF9E9E9E, 0x669E9E9E, 0x669E9E9E, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0x40000000,
			0x40000000, 0xFFAAAAAA, 0xFF000000, 0xFF555555);
	public static final JHexEditorColors GREY_2 = new JHexEditorColors("Grey 2", 0xFFD6D6D6, 0xFF000000, 0xFFC5C5C5, 0xFFEEEEEE, 0xFFDADADA,
			0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0xFFBFBFBF, 0xFF111111, 0xFFDD1111, 0xFF888888, 0xFFEE8888, 0xFFB0B7C0,
			0xFFB0B7C0, 0x66B0B7C0, 0x66B0B7C0, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0x40000000,
			0x40000000, 0xFFDDDDE8, 0xFF000000, 0xFFAAAAAA);
	public static final JHexEditorColors HOLLOWAY = new JHexEditorColors("Holloway", 0xFF232176, 0xFFFFFFFF, 0xFF232176, 0xFFFFFFFF,
			0xFFFDF0D8, 0xFF232176, 0xFF232176, 0x40232176, 0x40232176, 0xFF43428A, 0xFF000000, 0xFFCC0000, 0xFF808080, 0xFFE68080,
			0xFFE5C050, 0xFFE2AF2D, 0x66E5C050, 0x66E2AF2D, 0xFF232176, 0xFF232176, 0xFF232176, 0xFF232176, 0x40232176, 0x40232176,
			0x40232176, 0x40232176, 0xFF43428A, 0xFFFFFFFF, 0xFF43428A);
	public static final JHexEditorColors HOT_PINK = new JHexEditorColors("Hot Pink", 0xFFCC00CC, 0xFFFFFF00, 0xFFCC9966, 0xFFFF00FF,
			0xFFEE00EE, 0xFFFFFF00, 0xFFFFFF00, 0x80FFFF00, 0x80FFFF00, 0xFFCC9966, 0xFF666600, 0xFFCC0000, 0xFF999980, 0xFFE68080,
			0xFFFFFF66, 0xFFFFFF66, 0x99FFFF66, 0x99FFFF66, 0xFFCC00CC, 0xFFCC00CC, 0xFFCC00CC, 0xFFCC00CC, 0x80CC00CC, 0x80CC00CC,
			0x80CC00CC, 0x80CC00CC, 0xFFCC00CC, 0xFFFFFF00, 0xFFCC9966);
	public static final JHexEditorColors LAVENDER = new JHexEditorColors("Lavender", 0xFFE4E0F6, 0xFF3A004E, 0xFFD3C3EF, 0xFFF6EAFF,
			0xFFE2DEF6, 0xFF3A004E, 0xFF3A004E, 0x403A004E, 0x403A004E, 0xFFDDB3FF, 0xFF091500, 0xFFD01500, 0xFF848C80, 0xFFF08C80,
			0xFFCB88FC, 0xFFCB88F6, 0x66CB88FC, 0x66CB88F6, 0xFF3A004E, 0xFF3A004E, 0xFF3A004E, 0xFF3A004E, 0x403A004E, 0x403A004E,
			0x403A004E, 0x403A004E, 0xFFD9D9F3, 0xFF3A034E, 0xFFC1ABE9);
	public static final JHexEditorColors MURICA = new JHexEditorColors("Murica", 0xFFFFDDDD, 0xFF0000CC, 0xFFDD0000, 0xFFFFFFFF, 0xFFE6E6FF,
			0xFF0000CC, 0xFF0000CC, 0x400000CC, 0x400000CC, 0xFFDD0000, 0xFFFF0000, 0xFFFF00FF, 0xFFFF8080, 0xFFFF80FF, 0xFFFF9999,
			0xFFFF9999, 0x66FF9999, 0x66FF9999, 0xFF0000CC, 0xFF0000CC, 0xFF0000CC, 0xFF0000CC, 0x400000CC, 0x400000CC, 0x400000CC,
			0x400000CC, 0xFFFFDDDD, 0xFF0000CC, 0xFFDD0000);
	public static final JHexEditorColors NEWTECH = new JHexEditorColors("Newtech", 0xFF662A85, 0xFFFFFFFF, 0xFF331542, 0xFFFFFFFF,
			0xFFE9E2F8, 0xFF110018, 0xFF110018, 0x40110018, 0x40110018, 0xFF994068, 0xFF000000, 0xFFCC0000, 0xFF808080, 0xFFE68080,
			0xFFCC66FF, 0xFFCC66FF, 0x66CC66FF, 0x66CC66FF, 0xFF110018, 0xFF110018, 0xFF110018, 0xFF110018, 0x40110018, 0x40110018,
			0x40110018, 0x40110018, 0xFF662A85, 0xFFFFFFFF, 0xFF331542);
	public static final JHexEditorColors OCEAN = new JHexEditorColors("Ocean", 0xFFBDE5E6, 0xFF000000, 0xFFC7E3E7, 0xFFE6FBFA, 0xFFD6F1F1,
			0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0xFFA3D1D9, 0xFF190405, 0xFFD00405, 0xFF8C8283, 0xFFF08283, 0xFF89B8FA,
			0xFF89B8F1, 0x6689B8FA, 0x6689B8F1, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0x40000000,
			0x40000000, 0xFFB4DAE0, 0xFF000000, 0xFFC9E6EA);
	public static final JHexEditorColors PASTEL = new JHexEditorColors("Pastel", 0xFFB8CBFA, 0xFF511285, 0xFF6876E7, 0xFFFBFAC7, 0xFFD9FDD4,
			0xFF15544F, 0xFF15544F, 0x4015544F, 0x4015544F, 0xFF7D9E7E, 0xFF000000, 0xFFCC0000, 0xFF808080, 0xFFE68080, 0xFFFBB18C,
			0xFFD9B18C, 0x66FBB18C, 0x66D9B18C, 0xFF15544F, 0xFF15544F, 0xFF15544F, 0xFF15544F, 0x4015544F, 0x4015544F, 0x4015544F,
			0x4015544F, 0xFFD0BFEE, 0xFF37129C, 0xFF6876E7);
	public static final JHexEditorColors PINK = new JHexEditorColors("Pink", 0xFFF3D7D9, 0xFF550000, 0xFFFFB8CA, 0xFFFFEEEE, 0xFFF2D6D6,
			0xFF550000, 0xFF550000, 0x40550000, 0x40550000, 0xFFD3ABDE, 0xFF001111, 0xFFCC1111, 0xFF808888, 0xFFE68888, 0xFFF19EBD,
			0xFFF19EBD, 0x66F19EBD, 0x66F19EBD, 0xFF550000, 0xFF550000, 0xFF550000, 0xFF550000, 0x40550000, 0x40550000, 0x40550000,
			0x40550000, 0xFFF3D7D9, 0xFF7D0000, 0xFFFFBBCF);
	public static final JHexEditorColors POLYTECH = new JHexEditorColors("Polytech", 0xFF1E431B, 0xFFFFFFFF, 0xFF374E34, 0xFFFFFFFF,
			0xFFF2E9D0, 0xFF004400, 0xFF004400, 0x40004400, 0x40004400, 0xFFA0B29F, 0xFF000000, 0xFFCC0000, 0xFF808080, 0xFFE68080,
			0xFFD7AE45, 0xFFD7AE45, 0x66D7AE45, 0x66D7AE45, 0xFF004400, 0xFF004400, 0xFF004400, 0xFF004400, 0x40004400, 0x40004400,
			0x40004400, 0x40004400, 0xFFD7AE45, 0xFF003700, 0xFF374E34);
	public static final JHexEditorColors RED = new JHexEditorColors("Red", 0xFFFF8080, 0xFF000000, 0xFFFF0000, 0xFFFFE6E6, 0xFFFFBDBD,
			0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0xFFFF9199, 0xFF001919, 0xFFCC1919, 0xFF808C8C, 0xFFE68C8C, 0xFFED715F,
			0xFFED715F, 0x66ED715F, 0x66ED715F, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0x40000000,
			0x40000000, 0xFFFF8080, 0xFF000000, 0xFFFF0000);
	public static final JHexEditorColors RED_PASTEL = new JHexEditorColors("Red Pastel", 0xFFFBC6C8, 0xFF000000, 0xFFC893A5, 0xFFFFDCFF,
			0xFFFDD6D6, 0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0xFFE6AEEB, 0xFF002300, 0xFFCC2300, 0xFF809280, 0xFFE69280,
			0xFFF19B8F, 0xFFF19B8F, 0x99F19B8F, 0x99F19B8F, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0x40000000, 0x40000000,
			0x40000000, 0x40000000, 0xFFF6ADAE, 0xFF59000E, 0xFFCC7F88);
	public static final JHexEditorColors SAGE = new JHexEditorColors("Sage", 0xFFE6E6E6, 0xFF000000, 0xFFDEDEDE, 0xFFFFFFFF, 0xFFE6F3D9,
			0xFF32323D, 0xFF32323D, 0x4032323D, 0x4032323D, 0xFFAEB8BF, 0xFF000000, 0xFFCC0000, 0xFF808080, 0xFFE68080, 0xFFCEF696,
			0xFFCEF396, 0x99CEF696, 0x99CEF396, 0xFF32323D, 0xFF32323D, 0xFF32323D, 0xFF32323D, 0x4032323D, 0x4032323D, 0x4032323D,
			0x4032323D, 0xFFE6E6E6, 0xFF000000, 0xFF32323D);
	public static final JHexEditorColors SKY = new JHexEditorColors("Sky", 0xFFE1EDFF, 0xFF000055, 0xFFA4D3EB, 0xFFDEF8FF, 0xFFE0EAFF,
			0xFF000055, 0xFF000055, 0x40000055, 0x40000055, 0xFF9EB5FF, 0xFF210700, 0xFFDC0700, 0xFF908480, 0xFFF68480, 0xFF89B8FC,
			0xFF89B8FC, 0x6689B8FC, 0x6689B8FC, 0xFF000055, 0xFF000055, 0xFF000055, 0xFF000055, 0x40000055, 0x40000055, 0x40000055,
			0x40000055, 0xFFDEEEFF, 0xFF00007D, 0xFF9CB6CE);
	public static final JHexEditorColors TURQUOISE = new JHexEditorColors("Turquoise", 0xFFC3EAEA, 0xFF003A00, 0xFF61AF88, 0xFFEEFFEE,
			0xFFC3EAEA, 0xFF001F00, 0xFF001F00, 0x40001F00, 0x40001F00, 0xFF99BFCA, 0xFF110011, 0xFFCC0011, 0xFF888088, 0xFFE68088,
			0xFF65EEE0, 0xFF65EAE0, 0x6665EEE0, 0x6665EAE0, 0xFF001F00, 0xFF001F00, 0xFF001F00, 0xFF001F00, 0x40001F00, 0x40001F00,
			0x40001F00, 0x40001F00, 0xFFC3EAEA, 0xFF003A00, 0xFF61AF88);
	public static final JHexEditorColors VIOLET = new JHexEditorColors("Violet", 0xFFDFC0FF, 0xFF000000, 0xFFBD7FE6, 0xFFF9F0FF, 0xFFEEE0FB,
			0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0xFFD8ABFF, 0xFF060F00, 0xFFCF0F00, 0xFF838880, 0xFFE98880, 0xFFCB89FC,
			0xFFCB89FB, 0x66CB89FC, 0x66CB89FB, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0x40000000,
			0x40000000, 0xFFDFC0FF, 0xFF000000, 0xFFBF7FE6);
	public static final JHexEditorColors WHITE_ON_BLACK = new JHexEditorColors("White on Black", 0xFF333333, 0xFFFFFFFF, 0xFF444444,
			0xFF000000, 0xFF222222, 0xFFFFFFFF, 0xFFFFFFFF, 0x40FFFFFF, 0x40FFFFFF, 0xFF444444, 0xFFEEEEEE, 0xFFCCCCFF, 0xFF777777,
			0xFF666688, 0xFFFFFFFF, 0xFFFFFFFF, 0x66FFFFFF, 0x66FFFFFF, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0x40000000,
			0x40000000, 0x40000000, 0x40000000, 0xFF333333, 0xFFFFFFFF, 0xFF444444);
	public static final JHexEditorColors WHITE_ON_BLUE = new JHexEditorColors("White on Blue", 0xFF0000CC, 0xFFFFFFFF, 0xFF0000BB,
			0xFF0000FF, 0xFF0000DD, 0xFFFFFFFF, 0xFFFFFFFF, 0x80FFFFFF, 0x80FFFFFF, 0xFF0000DD, 0xFFEEEEFF, 0xFFFFFF66, 0xFF777788,
			0xFF888833, 0xFFFFFFFF, 0xFFFFFFFF, 0x99FFFFFF, 0x99FFFFFF, 0xFF0000FF, 0xFF0000FF, 0xFF0000FF, 0xFF0000FF, 0x800000FF,
			0x800000FF, 0x800000FF, 0x800000FF, 0xFF0000CC, 0xFFFFFFFF, 0xFF0000BB);
	public static final JHexEditorColors WILD_BLUE = new JHexEditorColors("Wild Blue", 0xFF71CAD0, 0xFF550000, 0xFF5DBBCB, 0xFFD9F3FF,
			0xFFCBEEEE, 0xFF590059, 0xFF590059, 0x40590059, 0x40590059, 0xFF6F77BF, 0xFF260C00, 0xFFE00C00, 0xFF938680, 0xFFF08680,
			0xFF89B8FC, 0xFF89B8EE, 0x6689B8FC, 0x6689B8EE, 0xFF590059, 0xFF590059, 0xFF590059, 0xFF590059, 0x40590059, 0x40590059,
			0x40590059, 0x40590059, 0xFF5DBACA, 0xFF6C18B0, 0xFF184B81);
	public static final JHexEditorColors WILD_RED = new JHexEditorColors("Wild Red", 0xFFE56278, 0xFF004E00, 0xFFB8377E, 0xFFFFDCFF,
			0xFFFDCACA, 0xFF00004E, 0xFF00004E, 0x4000004E, 0x4000004E, 0xFFCC9DD4, 0xFF002300, 0xFFCC2300, 0xFF809180, 0xFFE69180,
			0xFFEC6746, 0xFFEC6746, 0x66EC6746, 0x66EC6746, 0xFF000046, 0xFF000046, 0xFF000046, 0xFF000046, 0x40000046, 0x40000046,
			0x40000046, 0x40000046, 0xFFE6949C, 0xFF7C2D7D, 0xFFCC7F88);
	public static final JHexEditorColors YELLOW = new JHexEditorColors("Yellow", 0xFFE6E658, 0xFF000000, 0xFFF6F3B2, 0xFFF0F095, 0xFFEAEA80,
			0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0xFFCCC16E, 0xFF0F0F6A, 0xFFCF0F6A, 0xFF8888B5, 0xFFE888B5, 0xFFD7C63D,
			0xFFD7C63D, 0x66D7C63D, 0x66D7C63D, 0xFF000000, 0xFF000000, 0xFF000000, 0xFF000000, 0x40000000, 0x40000000, 0x40000000,
			0x40000000, 0xFFEBE98A, 0xFF000000, 0xFFFCFA92);

	public static final JHexEditorColors[] BUILTINS = {
			SYSTEM, MONO,
			AMBER, AQUA, AVOCADO,
			BLACK_AND_WHITE, BLUE,
			DARK_BLUE,
			GREEN, GREENSCREEN, GREY_1, GREY_2,
			HOLLOWAY, HOT_PINK,
			LAVENDER, MURICA, NEWTECH, OCEAN,
			PASTEL, PINK, POLYTECH,
			RED, RED_PASTEL,
			SAGE, SKY,
			TURQUOISE, VIOLET,
			WHITE_ON_BLACK, WHITE_ON_BLUE,
			WILD_BLUE, WILD_RED,
			YELLOW,
			THEMED_INSTANCE
	};

	public Color[] getColorArray() {
		return new Color[] {
				addressAreaEven, addressTextEven, addressDivider,
				hexAreaEven, hexAreaOdd, hexTextEven, hexTextOdd,
				textUnprintableEven, textUnprintableOdd, hexDivider,
				activeCursor, activeCursorMidByte,
				inactiveCursor, inactiveCursorMidByte,
				hexAreaActiveHighlightEven, hexAreaActiveHighlightOdd,
				hexAreaInactiveHighlightEven, hexAreaInactiveHighlightOdd,
				hexTextActiveHighlightEven, hexTextActiveHighlightOdd,
				hexTextInactiveHighlightEven, hexTextInactiveHighlightOdd,
				textUnprintableActiveHighlightEven, textUnprintableActiveHighlightOdd,
				textUnprintableInactiveHighlightEven, textUnprintableInactiveHighlightOdd,
				headerArea, headerText, headerDivider,
				addressAreaOdd, addressTextOdd, textAreaEven, textAreaOdd,
				textPrintableEven, textPrintableOdd, textDivider,
				textAreaActiveHighlightEven, textAreaActiveHighlightOdd,
				textAreaInactiveHighlightEven, textAreaInactiveHighlightOdd,
				textPrintableActiveHighlightEven, textPrintableActiveHighlightOdd,
				textPrintableInactiveHighlightEven, textPrintableInactiveHighlightOdd,
		};
	}

	public int[] getRGBArray() {
		Color[] colors = getColorArray();
		int[] rgbs = new int[colors.length];
		for (int i = 0; i < colors.length; i++) {
			rgbs[i] = (colors[i] != null) ? colors[i].getRGB() : 0;
		}
		return rgbs;
	}

	private static Color safeGetColor(Color[] colors, int index, Color fallback) {
		Color c = (colors != null && index >= 0 && index < colors.length) ? colors[index] : null;
		return (c != null) ? c : fallback;
	}

	private static Color[] convertIntsToColors(int[] intColors) {
		if (intColors == null)
			return new Color[0];
		Color[] colorArray = new Color[intColors.length];
		for (int i = 0; i < intColors.length; i++) {
			colorArray[i] = new Color(intColors[i], true);
		}
		return colorArray;
	}

	private static Color blend(Color c1, Color c2, float alpha) {
		if (c1 == null || c2 == null)
			return c1 != null ? c1 : (c2 != null ? c2 : Color.GRAY);
		float r = (c1.getRed() * (1.0f - alpha)) + (c2.getRed() * alpha);
		float g = (c1.getGreen() * (1.0f - alpha)) + (c2.getGreen() * alpha);
		float b = (c1.getBlue() * (1.0f - alpha)) + (c2.getBlue() * alpha);
		return new Color(Math.max(0, Math.min(255, (int) r)),
				Math.max(0, Math.min(255, (int) g)),
				Math.max(0, Math.min(255, (int) b)));
	}

	private static Color getColor(String key, Color fallback) {
		Color color = UIManager.getColor(key);
		return (color != null) ? color : fallback;
	}

	@Override
	public String toString() {
		return name;
	}
}
