package org.yy.kyvb;

import android.content.Context;
import android.util.Log;
import android.os.Bundle;
import android.content.Intent;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
//import com.iflytek.speech.util.ApkInstaller;
//import com.iflytek.sunflower.FlowerCollector;

// 语音合成
public class FlySynthesizer {
    private Context mContext;
	// 语音合成对象
	private SpeechSynthesizer mTts;
	// 语记安装助手类
	ApkInstaller mInstaller ;

	// 引擎类型
	private String mEngineType = SpeechConstant.TYPE_CLOUD;
	
    private String mVoicer = "xiaoqi";         // 默认发音人
    private String mSpeed = "50";               // 合成语速
    private String mPitch = "50";               // 合成音调
    private String mVolume = "50";              // 合成音量
    private String mStreamType = "3";           // 播放器音频流类型
    private String mRequestFocus = "true";      // 播放合成音频打断音乐播放，默认为true

	/**
	 * 初始化监听。
	 */
	private InitListener mTtsInitListener = new InitListener() {
		@Override
		public void onInit(int code) {
			Log.v( "Fly", "InitListener init() code = " + code );
			if( code != ErrorCode.SUCCESS ) {
        		FlyHelper.getInstance().showTip( "SpeechSynthesizer 初始化失败,错误码：" + code );
        	} else {
				// 初始化成功，之后可以调用startSpeaking方法
        		// 注：有的开发者在onCreate方法中创建完合成对象之后马上就调用startSpeaking进行合成，
        		// 正确的做法是将onCreate中的startSpeaking调用移至这里
			}
		}
	};

    public void init( Context context ) {
        mContext = context;

		mTts = SpeechSynthesizer.createSynthesizer( context, mTtsInitListener );
		mInstaller = new ApkInstaller( context );
    }

    public void onDestroy( Context context ) {
		mTts.stopSpeaking();
		// 退出时释放连接
		mTts.destroy();
    }

    public void setVoicer( String voicer ) {
        mVoicer = voicer;
    }

    public void setSpeed( String speed ) {
        mSpeed = speed;
    }

    public void setPitch( String pitch ) {
        mPitch = pitch;
    }

    public void setVolume( String volume ) {
        mVolume = volume;
    }

    public void setStreamType( String streamType ) {
        mStreamType = streamType;
    }

    public void setRequestFocus( String requestFocus ) {
        mRequestFocus = requestFocus;
    }

	/**
	 * 合成回调监听。
	 */
	private SynthesizerListener mTtsListener = new SynthesizerListener() {
		@Override
		public void onSpeakBegin() {
			FlyHelper.getInstance().showTip( "开始播放" );
		}

		@Override
		public void onSpeakPaused() {
			FlyHelper.getInstance().showTip( "暂停播放" );
		}

		@Override
		public void onSpeakResumed() {
			FlyHelper.getInstance().showTip( "继续播放" );
		}

		@Override
		public void onBufferProgress(int percent, int beginPos, int endPos, String info) {
			//// 合成进度
			//mPercentForBuffering = percent;
			//FlyHelper.getInstance().showTip( String.format( getString(R.string.tts_toast_format), mPercentForBuffering, mPercentForPlaying ) );
		}

		@Override
		public void onSpeakProgress(int percent, int beginPos, int endPos) {
			//// 播放进度
			//mPercentForPlaying = percent;
			//FlyHelper.getInstance().showTip(String.format(getString(R.string.tts_toast_format), mPercentForBuffering, mPercentForPlaying));
		}

		@Override
		public void onCompleted( SpeechError error ) {
			if( error == null ) {
				FlyHelper.getInstance().showTip( "播放完成" );
			} else if( error != null ) {
				FlyHelper.getInstance().showTip( error.getPlainDescription( true ) );
			}

            // 
            Intent speakEndIntent = new Intent( FlyHelper.FLY_SPEAK_END );
            mContext.sendBroadcast( speakEndIntent );
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
			// 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
			// 若使用本地能力，会话id为null
			//	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
			//		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
			//		Log.d(TAG, "session id =" + sid);
			//	}
		}
	};

    public void startSpeaking( String text ) {
        // 清空参数
        mTts.setParameter( SpeechConstant.PARAMS, null );
        // 根据合成引擎设置相应参数
        mTts.setParameter( SpeechConstant.ENGINE_TYPE, mEngineType );
        // 设置在线合成发音人
        mTts.setParameter( SpeechConstant.VOICE_NAME, mVoicer );
        // 设置合成语速
        mTts.setParameter( SpeechConstant.SPEED, mSpeed );
        // 设置合成音调
        mTts.setParameter( SpeechConstant.PITCH, mPitch );
        // 设置合成音量
        mTts.setParameter( SpeechConstant.VOLUME, mVolume );
        // 设置播放器音频流类型
        mTts.setParameter( SpeechConstant.STREAM_TYPE, mStreamType );
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter( SpeechConstant.KEY_REQUEST_FOCUS, mRequestFocus );

        //// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        //// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        //mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        //mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/tts.wav");

        int code = mTts.startSpeaking( text, mTtsListener );
        if( code != ErrorCode.SUCCESS ) {
            if( code == ErrorCode.ERROR_COMPONENT_NOT_INSTALLED ) {
                // 未安装则跳转到提示安装页面
                mInstaller.install();
            } else {
               FlyHelper.getInstance().showTip( "语音合成失败,错误码: " + code );
            }
        }
    }

    public void stopSpeaking() {
        mTts.stopSpeaking();
    }

    public void pauseSpeaking() {
        mTts.pauseSpeaking();
    }

    public void resumeSpeaking() {
        mTts.resumeSpeaking();
    }
}
