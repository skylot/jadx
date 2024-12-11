package jadx.core.export;

public class GradleInfoStorage {

	private boolean vectorPathData;

	private boolean vectorFillType;

	private boolean useApacheHttpLegacy;

	private boolean nonFinalResIds;

	public boolean isVectorPathData() {
		return vectorPathData;
	}

	public void setVectorPathData(boolean vectorPathData) {
		this.vectorPathData = vectorPathData;
	}

	public boolean isVectorFillType() {
		return vectorFillType;
	}

	public void setVectorFillType(boolean vectorFillType) {
		this.vectorFillType = vectorFillType;
	}

	public boolean isUseApacheHttpLegacy() {
		return useApacheHttpLegacy;
	}

	public void setUseApacheHttpLegacy(boolean useApacheHttpLegacy) {
		this.useApacheHttpLegacy = useApacheHttpLegacy;
	}

	public boolean isNonFinalResIds() {
		return nonFinalResIds;
	}

	public void setNonFinalResIds(boolean nonFinalResIds) {
		this.nonFinalResIds = nonFinalResIds;
	}
}
