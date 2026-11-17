package jadx.gui.ui.filedialog;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomFileChooserTest {

	@Test
	void pathOfFile() {
		assertThat(CustomFileChooser.toPathOrNull(new File("dir", "app.apk")))
				.isEqualTo(Path.of("dir", "app.apk"));
	}

	@Test
	void noPathForFileOutsideFileSystem() {
		// like the "This PC" shell folder on Windows, but invalid on every platform
		assertThat(CustomFileChooser.toPathOrNull(new File("virtual\0folder"))).isNull();
	}
}
