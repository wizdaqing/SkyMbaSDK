package cn.wiz.sdk.api;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.wiz.sdk.api.WizObject.WizAbstract;
import cn.wiz.sdk.api.WizObject.WizAvatar;
import cn.wiz.sdk.api.WizObject.WizKb;
import cn.wiz.sdk.api.WizObject.WizObjectBase;
import cn.wiz.sdk.api.WizStrings.WizStringId;
import cn.wiz.sdk.db.WizDatabase;

import android.os.Handler;
import android.os.Message;

public class WizEventsCenter implements Handler.Callback {
	
	private static WizEventsCenter mEventsCenter = new WizEventsCenter();
	//
	private Handler mHandler = new Handler(this);
	//
	private static WizEventsCenter getCenter() {
		return mEventsCenter;
	}
	
	private static Handler getHandler() {
		return getCenter().mHandler;
	}	
	
	@Override
	public boolean handleMessage(Message msg) {
		processMessage(msg);
		return false;
	}

	//
	//
	@SuppressWarnings("unchecked")
	private void processMessage(Message msg)
	{
		switch (msg.what)
		{
		case MESSAGE_ID_SYNC_BEGIN:
			dispatchOnSyncBegin();
			break;
		case MESSAGE_ID_SYNC_END:
			dispatchOnSyncEnd(msg.arg1 == 1);
			break;
		case MESSAGE_ID_SYNC_PROGRESS:
			dispatchOnSyncProgress(msg.arg1);
			break;
		case MESSAGE_ID_SYNC_STATUS:
			dispatchOnSyncStatus((String)msg.obj);
			break;
		case MESSAGE_ID_SYNC_EXCEPTION:
			dispatchOnSyncException((Exception)msg.obj);
			break;
		case MESSAGE_ID_SYNC_KB_BEGIN:
			dispatchOnSyncKbBegin((WizKb)msg.obj, msg.arg1 == 1);
			break;
		case MESSAGE_ID_SYNC_KB_END:
			dispatchOnSyncKbEnd((WizKb)msg.obj, msg.arg1 == 1, msg.arg2 == 1);
			break;
		case MESSAGE_ID_SYNC_KB_PROGRESS:
			dispatchOnSyncKbProgress((WizKb)msg.obj, msg.arg1);
			break;
		case MESSAGE_ID_SYNC_KB_STEP:
			dispatchOnSyncKbStep((WizKb)msg.obj, msg.arg1);
			break;
		case MESSAGE_ID_OBJECT_SYNC_STATUS_CHANGED:
			dispatchOnObjectSyncStatusChanged((WizObjectBase)msg.obj, msg.arg1);
			break;
		case MESSAGE_ID_DOCUMENT_ABSTRACT_CREATED:
			dispatchOnDocumentAbstractCreated((WizAbstract)msg.obj);
			break;
		case MESSAGE_ID_DATABASE_REFRESH_OBJECT:
			dispatchOnDatabaseRefreshObject((WizDatabase)msg.obj, msg.arg1);
			break;
		case MESSAGE_ID_AVATAR_DOWNLOADED:
			dispatchOnAvatarDownloaded((WizAvatar)msg.obj);
			break;
		case MESSAGE_ID_GROUPINFO_DOWNLOADED:
			dispatchOnGroupInfoDownloaded();
			break;
		case MESSAGE_ID_MESSAGE_DOWNLOADED:
			dispatchOnMessagesDownloaded();
			break;
		case MESSAGE_ID_READ_STAUS_CHANGED:
			dispatchOnReadStausChanged((List<WizObjectBase>)msg.obj);
			break;
		}
	}
	//
	///////////////////////////////////sync all/////////////////////////////////////////
	static public interface WizSyncEventsListener {
		abstract public void onSyncBegin();
		abstract public void onSyncEnd(boolean succeeded);
		abstract public void onSyncProgress(int progress);
		abstract public void onSyncStatus(String status);
		abstract public void onSyncException(Exception e);
	}
	public static void addSyncListener(WizSyncEventsListener callback) {
		getSyncListeners().add(callback);
	}
	//
	public static void removeSyncListener(WizSyncEventsListener callback) {
		getSyncListeners().remove(callback);
	}
	
	public static void sendSyncBeginMessage()
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_SYNC_BEGIN;
		//
		handler.sendMessage(msg);
	}
	//
	
	
	public static void sendSyncEndMessage(boolean succeeded)
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_SYNC_END;
		msg.arg1 = succeeded ? 1 : 0;
		//
		handler.sendMessage(msg);
	}

	public static void sendSyncrogressMessage(int progress)
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_SYNC_PROGRESS;
		msg.arg1 = progress;
		//
		handler.sendMessage(msg);
	}

	
	public static void sendSyncStatusMessage(String status)
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_SYNC_STATUS;
		msg.obj = status;
		//
		handler.sendMessage(msg);
	}
	
	public static void sendSyncExceptionMessage(Exception e)
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_SYNC_EXCEPTION;
		msg.obj = e;
		//
		handler.sendMessage(msg);
	}


	public static void sendSyncStatusMessage(WizStringId id) {
		sendSyncStatusMessage(WizStrings.getString(id));
	}
	public static void sendSyncStatusMessage(WizStringId id, String param1) {
		//
		String ret = WizStrings.getString(id);
		//
		ret = ret.replaceAll("%1", param1);
		//
		sendSyncStatusMessage(ret);
	}
	public static void sendSyncStatusMessage(WizStringId id, String param1, String param2) {
		//
		String ret = WizStrings.getString(id);
		//
		ret = ret.replaceAll("%1", param1);
		ret = ret.replaceAll("%2", param2);
		//
		sendSyncStatusMessage(ret);
	}


	private Set<WizSyncEventsListener> mSyncListeners = new HashSet<WizSyncEventsListener>();
	
	private static Set<WizSyncEventsListener> getSyncListeners()
	{
		return getCenter().mSyncListeners;
	}

	private void dispatchOnSyncBegin()
	{
		Set<WizSyncEventsListener> set = getSyncListeners();
		//
		for (WizSyncEventsListener obj : set)
		{
			obj.onSyncBegin();
		}
	}
	private void dispatchOnSyncEnd(boolean suucceeded)
	{
		Set<WizSyncEventsListener> set = getSyncListeners();
		//
		for (WizSyncEventsListener obj : set)
		{
			obj.onSyncEnd(suucceeded);
		}
	}

	private void dispatchOnSyncProgress(int progress)
	{
		Set<WizSyncEventsListener> set = getSyncListeners();
		//
		for (WizSyncEventsListener obj : set)
		{
			obj.onSyncProgress(progress);
		}
	}
	private void dispatchOnSyncStatus(String message)
	{
		Set<WizSyncEventsListener> set = getSyncListeners();
		//
		for (WizSyncEventsListener obj : set)
		{
			obj.onSyncStatus(message);
		}
	}
	
	private void dispatchOnSyncException(Exception e)
	{
		Set<WizSyncEventsListener> set = getSyncListeners();
		//
		for (WizSyncEventsListener obj : set)
		{
			obj.onSyncException(e);
		}
	}
	
	///////////////////////////////////sync kb/////////////////////////////////////////
	static public enum WizSyncKbStep {
		BeforeUploadDeletedGUIDs,
		AfterUploadDeletedGUIDs,
		BeforeUploadTags,
		AfterUploadTags,
		BeforeUploadDocuments,
		AfterUploadDocuments,
		BeforeUploadAttachments,
		AfterUploadAttachments,
		BeforeDownloadDeletedGUIDs,
		AfterDownloadDeletedGUIDs,
		BeforeDownloadKeyValues,
		AfterDownloadKeyValues,
		BeforeDownloadTags,
		AfterDownloadTags,
		BeforeDownloadDocuments,
		AfterDownloadDocuments,
		BeforeDownloadAttachments,
		AfterDownloadAttachments,
	}

	static public interface WizSyncKbEventsListener {
		abstract public void onSyncKbBegin(WizKb kb, boolean uploadOnly);
		abstract public void onSyncKbEnd(WizKb kb, boolean uploadOnly, boolean succeeded);
		abstract public void onSyncKbProgress(WizKb kb, int progress);
		abstract public void onSyncKbStep(WizKb kb, WizSyncKbStep step);
	}
	public static void addKbListener(WizSyncKbEventsListener callback) {
		getKbListeners().add(callback);
	}
	public static void removeKbListener(WizSyncKbEventsListener callback) {
		getKbListeners().remove(callback);
	}
	
	public static void sendSyncKbBeginMessage(WizKb kb, boolean uploadOnly)
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_SYNC_KB_BEGIN;
		msg.obj = kb;
		msg.arg1 = uploadOnly ? 1 : 0;
		//
		handler.sendMessage(msg);
	}

	public static void sendSyncKbEndMessage(WizKb kb, boolean uploadOnly, boolean succeeded)
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_SYNC_KB_END;
		msg.obj = kb;
		msg.arg1 = uploadOnly ? 1 : 0;
		msg.arg2 = succeeded ? 1 : 0;
		//
		handler.sendMessage(msg);
	}

	public static void sendSyncKbProgressMessage(WizKb kb, int progress)
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_SYNC_KB_PROGRESS;
		msg.obj = kb;
		msg.arg1 = progress;
		//
		handler.sendMessage(msg);
	}
	//
	public static void sendSyncKbStepMessage(WizKb kb, WizSyncKbStep step)
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_SYNC_KB_STEP;
		msg.obj = kb;
		msg.arg1 = step.ordinal();
		//
		handler.sendMessage(msg);
	}
		
	private Set<WizSyncKbEventsListener> mSyncKbListeners = new HashSet<WizSyncKbEventsListener>();
	private static Set<WizSyncKbEventsListener> getKbListeners() {
		return getCenter().mSyncKbListeners;
	}
	//
	private void dispatchOnSyncKbBegin(WizKb kb, boolean uploadOnly)
	{
		Set<WizSyncKbEventsListener> set = getKbListeners();
		//
		for (WizSyncKbEventsListener obj : set)
		{
			obj.onSyncKbBegin(kb, uploadOnly);
		}
	}
	private void dispatchOnSyncKbEnd(WizKb kb, boolean uploadOnly, boolean succeeded)
	{
		Set<WizSyncKbEventsListener> set = getKbListeners();
		//
		for (WizSyncKbEventsListener obj : set)
		{
			obj.onSyncKbEnd(kb, succeeded, uploadOnly);
		}
	}

	private void dispatchOnSyncKbProgress(WizKb kb, int progress)
	{
		Set<WizSyncKbEventsListener> set = getKbListeners();
		//
		for (WizSyncKbEventsListener obj : set)
		{
			obj.onSyncKbProgress(kb, progress);
		}
	}
	private void dispatchOnSyncKbStep(WizKb kb, int step)
	{
		Set<WizSyncKbEventsListener> set = getKbListeners();
		//
		WizSyncKbStep currStep = WizSyncKbStep.BeforeUploadDeletedGUIDs;
		for (WizSyncKbStep s : WizSyncKbStep.values()) {
			if (s.ordinal() == step) {
				currStep = s;
				break;
			}
		}
		//
		for (WizSyncKbEventsListener obj : set)
		{
			obj.onSyncKbStep(kb, currStep);
		}
	}
	//
	///////////////////////////////////DownloadObject/////////////////////////////////////////
	
	static public enum WizObjectSyncStatus {
		ObjectUploaded,
		ObjectDownloaded
	}

	static public interface WizObjectSyncStatusEventsListener {
		abstract public void onObjectSyncStatusChanged(WizObjectBase objDownload, WizObjectSyncStatus status);
	}
	

	public static void addObjectSyncStatusListener(WizObjectSyncStatusEventsListener callback) {
		getObjectSyncStatusListeners().add(callback);
	}
	public static void removeObjectSyncStatusListener(WizObjectSyncStatusEventsListener callback) {
		getObjectSyncStatusListeners().remove(callback);
	}
	
	public static void sendObjectSyncStatusChangedMessage(WizObjectBase obj, WizObjectSyncStatus status)
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_OBJECT_SYNC_STATUS_CHANGED;
		msg.obj = obj;
		msg.arg1 = status.ordinal();
		//
		handler.sendMessage(msg);
	}
	//
	
	private Set<WizObjectSyncStatusEventsListener> mObjectSyncStatusListeners = new HashSet<WizObjectSyncStatusEventsListener>();
	
	private static Set<WizObjectSyncStatusEventsListener> getObjectSyncStatusListeners() {
		return getCenter().mObjectSyncStatusListeners;
	}
	
	private void dispatchOnObjectSyncStatusChanged(WizObjectBase objDownload, int status)
	{
		Set<WizObjectSyncStatusEventsListener> set = getObjectSyncStatusListeners();
		//
		WizObjectSyncStatus currStatus = WizObjectSyncStatus.ObjectDownloaded;
		for (WizObjectSyncStatus s : WizObjectSyncStatus.values()) {
			if (s.ordinal() == status) {
				currStatus = s;
				break;
			}
		}
		//
		for (WizObjectSyncStatusEventsListener obj : set)
		{
			obj.onObjectSyncStatusChanged(objDownload, currStatus);
		}
	}
	//

	///////////////////////////////////Database/////////////////////////////////////////
	public static enum WizDatabaseObjectType {
		Folder,
		Tag,
		Document,
		Attachment,
		DocumentsCount,
		Message
	}

	static public interface WizDatabaseEventsListener {
		abstract public void onDatabaseRefreshObject(WizDatabase db, WizDatabaseObjectType type);
	}
	
	public static void addDatabaseListener(WizDatabaseEventsListener callback) {
		getDatabaseListeners().add(callback);
	}
	public static void removeDatabaseListener(WizDatabaseEventsListener callback) {
		getDatabaseListeners().remove(callback);
	}
	
	public static void sendDatabaseRefreshObject(WizDatabase db, WizDatabaseObjectType type)
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_DATABASE_REFRESH_OBJECT;
		msg.obj = db;
		msg.arg1 = type.ordinal();
		//
		handler.sendMessage(msg);
	}
	//
	private Set<WizDatabaseEventsListener> mDatabaseListeners = new HashSet<WizDatabaseEventsListener>();
	
	private static Set<WizDatabaseEventsListener> getDatabaseListeners() {
		return getCenter().mDatabaseListeners;
	}
	
	private void dispatchOnDatabaseRefreshObject(WizDatabase db, int arg1)
	{
		Set<WizDatabaseEventsListener> set = getDatabaseListeners();
		//
		WizDatabaseObjectType type = WizDatabaseObjectType.Document;
		for (WizDatabaseObjectType s : WizDatabaseObjectType.values()) {
			if (s.ordinal() == arg1) {
				type = s;
				break;
			}
		}
		for (WizDatabaseEventsListener obj : set)
		{
			obj.onDatabaseRefreshObject(db, type);
		}
	}
	//
	///////////////////////////////////DocumentAbstract/////////////////////////////////////////

	static public interface WizDocumentAbstractEventsListener {
		abstract public void onDocumentAbstractCreated(WizAbstract obj);
	}
	

	public static void addDocumentAbstractListener(WizDocumentAbstractEventsListener callback) {
		getDocumentAbstractListeners().add(callback);
	}
	public static void removeDocumentAbstractListener(WizDocumentAbstractEventsListener callback) {
		getDocumentAbstractListeners().remove(callback);
	}
	
	public static void sendDocumentAbstractCreatedMessage(WizAbstract obj)
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_DOCUMENT_ABSTRACT_CREATED;
		msg.obj = obj;
		//
		handler.sendMessage(msg);
	}
	//
	
	private Set<WizDocumentAbstractEventsListener> mDocumentAbstractListeners = new HashSet<WizDocumentAbstractEventsListener>();
	
	private static Set<WizDocumentAbstractEventsListener> getDocumentAbstractListeners() {
		return getCenter().mDocumentAbstractListeners;
	}
	
	private void dispatchOnDocumentAbstractCreated(WizAbstract objAbstract)
	{
		Set<WizDocumentAbstractEventsListener> set = getDocumentAbstractListeners();
		//
		for (WizDocumentAbstractEventsListener obj : set)
		{
			obj.onDocumentAbstractCreated(objAbstract);
		}
	}
	//
	///////////////////////////////////Avatar/////////////////////////////////////////
	
	static public interface WizAvatarListener {
		abstract public void onAvatarDownloaded(WizAvatar obj);
	}
	public static void addAvatarListener(WizAvatarListener callback) {
		getAvatarListeners().add(callback);
	}
	public static void removeAvatarListener(WizAvatarListener callback) {
		getAvatarListeners().remove(callback);
	}
	
	public static void sendAvatarDownloadedMessage(WizAvatar obj)
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_AVATAR_DOWNLOADED;
		msg.obj = obj;
		//
		handler.sendMessage(msg);
	}
	//
	
	private Set<WizAvatarListener> mAvatarListeners = new HashSet<WizAvatarListener>();
	
	private static Set<WizAvatarListener> getAvatarListeners() {
		return getCenter().mAvatarListeners;
	}
	
	private void dispatchOnAvatarDownloaded(WizAvatar objAvatar)
	{
		Set<WizAvatarListener> set = getAvatarListeners();
		//
		for (WizAvatarListener obj : set)
		{
			obj.onAvatarDownloaded(objAvatar);
		}
	}
	///////////////////////////////////ReadStausChanged/////////////////////////////////////////
	
	static public interface WizReadStausChangedListener {
		/**
		 * 参数objs：<br>
		 * 设置一个已读，返回消息为List大小为1的集合<br>
		 * 设置n个已读，返回消息为List大小为n的集合<br>
		 * 设置全部未读为已读，返回消息为null<br>
		 * @param objs 被修改的集合
		 */
		abstract public void onReadStausChanged(List<WizObjectBase> objs);
	}
	public static void addReadStausChangedListener(WizReadStausChangedListener callback) {
		getReadStausChangedListeners().add(callback);
	}
	public static void removeReadStausChangedListener(WizReadStausChangedListener callback) {
		getReadStausChangedListeners().remove(callback);
	}
	
	public static void sendReadStausChangedMessage(List<WizObjectBase> objs)
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_READ_STAUS_CHANGED;
		msg.obj = objs;
		//
		handler.sendMessage(msg);
	}
	//
	
	private Set<WizReadStausChangedListener> mReadStausChangedListeners = new HashSet<WizReadStausChangedListener>();
	
	private static Set<WizReadStausChangedListener> getReadStausChangedListeners() {
		return getCenter().mReadStausChangedListeners;
	}
	
	private void dispatchOnReadStausChanged(List<WizObjectBase> objs)
	{
		Set<WizReadStausChangedListener> set = getReadStausChangedListeners();
		//
		for (WizReadStausChangedListener obj : set)
		{
			obj.onReadStausChanged(objs);
		}
	}
	///////////////////////////////////Group Info /////////////////////////////////////////
	
	static public interface WizGroupInfoListener {
		abstract public void onGroupInfoDownloaded();
	}
	public static void addGroupInfoListener(WizGroupInfoListener callback) {
		getGroupInfoListeners().add(callback);
	}
	public static void removeGroupInfoListener(WizGroupInfoListener callback) {
		getGroupInfoListeners().remove(callback);
	}
	
	public static void sendGroupInfoDownloadedMessage()
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_GROUPINFO_DOWNLOADED;
		//
		handler.sendMessage(msg);
	}
	//
	
	private Set<WizGroupInfoListener> mGroupInfoListeners = new HashSet<WizGroupInfoListener>();
	
	private static Set<WizGroupInfoListener> getGroupInfoListeners() {
		return getCenter().mGroupInfoListeners;
	}
	
	private void dispatchOnGroupInfoDownloaded()
	{
		Set<WizGroupInfoListener> set = getGroupInfoListeners();
		//
		for (WizGroupInfoListener obj : set)
		{
			obj.onGroupInfoDownloaded();
		}
	}
	///////////////////////////////////Messages Info /////////////////////////////////////////
	
	static public interface WizMessageListener {
		abstract public void onMessageDownloaded();
	}
	public static void addMessageListener(WizMessageListener callback) {
		getMessageListeners().add(callback);
	}
	public static void removeMessageListener(WizMessageListener callback) {
		getMessageListeners().remove(callback);
	}
	
	public static void sendMessageDownloadedMessage()
	{
		Handler handler = getHandler();
		//
		Message msg = handler.obtainMessage();
		//
		msg.what = MESSAGE_ID_MESSAGE_DOWNLOADED;
		//
		handler.sendMessage(msg);
	}
	//
	
	private Set<WizMessageListener> mMessageListeners = new HashSet<WizMessageListener>();
	
	private static Set<WizMessageListener> getMessageListeners() {
		return getCenter().mMessageListeners;
	}
	
	private void dispatchOnMessagesDownloaded()
	{
		Set<WizMessageListener> set = getMessageListeners();
		//
		for (WizMessageListener obj : set)
		{
			obj.onMessageDownloaded();
		}
	}
	//
	///////////////////////////////////Message id/////////////////////////////////////////

	static private final int MESSAGE_ID_SYNC_BEGIN = 1;
	static private final int MESSAGE_ID_SYNC_END = 2;
	static private final int MESSAGE_ID_SYNC_PROGRESS = 3;
	static private final int MESSAGE_ID_SYNC_STATUS = 4;
	static private final int MESSAGE_ID_SYNC_EXCEPTION = 5;
	//
	static private final int MESSAGE_ID_SYNC_KB_BEGIN = 11;
	static private final int MESSAGE_ID_SYNC_KB_END = 12;
	static private final int MESSAGE_ID_SYNC_KB_PROGRESS = 13;
	static private final int MESSAGE_ID_SYNC_KB_STEP = 14;
	//
	static private final int MESSAGE_ID_OBJECT_SYNC_STATUS_CHANGED = 61;
	//
	static private final int MESSAGE_ID_DOCUMENT_ABSTRACT_CREATED = 71;
	
	static private final int MESSAGE_ID_DATABASE_REFRESH_OBJECT	= 81;
	
	static private final int MESSAGE_ID_AVATAR_DOWNLOADED	= 91;
	static private final int MESSAGE_ID_GROUPINFO_DOWNLOADED	= 51;
	static private final int MESSAGE_ID_MESSAGE_DOWNLOADED	= 21;
	static private final int MESSAGE_ID_READ_STAUS_CHANGED	= 22;
	
	//
	/*
	 * 要在主线程里面调用，进行一下初始化，否则会导致handler在工作线程初始化
	 */
	public static void init() {
		getHandler();
	}
}
