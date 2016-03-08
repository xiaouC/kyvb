package org.yy.kyvb;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.File;
import org.json.JSONObject;
import org.json.JSONException;

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
    public static boolean bIsRunning = false;
    public static final String RECV_MSG_INFO = "KYVB.RECV_MSG_INFO";

    private interface ItemInfo {
        public String getItemType();
        public List<String> getSpeakContent();
    }
    private List<ItemInfo> recvItems = new ArrayList<ItemInfo>();

    public static List<MsgInfo> recvMsgList = new ArrayList<MsgInfo>();

    public static String saveDir = "";
    public static int times = 5;

    public boolean isFlySpeaking = false;
    public VoicePlayer mVoicePlayer = null;
    public VoiceTypeConfig mVoiceTypeConfig = null;
    public int mScheduleIndex = -1;
    public VBRequest.onResponseListener rspListener = null;

    public static int mSpeakingState = FlySynthesizer.FLY_SPEAK_END;

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
        builder.setSmallIcon( R.drawable.ic_launcher );
        builder.setTicker( "Voice Service Start" );
        builder.setContentTitle( "Voice Broadcast" );
        builder.setContentText( "Voice Broadcast is running." );
        Notification notification = builder.build();

        startForegroundCompat( NOTIFICATION_ID, notification );

        FlyHelper.getInstance().init( this );
        mVoiceTypeConfig = new VoiceTypeConfig();
        mVoicePlayer = new VoicePlayer();

        rspListener = new VBRequest.onResponseListener() {
            public void onResponse( String data ) {
                Log.v( "cocos", "response data : " + data );
                try {
                    JSONObject msg_info = new JSONObject( data );

                    int status = msg_info.getInt( "status" );
                    Log.v( "cocos", "status : " + status );
                    if( status == 1 ) {     // 成功
                        String msg = msg_info.getString( "msg" );
                        Log.v( "cocos", "msg : " + msg );
                        String orderid = msg_info.getString( "orderid" );
                        Log.v( "cocos", "orderid : " + orderid );
                        double money = msg_info.getDouble( "money" );
                        Log.v( "cocos", "money : " + money );
                        String order_time = msg_info.getString( "order_time" );
                        Log.v( "cocos", "order_time : " + order_time );

                        final String f_money = formatMoney( money );

                        recvItems.add( new ItemInfo() {
                            public String getItemType() { return VoiceTypeConfig.VT_PAY_INFO; }
                            public List<String> getSpeakContent() {
                                List<String> ret_speak_list = new ArrayList<String>();
                                ret_speak_list.add( f_money );
                                return ret_speak_list;
                            }
                        });

                        MsgInfo mi = new MsgInfo();
                        mi.orderid = orderid;
                        mi.money = f_money;
                        recvMsgList.add( mi );

                        sendBroadcast( new Intent( RECV_MSG_INFO ) );

                        processData();
                    }
                } catch ( JSONException e ) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public int onStartCommand( Intent intent, int flags, int startId ) {
        super.onStartCommand( intent, flags, startId );
        Log.v( TAG, "onStartCommand" );
        bIsRunning = true;
        bIsDestroy = false;

        recvMsgList.clear();
        sendBroadcast( new Intent( RECV_MSG_INFO ) );

        saveDir = intent.getStringExtra( "cacheDir" );
        Log.v( TAG, "saveDir : " + saveDir );
        times = Integer.parseInt( intent.getStringExtra( "times" ) );
        Log.v( TAG, "times : " + times );

        // 马上请求一次
        VBRequest.requestBroadcastMessage( rspListener );

        // 定时请求
        YYSchedule.getInstance().cancelSchedule( mScheduleIndex );
        mScheduleIndex = YYSchedule.getInstance().scheduleCircle( times * 1000, new YYSchedule.onScheduleAction() {
            public void doSomething() {
                Log.v( TAG, "bIsDestroy : " + bIsDestroy );
                if( bIsDestroy ) {
                    return;
                }

                VBRequest.requestBroadcastMessage( rspListener );
            }
        });

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
        bIsRunning = false;

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
    public String formatMoney( double money ) {
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

            FlyHelper.getInstance().startSpeaking( "已收到付款：" + item.getSpeakContent().get( 0 ) + "元", "", new FlyHelper.onFlySpeakListener() {
                public void onCompleted() {
                    delayNextProcessData();
                }
            });
            //mVoiceTypeConfig.getSpeakFiles( item.getItemType(), item.getSpeakContent(), new VoiceTypeConfig.getFilesListener() {
            //    public void onFilesReady( List<String> files ) {
            //        if( files != null ) {
            //            mVoicePlayer.play( files, new VoicePlayer.onPlayEndListener() {
            //                public void onPlayEnd() {
            //                    delayNextProcessData();
            //                }
            //            });
            //        } else {
            //            delayNextProcessData();
            //        }
            //    }
            //});
        }
    }

    public void delayNextProcessData() {
        YYSchedule.getInstance().scheduleOnceTime( 1000, new YYSchedule.onScheduleAction() {
            public void doSomething() {
                if( bIsDestroy ) {
                    return;
                }

                isFlySpeaking = false;
                processData();
            }
        });
    }
}
