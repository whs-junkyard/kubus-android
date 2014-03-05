package th.in.whs.ku.bus.util;

import java.util.Arrays;

import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Find the nearest polyline
 * @param The first is the target point. The rest are polylines. All in [lat,lon] format.
 * They are assumed as in a closed loop
 * @return A line [[lat0, lon0], [lat1, lon1]]. lat1/lon1 will be existing polyline point 
 */
public class NearestLineToPoint extends AsyncTask<Double[], Void, Double[][]> {

	@Override
	protected Double[][] doInBackground(Double[]... params) {
		double[] target = toPrimitive(params[0]);
		
		int minIndex = 1;
		float minLength = Float.MAX_VALUE;
		double[] minProjection = null;
		// start from 1 -> 2
		for(int i=1; i < params.length; i++){
			double[][] polyline;
			if(i == 1){
				polyline = new double[][]{
					toPrimitive(params[params.length - 1]),
					toPrimitive(params[i])
				};
			}else{
				polyline = new double[][]{
					toPrimitive(params[i-1]),
					toPrimitive(params[i])
				};
			}
			
			double[] projection = pointProjection(target, polyline);
			
			if(minProjection == null){
				minProjection = projection;
			}
			float length = distanceToPoint(target, projection);
			if(length < minLength){
				minLength = length;
				minIndex = i;
				minProjection = projection;
			}
		}
		
		if(minProjection == null){
			return null;
		}
		Log.d("NearestLineToPoint", "Returning with distance " + minLength);
		return new Double[][]{
			new Double[]{
				minProjection[0],
				minProjection[1]
			},
			params[minIndex]
		};
		
	}
	
	private double[] toPrimitive(Double[] arr){
		double[] out = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			out[i] = arr[i];
		}
		return out;
	}
	
	private float distanceToPoint(double[] point, double[] projection){
		if(projection == null){
			return Float.MAX_VALUE;
		}
		float[] out = new float[1];
		Location.distanceBetween(point[0], point[1], projection[0], projection[1], out);
		return out[0];
	}
	
	/**
	 * https://stackoverflow.com/questions/15232356/projection-of-a-point-on-line-defined-by-2-points
	 * @param point
	 * @param polyline
	 * @return point or null
	 */
	private double[] pointProjection(double[] point, double[][] polyline){
		if(Arrays.equals(polyline[0], polyline[1])){
			polyline[0][0] -= 0.00001;
		}
		
		double U = ((point[0] - polyline[0][0]) * (polyline[1][0] - polyline[0][0])) 
				+ ((point[1] - polyline[0][1]) * (polyline[1][1] - polyline[0][1]));
		double Udenom = Math.pow(polyline[1][0] - polyline[0][0], 2) 
				+ Math.pow(polyline[1][1] - polyline[0][1], 2);
		U /= Udenom;
		
		double out[] = {
				polyline[0][0] + (U * (polyline[1][0] - polyline[0][0])),
				polyline[0][1] + (U * (polyline[1][1] - polyline[1][1]))
		};
		
		return out;
	}
	
}
