package org.yy.kyvb;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.widget.BaseAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Button;
import android.widget.TextView;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.os.AsyncTask;
import android.text.Html.ImageGetter;
import android.graphics.drawable.Drawable;
import android.text.Html;
import java.lang.reflect.Field; 
import android.util.Log;

public class YYListAdapter extends BaseAdapter {
    public interface onYYListItemHandler {
        void item_handle( int position, Object view_obj );
    }

    //////////////////////////////////////////////////////////////////////////////////////
    private static YYListAdapter yy_adapter;

    //////////////////////////////////////////////////////////////////////////////////////
    private LayoutInflater mInflater;

    //////////////////////////////////////////////////////////////////////////////////////
    private Activity main_activity;
    private Integer layout_res;                                 // 对应的每一个 item 的 R.layout.xxxxx
    public List<Map<Integer,onYYListItemHandler>> list_data;    // 数据

    public YYListAdapter( Activity activity, Integer layoutRes, List<Map<Integer,onYYListItemHandler>> data ) {
        yy_adapter = this;

        main_activity = activity;
        layout_res = layoutRes;
        list_data = data;

        // 
        mInflater = LayoutInflater.from( activity );
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return list_data.size();
    }

    @Override
    public Object getItem( int position ) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public long getItemId( int position ) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView( final int position, View convertView, ViewGroup parent ) {
        if( convertView == null ) {
            convertView = mInflater.inflate( layout_res, null );
        }

        Map<Integer,onYYListItemHandler> item_data = list_data.get( position );
        for( Map.Entry<Integer,onYYListItemHandler> item_entry : item_data.entrySet() ) {
            Integer v_id = item_entry.getKey();
            Object view_obj = convertView.findViewById( v_id );

            onYYListItemHandler list_item_handler = item_entry.getValue();
            list_item_handler.item_handle( position, view_obj );
        }

        return convertView;
    }

    //////////////////////////////////////////////////////////////////////////////////////
    public static class updateListViewTask extends AsyncTask< String, Integer, String > {
        @Override
        protected String doInBackground( String... params ) {
            //arr.set( Integer.parseInt( params[0] ), params[1] );
            //params得到的是一个数组，params[0]在这里是"0",params[1]是"第1项"
            //Adapter.notifyDataSetChanged();
            //执行添加后不能调用 Adapter.notifyDataSetChanged()更新UI，因为与UI不是同线程
            //下面的onPostExecute方法会在doBackground执行后由UI线程调用
            return null;	
        }

        @Override
        protected void onPostExecute( String result ) {
            // TODO Auto-generated method stub
            super.onPostExecute( result );
            yy_adapter.notifyDataSetChanged();
        }
    }
}

