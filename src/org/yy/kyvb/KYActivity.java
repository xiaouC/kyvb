package org.yy.kyvb;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import org.json.JSONObject;
import org.json.JSONException;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.EditText;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ContextThemeWrapper;
import android.app.AlertDialog;
import android.view.KeyEvent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AccelerateInterpolator;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.content.DialogInterface;
import android.app.Dialog;

public class KYActivity extends Activity
{
    private KYActivity mActivity;
    private AlertDialog cur_show_ad = null;
    private String mLastLoginComid = "";
    private Dialog waiting_ad = null;

    private BroadcastReceiver recvMsgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateListView();
        }
    };

    private BroadcastReceiver playMsgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateMikeState();
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        mActivity = this;

        setContentView( R.layout.main_2 );

        loadSharedPreferences();

        fadeInOut();
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver( recvMsgReceiver );
            unregisterReceiver( playMsgReceiver );
        } catch ( IllegalArgumentException e ) {
            if( e.getMessage().contains( "Receiver not registered" ) ) {
                // Ignore this exception. This is exactly what is desired
            } else {
                // unexpected, re-throw
                throw e;
            }
        }

        super.onDestroy();
    }

    private void fadeInOut() {
        ImageView ivLoading = (ImageView)findViewById( R.id.loading );

        AnimationSet animSet = new AnimationSet(true);
        animSet.setInterpolator( new AccelerateInterpolator() );

        AlphaAnimation anim_1 = new AlphaAnimation( 0, 1 );
        anim_1.setDuration( 1500 );
        animSet.addAnimation( anim_1 );

        AlphaAnimation anim_2 = new AlphaAnimation( 1, 0 );
        anim_2.setDuration( 1500 );
        anim_2.setStartOffset( 2500 );
        animSet.addAnimation( anim_2 );

        ivLoading.startAnimation( animSet );

        Log.v( "cocos", "fadeInOut 111111111111111111111111111111111" );
        YYSchedule.getInstance().scheduleOnceTime( 4000, new YYSchedule.onScheduleAction() {
            public void doSomething() {
                Log.v( "cocos", "doSomething startView 111111111111111111111111111111111" );
                startView();
            }
        });
    }

    private void startView() {
        Log.v( "cocos", "startView 222222222222222222222222222222222222222222222222222222222" );
        setContentView( R.layout.main );

        ImageButton btn = (ImageButton)findViewById( R.id.btn_setting );
        btn.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                showLogin();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction( VoiceBroadcastService.RECV_MSG_INFO );
        registerReceiver( recvMsgReceiver, filter );  

        IntentFilter filter1 = new IntentFilter();
        filter1.addAction( FlySynthesizer.FLY_SPEAKING_FLAG );
        registerReceiver( playMsgReceiver, filter1 );  

        updateListView();
        updateMikeState();

        if( !VoiceBroadcastService.bIsRunning ) {
            showLogin();
        }
    }

    private void showLogin() {
        if( cur_show_ad != null ) {
            cur_show_ad.hide();
            cur_show_ad = null;
        }

        LayoutInflater li = LayoutInflater.from( mActivity );
        View view = li.inflate( R.layout.setting, null );

        AlertDialog.Builder builder = new AlertDialog.Builder( new ContextThemeWrapper( mActivity, R.style.setting_dlg ) );
        builder.setView( view );
        builder.setCancelable( true );

        cur_show_ad = builder.create();

        final ImageView iv = (ImageView)view.findViewById( R.id.warning_pic );
        final TextView tv = (TextView)view.findViewById( R.id.warning_tips );
        iv.setVisibility( View.INVISIBLE );
        tv.setVisibility( View.INVISIBLE );

        final EditText et = (EditText)view.findViewById( R.id.store_id );
        et.setText( mLastLoginComid );

        // 
        Button btn_submit = (Button)view.findViewById( R.id.btn_submit );
        if( btn_submit != null ) {
            btn_submit.setOnClickListener( new View.OnClickListener () {
                public void onClick( View v ){
                    showWaitingDialog();

                    mLastLoginComid = et.getText().toString();
                    saveSharedPreferences();

                    YYSchedule.getInstance().scheduleOnceTime( 100, new YYSchedule.onScheduleAction() {
                        public void doSomething() {
                            VBRequest.ky_comid = mLastLoginComid;
                            VBRequest.requestVerify( new VBRequest.onResponseListener() {
                                public void onResponse( String data ) {
                                    hideWaitingDialog();

                                    Log.v( "cocos", "response data : " + data );
                                    try {
                                        JSONObject verify_info = new JSONObject( data );

                                        int status = verify_info.getInt( "status" );
                                        Log.v( "cocos", "status : " + status );
                                        if( status == 1 ) {     // 成功
                                            String msg = verify_info.getString( "msg" );
                                            Log.v( "cocos", "msg : " + msg );
                                            String times = verify_info.getString( "times" );
                                            Log.v( "cocos", "times : " + times );
                                            String store_name = verify_info.getString( "store_name" );
                                            Log.v( "cocos", "store_name : " + store_name );

                                            TextView tv_store_name = (TextView)findViewById( R.id.store_name );
                                            tv_store_name.setText( store_name );

                                            // 服务开启
                                            Intent intentService = new Intent( KYActivity.this, VoiceBroadcastService.class );
                                            intentService.putExtra( "cacheDir", getCacheDir().getPath() );
                                            intentService.putExtra( "times", times );
                                            startService( intentService );

                                            updateMikeState();

                                            // 关闭弹窗
                                            if( cur_show_ad != null ) {
                                                cur_show_ad.hide();
                                                cur_show_ad = null;
                                            }
                                        } else {
                                            iv.setVisibility( View.VISIBLE );
                                            tv.setVisibility( View.VISIBLE );
                                        }
                                    } catch ( JSONException e ) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    });
                }
            });
        }

        cur_show_ad.setCanceledOnTouchOutside( false );   // 设置点击 Dialog 外部任意区域关闭 Dialog
        cur_show_ad.show();
    }

    public boolean onKeyDown( int keyCode, KeyEvent event )
    {
        if( keyCode == KeyEvent.KEYCODE_BACK )
        {
            if( cur_show_ad != null ) {
                cur_show_ad.hide();
                cur_show_ad = null;
            }

            LayoutInflater li = LayoutInflater.from( mActivity );
            View view = li.inflate( R.layout.exit, null );

            AlertDialog.Builder builder = new AlertDialog.Builder( new ContextThemeWrapper( mActivity, R.style.setting_dlg ) );
            builder.setView( view );
            builder.setCancelable( true );

            cur_show_ad = builder.create();

            // 
            Button btn_backstage = (Button)view.findViewById( R.id.btn_backstage );
            if( btn_backstage != null ) {
                btn_backstage.setOnClickListener( new View.OnClickListener () {
                    public void onClick( View v ){
                        if( cur_show_ad != null ) {
                            cur_show_ad.hide();
                            cur_show_ad = null;
                        }

                        finish();
                    }
                });
            }

            // 
            Button btn_exit = (Button)view.findViewById( R.id.btn_exit );
            if( btn_exit != null ) {
                btn_exit.setOnClickListener( new View.OnClickListener () {
                    public void onClick( View v ){
                        Intent intentService = new Intent( KYActivity.this, VoiceBroadcastService.class );
                        stopService( intentService );

                        if( cur_show_ad != null ) {
                            cur_show_ad.hide();
                            cur_show_ad = null;
                        }

                        finish();
                    }
                });
            }

            cur_show_ad.setCanceledOnTouchOutside( false );   // 设置点击 Dialog 外部任意区域关闭 Dialog
            cur_show_ad.show();
        }

        return false;
    }

    public void updateListView() {
        ListView lv = (ListView)findViewById( R.id.item_list );
        lv.setAdapter( new YYListAdapter( this, R.layout.listview_item, getItemListData() ) );
    }

    public List<Map<Integer,YYListAdapter.onYYListItemHandler>> getItemListData() {
        List<Map<Integer,YYListAdapter.onYYListItemHandler>> ret_data = new ArrayList<Map<Integer,YYListAdapter.onYYListItemHandler>>();

        for( int i=0; i < VoiceBroadcastService.recvMsgList.size(); ++i ) {
            final MsgInfo mi = VoiceBroadcastService.recvMsgList.get( i );

            Map<Integer,YYListAdapter.onYYListItemHandler> map = new HashMap<Integer,YYListAdapter.onYYListItemHandler>();
            map.put( R.id.order_text, new YYListAdapter.onYYListItemHandler() {
                @Override
                public void item_handle( int position, Object view_obj ) {
                    TextView tv = (TextView)view_obj;

                    String text_1 = "订单号" + mi.orderid;
                    tv.setText( text_1 );
                }
            });
            map.put( R.id.money_text, new YYListAdapter.onYYListItemHandler() {
                @Override
                public void item_handle( int position, Object view_obj ) {
                    TextView tv = (TextView)view_obj;

                    String text_1 = "支付" + mi.money + "¥";
                    tv.setText( text_1 );
                }
            });
            ret_data.add( map );
        }

        return ret_data;
    }

    public void updateMikeState() {
        ImageView iv = (ImageView)findViewById( R.id.oval_state );
        if( VoiceBroadcastService.mSpeakingState == FlySynthesizer.FLY_SPEAK_START ) {
            iv.setImageResource( R.drawable.oval_2 );
        } else {
            iv.setImageResource( R.drawable.oval_1 );
        }
    }

    // 
    public static String PREFER_NAME = "ky_data";
    public static int MODE = Context.MODE_PRIVATE;
    private boolean bIsLoading = false;
    public void loadSharedPreferences() {
        bIsLoading = true;

        SharedPreferences share = getSharedPreferences( PREFER_NAME, MODE );

        mLastLoginComid = share.getString( "lastLoginComid", "" );

        bIsLoading = false;
    }

    public void saveSharedPreferences() {
        if( bIsLoading )
            return;

        SharedPreferences share = getSharedPreferences( PREFER_NAME, MODE );
        SharedPreferences.Editor editor = share.edit();

        editor.putString( "lastLoginComid", mLastLoginComid );

        editor.commit();
    }

    public void showWaitingDialog() {
        if( waiting_ad != null ) {
            return;
        }

        waiting_ad = new Dialog( this, R.style.alert_waiting );
        View view = View.inflate( this, R.layout.alert_waiting, null );
        waiting_ad.setContentView( view );

        ImageView iv = (ImageView)view.findViewById( R.id.anim_waiting );
        AnimationDrawable anim = (AnimationDrawable)iv.getBackground();
        anim.start();

        waiting_ad.show();
    }

    public void hideWaitingDialog() {
        if( waiting_ad != null ) {
            waiting_ad.hide();
            waiting_ad = null;
        }
    }
}
