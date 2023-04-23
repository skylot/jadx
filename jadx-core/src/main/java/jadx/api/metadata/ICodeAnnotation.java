package jadx.api.metadata;

public interface ICodeAnnotation {

	enum AnnType {
		CLASS,
		FIELD,
		METHOD,
		PKG,
		VAR,
		VAR_REF,
		DECLARATION,
		OFFSET,
		END // class or method body end
	}

	AnnType getAnnType();
}
