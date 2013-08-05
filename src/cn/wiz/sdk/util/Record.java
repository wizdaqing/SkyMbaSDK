/**
 * 
 */
package cn.wiz.sdk.util;

import java.io.File;
import java.io.IOException;

import android.annotation.SuppressLint;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

/**
 * @author zwy create Date message： 录音操作
 */
public class Record {

	private MediaRecorder mMediaRecorder;
	private String mVoicePath;
	private String mVoiceName;
	private File mWizVoiceFile;
	private boolean isRecord = false;

	public Record(String filePath, String fileName) {
		setRecordData(filePath, fileName);
	}

	public Record() {
	}

	public void setRecordData(String filePath, String fileName) {
		mVoicePath = filePath;
		mVoiceName = fileName;
	}

	public boolean onRecordStart() throws Exception {
		try {
			if (!WizMisc.isSDCardAvailable())
				throw new Exception("Please Insert SD Card");

			mWizVoiceFile = new java.io.File(mVoicePath, mVoiceName);
			mMediaRecorder = new MediaRecorder();
			/* 设定录音来源为麦克风 */
			mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
			mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
			mMediaRecorder.setOutputFile(mWizVoiceFile.getAbsolutePath());
			mMediaRecorder.prepare();
			mMediaRecorder.start();
			isRecord = true;
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	//
	public String onRecordStop() {
		if (mWizVoiceFile != null) {
			try {
				isRecord = false;
				mMediaRecorder.stop();
				mMediaRecorder.release();
			} catch (IllegalStateException e) {
			}
			mMediaRecorder = null;
			if (!TextUtils.isEmpty(mVoiceName))
				return mVoiceName;
		}
		return "";
	}

//	public boolean isRecord() {
//		return isRecord;
//	}

	public int getVolume() {
		if (mMediaRecorder == null)
			return 0;

		int ratio = mMediaRecorder.getMaxAmplitude() / 600;
		int db = 0;// 分贝 也可以理解为定义的音量大小
		if (ratio > 1)
			db = (int) (20 * Math.log10(ratio));// db就是我们需要取得的音量的值。
		return db;
	}

	public void setOnVolumeChanged(OnVolumeListener listener) {
		setOnVolumeChanged(listener, -1);
	}

	private int mVolume = -1;
	private long mSleepTime = 100;
	private OnVolumeListener mVolumeChangeListener;
	private int MESSAGE_WHAT = 2;
	/**
	 * 自动监听音量变化,当音量变化时发出事件
	 * @param listener
	 * @param sleepTime
	 */
	@SuppressLint("HandlerLeak")
	public void setOnVolumeChanged(OnVolumeListener listener, long sleepTime) {
		if (sleepTime > 0)
			mSleepTime = sleepTime;
		mVolumeChangeListener = listener;

		Handler volumeHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				if (!isRecord){
					this.removeMessages(MESSAGE_WHAT);
					return;
				}

				int volume = getVolume();
				if (mVolume != volume) {
					mVolume = volume;
					mVolumeChangeListener.onVolumeChanged(volume);
				}
				sendMessageDelayed(Message.obtain(this, MESSAGE_WHAT), mSleepTime);
			}
		};

		volumeHandler.sendMessage(Message.obtain(volumeHandler, MESSAGE_WHAT));
	}

	public interface OnVolumeListener{
		void onVolumeChanged(int volume);
	}

}
