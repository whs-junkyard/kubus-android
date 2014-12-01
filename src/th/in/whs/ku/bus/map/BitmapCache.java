package th.in.whs.ku.bus.map;

import android.support.v4.util.LruCache;

import com.google.android.gms.maps.model.BitmapDescriptor;

public abstract class BitmapCache<T> extends LruCache<T, BitmapDescriptor> {
	public BitmapCache() {
		super(getCacheSize());
	}
	
	public BitmapDescriptor getCache(T key){
		return super.get(key);
	}
	
	protected static int getCacheSize(){
		return 20;
	}
}
