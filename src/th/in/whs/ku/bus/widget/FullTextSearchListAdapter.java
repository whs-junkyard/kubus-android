package th.in.whs.ku.bus.widget;

import java.util.ArrayList;
import java.util.List;

import th.in.whs.ku.bus.widget.FullTextSearchListAdapter.SearchableItem;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

public class FullTextSearchListAdapter<T extends SearchableItem> extends ArrayAdapter<T>{
	
	private Filter mFilter;
	private List<T> objects;
	private List<T> objectsShow;

	public FullTextSearchListAdapter(Context context,
			int layout,
			List<T> objects) {
		super(context, layout, objects);
		this.objects = objects;
		this.objectsShow = new ArrayList<T>(objects);
	}
	
	@Override
	public void notifyDataSetChanged(){
		this.objectsShow = new ArrayList<T>(objects);
		super.notifyDataSetChanged();
	}
	
	/**
	 * Used in search
	 */
	private void dataSetChanged(){
		super.notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return this.objectsShow.size();
	}

	@Override
	public T getItem(int position) {
		return this.objectsShow.get(position);
	}
	
	@Override
	public Filter getFilter(){
		if(mFilter == null){
			mFilter = new CustomFilter();
		}
		return mFilter;
	}
	
	private class CustomFilter extends Filter {
		@Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            objectsShow = (List<T>) results.values;
            if (results.count > 0) {
            	dataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults results = new FilterResults();
			
			if(constraint == null || constraint.length() == 0){
				results.values = new ArrayList<T>(objects);
				results.count = objects.size();
			}else{
				String search = constraint.toString().toLowerCase();
				ArrayList<SearchableItem> result = new ArrayList<SearchableItem>();
				results.values = result;
				
				for(SearchableItem item : objects){
					String name = item.getSearchString();
					if(name.toLowerCase().contains(search)){
						result.add(item);
					}
				}
				
				results.count = result.size();
			}
			
			return results;
		}
	}
	
	public static interface SearchableItem {
		String getSearchString();
	}

}