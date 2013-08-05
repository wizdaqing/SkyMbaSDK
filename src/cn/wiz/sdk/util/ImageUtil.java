package cn.wiz.sdk.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.text.Html;
import android.text.Html.ImageGetter;
import android.text.TextUtils;
import android.util.Log;

public class ImageUtil {

	 /**
	 * 截取一个图像的中央区域
	 *
	 * @param image
	 * 图像File
	 * @param w
	 * 需要截图的宽度
	 * @param h
	 * 需要截图的高度
	 * @return 返回一个
	 * @throws IOException
	 */
	 public static void cutImage(String src, String dest, int w, int h) {
	//
	// InputStream inputStream = null;
	// BufferedImage bufferedSrcImage = null;
	// BufferedImage bufferedDestImage = null;
	// try {
	// File image = new File(src);
	// inputStream = new FileInputStream(image);
	// // 用ImageIO读取字节流
	// bufferedSrcImage = ImageIO.read(inputStream);
	// /**
	// * 返回源图片的宽度、高度
	// */
	// int srcW = bufferedSrcImage.getWidth();
	// int srcH = bufferedSrcImage.getHeight();
	// int x = 0, y = 0;
	// // 使截图区域居中
	// x = srcW / 2 - w / 2;
	// y = srcH / 2 - h / 2;
	// srcW = srcW / 2 + w / 2;
	// srcH = srcH / 2 + h / 2;
	// // 生成图片
	// bufferedDestImage = new BufferedImage(w, h,
	// BufferedImage.TYPE_INT_RGB);
	// Graphics g = bufferedDestImage.getGraphics();
	// g.drawImage(bufferedSrcImage, 0, 0, w, h, x, y, srcW, srcH, null);
	// ImageIO.write(bufferedDestImage, "jpg", new File(dest));
	// } catch (FileNotFoundException e) {
	// } catch (IOException e) {
	// e.printStackTrace();
	// } finally {
	// try {
	// inputStream.close();
	// bufferedSrcImage.flush();
	// bufferedDestImage.flush();
	// bufferedSrcImage = null;
	// bufferedDestImage = null;
	// } catch (IOException e) {
	// }
	// }
	 }

	 /**
	  * 根据文件名获取存取图片的格式及保存质量
	  * @param bmp
	  * @param fileName
	  * @return
	  */
	 // 保存图片
	@SuppressLint("DefaultLocale")
	public static boolean saveBitmap(Bitmap bmp, String fileName){
		Bitmap.CompressFormat format = Bitmap.CompressFormat.PNG;
		int quality = 100;
		String type = FileUtil.getTypeForFile(fileName);
		if (TextUtils.isEmpty(type))
			type = "";
		type = type.toLowerCase();
		if (type.equals(".jpg") || type.equals(".jpeg")) {
			format = Bitmap.CompressFormat.JPEG;
			quality = 80;
		} else if (type.equals(".png")) {
			format = Bitmap.CompressFormat.PNG;
		}

		return saveBitmap(bmp, format, quality, fileName);
	}

	// 保存图片
	public static boolean saveBitmap(Bitmap bmp, CompressFormat format, int quality, String fileName){
		java.io.FileOutputStream out = null;
		try {
			out = new java.io.FileOutputStream(fileName);
			bmp.compress(format, quality, out);
		} catch (FileNotFoundException e) {
			return false;
		} catch (Exception e) {
			return false;
		} finally {
			try {
				out.flush();
				out.close();
			} catch (IOException e) {
			}
		}

		return true;
	}

	/**
	 * 处理被旋转的图片
	 * @param fileName picture's file name
	 * @param zoom true if you want zoom the picture
	 * @return
	 */
	public static boolean reSaveRevolvingBitmap(Context ctx, String fileName, boolean zoom){
		File file = new File(fileName);
		int degree = readBitmapDegree(file.getAbsolutePath());
		if (degree == 0)
			return true;
		
		Bitmap srcBmp;
		if (zoom) {
			srcBmp = zoomBitmap(ctx, fileName, 1024, 1024);
		} else {
			srcBmp = BitmapFactory.decodeFile(fileName);
		}

		Bitmap destBmp = rotaingBitmap(srcBmp, degree);
		return saveBitmap(destBmp , fileName);
	}

	/** 
	 * 读取图片属性：旋转的角度 
	 * @param path 图片绝对路径 
	 * @return degree旋转的角度 
	 */
	@SuppressLint("NewApi")
	public static int readBitmapDegree(String path) {
		int degree = 0;
		try {
			ExifInterface exifInterface = new ExifInterface(path);
			int orientation = exifInterface.getAttributeInt(
					ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			switch (orientation) {
			case ExifInterface.ORIENTATION_ROTATE_90:
				degree = 90;
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:
				degree = 180;
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:
				degree = 270;
				break;
			}
		} catch (IOException e) {
		}
		return degree;
	}

	/**
	 * 旋转图片
	 * 
	 * @param angle
	 * @param bitmap
	 * @return Bitmap
	 */
	public static Bitmap rotaingBitmap(Bitmap srcBmp, int angle) {
		if (srcBmp == null)
			return null;
		// 旋转图片 动作
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		// 创建新的图片
		Bitmap destBmp = Bitmap.createBitmap(srcBmp, 0, 0, srcBmp.getWidth(),
				srcBmp.getHeight(), matrix, true);
		recycleOldBmp(srcBmp, destBmp);
		return destBmp;
	}

	// 缩放图片
	public static Bitmap resizeBitmap(Bitmap bmp, int newWidth, int newHeight) {
		if (bmp == null)
			 return null;
		
		if (newWidth <= 0 || newHeight <= 0) {
			return null;
		}
		int width = bmp.getWidth();
		int height = bmp.getHeight();
		// 设置想要的大小
		// 计算缩放比例
		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;
		// 取得想要缩放的matrix参数
		Matrix matrix = new Matrix();
		matrix.postScale(scaleWidth, scaleHeight);
		// 得到新的图片
		Bitmap newBmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, false);
		return newBmp;
	}

	public static Bitmap getResizeBmp(Bitmap bmp, int maxLength, boolean setHeight) {
		if (bmp != null) {
			if (bmp.getWidth() > maxLength || bmp.getHeight() > maxLength) {// 缩放图片
				int width = bmp.getWidth();
				int height = bmp.getHeight();
				double fWidth = width / maxLength;
				double fHeight = height / maxLength;
				double fRate = Math.max(fWidth, fHeight);
				if (setHeight)
					fRate = fHeight;
				int newWidth = (int) (width / fRate);
				int newHeight = (int) (height / fRate);
				//
				if (setHeight)
					return resizeBitmap(bmp, newWidth, maxLength);
				return resizeBitmap(bmp, newWidth, newHeight);
			}
		}
		return null;
	}

	public static Bitmap getResizeBmpByMin(Bitmap bmp, int minLength) {
		if (bmp != null) {
			if (bmp.getWidth() > minLength || bmp.getHeight() > minLength) {// 缩放图片
				int width = bmp.getWidth();
				int height = bmp.getHeight();
				double fWidth = width / minLength;
				double fHeight = height / minLength;
				double fRate = Math.min(fWidth, fHeight);
				int newWidth = (int) (width / fRate);
				int newHeight = (int) (height / fRate);
				return resizeBitmap(bmp, newWidth, newHeight);
			}
		}
		return null;
	}

	public static Bitmap getResizeBmpByMax(Bitmap bmp, int maxLength) {
		if (bmp != null) {
			if (bmp.getWidth() > maxLength || bmp.getHeight() > maxLength) {// 缩放图片
				int width = bmp.getWidth();
				int height = bmp.getHeight();
				double fWidth = width / maxLength;
				double fHeight = height / maxLength;
				double fRate = Math.max(fWidth, fHeight);
				int newWidth = (int) (width / fRate);
				int newHeight = (int) (height / fRate);
				return resizeBitmap(bmp, newWidth, newHeight);
			}
		}
		return bmp;
	}

	public static Bitmap cutImage(Bitmap bmp, int newWidth, int newHeight,
			boolean isPerfect) {
		if (bmp == null)
			return null;

		int width = bmp.getWidth();
		int height = bmp.getHeight();

		if (isPerfect && width < newWidth) {
			return null;
		}

		if (isPerfect && height < newHeight) {
			return null;
		}

		Bitmap resBmp = Bitmap.createBitmap(bmp, (width / 2 - newWidth / 2),
				(height / 2 - newHeight / 2), newWidth, newHeight);
		return resBmp;
	}

	public static void recycleOldBmp(Bitmap oldBmp, Bitmap newBmp){
		if (oldBmp == null)
			return;

		if (oldBmp == newBmp)
			return;

		if (oldBmp.isRecycled())
			return;

		oldBmp.recycle();
		System.gc();
	}

	public static void recycleBmp(Bitmap bmp){
		if (bmp == null)
			return;

		if (bmp.isRecycled())
			return;

		bmp.recycle();
		System.gc();
	}
	
	public static boolean isRecycleBmp(Bitmap bmp) {
		if (bmp == null)
			return true;

		int height = bmp.getHeight();
		int widght = bmp.getHeight();
		int i1 = height / widght;
		int i2 = widght / height;

		if (height <= 20 || widght <= 20 || i1 > 10 || i2 > 10) {
			bmp.recycle();
			return true;
		}
		return false;

	}

	public static Bitmap byte2Bitmap(byte[] image) {
		if (image == null)
			return null;
		//
		if (image.length <= 0)
			return null;
		//
		return android.graphics.BitmapFactory.decodeByteArray(image, 0,
				image.length);
	}

	public static int computeSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		int initialSize = computeInitialSampleSize(options, minSideLength,
				maxNumOfPixels);

		int roundedSize;
		if (initialSize <= 8) {
			roundedSize = 1;
			while (roundedSize < initialSize) {
				roundedSize <<= 1;
			}
		} else {
			roundedSize = (initialSize + 7) / 8 * 8;
		}

		return roundedSize;
	}

	public static int computeInitialSampleSize(BitmapFactory.Options options,
			int minSideLength, int maxNumOfPixels) {
		double w = options.outWidth;
		double h = options.outHeight;

		int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math
				.sqrt(w * h / maxNumOfPixels));
		int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(
				Math.floor(w / minSideLength), Math.floor(h / minSideLength));

		if (upperBound < lowerBound) {
			return lowerBound;
		}

		if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
			return 1;
		} else if (minSideLength == -1) {
			return lowerBound;
		} else {
			return upperBound;
		}
	}

	public static Bitmap getResourcesBitmap(String imageFile) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(imageFile, opts);

		opts.inSampleSize = computeSampleSize(opts, -1, 128 * 128);
		opts.inJustDecodeBounds = false;
		Bitmap bmp = null;
		try {
			bmp = BitmapFactory.decodeFile(imageFile, opts);
		} catch (OutOfMemoryError err) {
		} catch (Exception e) {
		}
		return bmp;
	}

	// drawable 转换成 bitmap
	public static Bitmap drawable2Bitmap(Drawable drawable) {
		try {
			int width = drawable.getIntrinsicWidth(); // 取 drawable 的长宽
			int height = drawable.getIntrinsicHeight();
			Bitmap.Config config = drawable.getOpacity() != PixelFormat.JPEG ? Bitmap.Config.ARGB_4444
					: Bitmap.Config.RGB_565; // 取 drawable 的颜色格式
			Bitmap bitmap = Bitmap.createBitmap(width, height, config);
			Canvas canvas = new Canvas(bitmap); // 建立对应 bitmap 的画布
			drawable.setBounds(0, 0, width, height);
			drawable.draw(canvas); // 把 drawable 内容画到画布中
			return bitmap;
		} catch (OutOfMemoryError err) {
			return null;
		}
	}

	public static Bitmap zoomBitmap(Context ctx, String file, int sWidth) {
		InputStream inputStream = null;
		try {
			inputStream = openSharedFileInputStreamByFileName(ctx, file);

			BitmapFactory.Options op = new BitmapFactory.Options();
			op.inJustDecodeBounds = true;

			BitmapFactory.decodeStream(inputStream , null, op);
			int sampleSize = 1;
			if (op.outWidth > sWidth) {
				sampleSize = (int) Math.ceil(op.outWidth / (float) sWidth); // 计算宽度比例
			}
			//
			return getBitmap(ctx, file, sampleSize);
		} catch (FileNotFoundException e) {
			return null;
		} finally {
			try {
				inputStream.close();
			} catch (Exception e) {
			}
		}
	}
	
	private static InputStream openSharedFileInputStreamByFileName(Context ctx, String file) throws FileNotFoundException{
		File tmpFile = new File(file);
		Uri tmpUri = Uri.fromFile(tmpFile);
		return ctx.getContentResolver().openInputStream(tmpUri);
	}
	
	public static Bitmap zoomBitmap(Context ctx, String file, int maxWidth, int maxHeigth) {
		InputStream inputStream = null;
		try {
			inputStream = openSharedFileInputStreamByFileName(ctx, file);

			BitmapFactory.Options op = new BitmapFactory.Options();
			op.inJustDecodeBounds = true;

			BitmapFactory.decodeStream(inputStream , null, op);
			int sampleWidthSize = (int) Math.ceil(op.outWidth / maxWidth); // 计算宽度比例
			int sampleHeightSize = (int) Math.ceil(op.outHeight / maxHeigth);
			int sampleSize = Math.max(sampleWidthSize, sampleHeightSize);
			return getBitmap(ctx, file, sampleSize);
		} catch (FileNotFoundException e) {
			return null;
		} finally {
			try {
				inputStream.close();
			} catch (Exception e) {
			}
		}
	}

	public static Bitmap getBitmap(Context ctx, String file, int sampleSize) throws FileNotFoundException{
		InputStream inputStream = null;
		try {
			inputStream = openSharedFileInputStreamByFileName(ctx, file);

			BitmapFactory.Options op = new BitmapFactory.Options();
			op.inPreferredConfig = Bitmap.Config.RGB_565;
			op.inSampleSize = sampleSize;
			op.inJustDecodeBounds = false; // 注意这里，一定要设置为false，因为上面我们将其设置为true来获取图片尺寸了
			Bitmap bmp = BitmapFactory.decodeStream(inputStream, null, op);
			return bmp;
		} finally {
			try {
				inputStream.close();
			} catch (Exception e) {
			}
		}
	}

	// 读取bitmap对象
	public static Bitmap getBitmap(String name) {
		try {
			return BitmapFactory.decodeFile(name);
		} catch (OutOfMemoryError err) {
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	//
	public static boolean getBitmapSizeFromStream(InputStream is, int[] ret) {
		try {
			if (ret == null)
				return false;
			if (ret.length < 2)
				return false;
			//
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(is, null, opts);
			//
			ret[0] = opts.outWidth;
			ret[1] = opts.outHeight;
			return ret[0] > 0 && ret[1] > 0;
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public static Bitmap resizeAndCutBitmap(Bitmap bmpOrg, int newWidth,
			int newHeight) {

		try {
			int orgWidth = bmpOrg.getWidth();
			int orgHeight = bmpOrg.getHeight();
			//
			if (orgWidth <= 0 || orgHeight <= 0)
				return null;
			//
			float scaleX = newWidth / (float) orgWidth;
			float scaleY = newHeight / (float) orgHeight;
			float baseScale = Math.max(scaleX, scaleY);
			//
			int destWidth = (int) (newWidth / baseScale);
			int destHeight = (int) (newHeight / baseScale);
			//
			int x = (orgWidth - destWidth) / 2;
			int y = (orgHeight - destHeight) / 2;
			//
			final Matrix matrix = new Matrix();
			matrix.setScale(baseScale, baseScale);
			Bitmap bitmapRet = Bitmap.createBitmap(bmpOrg, x, y, destWidth,
					destHeight, matrix, false);
	
			return bitmapRet;
		}
		catch (OutOfMemoryError e) {
			return null;
		}
		catch (Exception e) {
			return null;
		}
	}

	// resources
	public static ImageGetter getImageGetter(Context ctx, String srcPath, String owner, ArrayList<Bitmap> drawables) {
		class MyImageGetter implements Html.ImageGetter {
			public MyImageGetter(Context ctx, String srcPath, String owner, ArrayList<Bitmap> drawables) {
				mContext = ctx;
				mSrcPath = srcPath;
				mOwner = owner;
				mDrawables = drawables;
			}
			private String mSrcPath;
			private String mOwner;
			private Context mContext;
			private ArrayList<Bitmap> mDrawables;
			public Drawable getDrawable(String source) {
				if (source.indexOf("/") == 0) {
					source = FileUtil.pathRemoveBackslash(mSrcPath) + source;
				} else {
					source = FileUtil.pathAddBackslash(mSrcPath) + source;
				}
				Drawable drawable = getDrawableForSpan(mContext, source, mOwner, mDrawables);
				return drawable;
			}
		};
		//
		return new MyImageGetter(ctx, srcPath, owner, drawables);
	}

	public static final String mSpanImageOwnerSD = "SDCard";
	public static final String mSpanImageOwnerAs = "Assets";

	// 获取drawable对象(缩放后的对象)
	public static Drawable getDrawableForSpan(Context ctx, String src,
			String owner, ArrayList<Bitmap> drawables) {
		Drawable drawable = null;
		int screenWidth = ctx.getResources().getDisplayMetrics().widthPixels;
		if (TextUtils.isEmpty(owner) || owner.equals(mSpanImageOwnerSD)) {
			drawable = ImageUtil.zoomDrawable(ctx, src, screenWidth / 3, drawables);
		} else if (owner.equals(mSpanImageOwnerAs)) {
			drawable = ImageUtil.getDrawableFromAsset(ctx, src);
		}
		if (drawable == null)
			return null;

		drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight());
		return drawable;
	}

	public static Drawable zoomDrawable(Drawable drawable, int dWidth) {
		Bitmap oldbmp = null;
		Bitmap newbmp = null;
		try {
			oldbmp = drawable2Bitmap(drawable);
			if (oldbmp == null)
				return null;
			newbmp = getResizeBmpByMax(oldbmp, dWidth);
			return new BitmapDrawable(newbmp);
		} finally {
			if (oldbmp != null && oldbmp != newbmp)
				oldbmp.recycle();
		}
	}

	public static Drawable zoomDrawable(Context ctx, String file, int sWidth, ArrayList<Bitmap> drawables) {
		Bitmap bmp = zoomBitmap(ctx, file, sWidth);
		if (bmp == null)
			return null;
		//
		drawables.add(bmp);
		//
		return new BitmapDrawable(ctx.getResources(), bmp);
	}
	public static Drawable zoomDrawable(Context ctx, String file, int sWidth) {
		Bitmap bmp = zoomBitmap(ctx, file, sWidth);
		if (bmp == null)
			return null;
		return new BitmapDrawable(bmp);
	}

	public static Drawable getDrawableFromSDCard(String src) {
		InputStream is = null;
		try {
			URL url = new URL("file://" + src);
			is = url.openStream();
			return Drawable.createFromStream(is, "");
		} catch (OutOfMemoryError e) {
			return null;
		} catch (Exception e) {
			return null;
		} finally {
			try {
				is.close();
			} catch (Exception e) {
			}
		}
	}

	public static Drawable getDrawableFromAsset(Context ctx, String src) {
		InputStream is = null;
		try {
			AssetManager am = ctx.getResources().getAssets();
			is = am.open(src);
			return Drawable.createFromStream(is, "");
		} catch (OutOfMemoryError e) {
			return null;
		} catch (Exception e) {
			return null;
		} finally {
			try {
				is.close();
			} catch (IOException e) {
			}
		}
	}
	
	public static byte[] bitmap2ByteArrayNoRecycle(Bitmap bmp) {
		//
		if (bmp == null)
			return null;
		//
		ByteArrayOutputStream os = null;
		try {
			os = new ByteArrayOutputStream();
			// 将Bitmap压缩成JPEG编码，质量为70%存储
			bmp.compress(Bitmap.CompressFormat.JPEG, 70, os);
			return os.toByteArray();
		} catch (Exception e) {
			return null;
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	/**
	 * Converts a immutable bitmap to a mutable bitmap. This operation doesn't allocates
	 * more memory that there is already allocated.
	 * 
	 * @param imgIn - Source image. It will be released, and should not be used more
	 * @return a copy of imgIn, but muttable.
	 */
	public static Bitmap convertToMutable(Bitmap imgIn) {
	    try {
	        //this is the file going to use temporally to save the bytes. 
	        // This file will not be a image, it will store the raw image data.
	        File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

	        //Open an RandomAccessFile
	        //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
	        //into AndroidManifest.xml file
	        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

	        // get the width and height of the source bitmap.
	        int width = imgIn.getWidth();
	        int height = imgIn.getHeight();
	        Config type = imgIn.getConfig();
	        if (type == null) {
	        	return imgIn;
	        }
	        
	        //Copy the byte to the file
	        //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
	        FileChannel channel = randomAccessFile.getChannel();
	        MappedByteBuffer map = channel.map(MapMode.READ_WRITE, 0, imgIn.getRowBytes()*height);
	        imgIn.copyPixelsToBuffer(map);
	        //recycle the source bitmap, this will be no longer used.
	        imgIn.recycle();
	        System.gc();// try to force the bytes from the imgIn to be released

	        //Create a new bitmap to load the bitmap again. Probably the memory will be available. 
	        imgIn = Bitmap.createBitmap(width, height, type);
	        map.position(0);
	        //load it back from temporary 
	        imgIn.copyPixelsFromBuffer(map);
	        //close the temporary file and channel , then delete that also
	        channel.close();
	        randomAccessFile.close();

	        int width1 = imgIn.getWidth();
	        int height1 = imgIn.getHeight();
	        Log.v("bmp", "width1:" + width1 + ";width1:" + height1);
	        // delete the temp file
	        file.delete();

	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } 

	    return imgIn;
	}
	/**
	 * 图片圆角
	 * @param context
	 * @param resId
	 * @return
	 */
	public static Bitmap getRoundCornerBitmap(Context context, Bitmap bitmap) { 
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);  
        Canvas canvas = new Canvas(output);  
        int color = 0xff424242;  
        Paint paint = new Paint();  
        Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());  
        RectF rectF = new RectF(rect);  
        float roundPx = 8;  
        paint.setAntiAlias(true);  
        canvas.drawARGB(0, 0, 0, 0);  
        paint.setColor(color);  
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);  
        paint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));  
        canvas.drawBitmap(bitmap, rect, rect, paint); 
        bitmap.recycle();
        return output;  
    }
	public static Bitmap decodeSampledBitmapFromBytes(byte[] data, int reqWidth, int reqHeight){
		BitmapFactory.Options options = new BitmapFactory.Options();
	    options.inJustDecodeBounds = true;
	    BitmapFactory.decodeByteArray(data, 0, data.length);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeByteArray(data, 0, data.length);
	}
    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates
     * the closest inSampleSize that will result in the final decoded bitmap having a width and
     * height equal to or larger than the requested width and height. This implementation does not
     * ensure a power of 2 is returned for inSampleSize which can be faster when decoding but
     * results in a larger bitmap which isn't as useful for caching purposes.
     *
     * @param options An options object with out* params already populated (run through a decode*
     *            method with inJustDecodeBounds==true
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee a final image
            // with both dimensions larger than or equal to the requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            final float totalPixels = width * height;

            // Anything more than 2x the requested pixels we'll sample down further
            final float totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
                inSampleSize++;
            }
        }
        return inSampleSize;
    }

	public static boolean filterImageByMinLength(Context ctx, String file,
			int minLength) {
		try {
			BitmapFactory.Options op = getOptions(ctx, file, true);
			int width = op.outWidth;
			int height = op.outHeight;

			return width > minLength && height > minLength;
		} catch (FileNotFoundException e) {
			return false;
		}
	}

	public static boolean filterImageByMaxLength(Context ctx, String file,
			int maxLength) {
		try {
			BitmapFactory.Options op = getOptions(ctx, file, true);
			int width = op.outWidth;
			int height = op.outHeight;

			return width < maxLength && height < maxLength;
		} catch (FileNotFoundException e) {
			return false;
		}
	}

	private static BitmapFactory.Options getOptions(Context ctx, String file, boolean decodeBounds) throws FileNotFoundException{
		InputStream inputStream = null;
		try {
			inputStream = openSharedFileInputStreamByFileName(ctx, file);

			BitmapFactory.Options op = new BitmapFactory.Options();
			op.inJustDecodeBounds = decodeBounds;
			BitmapFactory.decodeStream(inputStream, null, op);
			return op;
		} finally {
			try {
				inputStream.close();
			} catch (Exception e) {
			}
		}
	}
}
