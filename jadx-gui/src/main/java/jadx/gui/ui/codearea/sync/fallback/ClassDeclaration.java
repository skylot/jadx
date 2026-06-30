package jadx.gui.ui.codearea.sync.fallback;

import java.util.Objects;

public class ClassDeclaration implements IDeclaration {
	private final AbstractCodeAreaLine line;
	private final String name;

	public ClassDeclaration(AbstractCodeAreaLine line) throws FallbackSyncException {
		this.name = line.extractDeclaredClassName();
		if (this.name == null) {
			throw new FallbackSyncException("line does not declare a class: " + toString());
		}
		this.line = line;
	}

	@Override
	public String getIdentifyingName() {
		return name;
	}

	@Override
	public AbstractCodeAreaLine getLine() {
		return line;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ClassDeclaration) {
			ClassDeclaration cd = (ClassDeclaration) o;
			return this.getIdentifyingName().equals(cd.getIdentifyingName());
		}
		return false;
	}

	// Not necessary but removes checkstyle warning
	@Override
	public int hashCode() {
		return Objects.hash(line, name);
	}
}
