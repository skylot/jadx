package jadx.plugins.input.java.data.attributes;

import jadx.plugins.input.java.data.DataReader;
import jadx.plugins.input.java.data.JavaClassData;

public interface IJavaAttributeReader {
	IJavaAttribute read(JavaClassData clsData, DataReader reader);
}
