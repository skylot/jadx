package jadx.gui.ui.codearea;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.gui.jobs.BackgroundExecutor;
import jadx.gui.jobs.IBackgroundTask;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.theme.EditorThemeManager;
import jadx.gui.ui.codearea.theme.RSTABundledTheme;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.panel.ProgressPanel;
import jadx.gui.utils.FontUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SmaliAreaFontTest {

	@Test
	void testBundledThemeClonesSyntaxSchemePerArea() {
		RSTABundledTheme theme = new RSTABundledTheme("default");
		theme.load();

		RSyntaxTextArea first = new RSyntaxTextArea();
		theme.apply(first);
		Font firstFont = FontUtils.getCompositeFont(Font.MONOSPACED, Font.PLAIN, 15);
		first.setFont(firstFont);

		RSyntaxTextArea second = new RSyntaxTextArea();
		theme.apply(second);
		Font secondFont = FontUtils.getCompositeFont(Font.SANS_SERIF, Font.PLAIN, 18);
		second.setFont(secondFont);

		SyntaxScheme firstScheme = first.getSyntaxScheme();
		SyntaxScheme secondScheme = second.getSyntaxScheme();
		assertThat(firstScheme).isNotSameAs(secondScheme);

		assertThat(firstScheme.getStyle(TokenTypes.RESERVED_WORD).font.getSize()).isEqualTo(15);
		assertThat(secondScheme.getStyle(TokenTypes.RESERVED_WORD).font.getSize()).isEqualTo(18);
		assertThat(secondScheme.getStyle(TokenTypes.RESERVED_WORD).font.getFamily()).isEqualTo(secondFont.getFamily());
	}

	@Test
	void testSmaliAreaNormalAndDebugModelsMatchTokenFontsAndMetrics(@TempDir Path tempDir) throws Exception {
		MainWindow mainWindow = buildTestMainWindow();
		JadxSettings settings = mainWindow.getSettings();
		Font codeFont = FontUtils.getCompositeFont(Font.SANS_SERIF, Font.PLAIN, 12);
		Font smaliFont = FontUtils.getCompositeFont(Font.MONOSPACED, Font.PLAIN, 16);
		settings.setCodeFont(codeFont);
		settings.setSmaliFont(smaliFont);

		EditorThemeManager themeManager = mainWindow.getEditorThemeManager();
		themeManager.setTheme("RSTA:default");

		ContentPanel contentPanel = new ContentPanel(null, null) {
			private static final long serialVersionUID = 1L;

			@Override
			public void loadSettings() {
			}

			@Override
			public MainWindow getMainWindow() {
				return mainWindow;
			}
		};

		Path smaliFile = tempDir.resolve("TestClass.smali");
		Files.writeString(smaliFile, ""
				+ ".class public LTestClass;\n"
				+ ".super Ljava/lang/Object;\n"
				+ ".method public static test()V\n"
				+ "    .registers 0\n"
				+ "    return-void\n"
				+ ".end method\n");
		JadxArgs args = new JadxArgs();
		args.setInputFile(smaliFile.toFile());
		try (JadxDecompiler decompiler = new JadxDecompiler(args)) {
			decompiler.load();
			JavaClass javaCls = decompiler.getClasses().get(0);
			JClass jClass = new JClass(javaCls, null, null);

			SmaliArea normalArea = new SmaliArea(contentPanel, jClass, false);
			assertThat(normalArea.getFont().getFamily()).isEqualTo(smaliFont.getFamily());
			assertThat(normalArea.getFont().getSize()).isEqualTo(smaliFont.getSize());
			assertTokenFontsMatchMetrics(normalArea, smaliFont);

			// Re-applying settings should keep smaliFont and token font/metrics consistency
			normalArea.loadSettings();
			assertThat(normalArea.getFont().getFamily()).isEqualTo(smaliFont.getFamily());
			assertThat(normalArea.getFont().getSize()).isEqualTo(smaliFont.getSize());
			assertTokenFontsMatchMetrics(normalArea, smaliFont);

			SmaliArea debugArea = new SmaliArea(contentPanel, jClass, true);
			assertThat(debugArea.getFont().getFamily()).isEqualTo(smaliFont.getFamily());
			assertThat(debugArea.getFont().getSize()).isEqualTo(smaliFont.getSize());
			assertTokenFontsMatchMetrics(debugArea, smaliFont);
		}
	}

	private static void assertTokenFontsMatchMetrics(RSyntaxTextArea area, Font expectedBaseFont) throws Exception {
		BufferedImage img = new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = img.createGraphics();
		try {
			Method refresh = RSyntaxTextArea.class.getDeclaredMethod("refreshFontMetrics", Graphics2D.class);
			refresh.setAccessible(true);
			refresh.invoke(area, g2d);

			int[] tokenTypes = {
					TokenTypes.IDENTIFIER,
					TokenTypes.RESERVED_WORD,
					TokenTypes.RESERVED_WORD_2,
					TokenTypes.DATA_TYPE,
					TokenTypes.COMMENT_EOL,
					TokenTypes.FUNCTION,
					TokenTypes.LITERAL_BOOLEAN,
					TokenTypes.LITERAL_STRING_DOUBLE_QUOTE,
			};
			for (int tokenType : tokenTypes) {
				Font tokenFont = area.getFontForTokenType(tokenType);
				Font metricsFont = area.getFontMetricsForTokenType(tokenType).getFont();
				assertThat(tokenFont)
						.as("Font and FontMetrics mismatch for token type %d", tokenType)
						.isEqualTo(metricsFont);
				assertThat(tokenFont.getFamily()).isEqualTo(expectedBaseFont.getFamily());
				assertThat(tokenFont.getSize()).isEqualTo(expectedBaseFont.getSize());
			}
		} finally {
			g2d.dispose();
		}
	}

	private static MainWindow buildTestMainWindow() throws Exception {
		MainWindow mainWindow = TestMainWindowShim.build();
		JadxSettings settings = mainWindow.getSettings();

		Field themeField = MainWindow.class.getDeclaredField("editorThemeManager");
		themeField.setAccessible(true);
		themeField.set(mainWindow, new EditorThemeManager(settings));

		Field execField = MainWindow.class.getDeclaredField("backgroundExecutor");
		execField.setAccessible(true);
		execField.set(mainWindow, new BackgroundExecutor(settings, new ProgressPanel(mainWindow, false)) {
			@Override
			public void execute(IBackgroundTask task) {
			}
		});
		return mainWindow;
	}
}
