package th.in.whs.ku.bus.api;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import th.in.whs.ku.bus.R;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.loopj.android.http.JsonHttpResponseHandler;

public class BusStopList implements Parcelable, Cloneable {
	private static BusStopList instance = new BusStopList();
	public static void initialize(){
		BusStopList oldInstance = instance;
		instance = new BusStopList();
		instance.listeners.copyFrom(oldInstance.listeners, false);
		instance.progress.copyFrom(oldInstance.progress, false);
		instance.error.copyFrom(oldInstance.error, false);
	}
	public static void initialize(BusStopList supplied){
		if(supplied != null){
			BusStopList oldInstance = instance;
			instance = supplied;
			instance.listeners.copyFrom(oldInstance.listeners, instance.hasData);
			instance.progress.copyFrom(oldInstance.progress, instance.isLoading);
			instance.error.copyFrom(oldInstance.error, false);
		}else{
			initialize();
		}
	}
	public static int registerUpdateListener(ListenerList.Listener listener){
		return instance.listeners.register(listener);
	}
	public static int registerUpdateListener(ListenerList.Listener listener, boolean fire){
		return instance.listeners.register(listener, fire, instance.hasData);
	}
	public static int registerProgressListener(ListenerList.Listener listener){
		return instance.progress.register(listener);
	}
	public static int registerErrorListener(ListenerList.Listener listener, boolean fire){
		return instance.error.register(listener, fire, instance.hasError);
	}
	public static void removeUpdateListener(int ID){
		instance.listeners.remove(ID);
	}
	public static void removeUpdateListener(ListenerList.Listener listener){
		instance.listeners.remove(listener);
	}
	public static void removeProgressListener(int ID){
		instance.progress.remove(ID);
	}
	public static void removeProgressListener(ListenerList.Listener listener){
		instance.progress.remove(listener);
	}
	public static void removeErrorListener(int ID){
		instance.error.remove(ID);
	}
	public static void removeErrorListener(ListenerList.Listener listener){
		instance.error.remove(listener);
	}
	public static BusStopList instance(){
		return instance.clone();
	}
	public static void refresh(){
		instance.loadData();
	}
	public static void refreshIfNoData(){
		if(!instance.hasData){
			refresh();
		}
	}
	public static JSONObject data(){
		return instance.getData();
	}

	// object

	private JSONObject data;
	private ListenerList listeners = new ListenerList();
	private ListenerList progress = new ListenerList();
	private ListenerList error = new ListenerList();
	private boolean hasData = false;
	private boolean isLoading = false;
	private boolean hasError = false;
	
	private BusStopList(){}

	private BusStopList(String data){
		try {
			this.data = new JSONObject(data);
			this.hasData = true;
		} catch (JSONException e) {
		}
	}

	// listeners are not cloned!
	private BusStopList(BusStopList clone){
		this(clone.data.toString());
	}

	private JSONObject getData(){
		return data;
	}

	private void loadData(){
		Log.d("BusStopList", "Downloading bus stop data");
		isLoading = true;
		API.get("appapi/mobileService/getRouteAndStop.php", null, new JsonHttpResponseHandler(){
			@Override
			public void onSuccess(int statusCode, Header[] headers,
					JSONObject resp) {
				isLoading = false;
				data = resp;
				hasData = true;
				hasError = false;

				Log.d("BusStopList API", "Firing to "+String.valueOf(listeners.size())+" listeners");
				listeners.fire();
			}
			
			@Override
			public void onFailure(int statusCode, Header[] headers,
					Throwable throwable, JSONObject errorResponse) {
				isLoading = false;
				hasError = true;
				Log.e("BusStopList API", "Download bus stop data failed", throwable);
				error.fire();
			}

			@Override
			public void onProgress(int bytesWritten, int totalSize) {
				Log.d("BusPosition API", "Onprogress fire");
				Bundle data = new Bundle();
				data.putFloat("progress", bytesWritten/totalSize);
				data.putInt("bytesWritten", bytesWritten);
				data.putInt("totalSize", totalSize);
				progress.fire(data);
			}
		});
	}

	// clonable

	public BusStopList clone(){
		if(this.data != null){
			return new BusStopList(this.data.toString());
		}else{
			return new BusStopList();
		}
	}

	// parcelable

	@Override
	public int describeContents() {
		return Parcelable.CONTENTS_FILE_DESCRIPTOR;
	}
	@Override
	public void writeToParcel(Parcel out, int flags) {
		if(data != null){
			out.writeString(data.toString());
		}
	}

	public static final Parcelable.Creator<BusStopList> CREATOR
	= new Parcelable.Creator<BusStopList>() {
		public BusStopList createFromParcel(Parcel in) {
			String data = in.readString();
			if(data.length() == 0){
				return new BusStopList(data);
			}else{
				return new BusStopList();
			}
		}

		public BusStopList[] newArray(int size) {
			return new BusStopList[size];
		}
	};
}
