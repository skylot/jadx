package jadx.api;

import jadx.core.codegen.CodeWriter;
import jadx.core.utils.files.ZipSecurity;
import jadx.core.xmlgen.ResContainer;

public class ResourceFileContent extends ResourceFile {

	private final CodeWriter content;

	private ResourceFileContent(String name, ResourceType type, CodeWriter content) {
		super(null, name, type);
		this.content = content;
	}

	@Override
	public ResContainer loadContent() {
		return ResContainer.singleFile(getName(), content);
	}
	
	public static ResourceFileContent createResourceFileContentInstance(String name, ResourceType type, CodeWriter content) {
		if(!ZipSecurity.isValidZipEntryName(name)) {
			return null;
		}
		return new ResourceFileContent(name, type, content);
	}
}
