package jadx.core.dex.nodes;

import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.instructions.args.ArgType;

import com.android.dx.rop.code.AccessFlags;

public class ResRefField extends FieldNode {

	public ResRefField(DexNode dex, String str) {
		super(dex.root().getAppResClass(),
				FieldInfo.from(dex, dex.root().getAppResClass().getClassInfo(), str, ArgType.INT),
				AccessFlags.ACC_PUBLIC);
	}
}
