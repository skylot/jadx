package jadx.zip;

import java.io.Closeable;
import java.io.IOException;

public interface IZipParser extends Closeable {

	ZipContent open() throws IOException;
}
