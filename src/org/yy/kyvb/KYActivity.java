package org.yy.kyvb;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;

public class KYActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Intent intentService = new Intent( KYActivity.this, VoiceBroadcastService.class );
        startService( intentService );
    }

    @Override
    protected void onDestroy() {
        Intent intentService = new Intent( KYActivity.this, VoiceBroadcastService.class );
        stopService( intentService );

        super.onDestroy();
    }

}
