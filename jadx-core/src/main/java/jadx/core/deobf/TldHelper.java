package jadx.core.deobf;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Provides a list of all top level domains with 3 characters and less,
 * so we can exclude them from deobfuscation.
 */
public class TldHelper {

	private static final Set<String> TLD_SET = loadTldFile();

	private static Set<String> loadTldFile() {
		Set<String> tldNames = new HashSet<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TldHelper.class.getResourceAsStream("tld_3.txt")))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("#") && !line.isEmpty()) {
					tldNames.add(line);
				}
			}
			return tldNames;
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to load top level domain list tld_3.txt", e);
		}
	}

	public static boolean contains(String name) {
		return TLD_SET.contains(name);
	}
}
