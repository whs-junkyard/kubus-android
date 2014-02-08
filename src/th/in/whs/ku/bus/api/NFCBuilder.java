package th.in.whs.ku.bus.api;

import java.io.IOException;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NdefFormatable;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class NFCBuilder {
	public static NdefMessage createMessage(Context context, Uri uri){
		if(Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH){
			return null;
		}
		NdefRecord[] records = new NdefRecord[]{
				NdefRecord.createUri(uri),
				NdefRecord.createApplicationRecord(context.getPackageName())
		};
		return new NdefMessage(records);
	}
	
	public static void writeToTag(NdefMessage message, Tag tag, boolean readonly) throws IOException, FormatException, TagLostException{
		writeToTag(message, NdefFormatable.get(tag), readonly);
	}
	
	public static void writeToTag(NdefMessage message, NdefFormatable tag, boolean readonly) throws IOException, FormatException, TagLostException{
		if(tag == null){
			throw new IOException("Tag is not compatible with NdefFormatable");
		}
		tag.connect();
		
		if(readonly){
			tag.formatReadOnly(message);
		}else{
			tag.format(message);
		}
		
		tag.close();
	}
}
