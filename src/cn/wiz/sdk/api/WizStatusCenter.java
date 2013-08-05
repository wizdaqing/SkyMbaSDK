package cn.wiz.sdk.api;

import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import cn.wiz.sdk.api.WizStrings.WizStringId;
import cn.wiz.sdk.db.WizDatabase;
import cn.wiz.sdk.settings.WizSystemSettings;

public class WizStatusCenter {
	private static ConcurrentHashMap<String, String> mStringMap = new ConcurrentHashMap<String, String>();
	private static ConcurrentHashMap<String, Boolean> mBoolMap = new ConcurrentHashMap<String, Boolean>();
	private static ConcurrentHashMap<String, Thread> mThreadMap = new ConcurrentHashMap<String, Thread>();
	private static ConcurrentHashMap<String, Long> mIntMap = new ConcurrentHashMap<String, Long>();
	
	private static String getKey (String userId, String subKey) {
		return userId + "/" + subKey;
	}
	
	private static void setString(String userId, String subKey, String val) {
		mStringMap.put(getKey(userId, subKey), val);
	}
	
	private static String getString(String userId, String subKey, String defaultValue) {
		String ret = mStringMap.get(getKey(userId, subKey));
		if (ret == null)
			return "";
		return ret;
	}
	private static void setThread(String userId, String subKey, Thread val) {
		mThreadMap.put(getKey(userId, subKey), val);
	}
	
	private static Thread getThread(String userId, String subKey) {
		return mThreadMap.get(getKey(userId, subKey));
	}
	
	public static void setBool(String userId, String subKey, boolean val) {
		mBoolMap.put(getKey(userId, subKey), val);
	}
	
	public static boolean getBool(String userId, String subKey, boolean defaultValue) {
		Boolean b = mBoolMap.get(getKey(userId, subKey));
		if (b == null)
			return defaultValue;
		return b.booleanValue();
	}

	public static void setInt(String userId, String subKey, int val) {
		setLong(userId, subKey, val);
	}
	
	public static int getInt(String userId, String subKey, int defaultValue) {
		return (int)getLong(userId, subKey, defaultValue);
	}
	

	public static void setLong(String userId, String subKey, long val) {
		mIntMap.put(getKey(userId, subKey), val);
	}
	
	public static long getLong(String userId, String subKey, long defaultValue) {
		Long b = mIntMap.get(getKey(userId, subKey));
		if (b == null)
			return defaultValue;
		return b.longValue();
	}
	
	///////////////////////////////////////////////////////////////////
	//可以用于存储一些常用的变量
	public static String getCurrentUserId() {
		return getString("", "CurrentUserId", null);
	}
	public static void setCurrentUserId(String userId) {
		setString("", "CurrentUserId", userId);
	}

	public static String getASUrl() {
		return getString("", "ASXmlRpcUrl", null);
	}

	public static void setASUrl(String url) {
		setString("", "ASXmlRpcUrl", url);
	}
	
	public static String getLoggerUrl() {
		return getString("", "LoggerUrl", null);
	}

	public static void setLoggerUrl(String url) {
		setString("", "LoggerUrl", url);
	}

	public static String getCrashUrl() {
		return getString("", "CrashUrl", null);
	}

	public static void setCrashUrl(String url) {
		setString("", "CrashUrl", url);
	}

	//注意，token的获取，不要从这里直接获取，而应该通过TokenManager获取
	//这里仅用于保存
	public static String getCurrentToken(String userId) {
		return getString(userId, "CurrentToken", null);
	}
	public static void setCurrentToken(String userId, String newToken) {
		setString(userId, "CurrentToken", newToken);
	}


	//设置是否有未读的笔记
	//需要保存在数据库里面，下次启动的时候还可以获取
	//默认没有未读的笔记
	public static void setHasUnreadDocuments(Context ctx, String userId, boolean hasUnreadDocuments) {
		WizDatabase.getDb(ctx, userId, "").setMetaInt("Common", "HasUnreadDocuments", hasUnreadDocuments ? 1 : 0);
	}
	//
	//返回是否有未读的笔记
	public static boolean getHasUnreadDocuments(Context ctx, String userId) {
		return 1 == WizDatabase.getDb(ctx, userId, "").getMetaIntDef("Common", "HasUnreadDocuments", 0);
	}
	
	////////////////////////thread//////////////////////////////
	//下面这些状态，用于同步的线程的控制
	//
	//
	//可以获取当前是否正在同步全部数据
	public static void setSyncingAll(Context ctx, String userId, boolean b) {
		setBool(userId, "SyncingAll", b);
	}
	public static boolean isSyncingAll(String userId) {
		return getBool(userId, "SyncingAll", false);
	}
	//是否正在上传某一个kb的数据
	public static void setUploadingKb(Context ctx, String userId, boolean b) {
		setBool(userId, "UploadingKb", b);
	}
	public static boolean isUploadingKb(String userId) {
		return getBool(userId, "UploadingKb", false);
	}

	//auto sync
	public static Thread getCurrentSyncThread(String userId) {
		return getThread(userId, "CurrentSyncThread");
	}
	public static void setCurrentSyncThread(String userId, Thread thread) {
		setThread(userId, "CurrentSyncThread", thread);
	}

	//document abstract
	public static Thread getCurrentDocumentAbstractThread(String userId) {
		return getThread(userId, "CurrentDocumentAbstractThread");
	}

	public static void setCurrentDocumentAbstractThread(String userId,
			WizDocumentAbstractCache thread) {
		setThread(userId, "CurrentDocumentAbstractThread", thread);
	}
	//avatar
	public static Thread getCurrentAvatarThread(String userId) {
		return getThread(userId,"CurrentAvatarThread");
	}
	
	public static void setCurrentAvatarThread(String userId, WizAvatarCache thread) {
		setThread(userId, "CurrentAvatarThread", thread);
	}

	//download documents
	public static Thread getCurrentDownloadDocumentsThread(String userId) {
		return getThread(userId,"CurrentDownloadDocumentsThread");
	}

	public static void setCurrentDownloadDocumentsThread(String userId, WizDownloadDocuments thread) {
		setThread(userId, "CurrentDownloadDocumentsThread", thread);
	}

	//documents count
	public static Thread getCurrentDocumentsCountThread(String userId) {
		return getThread(userId, "CurrentDocumentsCountThread");
	}

	public static void setCurrentDocumentsCountThread(String userId, Thread val) {
		setThread(userId, "CurrentDocumentsCountThread", val);
	}
	
	////////////////////////sync thread status//////////////////////////////
	//sync all
	public static boolean isStoppingSyncAll(String userId) {
		return getBool(userId, "IsStopSyncAll", false);
	}
	public static void setStoppingSyncAll(String userId, boolean b) {
		if (b) {
			if (isSyncingAll(userId)) {
				WizEventsCenter.sendSyncStatusMessage(WizStringId.STOPPING_SYNC);
			}
		}
		setBool(userId, "IsStopSyncAll", b);
	}
	//sync
	public static boolean isStoppingSyncThread(String userId) {
		return getBool(userId, "IsStopSync", false);
	}
	public static void setStoppingSyncThread(String userId, boolean b) {
		setBool(userId, "IsStopSync", b);
	}
	//download object
	public static boolean isStoppingDownloadDataThread(String userId) {
		return getBool(userId, "IsStopDownloadData", false);
	}
	public static void setStoppingDownloadDataThread(String userId, boolean b) {
		setBool(userId, "IsStopDownloadData", b);
	}
	//download avatar
	public static boolean isStoppingDownloadAvatarThread(String userId) {
		return getBool(userId, "IsStopDownloadAvatar", false);
	}
	public static void setStoppingDownloadAvatarThread(String userId, boolean b) {
		setBool(userId, "IsStopDownloadAvatar", b);
	}
	//document abstract
	public static boolean isStoppingDocumentAbstractThread(String userId) {
		return getBool(userId, "IsStopDocumentAbstract", false);
	}	
	public static void setStoppingDocumentAbstractThread(String userId, boolean b) {
		setBool(userId, "IsStopDocumentAbstract", b);
	}
	//documents count
	public static boolean isStoppingDocumentsCountThread(String userId) {
		return getBool(userId, "IsStopDocumentsCount", false);
	}	
	public static void setStoppingDocumentsCountThread(String userId, boolean b) {
		setBool(userId, "IsStopDocumentsCount", b);
	}
	//
	public static void stopAll(String userId) {
		setStoppingSyncThread(userId, true);
		setStoppingDownloadDataThread(userId, true);
		setStoppingDocumentAbstractThread(userId, true);
		setStoppingDocumentsCountThread(userId, true);
		setStoppingDownloadAvatarThread(userId, true);
	}
	//
	public static void resetAll(String userId) {
		setStoppingSyncThread(userId, false);
		setStoppingDownloadDataThread(userId, false);
		setStoppingDocumentAbstractThread(userId, false);
		setStoppingDocumentsCountThread(userId, false);
		setStoppingDownloadAvatarThread(userId, false);
	}
	
	
	//
	public static long getLastSyncTime(Context ctx, String userId) {
		long t = WizSystemSettings.getLastSyncTime(ctx, userId);
		//
		return t;
	}
	public static void setLastSyncTime(Context ctx, String userId, long t) {
		WizSystemSettings.setLastSyncTime(ctx, userId, t);
	}
	
	public static void setLastSyncTime(Context ctx, String userId) {
		WizSystemSettings.setLastSyncTime(ctx, userId, System.currentTimeMillis());
	}
	//
	public static boolean needAutoSync(Context ctx, String userId) {
		long now = System.currentTimeMillis();
		long last = getLastSyncTime(ctx, userId);
		//
		long minute = 1000 * 60;
		//
		long span = now - last;
		return span > minute * 30;	//30分钟内不再自动同步
	}
	//
	//启动所有的后台线程，除了全部同步
	//
	public static void startAllThreads(final Context ctx, final String userId, final String password) {
		resetAll(userId);
		//
		WizDocumentAbstractCache.startDocumentAbstractThread(ctx, userId);
		WizSync.startSyncThread(ctx, userId, password);
		WizDatabase.startDocumentsCountThread(ctx, userId);

		boolean autoSync = WizSystemSettings.isAutoSync(ctx);
		if (autoSync) {
			new Thread() {
				public void run() {
					try {
						sleep(1000);	//延迟一秒钟
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					WizSync.autoSyncAll(ctx, userId);	//自动同步
				}
			}.start();
		}
	}


}
