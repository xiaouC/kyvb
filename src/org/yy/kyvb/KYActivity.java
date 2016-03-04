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

public class KYActivity extends Activity
{
    private KYActivity mActivity;
    private AlertDialog cur_show_ad = null;

    private BroadcastReceiver recvMsgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateListView();
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        mActivity = this;

        setContentView(R.layout.main);

        final TextView tv_store_name = (TextView)findViewById( R.id.store_name );

        ImageButton btn = (ImageButton)findViewById( R.id.btn_setting );
        btn.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
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

                // 
                Button btn_submit = (Button)view.findViewById( R.id.btn_submit );
                if( btn_submit != null ) {
                    btn_submit.setOnClickListener( new View.OnClickListener () {
                        public void onClick( View v ){
                            //String ky_comid = "ky123456_96";
                            VBRequest.ky_comid = et.getText().toString();
                            VBRequest.requestVerify( new VBRequest.onResponseListener() {
                                public void onResponse( String data ) {
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
                                            tv_store_name.setText( store_name );

                                            // 服务开启
                                            Intent intentService = new Intent( KYActivity.this, VoiceBroadcastService.class );
                                            intentService.putExtra( "cacheDir", getCacheDir().getPath() );
                                            intentService.putExtra( "times", times );
                                            startService( intentService );

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

                cur_show_ad.setCanceledOnTouchOutside( false );   // 设置点击 Dialog 外部任意区域关闭 Dialog
                cur_show_ad.show();
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction( VoiceBroadcastService.RECV_MSG_INFO );
        registerReceiver( recvMsgReceiver, filter );  

        updateListView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
}
