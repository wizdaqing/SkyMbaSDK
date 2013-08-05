package cn.wiz.sdk.api;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import cn.wiz.sdk.api.WizAsyncAction.WizAction;
import cn.wiz.sdk.api.WizAsyncAction.WizAsyncActionThread;
import cn.wiz.sdk.settings.WizAccountSettings;
import cn.wiz.sdk.util.WizMisc;

public class WizLogger {

	public static void init() {
		
	}
	static private String getLogUrl() throws Exception {
		String urlOld = WizStatusCenter.getLoggerUrl();
		if (urlOld != null && !urlOld.equals(""))
			return urlOld;
		//
		String url = WizMisc.getUrlFromApi("log_http");
		WizStatusCenter.setLoggerUrl(url);
		//
		return url;
	}
	static private String getCrashUrl() throws Exception {
		String urlOld = WizStatusCenter.getCrashUrl();
		if (urlOld != null && !urlOld.equals(""))
			return urlOld;
		//
		String url = WizMisc.getUrlFromApi("crash_http");
		WizStatusCenter.setCrashUrl(url);
		//
		return url;
	}
	
	static private String makeUrl(String url, String versionName, String deviceName, String oemSource, String action) {
		return url + "?k1=android" + 			//android
					  "&k2=" + versionName + 	//version name
					  "&k3=" + deviceName + 	//device name
					  "&k4=" + oemSource + 		//OEM
					  "&k5=" + action;			//action
	}
	//
	static private String makeUrl(Context ctx, String url, String action) {
		String versionName = getVersionName(ctx);
		String deviceName = getDeviceName(ctx);
		//
		return makeUrl(url, versionName, deviceName, getOEMSource(ctx), action);
	}
	//
	static private void logCore(final Context ctx, final String action) {
		//
		WizAsyncAction.startAsyncAction(null, new WizAction() {

			@Override
			public Object work(WizAsyncActionThread thread, Object actionData)
					throws Exception {
				//
				try {
					String url = getLogUrl();
					//
					url = makeUrl(ctx, url, action);
					//
					HttpGet httpget = new HttpGet(url);
					//
					try {
						DefaultHttpClient client = WizMisc.getDefaultConnection();
						client.execute(httpget);
					}
					catch (Exception e) {
						httpget.abort();
					}
				}
				catch (Exception e) {
				}
				catch (OutOfMemoryError e) {
				}
				catch (Throwable e) {
				}
				//
				return null;
			}

			@Override
			public void onBegin(Object actionData) {
			}

			@Override
			public void onEnd(Object actionData, Object ret) {
			}

			@Override
			public void onException(Object actionData, Exception e) {
			}

			@Override
			public void onStatus(Object actionData, String status, int arg1,
					int arg2, Object obj) {
			}
			
		});
	}

	static private void crashCore(final String info) {
		WizAsyncAction.startAsyncAction(null, new WizAction() {

			@Override
			public Object work(WizAsyncActionThread thread, Object actionData)
					throws Exception {
				//
				try {
					String url = getCrashUrl();
					HttpPost httppost = new HttpPost(url);
					try {
						List<NameValuePair> list = new ArrayList<NameValuePair>();
						list.add(new BasicNameValuePair("platform", "android"));
						list.add(new BasicNameValuePair("error", info));
						
						httppost.setEntity(new UrlEncodedFormEntity(list, HTTP.UTF_8));
						
						DefaultHttpClient client = WizMisc.getDefaultConnection();
						client.execute(httppost);
					}
					catch (Exception e) {
						httppost.abort();
					}
				}
				catch (Exception e) {
				}
				catch (OutOfMemoryError e) {
				}
				catch (Throwable e) {
				}
				//
				return null;
			}

			@Override
			public void onBegin(Object actionData) {
			}

			@Override
			public void onEnd(Object actionData, Object ret) {
			}

			@Override
			public void onException(Object actionData, Exception e) {
			}

			@Override
			public void onStatus(Object actionData, String status, int arg1,
					int arg2, Object obj) {
			}
			
		});
	}

	static public void logAction(Context ctx, String action) {
		logCore(ctx, action);
	}

	public static void logActionOneDay(Context ctx ,String action){
		try {
			SharedPreferences sp  = ctx.getSharedPreferences("config",Context.MODE_PRIVATE);
			int today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR); 
			if(today != sp.getInt(action, -1)){
				WizLogger.logAction(ctx, action);
				Editor editor = sp.edit();
				editor.putInt(action,today);
				editor.commit();
			}
		}
		catch (Exception e) {
		}
	}
	//
	static public void logException(String errorMessage) {
		crashCore(errorMessage);
	}
	//
	static public void logMessage(String message) {
		crashCore("sendMessage\n" + message);
	}
	//
	@SuppressWarnings("rawtypes")
	static public void logMessage(HashMap<String, Object> messages, String head) {
		if (messages == null)
			return;

		StringBuffer messageBuffer = new StringBuffer();
		messageBuffer.append(head + "\n");

		Iterator it = messages.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry entry = (Map.Entry) it.next();
			Object key = entry.getKey();
			Object value = entry.getValue();
			messageBuffer.append(key + ":" + value + "\n");
		}

		logMessage(messageBuffer.toString());
	}

	static public void logMessage(Object object, String head) {
		if (object == null)
			return;

		HashMap<String, Object> messages = WizObject.getClassProperty(object);
		logMessage(messages, head);
	}
	
	static public void logException(Context ctx, Throwable ex) {
		//
		try {
			ConcurrentHashMap<String, String> infos = getDeviceInfo(ctx);
			//
			StringBuffer sb = new StringBuffer();
			for (Map.Entry<String, String> entry : infos.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				sb.append(key + "=" + value + "\n");
			}

			sb.append("userId = " + WizAccountSettings.getUserId(ctx) + "\n");

			Writer writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			ex.printStackTrace(printWriter);
			Throwable cause = ex.getCause();
			while (cause != null) {
				cause.printStackTrace(printWriter);
				cause = cause.getCause();
			}
			printWriter.close();
			String result = writer.toString();
			sb.append(result);
			final String errorMessage = sb.toString();
			//
			//WizLogUtil.getInstance(ctx).logV("wizError", errorMessage);
			logException(errorMessage);
		}
		catch (Throwable e) {
		}
		
	}
	
	synchronized static public String getDeviceName(Context ctx) {
	
		try {
			ConcurrentHashMap<String, String> infos = getDeviceInfo(ctx);
			return infos.get("PRODUCT");
		} catch (Exception e) {
			return "";
		}
	}


	synchronized static public String getVersionCode(Context ctx) {
	
		try {
			ConcurrentHashMap<String, String> infos = getDeviceInfo(ctx);
			return infos.get("VERSIONCODE");
		} catch (Exception e) {
			return "";
		}
	}


	synchronized static public String getVersionName(Context ctx) {
	
		try {
			ConcurrentHashMap<String, String> infos = getDeviceInfo(ctx);
			return infos.get("VERSIONNAME");
		} catch (Exception e) {
			return "";
		}
	}
	
	/**
	 * 收集设备参数信息
	 * @param ctx
	 */
	static private ConcurrentHashMap<String, String> mInfos = null;

	@SuppressLint("DefaultLocale")
	synchronized static private ConcurrentHashMap<String, String> getDeviceInfo(Context ctx) {
		
		if (mInfos != null)
			return mInfos;
		//
		mInfos = new ConcurrentHashMap<String, String>();
		//
		try {
			PackageManager pm = ctx.getPackageManager();
			PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
			if (pi != null) {
				String versionName = pi.versionName == null ? "null" : pi.versionName;
				String versionCode = pi.versionCode + "";
				mInfos.put("VERSIONNAME", versionName);
				mInfos.put("VERSIONCODE", versionCode);
			}
		} catch (NameNotFoundException e) {
		}
		Field[] fields = Build.class.getDeclaredFields();
		for (Field field : fields) {
			try {
				field.setAccessible(true);
				mInfos.put(field.getName().toUpperCase(), field.get(null).toString());
			} catch (Exception e) {
			}
		}
		//
		return mInfos;
	}

	static public String getOEMSource(Context ctx) {
		String CHANNELID = "wiz";
		try {
			ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(
					ctx.getPackageName(), PackageManager.GET_META_DATA);
			Object value = ai.metaData.get("CHANNEL");
			if (value != null) {
				CHANNELID = value.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return CHANNELID;
	}
	
	public static final String OEM_HOLDAY_RULE = "oem_main_holiday";
	//
	static public HashMap<String, Integer> findOEMHoldayResourses(Context ctx, Class<?> clazz) {
		return findOEMResourses(ctx, clazz.getDeclaredFields(), OEM_HOLDAY_RULE);
	}

	public static final String OEM_SPLASH_RULE = "oem_splash";
	//
	static public Integer findOEMSplashResourse(Context ctx, Class<?> clazz) {
		final int splashResId = findOEMResourse(ctx, clazz.getDeclaredFields(), OEM_SPLASH_RULE);
		return splashResId;
	}
	//
	public static final String OEM_ABOUT_RULE = "oem_market";
	static public Integer findOEMAboutResourse(Context ctx, Class<?> clazz) {
		final int splashResId = findOEMResourse(ctx, clazz.getDeclaredFields(), OEM_ABOUT_RULE);
		return splashResId;
	}
	//
	static public boolean isFirstPublisher(Context ctx, Class<?> clazz) {
		return -1 != findOEMSplashResourse(ctx, clazz);
	}
	//
	@SuppressLint("DefaultLocale")
	static private Integer findOEMResourse(Context ctx, Field[] fields, String rule) {
		String oem = getOEMSource(ctx);
		for (Field field : fields) {
			String name = field.getName().toLowerCase();
			if (name.startsWith(rule) && name.indexOf(oem) != -1) {
				//发现资源并且是该渠道 
				try {
					return ((Integer) field.get(null));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return -1;
	}

	@SuppressLint("DefaultLocale")
	static private HashMap<String, Integer> findOEMResourses(Context ctx, Field[] fields, String rule) {
		HashMap<String, Integer> hashMap = new HashMap<String, Integer>();
		for (Field field : fields) {
			String name = field.getName().toLowerCase();
			if (name.startsWith(rule)) {
				//发现资源
				try {
					hashMap.put(name, ((Integer) field.get(null)));
				} catch (Exception e) {
				}
			}
		}
		return hashMap;
	}
	
	//
	public static String getVisitOEMAboutUrl(Context ctx) {
		String url = "http://api.wiz.cn/?p=wiz&plat=android&c=oem_link&a=" + getOEMSource(ctx);
		//
		return url;
	}
	
	@SuppressLint("DefaultLocale")
	public static boolean isMeizuMx(Context ctx) {
		String deviceName = getDeviceName(ctx);
		if (deviceName == null)
			return false;
		//
		deviceName = deviceName.toLowerCase();
		//
		return deviceName.startsWith("meizu_mx");
	}
	
	@SuppressLint("DefaultLocale")
	public static boolean isMeizu(Context ctx) {
		String deviceName = getDeviceName(ctx);
		if (deviceName == null)
			return false;
		//
		deviceName = deviceName.toLowerCase();
		//
		return deviceName.contains("meizu");
	}

}
