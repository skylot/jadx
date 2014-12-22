package jadx.core.xmlgen;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.PrintWriter;

import java.lang.reflect.Field;

import java.util.HashMap;
import java.util.Map;

import android.R.style;

/* TODO:
	Don't die when error occurs
	Check error cases, maybe checked const values are not always the same
	Better error messages
	What to do, when Binary XML Manifest is > size(int)?
	Check for missung chunk size types
	Implement missing data types
	Use linenumbers to recreate EXACT AndroidManifest
	Check Element chunk size
*/

public class BinaryXMLParser {
	private byte[] bytes;
	private String[] strings;
	private int count;
	private String nsPrefix="ERROR";
	private String nsURI="ERROR";
	private String currentTag="ERROR";
	private int numtabs=-1;
	private boolean wasOneLiner=false;
	private PrintWriter writer;
	private Map<Integer, String> styleMap = null;

	public BinaryXMLParser(String xmlfilepath, String xmloutfilepath) {
		try {
			writer = new PrintWriter(xmloutfilepath,"UTF-8");
		} catch(FileNotFoundException fnfe) { die("FNFE"); }
		catch(UnsupportedEncodingException uee) { die("UEE"); }
		if(null==writer) die("null==writer");
		writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		File manifest = new File(xmlfilepath);
		if(null==manifest) die("null==manifest");
		bytes = new byte[(int) manifest.length()];
		try {
			InputStream is = null;
			try {
				is = new BufferedInputStream(new FileInputStream(manifest));
				int total = 0;
				while(total < bytes.length) {
					int remain = bytes.length - total;
					int read = is.read(bytes, total, remain);
					if(read > 0) total += read;
				}
			} finally {
				is.close();
			}
		} catch(FileNotFoundException fnfe) { die("FILE NOT FOUND"); }
		catch(IOException ioe) { die("IOE"); }
		count=0;
		styleMap = new HashMap<Integer, String>();
		if(null==styleMap) die("null==styleMap");
		for(Field f : android.R.style.class.getFields()) {
			try {
				styleMap.put(f.getInt(f.getType()),f.getName());
			} catch(IllegalAccessException iae) {
				die("IAE");
			}
		}
	}

	public BinaryXMLParser(byte[] xmlfilebytes, String xmloutfilepath) {
		System.out.println("XMLOUTFILEPATH: " + xmloutfilepath);
		try {
			writer = new PrintWriter(xmloutfilepath,"UTF-8");
		} catch(FileNotFoundException fnfe) { die("FNFE"); }
		catch(UnsupportedEncodingException uee) { die("UEE"); }
		if(null==writer) die("null==writer");
		writer.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		bytes = xmlfilebytes;
		count=0;
		styleMap = new HashMap<Integer, String>();
		if(null==styleMap) die("null==styleMap");
		for(Field f : android.R.style.class.getFields()) {
			try {
				styleMap.put(f.getInt(f.getType()),f.getName());
			} catch(IllegalAccessException iae) {
				die("IAE");
			}
		}
	}

	public void parse() {
		if(cInt16(bytes, count) != 0x0003) die("Version is not 3");
		if(cInt16(bytes, count) != 0x0008) die("Size of header is not 8");
		if(cInt32(bytes, count) != bytes.length) die("Size of manifest doesn't match");
		while((count+2)<=bytes.length) {
			int type = cInt16(bytes, count);
			if(type==0x0001) parseStringPool();
			else if(type==0x0180) parseResourceMap();
			else if(type==0x0100) parseNameSpace();
			else if(type==0x0101) parseNameSpaceEnd();
			else if(type==0x0102) parseElement();
			else if(type==0x0103) parseElementEnd();
			else if(type==0x0000) continue; // NullType is just doing nothing
			else die("Type: " + Integer.toHexString(type) + " not yet implemented");
		}
		writer.close();
	}

	private void parseStringPool() {
		if(cInt16(bytes, count) != 0x001c) die("Header header size not 28");
		int hsize = cInt32(bytes, count);
		int stringCount = cInt32(bytes, count);
		int styleCount = cInt32(bytes, count);
		int flags = cInt32(bytes, count);
		int stringsStart = cInt32(bytes, count);
		int stylesStart = cInt32(bytes, count);
		int[] stringsOffsets = new int[stringCount];
		for(int i=0; i<stringCount; i++) {
			stringsOffsets[i] = cInt32(bytes, count);
		}
		strings = new String[stringCount];
		for(int i=0; i<stringCount; i++) {
			int off = 8 + stringsStart + stringsOffsets[i];
			int strlen = cInt16(bytes, off);
			byte[] str = new byte[strlen*2];
			System.arraycopy(bytes, count, str, 0, strlen*2);
			count+=strlen*2;
			strings[i] = new String(str, Charset.forName("UTF-16LE"));
			count+=2;
		}
	}

	private void parseResourceMap() {
		if(cInt16(bytes, count) != 0x8) die("Header size of resmap is not 8!");
		int rhsize = cInt32(bytes, count);
		int[] ids = new int[(rhsize-8)/4];
		for(int i=0; i<ids.length; i++) {
			ids[i]=cInt32(bytes, count);
		}
	}

	private void parseNameSpace() {
		if(cInt16(bytes, count) != 0x0010) die("NAMESPACE header is not 0x0010");
		if(cInt32(bytes, count) != 0x18) die("NAMESPACE header chunk is not 0x18 big");
		int beginLineNumber = cInt32(bytes, count);
		int comment = cInt32(bytes, count);
		int beginPrefix = cInt32(bytes, count);
		nsPrefix = strings[beginPrefix];
		int beginURI = cInt32(bytes, count);
		nsURI=strings[beginURI];
	}

	private void parseNameSpaceEnd() {
		if(cInt16(bytes, count) != 0x0010) die("NAMESPACE header is not 0x0010");
		if(cInt32(bytes, count) != 0x18) die("NAMESPACE header chunk is not 0x18 big");
		int endLineNumber = cInt32(bytes, count);
		int comment = cInt32(bytes, count);
		int endPrefix = cInt32(bytes, count);
		nsPrefix = strings[endPrefix];
		int endURI = cInt32(bytes, count);
		nsURI=strings[endURI];
	}

	private void parseElement() {
		numtabs+=1;
		if(cInt16(bytes, count) != 0x0010) die("ELEMENT HEADER SIZE is not 0x10");
		count+=4; // TODO: Check element chunk size
		int elementBegLineNumber = cInt32(bytes, count);
		int comment = cInt32(bytes, count);
		int startNS = cInt32(bytes, count);
		int startNSName = cInt32(bytes, count); // actually is elementName...
		if(!wasOneLiner && !"ERROR".equals(currentTag) && !currentTag.equals(strings[startNSName])) {
			writer.println(">");
		}
		wasOneLiner=false;
		currentTag=strings[startNSName];
		for(int i=0; i<numtabs; i++) writer.print("\t");
		writer.print("<" + strings[startNSName]);
		int attributeStart = cInt16(bytes, count);
		if(attributeStart != 0x14) die("startNS's attributeStart is not 0x14");
		int attributeSize = cInt16(bytes, count);
		if(attributeSize != 0x14) die("startNS's attributeSize is not 0x14");
		int attributeCount = cInt16(bytes, count); 
		int idIndex = cInt16(bytes, count);
		int classIndex = cInt16(bytes, count);
		int styleIndex = cInt16(bytes, count);
		if("manifest".equals(strings[startNSName])) writer.print(" xmlns:\""+nsURI+"\"");
		if(attributeCount>0) writer.print(" ");
		for(int i=0; i<attributeCount; i++) {
			int attributeNS = cInt32(bytes, count);
			int attributeName = cInt32(bytes, count);
			int attributeRawValue = cInt32(bytes, count);
			int attrValSize = cInt16(bytes, count);
			if(attrValSize != 0x08) die("attrValSize != 0x08 not supported");
			if(cInt8(bytes, count) != 0) die("res0 is not 0");
			int attrValDataType = cInt8(bytes, count);
			int attrValData = cInt32(bytes, count);
			if(attributeNS != -1) writer.print(nsPrefix+":");
			writer.print(strings[attributeName] + "=\"");
			if(attrValDataType==0x3) writer.print(strings[attrValData]);
			else if(attrValDataType==0x10) writer.print(attrValData);
			else if(attrValDataType==0x12) {
				// FIXME: What to do, when data is always -1?
				if(attrValData==0) writer.print("false");
				else if(attrValData==1 || attrValData==-1) writer.print("true");
				else writer.print("UNKNOWN_BOOLEAN_TYPE");
			} else if(attrValDataType==0x1) {
				if(attrValData<0x7f000000) {
					writer.print("@*");
					if(attributeNS != -1) writer.print(nsPrefix+":");
					writer.print("style/"+styleMap.get(attrValData).replaceAll("_", "."));
				} else {
					writer.print("0x" + Integer.toHexString(attrValData));
				}
			}
			else {
				if("configChanges".equals(strings[attributeName])) {
					if(attrValData==1152) writer.print("orientation");
					else if(attrValData==4016) writer.print("keyboard|keyboardHidden|orientation|screenLayout|uiMode");
					else if(attrValData==176) writer.print("keyboard|keyboardHidden|orientation");
					else if(attrValData==160) writer.print("keyboardHidden|orientation");
					else writer.print("UNKNOWN_DATA_"+Integer.toHexString(attrValData));
				} else {
					writer.print("UNKNOWN_DATA_TYPE_" + attrValDataType);
				}
			}
			writer.print("\"");
			if((i+1)<attributeCount) writer.print(" ");
		}
	}

	private void parseElementEnd() {
		if(cInt16(bytes, count) != 0x0010) die("ELEMENT END header is not 0x0010");
		if(cInt32(bytes, count) != 0x18) die("ELEMENT END header chunk is not 0x18 big");
		int endLineNumber = cInt32(bytes, count);
		int comment = cInt32(bytes, count);
		int elementNS = cInt32(bytes, count);
		int elementName = cInt32(bytes, count);
		if(currentTag==strings[elementName]) {
			writer.println(" />");
			wasOneLiner=true;
		} else {
			for(int i=0; i<numtabs; i++) writer.print("\t");
			writer.print("</");
			if(elementNS != -1) writer.print(strings[elementNS]+":");
			writer.println(strings[elementName]+">");
		}
		numtabs-=1;
	}

	private int cInt8(byte[] bytes, int offset) {
		byte[] tmp = new byte[4];
		tmp[3]=bytes[count++];
		return ByteBuffer.wrap(tmp).getInt();
	}

	private int cInt16(byte[] bytes, int offset) {
		byte[] tmp = new byte[4];
		tmp[3]=bytes[count++];
		tmp[2]=bytes[count++];
		return ByteBuffer.wrap(tmp).getInt();
	}

	private int cInt32(byte[] bytes, int offset) {
		byte[] tmp = new byte[4];
		for(int i=0;i <4; i++) tmp[3-i]=bytes[count+i];
		count+=4;
		return ByteBuffer.wrap(tmp).getInt();
	}

	private void die(String message) {
		System.err.println(message);
		System.exit(3);
	}
}
