package jadx.core.dex.nodes.parser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.Consts;
import jadx.core.dex.attributes.IAttributeNode;
import jadx.core.dex.attributes.annotations.Annotation;
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

	@SuppressWarnings("unchecked")
	public static SignatureParser fromNode(IAttributeNode node) {
		Annotation a = node.getAnnotation(Consts.DALVIK_SIGNATURE);
		if (a == null) {
			return null;
		}
		return new SignatureParser(mergeSignature((List<String>) a.getDefaultValue()));
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
		if (mark >= pos) {
			return "";
		}
		return sign.substring(mark, pos);
	}

	/**
	 * Inclusive slice (includes current character)
	 */
	private String inclusiveSlice() {
		if (mark >= pos) {
			return "";
		}
		return sign.substring(mark, pos + 1);
	}

	private boolean forwardTo(char lastChar) {
		int startPos = pos;
		char ch;
		while ((ch = next()) != STOP_CHAR) {
			if (ch == lastChar) {
				return true;
			}
		}
		pos = startPos;
		return false;
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

	private String consumeUntil(char lastChar) {
		mark();
		return forwardTo(lastChar) ? slice() : null;
	}

	public ArgType consumeType() {
		char ch = next();
		mark();
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
				if (forwardTo(';')) {
					return ArgType.genericType(slice());
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
		throw new JadxRuntimeException("Can't parse type: " + debugString());
	}

	private ArgType consumeObjectType(boolean incompleteType) {
		mark();
		int ch;
		do {
			ch = next();
			if (ch == STOP_CHAR) {
				return null;
			}
		} while (ch != '<' && ch != ';');

		if (ch == ';') {
			String obj;
			if (incompleteType) {
				obj = slice().replace('/', '.');
			} else {
				obj = inclusiveSlice();
			}
			return ArgType.object(obj);
		} else {
			// generic type start ('<')
			String obj = slice();
			if (!incompleteType) {
				obj += ";";
			}
			ArgType[] genArr = consumeGenericArgs();
			consume('>');

			ArgType genericType = ArgType.generic(obj, genArr);
			if (lookAhead('.')) {
				consume('.');
				next();
				// type parsing not completed, proceed to inner class
				ArgType inner = consumeObjectType(true);
				return ArgType.genericInner(genericType, inner.getObject(), inner.getGenericTypes());
			} else {
				consume(';');
				return genericType;
			}
		}
	}

	private ArgType[] consumeGenericArgs() {
		List<ArgType> list = new LinkedList<>();
		ArgType type;
		do {
			if (lookAhead('*')) {
				next();
				type = ArgType.wildcard();
			} else if (lookAhead('+')) {
				next();
				type = ArgType.wildcard(consumeType(), 1);
			} else if (lookAhead('-')) {
				next();
				type = ArgType.wildcard(consumeType(), -1);
			} else {
				type = consumeType();
			}
			if (type != null) {
				list.add(type);
			}
		} while (type != null && !lookAhead('>'));
		return list.toArray(new ArgType[list.size()]);
	}

	/**
	 * Map of generic types names to extends classes.
	 * <p/>
	 * Example: "<T:Ljava/lang/Exception;:Ljava/lang/Object;>"
	 */
	public Map<ArgType, List<ArgType>> consumeGenericMap() {
		if (!lookAhead('<')) {
			return Collections.emptyMap();
		}
		Map<ArgType, List<ArgType>> map = new LinkedHashMap<>(2);
		consume('<');
		while (true) {
			if (lookAhead('>') || next() == STOP_CHAR) {
				break;
			}
			String id = consumeUntil(':');
			if (id == null) {
				LOG.error("Can't parse generic map: {}", sign);
				return Collections.emptyMap();
			}
			tryConsume(':');
			List<ArgType> types = consumeExtendsTypesList();
			map.put(ArgType.genericType(id), types);
		}
		consume('>');
		return map;
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
			if (!argType.equals(ArgType.OBJECT)) {
				if (types.isEmpty()) {
					types = new LinkedList<>();
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

	public List<ArgType> consumeMethodArgs() {
		consume('(');
		if (lookAhead(')')) {
			consume(')');
			return Collections.emptyList();
		}
		List<ArgType> args = new LinkedList<>();
		do {
			args.add(consumeType());
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

	private String debugString() {
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
