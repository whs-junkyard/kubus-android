package th.in.whs.ku.bus;

import android.annotation.TargetApi;
import android.nfc.NdefMessage;
import android.os.Build;

public interface NFCSettableActivity {
	/**
	 * Set Android Beam NFC message
	 * @param message
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	public void setNFC(NdefMessage message);
	/**
	 * Reset or disable Android Beam message
	 */
	public void unsetNFC();
}
