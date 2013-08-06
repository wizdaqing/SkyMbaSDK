package cn.wiz.sdk.settings;

import cn.wiz.sdk.db.WizDatabase;
import cn.wiz.sdk.util.WizMisc;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

/**
 * @author zwy
 * @E-Mail 1010482327@qq.com
 * @version create date 2012-10-12 11:18:39
 * 默认值为设置文件中不存在某设置值时使用的，不存在的情况有以下两种情况:1.设置文件被清理，2.未写入设置信息;
 * @Message :system setting about account,and get some value about android
 *          device and some value about wiz app
 */
public class WizSystemSettings {

	private static SharedPreferences getSharePreferences(Context ctx) {
		return PreferenceManager.getDefaultSharedPreferences(ctx);
	}

	private static SharedPreferences.Editor getSharePreferencesEditor(
			Context ctx) {
		return getSharePreferences(ctx).edit();
	}

	private static String getSettingMessage(Context ctx, String key,
			String defaultVaule) {
		SharedPreferences settings = getSharePreferences(ctx);
		return settings.getString(key, defaultVaule);
	}

	private static void setSettingMessage(Context ctx, String key, String value) {
		SharedPreferences.Editor editor = getSharePreferencesEditor(ctx);
		editor.putString(key, value);
		editor.commit();
	}

	private static boolean getSettingMessage(Context ctx, String key,
			boolean defaultVaule) {
		SharedPreferences settings = getSharePreferences(ctx);
		return settings.getBoolean(key, defaultVaule);
	}

	private static void setSettingMessage(Context ctx, String key, boolean value) {
		SharedPreferences.Editor editor = getSharePreferencesEditor(ctx);
		editor.putBoolean(key, value);
		editor.commit();
	}

	private static Integer getSettingMessage(Context ctx, String key, int defaultVaule) {
		SharedPreferences settings = getSharePreferences(ctx);
		return settings.getInt(key, defaultVaule);
	}

	private static void setSettingMessage(Context ctx, String key, int value) {
		SharedPreferences.Editor editor = getSharePreferencesEditor(ctx);
		editor.putInt(key, value);
		editor.commit();
	}

	private static Long getSettingMessage(Context ctx, String key, long defaultVaule) {
		SharedPreferences settings = getSharePreferences(ctx);
		return settings.getLong(key, defaultVaule);
	}

	private static void setSettingMessage(Context ctx, String key, long value) {
		SharedPreferences.Editor editor = getSharePreferencesEditor(ctx);
		editor.putLong(key, value);
		editor.commit();
	}

	public static final String systemSettingsPwdProtection = "systemSettingsCheckBox";
	public static boolean isPasswordProtection(Context ctx) {
		return getSettingMessage(ctx, systemSettingsPwdProtection, false);
	}

	public static void setPasswordProtection(Context ctx, boolean value) {
		setSettingMessage(ctx, systemSettingsPwdProtection, value);
	}

	private static final String widgetPasswordProtection = "widgetPasswordProtection";
	public static boolean isWidgetPasswordProtection(Context ctx) {

		boolean defaultValue = isPasswordProtection(ctx);
		return getSettingMessage(ctx, widgetPasswordProtection, defaultValue);
	}

	public static void setWidgetPasswordProtection(Context ctx, boolean value) {
		setSettingMessage(ctx, widgetPasswordProtection, value);
	}

	public static void setWidgetPasswordProtection(Context ctx) {
		boolean defaultValue = isPasswordProtection(ctx);
		setWidgetPasswordProtection(ctx, defaultValue);
	}

	private static final String systemSettingsPassword = "systemSettingsPassword";
	public static String getSystemPassword(Context ctx) {
		return getSettingMessage(ctx, systemSettingsPassword, "6688");
	}

	public static void setSystemPassword(Context ctx, String value) {
		setSettingMessage(ctx, systemSettingsPassword, value);
	}

	public static boolean isAutoSyncUseInWifi(Context ctx) {
		return WizMisc.isWifi(ctx) && isAutoSync(ctx);
	}

	private static final String autoSyncCheckbox = "autoSyncCheckBox";
	public static boolean isAutoSync(Context ctx) {
		return getSettingMessage(ctx, autoSyncCheckbox, true);
	}

	public static void setAutoSync(Context ctx) {
		setSettingMessage(ctx, autoSyncCheckbox, true);
	}

	public static boolean isWifiOnlyDownloadData(Context ctx) {
		return getSettingMessage(ctx, "systemSettingsWifionlyDownloadData", true);
	}

	public static void setWifiOnlyDownloadData(Context ctx, boolean value) {
		setSettingMessage(ctx, "systemSettingsWifionlyDownloadData", value);
	}

	public static final String downloadTypeOfAll = "2";
	public static final String downloadTypeOfRecent = "1";
	public static final String downloadTypeOfNull = "0";

	public static final String personalDownloadTypeListPreference = "personalDownloadTypeListPreference";
	public static String getPersonalDownLoadDataType(Context ctx) {
		return getSettingMessage(ctx, personalDownloadTypeListPreference, downloadTypeOfRecent);
	}

	public static void setPersonalDownLoadDataType(Context ctx, String value) {
		setSettingMessage(ctx, personalDownloadTypeListPreference, value);
	}

	public static final String groupDownloadTypeListPreference = "groupDownloadTypeListPreference";
	public static String getGroupDownLoadDataType(Context ctx) {
		return getSettingMessage(ctx, groupDownloadTypeListPreference, downloadTypeOfNull);
	}

	public static void setGroupDownLoadDataType(Context ctx, String value) {
		setSettingMessage(ctx, groupDownloadTypeListPreference, value);
	}

	public static boolean isAutoAdaptsScreen(Context ctx) {
		return getSettingMessage(ctx, "autoAdaptToPhoneScreenCheckBox", true);
	}

	public static boolean isOpenNightMode(Context ctx) {
		return getSettingMessage(ctx, "system_settings_night_mode", false);
	}

	public static void setNightModeState(Context ctx, boolean state) {
		setSettingMessage(ctx, "system_settings_night_mode", state);
	}
	
	public static boolean isShakeEnable(Context ctx) {
		return getSettingMessage(ctx, "manage_shake_checkbox", true);
	}
	
	public static final String defaultPagePreference = "system_setting_default_page";
	public enum HomePage{
		MESSAGES,PERSONAL_NOTES,NULL
	}
	public static HomePage getHomePagePreference(Context ctx) {
		HomePage[] values = HomePage.values();
		int value = Integer.parseInt(getSettingMessage(ctx, defaultPagePreference, HomePage.valueOf("NULL").ordinal() + ""));
		return values[value];
	}
	public static void setHomePagePreference(Context ctx, HomePage homePage) {
		if(homePage == HomePage.NULL){
			return;
		}
		setSettingMessage(ctx, defaultPagePreference, homePage.ordinal()+"");
	}
	
	public static boolean isIncludeChildrenFolderNotes(Context ctx) {
		return getSettingMessage(ctx, "includeChildrenFolderNoteCheckBox", true);
	}

	public static boolean isIncludeChildrenTagNotes(Context ctx) {
		return getSettingMessage(ctx, "includeChildrenTagNoteCheckBox", true);
	}

	private static final String systemDefaultAccount = "systemDefaultAccountUserId";
	public static String getDefaultUserId(Context ctx) {
		return getSettingMessage(ctx, systemDefaultAccount, "");
	}

	public static void setDefaultUserId(Context ctx, String defaultValue) {
		WizAccountSettings.logoutCurrentUserId();
		setSettingMessage(ctx, systemDefaultAccount, defaultValue);
	}

	private static final String DEFAULT_DIRECTORY = "/My Notes/";
	public static final String systemSettingsDefaultDirectory = "systemSettingsDefaultDirectory";
	public static String getDefaultDirectory(Context ctx) {
		return getSettingMessage(ctx, systemSettingsDefaultDirectory, DEFAULT_DIRECTORY);
	}

	public static void setDefaultDirectory(Context ctx, String defaultValue) {
		setSettingMessage(ctx, systemSettingsDefaultDirectory, defaultValue);
	}

	public static void setDefaultDirectory(Context ctx) {
		setDefaultDirectory(ctx, DEFAULT_DIRECTORY);
	}

	public static final int DEFAULT_ABS_IMG_LENGTH = 60;
	private static final String systemSetAbstractImgLength = "abstractImgLength";
	public static int getDefaultAbsImgLength(Context ctx) {
		return getSettingMessage(ctx, systemSetAbstractImgLength, DEFAULT_ABS_IMG_LENGTH);
	}

	public static void setDefaultAbsImgLength(Context ctx, int value) {
		setSettingMessage(ctx, systemSetAbstractImgLength, value);
	}

	private static final String sysTextOfInsert2html = "systemTextOfInsertText2html";
	public static String getTextOfInsert2html(Context ctx) {
		String defaultValue = "Insert text";
		return getSettingMessage(ctx, sysTextOfInsert2html, defaultValue);
	}

	public static void setTextOfInsert2html(Context ctx, String defaultValue) {
		setSettingMessage(ctx, sysTextOfInsert2html, defaultValue);
	}

	private static String getKeyOfLastSyncTimeKey(String userId) {
		return "LastSyncTime_" + userId;
	}

	/**
	 * the time of the last synchronization
	 * it's that you have never synchroized, when the last sync time is null, so the default vaule is 0
	 * @return 
	 */
	public static long getLastSyncTime(Context ctx, String userId) {
		return getSettingMessage(ctx, getKeyOfLastSyncTimeKey(userId), (long) 0);
	}

	public static void setLastSyncTime(Context ctx, String userId, long time) {
		if (time <= 0)
			time = System.currentTimeMillis();
		setSettingMessage(ctx, getKeyOfLastSyncTimeKey(userId), time);
	}

	private static final double screenSize = 7.0;
	public static boolean isPhone(Activity ctx) {
		boolean isPhone = true;

		DisplayMetrics metric = new DisplayMetrics();
		ctx.getWindowManager().getDefaultDisplay().getMetrics(metric);
		int screenWidth = metric.widthPixels; // 屏幕宽度（像素）
		int screenHeight = metric.heightPixels; // 屏幕高度（像素）
		int densityDpi = metric.densityDpi; // 屏幕密度DPI（120 / 160 / 240 / 320）
		double sizeWidth = WizMisc.divide(screenWidth, densityDpi, 2);
		double sizeHeight = WizMisc.divide(screenHeight, densityDpi, 2);
		double size = Math.sqrt(Math.pow(sizeWidth, 2) + Math.pow(sizeHeight, 2));

		isPhone = size < screenSize;
		return isPhone;
	}

	public static int getScreenWidth(Activity ctx) {
		DisplayMetrics metric = new DisplayMetrics();
		ctx.getWindowManager().getDefaultDisplay().getMetrics(metric);
		return metric.widthPixels; // 屏幕宽度（像素）
	}

	public static int getScreenHeight(Activity ctx) {
		DisplayMetrics metric = new DisplayMetrics();
		ctx.getWindowManager().getDefaultDisplay().getMetrics(metric);
		return metric.heightPixels; // 屏幕高度（像素）
	}

	public static int getScreenDenaityDpi(Activity ctx){
		DisplayMetrics metric = new DisplayMetrics();
		ctx.getWindowManager().getDefaultDisplay().getMetrics(metric);
		return metric.densityDpi; // 屏幕密度DPI（120 / 160 / 240 / 320）
	}

	public static float getScreenDensity(Activity ctx) {
		DisplayMetrics metric = new DisplayMetrics();
		ctx.getWindowManager().getDefaultDisplay().getMetrics(metric);
		return metric.density; // 屏幕密度（0.75 / 1.0 / 1.5 / 2.0）
	}

	public static boolean isUpdateWiz(Context ctx, String userId, String kbGuid) {
		int indexVersionCode = WizDatabase.getDb(ctx, userId, kbGuid)
				.getWizVersion();
		int localVersionCode = getVersionCode(ctx);
		if (localVersionCode <= 0)
			return false;
		return indexVersionCode > localVersionCode;
	}

	public static int getVersionCode(Context ctx) {
		try {
			android.content.pm.PackageManager pm = ctx.getPackageManager();
			android.content.pm.PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), 0);
			return pi.versionCode;// // 获取在AndroidManifest.xml中配置的版本号VersionCode

		} catch (android.content.pm.PackageManager.NameNotFoundException e) {
			return 0;
		}
	}

	public static String getVersionName(Context ctx) {
		try {
			android.content.pm.PackageManager pm = ctx.getPackageManager();
			android.content.pm.PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), 0);
			return pi.versionName;// 获取在AndroidManifest.xml中配置的版本号VersionName
		} catch (android.content.pm.PackageManager.NameNotFoundException e) {
			return "";
		}
	}

	public static int getVersionSDK() {
		try {
			return Integer.parseInt(Build.VERSION.SDK);
		} catch (NumberFormatException e) {
			return 4;
		}
	}

	@SuppressWarnings("static-access")
	private static InputMethodManager getInputManager(Activity ctx){
		return (InputMethodManager) ctx.getSystemService(ctx.INPUT_METHOD_SERVICE);
	}

	public static void hideSoftInputWindow(Activity ctx, View view, View getFocusView) {
		getFocusView.setFocusable(true);
		getFocusView.requestFocus();
		getInputManager(ctx).hideSoftInputFromWindow(view.getWindowToken(), 0);
	}

	public static void hideSoftInputWindow(Activity ctx) {
		View currentFocusView = ctx.getCurrentFocus();
		hideSoftInputWindow(ctx, currentFocusView, new View(ctx));
	}

	public static void showSoftInputWindow(Activity ctx, View view){
		view.requestFocus();
		getInputManager(ctx).showSoftInput(view, InputMethodManager.RESULT_SHOWN);
	}

	public static boolean hidedSoftInput(Activity ctx) {
		
		return ctx.getWindow().getAttributes().softInputMode == WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE;
	}

	public static boolean isVerticalScreen(Activity ctx) {
		return ctx.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
	}

	// <!-- SDK Version -->
	public static final int androidSDKVersionOf1_5 = 3;
	public static final int androidSDKVersionOf1_6 = 4;
	public static final int androidSDKVersionOf2_1 = 7;
	public static final int androidSDKVersionOf2_2 = 8;
	public static final int androidSDKVersionOf2_3_1 = 9;
	public static final int androidSDKVersionOf2_3_3 = 10;
	public static final int androidSDKVersionOf3_0 = 11;
	public static final int androidSDKVersionOf3_1 = 12;
	public static final int androidSDKVersionOf3_2 = 13;
	public static final int androidSDKVersionOf4_0 = 14;
	public static final int androidSDKVersionOf4_0_3 = 15;
	public static final int androidSDKVersionOf4_1_2 = 16;
	public static final int androidSDKVersionOf4_2_0 = 17;

	public static boolean isSDKVersion3(Context ctx) {
		int sdkVersion = WizSystemSettings.getVersionSDK();
		return sdkVersion >= androidSDKVersionOf3_0;
	}
	
	public static boolean isSDKVersion4(Context ctx) {
		int sdkVersion = WizSystemSettings.getVersionSDK();
		return sdkVersion >= androidSDKVersionOf4_0;
	}

	public static boolean isSDKVersion4_12(Context ctx) {
		int sdkVersion = WizSystemSettings.getVersionSDK();
		return sdkVersion >= androidSDKVersionOf4_1_2;
	}

	public static String systemTimeFormat(Context ctx) {
		ContentResolver resolver = ctx.getContentResolver();
		String name = android.provider.Settings.System.DATE_FORMAT;
		return android.provider.Settings.System.getString(resolver, name);
	}

}
