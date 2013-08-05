package cn.wiz.sdk.api;

import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;
import android.content.Context;
import android.text.TextUtils;
import cn.wiz.sdk.api.WizObject.WizAvatar;
import cn.wiz.sdk.db.WizGlobalDatabase;
import cn.wiz.sdk.settings.WizSystemSettings;
import cn.wiz.sdk.util.ImageUtil;
import cn.wiz.sdk.util.WizMisc;
/**
 * 内存中存放的是圆角图标.  数据库和网络中获取的是原图  对外提供圆角图
 * @author wiz_chentong
 */
public class WizAvatarCache extends Thread {
	
	private Context mContext;
	private String mAvatarUrl ;
	private String mUserGuid;
	private AvatarLoadStack mAvatarLoadStack = new AvatarLoadStack();
	private AvatarMemCache mCacheQueue = new AvatarMemCache();
	
	private WizAvatarCache(Context ctx, String userGuid) {
		mContext = ctx;
		mUserGuid = userGuid;
	}
	
	@Override
	public void run() {
		//
		this.setPriority(MIN_PRIORITY);
		//
		while (!isStop(mUserGuid)) {
			String userGuid = mAvatarLoadStack.get();
			//
			if (userGuid == null) {
				try {
					sleep(100);
				} catch (InterruptedException e) {
				}
				//
				continue;
			}
			
			//db
			WizAvatar avatar = getAvatarFromDb(userGuid);
			if(avatar != null){
				WizEventsCenter.sendAvatarDownloadedMessage(avatar);
				continue ;
			}
			
			//net
			try {
				avatar = getAvatarFromServer(userGuid);
			} catch (Exception e) {
				e.printStackTrace();
			} 
			if(avatar != null){
				WizEventsCenter.sendAvatarDownloadedMessage(avatar);
			}
			
//			else{
//				mDownloadStack.add(userGuid);//失败重新加入
//			}
			//
//			try {
//				sleep(10);
//			} catch (InterruptedException e) {
//			}
			
		}
		
	}

	private boolean isStop(String userId) {
		return WizStatusCenter.isStoppingDownloadAvatarThread(userId);
	}
	/**
	 * 下载头像
	 * @param ctx
	 * @param avatarGuid
	 * @return
	 */
	private WizAvatar loadAvatar(String avatarGuid) {
		//ram
		WizAvatar avatar = mCacheQueue.get(avatarGuid);
		if(avatar != null){
			return avatar;
		}
		mAvatarLoadStack.add(avatarGuid);
		return null ;
	}
	private boolean needReload(WizAvatar avatar){
		Calendar calender = Calendar.getInstance();
		calender.setTimeInMillis(avatar.lastModified);
		int lastModifiedDay = calender.get(Calendar.DAY_OF_YEAR);
		calender.setTime(new Date());
		int today =calender.get(Calendar.DAY_OF_YEAR) ;
		if(today != lastModifiedDay && WizMisc.isWifi(mContext)){
			return true;
		}
		return false;
	}
	/**
	 * 从Db获取Avatar
	 * @param userGuid
	 * @return
	 */
	private WizAvatar getAvatarFromDb(String userGuid) {
		//db
		WizAvatar avatar = WizGlobalDatabase.getDb(mContext).getAvatarById(userGuid);
		if(avatar == null || needReload(avatar)){
			return null ;
		}
		//db
		avatar.bitmap = ImageUtil.getRoundCornerBitmap(mContext ,avatar.bitmap);
		mCacheQueue.add(userGuid, avatar);
		return avatar;
	}
	/**
	 * 
	 * 获取头像地址。
	 * 
	 * @param avatarGuid
	 * @return
	 * @throws XmlRpcException
	 * @throws XmlRpcFault
	 */
	private String getAvatarUrl(String avatarGuid) throws XmlRpcException, XmlRpcFault {
		
		String url = null ;
		if(mAvatarUrl == null ){
			try {
				mAvatarUrl = WizMisc.getUrlFromApi("avatar");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		url = mAvatarUrl.replaceAll("\\{userGuid\\}", avatarGuid);
		return url;
	}
	/**
	 * 从服务器获取头像。获取失败返回null
	 * @param ctx
	 * @return
	 */
	private WizAvatar getAvatarFromServer(String avatarGuid) {
		//未开启
		boolean isWifi = WizMisc.isWifi(mContext);
		boolean isWifiOnlySetted = WizSystemSettings.isWifiOnlyDownloadData(mContext);
		//
		/*
		 * 开始的时候是WiFi，或者设置了只在WiFi状态下下载离线数据
		 * 那么就会认为只能在WiFi下面下载数据
		 */
		// 
		boolean canDownloadData = isWifi || !isWifiOnlySetted;
		if(!canDownloadData){
			return null;
		}
		try {
			String imageUrl = getAvatarUrl(avatarGuid);
			URL url = new URL(imageUrl);
			URLConnection conn = url.openConnection();
			conn.setConnectTimeout(5000);
			conn.connect();
			WizGlobalDatabase db = WizGlobalDatabase.getDb(mContext);
			db.setAvatar(avatarGuid, conn.getInputStream());
			WizAvatar avatar = db.getAvatarById(avatarGuid);
			if(avatar != null){
				avatar.bitmap = ImageUtil.getRoundCornerBitmap(mContext ,avatar.bitmap);
				mCacheQueue.add(avatarGuid, avatar);
				return avatar;
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * 获取头像。
	 * @param ctx
	 * @param userId
	 * @param avatarGuid  也可以使avatarUserId
	 * @return
	 */
	public static WizAvatar getAvatar(Context ctx, String userId, String avatarGuid) {
		Thread thread = startAvatarThread(ctx, userId);
		if(thread != null){
			//
			if (thread instanceof WizAvatarCache) {
				WizAvatarCache cacheThread = (WizAvatarCache)thread;
				//
				return cacheThread.loadAvatar(avatarGuid);
			}
		}
		//
		return null;
	}
	private static Thread startAvatarThread(Context ctx, String userId) {
		//已经开启
		Thread oldThread = WizStatusCenter.getCurrentAvatarThread(userId);
		if (oldThread != null) {
			if (oldThread.isAlive())
				return oldThread;
		}
		WizAvatarCache newThread = new WizAvatarCache(ctx, userId);
		//
		WizStatusCenter.setCurrentAvatarThread(userId, newThread);
		//
		newThread.start();
		return newThread;
	}
	
	/**
	 * 头像加载栈,用于记录需要加载的头像guid
	 * @author wiz_chentong
	 *
	 */
	private class AvatarLoadStack{
		//保证不重复
		private Set<String> mAvatarsSet = new HashSet<String>();
		//控制移除顺序
		private LinkedList<String> mAvatarStack = new LinkedList<String>();
		public void add(String avatarGuid){       		//主线程
			if (mAvatarsSet.contains(avatarGuid)){
				//update
				mAvatarStack.remove(avatarGuid);
				mAvatarStack.addFirst(avatarGuid);
				return ;
			}
			mAvatarsSet.add(avatarGuid);
			mAvatarStack.addFirst(avatarGuid);
		}
		/**
		 * @return avatarGuid
		 */
		public String get(){							//子线程
			if (mAvatarsSet.isEmpty())	
				return null;
			//
			String guid = mAvatarStack.removeFirst();//从最近的开始下载
			mAvatarsSet.remove(guid);
			return guid;
		}
	}
	/**
	 * 头像内存缓存 队列
	 * @author wiz_chentong
	 *
	 */
	private class AvatarMemCache{
		//存放
		private Map<String, WizAvatar> mAvatarMemCache = new HashMap<String, WizAvatar>();
		//控制缓存过大时的移除顺序: 移除第一个加入的    队列: 先进先出
		private LinkedList<String> mAvatarQueue = new LinkedList<String>();
		//缓存大小
		private static final int CACHE_NUM = 20;
		/**
		 * 加入到缓存队列
		 * @param avatarGuid
		 * @param avatar
		 */
		public void add(String avatarGuid, WizAvatar avatar){	//子线程
			
			if (mAvatarMemCache.containsKey(avatarGuid)){
				mAvatarMemCache.put(avatarGuid, avatar);//只更新  不加入缓存
				return ;
			}
			
			//加入缓存队列
			mAvatarQueue.addLast(avatarGuid);
			mAvatarMemCache.put(avatarGuid, avatar);//添加
			
			// 保证cache不过大
			if(mAvatarMemCache.size() > CACHE_NUM){
				//移除首位
				String tempAvatarGuid = mAvatarQueue.removeFirst();
				//如果是当前用户的头像.则不做处理.不移除  尽量保证侧滑栏不出现默认头像
				if(TextUtils.equals(avatarGuid, mUserGuid)){
					mAvatarQueue.addLast(mUserGuid);
					tempAvatarGuid = mAvatarQueue.removeFirst();
				}
				WizAvatar temp = mAvatarMemCache.remove(tempAvatarGuid);
				temp.bitmap.recycle();
				temp = null ;
			}
		}
		/**
		 * 没有返回null
		 * @param avatarGuid
		 * @return
		 */
		public WizAvatar get(String avatarGuid){					 //主线程
			if (mAvatarMemCache.isEmpty())
				return null;
			//
			return mAvatarMemCache.get(avatarGuid);
		}
	}
}
