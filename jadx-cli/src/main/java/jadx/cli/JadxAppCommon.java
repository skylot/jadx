package jadx.cli;

import java.util.Set;

import jadx.api.JadxArgs;
import jadx.api.security.JadxSecurityFlag;
import jadx.api.security.impl.JadxSecurity;
import jadx.commons.app.JadxCommonEnv;
import jadx.zip.security.DisabledZipSecurity;
import jadx.zip.security.IJadxZipSecurity;
import jadx.zip.security.JadxZipSecurity;

public class JadxAppCommon {

	public static void applyEnvVars(JadxArgs jadxArgs) {
		Set<JadxSecurityFlag> flags = JadxSecurityFlag.all();
		IJadxZipSecurity zipSecurity;

		boolean disableXmlSecurity = JadxCommonEnv.getBool("JADX_DISABLE_XML_SECURITY", false);
		if (disableXmlSecurity) {
			flags.remove(JadxSecurityFlag.SECURE_XML_PARSER);
			// TODO: not related to 'xml security', but kept for compatibility
			flags.remove(JadxSecurityFlag.VERIFY_APP_PACKAGE);
		}

		boolean disableZipSecurity = JadxCommonEnv.getBool("JADX_DISABLE_ZIP_SECURITY", false);
		if (disableZipSecurity) {
			flags.remove(JadxSecurityFlag.SECURE_ZIP_READER);
			zipSecurity = DisabledZipSecurity.INSTANCE;
		} else {
			JadxZipSecurity jadxZipSecurity = new JadxZipSecurity();
			int maxZipEntriesCount = JadxCommonEnv.getInt("JADX_ZIP_MAX_ENTRIES_COUNT", -2);
			if (maxZipEntriesCount != -2) {
				jadxZipSecurity.setMaxEntriesCount(maxZipEntriesCount);
			}
			int zipBombMinUncompressedSize = JadxCommonEnv.getInt("JADX_ZIP_BOMB_MIN_UNCOMPRESSED_SIZE", -2);
			if (zipBombMinUncompressedSize != -2) {
				jadxZipSecurity.setZipBombMinUncompressedSize(zipBombMinUncompressedSize);
			}
			int setZipBombDetectionFactor = JadxCommonEnv.getInt("JADX_ZIP_BOMB_DETECTION_FACTOR", -2);
			if (setZipBombDetectionFactor != -2) {
				jadxZipSecurity.setZipBombDetectionFactor(setZipBombDetectionFactor);
			}
			zipSecurity = jadxZipSecurity;
		}
		jadxArgs.setSecurity(new JadxSecurity(flags, zipSecurity));
	}
}
