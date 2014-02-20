package th.in.whs.ku.bus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;

import th.in.whs.ku.bus.api.API;
import th.in.whs.ku.bus.api.Bus;
import th.in.whs.ku.bus.api.BusStatus;
import th.in.whs.ku.bus.api.BusStopList;
import th.in.whs.ku.bus.api.TimeAgo;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

public class ThereFragment extends Fragment implements OnItemClickListener {

	private static final int SELECT_INTENT_CODE = 673;
	/**
	 * Duration which startTimeupdate() will be fired
	 */
	private static final int TIMER_UPDATE_MS = 500;

	private Button[] buttons = new Button[2];
	private int selectingIndex;
	private String[] dest = new String[2];
	private float[] percentLoaded;
	private JSONArray[] data;
	private ListView listView;
	private ArrayList<BusStatus> list;
	private LayoutInflater inflater;
	private Handler handler;
	private Bundle savedData;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		buttons[0] = (Button) getView().findViewById(R.id.from);
		buttons[0].setOnClickListener(new BusStopSelectHandler(0));
		buttons[1] = (Button) getView().findViewById(R.id.to);
		buttons[1].setOnClickListener(new BusStopSelectHandler(1));

		listView = (ListView) getView().findViewById(R.id.busList);
		list = new ArrayList<BusStatus>();
		BusStopAdapter adapter = new BusStopAdapter(getActivity(), list);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
		
		final View notifyBtn = getView().findViewById(R.id.notifyBtn);
		notifyBtn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				String regId = GCMController.getInstance().getRegID();
				if(regId == null || regId.length() == 0){
					AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
					alert.setMessage(R.string.gcm_not_avail);
					alert.show();
					return;
				}
				notifyBtn.setEnabled(false);
				RequestParams params = new RequestParams();
				params.add("stop", dest[0]);
				List<String> passing = BusStopList.getPassingLine(dest[0], dest[1]);
				for(String lineId : passing){
					params.add("line[]", lineId);
				}
				params.add("backend", "gcm");
				params.add("gcm_id", regId);
				API.registerNotify(params, new AsyncHttpResponseHandler(){

					@Override
					public void onSuccess(String content) {
						if(!content.equals("ok")){
							AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
							alert.setMessage(content);
							alert.show();
							notifyBtn.setEnabled(true);
						}else{
							// http://sentry.whs.in.th/kusmartbus/android/group/158/
							if(getActivity() != null){
								Toast.makeText(getActivity(), R.string.notify_ok, Toast.LENGTH_LONG).show();
							}
						}
					}

					@Override
					public void onFailure(int statusCode, Throwable error,
							String content) {
						// http://sentry.whs.in.th/kusmartbus/android/group/124/
						if(getActivity() != null){
							Toast.makeText(getActivity(), R.string.internet_error, Toast.LENGTH_LONG).show();
						}
						Log.e("ThereFragment", "Unable to register for notification", error);
					}
					
				});
			}
			
		});
		
		if(savedInstanceState != null){
			restoreViewState(savedInstanceState.getBundle("viewState"));
		}
		
		if (dest[0] == null) {
			autoSelectFromStop();
		}

		handler = new Handler();
	}
	
	@Override
	public void onPause(){
		super.onPause();
		setSavedData(saveViewState());
	}
	
	private void setSavedData(Bundle data){
		if(data == null){
			return;
		}
		savedData = data;
	}

	@Override
	public void onResume() {
		super.onResume();
		if(savedData != null){
			restoreViewState(savedData);
			savedData = null;
		}
		startTimeupdate();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		this.inflater = inflater;
		return inflater.inflate(R.layout.there, container, false);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		API.cancel(getActivity());
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putStringArray("dest", dest);
		outState.putInt("selectingIndex", selectingIndex);
		outState.putFloatArray("percentLoaded", percentLoaded);
		if (data != null) {
			String[] dataArray = new String[data.length];
			for (int i = 0; i < data.length; i++) {
				if (data[i] != null) {
					dataArray[i] = data[i].toString();
				}
			}
			outState.putStringArray("data", dataArray);
		}
		outState.putBundle("viewState", saveViewState());
	}
	
	private Bundle saveViewState(){
		Bundle out = new Bundle();
		if(buttons == null){
			return null;
		}
		String[] buttonTxt = new String[buttons.length];
		for (int i = 0; i < buttons.length; i++) {
			if(buttons[i] == null){
				return null;
			}
			buttonTxt[i] = (String) buttons[i].getText();
		}
		out.putStringArray("buttons", buttonTxt);
		out.putParcelableArrayList("list", list);
		if(getView() != null){
			out.putString("lines", (String) ((TextView) getView()
				.findViewById(R.id.busLine)).getText());
			out.putBoolean("loading", getView().findViewById(R.id.progress).getVisibility() == View.VISIBLE);
			out.putBoolean("canNotify", getView().findViewById(R.id.notifyBtn).isEnabled());
		}
		return out;
	}
	
	private void restoreViewState(Bundle state){
		if(state == null){
			return;
		}
		list = state.getParcelableArrayList("list");
		BusStopAdapter adapter = new BusStopAdapter(getActivity(), list);
		listView.setAdapter(adapter);

		TextView busLine = (TextView) getView().findViewById(R.id.busLine);
		// these if for empty is from that
		// onActivityResult is fired before onResume thus the label will be
		// overwritten by the saved state
		if(busLine.getText().equals("")){
			busLine.setText(state.getString("lines"));
			getView().findViewById(R.id.notifyBtn).setEnabled(state.getBoolean("canNotify"));
		}
		
		setShowProgress(state.getBoolean("loading"));

		String[] buttonTxt = state.getStringArray("buttons");
		for (int i = 0; i < buttons.length; i++) {
			if(buttons[i].getText().equals("")){
				buttons[i].setText(buttonTxt[i]);
			}
		}

		if (isBothStopSelected()) {
			View noBus = getView().findViewById(R.id.nobus);
			if (list.size() == 0) {
				noBus.setVisibility(View.VISIBLE);
			} else {
				noBus.setVisibility(View.GONE);
			}
		}
		
		if(percentLoaded != null){
			updatePercentage();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			return;
		}
		dest = savedInstanceState.getStringArray("dest");
		selectingIndex = savedInstanceState.getInt("selectingIndex");
		percentLoaded = savedInstanceState.getFloatArray("percentLoaded");
		String[] dataArray = savedInstanceState.getStringArray("data");
		if (dataArray != null) {
			data = new JSONArray[dataArray.length];
			for (int i = 0; i < dataArray.length; i++) {
				if (dataArray[i] == null) {
					continue;
				}
				try {
					data[i] = new JSONArray(dataArray[i]);
				} catch (JSONException e) {
				}
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//Fragment x= (Fragment) crashy();
		if (requestCode == SELECT_INTENT_CODE && data != null) {
			buttons[selectingIndex].setText(data.getStringExtra("name"));
			dest[selectingIndex] = data.getStringExtra("id");
			if (isBothStopSelected()) {
				loadBusInfo();
			}
			return;
		}
	}
	
	private Object crashy(){return "lei";}

	/**
	 * Timer to update duration until bus arrive in the view
	 */
	private void startTimeupdate() {
		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				if (!isVisible()) {
					return;
				}
				((ArrayAdapter) listView.getAdapter()).notifyDataSetChanged();
				startTimeupdate();
			}

		}, TIMER_UPDATE_MS);
	}

	private void autoSelectFromStop() {
		openStopList(0, true);
	}

	private boolean isBothStopSelected() {
		for (String stop : dest) {
			if (stop == null) {
				return false;
			}
		}
//		if (dest[0].equals(dest[1])) {
//			return false;
//		}
		return true;
	}

	private void loadBusInfo() {
		loadBusInfo(dest[0], dest[1]);
	}

	private void loadBusInfo(String idFrom, String idTo) {
		if(getView() == null){
			return;
		}
		percentLoaded = new float[2];
		data = new JSONArray[2];

		API.cancel(getActivity());

		ArrayAdapter adapter = (ArrayAdapter) listView.getAdapter();
		adapter.clear();
		adapter.notifyDataSetChanged();

		loadAStopInfo(idFrom, 0);
		loadAStopInfo(idTo, 1);
		updatePercentage();
		setShowProgress(true);

		TextView passing = (TextView) getView().findViewById(R.id.busLine);
		List<String> passingLines = BusStopList.getPassingLine(dest[0], dest[1]);
		getView().findViewById(R.id.notifyBtn).setEnabled(passingLines.size() > 0);
		if (passingLines.size() == 0) {
			passing.setText(R.string.no_bus);
		} else {
			Collections.sort(passingLines);
			StringBuilder passingLinesText = new StringBuilder();
			Iterator passingLinesIterator = passingLines.iterator();
			while (passingLinesIterator.hasNext()) {
				passingLinesText.append(passingLinesIterator.next());
				if (passingLinesIterator.hasNext()) {
					passingLinesText.append(", ");
				}
			}
			passing.setText(passingLinesText);
		}
	}

	private void loadAStopInfo(String id, final int index) {
		RequestParams params = new RequestParams();
		params.put("busstationid", id);
		Log.d("ThereFragment", "loading stop " + id + " to " + index);
		API.get(getActivity(), "map/getBusStatusData", params,
				new JsonHttpResponseHandler() {
					@Override
					public void onSuccess(int statusCode, Header[] headers,
							JSONArray loadedData) {
						data[index] = loadedData;
						processData();
					}

					@Override
					public void onProgress(int bytesWritten, int totalSize) {
						percentLoaded[index] = (float) bytesWritten
								/ (float) totalSize;
						updatePercentage();
					}

					@Override
					public void onFailure(int statusCode, Header[] headers,
							String responseString, Throwable throwable) {
						Toast.makeText(getActivity(), R.string.internet_error,
								Toast.LENGTH_LONG).show();
						Log.e("ThereFragment", "Error in " + index, throwable);
					}
				});
	}

	private void updatePercentage() {
		if (getActivity() == null || percentLoaded.length == 0) {
			return;
		}
		float sum = 0;
		for (float one : percentLoaded) {
			sum += one;
		}
		sum /= (float) percentLoaded.length;
		getActivity().setProgress((int) Math.ceil(sum * 10000));
	}

	private void processData() {
		for (JSONArray aData : data) {
			if (aData == null) {
				return;
			}
		}

		// make a list of busid in destination stop
		int length = data[1].length();
		ArrayList<Integer> busId = new ArrayList<Integer>(length);
		for (int i = 0; i < length; i++) {
			try {
				BusStatus bus = new BusStatus(data[1].getJSONObject(i));
				if(bus.getBus() != null && bus.getBus().isinpark){
					continue;
				}
				busId.add(bus.id);
			} catch (JSONException e) {
			}
		}

		// interate starting stop and find intersecting bus
		ArrayAdapter adapter = (ArrayAdapter) listView.getAdapter();
		adapter.clear();
		length = data[0].length();
		for (int i = 0; i < length; i++) {
			try {
				BusStatus bus = new BusStatus(data[0].getJSONObject(i));
				if (busId.contains(bus.id)) {
					adapter.add(bus);
				}
			} catch (JSONException e) {
			}
		}
		adapter.notifyDataSetChanged();

		setShowProgress(false);
		
		if(getView() == null){
			return;
		}

		View noBus = getView().findViewById(R.id.nobus);
		if (adapter.getCount() == 0) {
			noBus.setVisibility(View.VISIBLE);
		} else {
			noBus.setVisibility(View.GONE);
		}
	}

	private void openStopList(int type) {
		openStopList(type, false);
	}

	/**
	 * Show or hide the progress circle
	 * 
	 * @param show
	 *            Show the progress circle or not
	 */
	private void setShowProgress(boolean show) {
		if (getView() == null) {
			return;
		}
		View progress = getView().findViewById(R.id.progress);
		if (progress == null) {
			return;
		}
		if (show) {
			progress.setVisibility(View.VISIBLE);
		} else {
			progress.setVisibility(View.GONE);
		}
	}

	private void openStopList(int type, boolean returnClosest) {
		selectingIndex = type;
		Intent intent = new Intent(getActivity(), BusStopListActivity.class);
		intent.putExtra("type", type);
		intent.putExtra("returnClosest", returnClosest);
		startActivityForResult(intent, SELECT_INTENT_CODE);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		BusStatus bus = this.list.get(position);
		
		Intent intent = new Intent(getActivity(), RouteMapActivity.class);
		intent.putExtra("line", bus.lineid);
		intent.putExtra("bus", bus.id);
		intent.putExtra("from", dest[0]);
		intent.putExtra("to", dest[1]);
		this.startActivity(intent);
	}

	private final class BusStopSelectHandler implements OnClickListener {
		private int index;

		public BusStopSelectHandler(int index) {
			this.index = index;
		}

		@Override
		public void onClick(View arg0) {
			openStopList(index);
		}
	}

	private class BusStopAdapter extends ArrayAdapter<BusStatus> {
		public BusStopAdapter(Context context, ArrayList<BusStatus> list) {
			super(context, R.layout.busline_row, list);
		}

		@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
		@SuppressWarnings("deprecation")
		public View getView(int position, View convertView, ViewGroup parent) {
			View vi = convertView;
			if (convertView == null) {
				vi = inflater.inflate(R.layout.busline_row, null);
			}
			BusStatus data = this.getItem(position);

			TextView busName = (TextView) vi.findViewById(R.id.busName);
			TextView busNo = (TextView) vi.findViewById(R.id.busNo);
			TextView lineNo = (TextView) vi.findViewById(R.id.lineNo);

			String timeString;
			try {
				TimeAgo time = data.estimated_time;
				int min = time.getMinuteLeft();
				if(min == 0){
					timeString = getString(R.string.lt_min);
				}else{
					timeString = String.valueOf(min);
				}
			} catch (Exception e) {
				timeString = getString(R.string.unknown_time);
			}
			busNo.setText(timeString);

			Bus bus = data.getBus();
			if(bus != null){
				busName.setText(bus.name);
				
				NinePatchDrawable background = (NinePatchDrawable) getActivity().getResources().getDrawable(R.drawable.cards);
				int color = getResources().getColor(bus.getColor());
				background.setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY);
				View bgView = vi.findViewById(R.id.busLineRowBg);
				if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
					bgView.setBackground(background);
				}else{
					bgView.setBackgroundDrawable(background);
				}
			}else{
				busName.setText(data.name);
			}
			lineNo.setText(data.linename);

			return vi;
		}
	}
}
