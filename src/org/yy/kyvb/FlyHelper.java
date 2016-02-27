package org.yy.kyvb;

import android.content.Context;
import android.widget.Toast;
import android.util.Log;

import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SpeechConstant;

public class FlyHelper {
    public static FlyHelper fly_helper_instance = null;
    public static FlyHelper getInstance() {
        if( fly_helper_instance == null ) {
            fly_helper_instance = new FlyHelper();
        }
        return fly_helper_instance;
    }

	private Toast mToast;

    public static final String FLY_SPEAK_END    = "kyvb.fly.speak.end";
    private FlySynthesizer  fly_synthesizer     = new FlySynthesizer();               // 语音合成

    public void init( Context context ) {
        Log.v( "Fly", "init" );

		mToast = Toast.makeText( context, "", Toast.LENGTH_SHORT );

		// 应用程序入口处调用,避免手机内存过小，杀死后台进程,造成SpeechUtility对象为null
		// 设置你申请的应用appid
		SpeechUtility.createUtility( context, SpeechConstant.APPID + "=56d15968" );

        // 
        fly_synthesizer.init( context );
    }

    public void onDestroy( Context context ) {
        fly_synthesizer.onDestroy( context );
    }

	public void showTip( final String str ) {
		mToast.setText( str );
		mToast.show();
    }

    // synthesize
    public static void startSpeaking( String text ) { FlyHelper.getInstance().fly_synthesizer.startSpeaking( text ); }
    public static void stopSpeaking() { FlyHelper.getInstance().fly_synthesizer.stopSpeaking(); }
    public static void pauseSpeaking() { FlyHelper.getInstance().fly_synthesizer.pauseSpeaking(); }
    public static void resumeSpeaking() { FlyHelper.getInstance().fly_synthesizer.resumeSpeaking(); }
    public static void setVoicer( String voicer ) { FlyHelper.getInstance().fly_synthesizer.setVoicer( voicer ); }
    public static void setSpeed( String speed ) { FlyHelper.getInstance().fly_synthesizer.setSpeed( speed ); }
    public static void setPitch( String pitch ) { FlyHelper.getInstance().fly_synthesizer.setPitch( pitch ); }
    public static void setVolume( String volume ) { FlyHelper.getInstance().fly_synthesizer.setVolume( volume ); }
    public static void setStreamType( String streamType ) { FlyHelper.getInstance().fly_synthesizer.setStreamType( streamType ); }
    public static void setRequestFocus( String requestFocus ) { FlyHelper.getInstance().fly_synthesizer.setRequestFocus( requestFocus ); }
}
