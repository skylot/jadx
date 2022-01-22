package jadx.core.dex.nodes.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.SignatureAttr;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class SignatureParser {
	private static final Logger LOG = LoggerFactory.getLogger(SignatureParser.class);

	private static final char STOP_CHAR = 0;

	private final String sign;
	private final int end;
	private int pos;
	private int mark;

	public SignatureParser(String signature) {
		sign = signature;
		end = sign.length();
		pos = -1;
		mark = 0;
	}

	@Nullable
	public static SignatureParser fromNode(IAttributeNode node) {
		String signature = getSignature(node);
		if (signature == null) {
			return null;
		}
		return new SignatureParser(signature);
	}

	@Nullable
	public static String getSignature(IAttributeNode node) {
		SignatureAttr attr = node.get(JadxAttrType.SIGNATURE);
		if (attr == null) {
			return null;
		}
		return attr.getSignature();
	}

	private char next() {
		pos++;
		if (pos >= end) {
			return STOP_CHAR;
		}
		return sign.charAt(pos);
	}

	private boolean lookAhead(char ch) {
		int next = pos + 1;
		return next < end && sign.charAt(next) == ch;
	}

	private void mark() {
		mark = pos;
	}

	/**
	 * Exclusive slice.
	 *
	 * @return string from 'mark' to current position (not including current character)
	 */
	private String slice() {
		int start = mark == -1 ? 0 : mark;
		if (start >= pos) {
			return "";
		}
		return sign.substring(start, pos);
	}

	/**
	 * Inclusive slice (includes current character)
	 */
	private String inclusiveSlice() {
		int start = mark;
		if (start == -1) {
			start = 0;
		}
		int last = pos + 1;
		if (start >= last) {
			return "";
		}
		return sign.substring(start, last);
	}

	private boolean skipUntil(char untilChar) {
		int startPos = pos;
		while (true) {
			if (lookAhead(untilChar)) {
				return true;
			}
			char ch = next();
			if (ch == STOP_CHAR) {
				pos = startPos;
				return false;
			}
		}
	}

	private void consume(char exp) {
		char c = next();
		if (exp != c) {
			throw new JadxRuntimeException("Consume wrong char: '" + c + "' != '" + exp
					+ "', sign: " + debugString());
		}
	}

	private boolean tryConsume(char exp) {
		if (lookAhead(exp)) {
			next();
			return true;
		}
		return false;
	}

	@Nullable
	public String consumeUntil(char lastChar) {
		mark();
		return skipUntil(lastChar) ? inclusiveSlice() : null;
	}

	public ArgType consumeType() {
		char ch = next();
		switch (ch) {
			case 'L':
				ArgType obj = consumeObjectType(false);
				if (obj != null) {
					return obj;
				}
				break;
			case 'T':
				next();
				mark();
				String typeVarName = consumeUntil(';');
				if (typeVarName != null) {
					consume(';');
					if (typeVarName.contains(")")) {
						throw new JadxRuntimeException("Bad name for type variable: " + typeVarName);
					}
					return ArgType.genericType(typeVarName);
				}
				break;

			case '[':
				return ArgType.array(consumeType());

			case STOP_CHAR:
				return null;

			default:
				// primitive type (one char)
				ArgType type = ArgType.parse(ch);
				if (type != null) {
					return type;
				}
				break;
		}
		throw new JadxRuntimeException("Can't parse type: " + debugString() + ", unexpected: " + ch);
	}

	private ArgType consumeObjectType(boolean innerType) {
		mark();
		int ch;
		do {
			if (innerType && lookAhead('.')) {
				// stop before next nested inner class
				return ArgType.object(inclusiveSlice());
			}
			ch = next();
			if (ch == STOP_CHAR) {
				return null;
			}
		} while (ch != '<' && ch != ';');

		if (ch == ';') {
			String obj;
			if (innerType) {
				obj = slice().replace('/', '.');
			} else {
				obj = inclusiveSlice();
			}
			return ArgType.object(obj);
		}
		// generic type start ('<')
		String obj = slice();
		if (!innerType) {
			obj += ';';
		} else {
			obj = obj.replace('/', '.');
		}
		List<ArgType> typeVars = consumeGenericArgs();
		consume('>');

		ArgType genericType = ArgType.generic(obj, typeVars);
		if (!lookAhead('.')) {
			consume(';');
			return genericType;
		}
		consume('.');
		next();
		// type parsing not completed, proceed to inner class
		ArgType inner = consumeObjectType(true);
		if (inner == null) {
			throw new JadxRuntimeException("No inner type found: " + debugString());
		}
		// for every nested inner type create nested type object
		while (lookAhead('.')) {
			genericType = ArgType.outerGeneric(genericType, inner);
			consume('.');
			next();
			inner = consumeObjectType(true);
			if (inner == null) {
				throw new JadxRuntimeException("Unexpected inner type found: " + debugString());
			}
		}
		return ArgType.outerGeneric(genericType, inner);
	}

	private List<ArgType> consumeGenericArgs() {
		List<ArgType> list = new ArrayList<>();
		ArgType type;
		do {
			if (lookAhead('*')) {
				next();
				type = ArgType.wildcard();
			} else if (lookAhead('+')) {
				next();
				type = ArgType.wildcard(consumeType(), ArgType.WildcardBound.EXTENDS);
			} else if (lookAhead('-')) {
				next();
				type = ArgType.wildcard(consumeType(), ArgType.WildcardBound.SUPER);
			} else {
				type = consumeType();
			}
			if (type != null) {
				list.add(type);
			}
		} while (type != null && !lookAhead('>'));
		return list;
	}

	/**
	 * Map of generic types names to extends classes.
	 * <p>
	 * Example: "&lt;T:Ljava/lang/Exception;:Ljava/lang/Object;&gt;"
	 */
	@SuppressWarnings("ConditionalBreakInInfiniteLoop")
	public List<ArgType> consumeGenericTypeParameters() {
		if (!lookAhead('<')) {
			return Collections.emptyList();
		}
		List<ArgType> list = new ArrayList<>();
		consume('<');
		while (true) {
			if (lookAhead('>') || next() == STOP_CHAR) {
				break;
			}
			String id = consumeUntil(':');
			if (id == null) {
				throw new JadxRuntimeException("Failed to parse generic types map");
			}
			consume(':');
			tryConsume(':');
			List<ArgType> types = consumeExtendsTypesList();
			list.add(ArgType.genericType(id, types));
		}
		consume('>');
		return list;
	}

	/**
	 * List of types separated by ':' last type is 'java.lang.Object'.
	 * <p/>
	 * Example: "Ljava/lang/Exception;:Ljava/lang/Object;"
	 */
	private List<ArgType> consumeExtendsTypesList() {
		List<ArgType> types = Collections.emptyList();
		boolean next;
		do {
			ArgType argType = consumeType();
			if (argType == null) {
				throw new JadxRuntimeException("Unexpected end of signature");
			}
			if (!argType.equals(ArgType.OBJECT)) {
				if (types.isEmpty()) {
					types = new ArrayList<>();
				}
				types.add(argType);
			}
			next = lookAhead(':');
			if (next) {
				consume(':');
			}
		} while (next);
		return types;
	}

	public List<ArgType> consumeMethodArgs(int argsCount) {
		consume('(');
		if (lookAhead(')')) {
			consume(')');
			return Collections.emptyList();
		}
		List<ArgType> args = new ArrayList<>(argsCount);
		int limit = argsCount + 10; // just prevent endless loop, args count can be different for synthetic methods
		do {
			ArgType type = consumeType();
			if (type == null) {
				throw new JadxRuntimeException("Unexpected end of signature");
			}
			args.add(type);
			if (args.size() > limit) {
				throw new JadxRuntimeException("Arguments count limit reached: " + args.size());
			}
		} while (!lookAhead(')'));
		consume(')');
		return args;
	}

	private static String mergeSignature(List<String> list) {
		if (list.size() == 1) {
			return list.get(0);
		}
		StringBuilder sb = new StringBuilder();
		for (String s : list) {
			sb.append(s);
		}
		return sb.toString();
	}

	public String getSignature() {
		return sign;
	}

	private String debugString() {
		if (pos >= sign.length()) {
			return sign;
		}
		return sign + " at position " + pos + " ('" + sign.charAt(pos) + "')";
	}

	@Override
	public String toString() {
		if (pos == -1) {
			return sign;
		}
		return sign.substring(0, mark) + '{' + sign.substring(mark, pos) + '}' + sign.substring(pos);
	}
}
