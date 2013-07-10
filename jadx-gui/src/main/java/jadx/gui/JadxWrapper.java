package jadx.gui;

import jadx.api.IJadxArgs;
import jadx.api.Decompiler;
import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.core.utils.exceptions.DecodeException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JadxWrapper {
	private final Decompiler decompiler;

	public JadxWrapper(IJadxArgs jadxArgs) {
		this.decompiler = new Decompiler(jadxArgs);
	}

	public void openFile(File file) {
		try {
			this.decompiler.loadFile(file);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DecodeException e) {
			e.printStackTrace();
		}
	}

	public List<JavaClass> getClasses() {
		return decompiler.getClasses();
	}

	public List<JavaPackage> getPackages() {
		return decompiler.getPackages();
	}

}
