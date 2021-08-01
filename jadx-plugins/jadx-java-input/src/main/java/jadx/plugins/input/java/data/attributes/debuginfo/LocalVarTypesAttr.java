package jadx.plugins.input.java.data.attributes.debuginfo;

import java.util.ArrayList;
import java.util.List;

import jadx.plugins.input.java.data.ConstPoolReader;
import jadx.plugins.input.java.data.attributes.IJavaAttribute;
import jadx.plugins.input.java.data.attributes.IJavaAttributeReader;

public class LocalVarTypesAttr implements IJavaAttribute {
	private final List<JavaLocalVar> vars;

	public LocalVarTypesAttr(List<JavaLocalVar> vars) {
		this.vars = vars;
	}

	public List<JavaLocalVar> getVars() {
		return vars;
	}

	public static IJavaAttributeReader reader() {
		return (clsData, reader) -> {
			ConstPoolReader constPool = clsData.getConstPoolReader();
			int len = reader.readU2();
			List<JavaLocalVar> varsList = new ArrayList<>(len);
			for (int i = 0; i < len; i++) {
				int startOffset = reader.readU2();
				int endOffset = startOffset + reader.readU2() - 1;
				int nameIdx = reader.readU2();
				int typeIdx = reader.readU2();
				int varNum = reader.readU2();
				varsList.add(new JavaLocalVar(
						varNum,
						constPool.getUtf8(nameIdx),
						null,
						constPool.getUtf8(typeIdx),
						startOffset, endOffset));
			}
			return new LocalVarTypesAttr(varsList);
		};
	}
}
