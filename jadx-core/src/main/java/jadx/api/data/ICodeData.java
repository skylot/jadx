package jadx.api.data;

import java.util.List;

public interface ICodeData {

	List<ICodeComment> getComments();

	List<ICodeRename> getRenames();

	boolean isEmpty();
}
