package th.in.whs.ku.bus.map;

import th.in.whs.ku.bus.R;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class StopIconCache extends BitmapCache<String> {

	public static final StopIconCache instance = new StopIconCache();
	
	@Override
	public BitmapDescriptor getCache(String lineId){
		if(lineId.equals("6")){
			lineId = "4";
		}
		return super.get(lineId);
	}

	@Override
	protected BitmapDescriptor create(String lineId) {
		int id = Integer.valueOf(lineId);
		switch(id){
		case 1:
			return BitmapDescriptorFactory.fromResource(R.drawable.pin_red);
		case 2:
			return BitmapDescriptorFactory.fromResource(R.drawable.pin_blue);
		case 3:
			return BitmapDescriptorFactory.fromResource(R.drawable.pin_green2);
		case 4:
			return BitmapDescriptorFactory.fromResource(R.drawable.pin_yellow);
		case 5:
			return BitmapDescriptorFactory.fromResource(R.drawable.pin_black);
		default:
			return BitmapDescriptorFactory.fromResource(R.drawable.pin_gray);
		}
	}
}
