package jadx.core.dex.nodes.parser;

import jadx.core.dex.attributes.annotations.Annotation;
import jadx.core.dex.attributes.annotations.Annotation.Visibility;
import jadx.core.dex.attributes.annotations.AnnotationsList;
import jadx.core.dex.attributes.annotations.MethodParameters;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.DecodeException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.android.dx.io.DexBuffer.Section;

public class AnnotationsParser {

	private final DexNode dex;

	public AnnotationsParser(ClassNode cls, int offset) throws DecodeException {
		this.dex = cls.dex();
		Section section = dex.openSection(offset);

		// TODO read as unsigned int
		int classAnnotationsOffset = section.readInt();
		int fieldsCount = section.readInt();
		int annotatedMethodsCount = section.readInt();
		int annotatedParametersCount = section.readInt();

		if (classAnnotationsOffset != 0) {
			cls.getAttributes().add(readAnnotationSet(classAnnotationsOffset));
		}

		for (int i = 0; i < fieldsCount; i++) {
			FieldNode f = cls.searchFieldById(section.readInt());
			f.getAttributes().add(readAnnotationSet(section.readInt()));
		}

		for (int i = 0; i < annotatedMethodsCount; i++) {
			MethodNode m = cls.searchMethodById(section.readInt());
			m.getAttributes().add(readAnnotationSet(section.readInt()));
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
			mth.getAttributes().add(params);
		}
	}

	private AnnotationsList readAnnotationSet(int offset) throws DecodeException {
		Section section = dex.openSection(offset);
		int size = section.readInt();
		List<Annotation> list = new ArrayList<Annotation>(size);
		for (int i = 0; i < size; i++) {
			Section anSection = dex.openSection(section.readInt());
			Annotation a = readAnnotation(dex, anSection, true);
			list.add(a);
		}
		return new AnnotationsList(list);
	}

	private static final Annotation.Visibility[] VISIBILITIES = new Annotation.Visibility[]{
			Annotation.Visibility.BUILD,
			Annotation.Visibility.RUNTIME,
			Annotation.Visibility.SYSTEM
	};

	public static Annotation readAnnotation(DexNode dex, Section s, boolean readVisibility) throws DecodeException {
		EncValueParser parser = new EncValueParser(dex, s);
		Visibility visibility = null;
		if (readVisibility) {
			visibility = VISIBILITIES[s.readByte()];
		}
		int typeIndex = s.readUleb128();
		int size = s.readUleb128();
		Map<String, Object> values = new LinkedHashMap<String, Object>(size);
		for (int i = 0; i < size; i++) {
			String name = dex.getString(s.readUleb128());
			values.put(name, parser.parseValue());
		}
		return new Annotation(visibility, dex.getType(typeIndex), values);
	}
}
