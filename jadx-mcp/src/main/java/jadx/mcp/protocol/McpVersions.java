package jadx.mcp.protocol;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Supported MCP protocol revisions (all use Streamable HTTP transport)
 */
public final class McpVersions {
	public static final String V2025_11_25 = "2025-11-25";
	public static final String V2025_06_18 = "2025-06-18";
	public static final String V2025_03_26 = "2025-03-26";

	public static final String LATEST = V2025_11_25;

	/**
	 * Version assumed by server if client doesn't send 'MCP-Protocol-Version' header
	 */
	public static final String DEFAULT_HTTP_VERSION = V2025_03_26;

	public static final List<String> SUPPORTED = Collections.unmodifiableList(
			Arrays.asList(V2025_11_25, V2025_06_18, V2025_03_26));

	private McpVersions() {
	}

	public static boolean isSupported(@Nullable String version) {
		return version != null && SUPPORTED.contains(version);
	}

	/**
	 * Return requested version if supported, otherwise latest supported version.
	 * Client should disconnect if it doesn't support returned version.
	 */
	public static String negotiate(@Nullable String requested) {
		if (isSupported(requested)) {
			return requested;
		}
		return LATEST;
	}

	/**
	 * JSON-RPC batching allowed only in 2025-03-26 revision
	 */
	public static boolean isBatchAllowed(@Nullable String version) {
		return V2025_03_26.equals(version);
	}

	/**
	 * Structured tool output ('structuredContent' and 'outputSchema') added in 2025-06-18
	 */
	public static boolean isStructuredOutputSupported(@Nullable String version) {
		return version != null && !V2025_03_26.equals(version);
	}
}
