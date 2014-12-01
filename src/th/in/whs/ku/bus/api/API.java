package th.in.whs.ku.bus.api;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import th.in.whs.ku.bus.BuildConfig;
import android.net.Uri;
import android.os.Build;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

public class API {
	protected static final String BASE_URL="http://kusmartbus.netburzt.com/";
	protected static final String STREAM_URL="http://madoka.whs.in.th:58439/";

	public static final OkHttpClient client = new OkHttpClient();
	public static String userAgent = "";
	
	public static Request.Builder getRequestBuilder(String url){
		return getRequestBuilder(url, null);
	}
	
	public static Request.Builder getRequestBuilder(String url, Map<String, String> params){
		Uri.Builder builder = Uri.parse(getAbsoluteUrl(url)).buildUpon();
		
		if(params != null){
			for(Map.Entry<String, String> entry : params.entrySet()){
				builder.appendQueryParameter(entry.getKey(), entry.getValue());
			}
		}
		
		return new Request.Builder()
			.addHeader("User-Agent", userAgent)
			.url(builder.build().toString());
	}
	
	public static Call get(String url){
		Request request = getRequestBuilder(url, null)
				.get()
				.build();
		return client.newCall(request);
	}
	
	public static Call get(String url, Map<String, String> params){
		Request request = getRequestBuilder(url, params)
				.get()
				.build();
		return client.newCall(request);
	}
	
	public static Call post(String url, Map<String, String> params){
		FormEncodingBuilder builder = new FormEncodingBuilder();
		
		if(params != null){
			for(Map.Entry<String, String> entry : params.entrySet()){
				builder.add(entry.getKey(), entry.getValue());
			}
		}
		
		Request request = getRequestBuilder(url)
				.post(builder.build())
				.build();
		return client.newCall(request);
	}
	
	public static Call registerNotify(String stop, List<String> passingLine, String regId) {
		FormEncodingBuilder builder = new FormEncodingBuilder();
		
		builder.add("backend", "gcm");
		builder.add("stop", stop);
		builder.add("gcm_id", regId);
		for(String lineId : passingLine){
			builder.add("line[]", lineId);
		}
		
		Request request = getRequestBuilder(STREAM_URL + "push/stop")
				.post(builder.build())
				.build();
		return client.newCall(request);
	}

	private static String getAbsoluteUrl(String relativeUrl) {
		if(relativeUrl.startsWith("http://")){
			return relativeUrl;
		}
		return BASE_URL + relativeUrl;
	}

	public static void init(String version) {
	    StringBuilder userAgent = new StringBuilder("KUSmartBus/");
	    userAgent.append(version);
	    userAgent.append(" (Android/");
	    userAgent.append(Build.VERSION.RELEASE);
	    userAgent.append("; ");
	    userAgent.append(Locale.getDefault().getLanguage());
	    userAgent.append("; ");
	    userAgent.append(Build.BRAND);
	    userAgent.append(" ");
	    userAgent.append(Build.MODEL);
	    if(BuildConfig.DEBUG){
	    	userAgent.append("; DEBUG");
	    }
	    userAgent.append(")");
	    API.userAgent = userAgent.toString();
	}
	
}
