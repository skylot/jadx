package jadx.core.deobf;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NameMapper {

	private static final Set<String> RESERVED_NAMES = new HashSet<String>(
			Arrays.asList(new String[]{
					"abstract",
					"assert",
					"boolean",
					"break",
					"byte",
					"case",
					"catch",
					"char",
					"class",
					"const",
					"continue",
					"default",
					"do",
					"double",
					"else",
					"enum",
					"extends",
					"false",
					"final",
					"finally",
					"float",
					"for",
					"goto",
					"if",
					"implements",
					"import",
					"instanceof",
					"int",
					"interface",
					"long",
					"native",
					"new",
					"null",
					"package",
					"private",
					"protected",
					"public",
					"return",
					"short",
					"static",
					"strictfp",
					"super",
					"switch",
					"synchronized",
					"this",
					"throw",
					"throws",
					"transient",
					"true",
					"try",
					"void",
					"volatile",
					"while",
			})
	);

	public static boolean isReserved(String str) {
		return RESERVED_NAMES.contains(str);
	}

}
