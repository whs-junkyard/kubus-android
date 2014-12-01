package th.in.whs.ku.bus.map;

import th.in.whs.ku.bus.R;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class BusIconCache extends BitmapCache<Integer> {

	public static final BusIconCache instance = new BusIconCache();
	
	@Override
	public BitmapDescriptor getCache(Integer lineId){
		if(lineId == 6){
			lineId = 4;
		}
		return super.getCache(lineId);
	}

	@Override
	protected BitmapDescriptor create(Integer lineId) {
		switch(lineId){
		case 1:
			return BitmapDescriptorFactory.fromResource(R.drawable.bus_red);
		case 2:
			return BitmapDescriptorFactory.fromResource(R.drawable.bus_blue);
		case 3:
			return BitmapDescriptorFactory.fromResource(R.drawable.bus_green);
		case 4:
			return BitmapDescriptorFactory.fromResource(R.drawable.bus_yellow);
		case 5:
			return BitmapDescriptorFactory.fromResource(R.drawable.bus_black);
		default:
			return BitmapDescriptorFactory.fromResource(R.drawable.bus_gray);
		}
	}
}
