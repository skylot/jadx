package jadx.gui.utils.codecache.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jadx.api.metadata.ICodeAnnotation;
import jadx.core.dex.nodes.RootNode;

public class CodeAnnotationAdapter implements DataAdapter<ICodeAnnotation> {
	private final Map<String, TypeInfo> adaptersByCls;
	private final TypeInfo[] adaptersByTag;

	public CodeAnnotationAdapter(RootNode root) {
		Map<String, DataAdapter<?>> map = registerAdapters(root);
		int size = map.size();
		adaptersByCls = new HashMap<>(size);
		adaptersByTag = new TypeInfo[size + 1];
		int tag = 1;
		for (Map.Entry<String, DataAdapter<?>> entry : map.entrySet()) {
			TypeInfo typeInfo = new TypeInfo(tag, entry.getValue());
			adaptersByCls.put(entry.getKey(), typeInfo);
			adaptersByTag[tag] = typeInfo;
			tag++;
		}
	}

	private Map<String, DataAdapter<?>> registerAdapters(RootNode root) {
		Map<String, DataAdapter<?>> map = new HashMap<>();
		MethodNodeAdapter mthAdapter = new MethodNodeAdapter(root);
		map.put("cls", new ClassNodeAdapter(root));
		map.put("fld", new FieldNodeAdapter(root));
		map.put("mth", mthAdapter);
		map.put("def", new NodeDeclareRefAdapter(this));
		map.put("var", new VarNodeAdapter(mthAdapter));
		map.put("vrf", VarRefAdapter.INSTANCE);
		map.put("off", InsnCodeOffsetAdapter.INSTANCE);
		return map;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void write(DataOutput out, ICodeAnnotation value) throws IOException {
		if (value == null) {
			out.writeByte(0);
			return;
		}
		TypeInfo typeInfo = adaptersByCls.get(value.getTagName());
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
