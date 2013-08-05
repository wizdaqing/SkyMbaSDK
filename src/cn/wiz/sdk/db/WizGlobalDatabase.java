package cn.wiz.sdk.db;

import java.io.InputStream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import cn.wiz.sdk.api.WizObject.WizAvatar;
import cn.wiz.sdk.settings.WizAccountSettings;
import cn.wiz.sdk.util.FileUtil;
import cn.wiz.sdk.util.ImageUtil;
/**
 * 注：此类用于获取、存储用户头像  由于初期设计考虑不周。此类中要求传入userGuid时，传userGuid 和 userId都有效
 * @author wiz_chentong
 *
 */
public class WizGlobalDatabase {

	static private Object mLock = new Object();
	static private WizGlobalDatabase mDatabase;

	@SuppressLint("DefaultLocale")
	static public WizGlobalDatabase getDb(Context ctx) {
		synchronized (mLock) {
			if (mDatabase != null)
				return mDatabase;
			//
			mDatabase = new WizGlobalDatabase(ctx);
			//
			return mDatabase;
		}
	}

	//
	@SuppressLint("DefaultLocale")
	public static void closeDb() {
		synchronized (mLock) {
			if (mDatabase == null)
				return;
			//
			try {
				mDatabase.closeDatabase();
			} catch (Exception e) {

			}
		}
	}
	/*--------------------------------------------------Avatar --------------------------------------------------*/
	/**
	 * 有对应的头像则返回，没有返回null
	 * 
	 * @param userGuid  也可以直接传入userId，可视为 userGuid   因为服务器也可以通过userId得到头像。  隐患：没有唯一标识。同一个用户头像有可能存了两份。
	 * @return
	 */
	public WizAvatar getAvatarById(String userGuid){
		WizAvatar avatar = null;
		TempAvatar temp = getAvatarByUserGuid(userGuid);
		byte[] blob = temp.blob;
		Bitmap bitmap = null ;
		try{
			if(blob != null){
				bitmap = ImageUtil.decodeSampledBitmapFromBytes(blob, 33, 33);
				avatar = new WizAvatar(userGuid, bitmap, temp.lastModified);
			}
		}catch(OutOfMemoryError e){
			e.printStackTrace();
		}
		return avatar;
	}
	
	public boolean setAvatar(String guid, InputStream in) {
		byte[] data = ImageUtil.bitmap2ByteArrayNoRecycle(BitmapFactory.decodeStream(in));
		if (checkAvatarExists(guid)){
			return updateAvatar(guid, data);
		}else{
			return addAvatar(guid, data);
		}
	}
	
	/*--------------------------Avatar  DAO--------------------------------*/
	
	private String mTableNameOfAvatar = "WIZ_AVATAR";
	private final String sqlTableAvatar = "CREATE TABLE " + mTableNameOfAvatar + " (\n"
			+ "USER_GUID					char(38)						PRIMARY KEY NOT NULL,\n"
			+ "AVATAR					    blob							NOT NULL,\n"
			+ "LAST_MODIFIED				long							\n"
			+ ")";

	private final String sqlFieldAvatar = "USER_GUID, AVATAR, LAST_MODIFIED";

	private SQLiteDatabase mDB;

	private WizGlobalDatabase(Context ctx) {
		//
		String path = WizAccountSettings.getDataRootPath(ctx);
		path = FileUtil.pathAddBackslash(path);

		String fileName = path + "global.db";

		if (!openDatabase(fileName)) {
		}
	}
	//
	private boolean openDatabase(String fileName) {
		if (mDB == null) {
			try {
				mDB = SQLiteDatabase.openOrCreateDatabase(fileName, null);
			} catch (Exception e) {
				return false;
			}
			if (!checkTable(mTableNameOfAvatar, sqlTableAvatar))
				return false;
		}
		//
		return true;
	}
	//
	private boolean checkTable(String tableName, String tableSql) {
		if (tableExists(tableName))
			return true;
		//
		return execSql(tableSql);
	}
	// 检索表
	private boolean tableExists(String tableName) {
		try {
			boolean exists = false;
			//
			Cursor cursor = null ;
			try {
			cursor = mDB.rawQuery(
					"select count(*) from sqlite_master where type='table' and tbl_name='"
							+ tableName + "'", null);
			
				if (cursor.moveToNext()) {
					int count = cursor.getInt(0);
					exists = (count == 1);
				}
			}catch(Exception e){
				e.printStackTrace();
			}finally {
				if(cursor != null){
					cursor.close();
				}
			}
			//
			return exists;
		} catch (SQLiteException err) {
			return false;
		}
	}
	
	private synchronized boolean execSql(String sql) {
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
	private synchronized boolean execSql(String sql, Object[] sqlData) {
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
	private void closeDatabase() {
		if (mDB != null) {
			mDB.close();
			mDB = null;
		}
	}
	
	private boolean updateAvatar(String guid, byte[] data) {
		String sql = "update " + mTableNameOfAvatar + " set AVATAR" + "=? ,LAST_MODIFIED = " + System.currentTimeMillis() +" where USER_GUID=" + stringToSQLString(guid);
		return execSql(sql, new Object[]{data});
	}
	private boolean addAvatar(String guid, byte[] data) {
		//
		String sql = "insert into " + mTableNameOfAvatar + " (USER_GUID, AVATAR, LAST_MODIFIED) values ("
				+ stringToSQLString(guid) + ", "
				+ "?,"
				+System.currentTimeMillis()
				+");";
		return execSql(sql, new Object[]{data});
	}
	private  String stringToSQLString(String str) {
		if (str == null)
			return "NULL";
		if (str.length() == 0)
			return "NULL";
		//
		str = str.replace("'", "''");
		//
		return "'" + str + "'";
	}
	private TempAvatar getAvatarByUserGuid(String guid) {
		String sql = "select " + sqlFieldAvatar
				+ " from " + mTableNameOfAvatar + " where USER_GUID="
				+ stringToSQLString(guid);
		return sqlToAvatar(sql);
	}
	//
	private class TempAvatar{
		public byte[] blob ;
		public long lastModified;
	}
	private TempAvatar sqlToAvatar(String sql) {
		Cursor cursor = null ;
		TempAvatar avatar = new TempAvatar();
		try {
			cursor = mDB.rawQuery(sql, null);
			//
			if (cursor.moveToNext()) {
				avatar.blob = cursor.getBlob(1);
				avatar.lastModified = cursor.getLong(2);
			}
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			if(cursor != null){
				cursor.close();
			}
		}
		//
		return avatar;
	}
	private boolean checkAvatarExists(String guid) {
		String sql = "select USER_GUID from " + mTableNameOfAvatar + " where USER_GUID=" + stringToSQLString(guid) + " limit 0 , 1";
		Cursor cursor = null;
		try{
			cursor = mDB.rawQuery(sql, null);
			if(cursor.moveToNext()) {
				return true;
			}
			return false;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}finally{
			if(cursor != null){
				cursor.close(); 
			}
		}
	}
}
