package th.in.whs.ku.bus.map;

import th.in.whs.ku.bus.R;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

public class DirectionIconCache extends BitmapCache<String> {
	
	public static final DirectionIconCache instance = new DirectionIconCache();
	
	@Override
	public BitmapDescriptor getCache(String lineId){
		if(lineId.equals("6")){
			lineId = "4";
		}
		return super.get(lineId);
	}

	@Override
	protected BitmapDescriptor create(String lineId) {
		switch(Integer.parseInt(lineId)){
		case 1:
			return BitmapDescriptorFactory.fromResource(R.drawable.chevron_red);
		case 2:
			return BitmapDescriptorFactory.fromResource(R.drawable.chevron_blue);
		case 3:
			return BitmapDescriptorFactory.fromResource(R.drawable.chevron_green);
		case 4:
			return BitmapDescriptorFactory.fromResource(R.drawable.chevron_yellow);
		case 5:
			return BitmapDescriptorFactory.fromResource(R.drawable.chevron_black);
		default:
			return BitmapDescriptorFactory.fromResource(R.drawable.chevron_gray);
		}
	}

}
