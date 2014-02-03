package th.in.whs.ku.bus.api;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.squareup.wire.Wire;

import th.in.whs.ku.bus.protobuf.Packet;
import android.os.Parcel;
import android.os.Parcelable;

public class BusStatus implements Parcelable{
	public int id;
	public int lineid;
	public String name;
	public String linename;
	public TimeAgo estimated_time;
	
	public BusStatus(JSONObject inp) throws JSONException{
		parseJSON(inp);
	}
	public BusStatus(th.in.whs.ku.bus.protobuf.BusStatus inp){
		parseProtobuf(inp);
	}
	
	public void parseJSON(JSONObject data) throws JSONException{
		id = data.getInt("busid");
		lineid = data.getInt("buslineid");
		name = data.getString("busname");
		linename = data.getString("buslinename");
		estimated_time = new TimeAgo(data.getString("estimatedtime"));
	}
	
	public void parseProtobuf(th.in.whs.ku.bus.protobuf.BusStatus data){
		id = data.id;
		lineid = data.lineid;
		name = data.name;
		linename = data.linename;
		estimated_time = new TimeAgo(data.estimated_time, data.estimated_time_since);
	}
	
	public Bus getBus(){
		return BusPosition.get(id);
	}
	
	public th.in.whs.ku.bus.protobuf.BusStatus toProtobuf(){
		return new th.in.whs.ku.bus.protobuf.BusStatus.Builder()
				.id(id)
				.lineid(lineid)
				.name(name)
				.linename(linename)
				.estimated_time((long) (estimated_time.getMilli() * 1000))
				.estimated_time_since((long) (estimated_time.getSinceTime() * 1000))
				.build();
	}
	@Override
	public int describeContents() {
		return 0;
	}
	@Override
	public void writeToParcel(Parcel out, int flags) {
		byte[] data = toProtobuf().toByteArray();
		out.writeInt(data.length);
		out.writeByteArray(data);
	}
	public static final Parcelable.Creator<BusStatus> CREATOR
	= new Parcelable.Creator<BusStatus>() {
		public BusStatus createFromParcel(Parcel in) {
			byte[] data = new byte[in.readInt()];
			in.readByteArray(data);
			Wire wire = new Wire();
			th.in.whs.ku.bus.protobuf.BusStatus busStatus;
			try {
				busStatus = wire.parseFrom(data, th.in.whs.ku.bus.protobuf.BusStatus.class);
			} catch (IOException e) {
				return null;
			}
			return new BusStatus(busStatus);
		}

		public BusStatus[] newArray(int size) {
			return new BusStatus[size];
		}
	};
}