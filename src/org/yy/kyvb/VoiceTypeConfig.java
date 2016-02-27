package org.yy.kyvb;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.File;

import android.util.Log;

public class VoiceTypeConfig
{
    public class VoiceInfo {
        String saveFileName;
        String speakText;       // 当 speakText 为空的时候，从 speakTextList 中获取
    }

    public interface VoiceTypeInfo {
        public List<VoiceInfo> getPlayPattern();
    }

    public Map<String,VoiceTypeInfo> vtConfig = new HashMap<String,VoiceTypeInfo>();

    public static final String VT_PAY_INFO = "VT_PAY_INFO";
    public VoiceTypeConfig() {
        vtConfig.put( VT_PAY_INFO, new VoiceTypeInfo() {
            public List<VoiceInfo> getPlayPattern() {
                List<VoiceInfo> ret_pattern = new ArrayList<VoiceInfo>();

                // 前置
                VoiceInfo front = new VoiceInfo();
                front.saveFileName = "pay_front.wav";
                front.speakText = "已收到";
                ret_pattern.add( front );

                // 
                VoiceInfo vi = new VoiceInfo();
                vi.saveFileName = "pay_num.wav";
                vi.speakText = "";
                ret_pattern.add( vi );

                // 后置
                VoiceInfo post = new VoiceInfo();
                post.saveFileName = "pay_post.wav";
                post.speakText = "元付款";
                ret_pattern.add( post );

                return ret_pattern;
            }
        });
    }

    public interface getFilesListener {
        public void onFilesReady( List<String> files );
    }

    public void getSpeakFiles( String voiceType, List<String> speakTextList, getFilesListener filesListener ) {
        VoiceTypeInfo vtInfo = vtConfig.get( voiceType );
        if( vtInfo == null ) {
            filesListener.onFilesReady( null );

            return;
        }

        List<VoiceInfo> pattern = vtInfo.getPlayPattern();

        int stIndex = 0;
        for( int i=0; i < pattern.size(); ++i ) {
            VoiceInfo vi = pattern.get( i );
            if( vi.speakText.equals( "" ) ) {
                if( stIndex >= speakTextList.size() ) {
                    break;
                } else {
                    vi.speakText = speakTextList.get( stIndex );
                    vi.saveFileName = vi.speakText + ".wav";
                    stIndex = stIndex + 1;
                }
            }
        }

        getSpeakFile( pattern, 0, filesListener );
    }

    public void getSpeakFile( final List<VoiceInfo> pattern, final int index, final getFilesListener filesListener ) {
        if( index >= pattern.size() ) {
            List<String> ret_files = new ArrayList<String>();

            for( int i=0; i < pattern.size(); ++i ) {
                VoiceInfo vi = pattern.get( i );
                ret_files.add( vi.saveFileName );
            }

            filesListener.onFilesReady( ret_files );

            return;
        }

        VoiceInfo vi = pattern.get( index );
        if( fileIsExists( vi.saveFileName ) ) {
            getSpeakFile( pattern, index + 1, filesListener );
        } else {
            // 云端生成
            FlyHelper.startSpeaking( vi.speakText, VoiceBroadcastService.saveDir + "/" + vi.saveFileName, new FlyHelper.onFlySpeakListener() {
                public void onCompleted() {
                    getSpeakFile( pattern, index + 1, filesListener );
                }
            });
        }
    }

    public static boolean fileIsExists( String fileName ) {
        try {
            Log.v( "cocos", "fileIsExists : " + VoiceBroadcastService.saveDir + "/" + fileName );
            File f = new File( VoiceBroadcastService.saveDir, fileName );
            if( !f.exists() ) {
                Log.v( "cocos", "fileIsExists : 1 false" );
                return false;
            }
        } catch ( Exception e ) {
            Log.v( "cocos", "fileIsExists : 2 false" );
            return false;
        }

        Log.v( "cocos", "fileIsExists : true" );
        return true;
    }
}
