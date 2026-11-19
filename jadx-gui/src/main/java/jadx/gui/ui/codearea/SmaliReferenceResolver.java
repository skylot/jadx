package jadx.gui.ui.codearea;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.Utils;

/**
 * Parses and resolves Smali class, method, and field references at a cursor position.
 */
public final class SmaliReferenceResolver {

	public enum RefKind {
		CLASS,
		METHOD,
		FIELD
	}

	public static final class SmaliRef {
		private final RefKind kind;
		private final String classDescriptor;
		private final @Nullable String memberShortId;

		public SmaliRef(RefKind kind, String classDescriptor, @Nullable String memberShortId) {
			this.kind = Objects.requireNonNull(kind);
			this.classDescriptor = Objects.requireNonNull(classDescriptor);
			this.memberShortId = memberShortId;
		}

		public RefKind getKind() {
			return kind;
		}

		public String getClassDescriptor() {
			return classDescriptor;
		}

		public @Nullable String getMemberShortId() {
			return memberShortId;
		}

		public String getClassRawName() {
			return Utils.cleanObjectName(classDescriptor);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof SmaliRef)) {
				return false;
			}
			SmaliRef other = (SmaliRef) o;
			return kind == other.kind
					&& classDescriptor.equals(other.classDescriptor)
					&& Objects.equals(memberShortId, other.memberShortId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(kind, classDescriptor, memberShortId);
		}

		@Override
		public String toString() {
			if (memberShortId == null) {
				return classDescriptor;
			}
			return classDescriptor + "->" + memberShortId;
		}
	}

	private static final class MemberRef {
		private final RefKind kind;
		private final String shortId;
		private final int refEnd;

		private MemberRef(RefKind kind, String shortId, int refEnd) {
			this.kind = kind;
			this.shortId = shortId;
			this.refEnd = refEnd;
		}
	}

	private SmaliReferenceResolver() {
	}

	public static @Nullable JavaNode resolve(@Nullable JadxDecompiler decompiler, @Nullable String line, int column) {
		SmaliRef ref = parse(line, column);
		return resolve(decompiler, ref);
	}

	public static @Nullable JavaNode resolve(@Nullable JadxDecompiler decompiler, @Nullable SmaliRef ref) {
		if (decompiler == null || ref == null) {
			return null;
		}
		RootNode root = decompiler.getRoot();
		if (root == null) {
			return null;
		}
		ClassNode clsNode = root.resolveRawClass(ref.getClassRawName());
		if (clsNode == null) {
			return null;
		}
		switch (ref.getKind()) {
			case METHOD:
				MethodNode mthNode = searchMethod(root, clsNode, ref.getMemberShortId());
				if (mthNode != null) {
					JavaNode mthJavaNode = decompiler.getJavaNodeByRef(mthNode);
					if (mthJavaNode != null) {
						return mthJavaNode;
					}
				}
				return resolveClassJavaNode(decompiler, clsNode);
			case FIELD:
				FieldNode fldNode = searchField(root, clsNode, ref.getMemberShortId());
				if (fldNode != null) {
					JavaNode fldJavaNode = decompiler.getJavaNodeByRef(fldNode);
					if (fldJavaNode != null) {
						return fldJavaNode;
					}
				}
				return resolveClassJavaNode(decompiler, clsNode);
			case CLASS:
			default:
				return resolveClassJavaNode(decompiler, clsNode);
		}
	}

	public static @Nullable SmaliRef parse(@Nullable String line, int column) {
		if (line == null || column < 0 || column >= line.length()) {
			return null;
		}
		int len = line.length();
		int i = 0;
		while (i < len && i <= column) {
			char ch = line.charAt(i);
			if (ch == '#') {
				return null;
			}
			if (ch == '"' || ch == '\'') {
				i = skipQuoted(line, i, ch);
				continue;
			}
			if (ch == '(') {
				int protoEnd = parseMethodDescriptorEnd(line, i);
				if (protoEnd != -1) {
					SmaliRef ref = findClassInProto(line, i, protoEnd, column);
					if (ref != null) {
						return ref;
					}
					i = protoEnd;
					continue;
				}
				i++;
				continue;
			}
			if ((ch == '[' || ch == 'L') && (i == 0 || isBoundaryBeforeType(line.charAt(i - 1)))) {
				int typeStart = i;
				int clsStart = skipArrayDimensions(line, i);
				if (clsStart >= len) {
					return null;
				}
				char targetChar = line.charAt(clsStart);
				if (targetChar == 'L') {
					int clsEnd = parseClassDescriptorEnd(line, clsStart);
					if (clsEnd == -1) {
						i = clsStart + 1;
						continue;
					}
					String classDesc = line.substring(clsStart, clsEnd);
					MemberRef memberRef = parseMemberRefAt(line, clsEnd);
					int refEnd = memberRef != null ? memberRef.refEnd : clsEnd;
					if (column >= typeStart && column < clsEnd) {
						return new SmaliRef(RefKind.CLASS, classDesc, null);
					}
					if (memberRef != null && column >= clsEnd && column < refEnd) {
						return new SmaliRef(memberRef.kind, classDesc, memberRef.shortId);
					}
					i = refEnd;
					continue;
				}
				if (clsStart > typeStart && isPrimitiveFieldChar(targetChar)) {
					MemberRef memberRef = parseMemberRefAt(line, clsStart + 1);
					if (memberRef != null) {
						// Consume primitive-array receiver member reference (e.g. [I->clone()Ljava/lang/Object;)
						// so its return type is not misparsed as a standalone class descriptor.
						i = memberRef.refEnd;
						continue;
					}
				}
				i = clsStart > typeStart ? clsStart : clsStart + 1;
				continue;
			}
			i++;
		}
		return null;
	}

	private static @Nullable JavaNode resolveClassJavaNode(JadxDecompiler decompiler, ClassNode clsNode) {
		JavaNode node = decompiler.getJavaNodeByRef(clsNode);
		if (node instanceof JavaClass) {
			JavaClass javaCls = (JavaClass) node;
			if (clsNode.contains(AFlag.DONT_GENERATE) && clsNode.isTopClass() && javaCls.getCodeParent() != null) {
				return javaCls.getTopParentClass();
			}
		}
		return node;
	}

	private static @Nullable MethodNode searchMethod(RootNode root, ClassNode clsNode, @Nullable String shortId) {
		if (shortId == null) {
			return null;
		}
		if (shortId.startsWith("<init>(") || shortId.startsWith("<clinit>(")) {
			return clsNode.searchMethodByShortId(shortId);
		}
		return findInHierarchy(root, clsNode, cls -> cls.searchMethodByShortId(shortId));
	}

	private static @Nullable FieldNode searchField(RootNode root, ClassNode clsNode, @Nullable String shortId) {
		if (shortId == null) {
			return null;
		}
		return findInHierarchy(root, clsNode, cls -> cls.searchFieldByShortId(shortId));
	}

	private static @Nullable <T> T findInHierarchy(RootNode root, ClassNode startCls, Function<ClassNode, @Nullable T> finder) {
		Deque<ClassNode> queue = new ArrayDeque<>();
		Set<ClassNode> visited = new HashSet<>();
		queue.add(startCls);
		visited.add(startCls);
		while (!queue.isEmpty()) {
			ClassNode cls = queue.poll();
			T found = finder.apply(cls);
			if (found != null) {
				return found;
			}
			ArgType superType = cls.getSuperClass();
			if (superType != null) {
				enqueueType(root, superType, queue, visited);
			}
			for (ArgType ifaceType : cls.getInterfaces()) {
				enqueueType(root, ifaceType, queue, visited);
			}
		}
		return null;
	}

	private static void enqueueType(RootNode root, ArgType type, Deque<ClassNode> queue, Set<ClassNode> visited) {
		ClassNode resolved = root.resolveClass(type);
		if (resolved != null && visited.add(resolved)) {
			queue.add(resolved);
		}
	}

	private static @Nullable SmaliRef findClassInProto(String line, int parenPos, int protoEnd, int column) {
		int pos = parenPos + 1;
		boolean inReturn = false;
		while (pos < protoEnd) {
			if (line.charAt(pos) == ')') {
				inReturn = true;
				pos++;
				continue;
			}
			int typeStart = pos;
			int typeEnd = parseTypeDescriptor(line, pos, inReturn);
			if (typeEnd == -1) {
				return null;
			}
			int clsStart = skipArrayDimensions(line, typeStart);
			if (clsStart < typeEnd && line.charAt(clsStart) == 'L') {
				if (column >= typeStart && column < typeEnd) {
					return new SmaliRef(RefKind.CLASS, line.substring(clsStart, typeEnd), null);
				}
			}
			pos = typeEnd;
		}
		return null;
	}

	private static @Nullable MemberRef parseMemberRefAt(String line, int arrowPos) {
		int len = line.length();
		if (arrowPos + 2 >= len || line.charAt(arrowPos) != '-' || line.charAt(arrowPos + 1) != '>') {
			return null;
		}
		int memberStart = arrowPos + 2;
		int nameEnd = parseMemberNameEnd(line, memberStart);
		if (nameEnd == -1 || nameEnd >= len) {
			return null;
		}
		char sep = line.charAt(nameEnd);
		if (sep == '(') {
			int descEnd = parseMethodDescriptorEnd(line, nameEnd);
			if (descEnd == -1 || !isEndBoundaryAfterRef(line, descEnd)) {
				return null;
			}
			return new MemberRef(RefKind.METHOD, line.substring(memberStart, descEnd), descEnd);
		}
		if (sep == ':') {
			int typeEnd = parseTypeDescriptor(line, nameEnd + 1, false);
			if (typeEnd == -1 || !isEndBoundaryAfterRef(line, typeEnd)) {
				return null;
			}
			return new MemberRef(RefKind.FIELD, line.substring(memberStart, typeEnd), typeEnd);
		}
		return null;
	}

	private static int parseMemberNameEnd(String line, int memberStart) {
		int len = line.length();
		if (memberStart >= len) {
			return -1;
		}
		if (line.startsWith("<init>(", memberStart)) {
			return memberStart + 6;
		}
		if (line.startsWith("<clinit>(", memberStart)) {
			return memberStart + 8;
		}
		int p = memberStart;
		while (p < len && isValidSimpleNameChar(line.charAt(p))) {
			p++;
		}
		return p > memberStart ? p : -1;
	}

	private static int parseMethodDescriptorEnd(String line, int parenPos) {
		int len = line.length();
		if (parenPos >= len || line.charAt(parenPos) != '(') {
			return -1;
		}
		int pos = parenPos + 1;
		while (pos < len && line.charAt(pos) != ')') {
			int next = parseTypeDescriptor(line, pos, false);
			if (next == -1) {
				return -1;
			}
			pos = next;
		}
		if (pos >= len || line.charAt(pos) != ')') {
			return -1;
		}
		return parseTypeDescriptor(line, pos + 1, true);
	}

	private static int parseTypeDescriptor(String line, int pos, boolean allowVoid) {
		int len = line.length();
		if (pos >= len) {
			return -1;
		}
		char ch = line.charAt(pos);
		if (ch == 'V') {
			return allowVoid ? pos + 1 : -1;
		}
		if (isPrimitiveFieldChar(ch)) {
			return pos + 1;
		}
		if (ch == '[') {
			int idx = skipArrayDimensions(line, pos);
			return parseTypeDescriptor(line, idx, false);
		}
		if (ch == 'L') {
			return parseClassDescriptorEnd(line, pos);
		}
		return -1;
	}

	private static int parseClassDescriptorEnd(String line, int pos) {
		int len = line.length();
		if (pos >= len || line.charAt(pos) != 'L') {
			return -1;
		}
		boolean segmentEmpty = true;
		for (int i = pos + 1; i < len; i++) {
			char ch = line.charAt(i);
			if (ch == ';') {
				return segmentEmpty ? -1 : i + 1;
			}
			if (ch == '/') {
				if (segmentEmpty) {
					return -1;
				}
				segmentEmpty = true;
			} else if (isValidSimpleNameChar(ch)) {
				segmentEmpty = false;
			} else {
				return -1;
			}
		}
		return -1;
	}

	private static int skipArrayDimensions(String line, int pos) {
		int len = line.length();
		int idx = pos;
		while (idx < len && line.charAt(idx) == '[') {
			idx++;
		}
		return idx;
	}

	private static int skipQuoted(String line, int startPos, char quoteChar) {
		int len = line.length();
		int i = startPos + 1;
		while (i < len) {
			char ch = line.charAt(i);
			if (ch == '\\') {
				i += 2;
				continue;
			}
			if (ch == quoteChar) {
				return i + 1;
			}
			i++;
		}
		return len;
	}

	private static boolean isPrimitiveFieldChar(char ch) {
		switch (ch) {
			case 'Z':
			case 'B':
			case 'C':
			case 'S':
			case 'I':
			case 'J':
			case 'F':
			case 'D':
				return true;
			default:
				return false;
		}
	}

	private static boolean isValidSimpleNameChar(char ch) {
		if (Character.isWhitespace(ch)) {
			return false;
		}
		switch (ch) {
			case ';':
			case '[':
			case '(':
			case ')':
			case ':':
			case '/':
			case '.':
			case '<':
			case '>':
			case ',':
			case '"':
			case '\'':
			case '{':
			case '}':
			case '#':
			case '@':
			case '\\':
				return false;
			default:
				return !Character.isISOControl(ch);
		}
	}

	private static boolean isBoundaryBeforeType(char ch) {
		if (Character.isWhitespace(ch)) {
			return true;
		}
		switch (ch) {
			case ',':
			case ';':
			case '(':
			case ')':
			case ':':
			case '[':
			case '{':
			case '}':
			case '=':
			case '@':
				return true;
			default:
				return false;
		}
	}

	private static boolean isEndBoundaryAfterRef(String line, int pos) {
		if (pos >= line.length()) {
			return true;
		}
		char ch = line.charAt(pos);
		if (Character.isWhitespace(ch)) {
			return true;
		}
		switch (ch) {
			case ',':
			case '}':
			case ']':
			case ')':
			case '#':
				return true;
			default:
				return false;
		}
	}
}
