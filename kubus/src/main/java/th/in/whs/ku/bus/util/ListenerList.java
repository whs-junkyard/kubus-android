package th.in.whs.ku.bus.util;

import android.os.Bundle;
import android.util.SparseArray;

public class ListenerList {
	private SparseArray<Listener> listeners;
	private int lastId;
	
	public ListenerList(){
		listeners = new SparseArray<Listener>();
	}
	
	public synchronized int register(Listener listener, boolean fire, boolean wantFire){
		int out;
		out = register(listener);
		if(fire && wantFire){
			listener.onFired();
		}
		return out;
	}
	public synchronized int register(Listener listener){
		int out;
		synchronized(listeners){
			out = lastId++;
			listeners.append(out, listener);
		}
		return out;
	}
	
	public void copyTo(ListenerList target, boolean fire){
		synchronized(listeners){
			for(int i=0; i<listeners.size(); i++){
				int key = listeners.keyAt(i);
				target.listeners.append(key, listeners.valueAt(i));
				if(fire){
					listeners.valueAt(i).onFired();
				}
			}
		}
	}
	
	public void remove(int ID){
		listeners.remove(ID);
	}
	
	public void remove(Listener listener){
		listeners.remove(listeners.indexOfValue(listener));
	}
	
	public void copyFrom(ListenerList source, boolean fire){
		source.copyTo(this, fire);
	}
	
	public void fire(){
		for(int i=0; i<listeners.size(); i++){
			listeners.valueAt(i).onFired();
		}
	}
	public void fire(Bundle data){
		for(int i=0; i<listeners.size(); i++){
			listeners.valueAt(i).onFired(data);
		}
	}
	
	public int size(){
		return listeners.size();
	}
	
	public static abstract class Listener {
		public abstract void onFired();
		public void onFired(Bundle data){}
	}

}

