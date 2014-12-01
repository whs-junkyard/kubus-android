package th.in.whs.ku.bus.api;

import android.annotation.SuppressLint;
import android.util.Log;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;

import th.in.whs.ku.bus.BusMapController;
import th.in.whs.ku.bus.R;
import th.in.whs.ku.bus.protobuf.Packet;

public class Bus{
	public int id;
    public double latitude;
    public double longitude;
    public String name;
    public int lineid;
    public boolean isinline;
    public boolean isinpark;
    public boolean available;
    public long timestamp;
    public Bus(JSONObject obj){
    	try {
    		this.id = obj.getInt("busid");
    	    this.latitude = obj.getDouble("latitude");
    	    this.longitude = obj.getDouble("longitude");
    	    this.name = obj.getString("busname");
    	    this.lineid = obj.getInt("buslineid");
    	    if(obj.has("isinline")){
    	    	this.isinline = obj.getString("isinline").equals("t");
    	    }else{
    	    	this.isinline = false;
    	    }
    	    if(obj.has("isinpark")){
    	    	this.isinpark = obj.getString("isinpark").equals("t");
    	    }else{
    	    	this.isinpark = false;
    	    }
    	    if(obj.has("available")){
    	    	this.available = obj.getString("available").equals("1");
    	    }else{
    	    	this.available = false;
    	    }
    	    this.timestamp = getDateFormat().parse(obj.getString("bustimestamp")).getTime();
		} catch (JSONException e) {
			Log.e("Bus", "Bus JSON Parse error", e);
		} catch (ParseException e) {
		}
    }
    public Bus(Packet.BusUpdate obj){
    	this.id = obj.id;
        this.latitude = obj.latitude;
        this.longitude = obj.longitude;
        this.name = obj.name;
        this.lineid = obj.lineid;
        this.isinline = obj.isinline;
        this.isinpark = obj.isinpark;
        this.available = obj.available;
        this.timestamp = obj.timestamp;
    }
    @SuppressLint("SimpleDateFormat")
	public DateFormat getDateFormat(){
		return new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
	}
    public JSONObject toJSONObject() throws JSONException{
    	JSONObject out = new JSONObject();
    	out.put("busid", this.id);
    	out.put("latitude", this.latitude);
    	out.put("longitude", this.longitude);
    	out.put("busname", this.name);
    	out.put("buslineid", this.lineid);
    	out.put("isinline", this.isinline ? "t" : "f");
    	out.put("isinpark", this.isinpark ? "t" : "f");
    	out.put("available", this.available ? "1" : "0");
    	out.put("bustimestamp", getDateFormat().format(new Date(this.timestamp)));
    	return out;
    }
    public Packet.BusUpdate toProtobuf(){
    	return new Packet.BusUpdate.Builder()
    		.id(id)
    		.latitude(latitude)
    		.longitude(longitude)
    		.name(name)
    		.lineid(lineid)
    		.isinline(isinline)
    		.isinpark(isinpark)
    		.available(available)
    		.timestamp(timestamp)
    		.build();
    }
    public String toString(){
    	try {
			return toJSONObject().toString();
		} catch (JSONException e) {
			return "";
		}
    }
    public int getColor(){
    	return BusMapController.getColor(lineid);
    }
    
    public boolean equals(Bus b){
    	return b != null &&
    		this.id == b.id &&
    		this.name == b.name &&
    		this.lineid == b.lineid &&
    		this.isinline == b.isinline &&
    		this.isinpark == b.isinpark &&
    		this.available == b.available &&
    		this.latitude == b.latitude &&
    		this.longitude == b.longitude;
    }
    
    public boolean isLocationEqual(Bus b){
    	return b != null &&
    			this.latitude == b.latitude &&
        		this.longitude == b.longitude;
    }
    
    public boolean isLocationEqual(LatLng b){
    	return b != null &&
    			this.latitude == b.latitude &&
        		this.longitude == b.longitude;
    }
}