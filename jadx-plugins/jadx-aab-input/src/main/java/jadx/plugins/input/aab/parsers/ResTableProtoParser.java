package jadx.plugins.input.aab.parsers;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.android.aapt.Resources.ConfigValue;
import com.android.aapt.Resources.Entry;
import com.android.aapt.Resources.Package;
import com.android.aapt.Resources.ResourceTable;
import com.android.aapt.Resources.Type;
import com.android.aapt.Resources.Value;

import jadx.api.ICodeInfo;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.files.FileUtils;
import jadx.core.xmlgen.BinaryXMLStrings;
import jadx.core.xmlgen.IResTableParser;
import jadx.core.xmlgen.ResContainer;
import jadx.core.xmlgen.ResXmlGen;
import jadx.core.xmlgen.ResourceStorage;
import jadx.core.xmlgen.XmlGenUtils;
import jadx.core.xmlgen.entry.ProtoValue;
import jadx.core.xmlgen.entry.ResourceEntry;
import jadx.core.xmlgen.entry.ValuesParser;

public class ResTableProtoParser extends CommonProtoParser implements IResTableParser {
	private final RootNode root;
	private ResourceStorage resStorage;
	private String baseFileName = "";

	public ResTableProtoParser(RootNode root) {
		this.root = root;
	}

	@Override
	public void setBaseFileName(String fileName) {
		this.baseFileName = fileName;
	}

	@Override
	public void decode(InputStream inputStream) throws IOException {
		resStorage = new ResourceStorage(root.getArgs().getSecurity());
		ResourceTable table = ResourceTable.parseFrom(FileUtils.streamToByteArray(inputStream));
		for (Package p : table.getPackageList()) {
			parse(p);
		}
		resStorage.finish();
	}

	@Override
	public synchronized ResContainer decodeFiles() {
		ValuesParser vp = new ValuesParser(new BinaryXMLStrings(), resStorage.getResourcesNames());
		ResXmlGen resGen = new ResXmlGen(resStorage, vp, root.initManifestAttributes());
		ICodeInfo content = XmlGenUtils.makeXmlDump(root.makeCodeWriter(), resStorage);
		List<ResContainer> xmlFiles = resGen.makeResourcesXml(root.getArgs());
		return ResContainer.resourceTable(baseFileName, xmlFiles, content);
	}

	private void parse(Package p) {
		String name = p.getPackageName();
		resStorage.setAppPackage(name);
		parse(name, p.getTypeList());
	}

	private void parse(String packageName, List<Type> types) {
		for (Type type : types) {
			String typeName = type.getName();
			for (Entry entry : type.getEntryList()) {
				int id = entry.getEntryId().getId();
				String entryName = entry.getName();
				for (ConfigValue configValue : entry.getConfigValueList()) {
					String config = parse(configValue.getConfig());
					ResourceEntry resEntry = new ResourceEntry(id, packageName, typeName, entryName, config);
					resStorage.add(resEntry);

					ProtoValue protoValue;
					if (configValue.getValue().getValueCase() == Value.ValueCase.ITEM) {
						protoValue = new ProtoValue(parse(configValue.getValue().getItem()));
					} else {
						protoValue = parse(configValue.getValue().getCompoundValue());
					}
					resEntry.setProtoValue(protoValue);
				}
			}
		}
	}

	@Override
	public ResourceStorage getResStorage() {
		return resStorage;
	}

	@Override
	public BinaryXMLStrings getStrings() {
		return new BinaryXMLStrings();
	}
}
