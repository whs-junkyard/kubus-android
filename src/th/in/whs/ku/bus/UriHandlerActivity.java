package th.in.whs.ku.bus;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Parcelable;
import android.widget.Toast;

public class UriHandlerActivity extends Activity {

	@Override
	protected void onResume() {
		super.onResume();
		
		if(getIntent() == null || getIntent().getAction() == null){
			finish();
			return;
		}
		
		if(getIntent().getAction().equals(Intent.ACTION_VIEW)){
			Uri uri = this.getIntent().getData();
			handleUri(uri);
		}
		
		handleNfc();
	}
	
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	private void handleNfc(){
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD){
			return;
		}
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			NdefMessage msg = (NdefMessage) rawMsgs[0];
			for(NdefRecord record : msg.getRecords()){
				if(!Arrays.equals(NdefRecord.RTD_URI, record.getType())){
					continue;
				}
				String payload;
				try {
					payload = new String(record.getPayload(), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					payload = new String(record.getPayload());
				}
				Uri uri = Uri.parse(payload);
				handleUri(uri);
				break;
			}
	    }
	}
	
	private void handleUri(Uri uri){
		if(uri == null){
			invalidUri();
			return;
		}
		
		String host = uri.getHost();
		String path = uri.getPath();
		
		if(host.equals("stop") && path != null){ // kubus://stop/1 (no trailing slash
			openStopPage(path.substring(1));
		}else if(host.equals("home")){ // kubus://home
			Intent intent = getIntent(MainActivity.class);
			startActivity(intent);
			finish();
		}else if(host.equals("nfc")){
			Intent intent = getIntent(NFCProgramActivity.class);
			startActivity(intent);
			finish();
		}else{
			invalidUri();
			return;
		}
	}
	
	private void invalidUri(){
		Toast.makeText(this, android.R.string.httpErrorBadUrl, Toast.LENGTH_LONG).show();
		finish();
	}
	
	private void openStopPage(String id){
		Intent intent = getIntent(MainActivity.class);
		intent.putExtra(MainActivity.OPEN_STOP, id);
		startActivity(intent);
		finish();
	}
	
	private Intent getIntent(Class<? extends Activity> cls){
		Intent intent = new Intent(this, cls);
		//intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		return intent;
	}
	
}
