package jadx.gui.utils.codecache.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.annotations.InsnCodeOffset;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.api.metadata.annotations.VarRef;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;

public class CodeAnnotationAdapter implements DataAdapter<ICodeAnnotation> {
	private final Map<Class<?>, TypeInfo> adaptersByCls = new HashMap<>();
	private final TypeInfo[] adaptersByTag = new TypeInfo[7];

	public CodeAnnotationAdapter(RootNode root) {
		MethodNodeAdapter mthAdapter = new MethodNodeAdapter(root);
		VarRefAdapter varRefAdapter = new VarRefAdapter(mthAdapter);
		register(ClassNode.class, 1, new ClassNodeAdapter(root));
		register(FieldNode.class, 2, new FieldNodeAdapter(root));
		register(MethodNode.class, 3, mthAdapter);
		register(VarRef.class, 4, varRefAdapter);
		register(NodeDeclareRef.class, 5, new NodeDeclareRefAdapter(this));
		register(InsnCodeOffset.class, 6, InsnCodeOffsetAdapter.INSTANCE);
	}

	private <T> void register(Class<T> cls, int tag, DataAdapter<T> adapter) {
		TypeInfo typeInfo = new TypeInfo(tag, adapter);
		if (adaptersByCls.put(cls, typeInfo) != null) {
			throw new RuntimeException("Duplicate class: " + cls);
		}
		if (adaptersByTag[tag] != null) {
			throw new RuntimeException("Duplicate tag: " + tag);
		}
		adaptersByTag[tag] = typeInfo;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void write(DataOutput out, ICodeAnnotation value) throws IOException {
		if (value == null) {
			out.writeByte(0);
			return;
		}
		TypeInfo typeInfo = adaptersByCls.get(value.getClass());
		if (typeInfo == null) {
			throw new RuntimeException("Unexpected code annotation type: " + value.getClass().getSimpleName());
		}
		out.writeByte(typeInfo.getTag());
		typeInfo.getAdapter().write(out, value);
	}

	@Override
	public ICodeAnnotation read(DataInput in) throws IOException {
		int tag = in.readByte();
		if (tag == 0) {
			return null;
		}
		TypeInfo typeInfo = adaptersByTag[tag];
		if (typeInfo == null) {
			throw new RuntimeException("Unknown type tag: " + tag);
		}
		return (ICodeAnnotation) typeInfo.getAdapter().read(in);
	}

	@SuppressWarnings("rawtypes")
	private static class TypeInfo {
		private final int tag;
		private final DataAdapter adapter;

		private TypeInfo(int tag, DataAdapter adapter) {
			this.tag = tag;
			this.adapter = adapter;
		}

		public int getTag() {
			return tag;
		}

		public DataAdapter getAdapter() {
			return adapter;
		}
	}
}
