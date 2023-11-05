package jadx.core.deobf.conditions;

import java.util.ArrayList;
import java.util.List;

import jadx.api.deobf.IDeobfCondition;
import jadx.api.deobf.IRenameCondition;
import jadx.api.deobf.impl.CombineDeobfConditions;

public class JadxRenameConditions {

	/**
	 * This method provides a mutable list of default deobfuscation conditions used by jadx.
	 * To build {@link IRenameCondition} use {@link CombineDeobfConditions#combine(List)} method.
	 */
	public static List<IDeobfCondition> buildDefaultDeobfConditions() {
		List<IDeobfCondition> list = new ArrayList<>();
		list.add(new BaseDeobfCondition());
		list.add(new DeobfWhitelist());
		list.add(new ExcludePackageWithTLDNames());
		list.add(new ExcludeAndroidRClass());
		list.add(new AvoidClsAndPkgNamesCollision());
		list.add(new DeobfLengthCondition());
		return list;
	}

	public static IRenameCondition buildDefault() {
		return CombineDeobfConditions.combine(buildDefaultDeobfConditions());
	}
}
