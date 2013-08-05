package cn.wiz.sdk.api;

import cn.wiz.sdk.api.WizAsyncAction.WizAsyncActionThread.WizAsyncActionThreadMessageData;
import android.os.Handler;
import android.os.Message;

//异步封装
public class WizAsyncAction implements Handler.Callback{
	
	static public interface WizAsyncThread {
		abstract void sendStatusMessage(String statusText, int arg1, int arg2, Object obj);
	}
	//异步简单封装
	static public interface WizAction {
		//线程里面执行
		abstract Object work (WizAsyncActionThread thread, Object actionData) throws Exception;
		//在主线程里面执行，用于显示一些消息
		abstract void onBegin(Object actionData);
		abstract void onEnd(Object actionData, Object ret);
		abstract void onException(Object actionData, Exception e);
		abstract void onStatus(Object actionData, String status, int arg1, int arg2, Object obj);
	}
	
	private Handler mHandler = new Handler(this);
	static WizAsyncAction mAsyncAction = new WizAsyncAction();
	
	static public class WizAsyncActionThread extends Thread implements WizAsyncThread {
		Object mActionData;
		WizAction mAction;
		Handler mHandler;
		//
		static class WizAsyncActionThreadMessageData {
			public WizAsyncActionThreadMessageData(WizAsyncActionThread thread, String statusText, Object obj) {
				mThread = thread;
				mStatusText = statusText;
				mStatusObj = obj;
			}
			public WizAsyncActionThreadMessageData(WizAsyncActionThread thread) {
				mThread = thread;
			}
			public WizAsyncActionThreadMessageData(WizAsyncActionThread thread, Object ret) {
				mThread = thread;
				mRet = ret;
			}
			public WizAsyncActionThreadMessageData(WizAsyncActionThread thread, Exception e) {
				mThread = thread;
				mException = e;
			}
			WizAsyncActionThread mThread;
			Object mRet;
			Exception mException;
			String mStatusText;
			Object mStatusObj;
		}
		//
		public WizAsyncActionThread(Handler handler, Object actionData, WizAction action) {
			mHandler = handler;
			mActionData = actionData;
			mAction = action;
		}
		//
		public void run() {
			try {
				sendBeginMessage();
				Object ret = mAction.work(this, mActionData);
				sendEndMessage(ret);
			}
			catch (Exception e) {
				sendExceptionMessage(e);
			}
		}
		//
		static final int MSG_ID_BEGIN = 0;
		static final int MSG_ID_END = 1;
		static final int MSG_ID_EXCEPTION = 2;
		static final int MSG_ID_STATUS = 3;
		//
		void sendBeginMessage() {
			Message msg = mHandler.obtainMessage();
			msg.what = MSG_ID_BEGIN;
			msg.obj = new WizAsyncActionThreadMessageData(this);
			mHandler.sendMessage(msg);
		}
		//
		void sendEndMessage(Object ret) {
			Message msg = mHandler.obtainMessage();
			msg.what = MSG_ID_END;
			msg.obj = new WizAsyncActionThreadMessageData(this, ret);
			mHandler.sendMessage(msg);
		}
		//
		void sendExceptionMessage(Exception e) {
			Message msg = mHandler.obtainMessage();
			msg.what = MSG_ID_EXCEPTION;
			msg.obj = new WizAsyncActionThreadMessageData(this, e);
			mHandler.sendMessage(msg);
		}
		public void sendStatusMessage(String statusText, int arg1, int arg2, Object obj) {
			Message msg = mHandler.obtainMessage();
			msg.what = MSG_ID_STATUS;
			msg.arg1 = arg1;
			msg.arg2 = arg2;
			msg.obj = new WizAsyncActionThreadMessageData(this, statusText, obj);
			mHandler.sendMessage(msg);
		}
		//
		void onBegin() {
			mAction.onBegin(mActionData);
		}
		//
		void onEnd(Object ret) {
			mAction.onEnd(mActionData, ret);
		}
		//
		void onException(Exception e) {
			mAction.onException(mActionData, e);
		}
		//
		void onStatus(String statusText, int arg1, int arg2, Object obj) {
			mAction.onStatus(mActionData, statusText, arg1, arg2, obj);
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		//
		WizAsyncActionThreadMessageData data = (WizAsyncActionThreadMessageData)msg.obj;
		//
		switch (msg.what) {
		case WizAsyncActionThread.MSG_ID_BEGIN:
			data.mThread.onBegin();
			break;
		case WizAsyncActionThread.MSG_ID_END:
			data.mThread.onEnd(data.mRet);
			break;
		case WizAsyncActionThread.MSG_ID_EXCEPTION:
			data.mThread.onException(data.mException);
			break;
		case WizAsyncActionThread.MSG_ID_STATUS:
			data.mThread.onStatus(data.mStatusText, msg.arg1, msg.arg2, data.mStatusObj);
			break;
			
		}
		//
		return false;
	}
	//
	public Thread startAsyncActionCore(Object actionData, WizAction action) {
		WizAsyncActionThread thread = new WizAsyncActionThread(mHandler, actionData, action);
		//
		thread.start();
		//
		return thread;
	}
	//
	public static Thread startAsyncAction(Object actionData, WizAction action) {
		//
		return mAsyncAction.startAsyncActionCore(actionData, action);
	}
	
	//
	/*
	 *使用方法：类似按钮回掉
	 *
	public static void test() {
		//
		WizAsyncAction.startAsyncAction(null, new WizAction() {

			@Override
			public Object work(Object actionData) {
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
			
		});
		
	}
	*/
}
