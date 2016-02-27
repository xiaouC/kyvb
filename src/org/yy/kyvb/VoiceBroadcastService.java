package org.yy.kyvb;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.File;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Build.VERSION;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;

public class VoiceBroadcastService extends Service {
    private static final String TAG = "cocos";

    private boolean mReflectFlg = false;

    private static final int NOTIFICATION_ID = 1; // 如果 id 设置为 0, 会导致不能设置为前台service
    private static final Class<?>[] mSetForegroundSignature = new Class[] {boolean.class};
    private static final Class<?>[] mStartForegroundSignature = new Class[] {int.class, Notification.class};
    private static final Class<?>[] mStopForegroundSignature = new Class[] {boolean.class};

    private NotificationManager mNM;
    private Method mSetForeground;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mSetForegroundArgs = new Object[1];
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];

    public static boolean bIsDestroy = false;

    private static final String REQUEST_URL = "http://pokemonpro.sdk.dnastdio.com:8888/sdk/android/sdk/huawei/buoy";

    private interface ItemInfo {
        public String getItemType();
        public List<String> getSpeakContent();
    }
    private List<ItemInfo> recvItems = new ArrayList<ItemInfo>();

    public static String saveDir = "";
    public boolean isFlySpeaking = false;
    public VoicePlayer mVoicePlayer = null;
    public VoiceTypeConfig mVoiceTypeConfig = null;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.v( TAG, "onCreate" );

        mNM = (NotificationManager)getSystemService( Context.NOTIFICATION_SERVICE );

        try {
            mStartForeground = VoiceBroadcastService.class.getMethod( "startForeground", mStartForegroundSignature );
            mStopForeground = VoiceBroadcastService.class.getMethod( "stopForeground", mStopForegroundSignature );
        } catch ( NoSuchMethodException e ) {
            mStartForeground = mStopForeground = null;
        }

        try {
            mSetForeground = getClass().getMethod( "setForeground", mSetForegroundSignature );
        } catch ( NoSuchMethodException e ) {
            throw new IllegalStateException( "OS doesn't have Service.startForeground OR Service.setForeground!" );
        }  

        NotificationCompat.Builder builder = new NotificationCompat.Builder( this );
        PendingIntent contentIntent = PendingIntent.getActivity( this, 0, new Intent( this, KYActivity.class ), 0 );
        builder.setContentIntent( contentIntent );
        builder.setSmallIcon( R.drawable.icon );
        builder.setTicker( "Voice Service Start" );
        builder.setContentTitle( "Voice Broadcast" );
        builder.setContentText( "Voice Broadcast is running." );
        Notification notification = builder.build();

        startForegroundCompat( NOTIFICATION_ID, notification );

        FlyHelper.getInstance().init( this );
        mVoiceTypeConfig = new VoiceTypeConfig();
        mVoicePlayer = new VoicePlayer();

        for( int i=1; i < 11; ++i ) {
            final int index = i;
            recvItems.add( new ItemInfo() {
                public String getItemType() { return VoiceTypeConfig.VT_PAY_INFO; }
                public List<String> getSpeakContent() {
                    List<String> ret_speak_list = new ArrayList<String>();
                    ret_speak_list.add( formatMoney( index + 100.5f ) );
                    return ret_speak_list;
                }
            });
        }

        YYSchedule.getInstance().scheduleOnceTime( 1000, new YYSchedule.onScheduleAction() {
        //mScheduleIndex = YYSchedule.getInstance().scheduleCircle( 1000, new YYSchedule.onScheduleAction() {
            public void doSomething() {
                processData();
            }
        });
    }

    @Override
    public int onStartCommand( Intent intent, int flags, int startId ) {
        super.onStartCommand( intent, flags, startId );
        Log.v( TAG, "onStartCommand" );

        saveDir = intent.getStringExtra( "cacheDir" );
        Log.v( TAG, "saveDir : " + saveDir );

        return START_STICKY;
    }

    @Override
    public IBinder onBind( Intent intent ) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.v( TAG, "onDestroy" );

        bIsDestroy = true;

        YYSchedule.getInstance().cancelAllSchedule();

        stopForegroundCompat( NOTIFICATION_ID );

        FlyHelper.getInstance().onDestroy( this );

        // 清理缓存文件
        File directory = getCacheDir();
        if( directory != null && directory.exists() && directory.isDirectory() ) {
            for( File item : directory.listFiles() ) {
                item.delete();
            }
        }

        super.onDestroy();
    }

    void invokeMethod( Method method, Object[] args ) {
        try {
            method.invoke( this, args );
        } catch ( InvocationTargetException e ) {
            // Should not happen.
            Log.v( TAG, "Unable to invoke method", e );
        } catch ( IllegalAccessException e ) {
            // Should not happen.
            Log.v( TAG, "Unable to invoke method", e );
        }
    }

    /**
     * This is a wrapper around the new startForeground method, using the older
     * APIs if it is not available.
     */
    void startForegroundCompat( int id, Notification notification ) {
        if( mReflectFlg ) {
            // If we have the new startForeground API, then use it.
            if( mStartForeground != null ) {
                mStartForegroundArgs[0] = Integer.valueOf( id );
                mStartForegroundArgs[1] = notification;
                invokeMethod( mStartForeground, mStartForegroundArgs );
                return;
            }

            // Fall back on the old API.
            mSetForegroundArgs[0] = Boolean.TRUE;
            invokeMethod( mSetForeground, mSetForegroundArgs );
            mNM.notify( id, notification );
        } else {
            /* 还可以使用以下方法，当sdk大于等于5时，调用sdk现有的方法startForeground设置前台运行，
             * 否则调用反射取得的sdk level 5（对应Android 2.0）以下才有的旧方法setForeground设置前台运行 */
            if( VERSION.SDK_INT >= 5 ) {
                startForeground( id, notification );
            } else {
                // Fall back on the old API.
                mSetForegroundArgs[0] = Boolean.TRUE;
                invokeMethod( mSetForeground, mSetForegroundArgs );
                mNM.notify( id, notification );
            }
        }
    }

    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
    void stopForegroundCompat( int id ) {
        if( mReflectFlg ) {
            // If we have the new stopForeground API, then use it.
            if( mStopForeground != null ) {
                mStopForegroundArgs[0] = Boolean.TRUE;
                invokeMethod( mStopForeground, mStopForegroundArgs );
                return;
            }

            // Fall back on the old API.  Note to cancel BEFORE changing the
            // foreground state, since we could be killed at that point.
            mNM.cancel( id );
            mSetForegroundArgs[0] = Boolean.FALSE;
            invokeMethod( mSetForeground, mSetForegroundArgs );
        } else {
            /* 还可以使用以下方法，当sdk大于等于5时，调用sdk现有的方法stopForeground停止前台运行，
             * 否则调用反射取得的sdk level 5（对应Android 2.0）以下才有的旧方法setForeground停止前台运行 */
            if( VERSION.SDK_INT >= 5 ) {
                stopForeground( true );
            } else {
                // Fall back on the old API.  Note to cancel BEFORE changing the
                // foreground state, since we could be killed at that point.
                mNM.cancel( id );
                mSetForegroundArgs[0] = Boolean.FALSE;
                invokeMethod( mSetForeground, mSetForegroundArgs );
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void requestData() {
        try {
            URL url = new URL( REQUEST_URL );
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput( false ); // 是否输入参数
            connection.connect();

            BufferedReader reader = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
            StringBuffer readbuff = new StringBuffer();
            String lstr = null;
            while( ( lstr = reader.readLine() ) != null ) {
                readbuff.append( lstr );
            }
            connection.disconnect();
            reader.close();

            String recvData = readbuff.toString();
            Log.v( TAG, "recvData : " + recvData );
        } catch( MalformedURLException e ) {
            e.printStackTrace();
        } catch( IOException e ) {
            e.printStackTrace();
        }
    }

    public String formatMoney( float money ) {
        String ret_text = String.format( "%.02f", money );
        Log.v( "cocos", "formatMoney money : " + money );
        Log.v( "cocos", "formatMoney ret_text : " + ret_text );
        if( ret_text.endsWith( ".00" ) ) {
            ret_text.replace( ".00", "" );
        } else {
            if( ret_text.endsWith( "0" ) ) {
                ret_text = ret_text.substring( 0, ret_text.length() - 1 );
            }
        }
        Log.v( "cocos", "formatMoney ret_text : " + ret_text );
        return ret_text;
    }

    public void processData() {
        if( isFlySpeaking ) {
            return;
        }

        if( recvItems.size() > 0 ) {
            isFlySpeaking = true;

            ItemInfo item = recvItems.get( 0 );
            recvItems.remove( 0 );

            mVoiceTypeConfig.getSpeakFiles( item.getItemType(), item.getSpeakContent(), new VoiceTypeConfig.getFilesListener() {
                public void onFilesReady( List<String> files ) {
                    if( files != null ) {
                        mVoicePlayer.play( files, new VoicePlayer.onPlayEndListener() {
                            public void onPlayEnd() {
                                delayNextProcessData();
                            }
                        });
                    } else {
                        delayNextProcessData();
                    }
                }
            });
        }
    }

    public void delayNextProcessData() {
        YYSchedule.getInstance().scheduleOnceTime( 1000, new YYSchedule.onScheduleAction() {
            public void doSomething() {
                isFlySpeaking = false;
                processData();
            }
        });
    }
}
