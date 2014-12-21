package th.in.whs.ku.bus.util;

import th.in.whs.ku.bus.R;

public class BusColor {
	/**
	 * Helper method to get bus color
	 * @param lineid
	 * @return Color
	 */
	public static int getColor(int lineid){
    	switch(lineid){
		case 1:
			return R.color.line1;
		case 2:
			return R.color.line2;
		case 3:
			return R.color.line3;
		case 4:
			return R.color.line4;
		case 5:
			return R.color.line5;
		case 6:
			return R.color.line4;
		default:
			return R.color.gray;
		}
    }
}
