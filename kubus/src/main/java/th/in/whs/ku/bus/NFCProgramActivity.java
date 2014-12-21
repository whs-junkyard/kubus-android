package th.in.whs.ku.bus;

import android.annotation.TargetApi;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
public class NFCProgramActivity extends ActionBarActivity {
	
	private Fragment fragment = new NFCProgramFragment();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			Toast.makeText(this, R.string.nfc_min_ver, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		if(NfcAdapter.getDefaultAdapter(this) == null){
			Toast.makeText(this, R.string.no_nfc, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		
		if(getSupportFragmentManager().findFragmentByTag("NFCProgram") != null){
			return;
		}
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.add(android.R.id.content, fragment, "NFCProgram");
		ft.commit();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		Fragment fragment = getSupportFragmentManager().findFragmentByTag("NFCProgram");
		if(fragment instanceof NFCHandlerInterface){
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			if(tag != null){
				((NFCHandlerInterface) fragment).onTagReceived(tag);
			}
		}
	}

}
