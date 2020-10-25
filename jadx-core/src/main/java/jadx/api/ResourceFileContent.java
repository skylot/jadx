package jadx.api;

import jadx.core.xmlgen.ResContainer;

public class ResourceFileContent extends ResourceFile {
	private final ICodeInfo content;

	public ResourceFileContent(String name, ResourceType type, ICodeInfo content) {
		super(null, name, type);
		this.content = content;
	}

	@Override
	public ResContainer loadContent() {
		return ResContainer.textResource(getDeobfName(), content);
	}
}
