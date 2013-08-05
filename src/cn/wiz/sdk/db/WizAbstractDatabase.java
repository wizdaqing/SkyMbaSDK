package cn.wiz.sdk.db;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

import redstone.xmlrpc.zip.ZipEntry;
import redstone.xmlrpc.zip.ZipFile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import cn.wiz.sdk.api.WizObject.*;
import cn.wiz.sdk.settings.WizAccountSettings;
import cn.wiz.sdk.settings.WizSystemSettings;
import cn.wiz.sdk.util.FileUtil;
import cn.wiz.sdk.util.HTMLUtil;
import cn.wiz.sdk.util.ImageUtil;
import cn.wiz.sdk.util.WizMisc;

@SuppressWarnings("unused")
public class WizAbstractDatabase {

	static private Object mLock = new Object();
	static ConcurrentHashMap<String, WizAbstractDatabase> mDatabaseMap = new ConcurrentHashMap<String, WizAbstractDatabase>();

	@SuppressLint("DefaultLocale")
	static public WizAbstractDatabase getDb(Context ctx, String userId) {
		synchronized (mLock) {
			String key = "/" + userId + "/";
			key = key.toLowerCase();
			//
			WizAbstractDatabase db = mDatabaseMap.get(key);
			if (db != null)
				return db;
			//
			db = new WizAbstractDatabase(ctx, userId);
			//
			mDatabaseMap.put(key, db);
			//
			return db;
		}
	}

	//
	// 在切换用户的时候可以关闭
	@SuppressLint("DefaultLocale")
	static public void closeDb(String userId) {
		synchronized (mLock) {
			String key = Long.toString(Thread.currentThread().getId()) + "/"
					+ userId + "/";
			key = key.toLowerCase();
			//
			WizAbstractDatabase db = mDatabaseMap.get(key);
			if (db == null)
				return;
			//
			mDatabaseMap.put(key, null);
			//
			try {
				db.closeDatabase();
			} catch (Exception e) {

			}
		}
	}

	// //////////////////////////////////////////////////////////
	// private methods

	final String sqlTableAbstract = "CREATE TABLE WIZ_ABSTRACT (\n"
			+ "ABSTRACT_GUID					char(36)						not null,\n"
			+ "ABSTRACT_TYPE					varchar(50)						not null,\n"
			+ "AVSTRACT_TEXT					varchar(3000),\n"
			+ "ABSTRACT_IMAGE					blob,\n"
			+ "primary key (ABSTRACT_GUID, ABSTRACT_TYPE)\n" + ")";

	final String sqlFieldAbstract = "ABSTRACT_GUID, ABSTRACT_TYPE, AVSTRACT_TEXT, ABSTRACT_IMAGE";

	private SQLiteDatabase mDB;
	private String mAccountUserId;

	private WizAbstractDatabase(Context ctx, String userId) {
		mAccountUserId = userId;
		//
		String path = WizAccountSettings.getAccountPath(ctx, mAccountUserId);
		path = FileUtil.pathAddBackslash(path);

		String fileName = path + "temp.db";

		if (!openDatabase(fileName)) {
		}
	}

	// 检索表
	private boolean tableExists(String tableName) {
		try {
			boolean exists = false;
			//
			Cursor cursor = mDB.rawQuery(
					"select count(*) from sqlite_master where type='table' and tbl_name='"
							+ tableName + "'", null);
			try {
				if (cursor.moveToNext()) {
					int count = cursor.getInt(0);
					exists = (count == 1);
				}
			} finally {
				cursor.close();
			}
			//
			return exists;
		} catch (SQLiteException err) {
			return false;
		}
	}

	//
	private boolean checkTable(String tableName, String tableSql) {
		if (tableExists(tableName))
			return true;
		//
		return execSql(tableSql);
	}

	//
	private boolean openDatabase(String fileName) {
		if (mDB == null) {
			try {
				mDB = SQLiteDatabase.openOrCreateDatabase(fileName, null);
			} catch (Exception e) {
				return false;
			}
			if (!checkTable("WIZ_ABSTRACT", this.sqlTableAbstract))
				return false;
		}
		//
		return true;

	}

	public void closeDatabase() {
		if (mDB != null) {
			mDB.close();
			mDB = null;
		}
	}

	boolean updateBlob(String tableName, String fieldName, byte[] data,
			String where) {
		String sql = "update " + tableName + " set " + fieldName + "=? where "
				+ where;
		Object param[] = { data };
		//
		return execSql(sql, param);
	}

	boolean updateString(String tableName, String fieldName, String value,
			String where) {
		String sql = "update " + tableName + " set " + fieldName + "=? where "
				+ where;
		Object param[] = { value };
		return execSql(sql, param);
	}

	boolean updateImageData(String guid, String type, byte[] data) {
		return updateBlob("WIZ_ABSTRACT", "ABSTRACT_IMAGE", data,
				"ABSTRACT_GUID='" + guid + "' and ABSTRACT_TYPE='" + type + "'");
	}

	boolean updateTextData(String guid, String type, String value) {
		return updateString("WIZ_ABSTRACT", "AVSTRACT_TEXT", value,
				"ABSTRACT_GUID='" + guid + "' and ABSTRACT_TYPE='" + type + "'");
	}

	boolean setImageData(String guid, String type, byte[] data) {
		if (checkAbstract(guid, type))
			return updateImageData(guid, type, data);
		return false;
	}

	boolean setTextData(String guid, String type, String value) {
		if (checkAbstract(guid, type))
			return updateTextData(guid, type, value);
		return false;
	}

	boolean checkAbstract(String guid, String type) {
		if (getAbstractByGuidAndType(guid, type) != null)
			return true;
		return false;
	}

	synchronized boolean execSql(String sql, Object[] sqlData) {
		boolean ret = false;
		try {
			mDB.execSQL(sql, sqlData);
			ret = true;
		} catch (NullPointerException e) {
			ret = false;
		} catch (Exception err) {
			ret = false;
			// err.printStackTrace();
		}
		//
		return ret;
	}

	boolean execSql(String sql) {
		boolean ret = false;
		try {
			mDB.execSQL(sql);
			ret = true;
		} catch (NullPointerException e) {
			ret = false;
		} catch (Exception err) {
			ret = false;
			// err.printStackTrace();
		}
		//
		return ret;
	}

	static String stringToSQLString(String str) {
		if (str == null)
			return "NULL";
		if (str.length() == 0)
			return "NULL";
		//
		str = str.replace("'", "''");
		//
		return "'" + str + "'";
	}

	WizAbstract sqlToAbstract(String sql) {
		ArrayList<WizAbstract> arr = sqlToAbstractArray(sql);
		//
		if (0 == arr.size())
			return null;
		//
		return arr.get(0);
	}

	ArrayList<WizAbstract> sqlToAbstractArray(String sql) {
		ArrayList<WizAbstract> arr = new ArrayList<WizAbstract>();
		try {
			Cursor cursor = mDB.rawQuery(sql, null);
			//
			try {
				while (cursor.moveToNext()) {
					String guid = cursor.getString(0);
					String type = cursor.getString(1);
					String text = cursor.getString(2);
					byte[] blob = cursor.getBlob(3);
					//
					Bitmap bmp = null;
					try {
						if (blob != null) {
							bmp = ImageUtil.byte2Bitmap(blob);
						}
					} catch (Exception e) {
					}
					//
					WizAbstract elem = new WizAbstract(guid, type, text, bmp);
					arr.add(elem);
				}
			} finally {
				cursor.close();
			}
		} catch (Exception err) {
			err.printStackTrace();
		}
		//
		return arr;

	}

	boolean addAbstract(WizAbstract data) {
		//
		byte[] bmpArr = ImageUtil.bitmap2ByteArrayNoRecycle(data.abstractImage);
		//
		String sql = "insert into WIZ_ABSTRACT (ABSTRACT_GUID, ABSTRACT_TYPE, AVSTRACT_TEXT, ABSTRACT_IMAGE) values ("
				+ stringToSQLString(data.documentGuid)
				+ ", "
				+ stringToSQLString(data.abstractType) + ", ?,?);";
		Object[] objArray = new Object[] { data.abstractText, bmpArr };
		return execSql(sql, objArray);
	}

	boolean updateAbstract(WizAbstract data) {
		//
		WizAbstract dataExists = getAbstractByGuidAndType(data.documentGuid,
				data.abstractType);

		if (dataExists != null) {

			String text = data.abstractText;
			Bitmap bmp = data.abstractImage;
			//
			byte[] bmpArr = ImageUtil.bitmap2ByteArrayNoRecycle(bmp);
			//
			updateTextData(data.documentGuid, data.abstractType, text);
			updateImageData(data.documentGuid, data.abstractType, bmpArr);
		} else {
			addAbstract(data);
		}

		return true;
	}

	public static final String WIZ_ABSTRACT_TYPE_PAD = "Pad";
	public static final String WIZ_ABSTRACT_TYPE_PHONE = "Phone";

	void deleteOldAbstracts(String docGuid) {
		deleteOldAbstractByGuid(docGuid, WIZ_ABSTRACT_TYPE_PAD);
		deleteOldAbstractByGuid(docGuid, WIZ_ABSTRACT_TYPE_PHONE);
	}

	public void deleteOldAbstractByGuid(String docGuid, String type) {
		String sql = "delete from WIZ_ABSTRACT where ABSTRACT_GUID="
				+ stringToSQLString(docGuid);
		execSql(sql);
	}

	boolean deleteAbstracts(String docGuid) {
		ArrayList<WizAbstract> dataArrayExists = getAbstractsByGuid(docGuid);

		if (dataArrayExists == null || dataArrayExists.size() == 0)
			return true;

		String sql = "delete from WIZ_ABSTRACT where ABSTRACT_GUID="
				+ stringToSQLString(docGuid);
		return execSql(sql);
	}

	void deleteAbstracts(ArrayList<WizDocument> arr) {
		for (int i = 0; i < arr.size(); i++) {
			WizDocument data = arr.get(i);
			deleteAbstracts(data.guid);
		}
	}

	public WizAbstract getAbstractByGuidAndType(String docGuid, String type) {
		String sql = "select " + sqlFieldAbstract
				+ " from WIZ_ABSTRACT where ABSTRACT_GUID="
				+ stringToSQLString(docGuid) + " and ABSTRACT_TYPE="
				+ stringToSQLString(type);
		return sqlToAbstract(sql);
	}

	public ArrayList<WizAbstract> getAbstractsByGuid(String docGuid) {
		String sql = "select " + sqlFieldAbstract
				+ " from WIZ_ABSTRACT where ABSTRACT_GUID="
				+ stringToSQLString(docGuid);
		return sqlToAbstractArray(sql);
	}

	boolean dropTable(String tableName) {
		try {
			//
			String sql = "DROP TABLE " + stringToSQLString(tableName);
			return execSql(sql);
		} catch (Exception err) {
			err.printStackTrace();
		}
		return false;
	}

	public WizAbstract createAbstract(Context ctx, String userId,
			String documentGUID, String type) {
		//
		WizAbstract data = iniAbstract(ctx, userId, documentGUID, type);
		if (data == null)
			return null;
		//
		updateAbstract(data);
		//
		return data;
	}

	public static WizAbstract iniAbstract(Context ctx, String userId,
			String documentGUID, String type) {
		WizAbstractData data = iniAbstract(ctx, userId, documentGUID);
		if (data == null)
			return null;
		//
		WizAbstract currentAbstract = new WizAbstract(documentGUID, type,
				data.abstractText, data.abstractImage);
		//
		return currentAbstract;
	}

	// 摘要的字数长度
	private final static int ABSTRACT_STRING_LENGTH = 200;
	//
	private static int mThumbSize = 0;

	private static int getAbstractThumbSize(Context ctx) {
		if (mThumbSize != 0)
			return mThumbSize;

		int defaultSize = 55;
		try {
			//TODO:如果此处为非Activity对象，则会报异常
			Activity activity = (Activity) ctx;
			float density = WizSystemSettings.getScreenDensity(activity);
			mThumbSize = (int) (defaultSize * density);
		} catch (Exception e) {
			mThumbSize = defaultSize;
		}
		//
		return mThumbSize;
	}

	// 获取wizabstract对象的image
	@SuppressLint("DefaultLocale")
	public static WizAbstractData iniAbstract(Context ctx, String userId,
			String documentGuid) {
		String zipFileName = WizDocument.getZipFileName(ctx, userId,
				documentGuid);
		if (!FileUtil.fileExists(zipFileName))
			return null;
		//
		String textResult = null;
		Bitmap bmpResult = null;
		//
		ZipFile zip = null;
		try {
			zip = new ZipFile(zipFileName, "GBK");// 支持中文

			ZipEntry entryBitmap = null;
			//
			long maxFileSize = 0;
			//
			@SuppressWarnings("rawtypes")
			Enumeration entries = zip.getEntries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				String entryName = entry.getName();
				if (entryName == null)
					continue;
				if (entryName.equals(""))
					continue;
				//
				if (entryName.indexOf("index.htm") != -1) {
					InputStream is = null;
					try {
						is = zip.getInputStream(entry);
						textResult = FileUtil.loadTextFromStream(is);
						if (textResult.length() > 10 * 1024) {
							textResult = textResult.substring(0, 10 * 1024);
						}
						textResult = WizMisc.html2Text(textResult);
						textResult = textResult.trim();
						if (textResult.length() > ABSTRACT_STRING_LENGTH) {
							textResult = textResult.substring(0,
									ABSTRACT_STRING_LENGTH);
						}
					} finally {
						if (is != null) {
							is.close();
						}
					}
				} else {
					String ext = FileUtil.extractFileExt(entryName);
					if (ext == null)
						continue;
					ext = ext.toLowerCase();
					if (ext.equals(".jpg") || ext.equals(".png")
							|| ext.equals(".bmp")) {
						if (entry.getSize() > maxFileSize) {
							maxFileSize = entry.getSize();
							entryBitmap = entry;
						}
					}
				}
			}
			//
			if (entryBitmap != null) {
				//
				int bitmapWidth = 0;
				int bitmapHeight = 0;
				//
				{
					InputStream isInfo = null;
					try {
						int [] bitmapSize = new int[2];
						//
						bitmapSize[0] = 0;
						bitmapSize[1] = 0;
						//
						isInfo = zip.getInputStream(entryBitmap);
						//
						ImageUtil.getBitmapSizeFromStream(isInfo, bitmapSize);
						//
						bitmapWidth = bitmapSize[0];
						bitmapHeight = bitmapSize[1];
					}
					finally {
						if (isInfo != null) {
							isInfo.close();
						}
					}
				}
				//
				{
					InputStream isData = null;
					try {
						int thumbSize = getAbstractThumbSize(ctx);
						//
						if (bitmapWidth >= thumbSize || bitmapHeight >= thumbSize) {
							isData = zip.getInputStream(entryBitmap);
							//
							int inSampleSizeX = bitmapWidth / thumbSize;
							int inSampleSizeY = bitmapHeight / thumbSize;
							int inSampleSize = Math.min(inSampleSizeX, inSampleSizeY);
							//
							Bitmap bmpOrg = null;
							try {
						        BitmapFactory.Options opt = new BitmapFactory.Options();  
						        opt.inPreferredConfig = Bitmap.Config.RGB_565;   
						        opt.inPurgeable = true;  
						        opt.inInputShareable = true;
						        opt.inSampleSize = inSampleSize;
						        //
								bmpOrg = BitmapFactory.decodeStream(isData, null, opt); // 忽略内存不足错误
								bmpResult = ImageUtil.resizeAndCutBitmap(bmpOrg,
										thumbSize, thumbSize);
							} catch (OutOfMemoryError err) {
								err.printStackTrace();
								return null;
							} finally {
								if (bmpOrg != null) {
									bmpOrg.recycle();
									bmpOrg = null;
								}
							}
						}
					} finally {
						if (isData != null) {
							isData.close();
						}
					}
				}
			}
		} catch (Exception e) {
		} finally {
			if (zip != null) {
				try {
					zip.close();
				} catch (Exception e) {
				}
			}

		}
		//
		WizAbstractData data = new WizAbstractData();
		data.abstractText = textResult;
		data.abstractImage = bmpResult;
		//
		return data;
	}

	public static void deleteAbstract(Context ctx, String userId,
			String documentGuid) {
		WizAbstractDatabase db = WizAbstractDatabase.getDb(ctx, userId);
		db.deleteAbstracts(documentGuid);
	}
}
