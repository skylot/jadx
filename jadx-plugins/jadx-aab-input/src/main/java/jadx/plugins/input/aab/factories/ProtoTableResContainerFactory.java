package jadx.plugins.input.aab.factories;

import java.io.IOException;
import java.io.InputStream;

import org.jetbrains.annotations.Nullable;

import jadx.api.ResourceFile;
import jadx.api.ResourceType;
import jadx.api.plugins.resources.IResContainerFactory;
import jadx.api.plugins.resources.IResTableParserProvider;
import jadx.core.xmlgen.IResTableParser;
import jadx.core.xmlgen.ResContainer;

public class ProtoTableResContainerFactory implements IResContainerFactory {
	private final IResTableParserProvider provider;

	public ProtoTableResContainerFactory(IResTableParserProvider provider) {
		this.provider = provider;
	}

	@Override
	public @Nullable ResContainer create(ResourceFile resFile, InputStream inputStream) throws IOException {
		if (!resFile.getOriginalName().endsWith(".pb") || resFile.getType() != ResourceType.ARSC) {
			return null;
		}
		IResTableParser parser = provider.getParser(resFile);
		if (parser == null) {
			return null;
		}
		return parser.decodeFiles();
	}
}
