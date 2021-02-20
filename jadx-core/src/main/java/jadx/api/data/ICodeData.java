package jadx.api.data;

import java.util.List;

public interface ICodeData {

	long getUpdateId();

	List<ICodeComment> getComments();
}
