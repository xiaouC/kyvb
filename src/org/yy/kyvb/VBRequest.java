package org.yy.kyvb;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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

import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.os.Handler;
import android.os.Message;

public class VBRequest
{
    private static final String TAG = "cocos";

    private static final String VERIFY_URL = "http://ky.kuan1.cn/kyapi/index.php?action=checkStorePort&controller=Voice";
    private static final String BROADCAST_MESSAGE_URL = "http://ky.kuan1.cn/kyapi/index.php?action=index&controller=Voice";
    public static String ky_key = "ky_@0239732%^%$KEY";
    public static String ky_comid = "";

    public interface onResponseListener {
        public void onResponse( String data );
    }

    private static onResponseListener mRspListener = null;
    private static Handler handler = new Handler() {
        public void handleMessage( Message msg ) {
            String recvData = (String)msg.obj;
            if( mRspListener != null ) {
                mRspListener.onResponse( recvData );
            }
        }
    };

    public static void requestVerify( String verify_comid, onResponseListener rsp_listener ) {
        String ts = getTimestamp();
        Log.v( "cocos", "timestamp : " + ts );
        Log.v( "cocos", "verify_comid : " + verify_comid );
        Log.v( "cocos", "ky_key : " + ky_key );
        String md5 = stringToMD5( verify_comid + ky_key + ts );
        Log.v( "cocos", "md5 : " + md5 );

        Map<String,String> postParams = new HashMap<String,String>();
        postParams.put( "comid", verify_comid );
        postParams.put( "time", ts );
        postParams.put( "ticket", md5 );

        requestData( VERIFY_URL, postParams, rsp_listener );
    }

    public static String getTimestamp() {
        Long tsLong = System.currentTimeMillis() / 1000;
        return tsLong.toString();
    }

    public static String stringToMD5( String str ) {
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

    public static void requestBroadcastMessage( onResponseListener rsp_listener ) {
        String ts = getTimestamp();
        String md5 = stringToMD5( ky_comid + ky_key + ts );

        Map<String,String> postParams = new HashMap<String,String>();
        postParams.put( "comid", ky_comid );
        postParams.put( "time", ts );
        postParams.put( "ticket", md5 );

        requestData( BROADCAST_MESSAGE_URL, postParams, rsp_listener );
    }

    public static void requestData( final String requestURL, final Map<String,String> postParams, onResponseListener rspListener ) {
        Log.v( "cocos", "requestURL : " + requestURL );
        mRspListener = rspListener;

        new Thread(new Runnable() {
            public void run() {
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

                String recvData = "";
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

                    recvData = readbuff.toString();

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

                    Log.v( TAG, "recvData : " + recvData );

                    Message msg = new Message();
                    msg.obj = recvData;
                    handler.sendMessage( msg );
                }
            }
        }).start();
    }
}
