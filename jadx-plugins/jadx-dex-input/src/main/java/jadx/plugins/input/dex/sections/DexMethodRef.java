package jadx.plugins.input.dex.sections;

import java.util.List;

import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.utils.Utils;
import jadx.plugins.input.dex.DexReader;

public class DexMethodRef implements IMethodRef {

	private int uniqId;
	private String name;
	private String parentClassType;
	private String returnType;
	private List<String> argTypes;

	// lazy loading info
	private int dexIdx;
	private SectionReader sectionReader;

	public void initUniqId(DexReader dexReader, int idx) {
		this.uniqId = (dexReader.getUniqId() & 0xFFFF) << 16 | (idx & 0xFFFF);
	}

	@Override
	public void load() {
		if (sectionReader != null) {
			sectionReader.loadMethodRef(this, dexIdx);
			sectionReader = null;
		}
	}

	public void setDexIdx(int dexIdx) {
		this.dexIdx = dexIdx;
	}

	public void setSectionReader(SectionReader sectionReader) {
		this.sectionReader = sectionReader;
	}

	@Override
	public int getUniqId() {
		return uniqId;
	}

	public void reset() {
		name = null;
		parentClassType = null;
		returnType = null;
		argTypes = null;
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

	@Override
	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

	@Override
	public List<String> getArgTypes() {
		return argTypes;
	}

	public void setArgTypes(List<String> argTypes) {
		this.argTypes = argTypes;
	}

	@Override
	public String toString() {
		if (name == null) {
			// not loaded
			return Integer.toHexString(uniqId);
		}
		return getParentClassType() + "->" + getName() + '(' + Utils.listToStr(getArgTypes()) + ")" + getReturnType();
	}
}
