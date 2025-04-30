package jadx.api;

import jadx.core.xmlgen.ResContainer;

public class ResourceFileContainer extends ResourceFile {
	private final ResContainer container;

	public ResourceFileContainer(String name, ResourceType type, ResContainer container) {
		super(null, name, type);
		this.container = container;
	}

	@Override
	public ResContainer loadContent() {
		return container;
	}
}
