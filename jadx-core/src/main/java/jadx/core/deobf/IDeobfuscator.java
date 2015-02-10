package jadx.core.deobf;

import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.nodes.ClassNode;

public interface IDeobfuscator {

	public String getPackageName(String packageName);

	public String getClassShortName(ClassNode cls);
	public String getClassShortName(ClassInfo clsInfo);
	public String getClassName(ClassNode cls);
	public String getClassName(ClassInfo clsInfo);
	public String getClassFullName(ClassNode cls);
	public String getClassFullName(ClassInfo clsInfo);

	public String getClassFullPath(ClassInfo clsInfo);
}
