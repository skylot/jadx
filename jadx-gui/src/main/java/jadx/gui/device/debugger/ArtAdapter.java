package jadx.gui.device.debugger;

public class ArtAdapter {

	public interface IArtAdapter {
		int getRuntimeRegNum(int smaliNum, int regCount, int paramStart);

		boolean readNullObject();

		String typeForNull();
	}

	public static IArtAdapter getAdapter(int androidReleaseVer) {
		if (androidReleaseVer <= 8) {
			return new AndroidOreoAndBelow();
		} else {
			return new AndroidPieAndAbove();
		}
	}

	public static class AndroidOreoAndBelow implements IArtAdapter {
		@Override
		public int getRuntimeRegNum(int smaliNum, int regCount, int paramStart) {
			int localRegCount = regCount - paramStart;
			return (smaliNum + localRegCount) % regCount;
		}

		@Override
		public boolean readNullObject() {
			return true;
		}

		@Override
		public String typeForNull() {
			return "";
		}
	}

	public static class AndroidPieAndAbove implements IArtAdapter {
		@Override
		public int getRuntimeRegNum(int smaliNum, int regCount, int paramStart) {
			return smaliNum;
		}

		@Override
		public boolean readNullObject() {
			return false;
		}

		@Override
		public String typeForNull() {
			return "zero value";
		}
	}
}
