package jadx.core.deobf;

import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;

public class StubDeobfuscator implements IDeobfuscator {

	@Override
	public String getPackageName(String packageName) {
		return packageName;
	}

	@Override
	public String getClassShortName(ClassNode cls) {
		return cls.getShortName();
	}

	@Override
	public String getClassShortName(ClassInfo clsInfo) {
		return clsInfo.getShortName();
	}

	@Override
	public String getClassName(ClassNode cls) {
		return cls.getClassInfo().getNameWithoutPackage();
	}

	@Override
	public String getClassName(ClassInfo clsInfo) {
		return clsInfo.getNameWithoutPackage();
	}

	@Override
	public String getClassFullName(ClassNode cls) {
		return cls.getFullName();
	}

	@Override
	public String getClassFullName(ClassInfo clsInfo) {
		return clsInfo.getFullName();
	}

	@Override
	public String getClassFullPath(ClassInfo clsInfo) {
		return clsInfo.getFullPath();
	}

}
