package jadx.gui.cache.code.disk.adapters;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeAnnotation.AnnType;
import jadx.core.dex.nodes.RootNode;

public class CodeAnnotationAdapter implements DataAdapter<ICodeAnnotation> {
	private final Map<AnnType, TypeInfo> adaptersByCls;
	private final TypeInfo[] adaptersByTag;

	public CodeAnnotationAdapter(RootNode root) {
		Map<AnnType, DataAdapter<?>> map = registerAdapters(root);
		int size = map.size();
		adaptersByCls = new EnumMap<>(AnnType.class);
		adaptersByTag = new TypeInfo[size + 1];
		int tag = 1;
		for (Map.Entry<AnnType, DataAdapter<?>> entry : map.entrySet()) {
			TypeInfo typeInfo = new TypeInfo(tag, entry.getValue());
			adaptersByCls.put(entry.getKey(), typeInfo);
			adaptersByTag[tag] = typeInfo;
			tag++;
		}
	}

	private Map<AnnType, DataAdapter<?>> registerAdapters(RootNode root) {
		Map<AnnType, DataAdapter<?>> map = new EnumMap<>(AnnType.class);
		MethodNodeAdapter mthAdapter = new MethodNodeAdapter(root);
		map.put(AnnType.CLASS, new ClassNodeAdapter(root));
		map.put(AnnType.FIELD, new FieldNodeAdapter(root));
		map.put(AnnType.METHOD, mthAdapter);
		map.put(AnnType.DECLARATION, new NodeDeclareRefAdapter(this));
		map.put(AnnType.VAR, new VarNodeAdapter(mthAdapter));
		map.put(AnnType.VAR_REF, VarRefAdapter.INSTANCE);
		map.put(AnnType.OFFSET, InsnCodeOffsetAdapter.INSTANCE);
		map.put(AnnType.END, new NodeEndAdapter());
		return map;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void write(DataOutput out, ICodeAnnotation value) throws IOException {
		if (value == null) {
			out.writeByte(0);
			return;
		}
		TypeInfo typeInfo = adaptersByCls.get(value.getAnnType());
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
