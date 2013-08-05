package cn.wiz.sdk.api;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import cn.wiz.sdk.api.WizObject.WizDocument;
import cn.wiz.sdk.api.WizObject.WizKb;
import cn.wiz.sdk.api.WizObject.WizUserInfo;
import cn.wiz.sdk.db.WizDatabase;
import cn.wiz.sdk.settings.WizAccountSettings;

import android.content.Context;
import android.text.TextUtils;

/**
 * @Author: zwy
 * @E-mail: weiyazhang1987@gmail.com
 * @time Create Date: 2013-6-3上午9:53:19
 * @Message: 此类用于下载笔记
 **/
public class WizDownloadDocuments extends Thread {

	private Context mContext;
	private String mUserId;

	public WizDownloadDocuments(Context ctx, String user) {
		mContext = ctx;
		mUserId = user;
	}

	private WizASXmlRpcServer mAsServer;
	private HashMap<String, WizKb> mKbs = new HashMap<String, WizObject.WizKb>();

	private WizKb getKWizKb(String kbGuid) throws Exception {
		if (mKbs.containsKey(kbGuid)) {
			return mKbs.get(kbGuid);
		}

		checkAsServer();

		ArrayList<WizKb> kbs = mAsServer.getAllKbList();
		for (WizKb wizKb : kbs) {
			if (wizKb == null)
				continue;
			mKbs.put(wizKb.kbGuid, wizKb);
		}

		if (mKbs.containsKey(kbGuid)) {
			return mKbs.get(kbGuid);
		}

		return null;

	}

	/**
	 * 检测用户是否登录
	 * 
	 * @throws Exception
	 */
	private void checkAsServer() throws Exception {
		if (mAsServer == null) {
			mAsServer = new WizASXmlRpcServer(mContext, mUserId);
			String password = WizAccountSettings.getAccountPasswordByUserId(mContext, mUserId);
			WizUserInfo userInfo = mAsServer.clientLogin(password);

			//确保在离线个人账户下的笔记时查询到personalKbGuid
			WizDatabase db = WizDatabase.getDb(mContext, mUserId, null);
			db.onUserLogin(userInfo);
		}

	}

	/**
	 * 获取kb对应的WizKSXmlRpcServer
	 */
	private HashMap<String, WizKSXmlRpcServer> mKsServers = new HashMap<String, WizKSXmlRpcServer>();
	private WizKSXmlRpcServer getKsServer(String kbGuid) throws Exception {

		if (mKsServers.containsKey(kbGuid)) {
			return mKsServers.get(kbGuid);
		}

		WizKb kb = getKWizKb(kbGuid);
		if (kb == null)
			return null;

		checkAsServer();

		WizKSXmlRpcServer server = new WizKSXmlRpcServer(mContext, kb.kbDatabaseUrl, mUserId, mAsServer.getToken(), kbGuid);
		mKsServers.put(kbGuid, server);

		return server;
	}

	private class DownloadData {
		private WizDocument mDocument;
		private String mKbGuid;

		public DownloadData(String kbGuid, WizDocument document) {
			mKbGuid = kbGuid;
			mDocument = document;
		}

		public WizDocument getDocument() {
			return mDocument;
		}

		public String getKbGuid() {
			return mKbGuid;
		}

	}

	/**
	 * 添加一组下载任务
	 * @param ctx
	 * @param userId
	 * @param kbGuid
	 * @param documents
	 */
	public static void addDownloadActions(Context ctx, String userId, String kbGuid, ArrayList<WizDocument> documents){
		if (TextUtils.isEmpty(userId))
			return;

		Thread oldThread = WizStatusCenter.getCurrentDownloadDocumentsThread(userId);
		if (oldThread == null) {
			startThread(ctx, userId);
		}

		for (WizDocument document : documents) {
			if (document == null)
				continue;

			addDownloadAction(userId, kbGuid, document);
		}
	}

	/**
	 * 添加一个下载任务
	 * @param ctx
	 * @param userId
	 * @param kbGuid
	 * @param document
	 */
	public static void addDownloadAction(Context ctx, String userId, String kbGuid, WizDocument document){
		if (TextUtils.isEmpty(userId))
			return;

		Thread oldThread = WizStatusCenter.getCurrentDownloadDocumentsThread(userId);
		if (oldThread == null) {
			startThread(ctx, userId);
		}

		addDownloadAction(userId, kbGuid, document);
	}

	/**
	 * 添加个下载任务，此时已确定该UserId对应线程启动
	 * @param userId
	 * @param kbGuid
	 * @param document
	 */
	private static void addDownloadAction(String userId, String kbGuid, WizDocument document){

		Thread thread = WizStatusCenter.getCurrentDownloadDocumentsThread(userId);
		if (thread instanceof WizDownloadDocuments) {
			((WizDownloadDocuments) thread).addDownloadAction(kbGuid, document);
		}
	}

	private Stack<String> mDownloadKeys = new Stack<String>();
	private HashMap<String, DownloadData> mDownloadDatas = new HashMap<String, WizDownloadDocuments.DownloadData>();
	synchronized public void addDownloadAction(String kbGuid, WizDocument document) {
		String key = document.guid;
		if (mDownloadKeys.contains(key)) 
			return;

		if (mDownloadDatas == null) 
			mDownloadDatas = new HashMap<String, WizDownloadDocuments.DownloadData>();

		DownloadData data = new DownloadData(kbGuid, document);
		mDownloadDatas.put(key, data);
		mDownloadKeys.add(key);
	}

	synchronized private DownloadData getDownloadData() {

		if (mDownloadKeys.isEmpty()) {
			return null;
		}

		String key = mDownloadKeys.pop();

		if (mDownloadDatas == null)
			mDownloadDatas = new HashMap<String, WizDownloadDocuments.DownloadData>();

		if (mDownloadDatas.containsKey(key))
			return mDownloadDatas.remove(key);

		return null;
	}

	private void downloadDocument(String kbGuid, WizDocument document)
			throws Exception {
		WizDatabase db = WizDatabase.getDb(mContext, mUserId, kbGuid);
		if (TextUtils.isEmpty(kbGuid)) {
			kbGuid = db.getPersonalKb().kbGuid;// :检测调试
		}

		WizKSXmlRpcServer server = getKsServer(kbGuid);

		if (server == null)
			return;

		int errorCount = 0;
		while (true) {
			//
			if (errorCount > 5) // 连续错误超过5个，终止下载
				break;
			//
			try {
				//
				// WizEventsCenter.sendSyncStatusMessage(WizStrings.WizStringId.SYNC_DOWNLOADING_NOTE,
				// document.title);
				//
				db.onBeforeDownloadDocument(document);
				File zipFile = document.getZipFile(mContext, mUserId);
				server.downloadDocument(document.guid, zipFile);
				db.onDocumentDownloaded(document);
				//
				// 更新笔记摘要，需要看看这个功能是否会对速度造成影响
				WizDocumentAbstractCache.forceUpdateAbstract(mUserId, document.guid);

				break;
			} catch (Exception e) { // 网络或者其它的错误，终止
				errorCount++;
			}
		}
	}

	private boolean mStop = false;

	private boolean isStop() {
		return mStop;
	}

	public static void setStop(String userId){
		Thread oldThread = WizStatusCenter.getCurrentDownloadDocumentsThread(userId);
		if (oldThread == null)
			return;

		if (oldThread instanceof WizDownloadDocuments) {
			((WizDownloadDocuments) oldThread).setStop(true);
		}
	}

	protected void setStop(boolean stop) {
		mStop = stop;
	}

	@Override
	public void run() {
		super.run();

		while (!isStop()) {
			try {
				DownloadData data = getDownloadData();
				WizDocument document = null;
				if (data == null) {
					Thread.sleep(1000);
				} else {
					document = data.getDocument();
				}

				if (document == null)
					continue;

				String kbGuid = data.getKbGuid();
				downloadDocument(kbGuid, document);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void startThread(Context ctx, String userId) {
		Thread oldThread = WizStatusCenter
				.getCurrentDownloadDocumentsThread(userId);
		if (oldThread != null) {
			if (oldThread.isAlive())
				return;
		}

		WizDownloadDocuments newThread = new WizDownloadDocuments(ctx, userId);
		WizStatusCenter.setCurrentDownloadDocumentsThread(userId, newThread);
		newThread.start();
	}
}
