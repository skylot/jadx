package jadx.api;

import java.util.List;

public interface IDecompileScheduler {
	List<List<JavaClass>> buildBatches(List<JavaClass> classes);
}
