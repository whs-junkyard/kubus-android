package th.in.whs.ku.bus.api;

import java.util.Locale;

import th.in.whs.ku.bus.BuildConfig;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.FragmentActivity;

import com.loopj.android.http.*;

public class API {
	protected static final String BASE_URL="http://kubus.netburzt.com/";
	protected static final String STREAM_URL="http://madoka.whs.in.th:58439/";

	private static AsyncHttpClient client = new AsyncHttpClient();

	public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		client.get(getAbsoluteUrl(url), params, responseHandler);
	}
	
	public static void get(Context context, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		client.get(context, getAbsoluteUrl(url), params, responseHandler);
	}

	public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		client.post(getAbsoluteUrl(url), params, responseHandler);
	}
	
	public static void post(Context context, String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
		client.post(context, getAbsoluteUrl(url), params, responseHandler);
	}
	
	public static void registerNotify(RequestParams params, AsyncHttpResponseHandler responseHandler){
		client.post(STREAM_URL + "push/stop", params, responseHandler);
	}

	private static String getAbsoluteUrl(String relativeUrl) {
		return BASE_URL + relativeUrl;
	}

	public static void cancel(Context context) {
		client.cancelRequests(context, true);
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
		client.setUserAgent(userAgent.toString());
	}
}
