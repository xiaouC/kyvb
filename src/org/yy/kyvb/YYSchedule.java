package org.yy.kyvb;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Map;
import java.util.HashMap;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class YYSchedule
{
    public static YYSchedule yy_schedule_instance = null;
    public static YYSchedule getInstance() {
        if( yy_schedule_instance == null ) {
            yy_schedule_instance = new YYSchedule();
        }
        return yy_schedule_instance;
    }

    // 
    public interface onScheduleAction {
        void doSomething();
    }

    private Map<Integer,onScheduleAction> all_schedule_actions = new HashMap<Integer,onScheduleAction>();
    private Map<Integer,Timer> all_schedule_timers = new HashMap<Integer,Timer>();
    private Map<Integer,Timer> all_schedule_timers_circle = new HashMap<Integer,Timer>();
    private int nNextScheduleIndex = 0;

    private Handler handler;
    public YYSchedule() {
        handler = new Handler(){
            public void handleMessage( Message msg ) {
                Log.v( "cocos", "handleMessage msg.what : " + msg.what );
                onScheduleAction schedule_action = all_schedule_actions.get( msg.what );
                if( schedule_action != null ) {
                    // 如果不循环的话，就移除
                    Timer t_circle = all_schedule_timers_circle.get( msg.what );
                    if( t_circle == null ) {
                        all_schedule_actions.remove( msg.what );
                        all_schedule_timers.remove( msg.what );
                    }

                    Log.v( "cocos", "handleMessage doSomething 00000000000000000000000000000000" );
                    schedule_action.doSomething();
                }

                // 
                super.handleMessage( msg );
            }
        };
    }

    public int scheduleOnceTime( long delay, onScheduleAction schedule_action ) {
        final int schedule_index = nNextScheduleIndex++;
        all_schedule_actions.put( schedule_index, schedule_action );
        Log.v( "cocos", "scheduleOnceTime schedule_index : " + schedule_index );

        TimerTask task = new TimerTask() {
            public void run() {
                Message message = new Message();
                message.what = schedule_index;
                handler.sendMessage( message );
            }
        };

        Timer timer = new Timer( true );
        all_schedule_timers.put( schedule_index, timer );

        timer.schedule( task, delay );

        return schedule_index;
    }

    public int scheduleCircle( long time, onScheduleAction schedule_action ) {
        final int schedule_index = nNextScheduleIndex++;
        all_schedule_actions.put( schedule_index, schedule_action );
        Log.v( "cocos", "scheduleCircle schedule_index : " + schedule_index );

        TimerTask task = new TimerTask() {
            public void run() {
                Message message = new Message();
                message.what = schedule_index;
                handler.sendMessage( message );
            }
        };

        Timer timer = new Timer( true );
        all_schedule_timers_circle.put( schedule_index, timer );

        timer.schedule( task, time, time );

        return schedule_index;
    }

    public void cancelSchedule( int schedule_index ) {
        Log.v( "cocos", "cancelSchedule schedule_index : " + schedule_index );
        onScheduleAction schedule_action = all_schedule_actions.get( schedule_index );
        if( schedule_action != null ) {
            all_schedule_actions.remove( schedule_index );
            all_schedule_timers.remove( schedule_index );
            all_schedule_timers_circle.remove( schedule_index );
        }
    }

    public void cancelAllSchedule() {
        Log.v( "cocos", "cancelAllSchedule 3333333333333333333333333333333333333333333333333333333" );
        all_schedule_actions.clear();

        // 
        for( Map.Entry<Integer,Timer> item_entry : all_schedule_timers.entrySet() ) {
            Timer timer = item_entry.getValue();
            timer.cancel();
        }
        all_schedule_timers.clear();

        // 
        for( Map.Entry<Integer,Timer> item_entry : all_schedule_timers_circle.entrySet() ) {
            Timer timer = item_entry.getValue();
            timer.cancel();
        }
        all_schedule_timers_circle.clear();
    }
}
