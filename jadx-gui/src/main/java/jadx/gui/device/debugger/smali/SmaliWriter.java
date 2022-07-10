package jadx.gui.device.debugger.smali;

import jadx.api.ICodeInfo;
import jadx.api.impl.SimpleCodeInfo;
import jadx.api.impl.SimpleCodeWriter;
import jadx.core.dex.nodes.ClassNode;

public class SmaliWriter extends SimpleCodeWriter {

	private int line = 0;
	private final ClassNode cls;

	public SmaliWriter(ClassNode cls) {
		this.cls = cls;
	}

	public ClassNode getClassNode() {
		return cls;
	}

	@Override
	protected void addLine() {
		super.addLine();
		line++;
	}

	@Override
	public int getLine() {
		return line;
	}

	@Override
	public ICodeInfo finish() {
		return new SimpleCodeInfo(buf.toString());
	}
}
