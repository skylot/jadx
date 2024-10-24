package jadx.core.deobf.conditions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;

import jadx.core.dex.nodes.PackageNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Provides a list of all top level domains, so we can exclude them from deobfuscation.
 */
public class ExcludePackageWithTLDNames extends AbstractDeobfCondition {

	/**
	 * Lazy load TLD set
	 */
	private static class TldHolder {
		private static final Set<String> TLD_SET = loadTldSet();
	}

	private static Set<String> loadTldSet() {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(TldHolder.class.getResourceAsStream("tlds.txt")))) {
			return reader.lines()
					.filter(line -> !line.startsWith("#") && !line.isEmpty())
					.collect(Collectors.toSet());
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to load top level domain list file: tlds.txt", e);
		}
	}

	@Override
	public Action check(PackageNode pkg) {
		if (pkg.isRoot() && TldHolder.TLD_SET.contains(pkg.getName())) {
			return Action.FORBID_RENAME;
		}
		return Action.NO_ACTION;
	}
}
