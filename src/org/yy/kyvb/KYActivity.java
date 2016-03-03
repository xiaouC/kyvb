package org.yy.kyvb;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.Button;
import android.widget.ImageButton;
import android.view.View;
import android.view.LayoutInflater;
import android.view.ContextThemeWrapper;
import android.app.AlertDialog;

public class KYActivity extends Activity
{
    private KYActivity mActivity;
    private AlertDialog cur_show_ad = null;
    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle savedInstanceState )
    {
        super.onCreate(savedInstanceState);

        mActivity = this;

        setContentView(R.layout.main);

        //Intent intentService = new Intent( KYActivity.this, VoiceBroadcastService.class );
        //intentService.putExtra( "cacheDir", getCacheDir().getPath() );
        //startService( intentService );
        updateListView();

        ImageButton btn = (ImageButton)findViewById( R.id.btn_setting );
        btn.setOnClickListener( new View.OnClickListener() {
            public void onClick( View v ) {
                LayoutInflater li = LayoutInflater.from( mActivity );
                View view = li.inflate( R.layout.setting, null );

                AlertDialog.Builder builder = new AlertDialog.Builder( new ContextThemeWrapper( mActivity, R.style.setting_dlg ) );
                builder.setView( view );
                builder.setCancelable( true );

                cur_show_ad = builder.create();

                // 
                Button btn_submit = (Button)view.findViewById( R.id.btn_submit );
                if( btn_submit != null ) {
                    btn_submit.setOnClickListener( new View.OnClickListener () {
                        public void onClick( View v ){
                            // 先清理，再回调
                            cur_show_ad.hide();
                            cur_show_ad = null;
                        }
                    });
                }

                cur_show_ad.setCanceledOnTouchOutside( false );   // 设置点击 Dialog 外部任意区域关闭 Dialog
                cur_show_ad.show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        Intent intentService = new Intent( KYActivity.this, VoiceBroadcastService.class );
        stopService( intentService );

        super.onDestroy();
    }

    public void updateListView() {
        ListView lv = (ListView)findViewById( R.id.item_list );
        lv.setAdapter( new YYListAdapter( this, R.layout.listview_item, getItemListData() ) );
    }

    public List<Map<Integer,YYListAdapter.onYYListItemHandler>> getItemListData() {
        List<Map<Integer,YYListAdapter.onYYListItemHandler>> ret_data = new ArrayList<Map<Integer,YYListAdapter.onYYListItemHandler>>();

        for( int i=1; i < 10; ++i ) {
            Map<Integer,YYListAdapter.onYYListItemHandler> map = new HashMap<Integer,YYListAdapter.onYYListItemHandler>();
            map.put( R.id.order_text, new YYListAdapter.onYYListItemHandler() {
                @Override
                public void item_handle( int position, Object view_obj ) {
                    TextView tv = (TextView)view_obj;

                    String text_1 = "订单号28837734";
                    tv.setText( text_1 );
                }
            });
            map.put( R.id.money_text, new YYListAdapter.onYYListItemHandler() {
                @Override
                public void item_handle( int position, Object view_obj ) {
                    TextView tv = (TextView)view_obj;

                    String text_1 = "支付123¥";
                    tv.setText( text_1 );
                }
            });
            ret_data.add( map );
        }

        return ret_data;
    }
}
