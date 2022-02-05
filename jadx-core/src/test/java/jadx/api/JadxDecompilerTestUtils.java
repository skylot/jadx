package jadx.api;

import jadx.core.dex.nodes.RootNode;
import jadx.core.xmlgen.BinaryXMLParser;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JadxDecompilerTestUtils {
	public static JadxDecompiler getMockDecompiler() {
		JadxDecompiler decompiler = mock(JadxDecompiler.class);
		RootNode rootNode = new RootNode(new JadxArgs());
		when(decompiler.getRoot()).thenReturn(rootNode);
		when(decompiler.getBinaryXmlParser()).thenReturn(new BinaryXMLParser(rootNode));
		return decompiler;
	}
}
