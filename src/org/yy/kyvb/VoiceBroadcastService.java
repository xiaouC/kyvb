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

import java.util.Iterator;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

    private static final String VERIFY_URL = "http://pay.kuan1.cn/kysound/index/store";
    private static final String BROADCAST_MESSAGE_URL = "http://pay.kuan1.cn/kysound/index/get";

    private interface ItemInfo {
        public String getItemType();
        public List<String> getSpeakContent();
    }
    private List<ItemInfo> recvItems = new ArrayList<ItemInfo>();

    public static String saveDir = "";
    public static String ky_key = "ky_@0239732%^%$KEY";
    public static String ky_comid = "ky123456_96";

    public boolean isFlySpeaking = false;
    public VoicePlayer mVoicePlayer = null;
    public VoiceTypeConfig mVoiceTypeConfig = null;
    public int mScheduleIndex = -1;

    public interface onResponseListener {
        public void onResponse( String data );
    }

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

        //for( int i=1; i < 11; ++i ) {
        //    final int index = i;
        //    recvItems.add( new ItemInfo() {
        //        public String getItemType() { return VoiceTypeConfig.VT_PAY_INFO; }
        //        public List<String> getSpeakContent() {
        //            List<String> ret_speak_list = new ArrayList<String>();
        //            ret_speak_list.add( formatMoney( index + 100.5f ) );
        //            return ret_speak_list;
        //        }
        //    });
        //}

        //YYSchedule.getInstance().scheduleOnceTime( 1000, new YYSchedule.onScheduleAction() {
        ////mScheduleIndex = YYSchedule.getInstance().scheduleCircle( 1000, new YYSchedule.onScheduleAction() {
        //    public void doSomething() {
        //        processData();
        //    }
        //});

        YYSchedule.getInstance().scheduleOnceTime( 1000, new YYSchedule.onScheduleAction() {
            public void doSomething() {
                requestVerify();
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
    public String getTimestamp() {
        Long tsLong = System.currentTimeMillis() / 1000;
        return tsLong.toString();
    }

    public String stringToMD5( String str ) {
        try {
            byte[] hash = MessageDigest.getInstance( "MD5" ).digest( str.getBytes( "UTF-8" ) );

            StringBuilder hex = new StringBuilder( hash.length * 2 );
            for( byte b : hash ) {
                if( ( b & 0xFF ) < 0x10 )
                    hex.append("0");
                hex.append( Integer.toHexString( b & 0xFF ) );
            }

            return hex.toString();
        } catch ( NoSuchAlgorithmException e ) {
            throw new RuntimeException( "Huh, MD5 should be supported?", e );
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( "Huh, UTF-8 should be supported?", e );
        }

        //return "";
    }

    public void requestVerify() {
        String ts = getTimestamp();
        Log.v( "cocos", "timestamp : " + ts );
        Log.v( "cocos", "comid : " + ky_comid );
        Log.v( "cocos", "ky_key : " + ky_key );
        String md5 = stringToMD5( ky_comid + ky_key + ts );
        Log.v( "cocos", "md5 : " + md5 );

        Map<String,String> postParams = new HashMap<String,String>();
        postParams.put( "comid", ky_comid );
        postParams.put( "time", ts );
        postParams.put( "ticket", md5 );

        requestData( VERIFY_URL, postParams, new onResponseListener() {
            public void onResponse( String data ) {
                Log.v( "cocos", "response data : " + data );
                try {
                    JSONObject roleInfo = new JSONObject( data );

                    int status = roleInfo.getInt( "status" );
                    Log.v( "cocos", "status : " + status );
                    if( status == 1 ) {     // 成功
                        String msg = roleInfo.getString( "msg" );
                        Log.v( "cocos", "msg : " + msg );
                        int times = roleInfo.getInt( "times" );
                        Log.v( "cocos", "times : " + times );
                        String store_name = roleInfo.getString( "store_name" );
                        Log.v( "cocos", "store_name : " + store_name );

                        // 马上请求一次
                        requestBroadcastMessage();

                        // 定时请求
                        YYSchedule.getInstance().cancelSchedule( mScheduleIndex );
                        mScheduleIndex = YYSchedule.getInstance().scheduleCircle( times * 1000, new YYSchedule.onScheduleAction() {
                            public void doSomething() {
                                requestBroadcastMessage();
                            }
                        });
                    } else {
                        // 不成功，5 秒后重新尝试
                        YYSchedule.getInstance().scheduleOnceTime( 5000, new YYSchedule.onScheduleAction() {
                            public void doSomething() {
                                requestVerify();
                            }
                        });
                    }
                } catch ( JSONException e ) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void requestBroadcastMessage() {
        String ts = getTimestamp();
        String md5 = stringToMD5( ky_comid + ky_key + ts );

        Map<String,String> postParams = new HashMap<String,String>();
        postParams.put( "comid", ky_comid );
        postParams.put( "time", ts );
        postParams.put( "ticket", md5 );

        requestData( BROADCAST_MESSAGE_URL, postParams, new onResponseListener() {
            public void onResponse( String data ) {
                Log.v( "cocos", "response data : " + data );
                try {
                    JSONObject roleInfo = new JSONObject( data );

                    int status = roleInfo.getInt( "status" );
                    Log.v( "cocos", "status : " + status );
                    if( status == 1 ) {     // 成功
                        String msg = roleInfo.getString( "msg" );
                        Log.v( "cocos", "msg : " + msg );
                        String orderid = roleInfo.getString( "orderid" );
                        Log.v( "cocos", "orderid : " + orderid );
                        final double money = roleInfo.getDouble( "money" );
                        Log.v( "cocos", "money : " + money );
                        String order_time = roleInfo.getString( "order_time" );
                        Log.v( "cocos", "order_time : " + order_time );

                        recvItems.add( new ItemInfo() {
                            public String getItemType() { return VoiceTypeConfig.VT_PAY_INFO; }
                            public List<String> getSpeakContent() {
                                List<String> ret_speak_list = new ArrayList<String>();
                                ret_speak_list.add( formatMoney( money ) );
                                return ret_speak_list;
                            }
                        });

                        processData();
                    }
                } catch ( JSONException e ) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void requestData( String requestURL, Map<String,String> postParams, onResponseListener rspListener ) {
        Log.v( "cocos", "requestURL : " + requestURL );

        PrintWriter writer = null;  
        BufferedReader reader = null;
        HttpURLConnection connection = null;

        StringBuffer params = new StringBuffer();
        Iterator it = postParams.entrySet().iterator();
        while( it.hasNext() ) {
            Map.Entry element = (Map.Entry) it.next();
            params.append(element.getKey());
            params.append("=");
            params.append(element.getValue());
            params.append("&");
        }
        if( params.length() > 0 ) {
            params.deleteCharAt( params.length() - 1 );
        }

        try {
            URL url = new URL( requestURL );
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty( "accept", "*/*" );
            connection.setRequestProperty( "connection", "Keep-Alive" );
            connection.setRequestProperty( "Content-Length", String.valueOf( params.length() ) );
            connection.setDoOutput( true );
            connection.setDoInput( true );
            // 获取URLConnection对象对应的输出流 
            writer = new PrintWriter( connection.getOutputStream() );
            // 发送请求参数 
            writer.write( params.toString() );
            // flush输出流的缓冲 
            writer.flush();

            // 根据ResponseCode判断连接是否成功 
            int responseCode = connection.getResponseCode();
            if( responseCode != 200 ) {
                Log.v( TAG, "requestVerify response code : " + responseCode );
            }
            //connection.connect();

            reader = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
            StringBuffer readbuff = new StringBuffer();
            String lstr = null;
            while( ( lstr = reader.readLine() ) != null ) {
                readbuff.append( lstr );
            }

            String recvData = readbuff.toString();
            Log.v( TAG, "recvData : " + recvData );

            rspListener.onResponse( recvData );
        } catch( MalformedURLException e ) {
            e.printStackTrace();
        } catch( IOException e ) {
            e.printStackTrace();
        } finally {
            if( connection != null ) {
                connection.disconnect();
            }
            try {  
                if( writer != null ) {
                    writer.close();
                }
                if( reader != null ) {
                    reader.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

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
                isFlySpeaking = false;
                processData();
            }
        });
    }
}
