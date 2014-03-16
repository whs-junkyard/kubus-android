package th.in.whs.ku.bus.api;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;

import th.in.whs.ku.bus.protobuf.Packet;
import th.in.whs.ku.bus.util.ListenerList;
import android.content.Context;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.util.SparseArray;

import com.codebutler.android_websockets.WebSocketClient;
import com.joshdholtz.sentry.Sentry;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.squareup.wire.Wire;

public class BusPosition implements Parcelable, Cloneable {
	
	private final URI WS_ENDPOINT = URI.create("ws://madoka.whs.in.th:58439/primus/?version=2");
	private static Wire wire = new Wire();
	
	// facades 
	
	private static BusPosition instance = new BusPosition();
	private static Handler handler = new Handler();
	public static void initialize(){
		BusPosition oldInstance = instance;
		boolean reconnectWs = oldInstance.client.isConnected();
		oldInstance.client.disconnect();
		instance = new BusPosition();
		instance.setContext(oldInstance.context);
		instance.listeners.copyFrom(oldInstance.listeners, false);
		if(reconnectWs){
			instance.client.connect();
		}
	}
	public static void initialize(BusPosition supplied){
		if(supplied != null){
			BusPosition oldInstance = instance;
			boolean reconnectWs = oldInstance.client.isConnected();
			oldInstance.client.disconnect();
			instance = supplied;
			instance.listeners.copyFrom(oldInstance.listeners, instance.hasData);
			if(reconnectWs){
				instance.client.connect();
			}
		}else{
			initialize();
		}
	}
	public static Bus get(String id){
		return instance.getBus(id);
	}
	public static Bus get(Integer id){
		return instance.getBus(id);
	}
	public static void context(Context context){
		instance.context = context;
	}
	public static SparseArray<Bus> gets(){
		return instance.getMap();
	}
	public static int registerUpdateListener(ListenerList.Listener listener){
		return instance.listeners.register(listener);
	}
	public static int registerUpdateListener(ListenerList.Listener listener, boolean fire){
		return instance.listeners.register(listener, fire, instance.hasData);
	}
	public static void removeUpdateListener(int ID){
		instance.listeners.remove(ID);
	}
	public static void removeUpdateListener(ListenerList.Listener listener){
		instance.listeners.remove(listener);
	}
	public static BusPosition instance(){
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
	
	private static WSDisconnect wsdisconnect = new WSDisconnect();
	public static void wsConnect(){
		handler.removeCallbacks(wsdisconnect);
		instance.client.connect();
	}
	
	public static void wsDisconnect(){
		handler.postDelayed(wsdisconnect, 5000);
	}
	
	// object

	private SparseArray<Bus> map;
	private ListenerList listeners;
	private boolean hasData = false;
	private WebSocketClient client;
	private Context context;
	
	public void setContext(Context context) {
		this.context = context;
	}
	private BusPosition(){
		map = new SparseArray<Bus>();
		listeners = new ListenerList();
		prepareWS();
	}
	
	private BusPosition(String data){
		map = new SparseArray<Bus>();
		try {
			loadToMap(new JSONArray(data));
		} catch (JSONException e) {
		}
		listeners = new ListenerList();
		prepareWS();
	}
	
	private BusPosition(byte[] data){
		map = new SparseArray<Bus>();
		Packet packet;
		try {
			packet = wire.parseFrom(data, Packet.class);
			loadToMap(packet);
		} catch (IOException e) {
		}
		listeners = new ListenerList();
		prepareWS();
	}
	
	// listeners are not cloned!
	private BusPosition(BusPosition clone) throws JSONException{
		this(clone.toPacket().toByteArray());
	}
	
	private void loadData(){
		API.get("map/getbusposition", null, new JsonHttpResponseHandler(){
			@Override
			public void onSuccess(int statusCode, Header[] headers,
					JSONArray resp) {
				try {
					loadToMap(resp);
				} catch (JSONException e) {
				}
				
				listeners.fire();
			}
			
		});
	}
	
	private void loadToMap(JSONArray data) throws JSONException{
		for(int i=0; i<data.length(); i++){
			Bus item = new Bus(data.getJSONObject(i));
			map.put(item.id, item);
		}
		hasData = true;
	}
	
	private void loadToMap(Packet data){
		for(Packet.BusUpdate bus : data.uncompressed){
			Bus item = new Bus(bus);
			map.put(item.id, item);
		}
		hasData = true;
	}
	
	private Bus getBus(String busId){
		return getBus(Integer.parseInt(busId));
	}
	private Bus getBus(Integer busId){
		return map.get(busId);
	}
	private SparseArray<Bus> getMap(){
		// note: mutable!
		// making this immutable slow down the app a lot
		return map;
	}
	
	private JSONArray toJSONArray() throws JSONException{
		JSONArray out = new JSONArray();
		for(int i=0; i<map.size(); i++){
			out.put(map.valueAt(i).toJSONObject());
		}
		return out;
	}
	
	private Packet toPacket(){
		ArrayList<Packet.BusUpdate> uncompressed = new ArrayList<Packet.BusUpdate>(map.size());
		for(int i=0; i<map.size(); i++){
			uncompressed.add(map.valueAt(i).toProtobuf());
		}
		return new Packet.Builder().uncompressed(uncompressed).build();
	}
	
	private void prepareWS() {
		client = new WebSocketClient(WS_ENDPOINT, new WebSocketClient.Listener() {
		    @Override
		    public void onConnect() {
		        Log.d("BusPosition API", "Connected to WebSocket");
		    }

		    @Override
		    public void onMessage(byte[] data) {
		    	try {
					Packet message = wire.parseFrom(data, Packet.class);
					for(Packet.BusUpdate add : message.uncompressed){
						Bus bus = new Bus(add);
						map.put(bus.id, bus);
					}
					for(Packet.BusUpdate add : message.add){
						Bus bus = new Bus(add);
						map.put(bus.id, bus);
					}
					for(Packet.BusDelete del : message.del){
						map.remove(del.id);
					}
					for(Packet.BusPosition position : message.position){
						Bus bus = map.get(position.id);
						if(bus == null){
							Log.d("BusPosition API", "Got bus moved event, but no such bus was registered");
							continue;
						}
						bus.latitude = position.latitude;
						bus.longitude = position.longitude;
					}
				} catch (IOException e) {
					Log.e("BusPosition API", "MsgPack parse fail", e);
					return;
				}
		    	if(context != null){
			    	new Handler(context.getMainLooper()).post(new Runnable(){
	
						@Override
						public void run() {
							listeners.fire();
						}
			    		
			    	});
		    	}else{
		    		Log.e("BusPosition API", "Got message, but context is null");
		    	}
		    }

		    @Override
		    public void onDisconnect(int code, String reason) {
		        Log.d("BusPosition API", String.format("Disconnected from WebSocket Code: %d Reason: %s", code, reason));
		    }

		    @Override
		    public void onError(Exception error) {
		        Log.e("BusPosition API", "WebSocket Error", error);
		        //Sentry.captureException(error, Sentry.SentryEventBuilder.SentryEventLevel.WARNING);
		    }

			@Override
			public void onMessage(String message) {
				Log.e("BusPosition API", "Got string "+message);
			}

		}, null);
	}
	
	private static class WSDisconnect implements Runnable{

		@Override
		public void run() {
			instance.client.disconnect();
		}
		
	}
	
	// clonable
	
	public BusPosition clone(){
		if(this.map.size() > 0){
			try {
				return new BusPosition(this);
			} catch (JSONException e) {
				return new BusPosition();
			}
		}else{
			return new BusPosition();
		}
	}
	
	// parcelable
	
	@Override
	public int describeContents() {
		return Parcelable.CONTENTS_FILE_DESCRIPTOR;
	}
	@Override
	public void writeToParcel(Parcel out, int flags) {
		if(map.size() > 0){
			out.writeByteArray(toPacket().toByteArray());
		}
	}
	
	public static final Parcelable.Creator<BusPosition> CREATOR
	= new Parcelable.Creator<BusPosition>() {
		public BusPosition createFromParcel(Parcel in) {
			byte[] data = in.createByteArray();
			if(data.length > 0){
				return new BusPosition(data);
			}else{
				return new BusPosition();
			}
		}

		public BusPosition[] newArray(int size) {
			return new BusPosition[size];
		}
	};
	
}
