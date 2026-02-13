package jadx.gui.ui.codearea.sync.fallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jadx.core.utils.Utils;

class MethodDeclaration implements IDeclaration {
	private final AbstractCodeAreaLine line;
	private final Type returnType;
	private final List<Type> argTypes;
	private final String name;

	boolean isStatic;

	public static MethodDeclaration create(JavaCodeAreaLine line) throws FallbackSyncException {
		String methodName = line.extractDeclaredMethodName();
		if (methodName == null) {
			throw new FallbackSyncException("no method name found in java declaration");
		}

		// Get the return string
		String trimmed = line.getTrimmedStr();
		int methodNameStartPos = trimmed.indexOf(methodName);
		// -2 to jump to last char of return type
		// +1 to get to first char of return type
		int returnTypeStartPos = trimmed.lastIndexOf(' ', methodNameStartPos - 2) + 1;
		returnTypeStartPos = returnTypeStartPos > -1 ? returnTypeStartPos : 0;
		String returnStr = trimmed.substring(returnTypeStartPos, methodNameStartPos - 1);

		// Get the arg types
		String argString = trimmed.substring(trimmed.indexOf('(') + 1, trimmed.indexOf(')'));
		String[] argStringParts = argString.split(", ");
		List<String> argTypeStrings = new ArrayList<>();
		for (int i = 0; i < argStringParts.length; i++) {
			String part = argStringParts[i];
			if (part.isEmpty()) {
				break;
			}
			argTypeStrings.add(part.substring(0, part.indexOf(" ")));
		}

		boolean isStatic = trimmed.contains("static ");

		List<Type> argTypes = argTypeStrings.stream().map(s -> Type.fromJavaName(s)).collect(Collectors.toList());
		return new MethodDeclaration(line, Type.fromJavaName(returnStr), argTypes, isStatic, methodName);
	}

	public static MethodDeclaration create(SmaliAreaLine line) throws FallbackSyncException {
		String methodName = line.extractDeclaredMethodName();
		if (methodName == null) {
			throw new FallbackSyncException("no method name found in smali declaration");
		}

		// Get the return string
		String trimmed = line.getTrimmedStr();
		String returnStr = trimmed.substring(trimmed.indexOf(')') + 1);
		returnStr = returnStr.endsWith(";") ? returnStr.substring(0, returnStr.length() - 1) : returnStr;

		boolean isStatic = trimmed.contains("static ");

		return new MethodDeclaration(line, Type.fromSmaliName(returnStr), parseSmaliArgs(trimmed), isStatic, methodName);
	}

	private MethodDeclaration(AbstractCodeAreaLine line, Type returnType, List<Type> argTypes, boolean isStatic, String name) {
		this.line = line;
		this.returnType = returnType;
		this.argTypes = argTypes;
		this.isStatic = isStatic;
		this.name = name;
	}

	@Override
	public String getIdentifyingName() {
		return name;
	}

	@Override
	public AbstractCodeAreaLine getLine() {
		return line;
	}

	private static List<Type> parseSmaliArgs(String lineStr) {
		List<String> argTypeStrings = new ArrayList<>();
		String argString = lineStr.substring(lineStr.indexOf('(') + 1, lineStr.indexOf(')'));
		for (int i = 0; i < argString.length();) {
			char c = argString.charAt(i);
			if (c == 'L') {
				int j = i;
				for (; j < argString.length(); ++j) {
					if (argString.charAt(j) == ';') {
						argTypeStrings.add(argString.substring(i, j + 1));
						break;
					}
				}
				i = j + 1;
			} else if (c == '[') {
				argTypeStrings.add(argString.substring(i, i + 2));
				i += 2;
			} else if (c != ' ') {
				argTypeStrings.add(argString.substring(i, i + 1));
				++i;
			} else {
				++i;
			}
		}
		return argTypeStrings.stream().map(s -> Type.fromSmaliName(s)).collect(Collectors.toList());
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof MethodDeclaration) {
			MethodDeclaration decl = (MethodDeclaration) o;
			if (!decl.name.equals(this.name)) {
				return false;
			}
			if (decl.isStatic != this.isStatic) {
				return false;
			}
			if (!decl.returnType.equals(this.returnType)) {
				return false;
			}
			if (decl.argTypes.size() != this.argTypes.size()) {
				return false;
			}
			for (int i = 0; i < decl.argTypes.size(); ++i) {
				if (!decl.argTypes.get(i).equals(this.argTypes.get(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	// Not necessary but removes checkstyle warning
	@Override
	public int hashCode() {
		return Objects.hash(name, isStatic, returnType, argTypes);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("NAME=").append(name).append("+++")
				.append("RETURN=").append(returnType).append("+++")
				.append("ARGS=");
		for (final var a : argTypes) {
			sb.append(a).append(",");
		}
		return sb.toString();
	}

	private static class Type {
		private String smaliName;
		private String javaName;

		public static Type fromJavaName(String name) {
			return new Type(Utils.javaNameToSmaliName(name), name);
		}

		public static Type fromSmaliName(String name) {
			return new Type(name, Utils.smaliNameToJavaName(name));
		}

		private Type(String smaliName, String javaName) {
			this.smaliName = smaliName;
			this.javaName = javaName;
		}

		private boolean isNonPrimitive() {
			return smaliName.startsWith("L");
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Type) {
				Type t = (Type) o;
				if (t.isNonPrimitive() || this.isNonPrimitive()) {
					// One of them might be missing the package prefix
					return t.javaName.endsWith(this.javaName)
							|| this.javaName.endsWith(t.javaName);
				}
				return t.javaName.equals(this.javaName)
						|| t.smaliName.equals(this.smaliName);
				// Slightly less strict - should think about this more
				// && t.smaliName.equals(this.smaliName);
			}
			return false;
		}

		// Not necessary but removes checkstyle warning
		@Override
		public int hashCode() {
			return Objects.hash(this, javaName, smaliName);
		}

		@Override
		public String toString() {
			return "@" + smaliName + "-OR-" + javaName + "@";
		}
	}
}
