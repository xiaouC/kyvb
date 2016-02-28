package org.yy.kyvb;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;

import com.baidu.android.pushservice.CustomPushNotificationBuilder;
import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;

public class KYActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Intent intentService = new Intent( KYActivity.this, VoiceBroadcastService.class );
        intentService.putExtra( "cacheDir", getCacheDir().getPath() );
        startService( intentService );

        // baidu push
        PushManager.startWork( this, PushConstants.LOGIN_TYPE_API_KEY, "api_key" );
    }

    @Override
    protected void onDestroy() {
        Intent intentService = new Intent( KYActivity.this, VoiceBroadcastService.class );
        stopService( intentService );

        super.onDestroy();
    }

}
