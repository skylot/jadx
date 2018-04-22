package jadx.core.dex.nodes.parser;

import java.util.List;

import com.android.dex.Dex.Section;
import com.android.dex.Leb128;

import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.utils.exceptions.DecodeException;

public class StaticValuesParser extends EncValueParser {

	public StaticValuesParser(DexNode dex, Section in) {
		super(dex, in);
	}

	public int processFields(List<FieldNode> fields) throws DecodeException {
		int count = Leb128.readUnsignedLeb128(in);
		for (int i = 0; i < count; i++) {
			Object value = parseValue();
			if (i < fields.size()) {
				fields.get(i).addAttr(FieldInitAttr.constValue(value));
			}
		}
		return count;
	}
}
