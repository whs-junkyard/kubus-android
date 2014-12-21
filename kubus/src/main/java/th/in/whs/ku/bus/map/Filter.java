package th.in.whs.ku.bus.map;

import android.os.Parcel;
import android.os.Parcelable;

public class Filter implements Parcelable{
	/**
	 * BUS: Filter by bus ID (1 bus)
	 * LINE: Filter by line ID (all bus of that line)
	 */
	public static enum FilterType {
		BUS,
		LINE
	}
	public Filter.FilterType type;
	public Integer value;
	public Filter(Filter.FilterType type, Integer value) {
		this.type = type;
		this.value = value;
	}
	
	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeSerializable(type);
		dest.writeInt(value);
	}
	
	public static final Parcelable.Creator<Filter> CREATOR = new Parcelable.Creator<Filter>() {
		public Filter createFromParcel(Parcel in) {
			return new Filter(
					(FilterType) in.readSerializable(),
					in.readInt()
			);
		}

		public Filter[] newArray(int size) {
			return new Filter[size];
		}
	};
}