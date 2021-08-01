package jadx.plugins.input.java.data.attributes.debuginfo;

import java.util.ArrayList;
import java.util.List;

import jadx.plugins.input.java.data.ConstPoolReader;
import jadx.plugins.input.java.data.attributes.IJavaAttribute;
import jadx.plugins.input.java.data.attributes.IJavaAttributeReader;

public class LocalVarsAttr implements IJavaAttribute {
	private final List<JavaLocalVar> vars;

	public LocalVarsAttr(List<JavaLocalVar> vars) {
		this.vars = vars;
	}

	public List<JavaLocalVar> getVars() {
		return vars;
	}

	public static IJavaAttributeReader reader() {
		return (clsData, reader) -> {
			ConstPoolReader constPool = clsData.getConstPoolReader();
			int count = reader.readU2();
			List<JavaLocalVar> varsList = new ArrayList<>(count);
			for (int i = 0; i < count; i++) {
				int startOffset = reader.readU2();
				int length = reader.readU2();
				int endOffset = startOffset + length - 1;
				int nameIdx = reader.readU2();
				int typeIdx = reader.readU2();
				int varNum = reader.readU2();
				varsList.add(new JavaLocalVar(varNum,
						constPool.getUtf8(nameIdx),
						constPool.getUtf8(typeIdx),
						null,
						startOffset, endOffset));
			}
			return new LocalVarsAttr(varsList);
		};
	}
}
