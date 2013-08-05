package cn.wiz.sdk.api;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;
import android.content.Context;
import cn.wiz.sdk.api.WizEventsCenter.WizSyncKbStep;
import cn.wiz.sdk.api.WizKSXmlRpcServer.WizAlterModifiedException;
import cn.wiz.sdk.api.WizObject.WizAttachment;
import cn.wiz.sdk.api.WizObject.WizDeletedGUID;
import cn.wiz.sdk.api.WizObject.WizDocument;
import cn.wiz.sdk.api.WizObject.WizKb;
import cn.wiz.sdk.api.WizObject.WizKbVersion;
import cn.wiz.sdk.api.WizObject.WizMessage;
import cn.wiz.sdk.api.WizObject.WizObjectBase;
import cn.wiz.sdk.api.WizObject.WizPersonalKb;
import cn.wiz.sdk.api.WizObject.WizTag;
import cn.wiz.sdk.api.WizObject.WizUserInfo;
import cn.wiz.sdk.api.WizStrings.WizStringId;
import cn.wiz.sdk.api.WizXmlRpcServer.WizKeyValue;
import cn.wiz.sdk.db.WizDatabase;
import cn.wiz.sdk.settings.WizAccountSettings;
import cn.wiz.sdk.settings.WizSystemSettings;
import cn.wiz.sdk.util.WizMisc;

public class WizSync extends Thread {


	private Context mContext;
	private String mUserId;
	private String mPassword;
	//
	private Boolean mUserCanceled = false;
	//
	private ConcurrentHashMap<String, WizKb> mKbs = new ConcurrentHashMap<String, WizKb>();
	
	private WizSync(Context ctx, String userId, String password) {
		mContext = ctx;
		mUserId = userId;
		mPassword = password;
	}
	//

	/*
	 * 一个虚假的kb，用于向map中添加同步完整数据的功能
	 * 这个kb需要记住是手工同步还是自动同步
	 */
	static public class WizSyncAllKb extends WizKb {
		//
		static final String SYNC_ALL_KB_GUID = "SyncAll";
		//
		private boolean mManualSyncAll = false;
		public WizSyncAllKb(boolean manualSyncAll) {
			mManualSyncAll = manualSyncAll;
			kbGuid = SYNC_ALL_KB_GUID;
		}
		public boolean isManualSyncAll() {
			return mManualSyncAll;
		}
		public boolean isPersonalKb() {
			return false;
		}
	}

	/*
	 * 同步功能，如果需要上传，则优先上传
	 * 然后进行完整同步
	 */
	public void run() {
		//
		this.setPriority(MIN_PRIORITY);
		//
		int errorCount = 0;
		while (!isStopSyncThread(mUserId)) {
			//
			WizKb kb = getKb();
			//
			//空闲
			if (kb == null) {
				//每次空闲的时候，都需要延长几秒钟
				doIdle(errorCount);
				//
				//检查是否需要自动进行完整同步
				checkNeedAutoSyncAll();
				continue;
			}
			//
			//完整同步
			if (kb instanceof WizSyncAllKb) {
				WizSyncAllKb syncAllKb = (WizSyncAllKb)kb;
				//
				boolean manualSyncAll = syncAllKb.isManualSyncAll();
				//
				//如果是手工同步，那么开始完整同步
				if (manualSyncAll) {
					startSyncAll(manualSyncAll); 
					continue;
				}
				//
				//如果是自动同步，则需要看看是否有错误发生
				//
				if (errorCount > 0) {
					//如果发生错误，则需要额外延长错误等待时间
					doIdle(errorCount);
				}
				//
				if (errorCount > 0)	//如果发生了错误，不再进行自动完整同步
					continue;
				//
				//等待一段时间后，执行完整同步
				if (!startSyncAll(false)) {
					errorCount++;
				}
				else {
					errorCount = 0;
				}
				continue;
			}
			//
			//
			//开始处理自动上传
			if (errorCount > 0) {
				//如果发生错误，则需要额外延长错误等待时间
				doIdle(errorCount);
			}
			//
			//如果禁止了自动同步，则不执行自动上传
			if (!WizSystemSettings.isAutoSync(mContext)) {
				continue;
			}
			//
			String token = getToken();	//网络问题？
			if (token == null) {
				errorCount++;
				continue;
			}
			//
			//同步单一kb，仅仅上传数据
			if  (!startSyncKb(token, kb)) {
				errorCount++;
			} else {
				errorCount = 0;
			}
		}
	}
	//
	private void addKb(WizKb kb) {
		//
		if (kb.isPersonalKb()) {
			mKbs.put("", kb);
		}
		else {
			mKbs.put(kb.kbGuid, kb);
		}
	}
	//
	private WizKb getKb() {
		if (mKbs.isEmpty())
			return null;
		//
		if (mKbs.contains("")) {	//私人笔记优先
			return mKbs.remove("");
		}
		//
		for (String key : mKbs.keySet()) {
			return mKbs.remove(key);
		}
		//
		return null;
	}
	//
	public boolean hasKb() {
		return !mKbs.isEmpty();
	}
	//
	/*
	 * 如果出现了错误，则延长idle
	 */
	private void doIdle (int errorCode) {
		int seconds = errorCode * errorCode * errorCode + 3;
		//
		int total = 1000 * seconds;	//3 + errorCount^3 seconds
		final int idle = 10;
		int count = total / idle;
		//
		for (int i = 0; i < count; i++) {
			//
			//在休息的时候，如果发现要完整同步，则立刻进行同步，
			//因为这是用户要求的
			if (needManualSyncAllNow())
				return;
			//
			if (isStopSyncThread(mUserId))
				return;
			//
			try {
				Thread.sleep(idle);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	//
	/*
	 * 获取token，用于上传数据的时候
	 */
	private String getToken() {
		try {
			return WizASXmlRpcServer.getToken(mContext, mUserId, mPassword);
		} catch (Exception e) {
			return null;
		}
	}

	///////////////////////////////////////////////////////////////
	//进行完整同步，或者同步单一kb
	/*
	 * 开始同步全部数据
	 */
	private boolean startSyncAll(boolean manualSyncAll) {
		if (WizDatabase.isAnonymousUserId(mUserId)) {
			return true;
		}
		//
		//开始同步全部数据
		boolean ret = syncAll(mContext, mUserId, mPassword, manualSyncAll);
		//更新全部笔记同步标记状态
		updateAllDataDownloadedStatus();
		//
		return ret;
	}
	
	/*
	 * 同步单一kb
	 */
	private boolean startSyncKb(String token, WizKb kb) {
		//
		if (WizDatabase.isAnonymousUserId(mUserId)) {
			return true;
		}
		//
		boolean ret = false;
		try {
			//先设置状态，再发送消息
			WizStatusCenter.setUploadingKb(mContext, mUserId, true);
			WizEventsCenter.sendSyncBeginMessage();
			WizEventsCenter.sendSyncStatusMessage(WizStringId.SYNC_START);
			//
			WizSyncKb syncKb = new WizSyncKb(mContext, mUserId, token, kb, true);	//自动同步
			syncKb.sync();
			//
			ret = true;
			//
			return true;
		} catch (Exception e) {
			WizEventsCenter.sendSyncExceptionMessage(e);
			return false;
		} finally {
			//先设置状态，再发送消息
			WizStatusCenter.setUploadingKb(mContext, mUserId, false);
			WizEventsCenter.sendSyncEndMessage(ret);
		}
	}
	//

	///////////////////////////////////////////////////////////////
	//手工设置是否进行完整同步
	//
	/*
	 * 设置是否需要完整的同步，需要设置是否是自动或者手工同步，如果是自动同步
	 * 需要判断wifi状态
	 */
	
	private void addSyncAllKb(boolean manualSyncAll) {
		addKb(new WizSyncAllKb(manualSyncAll));
		
		//
		if (manualSyncAll) {
			//
			//如果用户手工同步了，那么清除用户取消标记
			synchronized (mUserCanceled) {
				mUserCanceled = false;
			}
		}
	}
	//
	
	private boolean needManualSyncAllNow() {
		try {
			WizKb kb = mKbs.get(WizSyncAllKb.SYNC_ALL_KB_GUID);
			if (kb == null)
				return false;
			if (kb instanceof WizSyncAllKb) {
				return ((WizSyncAllKb)kb).isManualSyncAll();
			}
			return false;
		}
		catch (Exception e) {
			return false;
		}
	}

	/*
	 * 是否需要执行完整同步
	 */
	private void checkNeedAutoSyncAll() {
		//
		synchronized (mUserCanceled) {
			if (mUserCanceled)	//如果用户中止了同步，就不在自动检查是否需要下载数据了
				return;
		}
		//
		//
		if (!WizMisc.isWifi(mContext))
			return;
		//
		//如果没有开启自动同步，就不会自动下载数据
		if (!WizSystemSettings.isAutoSync(mContext))
			return;
		//
		//判断用户数据是否还没有下载完成
		if (isAllDataDownloaded())
			return;
		//
		addKb(new WizSyncAllKb(false));
	}
	//
	///////////////////////////////////////////////////////////////////
	//
	/*
	 * 判断是否所有的数据已经下载完毕，否则会自动在wifi状态下进行下载
	 */
	private int allDataDownloadedStatus = -1;
	private boolean isAllDataDownloadedCore() {
		WizDatabase db = WizDatabase.getDb(mContext, mUserId, null);
		if (db == null)
			return false;
		//
		if (null != db.getNextDocumentNeedToBeDownloaded())
			return false;
		//
		ArrayList<WizKb> kbs = db.getAllGroups();
		for (WizKb kb : kbs) {
			db = WizDatabase.getDb(mContext, mUserId, kb.kbGuid);
			if (null != db.getNextDocumentNeedToBeDownloaded())
				return false;
			//
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//
		return true;
	}
	/*
	 * 判断是否所有的数据已经下载完毕
	 */
	private boolean isAllDataDownloaded() {
		if (-1 == allDataDownloadedStatus) {
			allDataDownloadedStatus = isAllDataDownloadedCore() ? 1 : 0;
		}
		//
		return allDataDownloadedStatus == 1;
	}
	
	/*
	 * 需要更新一下数据同步状态
	 */
	private void updateAllDataDownloadedStatus() {
		allDataDownloadedStatus = -1;
	}
	//
	///////////////////////////////////////////////////////////////////////////
	/*
	 * 同步全部数据
	 */
	static private boolean syncAll(Context ctx, String userId, String password, boolean manualSyncAll) {

		if (WizDatabase.isAnonymousUserId(userId)) {
			return true;
		}
		//
		WizLogger.logActionOneDay(ctx, "sync");
		//
		boolean ret = false;
		try {
			try {
				//先设置状态，再发送消息
				WizStatusCenter.setSyncingAll(ctx, userId, true);
				WizEventsCenter.sendSyncBeginMessage();
				WizEventsCenter.sendSyncStatusMessage(WizStringId.SYNC_START);
				//
				syncAllCore(ctx, userId, password, manualSyncAll);
				//
				ret = true;
			} catch (Exception e) {
				WizEventsCenter.sendSyncExceptionMessage(e);
				//
				e.printStackTrace();
			}
		} finally {
			//先设置状态，再发送消息
			WizStatusCenter.setSyncingAll(ctx, userId, false);
			WizEventsCenter.sendSyncEndMessage(ret);
		}
		//
		return ret;
	}

	
	
	/////////////////////////////////////////////////////////////////////////////
	//
	//

	/*
	 * 是否停止同步全部数据操作，例如用户点击同步停止按钮
	 */
	static public boolean isStopSyncAll(String userId) {
		if (hasKbNeedToBeUpload(userId))
			return true;
		//
		if (!WizStatusCenter.isStoppingSyncAll(userId))
			return false;
		//
		WizSync sync= getCurrentSyncThread(userId);
		if (sync != null) {
			synchronized (sync.mUserCanceled) {
				sync.mUserCanceled = true;	//记住这个状态，不再进行自动同步
			}
		}
		//
		return true;
	}
	
	/*
	 * 是否要停止同步线程，例如切换账号，程序退出
	 */
	static boolean isStopSyncThread (String userId) {
		return WizStatusCenter.isStoppingSyncThread(userId);
	}
	//
	static WizSync getCurrentSyncThread(String userId) {
		Thread oldThread = WizStatusCenter.getCurrentSyncThread(userId);
		if (oldThread != null) {
			if (oldThread.isAlive())
				return (WizSync)oldThread;
		}
		//
		return null;
	}
	
	/*
	 * 开启同步线程
	 */
	static public void startSyncThread(Context ctx, String userId, String password) {
		//
		Thread oldThread = WizStatusCenter.getCurrentSyncThread(userId);
		if (oldThread != null) {
			if (oldThread.isAlive())
				return;
		}
		//
		WizSync newThread = new WizSync(ctx, userId, password);
		//
		WizStatusCenter.setCurrentSyncThread(userId, newThread);
		//
		newThread.start();
	}
	//
	/*
	 * 添加某一个kb进行同步，用于快速上传修改的数据
	 */
	static public boolean syncKb(String userId, WizKb kb) {
		//
		Thread thread = WizStatusCenter.getCurrentSyncThread(userId);
		if (thread == null)
			return false;
		//
		if (thread instanceof WizSync) {
			WizSync sync = (WizSync)thread;
			//
			sync.addKb(kb);
			//
			return true;
		}
		//
		return false;
	}
	
	//
	/*
	 * 添加某一个kb进行同步
	 */
	static public boolean syncKb(Context ctx, String userId, String kbGuid) {
		WizKb kb = WizDatabase.getDb(ctx, userId, kbGuid).getKbByGuid(kbGuid);
		if (kb==null)
			return false;
		//
		return syncKb(userId, kb);
	}
	//

	/*
	 * 开始完整同步
	 */
	static private boolean syncAll(Context ctx, String userId, boolean manualSyncAll) {
		Thread thread = WizStatusCenter.getCurrentSyncThread(userId);
		if (thread == null)
			return false;
		//
		if (thread instanceof WizSync) {
			WizSync sync = (WizSync)thread;
			//
			sync.addSyncAllKb(manualSyncAll);
			//
			return true;
		}
		//
		return false;
	}
	/*
	 * 手工同步全部数据
	 */
	static public boolean manualSyncAll(Context ctx, String userId) {
		return syncAll(ctx, userId, true);
	}
	/*
	 * 自动同步全部数据
	 */
	static public boolean autoSyncAll(Context ctx, String userId) {
		return syncAll(ctx, userId, false);
	}

	/*
	 * 判断是否还有kb需要同步
	 */
	static public boolean hasKbNeedToBeUpload(String userId) {
		Thread thread = WizStatusCenter.getCurrentSyncThread(userId);
		if (thread == null)
			return false;
		//
		if (thread instanceof WizSync) {
			WizSync sync = (WizSync)thread;
			//
			return sync.hasKb();
		}
		//
		return false;
	}

	//////////////////////////////////////////////////////////////////////////
	//
	/*
	 * 完整同步所有数据
	 */
	static private void syncAllCore(Context ctx, String userId, String password, boolean manualSyncAll)
			throws Exception {

		WizStatusCenter.setStoppingSyncAll(userId, false);
		//
		boolean autoSync = !manualSyncAll;
		boolean isWifi = WizMisc.isWifi(ctx);
		boolean isWifiOnlySetted = WizSystemSettings.isWifiOnlyDownloadData(ctx);
		//
		/*
		 * 自动同步，或者开始的时候是WiFi，或者设置了只在WiFi状态下下载离线数据
		 * 那么就会认为只能在WiFi下面下载数据
		 */
		//
		boolean wifiOnly = isWifi || autoSync || isWifiOnlySetted;
		WizASXmlRpcServer serverAccount = new WizASXmlRpcServer(ctx, userId);
		//
		try {
			//
			WizEventsCenter.sendSyncStatusMessage(WizStringId.SYNC_LOGIN);
			//
			WizUserInfo userInfo = serverAccount.clientLogin(password);
			//
			WizAccountSettings.addAccount(ctx, userId, password, userInfo.userGuid);
			WizDatabase db = WizDatabase.getDb(ctx, userId, null);
			db.onUserLogin(userInfo);
			//
			if (isStopSyncAll(userId))
				return;
			//
			ArrayList<WizKb> kbs = serverAccount.getGroupList();
			//
			db.saveDownloadKbs(kbs); // save kbs to settings
			WizEventsCenter.sendGroupInfoDownloadedMessage();
			
			syncBizMember(serverAccount, db);
			
			WizEventsCenter.sendSyncStatusMessage(WizStringId.SYNC_DOWNLOADING_MESSAGES);
			Map<String, WizSyncKb> messagesContentHelper = new HashMap<String, WizSyncKb>();
			for(WizKb kb : kbs){
				WizSyncKb syncKb = new WizSyncKb(ctx, userId, userInfo.token, kb, false);
				messagesContentHelper.put(kb.kbGuid, syncKb);
			}
			syncMessage(serverAccount, db, userId, messagesContentHelper);
			WizDatabase.getDb(ctx, userId, "").setUnreadMessagesCountDirty();
			
			// add personal kb
			String personalKbName = WizStrings
					.getString(WizStringId.PERSONAL_KB_NAME);
			WizKb personalKb = new WizPersonalKb(
					userInfo.personalKbDatabaseUrl, userInfo.personalKbGuid,
					personalKbName);
			kbs.add(0, personalKb);
			
			//
			if (isStopSyncAll(userId))
				return;
			//
			boolean hasUnreadDocuments = false;
			//
			int errorCount = 0;
			//
			for (WizKb kb : kbs) {
				if (isStopSyncAll(userId))
					return;
				//
				if (errorCount >= 10) {
					throw new Exception("Too many errors");
				}
				//
				WizSyncKb syncKb = new WizSyncKb(ctx, userId, userInfo.token,
						kb, false);
				//
				for (int i = 0; i < 2; i++) {
					try {
						syncKb.sync(); // 如果kb出错，可以忽略，同步下一个kb
						break; // 如果出错，同步两次
					} catch (Exception e) {
						errorCount++;
						e.printStackTrace();
					}
					//
					if (isStopSyncAll(userId))
						return;
				}
				//
				if (isStopSyncAll(userId))
					return;
				//
				if (!kb.isPersonalKb()) {
					if (WizDatabase.getDb(ctx, userId, kb.kbGuid).hasUnreadDocuments()) {
						hasUnreadDocuments = true;
						WizDatabase.getDb(ctx, userId, kb.kbGuid).setUnreadDocumentsCountDirty();
					}
				}
				//
				serverAccount.keepAlive(); // 检查token是否正常，如果出错，则终止
			}
			//
			//记录是否有未读的笔记
			//同步结束后，应该更新主界面右上角的图标，如果有新的笔记，增加提示。
			//用户点击后，可以显示哪一个群组有新的笔记
			//
			WizStatusCenter.setHasUnreadDocuments(ctx, userId, hasUnreadDocuments);
			//
			if (wifiOnly) {
				if (!WizMisc.isWifi(ctx))
					return;
			}
			//
			WizEventsCenter.sendSyncStatusMessage(WizStringId.SYNC_DOWNLOADING_NOTES_DATA);
			//
			for (WizKb kb : kbs) {
				if (isStopSyncAll(userId))
					return;
				//
				if (wifiOnly) {
					if (!WizMisc.isWifi(ctx))
						return;
				}
				//
				WizSyncKb syncKb = new WizSyncKb(ctx, userId, userInfo.token,
						kb, false);
				//
				syncKb.downloadData(wifiOnly);
				//
				if (isStopSyncAll(userId))
					return;
				//
				serverAccount.keepAlive(); // 检查token是否正常，如果出错，则终止
			}
		}
		//
		finally {
			try { // 注销不再报错
				serverAccount.clientLogout();
			} catch (Exception e) {
			}
		}
	}
	private static void syncBizMember(WizASXmlRpcServer serverAccount,
			WizDatabase personalDb) throws XmlRpcFault {
		Set<String> keys = personalDb.getBizKeys();
		for(String key : keys){
			long maxLocalVersion = personalDb.getBizMemberVersion(key);
			long serverVersion = serverAccount.getValueVersion(key);
			if(serverVersion > maxLocalVersion){
				WizKeyValue value = serverAccount.getValue(key);
				personalDb.saveBizMemberValue(key, value);
				personalDb.setBizMemberVersion(key, serverVersion);
			}
		}
	}
	private  static void syncMessage(WizASXmlRpcServer serverAccount,
			WizDatabase personalDb, String userId, Map<String, WizSyncKb> messagesContentHelper) throws XmlRpcFault, Exception {
		uploadMessages(serverAccount, personalDb, userId);
		downloadMessages(serverAccount, personalDb, userId, messagesContentHelper);
		WizEventsCenter.sendMessageDownloadedMessage();
	}

	private static void downloadMessages(WizASXmlRpcServer serverAccount,
			WizDatabase personalDb, String userId, Map<String, WizSyncKb> messagesContentHelper) throws Exception {

		long startVersion = personalDb.getMessageVersion();
		int count = 50;


		while (true) {
			if (isStopSyncAll(userId))
				return;
			ArrayList<WizMessage> messages = serverAccount.getMessages(
					startVersion + 1, count);
			//
			if (messages.size() > 0) {
				for(WizMessage message : messages){
					WizSyncKb kb = messagesContentHelper.get(message.kbGuid);
					try{
						WizDocument document = kb.mServer.getDocumentByGuid(message.documentGuid);
						kb.mDatabase.saveServerDocument(document);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				//
				personalDb.saveServerMessages(messages);
				startVersion = getMaxVersion(messages, startVersion);
				personalDb.setMessageVersion(startVersion);
			}
			//
			if (messages.size() < count)
				break;
		}
		//
	}
	private static <T> long getMaxVersion(ArrayList<T> arr, long currentMaxVersion) {
		long ret = currentMaxVersion;
		for (T elem : arr) {
			WizObjectBase obj = (WizObjectBase) elem;
			ret = Math.max(ret, obj.version);
		}
		//
		return ret;
	}
	private static void uploadMessages(WizASXmlRpcServer serverAccount,
			WizDatabase personalDb, String userId) throws XmlRpcException, XmlRpcFault {
		ArrayList<WizMessage> readedMessages = personalDb.getModifiedReadedMessages();
		ArrayList<WizMessage> unreadMessages = personalDb.getModifiedUnreadMessages();
		//
		if (readedMessages.size() == 0 && unreadMessages.size() == 0)
			return;
		//
		if (isStopSyncAll(userId))
			return;
		//
		String[] successIds = null ;
		if(readedMessages.size() > 0){
			successIds = serverAccount.changeServerReadStatus(readedMessages, 1);
			personalDb.onUploadedReadStatus(successIds);
		}
		if(unreadMessages.size() > 0){
			successIds = serverAccount.changeServerReadStatus(unreadMessages, 0);
			personalDb.onUploadedReadStatus(successIds);
		}
	}
	//
	

	/*
	 * 同步单一kb
	 */
	static public class WizSyncKb {

		private Context mContext;
		private String mUserId;
		private WizKb mKb;
		private WizKbVersion mKbServerVersion;
		private WizKbVersion mKbLocalVersion;
		private WizKSXmlRpcServer mServer;
		private boolean mUploadOnly;
		private WizDatabase mDatabase;
		private HashMap<String, WizKeyValue> mOldKeyValues;

		//
		public WizSyncKb(Context ctx, String userId, String token, WizKb kb,
				boolean uploadOnly) throws MalformedURLException {
			mContext = ctx;
			mUserId = userId;
			mKb = kb;
			mServer = new WizKSXmlRpcServer(ctx, kb.kbDatabaseUrl, userId,
					token, kb.kbGuid);
			mUploadOnly = uploadOnly;
			//
			mDatabase = WizDatabase.getDb(ctx, userId, kb.isPersonalKb() ? null
					: kb.kbGuid);
			//
			mKbLocalVersion = mDatabase.getVersions();
		}

		//
		/*
		 * 是否要进行停止
		 * 如果是仅仅上传同步，则判断是否要停止自动同步
		 * 否则判断是否停止全部
		 */
		private boolean isStop() {
			//
			//如果要停止线程，则全部终止
			if (isStopSyncThread(mUserId))
				return true;
			//
			if (mUploadOnly) { // auto sync
				return false;
			}
			//
			//如果在同步全部数据，判断是否要停止同步全部数据
			//也就是用户是否点击了停止按钮
			return isStopSyncAll(mUserId);
		}

		//

		/*
		 * 同步一个kb，进同步简单信息，不再这里下载数据
		 */
		public void sync() throws Exception {
			//
			if (WizDatabase.isAnonymousUserId(mUserId)) {
				return;
			}
			//
			boolean ret = false;
			//
			try {
				WizEventsCenter.sendSyncKbBeginMessage(mKb, mUploadOnly);
				WizEventsCenter.sendSyncStatusMessage(
						WizStringId.SYNC_KB_BEGIN, mKb.name);
				//
				syncCore();
				//
				ret = true;
			} finally {
				WizEventsCenter.sendSyncKbEndMessage(mKb, mUploadOnly, ret);
			}
		}
		
		public boolean canDownloadData(boolean wifiOnly) {
			if (wifiOnly) { // 开始同步的时候是wifi，然后中间中断了，变成了其他网络，则中断
				if (!WizMisc.isWifi(mContext)) {
					WizStatusCenter.setStoppingSyncAll(mUserId, true); // 停止全部同步
					WizEventsCenter.sendSyncStatusMessage(WizStringId.SYNC_DOWNLOADING_ATTACHMENTS);
					return false;
				}
			}
			//
			return true;
		}

		/*
		 * 下载数据
		 */
		//
		public void downloadData(boolean wifiOnly) {
			//
			int errorCount = 0;
			//
			while (true) {
				//
				if (isStop())
					return;
				//
				if (errorCount > 5) // 连续错误超过5个，终止下载
					return;
				//
				if (!canDownloadData(wifiOnly))
					return;
				//
				WizDocument doc = mDatabase.getNextDocumentNeedToBeDownloaded();
				if (doc == null)
					return;
				//
				try {
					//
					WizEventsCenter.sendSyncStatusMessage(
							WizStrings.WizStringId.SYNC_DOWNLOADING_NOTE,
							doc.title);
					//
					mDatabase.onBeforeDownloadDocument(doc);
					mServer.downloadDocument(doc.guid,
							doc.getZipFile(mContext, mUserId));
					mDatabase.onDocumentDownloaded(doc);
					//
					//
					// 更新笔记摘要，需要看看这个功能是否会对速度造成影响
					WizDocumentAbstractCache.forceUpdateAbstract(mUserId,
							doc.guid);
					//
					errorCount = 0;
				} catch (Exception e) { // 网络或者其它的错误，终止
					errorCount++;
				}
			}
		}

		//
		//
		private void onStep(WizSyncKbStep step) {
			if (this.mUploadOnly) // 自动同步不发送这些消息
				return;
			//
			WizEventsCenter.sendSyncKbStepMessage(mKb, step);
		}
		private void syncCore() throws Exception {
			//
			mOldKeyValues = null;
			//
			mKbServerVersion = mServer.getVersions();
			//
			if (isStop())
				return;
			// upload deleted list
			onStep(WizSyncKbStep.BeforeUploadDeletedGUIDs);
			uploadDeletedGUIDs();
			onStep(WizSyncKbStep.AfterUploadDeletedGUIDs);
			//
			if (isStop())
				return;
			// download deleted list
			onStep(WizSyncKbStep.BeforeDownloadDeletedGUIDs);
			downloadDeletedGUIDs();
			onStep(WizSyncKbStep.AfterDownloadDeletedGUIDs);
			//
			if (isStop())
				return;
			//
			onStep(WizSyncKbStep.BeforeDownloadKeyValues);
			uploadKeyValues();
			onStep(WizSyncKbStep.AfterDownloadKeyValues);
			// upload tag list
			onStep(WizSyncKbStep.BeforeUploadTags);
			uploadTags();
			onStep(WizSyncKbStep.AfterUploadTags);
			//
			if (isStop())
				return;
			// upload documents
			onStep(WizSyncKbStep.BeforeUploadDocuments);
			uploadDocuments();
			onStep(WizSyncKbStep.AfterUploadDocuments);

			if (isStop())
				return;
			// upload attachments
			onStep(WizSyncKbStep.BeforeUploadAttachments);
			uploadAttachments();
			onStep(WizSyncKbStep.AfterUploadAttachments);
			//
			if (mUploadOnly)
				return;
			//
			if (isStop())
				return;
			//
			// process key values
			onStep(WizSyncKbStep.BeforeDownloadKeyValues);
			downloadKeyValues();	//全部处理
			onStep(WizSyncKbStep.AfterDownloadKeyValues);
			//
			if (isStop())
				return;
			// download tag list
			onStep(WizSyncKbStep.BeforeDownloadTags);
			downloadTags();
			onStep(WizSyncKbStep.AfterDownloadTags);
			//
			if (isStop())
				return;
			// download documents list
			onStep(WizSyncKbStep.BeforeDownloadDocuments);
			downloadDocuments();
			onStep(WizSyncKbStep.AfterDownloadDocuments);
			//
			//
			// 重新更新服务器的数据，因为如果pc客户端文件夹被移动后，
			// 服务器上面已经没有这个文件夹了，
			// 但是手机同步的时候，因为原有的文件夹里面还有笔记，
			// 因此不会被删除，导致手机上还有空的文件夹
			// 因此在这里需要重新更新一下
			processOldKeyValues();
			//
			if (isStop())
				return;
			// download attachments list
			// Don't download attachment data at now
			onStep(WizSyncKbStep.BeforeDownloadAttachments);
			downloadAttachments();
			onStep(WizSyncKbStep.AfterDownloadAttachments);
			//
			mOldKeyValues = null;
		}

		// process key values
		void uploadKeyValues() {
			//
			// 只需要更新个人数据文件夹。群组不需要
			if (!mKb.isPersonalKb())
				return;
			//
			String[] keys = mDatabase.getAllKeys();
			//
			for (String key : keys) {
				uploadKeyValue(key);
			}
		}

		void downloadKeyValues() {
			//
			// 只需要更新个人数据文件夹。群组不需要
			if (!mKb.isPersonalKb())
				return;
			//
			String[] keys = mDatabase.getAllKeys();
			//
			for (String key : keys) {
				downloadKeyValue(key);
			}
		}

		//
		// 不抛出异常，忽略所有的异常，可以让同步继续进行下去
		void uploadKeyValue(String key) {
			//
			try {
				long localVersion = mDatabase.getKeyValueVersion(key);
				//
				if (localVersion == -1) { // upload key value
					String val = mDatabase.getKeyValue(key);
					//
					if (val != null) {
						long nServerVersion = mServer.setValue(key, val);
						mDatabase.setKeyValueVersion(key, nServerVersion);
					}

				}
			} catch (Exception e) {
			}
		}
		//
		// 不抛出异常，忽略所有的异常，可以让同步继续进行下去
		void downloadKeyValue(String key) {
			//
			if (mOldKeyValues == null) {
				mOldKeyValues = new HashMap<String, WizKeyValue>();
			}
			//
			try {
				long localVersion = mDatabase.getKeyValueVersion(key);
				//
				long serverVersion = mServer.getValueVersion(key);
				if (serverVersion > localVersion) { // download key value
					WizKeyValue data = mServer.getValue(key);
					//
					if (data.value != null) {
						//
						//第一次先不要设置版本号
						//需要等同步之后再重新更新数据，设置一下版本号
						mDatabase.saveKeyValue(key, data, false); 
						//
						mOldKeyValues.put(key, data);
					}
				}
			} catch (Exception e) {
			}
		}

		/*
		 * 重新设置服务器的key value数据 防止被移动的文件夹没有删除
		 */
		private void processOldKeyValues() {
			if (mOldKeyValues == null)
				return;
			//
			for (String key : mOldKeyValues.keySet()) {
				WizKeyValue data = mOldKeyValues.get(key);
				if (data != null) {
					mDatabase.saveKeyValue(key, data, true); // 最后一次才记住版本号
				}
			}
		}

		static private <T> long getMaxVersion(ArrayList<T> arr,
				long currentMaxVersion) {
			long ret = currentMaxVersion;
			for (T elem : arr) {
				WizObjectBase obj = (WizObjectBase) elem;
				ret = Math.max(ret, obj.version);
			}
			//
			return ret;
		}

		//
		final static private int LIST_COUNT = 50;

		static private <T> ArrayList<T> extractSubList(ArrayList<T> arr,
				int listCount) {
			ArrayList<T> subArr;
			//
			if (arr.size() < listCount) {
				subArr = arr;
			} else {
				subArr = new ArrayList<T>();
				for (int i = 0; i < listCount; i++) {
					subArr.add(arr.remove(0));
				}
			}
			//
			return subArr;
		}

		private void uploadDeletedGUIDs() throws XmlRpcException, XmlRpcFault {
			ArrayList<WizDeletedGUID> arr = mDatabase.getModifiedDeletedGUIDs();
			//
			if (arr.size() == 0)
				return;
			WizEventsCenter
			.sendSyncStatusMessage(WizStringId.SYNC_UPLOADING_DELETED_GUIDS);
			//
			while (true) {
				//
				if (isStop())
					return;
				//
				ArrayList<WizDeletedGUID> subArr = extractSubList(arr,
						LIST_COUNT);
				if (subArr.size() == 0)
					break;
				//
				mServer.postList("deleted.postList", "deleteds", subArr);
				mDatabase.onUploadedDeletedGUIDs(subArr);
				//
				if (subArr.size() < LIST_COUNT)
					break;
			}
		}

		private void uploadTags() throws XmlRpcException, XmlRpcFault {
			//
			ArrayList<WizTag> arr = mDatabase.getModifiedTags();
			//
			if (arr.size() == 0)
				return;
			//
			WizEventsCenter
					.sendSyncStatusMessage(WizStringId.SYNC_UPLOADING_TAGS);
			//
			while (true) {
				//
				if (isStop())
					return;

				ArrayList<WizTag> subArr = extractSubList(arr, LIST_COUNT);
				if (subArr.size() == 0)
					break;
				//
				mServer.postList("tag.postList", "tags", subArr);
				mDatabase.onUploadedTags(subArr);
				//
				if (subArr.size() < LIST_COUNT)
					break;
			}
		}

		private void uploadDocuments() {
			ArrayList<WizDocument> arr = mDatabase.getModifiedDocuments();
			//
			for (WizDocument doc : arr) {
				if (isStop())
					return;
				// 强制更新笔记附件数量
				doc.attachmentCount = mDatabase.getDocumentAttachmentCount(doc.guid);
				for (int i = 0; i < 2; i++) {
					try {
						mServer.uploadDocument(doc, doc.getZipFile(mContext, mUserId));
						WizEventsCenter.sendSyncStatusMessage(WizStringId.SYNC_UPLOADING_DOCUMENT, doc.title);
						mDatabase.onUploadedDocument(doc);
						break;
					} catch (WizAlterModifiedException e) {
					} catch (IOException e) {
					} catch (Exception e) {
						String errorMessage = "userId:" + mUserId;
						errorMessage += "\nkbGuid:" + mDatabase.getKbGuid();
						errorMessage += "\ndocument.guid:" + doc.guid;
						errorMessage += "\ndocument.title:" + doc.title;
						errorMessage += "\nErrorMessage:" + e.getMessage();
						WizLogger.logException(errorMessage);
					}
				}
			}
		}

		private void uploadAttachments() {
			ArrayList<WizAttachment> arr = mDatabase.getModifiedAttachments();
			//
			for (WizAttachment att : arr) {
				if (isStop())
					return;

				for (int i = 0; i < 2; i++) {
					try {
						if (mDatabase.getDocumentByGuid(att.docGuid) != null) {
							mServer.uploadAttachment(att, att.getZipFile(mContext, mUserId));
							WizEventsCenter.sendSyncStatusMessage(WizStringId.SYNC_UPLOADING_ATTACHMENT, att.name);
						}
						mDatabase.onUploadedAttachment(att);
						break;
					} catch (WizAlterModifiedException e) {
					} catch (IOException e) {
					} catch (Exception e) {
						String errorMessage = "userId:" + mUserId;
						errorMessage += "\nkbGuid:" + mDatabase.getKbGuid();
						errorMessage += "\nAttachment.guid:" + att.guid;
						errorMessage += "\nAttachment.title:" + att.name;
						errorMessage += "\nErrorMessage:" + e.getMessage();
						WizLogger.logException(errorMessage);
					}
				}
			}
		}

		private void downloadDeletedGUIDs() throws XmlRpcException, XmlRpcFault {
			if (mKbLocalVersion.deletedVersion >= mKbServerVersion.deletedVersion)
				return;
			//
			// 判断是否有对象，如果没有对象，则不需要下载任何删除记录，可以大大加快同步速度
			// 尤其是用户数据比较多的时候，删除的记录很多
			if (!mDatabase.hasObjects()) {
				mDatabase
						.setDeletedGUIDsVersion(mKbServerVersion.deletedVersion);
				return;
			}
			//
			WizEventsCenter
					.sendSyncStatusMessage(WizStringId.SYNC_DOWNLOADING_DELETED_GUIDS);
			//
			long startVersion = mKbLocalVersion.deletedVersion;
			int count = 50;

			while (true) {
				if (isStop())
					return;
				ArrayList<WizDeletedGUID> deleteds = mServer.getDeleteds(
						startVersion + 1, count);
				if (deleteds.size() > 0) {
					mDatabase.saveServerDeletedGUIDs(deleteds);
					//
					startVersion = getMaxVersion(deleteds, startVersion);
					mDatabase.setDeletedGUIDsVersion(startVersion);
				} else {
					mDatabase
							.setDeletedGUIDsVersion(mKbServerVersion.deletedVersion);
				}
				//
				if (deleteds.size() < count)
					break;
			}
		}

		private void downloadTags() throws XmlRpcException, XmlRpcFault {
			//
			if (mKbLocalVersion.tagVersion >= mKbServerVersion.tagVersion)
				return;
			//
			WizEventsCenter
					.sendSyncStatusMessage(WizStringId.SYNC_DOWNLOADING_TAGS);
			//
			long startVersion = mKbLocalVersion.tagVersion;
			int count = 50;

			while (true) {
				if (isStop())
					return;
				ArrayList<WizTag> tags = mServer.getTags(startVersion + 1,
						count);
				if (tags.size() > 0) {
					WizEventsCenter.sendSyncStatusMessage(tags.get(0).name);
					//
					mDatabase.saveServerTags(tags);
					//
					startVersion = getMaxVersion(tags, startVersion);
					mDatabase.setTagsVersion(startVersion);
				} else {
					mDatabase.setTagsVersion(mKbServerVersion.tagVersion);
				}
				//
				if (tags.size() < count)
					break;
			}
		}

		private void downloadDocuments() throws Exception {
			if (mKbLocalVersion.documentVersion >= mKbServerVersion.documentVersion)
				return;
			//
			WizEventsCenter
					.sendSyncStatusMessage(WizStringId.SYNC_DOWNLOADING_DOCUMENTS);
			//
			long startVersion = mKbLocalVersion.documentVersion;
			int count = 50;

			while (true) {
				if (isStop())
					return;
				ArrayList<WizDocument> documents = mServer.getDocuments(
						startVersion + 1, count);
				//
				if (documents.size() > 0) {
					//
					WizEventsCenter
							.sendSyncStatusMessage(documents.get(0).title);
					//
					mDatabase.saveServerDocuments(documents);
					//
					startVersion = getMaxVersion(documents, startVersion);
					mDatabase.setDocumentsVersion(startVersion);
					//
					// 尽早通知笔记列表下载，更新界面，例如每下载50个就通知一次
					WizEventsCenter.sendSyncKbStepMessage(mKb,
							WizSyncKbStep.AfterDownloadDocuments);
				} else {
					mDatabase
							.setDocumentsVersion(mKbServerVersion.documentVersion);
				}
				//
				if (documents.size() < count)
					break;
			}
		}


		private void downloadAttachments() throws XmlRpcException, XmlRpcFault {
			if (mKbLocalVersion.attachmentVersion >= mKbServerVersion.attachmentVersion)
				return;
			//
			WizEventsCenter
					.sendSyncStatusMessage(WizStringId.SYNC_DOWNLOADING_ATTACHMENTS);
			//
			long startVersion = mKbLocalVersion.attachmentVersion;
			int count = 50;

			while (true) {
				if (isStop())
					return;
				ArrayList<WizAttachment> attachments = mServer.getAttachments(
						startVersion + 1, count);
				//
				if (attachments.size() > 0) {
					WizEventsCenter
							.sendSyncStatusMessage(attachments.get(0).name);
					//
					mDatabase.saveServerAttachments(attachments);
					//
					startVersion = getMaxVersion(attachments, startVersion);
					mDatabase.setAttachmentsVersion(startVersion);
				} else {
					mDatabase.setAttachmentsVersion(mKbServerVersion.attachmentVersion);
				}
				//
				if (attachments.size() < count)
					break;
			}
		}
	}
}
