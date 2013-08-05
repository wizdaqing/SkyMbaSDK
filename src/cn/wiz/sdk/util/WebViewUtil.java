package cn.wiz.sdk.util;

import java.lang.reflect.Field;
import java.util.ArrayList;

import cn.wiz.sdk.settings.WizSystemSettings;
import cn.wiz.sdk.ui.WizColorPickerDialog;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.Html.ImageGetter;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.BulletSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ZoomButtonsController;

public class WebViewUtil {

	private static int start = 0;
	private static int end = 0;

	public static SpannableStringBuilder getSpan(EditText edit) {
		return new SpannableStringBuilder(edit.getText());
	}

	public static SpannableStringBuilder getRSpannable(EditText edit, Object obj) {
		SpannableStringBuilder span = getSpan(edit);
		span.setSpan(obj, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		return span;
	}

	public static SpannableStringBuilder getRSpanned(EditText edit, Object obj) {
		SpannableStringBuilder span = getSpan(edit);
		String sham = "S";
		end = start + sham.length();
		span.insert(start, sham);
		span.setSpan(obj, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
		return span;
	}

	public static int getSelectionStart(EditText edit) {
		return edit.getSelectionStart();
	}

	public static int getSelectionEnd(EditText edit) {
		return edit.getSelectionEnd();
	}

	public static boolean isStartEndEques(EditText edit) {
		start = getSelectionStart(edit);
		end = getSelectionEnd(edit);

		if (start > end) {
			int k = 0;
			k = start;
			start = end;
			end = k;
			return true;
		}
		return false;

	}

	public static int getSelection(EditText edit) {
		return end;
	}

	public static void setSelection(EditText edit, int index) {
		edit.setSelection(index);
	}

	public static void setEditTextView(EditText edit,
			SpannableStringBuilder strBuilder) {
		int index = getSelection(edit);
		edit.setText(strBuilder);
		setSelection(edit, index);
	}

	public static final String WIZ_NOTE_COLOR_TYPE_FONT = "fontColor";
	public static final String WIZ_NOTE_COLOR_TYPE_BG = "bgColor";

	// 颜色
	public static void setColor(Context ctx, final EditText edit,
			final String type) {
		if (isStartEndEques(edit))
			return;
		new WizColorPickerDialog(ctx,
				new WizColorPickerDialog.OnColorChangedListener() {

					@Override
					public void colorChanged(int color) {
						Object obj = null;
						if (WIZ_NOTE_COLOR_TYPE_FONT.equals(type)) {
							obj = new ForegroundColorSpan(color);
						} else if (WIZ_NOTE_COLOR_TYPE_BG.equals(type)) {
							obj = new BackgroundColorSpan(color);
						}
						if (obj != null) {
							setEditTextView(edit, getRSpannable(edit, obj));
						}
					}
				}, (new Paint()).getColor()).show();
	}

	// font颜色
	public static void setFontColor(Context ctx, final EditText edit) {
		setColor(ctx, edit, WIZ_NOTE_COLOR_TYPE_FONT);
	}

	// bg颜色
	public static void setFontBgColor(Context ctx, final EditText edit) {
		setColor(ctx, edit, WIZ_NOTE_COLOR_TYPE_BG);
	}

	// 分组
	public static void item(EditText edit) {
		if (isStartEndEques(edit))
			return;
		start = 0;
		end = 0;
		setEditTextView(edit, getRSpannable(edit, new WizBulletSpan()));
	}

	// +图片
	public static void addImage(Activity ctx, EditText edit, String src, ArrayList<Bitmap> drawables) {
		if (isStartEndEques(edit))
			return;
		//
		Drawable drawable = null;
		try {
			drawable = ImageUtil.getDrawableForSpan(ctx, src, ImageUtil.mSpanImageOwnerSD, drawables);
		} catch (OutOfMemoryError e) {
		}
		if (drawable == null)
			return;
		//
		String imgSrc = HTMLUtil.getImageSrc(src);
		ImageSpan imgSpan = new ImageSpan(drawable, imgSrc);
		setEditTextView(edit, getRSpanned(edit, imgSpan));

		//主动调用系统回收机制，但不保证执行回收
		if (drawable != null){
			drawable.setCallback(null);
			System.gc();
		}
	}

	// 下标
	public static void setSubscript(EditText edit) {
		if (isStartEndEques(edit))
			return;
		setEditTextView(edit, getRSpannable(edit, new SubscriptSpan()));
	}

	// 上标
	public static void setSuperscript(EditText edit) {
		if (isStartEndEques(edit))
			return;
		setEditTextView(edit, getRSpannable(edit, new SuperscriptSpan()));
	}

	// 黑体、粗体
	public static void setBold(EditText edit) {
		if (isStartEndEques(edit))
			return;
		Object obj = new StyleSpan(android.graphics.Typeface.BOLD);
		setEditTextView(edit, getRSpannable(edit, obj));
	}

	// 斜体
	public static void setItalic(EditText edit) {
		if (isStartEndEques(edit))
			return;
		Object obj = new StyleSpan(android.graphics.Typeface.ITALIC);
		setEditTextView(edit, getRSpannable(edit, obj));
	}

	// 设置字体大小
	public static void setFontSize(Context ctx, final EditText edit,
			Integer size) {
		if (isStartEndEques(edit))
			return;
		setEditTextView(edit, getRSpannable(edit, new AbsoluteSizeSpan(size)));
	}

	// 下划线
	public static void setUnderLine(EditText edit) {

		if (isStartEndEques(edit))
			return;
		setEditTextView(edit, getRSpannable(edit, new UnderlineSpan()));
	}

	// 删除线
	public static void setStrikethrough(EditText edit) {
		if (isStartEndEques(edit))
			return;
		setEditTextView(edit, getRSpannable(edit, new StrikethroughSpan()));
	}

	public static final int WIZ_NOTE_URL_TYPE_TEL = 0;
	public static final int WIZ_NOTE_URL_TYPE_MAIL = 1;
	public static final int WIZ_NOTE_URL_TYPE_SMS = 2;
	public static final int WIZ_NOTE_URL_TYPE_HTTP = 3;

	// 添加链接
	public static void setUrl(EditText edit, String url, int urlType) {

		String add = "";
		switch (urlType) {
		case WIZ_NOTE_URL_TYPE_TEL:
			add = "tel:";
			break;
		case WIZ_NOTE_URL_TYPE_MAIL:
			add = "mailto:";
			break;
		case WIZ_NOTE_URL_TYPE_SMS:
			add = "sms:";
			break;
		case WIZ_NOTE_URL_TYPE_HTTP:
			if (add.indexOf("http://") < 0) {
				add = "http://";
			}
			break;

		default:
			return;
		}
		setEditTextView(edit, getRSpannable(edit, new URLSpan(add + url + "")));
	}

	public static void updateWebViewScroll(WebView web, boolean pageUp) {
		int length = (int) (web.getContentHeight() * web.getScale());
		length -= web.getHeight();
		int webViewHeight = web.getHeight();
		int scrollX = web.getScrollX();
		int scrollY = web.getScrollY();
		if (pageUp) {
			scrollY -= webViewHeight;
			scrollY = scrollY < 0 ? 0 : scrollY;
		} else {
			scrollY += webViewHeight;
			scrollY = scrollY > length ? length : scrollY;
		}
		updateWebViewScroll(web, scrollX, scrollY);
	}

	public static void updateWebViewScroll(WebView web, int scrollX, int scrollY) {
		web.scrollTo(scrollX, scrollY);
	}

	public static void setZoomControlGone(View view) {
		Class<?> classType;
		Field field;
		try {
			classType = WebView.class;
			field = classType.getDeclaredField("mZoomButtonsController");
			field.setAccessible(true);
			ZoomButtonsController mZoomButtonsController = new ZoomButtonsController(
					view);
			mZoomButtonsController.getZoomControls().setVisibility(View.GONE);
			try {
				field.set(view, mZoomButtonsController);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
	}

	public static SpannableStringBuilder html2SpanBuilder(Activity ctx,
			String file, String srcPath, String owner, ArrayList<Bitmap> drawables) {
		if (TextUtils.isEmpty(file))
			return null;
		if (TextUtils.isEmpty(owner))
			owner = ImageUtil.mSpanImageOwnerSD;
		String body = HTMLUtil.getHtmlBody(file);
		body = HTMLUtil.clearCssTag(body);
		ImageGetter imgGetter = ImageUtil.getImageGetter(ctx, srcPath,
				owner, drawables);
		return (SpannableStringBuilder) Html.fromHtml(body, imgGetter, null);
	}

	@SuppressLint("NewApi")
	public static void setZoomControlGone(Context ctx, WebView web) {
		int sdkVersion = WizSystemSettings.getVersionSDK();
		int sdk3_0 = WizSystemSettings.androidSDKVersionOf3_0;
		if (sdkVersion > sdk3_0) {
			web.getSettings().setDisplayZoomControls(false);
		} else {
			WebViewUtil.setZoomControlGone(web);
		}
	}
	
	public static class WizBulletSpan extends BulletSpan {

		// private static final b a = c.a(EvernoteBulletSpan.class);
		private int b = 3;

		public WizBulletSpan() {
			super(20);
		}

		public void drawLeadingMargin(Canvas paramCanvas, Paint paramPaint,
				int paramInt1, int paramInt2, int paramInt3, int paramInt4,
				int paramInt5, CharSequence paramCharSequence, int paramInt6,
				int paramInt7, boolean paramBoolean, Layout paramLayout) {
			// if (((Spanned)paramCharSequence).getSpanStart(this) != paramInt6)
			// return;
			Paint.Style localStyle1 = paramPaint.getStyle();

			int i = paramPaint.getColor();
			paramPaint.setColor(-16777216);
			Paint.Style localStyle2 = Paint.Style.STROKE;
			paramPaint.setStyle(localStyle2);
			int j = paramInt2 * 5 + paramInt1;
			int k = this.b;
			float f1 = j + k;
			float f2 = (paramInt3 + paramInt4) / 2.0F;
			// paramCanvas.drawCircle(f1, f2, 5.0F, paramPaint);
			paramCanvas.drawRect(f1, f2, f1 + 10, f2 + 10, paramPaint);
			paramPaint.setColor(i);
			paramPaint.setStyle(localStyle1);
		}

		public int getLeadingMargin(boolean paramBoolean) {
			return (this.b + 10 + 5);
		}
	}
}
