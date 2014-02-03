package th.in.whs.ku.bus;

import th.in.whs.ku.bus.api.TimeAgo;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

public class GCMIntentService extends IntentService {

	public GCMIntentService() {
		super("GCMIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            	String notifyMsg = String.format(
            			this.getText(R.string.notify_msg).toString(),
            			extras.getString("buslinename"),
            			extras.getString("busname"),
            			new TimeAgo(extras.getString("estimatedtime")).getMinuteLeft()
            	);
            	Intent openIntent = new Intent(this, MainActivity.class);
            	openIntent.putExtra(MainActivity.OPEN_STOP, extras.getString("stop"));
            	sendNotification(notifyMsg, openIntent);
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
	}
	
	private void sendNotification(String msg, Intent intent) {
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.bus_green)
        .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_launcher))
        .setContentTitle(this.getText(R.string.app_name))
        .setContentText(msg)
        .setTicker(msg)
        .setContentIntent(contentIntent)
        .setVibrate(new long[]{0, 100, 250, 250})
        .setAutoCancel(true);
        mNotificationManager.notify(1, mBuilder.build());
    }

}
