package com.joshdholtz.sentry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.joshdholtz.sentry.Sentry.SentryEventBuilder.SentryEventLevel;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class Sentry {
	
	private final static String VERSION = "1.1";
	
	private Context context;

	private String baseUrl;
	private String dsn;
	private String packageName;
	private Map<String, String> tags;
	private SentryEventCaptureListener captureListener;
	
	private OkHttpClient client = new OkHttpClient();
	public static final MediaType json = MediaType.parse("application/json; charset=utf-8");

	private static final String TAG = "Sentry";
	private static final String DEFAULT_BASE_URL = "https://app.getsentry.com";
	
	private Sentry() {

	}

	private static Sentry getInstance() {
		return LazyHolder.instance;
	}

	private static class LazyHolder {
		private static Sentry instance = new Sentry();
	}

	public static void init(Context context, String dsn) {
		init(context, DEFAULT_BASE_URL, dsn, new HashMap<String, String>());
	}

	public static void init(Context context, String baseUrl, String dsn) {
		init(context, baseUrl, dsn, new HashMap<String, String>());
	}

	public static void init(Context context, String baseUrl, String dsn, Map<String, String> tags) {
		Sentry instance = Sentry.getInstance();
		instance.context = context;
		instance.dsn = dsn;
		instance.packageName = context.getPackageName();
		instance.tags = tags;
		instance.baseUrl = baseUrl;

		
		Sentry.getInstance().setupUncaughtExceptionHandler();
	}
	
	private void setupUncaughtExceptionHandler() {
		
		UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
		if (currentHandler != null) {
			Log.d("Debugged", "current handler class="+currentHandler.getClass().getName());
		}
		
		// don't register again if already registered
		if (!(currentHandler instanceof SentryUncaughtExceptionHandler)) {
			// Register default exceptions handler
			Thread.setDefaultUncaughtExceptionHandler(
					new SentryUncaughtExceptionHandler(currentHandler, context));
		}
		
		sendAllCachedCapturedEvents();
	}
	
	private static String createXSentryAuthHeader() {
		String header = "";
		
		Uri uri = Uri.parse(Sentry.getInstance().dsn);
		Log.d("Sentry", "URI - " + uri);
		String authority = uri.getAuthority().replace("@" + uri.getHost(), "");
		
		String[] authorityParts = authority.split(":");
		String publicKey = authorityParts[0];
		String secretKey = authorityParts[1];
		
		header += "Sentry sentry_version=4,";
		header += "sentry_client=sentry-android/" + VERSION + ",";
		header += "sentry_timestamp=" + System.currentTimeMillis() +",";
		header += "sentry_key=" + publicKey + ",";
		header += "sentry_secret=" + secretKey;
		
		return header;
	}
	
	private static String getProjectId() {
		Uri uri = Uri.parse(Sentry.getInstance().dsn);
		String path = uri.getPath();
		String projectId = path.substring(path.lastIndexOf("/") + 1);
		
		return projectId;
	}

	public static void sendAllCachedCapturedEvents() {
		ArrayList<SentryEventRequest> unsentRequests = InternalStorage.getInstance().getUnsentRequests();
		for (SentryEventRequest request : unsentRequests) {
			Sentry.doCaptureEventPost(request);
		}
	}
	
	/**
	 * @param captureListener the captureListener to set
	 */
	public static void setCaptureListener(SentryEventCaptureListener captureListener) {
		Sentry.getInstance().captureListener = captureListener;
	}

	public static void captureMessage(String message) {
		Sentry.captureMessage(message, SentryEventLevel.INFO);
	}
	
	public static void captureMessage(String message, SentryEventLevel level) {
		Sentry.captureEvent(new SentryEventBuilder()
				.setMessage(message)
				.setLevel(level)
				.setTags(getInstance().tags)
		);
	}
	
	public static void captureException(Throwable t) {
		Sentry.captureException(t, SentryEventLevel.ERROR);
	}
	
	public static void captureException(Throwable t, SentryEventLevel level) {
		String culprit = getCause(t, t.getMessage());
		
		Sentry.captureEvent(new SentryEventBuilder()
			.setMessage(t.getMessage())
			.setCulprit(culprit)
			.setLevel(level)
			.setException(t)
			.setTags(getInstance().tags)
		);
	}

	public static void captureUncaughtException(Context context, Throwable t) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		t.printStackTrace(printWriter);
		try {
			// Random number to avoid duplicate files
			long random = System.currentTimeMillis();

			// Embed version in stacktrace filename
			File stacktrace = new File(getStacktraceLocation(context), "raven-" +  String.valueOf(random) + ".stacktrace");
			Log.d(TAG, "Writing unhandled exception to: " + stacktrace.getAbsolutePath());

			// Write the stacktrace to disk
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(stacktrace));
			oos.writeObject(t);
			oos.flush();
			// Close up everything
			oos.close();
		} catch (Exception ebos) {
			// Nothing much we can do about this - the game is over
			ebos.printStackTrace();
		}

		Log.d(TAG, result.toString());
	}

	private static String getCause(Throwable t, String culprit) {
		for (StackTraceElement stackTrace : t.getStackTrace()) {
			if (stackTrace.toString().contains(Sentry.getInstance().packageName)) {
				culprit = stackTrace.toString();
				break;
			}
		}
		
		return culprit;
	}

	private static File getStacktraceLocation(Context context) {
		return new File(context.getCacheDir(), "crashes");
	}
	
	public static void captureEvent(SentryEventBuilder builder) {
		final SentryEventRequest request;
		if (Sentry.getInstance().captureListener != null) {
			
			builder = Sentry.getInstance().captureListener.beforeCapture(builder);
			if (builder == null) {
				Log.e(Sentry.TAG, "SentryEventBuilder in captureEvent is null");
				return;
			}
			
			request = new SentryEventRequest(builder);
		} else {
			request = new SentryEventRequest(builder);
		}

		doCaptureEventPost(request);
	}

	private static boolean isNetworkAvailable() {
		PackageManager pm = Sentry.getInstance().context.getPackageManager();
		int hasPerm = pm.checkPermission(android.Manifest.permission.ACCESS_NETWORK_STATE, Sentry.getInstance().context.getPackageName());
		if (hasPerm == PackageManager.PERMISSION_DENIED) {
		   return true;
		}
		
	    ConnectivityManager connectivityManager = (ConnectivityManager) Sentry.getInstance().context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	
	private static void doCaptureEventPost(final SentryEventRequest request) {
		if (!isNetworkAvailable()) {
			InternalStorage.getInstance().addRequest(request);
			return;
		}
		
		Sentry instance = getInstance();
		
		RequestBody body = RequestBody.create(json, request.getRequestData());
		
		Request req = new Request.Builder()
			.url(instance.baseUrl + "/api/" + getProjectId() + "/store/")
			.addHeader("User-Agent", "sentry-android/" + VERSION)
			.addHeader("X-Sentry-Auth", createXSentryAuthHeader())
			.post(body)
			.build();
		
		instance.client.newCall(req).enqueue(new Callback(){

			@Override
			public void onResponse(Response res) throws IOException {
				InternalStorage.getInstance().removeBuilder(request);
				Log.d(TAG, "SendEvent - " + res.code() + " " + res.body().string());
			}
			
			@Override
			public void onFailure(Request req, IOException err) {
				InternalStorage.getInstance().addRequest(request);

				Log.e(TAG, "SendEvent - fail", err);
			}
			
		});
	}

	private class SentryUncaughtExceptionHandler implements UncaughtExceptionHandler {

		private UncaughtExceptionHandler defaultExceptionHandler;
		private Context context;

		// constructor
		public SentryUncaughtExceptionHandler(UncaughtExceptionHandler pDefaultExceptionHandler, Context context) {
			defaultExceptionHandler = pDefaultExceptionHandler;
			this.context = context;
		}

		@Override
		public void uncaughtException(Thread thread, Throwable e) {
			// Here you should have a more robust, permanent record of problems
			SentryEventBuilder builder = new SentryEventBuilder(e, SentryEventBuilder.SentryEventLevel.FATAL);
			if (Sentry.getInstance().captureListener != null) {
				builder = Sentry.getInstance().captureListener.beforeCapture(builder);
			}			

            if (builder != null) {
            	builder.setTags(tags);
                InternalStorage.getInstance().addRequest(new SentryEventRequest(builder));
            } else {
                Log.e(Sentry.TAG, "SentryEventBuilder in uncaughtException is null");
            }

			//call original handler  
			defaultExceptionHandler.uncaughtException(thread, e);  
		}

	}
	
	private static class InternalStorage {

		private final static String FILE_NAME = "unsent_requests";
		private ArrayList<SentryEventRequest> unsentRequests;
		
		private static InternalStorage getInstance() {
			return LazyHolder.instance;
		}

		private static class LazyHolder {
			private static InternalStorage instance = new InternalStorage();
		}
		
		private InternalStorage() {
			this.unsentRequests = this.readObject(Sentry.getInstance().context);
		}		
		
		/**
		 * @return the unsentRequests
		 */
		public ArrayList<SentryEventRequest> getUnsentRequests() {
			return unsentRequests;
		}

		public void addRequest(SentryEventRequest request) {
			synchronized(this) {
				if (!this.unsentRequests.contains(request)) {
					this.unsentRequests.add(request);
					this.writeObject(Sentry.getInstance().context, this.unsentRequests);
				}
			}
		}
		
		public void removeBuilder(SentryEventRequest request) {
			synchronized(this) {
				this.unsentRequests.remove(request);
				this.writeObject(Sentry.getInstance().context, this.unsentRequests);
			}
		}

		private void writeObject(Context context, ArrayList<SentryEventRequest> requests) {
			try {
				FileOutputStream fos = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(requests);
				oos.close();
				fos.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		private ArrayList<SentryEventRequest> readObject(Context context) {
			try {
				FileInputStream fis = context.openFileInput(FILE_NAME);
				ObjectInputStream ois = new ObjectInputStream(fis);
				ArrayList<SentryEventRequest> requests = (ArrayList<SentryEventRequest>) ois.readObject();
				return requests;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (StreamCorruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			return new ArrayList<SentryEventRequest>();
		}
	}

	public abstract static class SentryEventCaptureListener {
		
		public abstract SentryEventBuilder beforeCapture(SentryEventBuilder builder);
		
	}
	
	public static class SentryEventRequest implements Serializable {
		private String requestData;
		private UUID uuid;
		
		public SentryEventRequest(SentryEventBuilder builder) {
			this.requestData = new JSONObject(builder.event).toString();
			this.uuid = UUID.randomUUID();
		}
		
		/**
		 * @return the requestData
		 */
		public String getRequestData() {
			return requestData;
		}

		/**
		 * @return the uuid
		 */
		public UUID getUuid() {
			return uuid;
		}

		@Override
		public boolean equals(Object other) {
			SentryEventRequest otherRequest = (SentryEventRequest) other;
			
			if (this.uuid != null && otherRequest.uuid != null) {
				return uuid.equals(otherRequest.uuid);
			}
			
			return false;
		}
		
	}

	public static class SentryEventBuilder implements Serializable {

		private static final long serialVersionUID = -8589756678369463988L;
		
		private final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		static {
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		}
		
		private Map<String, Object> event;
		
		public static enum SentryEventLevel {
			
			FATAL("fatal"),
			ERROR("error"),
			WARNING("warning"),
			INFO("info"),
			DEBUG("debug");
			
			private String value;
			SentryEventLevel(String value) {
				this.value = value;
			}
			
		}
		
		public SentryEventBuilder() {
			event = new HashMap<String, Object>();
			event.put("event_id", UUID.randomUUID().toString().replace("-", ""));
			this.setPlatform("android");
			this.setTimestamp(System.currentTimeMillis());
			this.setModule("android-sentry", Sentry.VERSION);
			//this.setModule(AsyncHttpClient.class.getPackage().getName(), new AsyncHttpClient());
		}
		
		public SentryEventBuilder(Throwable t, SentryEventLevel level) {
			this();
			
			String culprit = getCause(t, t.getMessage());
			
			this.setMessage(t.getMessage())
			.setCulprit(culprit)
			.setLevel(level)
			.setException(t);
		}

		/**
		 * "message": "SyntaxError: Wattttt!"
		 * @param message
		 * @return
		 */
		public SentryEventBuilder setMessage(String message) {
			event.put("message", message);
			return this;
		}
		
		/**
		 * "timestamp": "2011-05-02T17:41:36"
		 * @param timestamp
		 * @return
		 */
		public SentryEventBuilder setTimestamp(long timestamp) {
			event.put("timestamp", sdf.format(new Date(timestamp)));
			return this;
		}
		
		/**
		 * "level": "warning"
		 * @param level
		 * @return
		 */
		public SentryEventBuilder setLevel(SentryEventLevel level) {
			event.put("level", level.value);
			return this;
		}
		
		/**
		 * "logger": "my.logger.name"
		 * @param logger
		 * @return
		 */
		public SentryEventBuilder setLogger(String logger) {
			event.put("logger", logger);
			return this;
		}
		
		/**
		 * "platform": "python"
		 * @param platform
		 * @return
		 */
		public SentryEventBuilder setPlatform(String platform) {
			event.put("platform", platform);
			return this;
		}
		
		/**
		 * "culprit": "my.module.function_name"
		 * @param culprit
		 * @return
		 */
		public SentryEventBuilder setCulprit(String culprit) {
			event.put("culprit", culprit);
			return this;
		}
		
		/**
		 * 
		 * @param tags
		 * @return
		 */
		public SentryEventBuilder setTags(Map<String,String> tags) {
			setTags(new JSONObject(tags));
			return this;
		}
		
		public SentryEventBuilder setTags(JSONObject tags) {
			try{
				tags.put("device", Build.DEVICE);
				tags.put("device_name", Build.MODEL);
				tags.put("device_brand", Build.BRAND);
				tags.put("android_version", String.valueOf(Build.VERSION.SDK_INT));
				tags.put("android_version_name", Build.VERSION.RELEASE);
			}catch(JSONException e){
			}
			event.put("tags", tags);
			return this;
		}
		
		public JSONObject getTags() {
			if (!event.containsKey("tags")) {
				setTags(new HashMap<String, String>());
			}
			
			return (JSONObject) event.get("tags");
		}
		
		/**
		 * 
		 * @param serverName
		 * @return
		 */
		public SentryEventBuilder setServerName(String serverName) {
			event.put("server_name", serverName);
			return this;
		}
		
		/**
		 * 
		 * @param modules
		 * @return
		 */
		public SentryEventBuilder setModules(List<String> modules) {
			event.put("modules", new JSONArray(modules));
			return this;
		}
		
		/**
		 * 
		 * @param extra
		 * @return
		 */
		public SentryEventBuilder setExtra(Map<String,String> extra) {
			setExtra(new JSONObject(extra));
			return this;
		}
		
		public SentryEventBuilder setExtra(JSONObject extra) {
			event.put("extra", extra);
			return this;
		}
		
		public JSONObject getExtra() {
			if (!event.containsKey("extra")) {
				setExtra(new HashMap<String, String>());
			}
			
			return (JSONObject) event.get("extra");
		}
		
		public SentryEventBuilder setModule(String name, String version) {
			if(!event.containsKey("modules")){
				event.put("modules", new HashMap<String, String>());
			}
			((Map<String, String>) event.get("modules")).put(name, version);
			return this;
		} 
		
		public JSONObject getModule() {
			if(!event.containsKey("modules")){
				event.put("modules", new HashMap<String, String>());
			}
			return new JSONObject((Map<String, String>) event.get("modules"));
		}
		
		public SentryEventBuilder setChecksum(String checksum) {
			event.put("checksum", calculateChecksum(checksum));
			return this;
		}
		
		public String getChecksum(){
			return (String) event.get("checksum");
		}
		
		/**
		 *
		 * @param t
		 * @return
		 */
		public SentryEventBuilder setException(Throwable t) {
			ArrayList<JSONObject> array = new ArrayList<JSONObject>();
			
			while(t != null){
				Map<String, Object> exception = new HashMap<String, Object>();
				exception.put("type", t.getClass().getName());
				exception.put("value", t.getMessage());
				try {
					exception.put("stacktrace", getStackTrace(t));
				} catch (JSONException e) { e.printStackTrace(); }
				array.add(new JSONObject(exception));
				t = t.getCause();
			}
			
			Collections.reverse(array);
			
			event.put("exception", new JSONArray(array));
			
			return this;
		}
		
		public static JSONObject getStackTrace(Throwable t) throws JSONException {
			ArrayList<JSONObject> array = new ArrayList<JSONObject>();
			Collection<String> notInApp = getNotInAppFrames();
			
			StackTraceElement[] elements = t.getStackTrace();
			for (int index = 0; index < elements.length; ++index) {
				StackTraceElement element = elements[index];
				JSONObject frame = new JSONObject();
				// raven-java does not display filename as it will replace module name
				// https://github.com/kencochrane/raven-java/blob/master/raven/src/main/java/net/kencochrane/raven/marshaller/json/StackTraceInterfaceBinding.java#L36
				//frame.put("filename", element.getFileName());
				frame.put("module", element.getClassName());
				frame.put("function", element.getMethodName());
				frame.put("lineno", element.getLineNumber());
				
				boolean inApp = true;
				for(String notInAppItem : notInApp){
					if(element.getClassName().startsWith(notInAppItem)){
						inApp = false;
					}
				}
				frame.put("in_app", inApp);
				
				array.add(frame);
			}
				
			Collections.reverse(array);
			JSONObject stackTrace = new JSONObject();
			stackTrace.put("frames", new JSONArray(array));
			return stackTrace;
		}
		
		// source: net.kencochrane.raven.event.EventBuilder
		private static String calculateChecksum(String string) {
			try{
				byte[] bytes = string.getBytes("UTF-8");
				Checksum checksum = new CRC32();
				checksum.update(bytes, 0, bytes.length);
				return Long.toHexString(checksum.getValue()).toUpperCase();
			}catch(UnsupportedEncodingException e){
				return "";
			}
		}
		
		// source: net.kencochrane.raven.DefaultRavenFactory
		protected static Collection<String> getNotInAppFrames() {
			return Arrays.asList(
					"com.sun.",
					"java.",
					"javax.",
					"org.omg.",
					"sun.",
					"junit.",
					// android specific
					"com.android.",
					"android.",
					"com.google.",
					"libcore.",
					"dalvik.",
					"map.",
					// app specific
					Sentry.class.getPackage().getName()
			);
		}
		
	}

}
