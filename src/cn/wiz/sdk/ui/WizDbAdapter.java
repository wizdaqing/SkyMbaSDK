package cn.wiz.sdk.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import cn.wiz.sdk.api.WizAsyncAction;
import cn.wiz.sdk.api.WizAsyncAction.WizAsyncActionThread;
import cn.wiz.sdk.api.WizObject.WizDocument;
import cn.wiz.sdk.api.WizObject.WizLocation;
import cn.wiz.sdk.api.WizObject.WizTag;
import cn.wiz.sdk.db.WizDatabase;
import cn.wiz.sdk.db.WizDatabase.DOCUMENT_COUNT_DATA;
import cn.wiz.sdk.db.WizDatabase.WizRange;
import cn.wiz.sdk.util.TimeUtil;

public class WizDbAdapter {
	
	static public interface WizAdapterHelperBase {
		abstract WizDatabase getDb();
		abstract void onDataLoaded(Adapter adapter, Object elements, boolean done);
	}
	static public interface WizTreeAdapterHelper extends WizAdapterHelperBase {
		abstract ImageView getButtonIcon(View v);
		abstract CheckBox getOptCheckBox(View v);
		abstract ArrayList<WizTreeElement> getRootElements();
		abstract View getView(HashSet<String> selectedItemsId, WizTreeElement element, View view, ViewGroup parent);
	}

	static public interface WizTreeElement {
		abstract String getId();
		abstract int getLevel(WizTreeAdapterHelper helper);
		abstract String getTitle();
		abstract boolean isExpanded();
		abstract void setExpanded(boolean b);
		abstract boolean hasChildren(WizTreeAdapterHelper helper);
		abstract WizTreeElement getParent(WizTreeAdapterHelper helper);
		abstract ArrayList<WizTreeElement> getChildren(WizTreeAdapterHelper helper);
		abstract boolean isRootItem();
		abstract String getChildCount(WizTreeAdapterHelper helper);
		abstract int getChildCountLevel(WizTreeAdapterHelper helper);
	}

	public static class WizTreeAdapter extends ArrayAdapter<WizTreeElement> {

		protected WizTreeAdapterHelper mAdapterHelper;
		protected ArrayList<WizTreeElement> mElements;
		protected HashSet<String> mSelectedItemsId;

		public WizTreeAdapter(Context ctx,  WizTreeAdapterHelper baseAdapter, HashSet<String> selectedItemsId) {
			super(ctx, 0);
			mAdapterHelper = baseAdapter;
			mElements = new ArrayList<WizTreeElement>();
			mSelectedItemsId = selectedItemsId;
			//
			refreshData();
		}

		@Override
		public int getCount() {
			try {
				return mElements.size();
			} catch (Exception e) {
				return 0;
			}
		}

		@Override
		public WizTreeElement getItem(int position) {
			try {
				return mElements.get(position);
			} catch (Exception e) {
				return null;
			}
		}
		
		

		public View getView(final int position, View view, ViewGroup parent) {
			View v = mAdapterHelper.getView(mSelectedItemsId, getItem(position), view,  parent);
			ImageView button = mAdapterHelper.getButtonIcon(v);
			if (button!=null){
				button.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						expandOrCollapseItem(position);
					}
				});
			}
//			CheckBox box = mAdapterHelper.getOptCheckBox(v);
//			if (box!=null) {
//				box.setOnClickListener(new View.OnClickListener() {
//
//					@Override
//					public void onClick(View v) {
//						Log.i("View.Id:time", v.getId()+":"+System.currentTimeMillis());
//					}
//				});
//
//				box.setOnCheckedChangeListener(new OnCheckedChangeListener() {
//
//					@Override
//					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//						Log.i("View.Id:time", buttonView.getId()+":"+System.currentTimeMillis());
//						String itemId = getItem(position).getId();
//						String tag = (String) buttonView.getTag();
//						if (TextUtils.equals(tag, itemId)) {
//							if (mSelectedItemsId==null)
//								mSelectedItemsId = new HashSet<String>();
//							if (isChecked) {
//								mSelectedItemsId.add(itemId);
//							}else {
//								mSelectedItemsId.remove(itemId);
//							}
//						}
//					}
//				});
//			}
			return v;
			
		}
		
		private void expandItem(int position) {
			WizTreeElement elem = getItem(position);
			//
			ArrayList<WizTreeElement> children = elem.getChildren(mAdapterHelper);
			//
			elem.setExpanded(true);

			mElements.addAll(position + 1, children);
		}
		
		private void collapsedItem(int position) {
			WizTreeElement elem = getItem(position);
			elem.setExpanded(false);
			int level = elem.getLevel(mAdapterHelper);

			position += 1;
			//
			while (true) {
				if (position >= mElements.size())
					break;
				WizTreeElement childElem = getItem(position);
				if (childElem == null)
					break;
				//
				if (childElem.getLevel(mAdapterHelper) <= level)
					break;
				//
				mElements.remove(position);
			}
		}
		//
		public void expandOrCollapseItem(int position) {
			WizTreeElement elem = getItem(position);
			if (elem.isExpanded()) {
				collapsedItem(position);
			} else {
				expandItem(position);
			}
			this.notifyDataSetChanged();
		}
		//
		public void refreshData() {
			
			WizAsyncAction.startAsyncAction(null, new WizAsyncAction.WizAction() {

				@Override
				public Object work(WizAsyncActionThread thread, Object actionData)
						throws Exception {
					//
					ArrayList<WizTreeElement> elements = mAdapterHelper.getRootElements();
					//
					//init status
					for (WizTreeElement elem : elements) {
						elem.hasChildren(mAdapterHelper);
						elem.isRootItem();
						elem.getLevel(mAdapterHelper);
					}
					
					//
					return elements;
				}

				@Override
				public void onBegin(Object actionData) {
				}

				@SuppressWarnings("unchecked")
				@Override
				public void onEnd(Object actionData, Object ret) {
					//
					mElements = (ArrayList<WizTreeElement>)(ret);
					//
					
					WizTreeAdapter.this.notifyDataSetChanged();
				}

				@Override
				public void onException(Object actionData, Exception e) {
				}

				@Override
				public void onStatus(Object actionData, String status,
						int arg1, int arg2, Object obj) {
				}
				
			});
		}
	}

	public static class WizTreeAdapterWithExtraItem extends WizTreeAdapter {

		protected int mExtraItemLayoutId = 0;
		protected LayoutInflater mInflater;

		public WizTreeAdapterWithExtraItem(Context ctx, WizTreeAdapterHelper baseAdapter, 
				HashSet<String> selectedItemsId, int extraItemLayoutId) {
			super(ctx, baseAdapter, selectedItemsId);
			//
			mExtraItemLayoutId = extraItemLayoutId;
			mInflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public int getCount() {
			try {
				int count = mElements.size();
				if (0 != mExtraItemLayoutId) {
					count++;
				}
				return count;
			} catch (Exception e) {
				return 0;
			}
		}

		@Override
		public WizTreeElement getItem(int position) {
			try {
				if (position < mElements.size()) {
					return mElements.get(position);
				}
				return null;
			} catch (Exception e) {
				return null;
			}
		}
		
		@Override
		public int getViewTypeCount() {
			if (0 == mExtraItemLayoutId)
				return 1;
			//
			return 2;
		}
		
		@Override
		public int getItemViewType(int position) {
			if (position < mElements.size())
				return 0;
			return 1;
		}
		
		public boolean isEnabled (int position) {
			if (position < mElements.size())
				return true;
			return false;
		}
		//
		public View getView(final int position, View view, ViewGroup parent) {
			int type = getItemViewType(position);
			//
			if (type == 0) {
				return super.getView(position, view, parent);
			}
			//
			View ret = view;
			if (ret == null) {
				ret = mInflater.inflate(mExtraItemLayoutId, null);  
			}
			//
			return ret;
		}
	}
	
	static public class WizTreeTag extends WizTag implements WizTreeElement {
		
		boolean expanded = false;
		int level = -1;
		int hasChildrenFlag = -1;
		
		public WizTreeTag(WizTag tag) {
			super(tag);
		}

		@Override
		public String getId() {
			return guid;
		}

		@Override
		public int getLevel(WizTreeAdapterHelper helper) {
			if (-1 == level) {
				level = helper.getDb().getTagLevel(this);
			}
			return level;
		}

		@Override
		public String getTitle() {
			return name;
		}

		@Override
		public boolean isExpanded() {
			return expanded;
		}

		@Override
		public void setExpanded(boolean b) {
			expanded = b;
		}

		@Override
		public boolean hasChildren(WizTreeAdapterHelper helper) {
			if (-1 == hasChildrenFlag) {
				boolean ret = helper.getDb().tagHasChildren(guid);
				hasChildrenFlag = ret ? 1 : 0;
			}
			//
			return hasChildrenFlag == 1;
		}

		@Override
		public WizTreeElement getParent(WizTreeAdapterHelper helper) {
			return new WizTreeTag(helper.getDb().getParentTag(this));
		}
		
		@Override
		public ArrayList<WizTreeElement> getChildren(WizTreeAdapterHelper helper) {
			WizDatabase db = helper.getDb();
			ArrayList<WizTag> tags = db.getChildTags(guid);
			//
			ArrayList<WizTreeElement> ret = new ArrayList<WizTreeElement>();
			//
			for (WizTag tag: tags) {
				ret.add(new WizTreeTag(tag));
			}
			//
			return ret;
		}
		
		@Override
		public boolean isRootItem() {
			return parentGuid == null || parentGuid.equals("");
		}

		@Override
		public String getChildCount(WizTreeAdapterHelper helper) {
			WizDatabase db = helper.getDb();
			//
			int ret = db.getDocumentsCountFromCache(this);
			if (0 == ret)
				return "";
			//
			return Integer.toString(ret);
		}

		@Override
		public int getChildCountLevel(WizTreeAdapterHelper helper) {
			WizDatabase db = helper.getDb();
			//
			int count = db.getDocumentsCountFromCache(this);
			WizRange range = db.getTagsDocumentsCountRange();
			//
			int start = range.start;
			int end = range.end;
			//
			if (count <= start) {
				if (count == end)	//count == start == end
					return 8;
				else
					return 0;
			}
			else if (count >= end)
				return 8;
			//
			count -= start;
			end -= start;
			//
			if (0 == end)
				return 0;
			//
			int level = (int)((count * 9.0) / end);
			//
			return level;
		}
	}
	

	static public class WizTreeLocation extends WizLocation implements WizTreeElement {
		//
		public WizTreeLocation(WizLocation loc) {
			super(loc);
			this.hasChildrenFlag = -1;
		}
		
		boolean expanded = false;
		private int hasChildrenFlag = -1;

		@Override
		public String getId() {
			return getLocation();
		}

		@Override
		public int getLevel(WizTreeAdapterHelper helper) {
			return super.getLevel();
		}

		@Override
		public String getTitle() {
			return getDisplayName();
		}

		@Override
		public boolean isExpanded() {
			return expanded;
		}

		@Override
		public void setExpanded(boolean b) {
			expanded = b;
		}

		@Override
		public boolean hasChildren(WizTreeAdapterHelper helper) {
			if (-1 == hasChildrenFlag) {
				boolean ret = helper.getDb().locationHasChildren(getLocation());
				hasChildrenFlag = ret ? 1 : 0;
			}
			return hasChildrenFlag == 1 ? true : false;
		}

		@Override
		public WizTreeElement getParent(WizTreeAdapterHelper helper) {
			WizDatabase db = helper.getDb();
			return new WizTreeLocation(db.getParentLocation(getLocation()));
		}
		
		@Override
		public ArrayList<WizTreeElement> getChildren(WizTreeAdapterHelper helper) {
			WizDatabase db = helper.getDb();
			ArrayList<WizLocation> locatons = db.getChildLocations(getLocation());
			//
			ArrayList<WizTreeElement> ret = new ArrayList<WizTreeElement>();
			//
			for (WizLocation loc : locatons) {
				ret.add(new WizTreeLocation(loc));
			}
			//
			return ret;
		}
		
		@Override
		public boolean isRootItem() {
			return isRoot();
		}

		@Override
		public String getChildCount(WizTreeAdapterHelper helper) {
			WizDatabase db = helper.getDb();
			DOCUMENT_COUNT_DATA data = db.getDocumentsCount2(this);
			//
			if (data.nSelf == 0 && data.nIncludeChildren == 0)
				return "0";
			//
			if (data.nSelf == data.nIncludeChildren)
				return Integer.toString(data.nSelf);
			
			return Integer.toString(data.nSelf) + "/" + Integer.toString(data.nIncludeChildren); 
		}

		@Override
		public int getChildCountLevel(WizTreeAdapterHelper helper) {
			return -1;
		}
	}


	static public interface WizListAdapterHelper extends WizAdapterHelperBase {
		abstract ArrayList<WizDocument> getDocuments();
		abstract ArrayList<WizListElement> getElements(WizAsyncActionThread thread, int count);
		abstract View getView(WizListElement element, View view, ViewGroup parent);

	}
	

	static public interface WizListElement {
		abstract String getId();
		abstract String getTitle();
		abstract String getSummary();
		abstract String getTime();
		abstract String getOwnerId();
		abstract String getOwnerName();
		abstract int getReadCount();
		abstract boolean isServerChanged();
		abstract boolean isLocalChanged();
		abstract int getAttachmentsCount(WizListAdapterHelper helper);
	}
	
	
	public static class WizListAdapter extends ArrayAdapter<WizListElement> {

		private WizListAdapterHelper mAdapterHelper;
		private ArrayList<WizListElement> mElements = new ArrayList<WizListElement>();
		private HashMap<String, WizListElement> mElementsMap = new HashMap<String, WizListElement>();

		public WizListAdapter(Context ctx,  WizListAdapterHelper adapterHelper) {
			super(ctx, 0);
			mAdapterHelper = adapterHelper;
			//
			refreshData();
		}
		@Override
		public int getCount() {
			try {
				return mElements.size();
			} catch (Exception e) {
				return 0;
			}
		}
		@Override
		public int getViewTypeCount() {
			return 2;
		}
		/**
		 * 0 : 个人笔记<br>
		 * 1    ： 群组笔记
		 */
		@Override
		public int getItemViewType(int position) {
			if(mAdapterHelper.getDb().isPersonalKb()){
				return 0;
			}
			return 1;
		}
		@Override
		public WizListElement getItem(int position) {
			try {
				return mElements.get(position);
			} catch (Exception e) {
				return null;
			}
		}
		
		/**
		 * 获取笔记列表
		 * @return document list
		 */
		public ArrayList<WizDocument> getDocuments(){
			return mAdapterHelper.getDocuments();
		}
		
		public View getView(final int position, View view, ViewGroup parent) {
			View v = mAdapterHelper.getView(getItem(position), view,  parent);
			return v;	
		}
		//
		public WizListElement getItemById(String guid) {
			return mElementsMap.get(guid);
		}
		//
		public void refreshData() {
			
			WizAsyncAction.startAsyncAction(null, new WizAsyncAction.WizAction() {
				
				@Override
				public Object work(WizAsyncActionThread thread, Object actionData)
						throws Exception {
					//
					/*	
					 * 先加载少量数据，然后再加载大量数据，测试发现没对速度没有太大的影响，无法提升效率
					 * 暂时先禁止
					*/
					//
					return mAdapterHelper.getElements(thread, 300);
				}

				@Override
				public void onBegin(Object actionData) {
				}

				@SuppressWarnings("unchecked")
				@Override
				public void onEnd(Object actionData, Object ret) {
					//
					onDataLoaded((ArrayList<WizListElement>)(ret), true);
				}

				@Override
				public void onException(Object actionData, Exception e) {
				}

				@SuppressWarnings("unchecked")
				@Override
				public void onStatus(Object actionData, String status,
						int arg1, int arg2, Object obj) {
					//
					onDataLoaded((ArrayList<WizListElement>)(obj), false);
				}
				//
				private void onDataLoaded(ArrayList<WizListElement> data, boolean done) {
					if (mElements != data){
						//
						mElements = data;
						//
						mElementsMap.clear();
						for (WizListElement elem : mElements) {
							mElementsMap.put(elem.getId(), elem);
						}
						//
						WizListAdapter.this.notifyDataSetChanged();
					}
					//
					mAdapterHelper.onDataLoaded(WizListAdapter.this, mElements, done);
				}
				
			});
		}
	}
	//
	
	static public class WizListDocument extends WizDocument implements WizListElement {
		
		public WizListDocument(WizDocument doc) {
			super(doc);
			ownerName = doc.owner;
		}

		@Override
		public String getId() {
			return guid;
		}

		@Override
		public String getTitle() {
			return title;
		}

		@Override
		public String getTime() {
			//
			String time = dateModified;
			//
			if (TextUtils.isEmpty(time))
				return "";

			String tody = TimeUtil.getCurrentDayTimeString();
//			String yesterday = TimeUtil.getCurrentDayTimeString(1);
			if (time.indexOf(tody) >= 0) {
				return TimeUtil.getCurrentTimeString(time);
			} else {
				return TimeUtil.getCurrentMdHmsTimeString(time);
			}
		}
		//
		public String getSummary() {
			return new WizLocation(location).getFullDisplayName();
		}
		public boolean isServerChanged() {
			return serverChanged != 0;
		}
		public boolean isLocalChanged() {
			return localChanged != 0;
		}
		public int getAttachmentsCount(WizListAdapterHelper helper) {
			return helper.getDb().getAttachmentsCountFromCache(guid);
		}
		@Override
		public String getOwnerId() {
			return owner;
		}
		
		private String ownerName;
		public void setOwnerName(String ownerName){
			this.ownerName = ownerName;
		}
		@Override
		public String getOwnerName() {
			return ownerName;
		}

		@Override
		public int getReadCount() {
			return readCount;
		}
	}
}
