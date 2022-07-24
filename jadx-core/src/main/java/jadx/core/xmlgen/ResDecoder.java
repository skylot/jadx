package jadx.core.xmlgen;

import java.io.IOException;
import java.io.InputStream;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class ResDecoder {

	public static IResParser decode(RootNode root, ResourceFile resFile, InputStream is) throws IOException {
		if (resFile.getType() != ResourceType.ARSC) {
			throw new IllegalArgumentException("Unexpected resource type for decode: " + resFile.getType() + ", expect ARSC");
		}
		IResParser parser = null;
		String fileName = resFile.getOriginalName();
		if (fileName.endsWith(".arsc")) {
			parser = new ResTableParser(root);
		}
		if (fileName.endsWith(".pb")) {
			parser = new ResProtoParser(root);
		}
		if (parser == null) {
			throw new JadxRuntimeException("Unknown type of resource file: " + fileName);
		}
		parser.decode(is);
		return parser;
	}
}
