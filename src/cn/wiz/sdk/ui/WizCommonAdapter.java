package cn.wiz.sdk.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class WizCommonAdapter {
	
	static public class WizKeyValue {
		public int id;
		public String key;
		public String value;
		public boolean enableMoreButton;
		//
		public WizKeyValue(Context ctx, int resId, String value, boolean enableMoreButton) {
			this.id = resId;
			this.key = ctx.getString(resId);
			this.value = value;
			this.enableMoreButton = enableMoreButton;
		}
		public WizKeyValue(int id, String key, String value, boolean enableMoreButton) {
			this.id = id;
			this.key = key;
			this.value = value;
			this.enableMoreButton = enableMoreButton;
		}

		public WizKeyValue(Context ctx, int resId, String value) {
			this.id = resId;
			this.key = ctx.getString(resId);
			this.value = value;
			this.enableMoreButton = false;
		}
		public WizKeyValue(int id, String key, String value) {
			this.id = id;
			this.key = key;
			this.value = value;
			this.enableMoreButton = false;
		}		
	}
	//
	static public class WizKeyValueArrayAdapter extends BaseAdapter {
		//
		protected Context mContext;
		protected WizKeyValue [] mElements = new WizKeyValue [] {};
		protected int mItemLayoutId; 
		protected int mKeyTextViewResId;
		protected int mValueTextViewResId;
		protected int mMoreButtonViewResId;
		protected LayoutInflater mInflater;
		protected Drawable mMoreButtonBackgroundDrawable;

		//
		public WizKeyValueArrayAdapter(Context ctx, WizKeyValue [] elements, int itemLayoutId, 
				int keyTextViewResId, int valueTextViewResId, 
				int moreButtonViewResId, int moreButtonBackgroundDrawableId) {
			//
			mContext = ctx;
			mElements = elements;
			mItemLayoutId = itemLayoutId; 
			mKeyTextViewResId = keyTextViewResId;
			mValueTextViewResId = valueTextViewResId;
			mMoreButtonViewResId = moreButtonViewResId;
			//
			mMoreButtonBackgroundDrawable = ctx.getResources().getDrawable(moreButtonBackgroundDrawableId);
			//
			mInflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		//
		@Override
		public int getCount() {
			return mElements.length;
		}

		@Override
		public Object getItem(int position) {
			return mElements[position];
		}

		@Override
		public long getItemId(int position) {
			return mElements[position].id;
		}
		//
		private static class ViewHolder {
			TextView keyView;
			TextView valueView;
			View moreButtonView;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			ViewHolder vh = null;
			if (view == null) {
				view = mInflater.inflate(mItemLayoutId, null);
				vh = new ViewHolder();
				view.setTag(vh);
				//
				vh.keyView = (TextView)view.findViewById(mKeyTextViewResId);
				vh.valueView = (TextView)view.findViewById(mValueTextViewResId);
				vh.moreButtonView = (TextView)view.findViewById(mMoreButtonViewResId);
			}
			else {
				vh = (ViewHolder)view.getTag();
			}
			//
			WizKeyValue elem = mElements[position];
			//
			vh.keyView.setText(elem.key);
			vh.valueView.setText(elem.value);
			if (elem.enableMoreButton) {
				vh.moreButtonView.setBackgroundDrawable(mMoreButtonBackgroundDrawable);
				vh.moreButtonView.setVisibility(View.VISIBLE);
			}
			else {
				if (vh.valueView != vh.moreButtonView) {
					vh.moreButtonView.setVisibility(View.INVISIBLE);
				}
			}
			//
			return view;
		}
	}
	

	static public class WizKeyValueArrayAdapterWithExtraItem extends WizKeyValueArrayAdapter {
		//
		protected int mExtraItemLayoutId = 0;
		//
		public WizKeyValueArrayAdapterWithExtraItem(Context ctx, WizKeyValue [] elements, int itemLayoutId, 
				int keyTextViewResId, int valueTextViewResId, 
				int moreButtonViewResId, int moreButtonBackgroundDrawableId, int extraItemLayoutId) {
			//
			super(ctx, elements, itemLayoutId, keyTextViewResId, valueTextViewResId, moreButtonViewResId, moreButtonBackgroundDrawableId);
			//
			mExtraItemLayoutId = extraItemLayoutId;
		}

		@Override
		public int getCount() {
			try {
				int count = mElements.length;
				if (0 != mExtraItemLayoutId) {
					count++;
				}
				return count;
			} catch (Exception e) {
				return 0;
			}
		}

		@Override
		public Object getItem(int position) {
			try {
				if (position < mElements.length) {
					return mElements[position];
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
			if (position < mElements.length)
				return 0;
			return 1;
		}
		
		public boolean isEnabled (int position) {
			if (position < mElements.length)
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
	//

	static public class WizKeyValueArrayAdapterWithExtraItem2 extends WizKeyValueArrayAdapterWithExtraItem {

		protected View mExtraItemView;
		//
		public WizKeyValueArrayAdapterWithExtraItem2(Context ctx,
				WizKeyValue[] elements, int itemLayoutId, int keyTextViewResId,
				int valueTextViewResId, int moreButtonViewResId,
				int moreButtonBackgroundDrawableId, View extraItemView) {
			//
			super(ctx, elements, itemLayoutId, keyTextViewResId, valueTextViewResId,
					moreButtonViewResId, moreButtonBackgroundDrawableId, extraItemView == null ? 0 : 1);
			//
			mExtraItemView = extraItemView;
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
				ret = mExtraItemView;  
			}
			//
			return ret;
		}
	}

}
