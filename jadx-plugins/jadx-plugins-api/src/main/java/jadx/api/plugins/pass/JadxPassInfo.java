package jadx.api.plugins.pass;

import java.util.List;

public interface JadxPassInfo {

	String getName();

	String getDescription();

	List<String> runAfter();

	List<String> runBefore();
}
