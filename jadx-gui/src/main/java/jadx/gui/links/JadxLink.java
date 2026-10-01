package jadx.gui.links;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;

/**
 * Link to a method (or class) inside an APK identified by its hash:
 * <br>
 * {@code jadx://<hash prefix>/<full.class.Name>.<method>(<arg types>)}
 * <br>
 * Example: {@code jadx://3fa9c01b7e/com.example.LoginActivity.check(java.lang.String,int)}
 * <br>
 * The legacy separator {@code jadx://<hash>:<target>} is still accepted on input.
 * <br>
 * Original (not renamed/deobfuscated) names are used, so links work for everybody with the same
 * APK.
 * Argument types are optional ({@code method()} or {@code method}), in that case the first overload
 * is used.
 * Without parentheses, a link to a class is also accepted.
 */
public final class JadxLink {
	public static final String SCHEME = "jadx";
	public static final int HASH_PREFIX_LEN = 10;

	private static final Pattern HASH_PATTERN = Pattern.compile("[0-9a-fA-F]{6,64}");

	private final String hashPrefix;
	private final String clsName;
	private final @Nullable String mthName;
	/**
	 * null if not specified
	 */
	private final @Nullable List<String> argTypes;

	private JadxLink(String hashPrefix, String clsName, @Nullable String mthName, @Nullable List<String> argTypes) {
		this.hashPrefix = hashPrefix.toLowerCase(Locale.ROOT);
		this.clsName = clsName;
		this.mthName = mthName;
		this.argTypes = argTypes;
	}

	public static boolean isJadxLink(@Nullable String str) {
		return str != null && str.trim().toLowerCase(Locale.ROOT).startsWith(SCHEME + ':');
	}

	/**
	 * @throws IllegalArgumentException on invalid link
	 */
	public static JadxLink parse(String link) {
		String str = link.trim();
		if (!isJadxLink(str)) {
			throw new IllegalArgumentException("Not a jadx link: " + link);
		}
		str = str.substring(SCHEME.length() + 1);
		// strip the authority marker ('//', or a single/absent slash)
		while (str.startsWith("/")) {
			str = str.substring(1);
		}
		// some launchers/browsers append a slash
		while (str.endsWith("/")) {
			str = str.substring(0, str.length() - 1);
		}

		// separator between hash and target: '/' (current format), or ':' (legacy, still accepted).
		// Target names never contain '/' or ':', so the first of either delimits the hash.
		int sep = firstIndexOf(str, '/', ':');
		if (sep == -1) {
			throw new IllegalArgumentException("Missing '/' after hash in link: " + link);
		}
		String hash = str.substring(0, sep);
		if (!HASH_PATTERN.matcher(hash).matches()) {
			throw new IllegalArgumentException("Invalid hash in link: " + link);
		}
		String ref = percentDecode(str.substring(sep + 1)).replace(" ", "");
		int argsStart = ref.indexOf('(');
		if (argsStart == -1) {
			// class link, or method without args specified: resolved later
			if (ref.isEmpty()) {
				throw new IllegalArgumentException("Missing class name in link: " + link);
			}
			return new JadxLink(hash, ref, null, null);
		}
		if (!ref.endsWith(")")) {
			throw new IllegalArgumentException("Missing ')' in link: " + link);
		}
		String clsAndMth = ref.substring(0, argsStart);
		int mthSep = clsAndMth.lastIndexOf('.');
		if (mthSep <= 0 || mthSep == clsAndMth.length() - 1) {
			throw new IllegalArgumentException("Expected 'class.method()' in link: " + link);
		}
		String argsStr = ref.substring(argsStart + 1, ref.length() - 1);
		List<String> args = argsStr.isEmpty() ? Collections.emptyList() : List.of(argsStr.split(","));
		return new JadxLink(hash, clsAndMth.substring(0, mthSep), clsAndMth.substring(mthSep + 1), args);
	}

	public static String build(String hash, String clsFullName) {
		return SCHEME + "://" + hash.substring(0, HASH_PREFIX_LEN) + '/' + percentEncode(clsFullName);
	}

	public static String build(String hash, MethodInfo mth) {
		StringBuilder sb = new StringBuilder();
		sb.append(mth.getDeclClass().getFullName()).append('.').append(mth.getName()).append('(');
		List<ArgType> args = mth.getArgumentsTypes();
		for (int i = 0; i < args.size(); i++) {
			if (i != 0) {
				sb.append(',');
			}
			sb.append(typeToString(args.get(i)));
		}
		sb.append(')');
		return SCHEME + "://" + hash.substring(0, HASH_PREFIX_LEN) + '/' + percentEncode(sb.toString());
	}

	/**
	 * Arg type as in Java source but with full class names, generics erased: 'int',
	 * 'java.lang.String[]'
	 */
	public static String typeToString(ArgType type) {
		if (type.isArray()) {
			return typeToString(type.getArrayElement()) + "[]";
		}
		if (type.isPrimitive()) {
			return type.getPrimitiveType().getLongName();
		}
		if (type.isObject()) {
			return type.getObject().replace('$', '.');
		}
		return type.toString();
	}

	/**
	 * Compare arg types from link with method arg types.
	 * Short class names are also accepted: 'String' matches 'java.lang.String'.
	 */
	public boolean argsMatch(List<ArgType> mthArgs) {
		if (argTypes == null) {
			return true;
		}
		if (argTypes.size() != mthArgs.size()) {
			return false;
		}
		for (int i = 0; i < argTypes.size(); i++) {
			String linkType = argTypes.get(i).replace('$', '.');
			String mthType = typeToString(mthArgs.get(i));
			if (!mthType.equals(linkType) && !mthType.endsWith('.' + linkType)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Encode chars which can break links in text editors or URI parsers ('<', '>', '[', ']', spaces,
	 * ...)
	 */
	private static String percentEncode(String str) {
		StringBuilder sb = new StringBuilder();
		for (byte b : str.getBytes(StandardCharsets.UTF_8)) {
			char c = (char) (b & 0xFF);
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
					|| c == '.' || c == '_' || c == '$' || c == ',' || c == '(' || c == ')' || c == '-') {
				sb.append(c);
			} else {
				sb.append('%').append(String.format("%02X", b & 0xFF));
			}
		}
		return sb.toString();
	}

	private static String percentDecode(String str) {
		if (str.indexOf('%') == -1) {
			return str;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
		for (int i = 0; i < bytes.length; i++) {
			byte b = bytes[i];
			if (b == '%' && i + 2 < bytes.length) {
				int hi = Character.digit(bytes[i + 1], 16);
				int lo = Character.digit(bytes[i + 2], 16);
				if (hi != -1 && lo != -1) {
					out.write(hi * 16 + lo);
					i += 2;
					continue;
				}
			}
			out.write(b);
		}
		return out.toString(StandardCharsets.UTF_8);
	}

	public String getHashPrefix() {
		return hashPrefix;
	}

	public String getClsName() {
		return clsName;
	}

	public @Nullable String getMthName() {
		return mthName;
	}

	public @Nullable List<String> getArgTypes() {
		return argTypes == null ? null : new ArrayList<>(argTypes);
	}

	/**
	 * Index of the first occurrence of any of the given chars, or -1.
	 */
	private static int firstIndexOf(String str, char a, char b) {
		int ia = str.indexOf(a);
		int ib = str.indexOf(b);
		if (ia == -1) {
			return ib;
		}
		if (ib == -1) {
			return ia;
		}
		return Math.min(ia, ib);
	}

	@Override
	public String toString() {
		return SCHEME + "://" + hashPrefix + '/' + clsName
				+ (mthName != null ? '.' + mthName + '(' + String.join(",", argTypes) + ')' : "");
	}
}
