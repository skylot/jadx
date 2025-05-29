package jadx.core.xmlgen;

import java.io.IOException;
import java.io.InputStream;

public interface IResTableParser {

	void decode(InputStream inputStream) throws IOException;

	ResContainer decodeFiles();

	ResourceStorage getResStorage();

	BinaryXMLStrings getStrings();

	default void setBaseFileName(String fileName) {
		// optional
	}
}
