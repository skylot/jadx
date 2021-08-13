package jadx.plugins.input.java.data;

import jadx.api.plugins.input.data.IMethodRef;
import jadx.plugins.input.java.JavaClassReader;
import jadx.plugins.input.java.utils.DescriptorParser;

public class JavaMethodRef extends JavaMethodProto implements IMethodRef {

	private int uniqId;
	private String parentClassType;
	private String name;
	private String descr;

	@Override
	public int getUniqId() {
		return uniqId;
	}

	public void initUniqId(JavaClassReader clsReader, int id, boolean fromConstPool) {
		int readerId = clsReader.getId();
		if (readerId > 0xFFFF || id > 0x7FFF) {
			// loaded more than 65535 classes or more than 32767 methods in this class -> disable caching
			this.uniqId = 0;
		} else {
			int source = fromConstPool ? 0 : 0x8000;
			this.uniqId = (readerId & 0xFFFF) << 16 | source | id & 0x7FFF;
		}
	}

	@Override
	public String getParentClassType() {
		return parentClassType;
	}

	public void setParentClassType(String parentClassType) {
		this.parentClassType = parentClassType;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescriptor() {
		return descr;
	}

	public void setDescr(String descr) {
		this.descr = descr;
	}

	public void reset() {
		this.setReturnType(null);
		this.setArgTypes(null);
	}

	@Override
	public void load() {
		if (getReturnType() == null) {
			DescriptorParser.fillMethodProto(descr, this);
		}
	}

	@Override
	public String toString() {
		return parentClassType + "->" + name + descr;
	}
}
