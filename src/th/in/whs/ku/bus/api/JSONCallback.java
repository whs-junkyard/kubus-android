package th.in.whs.ku.bus.api;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Handler;
import android.util.Log;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

abstract public class JSONCallback implements Callback {
	
	abstract protected Handler getHandler();
	
	@Override
	public void onResponse(Response req) throws IOException {
		if(getHandler() == null){
			return;
		}
		String body = req.body().string();
		try{
			final JSONArray obj = new JSONArray(body);
			getHandler().post(new Runnable(){
				public void run(){
					callback(obj);
				}
			});
			return;
		}catch(JSONException e){
		}
		try{
			final JSONObject obj = new JSONObject(body);
			getHandler().post(new Runnable(){
				public void run(){
					callback(obj);
				}
			});
			return;
		}catch(JSONException e){
		}
		callback(req);
	}

	@Override
	public void onFailure(Request req, IOException err) {
		Log.e("JSONCallback", "Request error - " + req.urlString(), err);
	}
	
	public void callback(JSONObject obj){
	}
	public void callback(JSONArray obj){
	}
	public void callback(Response obj){
	}
	
	
}
