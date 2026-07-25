package jadx.gui.settings;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jadx.core.utils.GsonUtils;
import jadx.gui.utils.LangLocale;
import jadx.gui.utils.NLS;

/** Reads {@code langLocale} from gui config before full Gson deserialization. */
public final class GuiConfigLocale {
	private static final Logger LOG = LoggerFactory.getLogger(GuiConfigLocale.class);

	private GuiConfigLocale() {
	}

	public static void applyFromConfig(Path configPath) {
		if (configPath == null || !Files.isRegularFile(configPath)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(configPath)).getAsJsonObject();
			JsonElement langElement = root.get("langLocale");
			if (langElement == null || langElement.isJsonNull()) {
				return;
			}
			LangLocale langLocale = GsonUtils.buildGson().fromJson(langElement, LangLocale.class);
			if (langLocale != null && langLocale.get() != null) {
				NLS.setLocale(langLocale);
			}
		} catch (Exception e) {
			LOG.warn("Failed to read language from config: {}", configPath, e);
		}
	}
}
