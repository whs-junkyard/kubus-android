package th.in.whs.ku.bus.api;

import android.content.Context;
import th.in.whs.ku.bus.R;

/**
 * Not really ago, more like time soon
 * @author whs
 */
public class TimeAgo {
	/**
	 * Time interval in seconds with milliseconds in floating point
	 */
	private double milli;
	/**
	 * The time which the interval starts count from in unix time in seconds
	 */
	private double since_time = 0;
	public TimeAgo(){
		milli = 0;
	}
	public TimeAgo(String time){
		milli = TimeAgo.parse(time);
		since_time = System.currentTimeMillis() / 1000;
	}
	public TimeAgo(double milli, double since_time){
		this.milli = milli;
		this.since_time = since_time;
	}
	public TimeAgo(double milli){
		this.milli = milli;
	}
	
	/**
	 * Parse time string to double
	 * @param time eg.00:09:34.611863
	 */
	public static double parse(String time){
		String[] times = time.split(":");
		double out = Double.parseDouble(times[2]);
		out += Integer.parseInt(times[1]) * 60;
		out += Integer.parseInt(times[0]) * 60 * 60;
		return out;
	}
	
	public int getMinuteLeft(){
		return (int) Math.floor(getTimeLeft() / 60.0);
	}
	
	public String toString(Context context){
		String out = "";
		double milli = Math.abs(this.milli);
		if(since_time > 0){
			milli = milli - ((System.currentTimeMillis()/1000) - since_time);
		}
		
		int hour = (int) Math.floor(milli / (60*60));
		if(hour > 0){
			out += String.format("%d %s ", hour, context.getString(hour == 1 ? R.string.hour : R.string.hours));
		}
		milli -= hour * 60 * 60;
		
		int minute = (int) Math.floor(milli / 60);
		if(minute > 0){
			out += String.format("%d %s ", minute, context.getString(minute == 1 ? R.string.minute : R.string.minutes));
		}
		milli -= minute * 60;
		
		int second = (int) Math.ceil(milli);
		
		if(second > 0 && minute < 1){
			return context.getString(R.string.lt_min);
		}
		
		/*if(second > 0){
			out += String.format("%d %s ", second, context.getString(second == 1 ? R.string.second : R.string.seconds));
		}*/
		
		return out.trim();
	}
	public double getMilli() {
		return milli;
	}
	public double getSinceTime() {
		return since_time;
	}
	public double getTimeLeft() {
		double milli = Math.abs(this.milli);
		if(since_time > 0){
			milli = milli - ((System.currentTimeMillis()/1000) - since_time);
		}
		return milli;
	}
}
