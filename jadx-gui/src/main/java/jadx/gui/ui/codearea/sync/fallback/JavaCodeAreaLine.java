package jadx.gui.ui.codearea.sync.fallback;

import javax.swing.text.BadLocationException;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.codearea.CodeArea;

public class JavaCodeAreaLine extends AbstractCodeAreaLine {
	private static final Logger LOG = LoggerFactory.getLogger(JavaCodeAreaLine.class);

	public JavaCodeAreaLine(CodeArea area, int lineIndex) throws BadLocationException {
		super(area, lineIndex);
	}

	@Override
	public AbstractCodeAreaLine getLineAt(int lineIndex) throws BadLocationException {
		return new JavaCodeAreaLine((CodeArea) getArea(), lineIndex);
	}

	@Override
	public boolean isClassDeclaration() {
		return getTrimmedStr().matches(".*\\b(class|interface|enum)\\b.*\\{");
	}

	@Override
	public boolean isMethodOrConstructorDeclaration() {
		String l = getTrimmedStr();
		// Skip control-flow constructs (to avoid matching 'if', 'for', etc.)
		// WARNING - we are relying on the code gen format output of jadx here and that it is trimmed.
		// it also assumes that jadx will never output two statements on the same line separated by ';'
		if (l.startsWith("if ")
				|| l.startsWith("for ")
				|| l.startsWith("while ")
				|| l.startsWith("switch ")
				|| l.startsWith("case ")
				|| l.startsWith("break ")
				|| l.startsWith("default ")
				|| l.startsWith("} else if ")
				|| l.startsWith("} else ")
				|| l.startsWith("try ")
				|| l.startsWith("} catch ")
				|| l.startsWith("} finally ")
				|| l.startsWith("throw ")
				|| l.startsWith("do ")
				|| l.startsWith("synchronized ")) {
			return false;
		}
		boolean hasParens = l.contains("(") && l.contains(")");
		boolean isDefined = l.endsWith("{");
		boolean isAbstract = l.contains("abstract") && l.endsWith(";");
		return hasParens && (isDefined || isAbstract);
	}

	@Override
	public boolean isFieldDeclaration() {
		try {
			IDeclaration enclosingDeclaration = getEnclosingScopeDeclaration();
			if (!(enclosingDeclaration instanceof ClassDeclaration)) {
				return false;
			}
			String line = getTrimmedStr();
			// This may also include fields which are anonymous classes or lambdas
			return line.endsWith(";") || line.contains(" = ");
		} catch (Exception ex) {
			LOG.error("{} - Unable to determine if line is a field declaration", LOG.getName(), ex);
		}
		return false;
	}

	@Override
	public final @Nullable String extractDeclaredClassName() {
		if (!isClassDeclaration()) {
			return null;
		}
		String[] tokens = getTrimmedStr().split("\\s+");
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].equals("class") || tokens[i].equals("interface") || tokens[i].equals("enum")) {
				if (i + 1 < tokens.length) {
					return tokens[i + 1];
				}
			}
		}
		return null;
	}

	@Override
	public @Nullable String extractDeclaredMethodName() {
		if (!isMethodOrConstructorDeclaration()) {
			return null;
		}
		int paren = getTrimmedStr().indexOf('(');
		String before = getTrimmedStr().substring(0, paren).trim();
		String[] parts = before.split("\\s+");
		return parts[parts.length - 1]; // last token
	}

	@Override
	protected MethodDeclaration createMethodDeclaration() throws FallbackSyncException {
		return MethodDeclaration.create(this);
	}
}
