package org.yy.kyvb;

import java.util.List;
import java.util.ArrayList;
import java.io.File;

import android.media.MediaPlayer;
import android.os.Environment;

public class VoicePlayer
{
    private MediaPlayer mMediaPlayer = null;

    public interface onPlayEndListener {
        public void onPlayEnd();
    }

    public VoicePlayer() {
        mMediaPlayer = new MediaPlayer();
    }

    private void realPlay( String fileName, final onPlayEndListener playEndListener ) {
        try {
            mMediaPlayer.reset();

            mMediaPlayer.setOnCompletionListener( new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion( MediaPlayer arg0 ) {
                    playEndListener.onPlayEnd();
                }
            });

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError( MediaPlayer player, int arg1, int arg2 ) {
                    playEndListener.onPlayEnd();
                    return false;
                }
            });

            File audioFile = new File( VoiceBroadcastService.saveDir, fileName );
            mMediaPlayer.setDataSource( audioFile.getAbsolutePath() );
            mMediaPlayer.prepare();
        } catch ( Exception e ) {
            e.printStackTrace();
            playEndListener.onPlayEnd();
        }

        mMediaPlayer.start();
    }

    public void play( List<String> files, onPlayEndListener playEndListener ) {
        play( files, 0, playEndListener );
    }

    private void play( final List<String> files, final int index, final onPlayEndListener playEndListener ) {
        if( index >= files.size() ) {
            playEndListener.onPlayEnd();

            return;
        }

        realPlay( files.get( index ), new onPlayEndListener() {
            public void onPlayEnd() {
                play( files, index + 1, playEndListener );
            }
        });
    }
}
