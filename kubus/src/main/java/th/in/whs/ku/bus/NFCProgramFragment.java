package th.in.whs.ku.bus;

import java.io.IOException;

import th.in.whs.ku.bus.util.NFCBuilder;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class NFCProgramFragment extends Fragment implements NFCHandlerInterface {

	protected static final int PICK_STOP_CODE = 1543;
	
	private NfcAdapter adapter;
	private PendingIntent pendingIntent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = NfcAdapter.getDefaultAdapter(getActivity());
		pendingIntent = PendingIntent.getActivity(
				getActivity(), 0, new Intent(getActivity(), getActivity().getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
	}

	@Override
	public void onPause() {
		super.onPause();
		adapter.disableForegroundDispatch(getActivity());
	}

	@Override
	public void onResume() {
		super.onResume();
		adapter.enableForegroundDispatch(getActivity(), pendingIntent, null, null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.nfcprogram, container, false);
	}

	@Override
	public void onViewCreated(final View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		view.findViewById(R.id.writeProtect).setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View view) {
				final CheckBox check = (CheckBox) view;
				if(check.isChecked()){
					AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

					alert.setTitle(R.string.write_protect_confirm_title);
					alert.setMessage(R.string.write_protect_confirm_body);
					alert.setCancelable(false);

					alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					});

					alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							check.setChecked(false);
						}
					});

					alert.show();
				}
			}

		});
		view.findViewById(R.id.pickStop).setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(getActivity(), BusStopListActivity.getCompatClass());
				startActivityForResult(intent, PICK_STOP_CODE);
			}

		});
		view.findViewById(R.id.home).setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				EditText txt = (EditText) view.findViewById(R.id.tagUri);
				txt.setText("kubus://home");
			}

		});
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == PICK_STOP_CODE && data != null){
			String id = data.getStringExtra("id");
			if(id == null){
				return;
			}
			EditText txt = (EditText) getView().findViewById(R.id.tagUri);
			txt.setText("kubus://stop/"+id);
		}
	}

	@Override
	public void onTagReceived(final Tag tag) {
		String uriString = ((EditText) getView().findViewById(R.id.tagUri)).getText().toString();
		if(uriString.isEmpty()){
			return;
		}
		Uri uri = Uri.parse(uriString);
		final NdefMessage message = NFCBuilder.createMessage(getActivity(), uri);
		final TextView log = (TextView) getView().findViewById(R.id.log);
		final boolean readonly = ((CheckBox) getView().findViewById(R.id.writeProtect)).isChecked();
		new Thread(new Runnable(){

			@Override
			public void run() {
				int errorCode = 0;
				try {
					NFCBuilder.writeToTag(message, tag, readonly);
				} catch (TagLostException e) {
					Log.e("NFCProgramFragment", "Tag Lost", e);
					errorCode = R.string.nfc_lost;
				} catch (IOException e) {
					Log.e("NFCProgramFragment", "IO Error", e);
					errorCode = R.string.nfc_io_error;
				} catch (FormatException e) {
					Log.e("NFCProgramFragment", "Message malformat", e);
					errorCode = R.string.nfc_message_error;
				}
				if(errorCode != 0){
					log.post(new UpdateLog(errorCode));
				}
				log.post(new UpdateLog("Program Success!\n\n" + getTagInfo(tag, message)));
			}
			
		}).start();
	}
	
	private String getTagInfo(Tag tag, NdefMessage message){
		return tag.toString()+"\n\n"+new String(message.toByteArray());
	}
		
	private class UpdateLog implements Runnable{

		private CharSequence str;
		
		public UpdateLog(CharSequence str){
			this.str = str;
		}
		
		public UpdateLog(int errorCode){
			this.str = getString(errorCode);
		}
		
		@Override
		public void run() {
			TextView log = (TextView) getView().findViewById(R.id.log);
			log.setText(str);
		}
		
	}

}
