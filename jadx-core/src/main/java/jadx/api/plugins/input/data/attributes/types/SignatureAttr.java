package jadx.api.plugins.input.data.attributes.types;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.PinnedAttribute;

public class SignatureAttr extends PinnedAttribute {

	private final String signature;

	public SignatureAttr(String signature) {
		this.signature = signature;
	}

	public String getSignature() {
		return signature;
	}

	@Override
	public IJadxAttrType<? extends IJadxAttribute> getAttrType() {
		return JadxAttrType.SIGNATURE;
	}

	@Override
	public String toString() {
		return "SIGNATURE: " + signature;
	}
}
