package jadx.zip;

import java.io.IOException;

public interface IZipParser {

	ZipContent open() throws IOException;

	void close() throws IOException;
}
