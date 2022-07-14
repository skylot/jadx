package jadx.core.xmlgen;

import java.io.IOException;
import java.io.InputStream;

public interface IResParser {

	void decode(InputStream inputStream) throws IOException;

	ResourceStorage getResStorage();

	String[] getStrings();
}
