package cn.wiz.sdk.db;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import cn.wiz.sdk.api.WizAsyncAction.WizAsyncActionThread;
import cn.wiz.sdk.api.WizDocumentAbstractCache;
import cn.wiz.sdk.api.WizEventsCenter;
import cn.wiz.sdk.api.WizEventsCenter.WizDatabaseObjectType;
import cn.wiz.sdk.api.WizObject;
import cn.wiz.sdk.api.WizObject.WizAttachment;
import cn.wiz.sdk.api.WizObject.WizCert;
import cn.wiz.sdk.api.WizObject.WizDeletedGUID;
import cn.wiz.sdk.api.WizObject.WizDocument;
import cn.wiz.sdk.api.WizObject.WizGroupKb;
import cn.wiz.sdk.api.WizObject.WizKb;
import cn.wiz.sdk.api.WizObject.WizKbVersion;
import cn.wiz.sdk.api.WizObject.WizLocation;
import cn.wiz.sdk.api.WizObject.WizMessage;
import cn.wiz.sdk.api.WizObject.WizObjectBase;
import cn.wiz.sdk.api.WizObject.WizPersonalKb;
import cn.wiz.sdk.api.WizObject.WizTag;
import cn.wiz.sdk.api.WizObject.WizUser;
import cn.wiz.sdk.api.WizObject.WizUserInfo;
import cn.wiz.sdk.api.WizStatusCenter;
import cn.wiz.sdk.api.WizStrings;
import cn.wiz.sdk.api.WizStrings.WizStringId;
import cn.wiz.sdk.api.WizXmlRpcServer.WizKeyValue;
import cn.wiz.sdk.settings.WizAccountSettings;
import cn.wiz.sdk.settings.WizSystemSettings;
import cn.wiz.sdk.util.FileUtil;
import cn.wiz.sdk.util.HTMLUtil;
import cn.wiz.sdk.util.HTMLUtil.WizJSAction;
import cn.wiz.sdk.util.TimeUtil;
import cn.wiz.sdk.util.WizMisc;
import cn.wiz.sdk.util.ZipUtil;

public class WizDatabase {

	final static public String ANONYMOUS_USER_ID = "anonymous@wiz.cn";

	private Context mContext;
	private String mDbFile;
	private String mUserId;
	private String mKbGuid;
	private SQLiteDatabase mDB;

	/*
	 * 构造一个Database对象
	 */
	private WizDatabase(Context ctx, String userId, String kbGuid) {
		mContext = ctx;
		mUserId = userId;
		mKbGuid = kbGuid;
		mDbFile = getDbFileName(ctx, userId, kbGuid);
	}

	/*
	 * 获取KbGuid
	 */
	public String getKbGuid() {
		if (mKbGuid == null)
			return "";
		//
		return mKbGuid;
	}

	/*
	 * 获取用户user id
	 */

	public String getUserId() {
		return mUserId;
	}

	public boolean isAnonymous() {
		return getUserId().equalsIgnoreCase(ANONYMOUS_USER_ID);
	}

	static public boolean isAnonymousUserId(String userId) {
		if (userId == null)
			return false;
		return userId.equalsIgnoreCase(ANONYMOUS_USER_ID);
	}

	/*
	 * 获取数据库文件名
	 */
	static private String getDbFileName(Context ctx, String userId,
			String kbGuid) {
		String path = WizAccountSettings.getRamAccountPath(ctx, userId);
		path = FileUtil.pathAddBackslash(path);
		if (TextUtils.isEmpty(kbGuid)) {
			return path + "index.db";
		} else {
			return path + kbGuid + ".db";
		}
	}

	/*
	 * 打开数据库
	 */
	private void open() {
		if (mDB == null) {
			try {
				mDB = SQLiteDatabase.openOrCreateDatabase(mDbFile, null);
				checkTables();
				setDocumentsModified(true, false);
			} catch (Exception e) {
			}
		}
	}

	/*
	 * 实现数据库单例 每一个kb一个对象
	 */
	static private Object mLock = new Object();
	static ConcurrentHashMap<String, WizDatabase> mDatabaseMap = new ConcurrentHashMap<String, WizDatabase>();

	@SuppressLint("DefaultLocale")
	static public WizDatabase getDb(Context ctx, String userId, String kbGuid) {
		synchronized (mLock) {
			if (kbGuid == null)
				kbGuid = "";
			//
			String key = "/" + userId + "/" + kbGuid;
			key = key.toLowerCase();
			//
			WizDatabase db = mDatabaseMap.get(key);
			if (db != null)
				return db;
			//
			db = new WizDatabase(ctx, userId, kbGuid);
			db.open();
			//
			mDatabaseMap.put(key, db);
			//
			return db;
		}
	}

	// //////////////////////////////////////////////////////////////////////////////
	// 目录公开的操作
	/*
	 * 获取所有的location
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<WizLocation> getAllLocations() {
		//
		refreshLocationsArray();
		//
		synchronized (mLocations) {
			return (ArrayList<WizLocation>) mLocations.clone();
		}
	}

	/*
	 * 判断location是否存在
	 */
	public boolean locationExists(String location) {
		ArrayList<WizLocation> arr = getAllLocations();
		//
		for (WizLocation loc : arr) {
			if (loc.getLocation().equals(location))
				return true;
		}
		//
		return false;
	}

	/*
	 * 获取所有的根目录
	 */
	public ArrayList<WizLocation> getRootLocations() {
		ArrayList<WizLocation> arr = new ArrayList<WizLocation>();
		for (int i = 0; i < 3; i++) {
			arr = getAllLocations();

			if (!WizMisc.isEmptyArray(arr))
				break;

			setLocationsModified(mUserId, true);
		}
		//
		if (arr.isEmpty()) {
			arr.add(new WizLocation("/My Notes/"));
			return arr;
		}
		//
		int count = arr.size();
		for (int i = count - 1; i >= 0; i--) {
			WizLocation location = arr.get(i);
			if (location.getLevel() != 0) {
				arr.remove(i);
			}
		}
		//
		Collections.sort(arr);
		//
		return arr;
	}

	/*
	 * 获取子目录
	 */
	public ArrayList<WizLocation> getChildLocations(String parentLocation) {
		ArrayList<WizLocation> arr = getAllLocations();
		//
		int parentLocationLength = parentLocation.length();
		//
		int count = arr.size();
		for (int i = count - 1; i >= 0; i--) {
			WizLocation location = arr.get(i);
			String s = location.getLocation();
			if (s.startsWith(parentLocation)) {
				if (s.indexOf('/', parentLocationLength) == s.length() - 1) {
					continue;
				}
			}
			//
			arr.remove(i);
		}
		//
		Collections.sort(arr);
		//
		return arr;
	}

	/*
	 * 获取附件数量，通过缓存获得
	 */
	public int getAttachmentsCountFromCache(String documentGuid) {

		Integer ret = this.mDocumentAttachmentsCountMap.get(documentGuid);
		if (ret == null)
			return 0;
		//
		return ret.intValue();
	}
	
	/*
	 * 获取附件数量，通过数据库获得
	 */
	public int getAttachmentsCountFromDb(String documentGuid) {

		String sql = "select count(*) from WIZ_DOCUMENT_ATTACHMENT where DOCUMENT_GUID = "
				+ stringToSQLString(documentGuid);
		return sqlToInt(sql, 0, 0);
	}


	/*
	 * 通过目录获取笔记数量
	 */
	public int getDocumentsCountFromCache(WizLocation loc) {

		DOCUMENT_COUNT_DATA data = this.mFolderDocumentsCountMap.get(loc
				.getLocation());
		if (data == null)
			return 0;
		//
		return data.nSelf;
	}	/*
	 * 通过目录获取笔记数量
	 */
	public int getDocumentsCountFromDb(WizLocation loc) {

		String sql = "select count(*) from WIZ_DOCUMENT where DOCUMENT_LOCATION like "
				+ stringToSQLString(loc.getLocation());
		return sqlToInt(sql, 0, 0);
	}

	/*
	 * 通过目录获取笔记数量
	 */
	public DOCUMENT_COUNT_DATA getDocumentsCount2(WizLocation loc) {

		DOCUMENT_COUNT_DATA data = this.mFolderDocumentsCountMap.get(loc
				.getLocation());
		if (data == null)
			return new DOCUMENT_COUNT_DATA();
		//
		return data;
	}

	/*
	 * 获取父目录
	 */
	public WizLocation getParentLocation(String location) {
		//
		WizLocation curr = new WizLocation(location);
		if (curr.isRoot())
			return null;
		//
		String parentLocation = curr.getParent();
		//
		ArrayList<WizLocation> arr = getAllLocations();
		for (WizLocation loc : arr) {
			if (loc.getLocation().equals(parentLocation))
				return loc;
		}
		//
		return new WizLocation(parentLocation);
	}

	/*
	 * 用于界面创建新的文件夹。创建文件夹之后，将本地数据标记为脏，需要同步数据到服务器
	 */
	public boolean localCreateLocation(String parent, String name) {
		String location = makeLocation(parent, name);
		return localCreateLocation(location);
	}

	public boolean localCreateLocation(String location) {
		WizLocation loc = new WizLocation(location);
		//
		if (addLocation(loc)) {
			invalidateLocalFolders(); // 标记本地文件夹为脏
			return true;
		}
		return false;
	}
	
	public void localDeleteFolderAndDocuments(WizAsyncActionThread thread, String location) {
		//
		boolean includeChildren = true;
		ArrayList<WizDocument> documents = this.getDocumentsByLocation(location, includeChildren);
		//
		for (WizDocument doc : documents) {
			boolean logDeletedGuid = true;
			removeDocument(doc.guid, logDeletedGuid);
			//
			if (thread != null) {
				thread.sendStatusMessage("", 0, 0, doc);
			}
		}
		//
		HashSet<String> ret = new HashSet<String>();
		//
		ArrayList<WizLocation> locations = getAllLocations();
		for (WizLocation loc : locations) {
			if (!loc.getLocation().startsWith(location)) {
				ret.add(loc.getLocation());
			}
		}
		//
		updateAllLocations(ret);
		//
		invalidateLocalFolders();
	}

	/*
	 * 是否包含子目录
	 */
	public boolean locationHasChildren(String location) {
		if (TextUtils.isEmpty(location))
			return false;
		int locationLength = location.length();
		ArrayList<WizLocation> arr = getAllLocations();
		for (WizLocation loc : arr) {
			String s = loc.getLocation();
			if (s.startsWith(location) && s.length() > locationLength)
				return true;
		}
		//
		return false;
	}

	// /////////////////////////////////////////////////////////////
	// 私有操作
	//
	private boolean isValidLocation(String location) {
		if (location == null)
			return false;
		location = location.trim();
		if (location.length() <= 2)
			return false;
		if (location.equals(""))
			return false;
		if (location.equals("/"))
			return false;
		if (location.equals("//"))
			return false;
		if (location.charAt(0) != '/')
			return false;
		if (location.charAt(location.length() - 1) != '/')
			return false;

		return true;

	}
	
	public boolean dropTable(String tableName) {
		try {
			String sql = "DROP TABLE " + stringToSQLString(tableName);
			return execSql(sql);
		} catch (Exception err) {
			err.printStackTrace();
		}
		return false;
	}

	/*
	 * 通过sql获取文件夹数据
	 */
	public ArrayList<WizLocation> sqlToLocations(String sql) {
		ArrayList<WizLocation> arr = new ArrayList<WizLocation>();
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				String location = cursor.getString(0);
				int pos = 0;
				try {
					pos = cursor.getInt(3); // 可能是空的
				} catch (Exception e) {
				}
				//
				if (!isValidLocation(location))
					continue;
				//
				arr.add(new WizLocation(location, pos));
			}
		} catch (Exception err) {
			err.printStackTrace();
		} finally {
			closeCursor(cursor);
		}
		Collections.sort(arr);

		return arr;
	}

	/*
	 * 通过WIZ_LOCATION获取一个 <String(location), WizLocation> map。
	 * key就是文件夹路径。注意，这里可能是不完整的数据
	 */
	private HashMap<String, WizLocation> getAllLocationsMapFromLocationTable() {
		String sql = "select " + sqlFieldListLocation + " from "
				+ mTableNameOfLocation
				+ " order by LOCATION_POS, DOCUMENT_LOCATION";
		ArrayList<WizLocation> arr = sqlToLocations(sql);
		//
		HashMap<String, WizLocation> ret = new HashMap<String, WizLocation>();
		for (WizLocation location : arr) {
			ret.put(location.getLocation(), location);
		}
		//
		return ret;
	}

	/*
	 * 重构Location数据。保证每一个可能的文件夹都存在 。 例如如果数据库里面仅包含了 /a/b/c/这样的一个路径，
	 * 那么需要保证返回的数据里面，包含 /a/, /a/b/, /a/b/c/ 这三个对象。
	 */
	private void rebuildLocationsData(HashMap<String, WizLocation> map) {
		//
		// 需要先把values复制一份，否则不能修改map
		ArrayList<WizLocation> old = new ArrayList<WizLocation>();
		//
		Collection<WizLocation> values = map.values();
		for (WizLocation location : values) {
			old.add(location);
		}
		//
		for (WizLocation location : old) {
			while (!location.isRoot()) {
				String parent = location.getParent();
				//
				if (!isValidLocation(parent))
					break;
				//
				if (!map.containsKey(parent)) {
					map.put(parent, new WizLocation(parent));
				}
				//
				location = new WizLocation(parent);
			}
		}
	}

	//
	/*
	 * 用于缓存全部的文件夹对象。 注意：这个对象包含了所有的文件夹数据 该数据仅限于内部使用
	 */
	private ArrayList<WizLocation> mLocations = new ArrayList<WizLocation>();

	/*
	 * 文件夹是否需要刷新 只有私人笔记有这个数据，不需要区分kb
	 */
	private static void setLocationsModified(String userId, boolean b) {
		WizStatusCenter.setBool(userId, "Locations", b);
	}

	/*
	 * 返回文件夹是否需要刷新
	 */
	private static boolean isLocationsModified(String userId) {
		return WizStatusCenter.getBool(userId, "Locations", true); // 默认是脏的
	}

	/*
	 * 刷新文件夹数据 如果文件夹数据没有改变，那么就不需要刷新 否则会进行刷新。
	 * 在这里会从两个数据源获取数据，WIZ_LOCATION表里面，以及WIZ_DOCUMENT这个表里面 并且会把数据进行整合以及重构，保证数据完整
	 */
	private void refreshLocationsArray() {
		//
		if (!isLocationsModified(mUserId))
			return;
		setLocationsModified(mUserId, false);
		//
		HashMap<String, WizLocation> all = getAllLocationsMapFromLocationTable();
		HashSet<String> documentLocations = getAllDocumentLocationsSet();
		//
		for (String s : documentLocations) {
			if (!isValidLocation(s))
				continue;
			//
			if (!all.containsKey(s)) {
				all.put(s, new WizLocation(s));
			}
		}
		//
		rebuildLocationsData(all);
		//
		synchronized (mLocations) {

			mLocations.clear();
			//
			for (WizLocation loc : all.values()) {
				mLocations.add(loc);
			}
			//
			for (int i = mLocations.size() - 1; i >= 0; i--) {
				if (mLocations.get(i).getLocation()
						.startsWith("/Deleted Items/")) {
					mLocations.remove(i);
				}
			}
			//
			Collections.sort(mLocations);
			//
			if (mLocations.isEmpty()) {
				mLocations.add(new WizLocation("/My Notes/"));
			}
		}
	}

	public static String selectNoteCondition() {
		return " DOCUMENT_LOCATION !='/Deleted Items/' ";
	}

	/*
	 * 从wiz_document获取所有的文件夹数据
	 */
	private HashSet<String> getAllDocumentLocationsSet() {
		String sql = "select distinct DOCUMENT_LOCATION from "
				+ mTableNameOfDocument + " where "
				+ selectNoteCondition() + " group by DOCUMENT_LOCATION";
		return sqlToStringSet(sql, 0);
	}

	/*
	 * 返回所有的文件夹路径，可以用于更新服务器数据
	 */
	private ArrayList<String> getAllLocationsString() {
		//
		ArrayList<String> arr = new ArrayList<String>();
		for (WizLocation loc : getAllLocations()) {
			arr.add(loc.getLocation());
		}
		return arr;
	}

	/*
	 * 通过服务器数据更新本地文件夹数据到wiz_location这个表 保存服务器的文件夹版本 使用事物进行更新
	 * 更新后需要设置文件夹数据为脏，同时通知界面更新
	 */

	private void setKeyValueOfFolders(WizKeyValue data,
			boolean saveServerVersion) {
		//
		try {
			mDB.beginTransaction();
			updateServerFolders(data.value); // 更新服务器数据
			if (saveServerVersion) {
				setKeyValueVersion("folders", data.version); // 保存版本号
			}
			mDB.setTransactionSuccessful();
		} finally {
			mDB.endTransaction();
		}
		//
		setLocationsModified(mUserId, true); // 设置数据为脏，需要刷新
		WizEventsCenter.sendDatabaseRefreshObject(this,
				WizDatabaseObjectType.Folder); // 通知更新界面
	}

	/*
	 * 保存服务器文件夹数据到本地 同时还会从wiz_document这个表里面获取所有数据，然后合并 合并后正规化所有的数据
	 * 保证包含所有的文件夹。将文件夹正规化，例如 /a/b/c/变成 /a/, /a/b/, /a/b/c/
	 */
	private void updateServerFolders(String folders) {
		if (TextUtils.isEmpty(folders))
			folders = "";
		String[] folderArray = folders.split("\\*");
		if (folderArray == null)
			folderArray = new String[0];
		//
		HashSet<String> locations = getAllDocumentLocationsSet();
		//
		for (String s : folderArray) {
			locations.add(s);
		}
		//
		HashSet<String> all = new HashSet<String>();
		for (String s : locations) {
			//
			if (s.equals(""))
				continue;
			//
			all.add(s);
			//
			WizLocation location = new WizLocation(s);
			while (!location.isRoot()) {
				String parent = location.getParent();
				all.add(parent);
				location = new WizLocation(parent);
			}
		}
		//
		updateAllLocations(all);
	}
	//
	private void updateAllLocations(HashSet<String> locations) {
		//
		//注意：更新文件夹之后，需要记住原来的排序结果
		//在这里之前出现了bug，进行解决
		//
		HashMap<String, WizLocation> oldLocations = getAllLocationsMapFromLocationTable();
		//
		clearTable(mTableNameOfLocation); // 删除全部数据，然后全部保存
		//
		for (String location : locations) {
			WizLocation loc = new WizLocation(location);
			//
			WizLocation oldLocation = oldLocations.get(loc.getLocation());
			if (oldLocation != null) {
				loc.pos = oldLocation.pos;
			}
			//
			addLocation(loc);
		}
	}

	/*
	 * 设置文件夹顺序数据，然后通知数据更新，界面更新
	 */
	private void setKeyValueOfFolderPos(WizKeyValue data) {
		try {
			mDB.beginTransaction();
			updateServerFolderPos(data.value);
			setKeyValueVersion("folders_pos", data.version);
			//
			setLocationsModified(mUserId, true); // 设置数据为脏，需要刷新
			WizEventsCenter.sendDatabaseRefreshObject(this,
					WizDatabaseObjectType.Folder); // 通知更新界面

			mDB.setTransactionSuccessful();
		} finally {
			mDB.endTransaction();
		}
	}

	/*
	 * 保存服务器的文件夹顺序
	 */
	private void updateServerFolderPos(String folderPos) {
		String str = folderPos;
		//
		str = str.trim();
		str = WizMisc.trimString(str, '{');
		str = WizMisc.trimString(str, '}');
		if (str.length() == 0)
			return;
		//
		str = str.replaceAll("\\n", "");
		str = str.replaceAll("\\r", "");
		//
		String[] arrPos = str.split("\\,");
		//
		for (String line : arrPos) {
			//
			String[] arrData = line.split("\\:");
			if (arrData == null)
				continue;
			//
			if (arrData.length != 2)
				continue;
			//
			String location = arrData[0];
			String pos = arrData[1];
			//
			location = location.trim();
			location = WizMisc.trimString(location, '"');
			location = WizMisc.trimString(location, '\'');
			//
			pos = pos.trim();
			//
			int nPos = Integer.parseInt(pos);
			if (0 == nPos)
				continue;
			//
			setLocationPos(location, nPos);
		}
		//
	}

	/*
	 * 正规化一个文件夹，去掉特殊字符
	 */
	static public String makeSingleLocation(String text) {
		text = HTMLUtil.removeSpecialChar(text);
		//
		return makeMultiLocation(text);
	}

	/*
	 * 正规化一个文件夹，在前后增加斜杠
	 */
	static public String makeMultiLocation(String text) {
		if (text == null) {
			return "";
		}
		if (text.equals(""))
			return "";
		if (text.equals("/"))
			return "";
		//
		text = FileUtil.pathAddBackslash(text);
		if (text.charAt(0) != '/')
			text = "/" + text;
		//
		text = text.replaceAll("//", "/");
		return text;
	}

	/*
	 * 生成一个合法的路径，通过父文件夹以及子文件夹名称，可以用于创建一个文件夹
	 */
	static public String makeLocation(String parent, String name) {
		//
		parent = makeMultiLocation(parent);
		//
		name = makeSingleLocation(name);
		//
		if (name == null)
			return parent;
		if (name.equals(""))
			return parent;
		//
		String ret = parent + name;
		ret = ret.replaceAll("//", "/");
		return ret;
	}

	/*
	 * 本地修改了文件夹数据，需要更新以及通知界面更新
	 */
	void invalidateLocalFolders() {
		setKeyValueVersion("folders", -1);
		setLocationsModified(mUserId, true); // 设置本地数据需要刷新
		WizEventsCenter.sendDatabaseRefreshObject(this,
				WizDatabaseObjectType.Folder); // 本地更新了数据，更新界面
	}

	/*
	 * 向服务器添加一个文件夹
	 */
	private boolean addLocation(WizLocation loc) {
		String sql = "insert into " + mTableNameOfLocation + " ("
				+ sqlFieldListLocation + ") values ("
				+ stringToSQLString(loc.getLocation()) + " , "
				+ stringToSQLString(loc.getParent()) + " , 0, " + loc.pos + ")";
		return execSql(sql);
	}

	/*
	 * 设置文件夹的顺序
	 */
	private boolean setLocationPos(String location, int pos) {
		String sql = "update " + mTableNameOfLocation + " set LOCATION_POS="
				+ pos + " where DOCUMENT_LOCATION="
				+ stringToSQLString(location);
		//
		return execSql(sql);
	}

	// ///////////////////////////////////////////////////////////////////////////
	// 标签操作

	/*
	 * 获取所有标签
	 */
	public ArrayList<WizTag> getAllTags() {
		String sql = sqlOfSelectTag() + sqlOfOrderByModifDesc();
		return sqlToTags(sql);
	}

	/*
	 * 获取根标签
	 */
	public ArrayList<WizTag> getRootTags(boolean includeUngrouped) {
		String sql = sqlOfSelectTag() + " WHERE TAG_PARENT_GUID is null or TAG_PARENT_GUID = ''" + " order by TAG_NAME";

		ArrayList<WizTag> tags =  sqlToTags(sql);
		if (!TextUtils.isEmpty(getKbGuid()) && includeUngrouped) {
			tags.add(0, getUngrouped());
		}
		return tags;
	}

	/*
	 * 获取根标签
	 */
	public ArrayList<WizTag> getRootTags() {
		return getRootTags(false);
	}

	/*
	 * 获取子标签
	 */
	public ArrayList<WizTag> getChildTags(String parentTagGuid) {
		if (TextUtils.isEmpty(parentTagGuid)) {
			return getRootTags();
		} else {
			String sql = sqlOfSelectTag() + " WHERE TAG_PARENT_GUID = " + stringToSQLString(parentTagGuid) + " order by TAG_NAME";

			return sqlToTags(sql);
		}
	}
	//
	/*
	 * 获取一个标签的所有子标签，限制标签深度，避免出现标签循环的错误
	 */
	private ArrayList<WizTag> getAllChildTags(String parentTagGuid, int level) {
		//
		if (level > 10)
			return new ArrayList<WizTag>();
		//
		ArrayList<WizTag> tagsAll = new ArrayList<WizTag>();
		//
		ArrayList<WizTag> children = getChildTags(parentTagGuid);
		tagsAll.addAll(children);
		//
		for (WizTag tag : children) {
			tagsAll.addAll(getAllChildTags(tag.guid, level + 1));
		}
		//
		return tagsAll;
	}
	/*
	 * 获取一个标签的所有子标签
	 */
	public ArrayList<WizTag> getAllChildTags(String parentTagGuid) {
		return getAllChildTags(parentTagGuid, 0);
	}
	/*
	 * 获取一个标签的所有子标签
	 */
	public ArrayList<String> getAllChildTagsName(String parentTagGuid) {
		ArrayList<WizTag> tags = getAllChildTags(parentTagGuid, 0);
		ArrayList<String> names = new ArrayList<String>();
		//
		for (WizTag tag : tags) {
			names.add(tag.name);
		}
		//
		return names;
	}

	/*
	 * 获取标签对应的笔记的数量
	 */
	private int getDocumentsCountCore(WizTag tag) {

		String sql = "select count(*) from WIZ_DOCUMENT where DOCUMENT_LOCATION not like '/Deleted Items/%' and DOCUMENT_TAG_GUIDS like '%"
				+ tag.guid + "%'";
		//
		return sqlToInt(sql, 0, 0);
	}

	/*
	 * 获取标签对应的笔记的数量，从cache里面获取，可能更新比较慢
	 */
	public int getDocumentsCountFromCache(WizTag tag) {
		Integer val = this.mTagDocumentsCountMap.get(tag.guid);
		if (val == null)
			return 0;
		//
		return val.intValue();
	}

	/*
	 * 获取标签对应笔记的数量范围，从cache里面获取
	 */
	public WizRange getTagsDocumentsCountRange() {
		return this.mTagDocumentsCountRange;
	}

	/*
	 * 获取标签对应的笔记的数量，从数据库里面获取，速度慢，但是更新快
	 */
	public int getDocumentsCountFromDb(WizTag tag) {
		return getDocumentsCountCore(tag);
	}

	/*
	 * 是否包含子标签
	 */

	public boolean tagHasChildren(String tagGuid) {
		if (TextUtils.isEmpty(tagGuid))
			return false;

		String sql = sqlOfSelectTag() + " WHERE TAG_PARENT_GUID = " + stringToSQLString(tagGuid) + " limit 0, 1";
		return hasRecord(sql);
	}

	/*
	 * 通过标签删除Tag
	 */
	public void deleteTag(String tagGuid) {
		removeTag(tagGuid, true);
	}

	/*
	 * 获取父标签
	 */
	public WizTag getParentTag(WizTag tag) {
		if (tag.parentGuid == null || tag.parentGuid.equals(""))
			return null;
		//
		return getTagByGuid(tag.parentGuid);
	}

	/*
	 * 是否是跟标签
	 */
	public boolean isRootTag(WizTag tag) {
		if (tag.parentGuid == null || tag.parentGuid.equals(""))
			return true;
		//
		return false;
	}

	/*
	 * 获取标签的级别
	 */
	public int getTagLevel(WizTag tag) {
		int level = 0;
		while (tag != null && !isRootTag(tag)) {
			tag = getParentTag(tag);
			level++;
		}
		//
		return level;
	}

	/*
	 * 从数据库删除一个标签，需要设置是否要添加到删除记录
	 */
	private boolean removeTag(String guid, boolean logDelGuid) {
		//
		String sql = "delete from " + mTableNameOfTag + " where TAG_GUID="
				+ stringToSQLString(guid);
		if (execSql(sql)) {
			if (logDelGuid) {
				logDeletedGUID(guid, WizObject.DATA_TYPE_TAG);
			}
			//
			WizEventsCenter.sendDatabaseRefreshObject(this,
					WizDatabaseObjectType.Tag);
			//
			return true;
		}
		//
		return false;
	}

	/*
	 * 通过标签获得笔记对象
	 */
	public ArrayList<WizDocument> getDocumentsByTag(String guid) {
		return getDocumentsByTag(guid, false);
	}

	/*
	 * 通过标签获得笔记对象，可以选择是否包含子标签
	 */
	public ArrayList<WizDocument> getDocumentsByTag(String guid, boolean includeChildren) {
		String sql = sqlOfSelectDocument(true);
		//
		String sqlTags = "";
		if (TextUtils.isEmpty(guid)) {
			sqlTags = " DOCUMENT_TAG_GUIDS = '' OR DOCUMENT_TAG_GUIDS = " + stringToSQLString("null") + " ";
		} else {
			HashSet<String> tagSet = new HashSet<String>();
			//
			tagSet.add(guid);
			if (includeChildren) {
				ArrayList<WizTag> tags = getAllChildTags(guid);
				//
				for (WizTag tag : tags) {
					tagSet.add(tag.guid);
				}
			}
			//
			ArrayList<String> arrTagSql = new ArrayList<String>();
			for (String tagGuid : tagSet) {
				arrTagSql.add(" DOCUMENT_TAG_GUIDS like " + stringToSQLString("%" + tagGuid + "%") + " ");
			}
			//
			sqlTags = WizMisc.stringArray2Strings(arrTagSql, " OR ");
		}
		//
		sql = sql + sqlTags + sqlOfOrderByModifDesc();
		//
		return sqlToDocuments(sql);
	}

	/*
	 * 通过标签获得未下载的笔记对象，可以选择是否包含子标签
	 */
	public ArrayList<WizDocument> getUnDownloadDocumentsByTag(String guid, boolean includeChildren) {
		//
		HashSet<String> tagSet = new HashSet<String>();
		//
		tagSet.add(guid);
		//
		if (includeChildren) {
			ArrayList<WizTag> tags = getAllChildTags(guid);
			//
			for (WizTag tag : tags) {
				tagSet.add(tag.guid);
			}
		}
		//
		ArrayList<String> arrTagSql = new ArrayList<String>();
		for (String tagGuid : tagSet) {
			arrTagSql.add("DOCUMENT_TAG_GUIDS like " + stringToSQLString("%" + tagGuid + "%"));
		}
		//
		String sqlTags = WizMisc.stringArray2Strings(arrTagSql, " OR ");
		//
		String sql = sqlOfSelectDocument(true) + " SERVER_CHANGED=1 AND ("
				+ sqlTags + ") " + sqlOfOrderByModifDesc();
		//
		return sqlToDocuments(sql);
	}

	/*
	 * 通过标签获取笔记列表
	 */
	public ArrayList<WizDocument> searchDocumentsByTags(ArrayList<String> tags) {
		if (WizMisc.isEmptyArray(tags))
			return null;

		String sql = sqlOfSelectDocument(false);
		for (int i = 0; i < tags.size(); i++)
			sql += " and DOCUMENT_TAG_GUIDS like "
					+ stringToSQLString("%" + tags.get(i) + "%");

		sql = sql + sqlOfOrderByModifDesc();
		return sqlToDocuments(sql);
	}

	/*
	 * 获取本地修改的标签，用于上穿
	 */

	public ArrayList<WizTag> getModifiedTags() {
		String sql = sqlOfSelectTag() + " where LOCAL_CHANGED=1 "
				+ sqlOfOrderByModifDesc();
		return sqlToTags(sql);
	}

	/*
	 * 本地修改的标签被上传了，需要设置版本号
	 */
	public void onUploadedTags(ArrayList<WizTag> tags) {
		for (WizTag tag : tags) {
			setTagLocalModified(tag.guid, false);
		}
	}

	/*
	 * 通过guid获得标签对象，否则返回空
	 */

	public WizTag getTagByGuid(String tagGuid) {
		String sql = sqlOfSelectTag() + " WHERE TAG_GUID = " + stringToSQLString(tagGuid);
		ArrayList<WizTag> arr = sqlToTags(sql);
		if (!WizMisc.isEmptyArray(arr))
			return arr.get(0);

		if (!TextUtils.isEmpty(getKbGuid())) {
			return getUngrouped();
		}

		return null;
	}

	private WizTag getUngrouped(){
		return new WizTag(WizStrings.getString(WizStringId.TAG_NAME_OF_KB_ROOT), "");
	}

	public void deleteTagAndChildren(WizAsyncActionThread thread, String guid){
		ArrayList<WizTag> array = getAllChildTags(guid);
		array.add(0, getTagByGuid(guid));
		int length = array.size();
		for (int i = length-1; i >= 0; i--) {
			WizTag currentTag = array.get(i);
			//
			if (thread !=null) {
				thread.sendStatusMessage("", 0, 0, currentTag);
			}

			deleteTagByGuid(currentTag.guid);
		}
	}

	//根据标签的ID删除标签并修改相应笔记信息
	public void deleteTagByGuid(String guid){
		ArrayList<WizDocument> array = getDocumentsByTag(guid);
		//修改改标签下的所有笔记的标签信息
		for (WizDocument document : array) {
			//更新笔记的更新标识
			if (document.localChanged == WizDocument.DOCUMENT_LOCAL_CHANGE_NULL){
				document.localChanged = WizDocument.DOCUMENT_LOCAL_CHANGE_INFO;
			}

			//更新笔记的标签信息
			String tagsGuid = document.tagGUIDs;
			HashSet<String> tagGuidSet = WizMisc.string2HashSet(tagsGuid, '*');
			tagGuidSet.remove(guid);
			tagsGuid = WizMisc.hashSet2String(tagGuidSet, '*');
			document.tagGUIDs = tagsGuid;
			//修改笔记数据记录
			saveLocalDocument(document, true);
		}
		//删除该标签
		deleteTag(guid);
	}

	/*
	 * 设置标签是否被本地修改
	 */
	private boolean setTagLocalModified(String tagGuid, boolean localModified) {
		int localChanged = localModified ? 1 : 0;
		//
		String sql = "update WIZ_TAG set LOCAL_CHANGED = " + localChanged
				+ " where TAG_GUID='" + tagGuid + "'";
		//
		return execSql(sql);
	}

	/*
	 * 通过标签名字以及父标签，来获取对应的标签
	 */
	public WizTag getTagByName(String tagName, String parentGuid) {
		String sql = sqlOfSelectTag() + " WHERE TAG_NAME = "
				+ stringToSQLString(tagName);
		if (!TextUtils.isEmpty(parentGuid))
			sql = sql + " AND TAG_PARENT_GUID = "
					+ stringToSQLString(parentGuid);

		ArrayList<WizTag> arr = sqlToTags(sql);
		if (!WizMisc.isEmptyArray(arr))
			return arr.get(0);

		return null;
	}

	/*
	 * 通过标签名字获取标签
	 */
	public WizTag getTagByName(String tagName) {
		return getTagByName(tagName, null);
	}

	/*
	 * 通过标签名字在指定的标签下面查找子标签
	 */

	public boolean tagExistsByName(String tagName, String parentGuid) {
		String sql = sqlOfSelectTag() + " WHERE TAG_NAME = "
				+ stringToSQLString(tagName);
		if (!TextUtils.isEmpty(parentGuid))
			sql = sql + " AND TAG_PARENT_GUID = "
					+ stringToSQLString(parentGuid);
		return hasRecord(sql);
	}

	/*
	 * 在所有的标签中按照名字查找指定的标签
	 */
	public boolean tagExistsByName(String tagName) {
		return tagExistsByName(tagName, null);
	}

	/*
	 * 判断标签是否存在
	 */
	public boolean tagExists(String tagGUID) {
		String sql = sqlOfSelectTag() + " where TAG_GUID = "
				+ stringToSQLString(tagGUID);
		return hasRecord(sql);
	}

	/*
	 * 保存服务器下载的标签
	 */
	public void saveServerTags(ArrayList<WizTag> arr) {

		int length = WizMisc.size(arr);
		for (int i = 0; i < length; i++) {
			if (!saveTag(arr.get(i), false))
				continue;
		}
		//
		WizEventsCenter.sendDatabaseRefreshObject(this,
				WizDatabaseObjectType.Tag);
	}

	/*
	 * 保存本地创建/修改的标签
	 */
	public void saveLocalTags(ArrayList<WizTag> arr) {

		int length = WizMisc.size(arr);
		for (int i = 0; i < length; i++) {
			if (!saveTag(arr.get(i), true))
				continue;
		}
		//
		WizEventsCenter.sendDatabaseRefreshObject(this,
				WizDatabaseObjectType.Tag);
	}

	/*
	 * 创建一个新的标签
	 */
	public void createTag(WizTag tag, boolean notify) {

		if (saveTag(tag, true))
			//
			if (notify) {
				WizEventsCenter.sendDatabaseRefreshObject(this,
						WizDatabaseObjectType.Tag);
			}
	}

	/*
	 * 本地修改一个标签，需要通知界面更新
	 */
	public void modifyTag(WizTag tag, boolean notify) {

		if (tag == null || TextUtils.isEmpty(tag.guid))
			return;

		if (saveTag(tag, true))
			//
			if (notify) {
				WizEventsCenter.sendDatabaseRefreshObject(this,
						WizDatabaseObjectType.Tag);
			}
	}

	/*
	 * 保存标签到数据库
	 */

	private boolean saveTag(WizTag tag, boolean localModified) {

		int localChanged = localModified ? 1 : 0;
		if (tagExists(tag.guid)) {
			String sql = "update " + mTableNameOfTag + " set TAG_NAME="
					+ stringToSQLString(tag.name) + ", TAG_DESCRIPTION="
					+ stringToSQLString(tag.description) + ", TAG_PARENT_GUID="
					+ stringToSQLString(tag.parentGuid) + ", LOCAL_CHANGED="
					+ intToSQLString(localChanged) + ", DT_MODIFIED="
					+ stringToSQLString(tag.dateModified) + " where TAG_GUID="
					+ stringToSQLString(tag.guid);
			return execSql(sql);
		} else {
			String sql = "insert into " + mTableNameOfTag + " ("
					+ sqlFieldListTag + ") values ("
					+ stringToSQLString(tag.guid) + ", "
					+ stringToSQLString(tag.parentGuid) + ", "
					+ stringToSQLString(tag.name) + ", "
					+ stringToSQLString(tag.description) + ", "
					+ intToSQLString(localChanged) + ", "
					+ stringToSQLString(tag.dateModified) + ")";
			return execSql(sql);
		}
	}

	/*
	 * 通过sql获取标签
	 */
	public ArrayList<WizTag> sqlToTags(String sql) {
		ArrayList<WizTag> arr = new ArrayList<WizTag>();
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				WizTag data = new WizTag();
				//
				data.guid = cursor.getString(0);
				data.parentGuid = cursor.getString(1);
				data.name = cursor.getString(2);
				data.description = cursor.getString(3);
				data.dateModified = cursor.getString(5);
				data.version = -1;
				arr.add(data);
			}
		} catch (Exception err) {
			err.printStackTrace();
		} finally {
			closeCursor(cursor);
		}
		Collections.sort(arr);

		return arr;
	}

	// /////////////////////////////////////////////////////////////////////////////////////
	// deleted guids operations
	//

	/*
	 * 保存服务器deleted guid，实际上不进行保存，而是删除本地数据
	 */
	public void saveServerDeletedGUIDs(ArrayList<WizDeletedGUID> dels) {
		if (dels.size() == 0)
			return;
		//
		boolean documents = false;
		boolean tags = false;
		//
		//
		mDB.beginTransaction();
		for (WizDeletedGUID del : dels) {
			if (del.type.equals(WizObject.DATA_TYPE_DOCUMENT)) {
				removeDocument(del.guid, false);
				documents = true;
			} else if (del.type.equals(WizObject.DATA_TYPE_TAG)) {
				removeTag(del.guid, false);
				tags = true;
			} else if (del.type.equals(WizObject.DATA_TYPE_ATTACHMENT)) {
				removeAttachment(del.guid, false);
			}
		}
		//
		mDB.setTransactionSuccessful();
		mDB.endTransaction();
		//
		if (documents) {
			setDocumentsModified(true, true);
		}
		if (tags) {
			WizEventsCenter.sendDatabaseRefreshObject(this,
					WizDatabaseObjectType.Tag);
		}
	}

	/*
	 * 获取所有的删除记录
	 */

	public ArrayList<WizDeletedGUID> getModifiedDeletedGUIDs() {
		String sql = sqlOfSelectDeletedGUID();
		return sqlToDeletedGUIDs(sql);
	}

	/*
	 * 已经上传了某些删除记录，需要从数据库里面删除
	 */
	public void onUploadedDeletedGUIDs(ArrayList<WizDeletedGUID> subArr) {
		removeDeletedGUIDs(subArr);
	}

	/*
	 * 通过sql查询删除记录
	 */
	public ArrayList<WizDeletedGUID> sqlToDeletedGUIDs(String sql) {
		ArrayList<WizDeletedGUID> ret = new ArrayList<WizDeletedGUID>();
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				WizDeletedGUID data = new WizDeletedGUID();
				data.guid = cursor.getString(0);
				data.type = cursor.getString(1);
				data.dateDeleted = cursor.getString(2);
				//
				ret.add(data);
			}
			return ret;
		} catch (Exception err) {
			err.printStackTrace();
		} finally {
			closeCursor(cursor);
		}
		return null;
	}

	/*
	 * 记录一个删除操作，添加到到已删除数据里面
	 */
	private boolean logDeletedGUID(String guid, String type) {
		String dtDeleted = TimeUtil.getCurrentSQLDateTimeString();
		String sql = "insert into " + mTableNameOfDeleted + " ("
				+ sqlFieldListDeletedGUID + ") values ("
				+ stringToSQLString(guid) + ", " + stringToSQLString(type)
				+ ", " + stringToSQLString(dtDeleted) + ")";
		//
		return execSql(sql);
	}

	/*
	 * 删除一条已删除记录
	 */

	private boolean removeDeletedGUID(WizDeletedGUID del) {
		String sql = sqlOfDeleteRecord(mTableNameOfDeleted) + " DELETED_GUID="
				+ stringToSQLString(del.guid);
		return execSql(sql);
	}

	/*
	 * 删除多条已删除记录
	 */
	private boolean removeDeletedGUIDs(ArrayList<WizDeletedGUID> dels) {
		boolean success = true;
		for (WizDeletedGUID del : dels) {
			success = success && removeDeletedGUID(del);
		}
		return success;
	}

	// ///////////////////////////////////////////////////////////////////////
	// attachment opeations

	/*
	 * 删除附件
	 */
	private boolean removeAttachment(String guid, boolean logDelGuid) {
		if (TextUtils.isEmpty(guid))
			return true;

		WizAttachment att = getAttachmentByGuid(guid);
		if (att == null)
			return true;

		if (logDelGuid)
			logDeletedGUID(guid, WizObject.DATA_TYPE_ATTACHMENT);
		String sql = "delete from " + mTableNameOfAttachment
				+ " where ATTACHMENT_GUID=" + stringToSQLString(guid);
		if (execSql(sql)) {
			String attFile = att.getZipFileName(mContext, mUserId);
			FileUtil.deleteFile(attFile);
			return true;
		}
		return false;

	}

	/*
	 * 删除附件
	 */

	public boolean deleteAttachment(WizAttachment att) {
		if (att == null)
			return true;

		if (removeAttachment(att.guid, true)) {
			return updateDocumentAttachmentCount(att.docGuid, true);
		} else {
			return false;
		}
	}

	//
	// public boolean saveAttachments(ArrayList<WizAttachment> atts)
	// throws Exception {
	//
	// boolean success = true;
	// if (WizMisc.isEmptyArray(atts))
	// return success;
	//
	// for (WizAttachment att : atts) {
	// success = success && saveAttachment(att);
	// }
	// return success;
	// }

	public boolean saveAttachment(WizAttachment att, String fileName) throws Exception {
		if (att == null)
			return false;

		String attGuid = att.guid;
		if (TextUtils.isEmpty(attGuid))
			return false;

		File zipFile = att.getZipFile(mContext, mUserId);

		ArrayList<String> files = new ArrayList<String>();
		files.add(fileName);
		ZipUtil.zipByApache(files, zipFile);
		saveAttachment(att, true);
		updateDocumentAttachmentCount(att.docGuid, true);
		//
		return true;
	}

	public boolean shareAttachment2Wiz(WizAttachment srcAtt, String destDocumentGuid, String destGuid, String destKbGuid) throws Exception {
		if (srcAtt == null || TextUtils.isEmpty(destGuid) || TextUtils.isEmpty(destDocumentGuid))
			return false;

		WizDatabase destDb = getDb(mContext, mUserId, destKbGuid);
		return shareAttachment2Wiz(srcAtt, destDocumentGuid, destGuid, destDb);
	}

	public boolean shareAttachment2Wiz(WizAttachment srcAtt, String destDocumentGuid, String destGuid, WizDatabase destDb) throws Exception {
		if (srcAtt == null || TextUtils.isEmpty(destGuid) || TextUtils.isEmpty(destDocumentGuid))
			return false;

		WizAttachment destAtt = new WizAttachment();
		destAtt.guid = destGuid;
		destAtt.docGuid = destDocumentGuid;
		destAtt.localChanged = WizAttachment.ATTACHMENT_LOCAL_CHANGE_DATA;
		destAtt.name = srcAtt.name;
		destAtt.location = srcAtt.location;
		destAtt.serverChanged = 0;
		destAtt.version = -1;
		destAtt.dataMd5 = "";
		destAtt.dateModified = TimeUtil.getCurrentSQLDateTimeString();
		destAtt.description = "";

		return shareAttachment2Wiz(srcAtt, destAtt, destDb);
	}

	public boolean shareAttachment2Wiz(WizAttachment strAtt, WizAttachment destAtt, WizDatabase destDb) throws Exception {
		if (strAtt == null || destAtt == null || destDb == null)
			return false;

		File srcFile = strAtt.getZipFile(mContext, mUserId);
		File destFile = destAtt.getZipFile(mContext, mUserId);

		FileUtil.copyFile(srcFile, destFile);

		return destDb.saveAttachment(destAtt, true);
	}

	/*
	 * 更新某一篇笔记附件数量
	 */
	private boolean updateDocumentAttachmentCount(String documentGuid, boolean updateCache) {
		WizDocument doc = getDocumentByGuid(documentGuid);
		if (doc == null)
			return true;

		int attachmentCount = getDocumentAttachmentCount(documentGuid);

		int oldLocalChanged = getDocumentLocalChanged(documentGuid);
		int localChanged = WizDocument.DOCUMENT_LOCAL_CHANGE_INFO;
		if (oldLocalChanged == WizDocument.DOCUMENT_LOCAL_CHANGE_DATA) {
			localChanged = WizDocument.DOCUMENT_LOCAL_CHANGE_DATA;
		}

		String sql = "update " + mTableNameOfDocument
				+ " set ATTACHMENT_COUNT = " + intToSQLString(attachmentCount)
				+ " , LOCAL_CHANGED=" + intToSQLString(localChanged)
				+ " where DOCUMENT_GUID=" + stringToSQLString(documentGuid);
		boolean ret = execSql(sql);
		//
		if (updateCache) {
			boolean modified = true;
			boolean notifyMessage = false;
			setDocumentsModified(modified, notifyMessage);
		}
		
		//
		return ret;
	}
	

	private boolean correctDocumentAttachmentCount(String documentGuid) {
		WizDocument doc = getDocumentByGuid(documentGuid);
		if (doc == null)
			return true;

		int attachmentCount = getDocumentAttachmentCount(documentGuid);

		String sql = "update " + mTableNameOfDocument
				+ " set ATTACHMENT_COUNT = " + intToSQLString(attachmentCount)
				+ " where DOCUMENT_GUID=" + stringToSQLString(documentGuid);
		boolean ret = execSql(sql);
		//
		boolean modified = true;
		boolean notifyMessage = false;
		setDocumentsModified(modified, notifyMessage);
		//
		return ret;
	}

	/*
	 * 获取笔记的附件数量
	 */

	public int getDocumentAttachmentCount(String documentGuid) {
		String sql = "select count(ATTACHMENT_GUID) from "
				+ mTableNameOfAttachment + " where DOCUMENT_GUID = "
				+ stringToSQLString(documentGuid);
		return sqlToInt(sql, 0, 0);
	}

	/*
	 * 通过guid获取附件
	 */
	public WizAttachment getAttachmentByGuid(String guid) {
		String sql = sqlOfSelectAttachment() + " where ATTACHMENT_GUID = "
				+ stringToSQLString(guid);
		ArrayList<WizAttachment> arr = sqlToAttachments(sql);
		if (!WizMisc.isEmptyArray(arr))
			return arr.get(0);
		return null;
	}

	/*
	 * 获取被修改的附件，然后上传
	 */
	public ArrayList<WizAttachment> getModifiedAttachments() {
		String sql = sqlOfSelectAttachment() + " where LOCAL_CHANGED=1"
				+ sqlOfOrderByModifDesc();
		return sqlToAttachments(sql);
	}

	/*
	 * 更新笔记local_changed标记，标记为已经上传
	 */
	public void onUploadedAttachment(WizAttachment att) {
		att.localChanged = 0;
		String sql = "update " + mTableNameOfAttachment + " set LOCAL_CHANGED="
				+ intToSQLString(WizAttachment.ATTACHMENT_LOCAL_CHANGE_NULL)
				+ ", ATTACHMENT_DATA_MD5=" + stringToSQLString(att.dataMd5)
				+ " where ATTACHMENT_GUID=" + stringToSQLString(att.guid);
		execSql(sql);
		//
		WizEventsCenter.sendObjectSyncStatusChangedMessage(att,
				WizEventsCenter.WizObjectSyncStatus.ObjectDownloaded);
	}

	/*
	 * 获取笔记的附件列表
	 */
	public ArrayList<WizAttachment> getDocumentAttachments(String docGuid) {
		String sql = sqlOfSelectAttachment() + " where DOCUMENT_GUID = "
				+ stringToSQLString(docGuid) + sqlOfOrderByModifDesc();
		return sqlToAttachments(sql);
	}

	/*
	 * 从数据库获取附件
	 */

	public ArrayList<WizAttachment> sqlToAttachments(String sql) {
		ArrayList<WizAttachment> arr = new ArrayList<WizAttachment>();
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				WizAttachment data = new WizAttachment();
				//
				data.guid = cursor.getString(0);
				data.docGuid = cursor.getString(1);
				data.name = cursor.getString(2);
				data.dataMd5 = cursor.getString(3);
				data.description = cursor.getString(4);
				data.dateModified = cursor.getString(5);
				data.serverChanged = cursor.getInt(6);
				data.localChanged = cursor.getInt(7);

				arr.add(data);
			}
		} catch (Exception err) {
			err.printStackTrace();
		} finally {
			closeCursor(cursor);
		}
		//
		return arr;
	}

	/**
	 * 判断附件在服务器端是否改变，是否需要下载对应的笔记
	 */
	public boolean isAttachmentServerChanged(String attGUID) {
		String sql = sqlOfSelectAttachment() + " where ATTACHMENT_GUID = "
				+ stringToSQLString(attGUID);

		int ret = sqlToInt(sql, 6, 0);
		return ret == 1;
	}

	/**
	 * 设置笔记在服务器端是否改变
	 */
	public void setAttachmentServerChanged(String guid, boolean changed) {
		String sql = "update " + mTableNameOfAttachment
				+ " set SERVER_CHANGED = " + (changed ? "1" : "0")
				+ " where ATTACHMENT_GUID=" + stringToSQLString(guid);
		execSql(sql);
	}

	/**
	 * 设置笔记在服务器端是否改变
	 */
	public void setAttachmentLocalChanged(String guid, boolean changed) {
		String sql = "update " + mTableNameOfAttachment
				+ " set LOCAL_CHANGED = " + (changed ? "1" : "0")
				+ " where ATTACHMENT_GUID=" + stringToSQLString(guid);
		execSql(sql);
	}

	/*
	 * 判断附件是否存在
	 */
	public boolean attachmentExists(String attGUID) {
		String sql = sqlOfSelectAttachment() + " where ATTACHMENT_GUID = "
				+ stringToSQLString(attGUID);
		return hasRecord(sql);
	}

	/*
	 * 保存附件到数据库
	 */

	private boolean saveAttachment(WizAttachment data, boolean localModified) {
		//
		data.localChanged = localModified ? WizAttachment.ATTACHMENT_LOCAL_CHANGE_DATA
				: WizAttachment.ATTACHMENT_LOCAL_CHANGE_NULL;
		//
		boolean ret = false;
		//
		if (attachmentExists(data.guid)) {
			String sql = "update " + mTableNameOfAttachment + " set "
					+ "ATTACHMENT_NAME=" + stringToSQLString(data.name)
					+ ", DOCUMENT_GUID=" + stringToSQLString(data.docGuid)
					+ ", ATTACHMENT_DATA_MD5="
					+ stringToSQLString(data.dataMd5)
					+ ", ATTACHMENT_DESCRIPTION="
					+ stringToSQLString(data.description) + ", DT_MODIFIED="
					+ stringToSQLString(data.dateModified)
					+ ", SERVER_CHANGED=" + intToSQLString(data.serverChanged)
					+ ", LOCAL_CHANGED=" + intToSQLString(data.localChanged)
					+ " where ATTACHMENT_GUID=" + stringToSQLString(data.guid);
			ret = execSql(sql);

		} else {
			String sql = "insert into " + mTableNameOfAttachment + " ("
					+ sqlFieldListAttachment + ") values ("
					+ stringToSQLString(data.guid) + ", "
					+ stringToSQLString(data.docGuid) + ", "
					+ stringToSQLString(data.name) + ", "
					+ stringToSQLString(data.dataMd5) + ","
					+ stringToSQLString(data.description) + ","
					+ stringToSQLString(data.dateModified) + ","
					+ intToSQLString(data.serverChanged) + ", "
					+ intToSQLString(data.localChanged) + ") ";
			ret = execSql(sql);
		}
		//
		boolean updateCache = localModified;
		//
		if (localModified) {
			updateDocumentAttachmentCount(data.docGuid, updateCache);
		}
		else {
			correctDocumentAttachmentCount(data.docGuid);
		}
		//
		return ret;
	}

	// 下载附件完成
	public void onAttachmentDownloaded(WizAttachment att) {
		att.serverChanged = 0;
		setAttachmentServerChanged(att.guid, false);
		WizEventsCenter.sendObjectSyncStatusChangedMessage(att,
				WizEventsCenter.WizObjectSyncStatus.ObjectDownloaded);
	}

	/*
	 * 保存服务器上面的附件
	 */
	public void saveServerAttachments(ArrayList<WizAttachment> attachments) {
		for (WizAttachment att : attachments) {
			//
			WizAttachment oldAttachment = getAttachmentByGuid(att.guid);
			if (oldAttachment != null && oldAttachment.localChanged == 0) { // 本地存在附件，并且没有修改
				if (!att.dataMd5.equals(oldAttachment.dataMd5)) { // 如果md5改变了
					att.serverChanged = 1; // 设置修改并且保存
				}
			}
			//
			saveAttachment(att, false);
		}
		//			
		boolean modified = true;
		boolean notifyMessage = false;
		setDocumentsModified(modified, notifyMessage);
	}

	/*
	 * 删除笔记的附件
	 */
	public boolean deleteDocumentAttachment(String documentGuid, boolean log) {
		return deleteAttachments(getDocumentAttachments(documentGuid), log);
	}

	/*
	 * 删除多个附件
	 */
	public boolean deleteAttachments(ArrayList<WizAttachment> attachments,
			boolean log) {
		if (WizMisc.isEmptyArray(attachments))
			return true;
		for (WizAttachment att : attachments) {
			removeAttachment(att.guid, log);
		}
		return true;
	}

	/*
	 * 即将下载附件
	 */
	public void onBeforeDownloadAttachment(WizAttachment att) throws Exception {
		String cacheFile = att.getAttachmentFileName(mContext);
		FileUtil.deleteFile(cacheFile);
		String zip = att.getZipFileName(mContext, mUserId);
		FileUtil.deleteFile(zip);
	}

	// ////////////////////////////////////////////////////////////////
	// 加密证书部分代码

	/*
	 * 获取证书
	 */
	public WizCert getCert() {

		WizCert data = new WizCert();
		data.n = getCertN();
		data.e = getCertE();
		data.encryptedD = getCertEncryptedD();
		data.hint = getCertHint();
		return data;
	}

	/*
	 * 保存证书
	 */
	public boolean saveCert(WizCert data) {

		boolean success = saveCertN(data.n);
		success = success && saveCertE(data.e);
		success = success && saveCertEncryptedD(data.encryptedD);
		success = success && saveCertHint(data.hint);
		return success;
	}

	// 查询CERT-N
	private String getCertN() {

		String n = getCertValue(keyOfCertN);
		return n;
	}

	// 设置CERT-N
	private boolean saveCertN(String value) {

		boolean success = saveCertValue(keyOfCertN, value);
		return success;
	}

	// 查询CERT-E
	private String getCertE() {

		String e = getCertValue(keyOfCertE);
		return e;
	}

	// 设置CERT-E
	private boolean saveCertE(String value) {

		boolean success = saveCertValue(keyOfCertE, value);
		return success;
	}

	// 查询CERT-ENCRYPTED_D
	private String getCertEncryptedD() {

		String d = getCertValue(keyOfCertEncryptedD);
		return d;
	}

	// 设置CERT-ENCRYPTED_D
	private boolean saveCertEncryptedD(String value) {

		boolean success = saveCertValue(keyOfCertEncryptedD, value);
		return success;
	}

	// 查询CERT-HINT
	public String getCertHint() {

		String hint = getCertValue(keyOfCertHint);
		return hint;
	}

	// 设置CERT-HINT
	private boolean saveCertHint(String value) {

		boolean success = saveCertValue(keyOfCertHint, value);
		return success;
	}

	private final String keyOfCert = "CERT";
	private final String keyOfCertN = "N";
	private final String keyOfCertE = "E";
	private final String keyOfCertEncryptedD = "ENCRYPTED_D";
	private final String keyOfCertHint = "HINT";

	private String getCertValue(String key) {
		String data = getMeta(keyOfCert, key, "");
		return data;
	}

	private boolean saveCertValue(String key, String value) {
		return setMeta(keyOfCert, key, value);
	}

	@SuppressWarnings("unused")
	private boolean setWizValue(String key, String value) {
		return setMeta("WIZ", key, value);
	}

	private boolean setWizValue(String key, int value) {
		return setMetaInt("WIZ", key, value);
	}

	private int getWizValue(String key, int def) {
		return getMetaIntDef("WIZ", key, def);
	}

	// ////////////////////////////////////////////////////////////////////////
	// document action

	/*
	 * 获取笔记是否修改了数据
	 */
	private int getDocumentLocalChanged(String documentGuid) {
		String sql = "select LOCAL_CHANGED from WIZ_DOCUMENT where DOCUMENT_GUID='"
				+ documentGuid + "'";
		//
		try {
			return sqlToInt(sql, 0, 0);
		} catch (Exception e) {
			return 0;
		}
		//
	}

	/**
	 * 
	 * @param guid document的唯一标识
	 * @param title 笔记的title
	 * @param urlString 要保存的网页地址
	 * @return 保存成功与否
	 * @throws Exception
	 */
	public boolean saveDocumentByUrl(String guid, String title, String urlString)
			throws Exception {

		if (TextUtils.isEmpty(urlString))
			return false;

		InputStream is = null;
		if (!WizMisc.isWifi(mContext) && !WizMisc.isNetworkAvailable(mContext)) {
			throw new Exception("Network unavaliable!");
		}
		HttpURLConnection connection = null;

		try {
			
			URL url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();
			int code = connection.getResponseCode();
			if (HttpURLConnection.HTTP_OK == code) {
				connection.connect();
				is = connection.getInputStream();
			}
			if (is == null)
				throw new Exception("");
			WizDocument document = new WizDocument();
			document.guid = guid;
			document.title = title;
			document.attachmentCount = 0;
			document.dateCreated = TimeUtil.getCurrentSQLDateTimeString();
			document.dateModified = document.dateCreated;
			document.location = WizSystemSettings.getDefaultDirectory(mContext);
			document.localChanged = WizDocument.DOCUMENT_LOCAL_CHANGE_DATA;
			document.serverChanged = 0;
			document.type = "document";
			document.fileType = "html";
			document.url = urlString;

			String filename = document.getEditNoteFileName(mContext);
			if (FileUtil.saveInputStreamToFile(is, filename)
					&& FileUtil.reSaveHtmlToUTF8(filename)) {
				return saveDocumentAndCompressZiw(document, true);
			}
			return false;
		} finally {
			try {
				is.close();
			} catch (Exception err) {
			}
		}
	}

	public static class SharedDocument{
		public String destKbGuid;
		public String destGuid;
		public String destTagGuids;
		public String destDir;
	}

	public void shareDocument2Wiz(String guid, ArrayList<SharedDocument> destKbGuids, boolean isCopyAttachment){
		for (SharedDocument sharedDocument : destKbGuids) {
			try {
				shareDocument2Wiz(guid, sharedDocument, isCopyAttachment);
			} catch (Exception e) {
				//TODO:可以处理异常
			}
		}
	}
	
	public void shareDocument2Wiz(String guid, SharedDocument destData, boolean isCopyAttachment) throws Exception{
		if (TextUtils.isEmpty(guid) || destData == null)
			return;

		WizDocument srcDocument = getDocumentByGuid(guid);
		String srcFileName = srcDocument.getZipFileName(mContext, mUserId);
		if (!FileUtil.fileExists(srcFileName))
			return;

		WizDocument destDocument = new WizDocument();
		destDocument.guid = destData.destGuid;
		destDocument.title = srcDocument.title;
		destDocument.tagGUIDs = destData.destTagGuids;
		destDocument.fileType = srcDocument.type;
		destDocument.dateCreated = TimeUtil.getCurrentSQLDateTimeString();
		destDocument.dateModified = destDocument.dateCreated;
		destDocument.localChanged = WizDocument.DOCUMENT_LOCAL_CHANGE_DATA;
		destDocument.attachmentCount = 0;
		destDocument.owner = mUserId;
		destDocument.location = destData.destDir;

		String destFileName = destDocument.getZipFileName(mContext, mUserId);
		if (!FileUtil.copyFile(srcFileName, destFileName))
			return;
		
		WizDatabase destDb = getDb(mContext, mUserId, destData.destKbGuid);
		if (isCopyAttachment) {
			//目的数据库对象
			ArrayList<WizAttachment> attachments = getDocumentAttachments(guid);
			for (WizAttachment attachment : attachments) {
				shareAttachment2Wiz(attachment, destData.destGuid, WizMisc.genGUID(), destDb);
			}
		}
		destDb.saveLocalDocument(destDocument, true);
	}

	/*
	 * 界面调用保存本地数据，新建或者修改笔记都这走这个
	 */
	synchronized public boolean saveDocument(String guid, String dir,
			HashSet<String> tagGuids, String type, String fileType,
			String dtCreate, int localChange, String title, String text, boolean notifyRefresh, String[] delTagIds)
			throws Exception {
		WizDocument document = new WizDocument();
		document.guid = guid;
		document.title = title;
		document.attachmentCount = 0;
		document.tagGUIDs = WizMisc.hashSet2String(tagGuids, '*');
		document.dateModified = TimeUtil.getCurrentSQLDateTimeString();
		document.dateCreated = dtCreate;
		document.location = dir;
		document.localChanged = localChange;
		document.serverChanged = 0;
		document.type = type;
		document.fileType = fileType;
		document.readCount = 1 ;
		document.owner = getDocumentOwnerByGuid(guid, mUserId);

		//
		if (localChange == WizDocument.DOCUMENT_LOCAL_CHANGE_INFO) {
			// 保存到数据库，计算修改标记，通知界面更新
			return saveLocalDocument(document, notifyRefresh);
		}
		//
		String fileName = document.getEditNoteFileName(mContext);
		String html = FileUtil.loadTextFromFile(fileName);
		// 拼HTML代码
		html = HTMLUtil.text2Html(html, text, title);
		html = WizJSAction.removeAllEditTags(mContext, html, delTagIds);

		//移除webkit返回的相对编辑时的绝对路径，
		String editPath = document.getEditNotePath(mContext);
		editPath = "file://" + FileUtil.pathAddBackslash(editPath);
		html = html.replaceAll(editPath, "");

		FileUtil.saveTextToFile(fileName, html, "UTF-8");
		return saveDocumentAndCompressZiw(document, notifyRefresh);
	}

	public boolean saveDocument(String guid, String dir,
			String documentTags, String type, String fileType,
			String dtCreate, int localChange, String title, String text, boolean notifyRefresh, String[] delTagIds) throws Exception {
		HashSet<String> tags = WizMisc.string2HashSet(documentTags, '*');
		return saveDocument(guid, dir, tags, type, fileType, dtCreate, localChange, title, text, notifyRefresh, delTagIds);
	}

	/*
	 * 保存笔记到数据库里面，并且进行打包
	 */

	private boolean saveDocumentAndCompressZiw(WizDocument doc, boolean notifyRefresh)
			throws Exception {
		String noteFile = doc.getEditNoteFileName(mContext);
		if (!FileUtil.fileExists(noteFile))
			return false;
		ArrayList<String> files = new ArrayList<String>();
		files.add(noteFile);

		String indexFilesPath = doc.getEditNoteIndexFilePath(mContext);
		indexFilesPath = FileUtil.pathAddBackslash(indexFilesPath);
		if (FileUtil.fileExists(indexFilesPath)) {
			
			File[] fileList = new File(indexFilesPath).listFiles();
			for (File file : fileList) {
				files.add(indexFilesPath + file.getName());
			}
		}
		//
		// 重新计算一下
		doc.attachmentCount = getDocumentAttachmentCount(doc.guid);

		//编辑的根目录
		String editRootPath = doc.getEditNotePath(mContext);
		editRootPath = FileUtil.pathAddBackslash(editRootPath);

//		String text = WizMisc.stringArray2Strings(files, "\n");
//		WizLogUtil.getInstance(mContext).logV("zipFileList", text);
		//压缩到的文件，zip文件
		File zipFile = doc.getZipFile(mContext, mUserId);
		ZipUtil.zipFilesByApache(files, zipFile, editRootPath);
		//

		// 需要zip之后才能更新摘要
		// 否则会导致摘要没有正确生成
		try {
			WizAbstractDatabase.getDb(mContext, mUserId).deleteAbstracts(
					doc.guid);
			WizDocumentAbstractCache.forceUpdateAbstract(mUserId, doc.guid);
		} catch (Exception e) {
		}
		//
		//
		doc.localChanged = WizDocument.DOCUMENT_LOCAL_CHANGE_DATA;
		// 保存到数据库，计算修改标记
		return saveLocalDocument(doc, notifyRefresh);
	}

	/**
	 * 更新笔记的附件数量
	 */
	public boolean onModifyLocalAttachmentCount(String guid){
		WizDocument docExists = getDocumentByGuid(guid);
		if (docExists==null)
			return true;

		int attachmentCount = getDocumentAttachmentCount(guid);
		if (docExists.localChanged != WizDocument.DOCUMENT_LOCAL_CHANGE_DATA) {
			docExists.localChanged = WizDocument.DOCUMENT_LOCAL_CHANGE_INFO;
		}

		String sql = "update " + mTableNameOfDocument
				+ " set ATTACHMENT_COUNT=" + intToSQLString(attachmentCount)
				+ ", LOCAL_CHANGED=" + intToSQLString(docExists.localChanged)
				+ " where DOCUMENT_GUID=" + stringToSQLString(guid);

		boolean modify = execSql(sql);
		if (modify) {
			setDocumentsModified(true, true); // 设置更新并且通知界面刷新
		}
		return modify;
	}
	/*
	 * 删除一篇笔记
	 */

	public boolean removeDocument(String guid, boolean logDelGuid) {
		if (logDelGuid)
			logDeletedGUID(guid, WizObject.DATA_TYPE_DOCUMENT);
		//
		deleteDocumentAttachment(guid, logDelGuid); // 删除附件数据
		//
		WizDocument doc = getDocumentByGuid(guid);
		if (doc == null)
			return true;
		//
		String sql = "delete from " + mTableNameOfDocument
				+ " where DOCUMENT_GUID=" + stringToSQLString(guid);
		//
		if (execSql(sql)) {
			String file = doc.getZipFileName(mContext, mUserId);
			FileUtil.deleteFile(file);
			//
			setDocumentsModified(true, logDelGuid);
			//
			return true;
		}
		return false;
	}

	/*
	 * 通过guid获取笔记
	 */
	public WizDocument getDocumentByGuid(String guid) {
		String sql = sqlOfSelectDocument(true) + " DOCUMENT_GUID="
				+ stringToSQLString(guid);
		//
		ArrayList<WizDocument> arr = sqlToDocuments(sql);
		if (!WizMisc.isEmptyArray(arr))
			return arr.get(0);

		return null;
	}
	
	/*
	 * 通过guid获取笔记
	 * 仅在保存服务端下载笔记时使用，
	 * 此时不过滤/Deleted Items/
	 */
	public WizDocument getDocumentByGuid1(String guid) {
		// String sql = sqlOfSelectDocument(true) + " DOCUMENT_GUID="
		// + stringToSQLString(guid);
		String sql = "select " + sqlFieldListDocument + " from "
				+ mTableNameOfDocument + " where " + " DOCUMENT_GUID="
				+ stringToSQLString(guid);
		//
		ArrayList<WizDocument> arr = sqlToDocuments(sql);
		if (!WizMisc.isEmptyArray(arr))
			return arr.get(0);

		return null;
	}

	/*
	 * 获取笔记的类型
	 */
	public String getDocumentType(String guid) {
		return getValueOfDocument(guid, "DOCUMENT_TYPE", "document");
	}

	/*
	 * 判断是否可以编辑笔记
	 */
	public boolean canEditDocument(String guid) {
		String type = getDocumentType(guid);
		return !TextUtils.equals(type, "todolist")
				&& !TextUtils.equals(type, "todolist2");
	}

	/*
	 * 笔记是否增删了
	 */
	private static void setDocumentsModified(String userId, String kbGuid,
			boolean b) {
		if (kbGuid == null)
			kbGuid = "";
		WizStatusCenter.setBool(userId, "DocumentsModified" + kbGuid, b);
	}

	/*
	 * 返回笔记是否增删了
	 */
	private static boolean isDocumentsModified(String userId, String kbGuid) {
		if (kbGuid == null)
			kbGuid = "";
		return WizStatusCenter.getBool(userId, "DocumentsModified" + kbGuid,
				true); // 默认是脏的
	}

	/*
	 * 判断笔记是否已经被下载
	 */
	public boolean isDocumentDownloaded(WizDocument document) {
		String fileName = document.getZipFileName(mContext, mUserId);
		return FileUtil.fileExists(fileName);
	}

	/*
	 * 判断笔记在服务器端是否改变
	 */
	public int getDocumentServerChanged(String guid) {
		String sql = "select SERVER_CHANGED from " + mTableNameOfDocument
				+ " where DOCUMENT_GUID=" + stringToSQLString(guid);
		//
		int ret = sqlToInt(sql, 0, 0);
		return ret;
	}

	/*
	 * 设置笔记在服务器端是否改变
	 */
	public void setDocumentServerChanged(String guid, int changed) {
		String sql = "update " + mTableNameOfDocument
				+ " set SERVER_CHANGED = " + intToSQLString(changed)
				+ " where DOCUMENT_GUID=" + stringToSQLString(guid);
		execSql(sql);
	}

	/*
	 * 计算笔记的修改属性
	 */
	private int calDocumentLocalChanged(WizDocument doc, int currentLocalChanged) {
		if (currentLocalChanged == WizDocument.DOCUMENT_LOCAL_CHANGE_DATA)
			return WizDocument.DOCUMENT_LOCAL_CHANGE_DATA;
		int oldChanged = getDocumentLocalChanged(doc.guid);
		if (oldChanged == WizDocument.DOCUMENT_LOCAL_CHANGE_DATA)
			return WizDocument.DOCUMENT_LOCAL_CHANGE_DATA;
		return WizDocument.DOCUMENT_LOCAL_CHANGE_INFO;
	}
	
	private String getDocumentOwnerByGuid(String guid, String defaultOwner){
		String sql = "select OWNER from " + mTableNameOfDocument
				+ " where DOCUMENT_GUID=" + stringToSQLString(guid);
		return sqlToString(sql, 0, defaultOwner);
	}

	/*
	 * 更改笔记的标签
	 */
	public boolean changeDocumentTagsByTagGuids(String documentGuid, ArrayList<String> tagGuidArray) {
		WizDocument doc = getDocumentByGuid(documentGuid);
		if (doc == null)
			return false;
		//
		doc.tagGUIDs = WizMisc.stringArray2Strings(tagGuidArray, '*');
		//
		doc.localChanged = calDocumentLocalChanged(doc,
				WizDocument.DOCUMENT_LOCAL_CHANGE_INFO);
		return saveLocalDocument(doc, true);
	}
	
	/*
	 * 更改笔记的标签
	 */
	public boolean changeDocumentTags(String documentGuid, ArrayList<WizTag> tagArray) {
		//
		ArrayList<String> tagGuids = new ArrayList<String>();
		//
		for (WizTag tag : tagArray) {
			tagGuids.add(tag.guid);
		}
		//
		return changeDocumentTagsByTagGuids(documentGuid, tagGuids);
	}
	/*
	 * 添加笔记的标签
	 */
	public boolean addDocumentTag(String documentGuid, WizTag tag) {
		//
		WizDocument doc = getDocumentByGuid(documentGuid);
		if (doc == null)
			return false;
		//
		String tagGuids = doc.tagGUIDs;
		if (tagGuids == null)
			tagGuids = "";
		if (tagGuids.length() == 0) 
			tagGuids = tag.guid;
		else
			tagGuids = tagGuids + "*" + tag.guid;
		//
		ArrayList<String> tags = WizMisc.string2ArrayList(tagGuids, '*');
		return changeDocumentTagsByTagGuids(documentGuid, tags);
	}

	/*
	 * 获取所有的需要上传的笔记
	 */

	public ArrayList<WizDocument> getModifiedDocuments() {
		String sql = sqlOfSelectDocument(true) + " LOCAL_CHANGED <> "
				+ intToSQLString(WizDocument.DOCUMENT_LOCAL_CHANGE_NULL)
				+ sqlOfOrderByModifDesc();
		//
		return sqlToDocuments(sql);
	}

	/*
	 * 完成上传笔记
	 */
	public void onUploadedDocument(WizDocument doc) {
		doc.localChanged = 0;
		String sql = "update " + mTableNameOfDocument + " set LOCAL_CHANGED="
				+ intToSQLString(WizDocument.DOCUMENT_LOCAL_CHANGE_NULL)
				+ ", DOCUMENT_DATA_MD5=" + stringToSQLString(doc.dataMd5)
				+ " where DOCUMENT_GUID=" + stringToSQLString(doc.guid);
		execSql(sql);
		//
		WizEventsCenter.sendObjectSyncStatusChangedMessage(doc,
				WizEventsCenter.WizObjectSyncStatus.ObjectUploaded);
	}

	/*
	 * 笔记被成功下载，清空服务器修改标记
	 */
	public void onDownloadedDocument(WizDocument doc) {
		setDocumentServerChanged(doc.guid,
				WizDocument.DOCUMENT_SERVER_CHANGE_NULL);
	}

	/*
	 * 返回最近几天的笔记
	 */
	public ArrayList<WizDocument> getDocumentsByDay(int countDay) {
		return getDocumentsByDays(countDay);
	}

	/*
	 * 返回最近几周的笔记
	 */
	public ArrayList<WizDocument> getDocumentsByWeek(int countWeek) {
		return getDocumentsByDays(countWeek * 7);
	}

	/*
	 * 返回最近几个月的笔记
	 */
	public ArrayList<WizDocument> getDocumentsByMonth(int countMonth) {

		return getDocumentsByDays(countMonth * 30);
	}

	/*
	 * 通过目录返回笔记，没有包含子目录
	 */

	public ArrayList<WizDocument> getDocumentsByLocation(String location) {
		return getDocumentsByLocation(location, false);
	}
	
	/*
	 * 通过目录返回笔记，可以选择是否包含子目录
	 */

	public ArrayList<WizDocument> getDocumentsByLocation(String location, boolean includeChildren) {
		//
		if (includeChildren) {
			location = location + "%";
		}
		//
		String sql = sqlOfSelectDocument(true) + " DOCUMENT_LOCATION like "
				+ stringToSQLString(location) + sqlOfOrderByModifDesc();
		//
		return sqlToDocuments(sql);
	}

	/*
	 * 通过目录返回未下载的笔记，可以选择是否包含子目录
	 */
	public ArrayList<WizDocument> getUnDownloadDocumentsByLocation(String location, boolean includeChildren) {
		//
		if (includeChildren) {
			location = location + "%";
		}
		//
		String sql = sqlOfSelectDocument(true) + " SERVER_CHANGED=1 AND DOCUMENT_LOCATION like " + stringToSQLString(location) + sqlOfOrderByModifDesc();
		//
		return sqlToDocuments(sql);
	}

	/*
	 * 通过天数返回返回笔记
	 */
	private ArrayList<WizDocument> getDocumentsByDays(int countDay) {
		String currentTime = TimeUtil
				.getCurrentSQLDateTimePastStringForDay(countDay);
		String sql = sqlOfSelectDocument(true) + " DT_MODIFIED>"
				+ stringToSQLString(currentTime) + sqlOfOrderByModifDesc();
		return sqlToDocuments(sql);
	}

	//TODO: 笔记阅读状态，未测试
	//
	/*
	 * 获取未读的笔记
	 */
	public ArrayList<WizDocument> getUnreadDocuments() {
		String sql = sqlOfSelectDocument(true) + " READCOUNT <= 0 "
				+ sqlOfOrderByModifDesc();
		return sqlToDocuments(sql);
	}
	private boolean mIsUnreadDocumentsCountDirty = true;//默认是脏的
	public void setUnreadDocumentsCountDirty(){
		mIsUnreadDocumentsCountDirty = true;
	}
	private int mUnreadDocumentsCount;
	public int getUnreadDocumentsCount(){
		if(mIsUnreadDocumentsCountDirty){
			mUnreadDocumentsCount = getUnreadDocumentsCountFromDb();//update
			mIsUnreadDocumentsCountDirty = false;
		}
		return mUnreadDocumentsCount;
		
	}
	public int getUnreadDocumentsCountByTag(String mTagId){
			
		if(mIsUnreadDocumentsCountDirty){
			mUnreadDocumentsCount = getUnreadDocumentsCountFromDbByTag(mTagId);//update
//			mIsUnreadDocumentsCountDirty = false;
		}
		return mUnreadDocumentsCount;
	}
	private int getUnreadDocumentsCountFromDb(){
		String sql = "select count(*) from "
				+ mTableNameOfDocument + " where "
				+ selectNoteCondition() +" and READCOUNT <= 0";
		Cursor cursor = null ;
		try{
			cursor = mDB.rawQuery(sql, null);
			int num = 0 ;
			if (cursor.moveToNext()) {
				num = cursor.getInt(0);
			}
			return num ;
		}finally{
			closeCursor(cursor);
		}
	}
	private int getUnreadDocumentsCountFromDbByTag(String mTagId){
		String sql = "select count(*) from "
			+ mTableNameOfDocument + " where "
			+ selectNoteCondition() +"and DOCUMENT_TAG_GUIDS = '" +mTagId+"' and READCOUNT <= 0" ;
	Cursor cursor = null ;
	try{
		cursor = mDB.rawQuery(sql, null);
		int num = 0 ;
		if (cursor.moveToNext()) {
			num = cursor.getInt(0);
		}
		return num ;
	}finally{
		closeCursor(cursor);
	}
	}
	/*
	 * 获取是否有未读的笔记
	 */
	private int mHasUnreadDocuments = -1;
	public boolean hasUnreadDocuments() {
		if (mHasUnreadDocuments == -1) {
			String sql = sqlOfSelectDocument(true) + " READCOUNT <= 0 limit 0, 1";
			boolean ret = hasRecord(sql);
			mHasUnreadDocuments = ret ? 1 : 0;
		}
		//
		return mHasUnreadDocuments == 1;
	}
	//
	/**
	 * 设置单篇笔记已读
	 */
	public boolean setDocumentReaded(WizDocument doc) {
		mHasUnreadDocuments = -1;
		//
		doc.readCount++ ;
		String sql = "update WIZ_DOCUMENT set READCOUNT= READCOUNT + 1 where DOCUMENT_GUID='" + doc.guid + "'";
		//
		boolean isSuccess = execSql(sql);
		if(isSuccess){
			setUnreadDocumentsCountDirty();
			
			List<WizObjectBase> docs = new ArrayList<WizObjectBase>();
			docs.add(doc);
			WizEventsCenter.sendReadStausChangedMessage(docs);
		}
		return isSuccess;
	}
	/**
	 *  批量设置笔记已读
	 * @param doucumentGuids
	 * @return
	 */
	public boolean setDocumentsReaded(List<WizDocument> docs) {
		mHasUnreadDocuments = -1;
		//
		boolean isSuccess = false;
		
		StringBuffer sql = new StringBuffer();
		sql.append("update ")
		.append(mTableNameOfDocument)
		.append(" set READCOUNT = READCOUNT + 1 where DOCUMENT_GUID in (") ;
		int length = docs.size();
		for(int i = 0 ; i < length; i++){
			WizDocument doc = docs.get(i);
			doc.readCount++;
			sql.append(stringToSQLString(doc.guid));
			if(i < length -1){
				sql.append(",");
			}else{
				sql.append(")");
			}
		}
		if(execSql(sql.toString())){
			isSuccess = true;
		}
		//成功即发消息通知并返回成功。
		if(isSuccess){
			setUnreadDocumentsCountDirty();
			List<WizObjectBase> objs = new ArrayList<WizObjectBase>(docs);
			WizEventsCenter.sendReadStausChangedMessage(objs);
		}
		return isSuccess;
	}
    
	/**
	 * 按标签id将未读标为已读
	 * @return
	 */
	public boolean setAllUnreadDocumentsReadByTagId(String mTagId){
		mHasUnreadDocuments = 0;
		//
		boolean isSuccess = false;
//		String sql = "update WIZ_DOCUMENT set READCOUNT = READCOUNT+1";
		String sql = "update WIZ_DOCUMENT set READCOUNT = 1 where READCOUNT <= 0 and DOCUMENT_TAG_GUIDS = '"+mTagId+"'";
		//
		if(execSql(sql)){
			isSuccess = true;
		}
		//成功即发消息通知并返回成功。
		if(isSuccess){
			setUnreadDocumentsCountDirty();
			WizEventsCenter.sendReadStausChangedMessage(null);
		}
		return isSuccess;
	}
    
    
	/**
	 * 全部未读标为已读 
	 * @return
	 */
	public boolean setAllUnreadDocumentsReaded() {
		mHasUnreadDocuments = 0;
		//
		boolean isSuccess = false;
//		String sql = "update WIZ_DOCUMENT set READCOUNT = READCOUNT+1";
		String sql = "update WIZ_DOCUMENT set READCOUNT = 1 where READCOUNT <= 0";
		//
		if(execSql(sql)){
			isSuccess = true;
		}
		//成功即发消息通知并返回成功。
		if(isSuccess){
			setUnreadDocumentsCountDirty();
			WizEventsCenter.sendReadStausChangedMessage(null);
		}
		return isSuccess;
	}
	

	//
	/**
	 * 标记全部笔记为已读，全部置为1
	 */
	public boolean markAllDocumentsAsRead() {
		mHasUnreadDocuments = 0;
		//
		boolean isSuccess = false;
		String sql = "update WIZ_DOCUMENT set READCOUNT = 1";
		//
		if(execSql(sql)){
			isSuccess = true;
		}
		//成功即发消息通知并返回成功。
		if(isSuccess){
			setUnreadDocumentsCountDirty();
		}
		return isSuccess;
	}
	
	/*
	 * 按照时间排序，获取笔记
	 */
	private ArrayList<WizDocument> getDocuments(int start, int end) {
		String sql = sqlOfSelectDocument(false) + sqlOfOrderByModifDesc();
		if (start < 0 || start >= end)
			start = 0;

		if (end > 0)
			sql = sql + " limit " + start + " , " + end;
		//
		return sqlToDocuments(sql);
	}

	/*
	 * 获取最近的100条笔记
	 */
	public ArrayList<WizDocument> getRecentDocuments() {
		return getRecentDocuments(100);
	}

	/*
	 * 获取近期笔记
	 */
	public ArrayList<WizDocument> getRecentDocuments(int count) {
		return getDocuments(0, count);
	}

	/*
	 * 按照关键字搜索笔记
	 */
	public ArrayList<WizDocument> searchDocumentsByKey(String searchText) {
		String sql = sqlOfSelectDocument(true) + " DOCUMENT_TITLE like "
				+ stringToSQLString("%" + searchText + "%")
				+ sqlOfOrderByModifDesc();
		//
		return sqlToDocuments(sql);
	}

	/*
	 * 搜索多个关键字
	 */
	public ArrayList<WizDocument> searchDocumentsByKeys(String searchText) {
		String[] keys = WizMisc.string2Array(searchText, ' ');
		return searchDocumentsByKeys(keys);
	}

	/*
	 * 搜索多个关键字
	 */
	public ArrayList<WizDocument> searchDocumentsByKeys(String[] searchText) {
		String sql = sqlOfSelectDocument(true) + " ( DOCUMENT_TITLE like ";
		String andSql = sql;
		for (int i = 0; i < searchText.length; i++) {
			String keyword = stringToSQLString("%" + searchText[i] + "%");
			if (i == searchText.length - 1) {
				andSql = andSql + keyword;
			} else {
				andSql = andSql + keyword + " and DOCUMENT_TITLE like ";
			}
		}
		final String sqlLast = ") " + sqlOfOrderByModifDesc();

		ArrayList<WizDocument> andArray = sqlToDocuments(andSql + sqlLast);

		if (!WizMisc.isEmptyArray(andArray)) {
			return andArray;
		} else {
			return sqlToDocuments(sqlLast);
		}
	}

	/*
	 * 搜索多个关键字
	 */
	public ArrayList<WizDocument> searchDocumentsByKeysAndOr(String[] searchText) {
		String sql = sqlOfSelectDocument(true) + " ( DOCUMENT_TITLE like ";
		String andSql = sql;
		String orSql = sql;
		for (int i = 0; i < searchText.length; i++) {
			String temporarySql = stringToSQLString("%" + searchText[i] + "%");
			if (i == searchText.length - 1) {
				andSql = andSql + temporarySql;
				orSql = orSql + temporarySql;
			} else {
				andSql = andSql + temporarySql + " and DOCUMENT_TITLE like ";
				orSql = orSql + temporarySql + " or DOCUMENT_TITLE like ";
			}
		}
		final String sqlLast = ") " + sqlOfOrderByModifDesc();

		ArrayList<WizDocument> andArray = sqlToDocuments(andSql + sqlLast);

		if (!WizMisc.isEmptyArray(andArray)) {
			return andArray;
		} else {
			return sqlToDocuments(orSql + sqlLast);
		}
	}

	/*
	 * 快速搜索
	 */
	public static final int SEARCH_TODAY = 0;
	public static final int SEARCH_YESTERDAY = 1;
	public static final int SEARCH_BEFORE_YESTERDAY = 2;
	public static final int SEARCH_A_WEEK = 3;
	public static final int SEARCH_TWO_WEEK = 4;
	public static final int SEARCH_A_MONTH = 5;

	public ArrayList<WizDocument> quickSearchDocuments(int searchType) {
		switch (searchType) {
		case SEARCH_TODAY:
			return getDocumentsByDay(0);

		case SEARCH_YESTERDAY:
			return getDocumentsByDay(1);

		case SEARCH_BEFORE_YESTERDAY:
			return getDocumentsByDay(2);

		case SEARCH_A_WEEK:
			return getDocumentsByWeek(1);

		case SEARCH_TWO_WEEK:
			return getDocumentsByWeek(2);

		case SEARCH_A_MONTH:
			return getDocumentsByMonth(1);

		default:
			return getRecentDocuments();
		}
	}

	/*
	 * 
	 * 
	 * 这个方法返回需要下载下一个的笔记对象，处理的时候，按照时间排序，最新的最先返回
	 * 
	 * 注意：需要判断当前数据库是否是群组数据库 还需要判断用户的离线设置，是不离线，一个月离线，还是全部离线 在这个地方返回适当的对象
	 * 本方法会在线程里面调用，因此需要注意，去读取离线设置的时候注意线程保护
	 */
	public WizDocument getNextDocumentNeedToBeDownloaded() {
		String downloadType = WizSystemSettings.downloadTypeOfRecent;
		if (isPersonalKb()) {
			downloadType = WizSystemSettings
					.getPersonalDownLoadDataType(mContext);
		} else {
			downloadType = WizSystemSettings.getGroupDownLoadDataType(mContext);
		}
		int countMonth = -1;
		if (WizSystemSettings.downloadTypeOfNull.equals(downloadType)) {
			return null;
		} else if (WizSystemSettings.downloadTypeOfRecent.equals(downloadType))
			countMonth = 1;
		ArrayList<WizDocument> downloads = getNextDocumentNeedToBeDownloaded(countMonth);
		if (WizMisc.isEmptyArray(downloads))
			return null;

		return downloads.get(0);
	}

	/*
	 * 即将下载某一篇笔记，可以用于清空临时文件等等
	 */

	public void onBeforeDownloadDocument(WizDocument doc) throws Exception {
		String cachePath = doc.getNotePath(mContext, false);
		FileUtil.deleteDirectory(cachePath);
		String zip = doc.getZipFileName(mContext, mUserId);
		FileUtil.deleteFile(zip);
	}

	/*
	 * 下载笔记完成，设置服务器修改标记
	 */

	public void onDocumentDownloaded(WizDocument doc) {
		setDocumentServerChanged(doc.guid, 0);
		doc.serverChanged = 0;
		//
		WizEventsCenter.sendObjectSyncStatusChangedMessage(doc,
				WizEventsCenter.WizObjectSyncStatus.ObjectDownloaded);
		//
		// 先不要生成摘要，影响效率
		// WizDocumentAbstractCache.getAbstract(mUserId, doc.guid);
	}

	//

	/*
	 * 查询数据库笔记
	 */
	public ArrayList<WizDocument> sqlToDocuments(String sql) {
		ArrayList<WizDocument> arr = new ArrayList<WizDocument>();
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				WizDocument data = new WizDocument();
				data.guid = cursor.getString(0);
				data.title = (cursor.getString(1));
				data.location = (cursor.getString(2));
				data.url = (cursor.getString(3));
				data.tagGUIDs = (cursor.getString(4));
				data.type = (cursor.getString(5));
				data.fileType = (cursor.getString(6));
				data.dateCreated = (cursor.getString(7));
				data.dateModified = (cursor.getString(8));
				data.dataMd5 = (cursor.getString(9));
				data.attachmentCount = (Integer.parseInt(cursor.getString(10)));
				data.serverChanged = (Integer.parseInt(cursor.getString(11)));
				data.localChanged = (Integer.parseInt(cursor.getString(12)));
				data.gpsLatitude = (Double.parseDouble(cursor.getString(13)));
				data.gpsLongtitude = (Double.parseDouble(cursor.getString(14)));
				data.gpsAltitude = (Double.parseDouble(cursor.getString(15)));
				data.gpsDop = (Double.parseDouble(cursor.getString(16)));
				data.gpsAddress = (cursor.getString(17));
				data.gpsCountry = (cursor.getString(18));
				data.gpsDescription = (cursor.getString(19));
				data.readCount = (Integer.parseInt(cursor.getString(23)));
				data.owner = (cursor.getString(24));
				data.encrypted = (Integer.parseInt(cursor.getString(25)) == 1);
				data.asterisk = (cursor.getInt(26));
				data.keywords = (cursor.getString(27));
				arr.add(data);
			}

		} catch (Exception err) {
			err.printStackTrace();
		} finally {
			closeCursor(cursor);
		}

		return arr;
	}
	/*
	 * 判断笔记是否存在
	 */
	public boolean documentExists(String documentGUID) {
		String sql = sqlOfSelectDocument(true) + " DOCUMENT_GUID = "
				+ stringToSQLString(documentGUID);
		return hasRecord(sql);
	}

	/*
	 * 判断某一个目录是否包含笔记
	 */

	public boolean hasDocumentsByLocation(String location) {
		String sql = sqlOfSelectDocument(true) + " DOCUMENT_LOCATION like "
				+ stringToSQLString(location + "%");
		return hasRecord(sql);
	}

	/*
	 * 判断某一个标签是否包含笔记
	 */
	public boolean hasDocumentsByTag(String tagGuid) {
		String sql = sqlOfSelectDocument(true) + " DOCUMENT_TAG_GUIDS like "
				+ stringToSQLString("%" + tagGuid + "%");
		return hasRecord(sql);
	}

	/*
	 * 保存服务器的笔记
	 */
	public void saveServerDocuments(ArrayList<WizDocument> arr)
			throws Exception {
		try {
			mDB.beginTransaction();
			boolean isPersonalKb = this.isPersonalKb();
			for (WizDocument doc : arr) {
				if (!isPersonalKb) {
					doc.readCount = 0;	//对于群组里面笔记，从服务器更新的时候，标记为未读，也就是作为新的笔记需要进行阅读
				}
				//
				if (!saveServerDocument(doc)) {
					throw new Exception("Can't save note: " + doc.title);
				}
			}
			mDB.setTransactionSuccessful();
		} finally {
			mDB.endTransaction();
		}
		//
		refreshDocumentsCountCache();
		WizEventsCenter.sendDatabaseRefreshObject(this,
				WizDatabaseObjectType.Document);
	}

	public boolean saveServerDocument(WizDocument doc) {
		//此时不过滤/Deleted Items/
		WizDocument docExists = getDocumentByGuid1(doc.guid);
		//
		if (docExists == null) { // 新的笔记
			doc.serverChanged = 1; // 标记为服务器修改了，进行下载
			doc.localChanged = 0; // 本地没有修改
			// 初始化一下创建时间
			if (TextUtils.isEmpty(doc.dateCreated)) {
				doc.dateCreated = doc.dateModified;
			}
			//
			return addDocument(doc);
		} else {
			String newMd5 = doc.dataMd5;
			String oldMd5 = docExists.dataMd5;
			if (TextUtils.isEmpty(newMd5)) {
				newMd5 = "";
			}

			if (newMd5.equals(oldMd5)) {
				doc.serverChanged = 0; // 数据没有改变，不需要重新下载数据
			} else {
				doc.serverChanged = 1; // 需要重新下载数据
			}
			//
			return modifyDocument(doc);
		}
	}


	public boolean saveLocalDocument(WizDocument doc, boolean notifyRefresh) {

		boolean ret = false;
		//
		WizDocument docExists = getDocumentByGuid(doc.guid);
		//
		if (docExists == null) {
			doc.serverChanged = 0; // 服务器没有
			doc.localChanged = WizDocument.DOCUMENT_LOCAL_CHANGE_DATA; // 新的笔记
			// 初始化一下创建时间
			if (TextUtils.isEmpty(doc.dateCreated)) {
				doc.dateCreated = doc.dateModified;
			}
			//
			ret = addDocument(doc);
		} else {
			doc.localChanged = this.calDocumentLocalChanged(doc, doc.localChanged); //
			doc.serverChanged = 0;
			doc.dateModified = TimeUtil.getCurrentSQLDateTimeString();
			//
			ret = modifyDocument(doc);
		}
		//
		if (ret) {
			if (notifyRefresh) {
				setDocumentsModified(true, true); // 设置更新并且通知界面刷新
			}
			//
		}
		return ret;
	}

	/*
	 * 添加一条笔记到数据库
	 */

	private boolean addDocument(WizDocument data) {

		mHasUnreadDocuments = -1;
		//
		if (data.location == null || data.location.length() == 0) {
			data.location = "/My Notes/";
		}
		//
		if (!locationExists(data.location)) {
			localCreateLocation(data.location);
		}

		String sql = "insert into " + mTableNameOfDocument + " ("
				+ sqlFieldListDocument + ") values ("
				+ stringToSQLString(data.guid) + ", "
				+ stringToSQLString(data.title) + ", "
				+ stringToSQLString(data.location) + ", "
				+ stringToSQLString(data.url) + ", "
				+ stringToSQLString(data.tagGUIDs) + ", "
				+ stringToSQLString(data.type) + ", "
				+ stringToSQLString(data.fileType) + ", "
				+ stringToSQLString(data.dateCreated) + ", "
				+ stringToSQLString(data.dateModified) + ", "
				+ stringToSQLString(data.dataMd5) + ", "
				+ intToSQLString(data.attachmentCount) + ", "
				+ intToSQLString(data.serverChanged) + ", "
				+ intToSQLString(data.localChanged) + ", "
				+ doubleToSQLString(data.gpsLatitude) + ", "
				+ doubleToSQLString(data.gpsLongtitude) + ", "
				+ doubleToSQLString(data.gpsAltitude) + ", "
				+ doubleToSQLString(data.gpsDop) + ", "
				+ stringToSQLString(data.gpsAddress) + ", "
				+ stringToSQLString(data.gpsCountry) + ", "
				+ stringToSQLString(data.gpsDescription) + ", '', '', '', "
				+ intToSQLString(data.readCount) + ", "
				+ stringToSQLString(data.owner) + ", "
				+ intToSQLString(data.encrypted ? 1 : 0) + ", "
				+ intToSQLString(data.asterisk) + ", "
				+ stringToSQLString(data.keywords) + 
				") ";

		if (execSql(sql)) {
			setDocumentsModified(true, false);
			return true;
		}
		return false;
	}

	/*
	 * 更新一条笔记
	 */
	private boolean modifyDocument(WizDocument data) {
		//
		mHasUnreadDocuments = -1;
		//
		if (data.location == null || data.location.length() == 0) {
			data.location = "/My Notes/";
		}
		//
		if (!locationExists(data.location)) {
			localCreateLocation(data.location);
		}
		try {
			String sql = "update " + mTableNameOfDocument
					+ " set DOCUMENT_TITLE=" + stringToSQLString(data.title)
					+ ", DOCUMENT_LOCATION=" + stringToSQLString(data.location)
					+ ", DOCUMENT_URL=" + stringToSQLString(data.url)
					+ ", DOCUMENT_TAG_GUIDS="
					+ stringToSQLString(data.tagGUIDs) + ", DOCUMENT_TYPE="
					+ stringToSQLString(data.type) + ", DOCUMENT_FILE_TYPE="
					+ stringToSQLString(data.fileType) + ", DT_CREATED="
					+ stringToSQLString(data.dateCreated) + ", DT_MODIFIED="
					+ stringToSQLString(data.dateModified)
					+ ", DOCUMENT_DATA_MD5=" + stringToSQLString(data.dataMd5)
					+ ", ATTACHMENT_COUNT="
					+ intToSQLString(data.attachmentCount)
					+ ", SERVER_CHANGED=" + intToSQLString(data.serverChanged)
					+ ", LOCAL_CHANGED=" + intToSQLString(data.localChanged)
					+ ", GPS_LATITUDE=" + doubleToSQLString(data.gpsLatitude)
					+ ", GPS_LONGTITUDE="
					+ doubleToSQLString(data.gpsLongtitude) + ", GPS_ALTITUDE="
					+ doubleToSQLString(data.gpsAltitude) + ", GPS_DOP="
					+ doubleToSQLString(data.gpsDop) + ", GPS_ADDRESS="
					+ stringToSQLString(data.gpsAddress) + ", GPS_COUNTRY="
					+ stringToSQLString(data.gpsCountry) + ", GPS_DESCRIPTION="
					+ stringToSQLString(data.gpsDescription)
					+ ", GPS_LEVEL1=''" + ", GPS_LEVEL2=''" + ", GPS_LEVEL3=''"
					+ ", READCOUNT=" + intToSQLString(data.readCount)
					+ ", OWNER=" + stringToSQLString(data.owner) + ", PROTECT="
					+ intToSQLString(data.encrypted ? 1 : 0)
					+ ", DOCUMENT_ASTERISK=" + intToSQLString(data.asterisk)
					+ ", DOCUMENT_KEYWORDS=" + stringToSQLString(data.keywords)
					+ " where DOCUMENT_GUID=" + stringToSQLString(data.guid);
			//
			if (execSql(sql)) {
				setDocumentsModified(true, false);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		//
		return false;
	}

	/*
	 * 获取下一条需要被下载的笔记
	 */

	private ArrayList<WizDocument> getNextDocumentNeedToBeDownloaded(
			int countMonth) {

		// TODO:需要根据群组，私人笔记进行分开判断
		String sql = sqlOfSelectDocument(true) + " SERVER_CHANGED=1 ";
		if (countMonth > 0) {
			String currentTime = TimeUtil
					.getCurrentSQLDateTimePastStringForMonth(countMonth);
			sql += " AND DT_MODIFIED>" + stringToSQLString(currentTime);
		}
		sql = sql + " " + sqlOfOrderByModifDesc() + " limit 0, 1";

		return sqlToDocuments(sql);
	}

	/*
	 * 是否有笔记
	 */

	public boolean hasDocuments() {
		String sql = "select * from WIZ_DOCUMENT limit 0, 1";
		return hasRecord(sql);
	}

	/*
	 * 是否有标签
	 */
	public boolean hasTags() {
		String sql = "select * from WIZ_TAG limit 0, 1";
		return hasRecord(sql);
	}

	/*
	 * 是否有附件
	 */
	public boolean hasAttachments() {
		String sql = "select * from WIZ_ATTACHMENT limit 0, 1";
		return hasRecord(sql);
	}

	/*
	 * 是否有对象
	 */
	public boolean hasObjects() {
		return hasDocuments() || hasTags() || hasAttachments();
	}

	// /////////////////////////////////////////////////////////////////////
	// 版本相关

	/*
	 * 返回本地数据的版本
	 */
	public WizKbVersion getVersions() {
		WizKbVersion kbVersion = new WizKbVersion();
		kbVersion.documentVersion = getDocumentVersion();
		kbVersion.tagVersion = getTagVersion();
		kbVersion.deletedVersion = getDeletedGUIDVersion();
		kbVersion.attachmentVersion = getAttachmentVersion();
		kbVersion.styleVersion = getStyleVersion();
		return kbVersion;
	}

	private String keyOfWizVersionCode = "VERSION_CODE";

	/*
	 * 设置WizNote客户端的版本号version_code
	 */
	public boolean setWizVersion(int code) {

		return setWizValue(keyOfWizVersionCode, code);
	}

	/*
	 * 获取WizNote客户端版本号
	 */
	public int getWizVersion() {
		return getWizValue(keyOfWizVersionCode, 0);
	}

	/*
	 * 设置笔记的版本号
	 */
	public void setDocumentsVersion(long version) {

		setSyncVersion(SyncVersionOfDocument, version);
	}
	/*
	 * 设置消息的版本号
	 */
	public void setMessageVersion(long version) {
		
		setSyncVersion(SyncVersionOfMessage, version);
	}
	
	/*
	 * 设置标签的版本号
	 */
	public void setTagsVersion(long version) {
		setSyncVersion(SyncVersionOfTag, version);
	}

	/*
	 * 设置style的版本号
	 */
	public void setStyleVersion(long version) {

		setSyncVersion(SyncVersionOfStyle, version);
	}

	/*
	 * 设置附件的版本号
	 */
	public void setAttachmentsVersion(long version) {
		setSyncVersion(SyncVersionOfAttachment, version);
	}

	private final static String SyncVersionOfDocument = "DOCUMENT";
	private final static String SyncVersionOfMessage = "MESSAGE";
	private final static String SyncVersionOfBizMember = "BIZ_MEMBER";
	private final static String SyncVersionOfAttachment = "ATTACHMENT";
	private final static String SyncVersionOfTag = "TAG";
	private final static String SyncVersionOfDeletedGUID = "DELETED_GUID";
	private final static String SyncVersionOfStyle = "DELETED_GUID";

	/*
	 * 获取本地对象的版本
	 */
	private long getSyncVersion(String type) {
		String str = getMeta("SYNC_VERSION", type, "0");
		//
		return Long.parseLong(str, 10);
	}

	/*
	 * 设置本地对象的版本
	 */
	private boolean setSyncVersion(String type, long ver) {
		String verString = Long.toString(ver);

		return setMeta("SYNC_VERSION", type, verString);
	}

	/*
	 * 设置已删除记录版本号
	 */
	public void setDeletedGUIDsVersion(long startVersion) {
		setSyncVersion(SyncVersionOfDeletedGUID, startVersion);
	}

	/*
	 * 笔记版本
	 */
	private long getDocumentVersion() {
		return getSyncVersion(SyncVersionOfDocument);
	}
	/*
	 * 消息版本
	 */
	public long getMessageVersion() {
		return getSyncVersion(SyncVersionOfMessage);
	}
	/*
	 * 附件版本
	 */
	private long getAttachmentVersion() {
		return getSyncVersion(SyncVersionOfAttachment);
	}

	/*
	 * 删除记录版本
	 */
	private long getDeletedGUIDVersion() {
		return getSyncVersion(SyncVersionOfDeletedGUID);
	}

	/*
	 * style版本
	 */
	private long getStyleVersion() {
		return getSyncVersion(SyncVersionOfStyle);
	}

	/*
	 * 标签版本
	 */
	private long getTagVersion() {
		return getSyncVersion(SyncVersionOfTag);
	}

	// //////////////////////////////////////////////////////////////
	// key value
	// 目前仅限于文件夹folders, folders_pos

	/*
	 * 获取本地的key value的value
	 */
	public String getKeyValue(String key) {
		//
		if (key.equals("folders")) {
			ArrayList<String> folderList = getAllLocationsString();
			String folders = WizMisc.stringArray2Strings(folderList, "*");
			return folders;
		} else if (key.equals("folders_pos")) {
			return null;
		}
		return null;
	}

	/*
	 * 设置key value数据。保存服务器上面的数据
	 */
	public void saveKeyValue(String key, WizKeyValue data,
			boolean saveServerVersion) {
		// 记录文件夹
		if (key.equals("folders")) {
			setKeyValueOfFolders(data, saveServerVersion);
		} else if (key.equals("folders_pos")) {
			setKeyValueOfFolderPos(data);
		}
	}

	/*
	 * 获取本地对象的版本。因为之前的版本更新笔记文件夹等有问题， 因此在这里全部重新设置一下
	 */
	private long getKeyValueVersionCore(String type) {
		String str = getMeta("KEY_VALUE_VERSION", type, "0");
		//
		return Long.parseLong(str, 10);
	}

	/*
	 * 设置本地对象的版本
	 */
	private boolean setKeyValueVersionCore(String type, long ver) {
		String verString = Long.toString(ver);

		return setMeta("KEY_VALUE_VERSION", type, verString);
	}

	/*
	 * 获取本地的key value 版本号
	 */
	public long getKeyValueVersion(String key) {
		//
		long ret = getKeyValueVersionCore(key);
		//
		if (key.equals("folders")) {
			// 手机客户端的文件夹，只有用户手工创建后才返回-1// 否则返回上次同步后记录的值，默认0
			return ret;

		} else if (key.equals("folders_pos")) {
			if (ret < 0)
				return 0;
			return ret; // 手机客户端不进行排序
		}
		return 0;
	}

	/*
	 * 设置key value的版本号
	 */
	public boolean setKeyValueVersion(String key, long version) {
		return setKeyValueVersionCore(key, version);
	}

	/*
	 * 获取需要同步的key value 暂时只有私人笔记需要同步文件夹数据
	 */
	public String[] getAllKeys() {
		//
		if (isPersonalKb()) {
			String[] keys = new String[] { "folders", "folders_pos" };
			return keys;
		} else {
			return new String[] {};
		}
	}

	// //////////////////////////////////////////////////////////////////
	// account相关

	private final String keyOfAccount = "ACCOUNT";

	/*
	 * 获取邀请码
	 */
	public String getInviteCode() {
		return getAccountValue(keyOfAccountInviteCode);
	}

	public boolean setInviteCode(String invitationCode) {
		return setAccountValue(keyOfAccountInviteCode, invitationCode);
	}


	/**
	 * 獲取用戶類型 return VIP || free
	 * @return
	 */
	public String getUserType() {
		return getAccountValue(keyOfAccountUserType);
	}

	public WizUserInfo getUserInfo(){
		WizUserInfo user = new WizUserInfo();
		user.displayName = getAccountValue(keyOfAccountDisplayName);
		user.inviteCode = getAccountValue(keyOfAccountInviteCode);
		user.userType = getAccountValue(keyOfAccountUserType);
		user.userLevel = getAccountValue(keyOfAccountUserLevel);
		user.userLevelName = getAccountValue(keyOfAccountUserLevelName);
		user.userPoints = getAccountValue(keyOfAccountUserPoints);
		user.email = getAccountValue(keyOfAccountEmail);
		user.mobile = getAccountValue(keyOfAccountMobile);
		user.nickName = getAccountValue(keyOfAccountNickName);
		return user;
	}

	/*
	 * 记录用户信息
	 */
	public void onUserLogin(WizUserInfo userInfo) {
		setAccountValue(keyOfAccountInviteCode, userInfo.inviteCode);
		setAccountValue(keyOfAccountDisplayName, userInfo.displayName);
		setAccountValue(keyOfAccountPersonalKbGuid, userInfo.personalKbGuid);
		setAccountValue(keyOfAccountPersonalKbDatabaseUrl,userInfo.personalKbDatabaseUrl);

		setAccountValue(keyOfAccountUserGuid, userInfo.userGuid);
		setAccountValue(keyOfAccountUserType, userInfo.userType);
		setAccountValue(keyOfAccountUserLevel, userInfo.userLevel);
		setAccountValue(keyOfAccountUserLevelName, userInfo.userLevelName);
		setAccountValue(keyOfAccountNickName, userInfo.nickName);
		setAccountValue(keyOfAccountEmail, userInfo.email);
		setAccountValue(keyOfAccountMobile, userInfo.mobile);
		setAccountValue(keyOfAccountUserPoints, userInfo.userPoints);
	}

	private final String keyOfAccountInviteCode = "INVITATE_CODE";
	private final String keyOfAccountDisplayName = "DISPLAY_NAME";
	private final String keyOfAccountPersonalKbGuid = "PERSONAL_KBGUID";
	private final String keyOfAccountPersonalKbDatabaseUrl = "PERSONAL_KBDATABASEURL";
	
	private final String keyOfAccountUserGuid = "USER_GUID";
	private final String keyOfAccountUserType = "USER_TYPE";
	private final String keyOfAccountUserLevel = "USER_LEVEL";
	private final String keyOfAccountUserLevelName = "USER_LEVEL_NAME";
	private final String keyOfAccountNickName = "NICK_NAME";
	private final String keyOfAccountEmail = "EMAIL";
	private final String keyOfAccountMobile = "MOBILE";
	private final String keyOfAccountUserPoints = "USER_POINTS";

	/*
	 * 获取account相关的数据
	 */
	private String getAccountValue(String key) {
		String value = getMeta(keyOfAccount, key, "");
		if (TextUtils.isEmpty(value))
			value = "";
		return value;
	}

	/*
	 * 设置account相关的数据
	 */
	private boolean setAccountValue(String key, String value) {
		return setMeta(keyOfAccount, key, value);
	}

	// //////////////////////////////////////////////////////////////////
	// kb相关

	/*
	 * 获取全部的群组
	 */
	public ArrayList<WizKb> getAllGroups() {
		//
		refreshAllGroupsData();
		//
		synchronized (mGroupsArray) {
			@SuppressWarnings("unchecked")
			ArrayList<WizKb> kb = (ArrayList<WizKb>) mGroupsArray.clone();
			return kb;
		}
	}

	private ArrayList<WizKb> mGroupsArray = new ArrayList<WizKb>();

	private void refreshAllGroupsData() {
		//
		if (!isGrpupsModified(mUserId))
			return;
		// /
		setGroupsModified(mUserId, false);
		//
		ArrayList<WizKb> kbs = new ArrayList<WizKb>();
		int count = getKbCount();
		for (int i = 0; i < count; i++) {
			WizGroupKb kb = getGroupKb(i);
			kbs.add(kb);
		}
		mGroupsArray = kbs;
	}

	//

	/*
	 * 群组是否增删了
	 */
	private static void setGroupsModified(String userId, boolean b) {
		WizStatusCenter.setBool(userId, "GroupsModified", b);
	}

	/*
	 * 群组笔记是否增删了
	 */
	private static boolean isGrpupsModified(String userId) {
		return WizStatusCenter.getBool(userId, "GroupsModified", true); // 默认是脏的
	}

	/*
	 * 通过kb guid获取某一个kb
	 */
	public WizKb getKbByGuid(String kbGuid) {
		//
		WizDatabase db = this;
		//
		if (!isPersonalKb()) {
			db = WizDatabase.getDb(mContext, mUserId, null);
		}
		//
		if (kbGuid == null || kbGuid.equals("")) {
			return db.getPersonalKb();
		}
		//
		int position = db.getKbPosition(kbGuid);
		if (position < 0)
			return null;
		WizGroupKb kb = db.getGroupKb(position);
		return kb;
	}
	public WizKb getKb(){
		return getKbByGuid(mKbGuid);
	}

	public boolean isOwner() {
		return getKbByGuid(mKbGuid).isOwner();
	}

	public boolean isAdmin() {
		return getKbByGuid(mKbGuid).isAdmin();
	}

	public boolean isSuper() {
		return getKbByGuid(mKbGuid).isSuper();
	}

	public boolean isEditor() {
		return getKbByGuid(mKbGuid).isEditor();
	}

	public boolean isAuthor() {
		return getKbByGuid(mKbGuid).isAuthor();
	}

	public boolean isReader() {
		return getKbByGuid(mKbGuid).isReader();
	}

	/*
	 * 保存kb
	 */
	public void saveDownloadKbs(ArrayList<WizKb> kbs) {
		int length = kbs.size();
		for (int i = 0; i < length; i++)
			saveGroupKb(i, kbs.get(i));

		setKbCount(length);
		//
		setGroupsModified(mUserId, true);
		refreshAllGroupsData();
	}

	/*
	 * 获取个人kb
	 */
	public WizKb getPersonalKb() {
		return new WizPersonalKb(
				getAccountValue(keyOfAccountPersonalKbDatabaseUrl),
				getAccountValue(keyOfAccountPersonalKbGuid),
				WizStrings.getString(WizStringId.PERSONAL_KB_NAME));
	}

	/*
	 * 判断当前数据库是否是一个个人kb
	 */
	public boolean isPersonalKb() {
		return mDbFile.indexOf("index.db") != -1;
	}

	private final String keyOfGroupKb = "GROUPKB";
	private final String keyOfGroupKbCount = "GROUPKB_COUNT";
	private final String keyOfGroupKbGuid = "GROUPKB_GUID";
	private final String keyOfGroupName = "GROUPKB_NAME";
	private final String keyOfGroupBizName = "GROUPKB_BIZ_NAME";
	private final String keyOfGroupBizGuid = "GROUPKB_BIZ_GUID";
	private final String keyOfGroupKBDatabaseUrl = "GROUPKB_KBDATABASEURL";
	private final String keyOfGroupKBUserRole = "GROUPKB_USERROLE";

	/*
	 * 获取某一个group
	 */
	private WizGroupKb getGroupKb(int position) {
		WizGroupKb kb = new WizGroupKb();
		kb.name = getKbValue(position, keyOfGroupName, "");
		kb.bizName = getKbValue(position, keyOfGroupBizName, "");
		kb.bizGuid = getKbValue(position, keyOfGroupBizGuid, "");
		kb.kbGuid = getKbValue(position, keyOfGroupKbGuid, "");
		kb.kbDatabaseUrl = getKbValue(position, keyOfGroupKBDatabaseUrl, "");
		kb.userRole = getKbValue(position, keyOfGroupKBUserRole,
				WizKb.GROUP_ROLE_READER);
		return kb;
	}

	/*
	 * 保存某一个kb
	 */
	private void saveGroupKb(int position, WizKb kb) {
		setKbValue(position, keyOfGroupName, kb.name);
		setKbValue(position, keyOfGroupBizName, kb.bizName);
		setKbValue(position, keyOfGroupBizGuid, kb.bizGuid);
		setKbValue(position, keyOfGroupKbGuid, kb.kbGuid);
		setKbValue(position, keyOfGroupKBDatabaseUrl, kb.kbDatabaseUrl);
		setKbValue(position, keyOfGroupKBUserRole, kb.userRole);
		setKbPosition(kb.kbGuid, position);
	}
	/*
	 * 获取kb value
	 */
	private String getKbValue(int position, String key, String def) {
		String value = getMeta(keyOfGroupKb + position, key, def);
		return value;
	}

	/*
	 * 设置kb value
	 */
	private boolean setKbValue(int position, String key, String value) {
		return setMeta(keyOfGroupKb + position, key, value);
	}

	/*
	 * 获取kb value
	 */
	private int getKbValue(int position, String key, int def) {
		int value = getMetaIntDef(keyOfGroupKb + position, key, def);
		return value;
	}

	/*
	 * 设置kb value
	 */
	private boolean setKbValue(int position, String key, int value) {
		return setMetaInt(keyOfGroupKb + position, key, value);
	}

	/*
	 * 获取某一个kb的index
	 */
	private int getKbPosition(String kbGuid) {
		int value = getMetaIntDef(keyOfGroupKb, kbGuid, -1);
		return value;
	}

	/*
	 * 设置某一个kb的index
	 */
	private boolean setKbPosition(String kbGuid, int position) {
		return setMetaInt(keyOfGroupKb, kbGuid, position);
	}

	/*
	 * 获取kb的数量
	 */
	private int getKbCount() {
		int count = getMetaIntDef(keyOfGroupKb, keyOfGroupKbCount, 0);
		return count;
	}

	/*
	 * 设置kb的数量
	 */
	private boolean setKbCount(int count) {
		return setMetaInt(keyOfGroupKb, keyOfGroupKbCount, count);
	}

	// ////////////////////////////////////////////////////////////////
	// 数据库操作相关

	/*
	 * 浮点数到sql
	 */
	private String doubleToSQLString(Double count) {
		String str = String.valueOf(count);
		return stringToSQLString(str);
	}

	/*
	 * int到sql
	 */
	private String intToSQLString(int count) {
		String str = String.valueOf(count);
		return stringToSQLString(str);
	}
	/*
	 * long到sql
	 */
	private String longToSQLString(long count) {
		String str = String.valueOf(count);
		return stringToSQLString(str);
	}

	/*
	 * 字符串到Sql
	 */
	private String stringToSQLString(String str) {
		if (TextUtils.isEmpty(str))
			return "''";
		//
		str = str.replace("'", "''");
		//
		return "'" + str + "'";
	}

	static private final String mTableNameOfAttachment = "WIZ_DOCUMENT_ATTACHMENT";
	static private final String mTableNameOfDeleted = "WIZ_DELETED_GUID";
	static public final String mTableNameOfDocument = "WIZ_DOCUMENT";
	static private final String mTableNameOfLocation = "WIZ_LOCATION";
	static private final String mTableNameOfMeta = "WIZ_META";
	static private final String mTableNameOfTag = "WIZ_TAG";
	static private final String mTableNameOfUser = "WIZ_USER";
	static private final String mTableNameOfMessage = "WIZ_MESSAGE";

	static private final String mIndexNameOfDocument = "DOCUMENTGUID";

	static private final String sqlFieldListLocation = "DOCUMENT_LOCATION, PARENT_LOCATION, LOCAL_CHANGED, LOCATION_POS";
	static private final String sqlFieldListAttachment = "ATTACHMENT_GUID, DOCUMENT_GUID, ATTACHMENT_NAME, ATTACHMENT_DATA_MD5, ATTACHMENT_DESCRIPTION, DT_MODIFIED, SERVER_CHANGED, LOCAL_CHANGED";
	static private final String sqlFieldListDeletedGUID = "DELETED_GUID, GUID_TYPE, DT_DELETED";
	static private final String sqlFieldListMeta = "META_NAME, META_KEY, META_VALUE";
	static private final String sqlFieldListTag = "TAG_GUID, TAG_PARENT_GUID, TAG_NAME, TAG_DESCRIPTION, LOCAL_CHANGED,DT_MODIFIED";
	static private final String sqlFieldListDocument = "DOCUMENT_GUID, DOCUMENT_TITLE, DOCUMENT_LOCATION, DOCUMENT_URL, DOCUMENT_TAG_GUIDS, DOCUMENT_TYPE, DOCUMENT_FILE_TYPE, DT_CREATED, DT_MODIFIED, DOCUMENT_DATA_MD5, ATTACHMENT_COUNT, SERVER_CHANGED, LOCAL_CHANGED, GPS_LATITUDE, GPS_LONGTITUDE, GPS_ALTITUDE, GPS_DOP, GPS_ADDRESS, GPS_COUNTRY, GPS_DESCRIPTION, GPS_LEVEL1, GPS_LEVEL2, GPS_LEVEL3, READCOUNT, OWNER, PROTECT, DOCUMENT_ASTERISK, DOCUMENT_KEYWORDS";
	static private final String sqlFieldListMessage = "MESSAGE_ID, BIZ_GUID, KB_GUID, DOCUMENT_GUID, MESSAGE_TITLE, SENDER_GUID, SENDER_ID, SENDER_ALIAS, RECEIVER_GUID, RECEIVER_ID, RECEIVER_ALIAS, VERSION, MESSAGE_TYPE, EMAIL_STATUS, SMS_STATUS, READ_COUNT, DT_CREATED, NOTE, MESSAGE_BODY, LOCAL_CHANGED";
	static private final String sqlFieldListUser = "USER_GUID, BIZ_GUID, USER_ID, USER_ALIAS, USER_PINYIN";

	/*
	 * 个人文件夹结构
	 */
	static private final String sqlTableLocation = "CREATE TABLE "
			+ mTableNameOfLocation + " (\n"
			+ "DOCUMENT_LOCATION		CHAR(255)		NOT NULL,\n"
			+ "PARENT_LOCATION					CHAR(255)				,\n"
			+ "LOCAL_CHANGED					int						,\n"
			+ "LOCATION_POS					int						,\n"
			+ "primary key (DOCUMENT_LOCATION)\n" + ")";

	/*
	 * 创建标签
	 */
	static private final String sqlTableTag = "CREATE TABLE " + mTableNameOfTag
			+ " (\n" + "TAG_GUID							char(36)		not null,\n"
			+ "TAG_PARENT_GUID					char(36)				,\n"
			+ "TAG_NAME							varchar(150)			,\n"
			+ "TAG_DESCRIPTION					varchar(600)			,\n"
			+ "LOCAL_CHANGED					int(1)					,\n"
			+ "DT_MODIFIED						char(19)				,\n" + "primary key (TAG_GUID)\n"
			+ ")";

	/*
	 * 笔记
	 */
	static private final String sqlTableDocument = "CREATE TABLE "
			+ mTableNameOfDocument + " (\n"
			+ "DOCUMENT_GUID		char(36)					not null,\n"
			+ "DOCUMENT_TITLE				varchar(768)				not null,\n"
			+ "DOCUMENT_LOCATION			varchar(768)						,\n"
			+ "DOCUMENT_URL					varchar(2048)						,\n"
			+ "DOCUMENT_TAG_GUIDS			varchar(2048)						,\n"
			+ "DOCUMENT_TYPE				varchar(20)							,\n"
			+ "DOCUMENT_FILE_TYPE			varchar(20)							,\n"
			+ "DT_CREATED					char(19)							,\n"
			+ "DT_MODIFIED					char(19)							,\n"
			+ "DOCUMENT_DATA_MD5			char(32)							,\n"
			+ "ATTACHMENT_COUNT				int									,\n"
			+ "SERVER_CHANGED				int									,\n"
			+ "LOCAL_CHANGED				int									,\n"
			+ "GPS_LATITUDE					Double								,\n"
			+ "GPS_LONGTITUDE				Double								,\n"
			+ "GPS_ALTITUDE					Double								,\n"
			+ "GPS_DOP						Double								,\n"
			+ "GPS_ADDRESS					varchar(200)						,\n"
			+ "GPS_COUNTRY					varchar(200)						,\n"
			+ "GPS_DESCRIPTION				varchar(500)						,\n"
			+ "GPS_LEVEL1					varchar(200)						,\n"
			+ "GPS_LEVEL2					varchar(200)						,\n"
			+ "GPS_LEVEL3					varchar(200)						,\n"
			+ "READCOUNT					int									,\n"
			+ "OWNER						varchar(50)							,\n"
			+ "PROTECT						int									,\n"
			+ "DOCUMENT_ASTERISK			int									,\n"
			+ "primary key (DOCUMENT_GUID)\n" + ")";

	/*
	 * meta
	 */
	static private final String sqlTableMeta = "CREATE TABLE "
			+ mTableNameOfMeta
			+ " (\n"
			+ "META_NAME                       varchar(50) NOT NULL COLLATE NOCASE	,\n"
			+ "META_KEY                        varchar(50) NOT NULL COLLATE NOCASE	,\n"
			+ "META_VALUE                      varchar(3000)						,\n"
			+ "primary key (META_NAME, META_KEY)\n" + ");";

	/*
	 * 删除记录
	 */
	static private final String sqlTableDeletedGUID = "CREATE TABLE "
			+ mTableNameOfDeleted + " (\n"
			+ "DELETED_GUID                   char(36)						not null,\n"
			+ "GUID_TYPE                      int							not null,\n"
			+ "DT_DELETED                     char(19)								,\n"
			+ "primary key (DELETED_GUID)\n" + ");";

	/*
	 * 附件
	 */
	static private final String sqlTableDocumentAttachment = "CREATE TABLE "
			+ mTableNameOfAttachment
			+ " (\n"
			+ "ATTACHMENT_GUID               char(36)                not null,\n"
			+ "DOCUMENT_GUID                 char(36)                not null,\n"
			+ "ATTACHMENT_NAME               varchar(768)            not null,\n"
			+ "ATTACHMENT_DATA_MD5           char(32),\n"
			+ "ATTACHMENT_DESCRIPTION        varchar(1000),\n"
			+ "DT_MODIFIED                   char(19),\n"
			+ "SERVER_CHANGED                int,\n"
			+ "LOCAL_CHANGED                 int,\n"
			+ "primary key (ATTACHMENT_GUID)\n" + ");";
	/*
	 * Message
	 */
	static private final String sqlTableMessage = "CREATE TABLE "
			+ mTableNameOfMessage 
			+ "(\n" 
			+ "MESSAGE_ID 				     int64 				PRIMARY KEY NOT NULL,\n"
			+ "BIZ_GUID 					 char(38) 				NOT NULL			,\n" 
			+ "KB_GUID 						 char(38),\n" 
			+ "DOCUMENT_GUID 				 char(38),\n" 
			+ "MESSAGE_TITLE 				 varchar(768),\n" 
			+ "SENDER_GUID 					 char(38),\n" 
			+ "SENDER_ID 					 varchar(128),\n" 
			+ "SENDER_ALIAS 				 varchar(32),\n" 
			+ "RECEIVER_GUID 				 char(38),\n" 
			+ "RECEIVER_ID 				     varchar(128),\n" 
			+ "RECEIVER_ALIAS 				 varchar(32),\n" 
			+ "VERSION 						 int64 				NOT NULL			,\n" 
			+ "MESSAGE_TYPE 				 int32 				NOT NULL			,\n" 
			+ "EMAIL_STATUS 			     int32,\n" 
			+ "SMS_STATUS 					 int32,\n" 
			+ "READ_COUNT 					 int32 				NOT NULL			,\n" 
			+ "DT_CREATED 					 char(19) 				NOT NULL			,\n" 
			+ "NOTE 				 		 varchar(256),\n"
			+ "MESSAGE_BODY 				 varchar(1024),\n"  
			+ "LOCAL_CHANGED 				 int\n"  
			+ ");";
	/*
	 * 创建biz用户信息数据库
	 */
	static private final String sqlTableUser = "CREATE TABLE " + mTableNameOfUser
			+ " (\n" 
			+ "USER_GUID					 char(38)			NOT NULL,\n"
			+ "BIZ_GUID						 char(38)			NOT NULL,\n"
			+ "USER_ID						 char(36)				,\n"
			+ "USER_ALIAS					 varchar(32)			,\n"
			+ "USER_PINYIN					 varchar(38)			,\n"
			+ "primary key (USER_GUID, BIZ_GUID)\n" 
			+ ")";

	/*
	 * 删除某一个表里面的记录
	 */
	static private String sqlOfDeleteRecord(String table) {
		String sql = "delete from " + table + " where ";
		return sql;
	}

	/*
	 * 查询标签
	 */
	static private String sqlOfSelectTag() {
		String sql = "select " + sqlFieldListTag + " from " + mTableNameOfTag;
		return sql;
	}

	/*
	 * 查询笔记
	 */
	static private String sqlOfSelectDocument(boolean joint) {
		String sql = "select " + sqlFieldListDocument + " from "
				+ mTableNameOfDocument + " where "
				+ selectNoteCondition();
		if (joint)
			return sql + " and ";
		else
			return sql;
	}

	/*
	 * 查询附件
	 */
	static private String sqlOfSelectAttachment() {
		String sql = "select " + sqlFieldListAttachment + " from "
				+ mTableNameOfAttachment;
		return sql;
	}

	/*
	 * 查询删除记录
	 */
	static private String sqlOfSelectDeletedGUID() {
		String sql = "select " + sqlFieldListDeletedGUID + " from "
				+ mTableNameOfDeleted;
		return sql;
	}

	/*
	 * 查询meta
	 */
	static private String sqlOfSelectMeta() {
		String sql = "select " + sqlFieldListMeta + " from " + mTableNameOfMeta;
		return sql;
	}

	/*
	 * 按照修改时间排序
	 */
	static private String sqlOfOrderByModifDesc() {
		return " order by DT_MODIFIED desc";
	}


	@SuppressLint("DefaultLocale")
	private String toUpperCase(String str) {
		return str.toUpperCase();
	}

	/*
	 * meta是否存在
	 */
	private boolean metaExists(String name, String key) {
		if (TextUtils.isEmpty(name) || TextUtils.isEmpty(key))
			return false;
		//
		name = toUpperCase(name);
		key = toUpperCase(key);
		String sql = sqlOfSelectMeta() + " where META_NAME="
				+ stringToSQLString(name) + " and META_KEY="
				+ stringToSQLString(key);
		return hasRecord(sql);
	}

	/*
	 * 设置meta
	 */

	private boolean setMeta(String name, String key, String value) {
		String sql = "";
		name = toUpperCase(name);
		key = toUpperCase(key);
		if (metaExists(name, key)) {
			sql = "update " + mTableNameOfMeta + " set META_VALUE="
					+ stringToSQLString(value) + " where META_NAME="
					+ stringToSQLString(name) + " and META_KEY="
					+ stringToSQLString(key);
		} else {
			sql = "insert into " + mTableNameOfMeta + " (" + sqlFieldListMeta
					+ ") values (" + stringToSQLString(name) + ", "
					+ stringToSQLString(key) + ", " + stringToSQLString(value)
					+ ")";
		}
		return execSql(sql);
	}

	/*
	 * 获取meta
	 */
	private String getMeta(String name, String key, String def) {
		name = toUpperCase(name);
		key = toUpperCase(key);
		String sql = "select META_VALUE from " + mTableNameOfMeta
				+ " where META_NAME=" + stringToSQLString(name)
				+ " and META_KEY=" + stringToSQLString(key);

		return sqlToString(sql, 0, def);
	}

	/*
	 * 设置meta
	 */
	public boolean setMetaInt(String name, String key, int value) {
		return setMeta(name, key, String.valueOf(value));
	}

	/*
	 * 获取val
	 */
	public int getMetaIntDef(String name, String key, int def) {

		String str = getMeta(name, key, String.valueOf(def));
		try {
			return Integer.parseInt(str, 10);
		} catch (Exception e) {
			return def;
		}
	}

	/*
	 * 清空表数据
	 */
	private boolean clearTable(String tabName) {
		String sql = "delete from " + stringToSQLString(tabName);
		return execSql(sql);
	}

	/*
	 * 获取笔记的某一个值
	 */
	private String getValueOfDocument(String guid, String key, String def) {

		String sql = "select " + key + " from " + mTableNameOfDocument
				+ " where DOCUMENT_GUID = " + stringToSQLString(guid);
		return sqlToString(sql, 0, def);
	}

	/*
	 * 关闭游标
	 */
	private void closeCursor(Cursor cursor) {
		try {
			if (cursor != null) {
				cursor.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/*
	 * 判断索引是否存在
	 */
	private boolean indexExists(String indexName, String indexTableName) {
		Cursor cursor = null;
		try {
			String sql = "PRAGMA index_list(" + indexTableName + ")";
			cursor = mDB.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				String indexNameSql = cursor.getString(1);
				if (indexName.equals(indexNameSql))
					return true;
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return false;
	}

	/*
	 * 创建一个索引
	 */
	private boolean createIndex(String indexName, String tableName,
			ArrayList<String> indexs) {
		try {
			String guidIndex = "create index " + indexName + " on " + tableName
					+ "(";
			for (String string : indexs)
				guidIndex = guidIndex + string + ",";

			if (guidIndex.lastIndexOf(",") == guidIndex.length() - 1)
				guidIndex = guidIndex.substring(0, guidIndex.length() - 1);

			return execSql(guidIndex + ");");
		} catch (Exception e) {
			return false;
		}

	}

	/*
	 * 检查一个索引，并自动创建
	 */
	private boolean checkIndex(String indexName, String tableName,
			ArrayList<String> indexs) {
		if (indexExists(indexName, tableName))
			return true;
		return createIndex(indexName, tableName, indexs);
	}
	
	public boolean checkColumn(String tableName, String columnName, String columnSqlType) {
		if (!columnExists(tableName, columnName)) {
			return execSql("alter table " + tableName
					+ "  add column " + columnName + " " + columnSqlType);
		}
		//
		return true;
	}

	/*
	 * 自动创建所有的数据表
	 */
	private boolean checkTables() {
		if (!checkTable(mTableNameOfDocument, sqlTableDocument))
			return false;
		checkIndex(mIndexNameOfDocument, mTableNameOfDocument,
				WizMisc.getNoteGuidIndexArray());
		if (!checkTable(mTableNameOfTag, sqlTableTag))
			return false;
		if (!checkTable(mTableNameOfLocation, sqlTableLocation))
			return false;
		if (!checkTable(mTableNameOfDeleted, sqlTableDeletedGUID))
			return false;
		if (!checkTable(mTableNameOfMeta, sqlTableMeta))
			return false;
		if (!checkTable(mTableNameOfAttachment, sqlTableDocumentAttachment))
			return false;
		if (!checkTable(mTableNameOfMessage, sqlTableMessage))
			return false;
		if (!checkTable(mTableNameOfUser, sqlTableUser))
			return false;
		//
		checkColumn(mTableNameOfLocation, "LOCATION_POS", "int");
		checkColumn(mTableNameOfDocument, "DOCUMENT_KEYWORDS", "varchar(300)");
		//
		return true;
	}

	public static enum TableColumnType {
		COLUMNTYPEINT, COLUMNTYPESTRING, COLUMNTYPEDOUBLE;
	}

	public boolean addTableColumn(String tableName, String columnName,
			TableColumnType type) {
		if (!columnExists(tableName, columnName)) {
			String sql = "alter table " + tableName + "  add column "
					+ columnName + " ";
			switch (type) {
			case COLUMNTYPEINT:
				sql += "int";
				break;
			case COLUMNTYPEDOUBLE:
				sql += "Double";
				break;
			case COLUMNTYPESTRING:
				sql += "varchar(255)";
				break;
			default:
				sql += "varchar(255)";
				break;
			}
			return execSql(sql);
		}
		return true;
	}

	/*
	 * 判断一列是否存在
	 */
	private boolean columnExists(String tableName, String columnName) {
		String sql = "select " + columnName + " from " + tableName
				+ " limit 0, 1";

		Cursor cursor = null;
		boolean exists = false;
		try {
			cursor = mDB.rawQuery(sql, null);
			int index = cursor.getColumnIndex(columnName);
			exists = (-1 != index);
			return exists;
		} catch (SQLiteException err) {
			return false;
		} finally {
			closeCursor(cursor);
		}
	}

	/*
	 * 检查table是否存在，并启动创建
	 */
	private boolean checkTable(String tableName, String tableSql) {
		if (tableExists(tableName))
			return true;
		//
		return execSql(tableSql);
	}

	/*
	 * 判断表是否存在
	 */
	private boolean tableExists(String tableName) {
		Cursor cursor = null;
		boolean exists = false;
		String sql = "select count(*) from sqlite_master where type='table' and tbl_name="
				+ stringToSQLString(tableName);
		try {
			cursor = mDB.rawQuery(sql, null);
			if (cursor.moveToNext()) {
				int count = cursor.getInt(0);
				exists = (count == 1);
			}
			return exists;
		} catch (SQLiteException err) {
			return false;
		} finally {
			closeCursor(cursor);
		}
	}

	/*
	 * 执行一条sql
	 */
	synchronized public boolean execSql(String sql) {
		boolean ret = false;
		try {
			mDB.execSQL(sql);
			ret = true;
		} catch (Exception err) {
			err.printStackTrace();
		}
		//
		return ret;
	}

	/*
	 * 判断是否有记录
	 */
	private boolean hasRecord(String sql) {
		boolean ret = false;
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, null);
			if (cursor.moveToNext()) {
				ret = true;
			}
		} catch (Exception err) {
			err.printStackTrace();
		} finally {
			closeCursor(cursor);
		}
		return ret;
	}

	/**
	 * 根据SQL语句以及所要获取字符串的偏移位置
	 * 查询表中的字符串列表
	 * @param sql
	 * @param index
	 * @return
	 */
	public ArrayList<String> sqlToStringArray(String sql, int index) {
		ArrayList<String> arr = new ArrayList<String>();
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				arr.add(cursor.getString(index));
			}
		} catch (Exception err) {
			err.printStackTrace();
		} finally {
			closeCursor(cursor);
		}
		//
		return arr;
	}

	/*
	 * sql到字符串集合
	 */
	public HashSet<String> sqlToStringSet(String sql, int index) {
		HashSet<String> set = new HashSet<String>();
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				set.add(cursor.getString(index));
			}
		} catch (Exception err) {
			err.printStackTrace();
		} finally {
			closeCursor(cursor);
		}
		//
		return set;
	}

	/*
	 * sql到字符串集合
	 */
	public HashMap<String, Integer> sqlToStringIntMap(String sql, int index1,
			int index2) {
		HashMap<String, Integer> ret = new HashMap<String, Integer>();
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				String key = cursor.getString(index1);
				int val = cursor.getInt(index2);
				//
				ret.put(key, val);

			}
		} catch (Exception err) {
			err.printStackTrace();
		} finally {
			closeCursor(cursor);
		}
		//
		return ret;
	}

	/*
	 * sql到int
	 */
	public int sqlToInt(String sql, int index, int def) {
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				int count = cursor.getInt(index);
				return count;
			}
		} catch (Exception err) {
		} finally {
			closeCursor(cursor);
		}
		return def;
	}

	/*
	 * sql到字符串
	 */
	public String sqlToString(String sql, int index, String def) {
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				return cursor.getString(index);
			}
			return def;
		} catch (Exception err) {
			err.printStackTrace();
			return def;
		} finally {
			closeCursor(cursor);
		}
	}

	// ////////////////////////////////////////////////////////////////////
	// documents count thread

	static private class WizDocumentsCountThread extends Thread {
		Context mContext;
		String mUserId;
		private LinkedList<String> mKbGuids = new LinkedList<String>();

		//
		WizDocumentsCountThread(Context ctx, String userId) {
			mContext = ctx;
			mUserId = userId;
			//
			WizEventsCenter.init();
		}

		private boolean isStop() {
			return WizStatusCenter.isStoppingDocumentsCountThread(mUserId);
		}

		public void refreshDocumentsCount(String kbGuid) {
			if (kbGuid == null)
				kbGuid = "";
			//
			synchronized (mKbGuids) {
				if (mKbGuids.contains(kbGuid))
					return;
				mKbGuids.add(kbGuid);
			}
		}

		//
		private String getNextKbGuid() {
			synchronized (mKbGuids) {
				//
				if (mKbGuids.isEmpty())
					return null;
				//
				return mKbGuids.poll();
			}
		}

		private void doIdle() {
			for (int i = 0; i < 5; i++) {
				if (isStop())
					return;
				//
				try {
					sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}

		@Override
		public void run() {
			//
			this.setPriority(MIN_PRIORITY);
			//
			while (!isStop()) {
				//
				try {
					String kbGuid = getNextKbGuid();
					//
					if (kbGuid == null) {
						doIdle();
						continue;
					}
					//
					WizDatabase db = WizDatabase.getDb(mContext, mUserId,
							kbGuid);
					if (db != null) {
						db.refreshDocumentsCountCache();
					}
				} catch (Exception e) {

				}
			}
		}
	}

	/*
	 * 笔记是否增删了
	 */
	private void setDocumentsModified(boolean b, boolean notifyMessage) {
		setDocumentsModified(mUserId, mKbGuid, b);
		if (b) {
			refreshDocumentsCount();
			if (notifyMessage) {
				WizEventsCenter.sendDatabaseRefreshObject(this,
						WizDatabaseObjectType.Document);
			}
		}
	}

	/*
	 * 返回笔记是否增删了
	 */
	private boolean isDocumentsModified() {
		return isDocumentsModified(mUserId, mKbGuid);
	}

	public static class DOCUMENT_COUNT_DATA {
		public int nSelf = 0;
		public int nIncludeChildren = 0;

		//
		DOCUMENT_COUNT_DATA() {
		}
	}

	static public class WizRange {
		public int start = 0;
		public int end = 0;
	}

	//
	private WizRange mTagDocumentsCountRange = new WizRange();
	private ConcurrentHashMap<String, Integer> mTagDocumentsCountMap = new ConcurrentHashMap<String, Integer>();
	private ConcurrentHashMap<String, DOCUMENT_COUNT_DATA> mFolderDocumentsCountMap = new ConcurrentHashMap<String, DOCUMENT_COUNT_DATA>();
	private ConcurrentHashMap<String, Integer> mDocumentAttachmentsCountMap = new ConcurrentHashMap<String, Integer>();

	//
	/*
	 * 刷新笔记数量
	 */
	private void refreshDocumentsCountCache() {
		if (!isDocumentsModified())
			return;
		//
		setDocumentsModified(false, false);
		//
		mFolderDocumentsCountMap = getFolderDocumentsCount();
		mTagDocumentsCountMap = getAllTagsDocumentCount();
		mDocumentAttachmentsCountMap = getAllDocumentAttachmentsCount();
		//
		WizEventsCenter.sendDatabaseRefreshObject(this,
				WizDatabaseObjectType.DocumentsCount);
	}

	/*
	 * 返回文件夹笔记数量
	 */
	private ConcurrentHashMap<String, DOCUMENT_COUNT_DATA> getFolderDocumentsCount() {
		//
		String sql = "select DOCUMENT_LOCATION as DOCUMENT_LOCATION_TEMP, count(*) as DOCUMENT_COUNT from WIZ_DOCUMENT group by DOCUMENT_LOCATION order by DOCUMENT_LOCATION";
		HashMap<String, Integer> mapOrg = sqlToStringIntMap(sql, 0, 1);

		ConcurrentHashMap<String, DOCUMENT_COUNT_DATA> mapRet = new ConcurrentHashMap<String, DOCUMENT_COUNT_DATA>();
		//
		for (String key : mapOrg.keySet()) {
			//
			DOCUMENT_COUNT_DATA data = new DOCUMENT_COUNT_DATA();
			data.nSelf = mapOrg.get(key);
			data.nIncludeChildren = data.nSelf;
			mapRet.put(key, data);
		}
		//
		for (String key : mapOrg.keySet()) {
			//
			int self = mapOrg.get(key);
			//
			WizLocation loc = new WizLocation(key);
			//
			while (true) {
				if (loc.isRoot())
					break;
				//
				String parentLocation = loc.getParent();
				if (parentLocation == null || parentLocation.length() == 0)
					break;
				//
				DOCUMENT_COUNT_DATA data = mapRet.get(parentLocation);
				//
				if (data == null) {
					data = new DOCUMENT_COUNT_DATA();
					mapRet.put(parentLocation, data);
				}
				//
				data.nIncludeChildren += self;
				//
				loc = new WizLocation(parentLocation);
			}
		}
		//
		return mapRet;
	}
	

	/*
	 * 返回笔记附件数量
	 */
	private ConcurrentHashMap<String, Integer> getAllDocumentAttachmentsCount() {
		//
		String sql = "select DOCUMENT_GUID, count(*) as DOCUMENT_COUNT from WIZ_DOCUMENT_ATTACHMENT group by DOCUMENT_GUID";
		//
		ConcurrentHashMap<String, Integer> ret = new ConcurrentHashMap<String, Integer>();
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				String key = cursor.getString(0);
				int val = cursor.getInt(1);
				//
				ret.put(key, val);

			}
		} catch (Exception err) {
			err.printStackTrace();
		} finally {
			closeCursor(cursor);
		}
		//
		return ret;
	}
	
	

	/*
	 * 给标签笔记数量+1
	 */
	private void increaseTagDocumentCount(
			ConcurrentHashMap<String, Integer> mapCount, String tagGuid) {
		Integer count = mapCount.get(tagGuid);
		if (count == null) {
			count = 0;
		}
		//
		count++;
		//
		mapCount.put(tagGuid, count);
	}

	/*
	 * 获取标签对应的笔记数量
	 */

	private ConcurrentHashMap<String, Integer> getAllTagsDocumentCount() {
		//
		String sql = "select DOCUMENT_TAG_GUIDS from WIZ_DOCUMENT where DOCUMENT_TAG_GUIDS<>'' and DOCUMENT_TAG_GUIDS is not null";

		ConcurrentHashMap<String, Integer> mapCount = new ConcurrentHashMap<String, Integer>();
		//
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, null);
			while (cursor.moveToNext()) {
				String tagsGuid = cursor.getString(0);
				//
				if (tagsGuid.indexOf('*') == -1) {
					increaseTagDocumentCount(mapCount, tagsGuid);
				} else {
					for (String tagGuid : WizMisc.string2HashSet(tagsGuid, '*')) {
						increaseTagDocumentCount(mapCount, tagGuid);
					}
				}
			}
		} catch (Exception err) {
			err.printStackTrace();
		} finally {
			closeCursor(cursor);
		}
		//
		mTagDocumentsCountRange.start = Integer.MAX_VALUE;
		mTagDocumentsCountRange.end = Integer.MIN_VALUE;
		//
		for (Integer count : mapCount.values()) {
			mTagDocumentsCountRange.start = Math.min(
					mTagDocumentsCountRange.start, count);
			mTagDocumentsCountRange.end = Math.max(mTagDocumentsCountRange.end,
					count);
		}
		//
		return mapCount;
	}

	//
	/*
	 * 刷新数据
	 */
	private void refreshDocumentsCount() {
		startDocumentsCountThread(mContext, mUserId);
		//
		WizDocumentsCountThread oldThread = (WizDocumentsCountThread) WizStatusCenter
				.getCurrentDocumentsCountThread(mUserId);
		if (oldThread == null)
			return;
		//
		oldThread.refreshDocumentsCount(mKbGuid);
	}

	//
	private void clearAllDatas() {
		clearTable(mTableNameOfTag);
		clearTable(mTableNameOfLocation);
		clearTable(mTableNameOfDocument);
		clearTable(mTableNameOfAttachment);
		clearTable(mTableNameOfMeta);
	}

	/*
	 * 起动线程
	 */
	static public void startDocumentsCountThread(Context ctx, String userId) {
		//
		Thread oldThread = WizStatusCenter
				.getCurrentDocumentsCountThread(userId);
		if (oldThread != null) {
			if (oldThread.isAlive())
				return;
		}
		//
		WizDocumentsCountThread newThread = new WizDocumentsCountThread(ctx,
				userId);
		//
		WizStatusCenter.setCurrentDocumentsCountThread(userId, newThread);
		//
		newThread.start();
	}

	/*
	 * 将笔记数据从一个账号完整复制到另外一个账号
	 */

	static public void copyAnonymousData(Context ctx, String newUserId)
			throws Exception {
		//
		copyAnonymousDataByCopyFile(ctx, newUserId);
	}

	static public void copyAnonymousDataByCopyFile(Context ctx, String newUserId)
			throws Exception {
		String newIndexFileName = getDbFileName(ctx, newUserId, "");
		String oldIndexFileName = getDbFileName(ctx, ANONYMOUS_USER_ID, "");
		//
		if (FileUtil.fileExists(newIndexFileName)) {
			throw new Exception(
					"Can't copy anonymous data by copy file, the dest file has already exists");
		}
		//
		if (!FileUtil.fileExists(oldIndexFileName)) {
			return;
		}
		//
		String newAccountPath = WizAccountSettings.getAccountPath(ctx, newUserId);
		String oldAccountPath = WizAccountSettings.getAccountPath(ctx, ANONYMOUS_USER_ID);
		//
		FileUtil.ensurePathExists(newAccountPath);
		//
		if (FileUtil.pathExists(oldAccountPath)) {
			if (!FileUtil.copyDirectory(oldAccountPath, newAccountPath)) {
				throw new Exception(
						"Can't copy anonymous data by copy file, can't copy anonymous files to new account");
			}
		}
		//
		FileUtil.copyFile(oldIndexFileName, newIndexFileName);
		//
		//
		WizDatabase db = WizDatabase.getDb(ctx, ANONYMOUS_USER_ID, null);
		db.clearAllDatas();
	}


	//	创建tags，根据传过来的字符串，用于批量创建tag
	public ArrayList<String> newTags(String tagsName) {
		ArrayList<String> tagArray = new ArrayList<String>();
		//
		if (TextUtils.isEmpty(tagsName)) {
			return tagArray;
		}
		//
		ArrayList<WizTag> newTags = new ArrayList<WizTag>();
		String[] tagName = WizMisc.string2Array(tagsName, "；。，;,");

		WizTag mCurrentTag;
		for (int i = 0; i < tagName.length; i++) {
			String name = tagName[i];
			if (TextUtils.isEmpty(name)) {
				continue;
			}
			mCurrentTag = getTagByName(name);
			if (mCurrentTag != null && tagArray.indexOf(mCurrentTag.guid) < 0) {
				tagArray.add(mCurrentTag.guid);
				continue;
			}

			mCurrentTag = newTag(name);
			newTags.add(mCurrentTag);
			tagArray.add(mCurrentTag.guid);
		}
		saveLocalTags(newTags);
		return tagArray;
	}

	private WizTag newTag(String name) {
		return newTag("", name);
	}

	private WizTag newTag(String parent, String name) {
		return newTag(parent, name, "");
	}

	public WizTag newTag(String parent, String name, String description) {
		WizTag mCurrentTag = new WizTag();
		mCurrentTag.guid = WizMisc.genGUID();
		mCurrentTag.description = description;
		mCurrentTag.name = name;
		mCurrentTag.parentGuid = parent;
		mCurrentTag.dateModified = TimeUtil.getCurrentSQLDateTimeString();
		return mCurrentTag;
	}
	
	
	/*------------------------------------------------message---------------------------------------*/
	
	/**
	 * 
	 * 保存服务器的消息
	 * 
	 * @param messages
	 * @throws Exception
	 */
	public void saveServerMessages(ArrayList<WizMessage> messages)
			throws Exception {
		try {
			mDB.beginTransaction();
			for (WizMessage msg : messages) {
				//
				if (!saveServerMessage(msg)) {
					throw new Exception("Can't save msg: " + msg.title);
				}
			}
			mDB.setTransactionSuccessful();
		} finally {
			mDB.endTransaction();
		}
		//
		WizEventsCenter.sendDatabaseRefreshObject(this,
				WizDatabaseObjectType.Message);
	}
	
	private boolean saveServerMessage(WizMessage msg) {
		WizMessage msgExists = getMessageByMsgId(msg.messageId);
		//
		if (msgExists == null) { // 新的消息
			return addMessage(msg);
		} else {
			return modifyMessage(msg);
		}
	}
	/**
	 * 
	 * 添加一条消息到数据库
	 * 
	 * @param msg
	 * @return
	 */
	private boolean addMessage(WizMessage msg) {

		String sql = "insert into " + mTableNameOfMessage + " ("
				+ sqlFieldListMessage + ") values ("
				+ longToSQLString(msg.messageId) + ", "
				+ stringToSQLString(msg.bizGuid) + ", "
				+ stringToSQLString(msg.kbGuid) + ", "
				+ stringToSQLString(msg.documentGuid) + ", "
				+ stringToSQLString(msg.title) + ", "
				+ stringToSQLString(msg.senderGuid) + ", "
				+ stringToSQLString(msg.senderId) + ", "
				+ stringToSQLString(msg.senderAlias) + ", "
				+ stringToSQLString(msg.receiverGuid) + ", "
				+ stringToSQLString(msg.receiverId) + ", "
				+ stringToSQLString(msg.receiverAlias) + ", "
				+ longToSQLString(msg.version) + ", "
				+ intToSQLString(msg.messageType) + ", "
				+ intToSQLString(msg.emailStatus) + ", "
				+ intToSQLString(msg.smsStatus) + ", "
				+ intToSQLString(msg.readCount) + ", "
				+ stringToSQLString(msg.dateCreated) + ", "
				+ stringToSQLString(msg.note) + ", "
				+ stringToSQLString(msg.body) + ", "
				+ intToSQLString(msg.localChanged) + ") ";
		return execSql(sql);
	}
	/**
	 * 更新一条消息
	 * 
	 * @param msg
	 * @return
	 */
	private boolean modifyMessage(WizMessage msg) {

		try {
			String sql = "update " + mTableNameOfMessage
					+ " set BIZ_GUID=" + stringToSQLString(msg.bizGuid)
					+ ", KB_GUID=" + stringToSQLString(msg.kbGuid)
					+ ", DOCUMENT_GUID=" + stringToSQLString(msg.documentGuid) 
					+ ", MESSAGE_TITLE=" + stringToSQLString(msg.title)
					+ ", SENDER_GUID=" + stringToSQLString(msg.senderGuid)
					+ ", SENDER_ID=" + stringToSQLString(msg.senderId)
					+ ", SENDER_ALIAS=" + stringToSQLString(msg.senderAlias)
					+ ", RECEIVER_GUID=" + stringToSQLString(msg.receiverGuid)
					+ ", RECEIVER_ID=" + stringToSQLString(msg.receiverId)
					+ ", RECEIVER_ALIAS=" + stringToSQLString(msg.receiverAlias)
					+ ", VERSION=" + longToSQLString(msg.version)
					+ ", MESSAGE_TYPE=" + intToSQLString(msg.messageType)
					+ ", EMAIL_STATUS=" + intToSQLString(msg.emailStatus)
					+ ", SMS_STATUS=" + intToSQLString(msg.smsStatus)
					+ ", READ_COUNT=" + intToSQLString(msg.readCount)
					+ ", DT_CREATED=" + stringToSQLString(msg.dateCreated)
					+ ", NOTE=" + stringToSQLString(msg.note)
					+ ", MESSAGE_BODY=" + stringToSQLString(msg.body) 
					+ ", LOCAL_CHANGED=" + intToSQLString(msg.localChanged) 
					+ "where MESSAGE_ID=" + longToSQLString(msg.messageId);
			return execSql(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//
		return false;
	}
	/**
	 * 
	 * 通过message id获取消息
	 * 
	 * @param messageId
	 * @return
	 */
	private WizMessage getMessageByMsgId(long messageId) {
		
		String sql = "select " + sqlFieldListMessage + " from " + mTableNameOfMessage + " where MESSAGE_ID = " + longToSQLString(messageId) ;
		//
		ArrayList<WizMessage> arr = sqlToMessages(sql, null);//new String[]{LongToSQLString(messageId)}
		if (!WizMisc.isEmptyArray(arr))
			return arr.get(0);
		
		return null;
	}
	private boolean mIsUnreadMessagesCountDirty = true;//默认是脏的
	public void setUnreadMessagesCountDirty(){
		mIsUnreadMessagesCountDirty = true;
	}
	private int mUnreadMessagesCount;
	public int getUnreadMessagesCount(){
		if(mIsUnreadMessagesCountDirty){
			mUnreadMessagesCount = getUnreadMessagesCountFromDb();//update
			mIsUnreadDocumentsCountDirty = false;
		}
		return mUnreadMessagesCount;
		
	}
	private int getUnreadMessagesCountFromDb(){
		String sql = "select count(*) from " + mTableNameOfMessage + " where READ_COUNT = 0";
		Cursor cursor = null ;
		try{
			cursor = mDB.rawQuery(sql, null);
			int num = 0 ;
			if (cursor.moveToNext()) {
				num = cursor.getInt(0);
			}
			return num ;
		}finally{
			closeCursor(cursor);
		}
	}
	/**
	 * 
	 * @param messageId
	 * @return
	 */
	public boolean setMessageReaded(WizMessage message){
		message.readCount++ ;
		String sql = "update " + mTableNameOfMessage + " set READ_COUNT = READ_COUNT + 1, LOCAL_CHANGED = 1 where MESSAGE_ID = " + longToSQLString(message.messageId) ;
		boolean isSuccess = execSql(sql);
		if(isSuccess){
			setUnreadMessagesCountDirty();
			List<WizObjectBase> messages = new ArrayList<WizObjectBase>();
			messages.add(message);
			WizEventsCenter.sendReadStausChangedMessage(messages);
		}
		return isSuccess;
	}
	/**
	 * 批量设置消息已读
	 * @param messageIds
	 * @return
	 */
	public boolean setMessagesReaded(List<WizMessage> messages) {
		//
		boolean isSuccess = false;
		
		StringBuffer sql = new StringBuffer();
		sql.append("update ")
		.append(mTableNameOfMessage)
		.append(" set READ_COUNT = READ_COUNT + 1, LOCAL_CHANGED = 1 where MESSAGE_ID in (") ;
		int length = messages.size();
		for(int i = 0 ; i < length; i++){
			WizMessage message = messages.get(i);
			message.readCount++ ;
			sql.append(longToSQLString(message.messageId));
			if(i < length -1){
				sql.append(",");
			}else{
				sql.append(")");
			}
		}
		
		if(execSql(sql.toString())){
			isSuccess = true;
		}
		//成功即发消息通知并返回成功。
		if(isSuccess){
			setUnreadMessagesCountDirty();
			List<WizObjectBase> objs = new ArrayList<WizObjectBase>(messages);
			WizEventsCenter.sendReadStausChangedMessage(objs);
		}
		return isSuccess;
	}
	/**
	 * 全部未读标为已读   
	 * @return
	 */
	public boolean setAllUnreadMessagesReaded() {
		boolean isSuccess = false;
//		缺陷：多次置全部已读导致数目不断增加
//		String sql = "update " + mTableNameOfMessage + " set READ_COUNT = READ_COUNT + 1, LOCAL_CHANGED = 1 ";
		String sql = "update " + mTableNameOfMessage + " set READ_COUNT = 1, LOCAL_CHANGED = 1 where READ_COUNT <= 0";
		//
		if(execSql(sql)){
			isSuccess = true;
		}
		//成功即发消息通知并返回成功。
		if(isSuccess){
			setUnreadMessagesCountDirty();
			WizEventsCenter.sendReadStausChangedMessage(null);
		}
		return isSuccess;
	}
	
	//
	/**
	 * 标记全部笔记为已读，全部置为1
	 */
	public boolean markAllMessagesAsRead() {
		boolean isSuccess = false;
		//
		String sql = "update " + mTableNameOfMessage + " set READ_COUNT = 1, LOCAL_CHANGED = 1 ";
		//
		if(execSql(sql)){
			isSuccess = true;
		}
		//成功即发消息通知并返回成功。
		if(isSuccess){
			setUnreadMessagesCountDirty();
		}
		return isSuccess;
	}
	public ArrayList<WizMessage> getModifiedUnreadMessages(){
		return getModifiedReadStatusMessages("READ_COUNT = 0");
	}
	public ArrayList<WizMessage> getModifiedReadedMessages(){
		return getModifiedReadStatusMessages("READ_COUNT > 0 ");
	}
	private ArrayList<WizMessage> getModifiedReadStatusMessages(String readStatusSearchSql){
		String sql = "select " + sqlFieldListMessage + " from " + mTableNameOfMessage + " where LOCAL_CHANGED = 1 and " + readStatusSearchSql;
		return sqlToMessages(sql, null);
	}
	
	public void onUploadedReadStatus(String[] ids){
		for(String id : ids){
			String sql = "update " + mTableNameOfMessage + " set LOCAL_CHANGED = 0 where MESSAGE_ID = " + stringToSQLString(id) ;
			execSql(sql);
		}
	}

	/**
	 * 
	 * 通过群组获取近期消息
	 * 
	 * @param kbGuid
	 * @return
	 */
	public ArrayList<WizMessage> getMessagesByGroup(String kbGuid) {
		if(TextUtils.isEmpty(kbGuid)){
			return getMessages();//
		}
		return getMessagesByColumn("KB_GUID", kbGuid);
	}
	/*
	 * 按照时间排序，获取消息
	 */
	private ArrayList<WizMessage> getMessagesByColumn(String columnName, String columnArgs) {
		String sql = "select " + sqlFieldListMessage + " from "
				+ mTableNameOfMessage 
				+" where " + columnName + " = ?" 
				+" order by DT_CREATED desc";
		return sqlToMessages(sql, new String[]{columnArgs});
	}
	/*
	 * 按照时间排序，获取消息
	 */
	private ArrayList<WizMessage> getMessages() {
		String sql = "select " + sqlFieldListMessage + " from "
				+ mTableNameOfMessage 
				+" order by DT_CREATED desc";
		return sqlToMessages(sql, null);
	}
	/**
	 * 	查询数据库消息
	 * 
	 * @param sql
	 * @param selectionArgs
	 * @return
	 */
	private ArrayList<WizMessage> sqlToMessages(String sql,String[] selectionArgs) {
		ArrayList<WizMessage> arr = new ArrayList<WizMessage>();
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, selectionArgs);
			while (cursor.moveToNext()) {
				WizMessage msg = new WizMessage();
				msg.messageId = cursor.getLong(0);
				msg.bizGuid = cursor.getString(1);
				msg.kbGuid = cursor.getString(2);
				msg.documentGuid = cursor.getString(3);
				msg.title = cursor.getString(4);
				msg.senderGuid = cursor.getString(5);
				msg.senderId = cursor.getString(6);
				msg.senderAlias = cursor.getString(7);
				msg.receiverGuid = cursor.getString(8);
				msg.receiverId = cursor.getString(9);
				msg.receiverAlias = cursor.getString(10);
				msg.version = cursor.getLong(11);
				msg.messageType = cursor.getInt(12);
				msg.emailStatus = cursor.getInt(13);
				msg.smsStatus = cursor.getInt(14);
				msg.readCount = cursor.getInt(15);
				msg.dateCreated = cursor.getString(16);
				msg.note = cursor.getString(17);
				msg.body = cursor.getString(18);
				msg.localChanged = cursor.getInt(19);
				arr.add(msg);
			}
			
		} catch (Exception err) {
			err.printStackTrace();
		} finally {
			closeCursor(cursor);
		}
		return arr;
	}
	public boolean deleteMessageByMessageId(long messageId){
		String sql = "delete from " + mTableNameOfMessage + " where MESSAGE_ID = " + longToSQLString(messageId) ;
		return execSql(sql);
	}
	/*------------------------------------------------user---------------------------------------*/
	
	/**
	 * 设置Biz成员的版本号
	 */
	public long getBizMemberVersion(String key) {
		String str = getMeta(SyncVersionOfBizMember, key, "0");
		//
		return Long.parseLong(str, 10);
	}

	/**
	 * 设置Biz成员的版本号
	 */
	public boolean setBizMemberVersion(String key, long ver) {
		String verString = Long.toString(ver);
		return setMeta(SyncVersionOfBizMember, key, verString);
	}
	
	/**
	 * 获取得到biz成员的key
	 * @return
	 */
	public Set<String> getBizKeys(){
		Set<String> keys = new HashSet<String>();
		for(int i = 0 ; i < getKbCount();i++){
			String key = getKbValue(i, keyOfGroupBizGuid, "");
			if(key.length() != 0 ){
				keys.add("biz_users/"+key);
			}
		}
		return keys;
	}
	/**
	 * 保存返回的关于biz成员的key
	 * @param ret
	 */
	public void saveBizMemberValue(String key, WizKeyValue ret){
		String bizGuid = key.replaceAll("biz_users/", "");
		ArrayList<WizUser> users= new ArrayList<WizUser>();
		try {
			JSONArray arr = new JSONArray(ret.value);
			for(int i = 0 ; i < arr.length() ; i++){
				WizUser user = new WizUser();
				JSONObject obj = arr.getJSONObject(i);
				user.alias = obj.getString("alias");
				user.pinyin = obj.getString("pinyin");
				user.userGuid = obj.getString("user_guid");
				user.userId = obj.getString("user_id");
				user.bizGuid =  bizGuid;
				users.add(user);
			}
			deleteUsersByBizGuid(bizGuid);
			saveServerUsers(users);
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private boolean deleteUsersByBizGuid(String bizGuid){
		String sql = "delete from " + mTableNameOfUser + " where BIZ_GUID = " + stringToSQLString(bizGuid) ;
		return execSql(sql);
	}
	private void saveServerUsers(ArrayList<WizUser> users)
			throws Exception {
		try {
			mDB.beginTransaction();
			for (WizUser user : users) {
				//
				if (!saveServerUser(user)) {
					throw new Exception("Can't save user: " + user.userId);
				}
			}
			mDB.setTransactionSuccessful();
		} finally {
			mDB.endTransaction();
		}
		//
	}
	
	private boolean saveServerUser(WizUser user) {
		WizUser userExists = getUserByUserGuidAndBizGuid(user.userGuid, user.bizGuid);
		//
		if (userExists == null) { // 新的消息
			return addUser(user);
		} else {
			return modifyUser(user);
		}
	}
	private WizUser getUserByUserGuidAndBizGuid(String userGuid, String bizGuid) {
		
		String sql = "select " + sqlFieldListUser + " from " + mTableNameOfUser + " where USER_GUID = " + stringToSQLString(userGuid) +"and BIZ_GUID = " + stringToSQLString(bizGuid);
		//
		ArrayList<WizUser> arr = sqlToUsers(sql, null);
		if (!WizMisc.isEmptyArray(arr))
			return arr.get(0);
		
		return null;
	}

	/**
	 * 通过bizGuid查找到其中的所有用户。
	 * @param bizGuid
	 * @return  如果bizGuid为空，返回null
	 */
	public HashMap<String, WizUser> getUsersByBizGuid(String bizGuid) {
		if(TextUtils.isEmpty(bizGuid))
			return null ;

		String sql = "select " + sqlFieldListUser + " from " + mTableNameOfUser + " where BIZ_GUID = " + stringToSQLString(bizGuid);

		//当且仅当为个人数据库时才会有相应的WizUser数据
		WizDatabase personalDatabase = WizDatabase.getDb(mContext, mUserId, null);
		ArrayList<WizUser> arr = personalDatabase.sqlToUsers(sql, null);

		HashMap<String, WizUser> users = new HashMap<String, WizObject.WizUser>();
		for(WizUser user : arr){
			users.put(user.userId, user);
		}
		return users;
	}
	/**
	 * 
	 * 添加一条用户信息
	 * 
	 * @param msg
	 * @return
	 */
	private boolean addUser(WizUser user) {

		String sql = "insert into " + mTableNameOfUser + " ("
				+ sqlFieldListUser + ") values ("
				+ stringToSQLString(user.userGuid) + ", "
				+ stringToSQLString(user.bizGuid) + ", "
				+ stringToSQLString(user.userId) + ", "
				+ stringToSQLString(user.alias) + ", "
				+ stringToSQLString(user.pinyin) + ") ";
		return execSql(sql);
	}
	/**
	 * 更新一个用户信息
	 * 
	 * @param msg
	 * @return
	 */
	private boolean modifyUser(WizUser user) {

		try {
			String sql = "update " + mTableNameOfUser
					+ " set USER_ID=" + stringToSQLString(user.userId)
					+ ", BIZ_GUID=" + stringToSQLString(user.bizGuid)
					+ ", USER_ALIAS=" + stringToSQLString(user.alias)
					+ ", USER_PINYIN=" + stringToSQLString(user.pinyin) 
					+ "where USER_GUID=" + stringToSQLString(user.userGuid);
			return execSql(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//
		return false;
	}
	/**
	 * 	查询数据库biz用户信息
	 * 
	 * @param sql
	 * @param selectionArgs
	 * @return
	 */
	private ArrayList<WizUser> sqlToUsers(String sql, String[] selectionArgs) {
		ArrayList<WizUser> arr = new ArrayList<WizUser>();
		Cursor cursor = null;
		try {
			cursor = mDB.rawQuery(sql, selectionArgs);
			while (cursor.moveToNext()) {
				WizUser user = new WizUser();
				user.userGuid = cursor.getString(0);
				user.bizGuid = cursor.getString(1);
				user.userId = cursor.getString(2);
				user.alias = cursor.getString(3);
				user.pinyin = cursor.getString(4);
				arr.add(user);
			}

		} catch (Exception err) {
			err.printStackTrace();
		} finally {
			closeCursor(cursor);
		}
		return arr;
	}
	/**
	 * 根据过滤条件，模糊查询得到某Biz成员
	 *
	 */
	public ArrayList<WizUser> getBizMembers(String selectionArg, String bizGuid){
		String sql ="select " + sqlFieldListUser + " from " + mTableNameOfUser +" where (USER_ALIAS like '" + selectionArg + "%' or USER_PINYIN like '" + selectionArg + "%'or USER_ID like '"+ selectionArg + "%') and BIZ_GUID = " + stringToSQLString(bizGuid) + " group by USER_GUID order by USER_PINYIN" ;
		return WizDatabase.getDb(mContext, mUserId, null).sqlToUsers(sql, null);
	}
	/**
	 * 得到某biz成员全部列表
	 * @return
	 */
	public ArrayList<WizUser> getAllBizMembers(String bizGuid){
		String sql = "select " + sqlFieldListUser + " from " + mTableNameOfUser + " where BIZ_GUID = " + stringToSQLString(bizGuid) + " order by USER_PINYIN ";
		return WizDatabase.getDb(mContext, mUserId, null).sqlToUsers(sql, null);
	}
}
