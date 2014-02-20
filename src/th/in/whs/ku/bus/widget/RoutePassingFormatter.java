package th.in.whs.ku.bus.widget;

import java.util.Collections;
import java.util.List;

import org.json.JSONException;

import th.in.whs.ku.bus.BusMapController;
import th.in.whs.ku.bus.R;
import th.in.whs.ku.bus.api.BusStopList;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;

public class RoutePassingFormatter {
	public static CharSequence getRoutePassingFormatted(Context context, String from, String to) {
		return getRoutePassingFormatted(context, BusStopList.getPassingLine(from, to));
	}
	
	public static CharSequence getRoutePassingFormatted(Context context, String id) {
		return getRoutePassingFormatted(context, BusStopList.getPassingLine(id));
	}
	
	public static CharSequence getRoutePassingFormatted(Context context, List<String> passingLines) {
		String prefix = context.getString(R.string.line_passing) + " ";
		SpannableStringBuilder out = new SpannableStringBuilder(prefix);
		
		Collections.sort(passingLines);
		
		for(String line : passingLines){
			int lengthStart = out.length()+1;
			out.append("  " + line + "  ");
			int lengthStop = out.length()-1;
			out.setSpan(new BackgroundColorSpan(context.getResources().getColor(BusMapController.getColor(Integer.valueOf(line)))), lengthStart, lengthStop, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
			out.setSpan(new ForegroundColorSpan(Color.WHITE), lengthStart, lengthStop, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
		}
		
		return out;
	}
}
