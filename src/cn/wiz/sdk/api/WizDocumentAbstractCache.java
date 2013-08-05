package cn.wiz.sdk.api;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import cn.wiz.sdk.api.WizObject.WizAbstract;
import cn.wiz.sdk.db.WizAbstractDatabase;

public class WizDocumentAbstractCache extends Thread {

	private Context mContext;
	private String mUserId;
	private HashSet<String> mCurrentDocuments = new HashSet<String>();
	private Stack<String> mDocuments = new Stack<String>();
	private ConcurrentHashMap<String, WizAbstract> mAbstracts = new ConcurrentHashMap<String, WizAbstract>();

	private WizDocumentAbstractCache(Context ctx, String userId) {
		mContext = ctx;
		mUserId = userId;
	}
	
	/*
	 * 清除最旧的20条数据，或者全部清除
	 */
	synchronized void clearCache(boolean clearAll) {
		if (clearAll) {
			mAbstracts.clear();
		}
		else {
			TreeMap<Long, String> old = new TreeMap<Long, String>();
			//
			for (WizAbstract elem : mAbstracts.values()) {
				old.put(elem.lastAccessed, elem.documentGuid);
			}
			//
			int i = 0;
			//
			Iterator<String> it = old.values().iterator();
		    while(it.hasNext()) {
				if (i >= 20)
					break;
				//
		    	String guid = it.next();
				if (guid != null) {
					mAbstracts.remove(guid);
				}
				//
				i++;
			}
		}
	}
	synchronized void addToCache(WizAbstract abs) {
		if (mAbstracts.size() >= 200) {
			clearCache(false);
		}
		mAbstracts.put(abs.documentGuid, abs);
		//
		if (abs.abstractImage != null) {
			//通知界面更新，显示图片摘要
			WizEventsCenter.sendDocumentAbstractCreatedMessage(abs);
		}
	}

	synchronized public WizAbstract getAbstract(String guid) {

		if (mAbstracts.containsKey(guid)) {
			WizAbstract abs = mAbstracts.get(guid);
			abs.lastAccessed = System.currentTimeMillis();

			return abs;
		}
		//
		if (mCurrentDocuments.contains(guid))
			return null;
		//
		mCurrentDocuments.add(guid);
		mDocuments.add(guid);
		//
		return null;
	}
	
	synchronized public void forceUpdateAbstract(String guid) {
		if (mCurrentDocuments.contains(guid))
			return;
		//
		mCurrentDocuments.add(guid);
		mDocuments.add(guid);
	}

	synchronized String getNextDocument() {
		//
		if (mDocuments.empty())
			return null;
		//
		String guid = mDocuments.pop();
		mCurrentDocuments.remove(guid);
		return guid;
	}

	private boolean isStop() {
		return WizStatusCenter.isStoppingDocumentAbstractThread(mUserId);
	}

	@Override
	public void run() {
		//
		this.setPriority(MIN_PRIORITY);
		//
		while (!isStop()) {
			String guid = getNextDocument();
			//
			if (guid == null) {
				try {
					sleep(100);
				} catch (InterruptedException e) {
				}
				//
				continue;
			}
			//
			WizAbstract abs = getAbstractDirect(mContext, mUserId, guid);
			//
			if (abs == null) {
				//如果没有生成，仍然要加入，在下载笔记后强制更新
				addToCache(new WizAbstract(guid, WIZ_ABSTRACT_TYPE, null, null));
			}
			else {
				if (abs.abstractImage == null)  {
					abs.abstractText = null;	//目前不显示文字摘要，因此暂时不需要存储文字
					addToCache(abs);
				}
				else {
					if (!abs.abstractImage.isRecycled()) {	//如果图片被回收了，那就忽略，等待下次加入
						addToCache(abs);
					}
				}
			}
			
			//
			try {
				sleep(10);
			} catch (InterruptedException e) {
			}
		}
	}
	//

	/*
	 * 开始生成摘要线程。如果不开启，则永远无法获得摘要
	 */
	static public void startDocumentAbstractThread(Context ctx, String userId) {
		//
		Thread oldThread = WizStatusCenter.getCurrentDocumentAbstractThread(userId);
		if (oldThread != null) {
			if (oldThread.isAlive())
				return;
		}
		//
		WizDocumentAbstractCache newThread = new WizDocumentAbstractCache(ctx, userId);
		//
		WizStatusCenter.setCurrentDocumentAbstractThread(userId, newThread);
		//
		newThread.start();
	}
	//
	/*
	 * 从cache获取摘要，没有的时候会在线程内声称摘要
	 */
	static public WizAbstract getAbstractFromCache(String userId, String documentGuid) {
		Thread thread = WizStatusCenter.getCurrentDocumentAbstractThread(userId);
		if (thread == null)
			return null;
		//
		if (thread instanceof WizDocumentAbstractCache) {
			WizDocumentAbstractCache cacheThread = (WizDocumentAbstractCache)thread;
			//
			return cacheThread.getAbstract(documentGuid);
		}
		//
		return null;
	}

	/*
	 * 强制更新笔记的摘要
	 */
	static public void forceUpdateAbstract(String userId, String documentGuid) {
		Thread thread = WizStatusCenter.getCurrentDocumentAbstractThread(userId);
		if (thread == null)
			return;
		//
		if (thread instanceof WizDocumentAbstractCache) {
			WizDocumentAbstractCache cacheThread = (WizDocumentAbstractCache)thread;
			//
			cacheThread.forceUpdateAbstract(documentGuid);
		}
	}
	
	/*
	 * 清理cache，可以在内存不足的时候执行
	 * 
	 */
	
	static public void clearCache(String userId, boolean clearAll) {
		Thread thread = WizStatusCenter.getCurrentDocumentAbstractThread(userId);
		if (thread == null)
			return;
		//
		if (thread instanceof WizDocumentAbstractCache) {
			WizDocumentAbstractCache cacheThread = (WizDocumentAbstractCache)thread;
			//
			cacheThread.clearCache(clearAll);
		}
	}
	
	
	private static final String WIZ_ABSTRACT_TYPE = "AndroidPhone&&Pad";

	/*
	 * 获取摘要，直接获取。不采用异步方式
	 */
	public static WizAbstract getAbstractDirect(Context ctx, String userId, String documentGUID) {
		//
		WizAbstractDatabase db = WizAbstractDatabase.getDb(ctx, userId);
		//
		WizAbstract currentAbstract = db.getAbstractByGuidAndType(documentGUID, WIZ_ABSTRACT_TYPE);
		//
//		System.out.print("get abstract: " + documentGUID);
		//
		if (currentAbstract != null)
			return currentAbstract;
		//
		currentAbstract = db.createAbstract(ctx, userId, documentGUID, WIZ_ABSTRACT_TYPE);
		return currentAbstract;
	}

	public static boolean isAbstract(WizAbstract currentAbstract) {
		if (currentAbstract == null)
			return false;

		if (currentAbstract.abstractImage != null)
			return true;
		if (currentAbstract.abstractText != null
				&& currentAbstract.abstractText.length() > 0)
			return true;

		return false;
	}

}
