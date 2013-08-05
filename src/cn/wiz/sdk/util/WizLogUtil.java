package cn.wiz.sdk.util;

import cn.wiz.sdk.settings.WizAccountSettings;
import cn.wiz.sdk.util.FileUtil;
import cn.wiz.sdk.util.TimeUtil;
import android.content.Context;
import android.util.Log;

public class WizLogUtil {

	private static Boolean mWizLogSwitch; // 日志文件总开关
	private static Boolean mWizLog2File;// 日志写入文件开关
	private static char mWizLogType = 'v';// 输入日志类型，w代表只输出告警信息等，v代表输出所有信息
	private static int mSaveLogDays;// sd卡中日志文件的最多保存天数
	private static String mWizLogFileName = "Log.txt";
	private Context mContext = null;
	private static WizLogUtil mLogUtil = null;

	public WizLogUtil(Context ctx) {
		mContext = ctx;

		String path = WizAccountSettings.getDataRootPath(mContext);
		path = FileUtil.pathAddBackslash(path);
		mWizLogFileName = path + mWizLogFileName;
	}

	public static WizLogUtil getInstance(Context ctx) {
		if (mLogUtil == null) {
			mLogUtil = new WizLogUtil(ctx);
		}
		mWizLogSwitch = true;
		mWizLog2File = true;
		mSaveLogDays = 0;
		return mLogUtil;

	}

	private static final char logTypeW = 'w';
	private static final char logTypeE = 'e';
	private static final char logTypeD = 'd';
	private static final char logTypeI = 'i';
	private static final char logTypeV = 'v';

	public static void logW(String tag, Object msg) { // 警告信息
		log(tag, msg.toString(), logTypeW);
	}

	public static void logE(String tag, Object msg) { // 错误信息
		log(tag, msg.toString(), logTypeE);
	}

	public static void logD(String tag, Object msg) {// 调试信息
		log(tag, msg.toString(), logTypeD);
	}

	public static void logI(String tag, Object msg) {//
		log(tag, msg.toString(), logTypeI);
	}

	public static void logV(String tag, Object msg) {
		log(tag, msg.toString(), logTypeV);
	}

	public static void logW(String tag, String text) {
		log(tag, text, logTypeW);
	}

	public static void logE(String tag, String text) {
		log(tag, text, logTypeE);
	}

	public static void logD(String tag, String text) {
		log(tag, text, logTypeD);
	}

	public static void logI(String tag, String text) {
		log(tag, text, logTypeI);
	}

	public void logV(String tag, String text) {
		log(tag, text, logTypeV);
	}

	/**
	 * 根据tag, msg和等级，输出日志
	 */
	private static void log(String tag, String msg, char level) {
		if (mWizLogSwitch) {
			if ('e' == level && ('e' == mWizLogType || 'v' == mWizLogType)) { // 输出错误信息
				Log.e(tag, msg);
			} else if ('w' == level
					&& ('w' == mWizLogType || 'v' == mWizLogType)) {
				Log.w(tag, msg);
			} else if ('d' == level
					&& ('d' == mWizLogType || 'v' == mWizLogType)) {
				Log.d(tag, msg);
			} else if ('i' == level
					&& ('d' == mWizLogType || 'v' == mWizLogType)) {
				Log.i(tag, msg);
			} else {
				Log.v(tag, msg);
			}
			if (mWizLog2File)
				writeLogtoFile(String.valueOf(level), tag, msg);
		}
	}

	/**
	 * 打开日志文件并写入日志
	 * **/
	private static void writeLogtoFile(String mylogtype, String tag, String text) {// 新建或打开日志文件
		String errorMessage = "time=" + TimeUtil.getCurrentSQLDateTimeString()
				+ "\nerror:\n" + text;
		FileUtil.updateDate2File(mWizLogFileName, errorMessage, true);
	}

	/**
	 * 删除制定的日志文件
	 * */
	public static void delFile() {// 删除日志文件
		FileUtil.deleteFile(mWizLogFileName);
	}

	public static Boolean getmWizLogSwitch() {
		return mWizLogSwitch;
	}

	public static void setmWizLogSwitch(Boolean mWizLogSwitch) {
		WizLogUtil.mWizLogSwitch = mWizLogSwitch;
	}

	public static Boolean getmWizLog2File() {
		return mWizLog2File;
	}

	public static void setmWizLog2File(Boolean mWizLog2File) {
		WizLogUtil.mWizLog2File = mWizLog2File;
	}

	public static int getmSaveLogDays() {
		return mSaveLogDays;
	}

	public static void setmSaveLogDays(int mSaveLogDays) {
		WizLogUtil.mSaveLogDays = mSaveLogDays;
	}
}
