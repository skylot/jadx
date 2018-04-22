package jadx.core.dex.nodes.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.android.dex.Dex.Section;

import jadx.core.dex.attributes.annotations.Annotation;
import jadx.core.dex.attributes.annotations.Annotation.Visibility;
import jadx.core.dex.attributes.annotations.AnnotationsList;
import jadx.core.dex.attributes.annotations.MethodParameters;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.DecodeException;

public class AnnotationsParser {

	private static final Visibility[] VISIBILITIES = {
			Visibility.BUILD,
			Visibility.RUNTIME,
			Visibility.SYSTEM
	};

	private final DexNode dex;
	private final ClassNode cls;

	public AnnotationsParser(ClassNode cls) {
		this.cls = cls;
		this.dex = cls.dex();
	}

	public void parse(int offset) throws DecodeException {
		Section section = dex.openSection(offset);

		// TODO read as unsigned int
		int classAnnotationsOffset = section.readInt();
		int fieldsCount = section.readInt();
		int annotatedMethodsCount = section.readInt();
		int annotatedParametersCount = section.readInt();

		if (classAnnotationsOffset != 0) {
			cls.addAttr(readAnnotationSet(classAnnotationsOffset));
		}

		for (int i = 0; i < fieldsCount; i++) {
			FieldNode f = cls.searchFieldById(section.readInt());
			f.addAttr(readAnnotationSet(section.readInt()));
		}

		for (int i = 0; i < annotatedMethodsCount; i++) {
			MethodNode m = cls.searchMethodById(section.readInt());
			m.addAttr(readAnnotationSet(section.readInt()));
		}

		for (int i = 0; i < annotatedParametersCount; i++) {
			MethodNode mth = cls.searchMethodById(section.readInt());
			// read annotation ref list
			Section ss = dex.openSection(section.readInt());
			int size = ss.readInt();
			MethodParameters params = new MethodParameters(size);
			for (int j = 0; j < size; j++) {
				params.getParamList().add(readAnnotationSet(ss.readInt()));
			}
			mth.addAttr(params);
		}
	}

	private AnnotationsList readAnnotationSet(int offset) throws DecodeException {
		if (offset == 0) {
			return AnnotationsList.EMPTY;
		}
		Section section = dex.openSection(offset);
		int size = section.readInt();
		if (size == 0) {
			return AnnotationsList.EMPTY;
		}
		List<Annotation> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			Section anSection = dex.openSection(section.readInt());
			Annotation a = readAnnotation(dex, anSection, true);
			list.add(a);
		}
		return new AnnotationsList(list);
	}

	public static Annotation readAnnotation(DexNode dex, Section s, boolean readVisibility) throws DecodeException {
		EncValueParser parser = new EncValueParser(dex, s);
		Visibility visibility = null;
		if (readVisibility) {
			byte v = s.readByte();
			visibility = VISIBILITIES[v];
		}
		int typeIndex = s.readUleb128();
		int size = s.readUleb128();
		Map<String, Object> values = new LinkedHashMap<>(size);
		for (int i = 0; i < size; i++) {
			String name = dex.getString(s.readUleb128());
			values.put(name, parser.parseValue());
		}
		ArgType type = dex.getType(typeIndex);
		Annotation annotation = new Annotation(visibility, type, values);
		if (!type.isObject()) {
			throw new DecodeException("Incorrect type for annotation: " + annotation);
		}
		return annotation;
	}
}
