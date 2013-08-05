package cn.wiz.sdk.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.Collator;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import cn.wiz.sdk.api.WizStrings.WizStringId;
import cn.wiz.sdk.db.WizDatabase;
import cn.wiz.sdk.settings.WizAccountSettings;
import cn.wiz.sdk.util.FileUtil;
import cn.wiz.sdk.util.WizMisc;

public class WizObject {

	// data info
	//public static final String DATA_EXTRAS_STRING_PATH = "notePath";
	public static final String DATA_TYPE_ATTACHMENT = "attachment";
	public static final String DATA_TYPE_LOCATION = "location";
	public static final String DATA_TYPE_DOCUMENT = "document";
	public static final String DATA_TYPE_TAG = "tag";

	static public class WizUserInfo {
		public String token;
		public String personalKbGuid;
		public String userGuid;
		public String personalKbDatabaseUrl;
		public String inviteCode;
		public String userType;
		public String displayName;
		public String userLevel;
		public String userLevelName;
		public String nickName;
		public String email;
		public String mobile;
		public String userPoints;

	}

	static public class WizCert {
		public String n;
		public String e;
		public String encryptedD;
		public String hint;
	}

	static public abstract class WizKb implements Comparable<WizKb> {
		public String name;
		public String bizName;
		public String bizGuid;
		public String kbGuid;
		public String kbDatabaseUrl;
		public int userRole;

		//
		abstract public boolean isPersonalKb();

		//
		public boolean isBizGroupKb() {
			if (bizName == null)
				return false;
			//
			if (0 == bizName.length())
				return false;
			//
			return true;
		}

		//
		public int compareTo(WizKb another) {
			//
			boolean personal1 = isPersonalKb();
			boolean personal2 = another.isPersonalKb();
			//
			if (personal1 && !personal2)
				return -1;
			else if (!personal1 && personal2)
				return 1;
			//
			String biz1 = bizName;
			if (biz1 == null)
				biz1 = "";
			String biz2 = another.bizName;
			if (biz2 == null)
				biz2 = "";
			//
			Collator cmp = java.text.Collator.getInstance();
			//
			if (biz1.length() > 0 && biz2.length() == 0) // 一个是biz群组，一个是个人群组
				return -1;
			else if (biz1.length() == 0 && biz2.length() > 0) // 一个是biz群组，一个是个人群组
				return 1;
			else if (biz1.length() > 0 && // 都是biz群组
					biz2.length() > 0) {
				int bizRet = cmp.compare(biz1, biz2); // 优先按照biz排序
				if (0 != bizRet)
					return bizRet;
			}
			//
			return cmp.compare(name, another.name);
		}

		/**
		 * user group: 权限管理 0--管理员， 10--超级用户， 50--超级编辑， 100--普通编辑， 1000--只读用户
		 */

		public static final int GROUP_ROLE_OWNER = -1;
		public static final int GROUP_ROLE_ADMIN = 0;
		public static final int GROUP_ROLE_SUPER = 10;
		public static final int GROUP_ROLE_EDITOR = 50;
		public static final int GROUP_ROLE_AUTOR = 100;
		public static final int GROUP_ROLE_READER = 1000;

		public boolean isOwner() {
			return userRole <= GROUP_ROLE_OWNER;
		}

		public boolean isAdmin() {
			return userRole <= GROUP_ROLE_ADMIN;
		}

		public boolean isSuper() {
			return userRole <= GROUP_ROLE_SUPER;
		}

		public boolean isEditor() {
			return userRole <= GROUP_ROLE_EDITOR;
		}

		public boolean isAuthor() {
			return userRole <= GROUP_ROLE_AUTOR;
		}

		public boolean isReader() {
			return userRole <= GROUP_ROLE_READER;
		}
	}

	static public class WizPersonalKb extends WizKb {
		public WizPersonalKb(String databaseUrl, String guid,
				String personalKbName) {
			kbDatabaseUrl = databaseUrl;
			kbGuid = guid;
			name = personalKbName;
			userRole = -1;
		}

		public boolean isPersonalKb() {
			return true;
		}
	}

	//
	static public class WizGroupKb extends WizKb {
		public boolean isPersonalKb() {
			return false;
		}
	}

	static public class WizKbVersion {
		public long documentVersion = -1;
		public long tagVersion = -1;
		public long deletedVersion = -1;
		public long attachmentVersion = -1;
		public long styleVersion = -1;
	}
	public static class WizMessage extends WizObjectBase{
		public long messageId;
		public String bizGuid;
		public String kbGuid;
		public String documentGuid;
		public String senderGuid;
		public String senderId;
		public String senderAlias;
		public String receiverGuid;
		public String receiverId;
		public String receiverAlias;
		public int messageType;
		public int emailStatus;
		public int smsStatus;
		public int readCount;
		public String dateCreated;
		public String title;
		public String note;
		public String body;
		public int localChanged;
		public WizMessage(){
			localChanged = 0;
		}
	}
	public static class WizUser{
		public String userGuid;
		public String bizGuid;
		public String userId;
		public String alias;
		public String pinyin;
	}
	static public class WizObjectBase {
		public long version = 0;
	}

	static public class WizDeletedGUID extends WizObjectBase {
		public String guid;
		public String type;
		public String dateDeleted;
	}

	static public class WizTag extends WizObjectBase implements
			Comparable<WizTag> {
		public String name;
		public String guid;
		public String parentGuid;
		public String description;
		public String dateModified;
		private int documentsCount = -1;

		//
		public WizTag() {
			version = 0;
		}

		public WizTag(String tagName, String guid) {
			this.name = tagName;
			this.guid = guid;
		}
		public WizTag(WizTag another) {
			name = another.name;
			guid = another.guid;
			parentGuid = another.parentGuid;
			description = another.description;
			dateModified = another.dateModified;
			version = another.version;
		}


		public int getDocumentsCountFromCache(WizDatabase db) {
			documentsCount = db.getDocumentsCountFromCache(this);
			return documentsCount;
		}

		public int getDocumentsCountFromDb(WizDatabase db) {
			documentsCount = db.getDocumentsCountFromDb(this);
			return documentsCount;
		}

		public int compareTo(WizTag another) {
			Collator cmp2 = java.text.Collator.getInstance();
			return cmp2.compare(name, another.name);
		}
		
		//
		public String getFullPath(WizDatabase db) {
			String ret = "/";
			//
			try {
				WizTag tag = this;
				//
				while (tag != null) {
					ret = "/" + tag.name + ret;
					if (TextUtils.isEmpty(tag.parentGuid))
						break;
					//
					tag = db.getTagByGuid(tag.parentGuid);
				}
				//
				return ret;
			}
			catch (Exception e) {
				return ret;
			}
		}
	}

	static public class WizDocument extends WizObjectBase implements
			Comparable<WizDocument> {
		public String guid;
		public String title;
		public String location;
		public String url;
		public String tagGUIDs;
		public String type;
		public String fileType;
		public String dataMd5;
		public String dateCreated;
		public String dateModified;
		public int attachmentCount;
		public int serverChanged;
		public int localChanged;
		public double gpsLatitude;
		public double gpsLongtitude;
		public double gpsAltitude;
		public double gpsDop;
		public String gpsAddress;
		public String gpsCountry;
		public String gpsDescription;
		public String owner;
		public int readCount;
		public int asterisk;
		public boolean encrypted;
		public String keywords;

		//
		public WizDocument() {
			attachmentCount = 0;
			serverChanged = 0;
			localChanged = 0;
			version = 0;
			encrypted = false;
		}

		public WizDocument(WizDocument another) {

			guid = another.guid;
			title = another.title;
			location = another.location;
			url = another.url;
			tagGUIDs = another.tagGUIDs;
			type = another.type;
			fileType = another.fileType;
			dataMd5 = another.dataMd5;
			dateCreated = another.dateCreated;
			dateModified = another.dateModified;
			attachmentCount = another.attachmentCount;
			serverChanged = another.serverChanged;
			localChanged = another.localChanged;
			gpsLatitude = another.gpsLatitude;
			gpsLongtitude = another.gpsLongtitude;
			gpsAltitude = another.gpsAltitude;
			gpsDop = another.gpsDop;
			gpsAddress = another.gpsAddress;
			gpsCountry = another.gpsCountry;
			gpsDescription = another.gpsDescription;
			owner = another.owner;
			readCount = another.readCount;
			asterisk = another.asterisk;
			encrypted = another.encrypted;
			keywords = another.keywords;
		}

		// localChange of note
		public static final int DOCUMENT_LOCAL_CHANGE_INFO = 2;
		public static final int DOCUMENT_LOCAL_CHANGE_DATA = 1;
		public static final int DOCUMENT_LOCAL_CHANGE_NULL = 0;
		// serverChange of note
		public static final int DOCUMENT_SERVER_CHANGE_NULL = 0;
		public static final int DOCUMENT_SERVER_CHANGE_DATA = 1;

		// mobile default about document
		public static final String noteFileTypeOfAtt = "att";
		public static final String notetypeOfDocument = "document";
		public static final String mDeletedDirectory = "/Deleted Items/";

		@Override
		public int compareTo(WizDocument another) {
			Collator cmp2 = java.text.Collator.getInstance();
			if (title == null || another.title == null) {
				return cmp2.compare(dateModified, another.dateModified);
			}
			//
			return cmp2.compare(title, another.title);
		}

		// 返回用于编辑的笔记路径
		static public String getEditNotePath(Context ctx, boolean create) throws Exception {
			String notePath = FileUtil.getCacheRootPath(ctx);
			notePath = FileUtil.pathAddBackslash(notePath);
			notePath = notePath + "note_edit";
			if (create) {
				if(!FileUtil.ensurePathExists(notePath)){// 创建目录
					throw new Exception("Can't create cache folder: " + notePath);
				
				}
			}
			return FileUtil.pathAddBackslash(notePath);
		}

		// 返回用于编辑的笔记路径
		public String getEditNotePath(Context ctx) throws Exception {
			return getEditNotePath(ctx, true);
		}

		public String getEditNoteIndexFilePath(Context ctx, boolean create) throws Exception {
			String path = getEditNotePath(ctx);
			path = FileUtil.pathAddBackslash(path);
			path = path + "index_files";
			if (create) {
				FileUtil.ensurePathExists(path);// 创建目录
			}
			return FileUtil.pathAddBackslash(path);
		}

		public String getEditNoteIndexFilePath(Context ctx) throws Exception {
			String path = getEditNoteIndexFilePath(ctx, true);
			return FileUtil.pathAddBackslash(path);
		}

		/*
		 * 用于编辑或者保存笔记的时候，通过这个文件以及里面的资源压缩后生成ziw文件
		 */
		public String getEditNoteFileName(Context ctx) throws Exception {
			String file = getEditNotePath(ctx);
			file = FileUtil.pathAddBackslash(file);
			return file + "index.html";
		}

		// 返回用于阅读的笔记路径
		static public String getNotePath(Context ctx, String documentGuid,
				boolean create) throws Exception {
			String notePath = FileUtil.getCacheRootPath(ctx);
			notePath = FileUtil.pathAddBackslash(notePath);
			notePath = notePath + documentGuid;
			if (create) {
				if (!FileUtil.ensurePathExists(notePath)) {
					// 创建目录
					throw new Exception("Can't create cache folder: " + notePath);
				}
			}
			return FileUtil.pathAddBackslash(notePath);
		}

		// 用于阅读的笔记路径
		public String getNotePath(Context ctx, boolean create) throws Exception {
			return getNotePath(ctx, guid, create);
		}

		public String getNotePatth(Context ctx) throws Exception {
			return getNotePath(ctx, true);
		}

		// 返回
		public String getNoteFileName(Context ctx) throws Exception {
			String file = getNotePatth(ctx);
			file = FileUtil.pathAddBackslash(file);
			return file + "index.html";
		}

		// 返回ziw文件路径
		static public String getZipFileName(Context ctx, String userId,
				String documentGuid) {

			String basePath = WizAccountSettings.getAccountPath(ctx, userId);
			basePath = FileUtil.pathAddBackslash(basePath);
			return basePath + documentGuid + ".ziw";
		}

		// 返回ziw文件路径
		public String getZipFileName(Context ctx, String userId) {
			return getZipFileName(ctx, userId, guid);
		}

		public File getZipFile(Context ctx, String userId) {
			return new File(getZipFileName(ctx, userId));
		}

		public WizDataStatus getDocumentStatus(Context ctx, String userId)
				throws Exception {
			if (TextUtils.isEmpty(guid))
				throw new Exception("Note id is empty");
			//
			if (this == null)
				throw new Exception("Note is null");
			//
			String zipFileName = getZipFileName(ctx, userId);
			File zipFile = getZipFile(ctx, userId);
			if (zipFile.length() <= 0) {
				zipFile.delete();
				return WizDataStatus.DOWNLOADDATA;
			}

			String viewFileName = getNoteFileName(ctx);
			boolean isServerChanged = this.serverChanged == 1;
			boolean existsZipFile = FileUtil.fileExists(zipFileName);
			if (isServerChanged || !existsZipFile) {
				return WizDataStatus.DOWNLOADDATA;
			} else if (FileUtil.fileExists(viewFileName)) {
				return WizDataStatus.VIEWDATA;
			} else if (isEncrypted(ctx, userId)) {
				return WizDataStatus.DECRYPTIONDATA;
			} else {
				return WizDataStatus.UNZIPDATA;
			}
		}

		public boolean isEncrypted(Context ctx, String userId) {
			File zipFile = getZipFile(ctx, userId);
			if (!zipFile.exists())
				return false;
			if (zipFile.length() <= 0) {
				return false;
			}
			FileInputStream in = null;
			try {
				in = new FileInputStream(zipFile);
				byte[] data = new byte[4];
				in.read(data);
				encrypted = isEncrypted(data);
			} catch (Exception e) {
			} finally {
				try {
					in.close();
				} catch (IOException e) {
				}
			}
			return encrypted;
		}

		public boolean isEncrypted(byte[] data) {
			return data != null && data[0] == 90 && data[1] == 73
					&& data[2] == 87 && data[3] == 82;
		}
	}

	static public enum WizDataStatus {

		DOWNLOADDATA("download"), UNZIPDATA("unzip"), DECRYPTIONDATA(
				"Decryption"), VIEWDATA("view");

		private String status;

		private WizDataStatus(String status) {
			this.status = status;
		}

		public String toString() {
			return status;
		}
	}

	static public class WizAttachment extends WizObjectBase implements Serializable{

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		public String guid;
		public String docGuid;
		public String name;
		public String description;
		public String dataMd5;
		public String dateModified;
		public int serverChanged;
		public int localChanged;
		public String location;

		public WizAttachment() {
			serverChanged = 0;
			localChanged = 0;
			version = 0;
		}

		// 获取解压后的附件文件名
		public String getAttachmentFileName(Context ctx) throws Exception {
			String notePath = WizDocument.getNotePath(ctx, docGuid, true);
			return FileUtil.pathAddBackslash(notePath) + name;
		}

		// 获取附件编辑的路径
		static public String getEditNoteAttachmentFileName(Context ctx,
				String documentGuid, String fileName) throws Exception {
			String editNotePath = WizDocument.getEditNotePath(ctx, true);
			editNotePath = FileUtil.pathAddBackslash(editNotePath);
			return editNotePath + fileName;
		}

		// 获取附件编辑的路径
		public String getEditNoteAttachmentFileName(Context ctx) throws Exception {
			return getEditNoteAttachmentFileName(ctx, docGuid, name);
		}

		//
		// 返回zip文件路径
		public String getZipFileName(Context ctx, String userId) {

			String basePath = WizAccountSettings.getAccountPath(ctx, userId);
			basePath = FileUtil.pathAddBackslash(basePath);
			return basePath + guid + ".ziw";
		}

		public File getZipFile(Context ctx, String userId) {
			return new File(getZipFileName(ctx, userId));
		}

		public WizDataStatus getAttachmentStatus(Context ctx, String userId) throws Exception {
			boolean serverChanged = this.serverChanged == 1;
			String zip = getZipFileName(ctx, userId);
			String file = getAttachmentFileName(ctx);
			if (serverChanged || !FileUtil.fileExists(zip)) {
				return WizDataStatus.DOWNLOADDATA;
			} else if (FileUtil.fileExists(file)) {
				return WizDataStatus.VIEWDATA;
			} else {
				return WizDataStatus.UNZIPDATA;
			}

		}
		
		// localChange of attahcment
		public static final int ATTACHMENT_LOCAL_CHANGE_NULL = 0;
		public static final int ATTACHMENT_LOCAL_CHANGE_DATA = 1;
		// serverChange of attachment
		public static final int ATTACHMENT_SERVER_CHANGE_NULL = 0;
		public static final int ATTACHMENT_SERVER_CHANGE_DATA = 1;
	}

	static public class WizAbstractData {
		public String abstractText;
		public Bitmap abstractImage;
	}

	static public class WizAbstract extends WizAbstractData {
		public String documentGuid;
		public String abstractType;
		public long lastAccessed = 0;

		//
		public WizAbstract(String guid, String type, String text, Bitmap img) {
			documentGuid = guid;
			abstractType = type;
			abstractText = text;
			abstractImage = img;
		}
	}
	static public class WizAvatar{
		public String userGuid;
		public Bitmap bitmap;
		public long lastModified;
		//
		public WizAvatar() {
		}
		public WizAvatar(String userGuid,Bitmap bitmap, long lastModified) {
			this.userGuid = userGuid;
			this.bitmap = bitmap;
			this.lastModified = lastModified;
		}
	}

	/**
	 * 账户信息
	 * @author zwy
	 */
	static public class WizAccount {
		public String accountUserId; // 账号id
		public String accountPassword; // 账号密码
		public String accountUserGuid;//账户唯一ID：userGuid
		public String accountDataFolder;//账户数据存储路径关键字
	}

	static public class WizLocation implements Comparable<WizLocation> {
		private String location;
		public int pos = 0;

		public WizLocation(String location) {
			this.location = location;
		}

		public WizLocation(String location, int pos) {
			this.location = location;
			this.pos = pos;
		}

		public WizLocation(WizLocation loc) {
			this.location = loc.location;
			this.pos = loc.pos;
		}

		public int getDocumentsCountFromCache(WizDatabase db) {
			return db.getDocumentsCountFromCache(this);
		}

		public int getDocumentsCountFromDb(WizDatabase db) {
			return db.getDocumentsCountFromDb(this);
		}

		//
		public int compareTo(WizLocation another) {
			//
			if (another.pos > 0 && pos == 0) {
				return 1;
			} else if (another.pos == 0 && pos > 0) {
				return -1;
			}
			//
			if (another.pos != pos) {
				return pos - another.pos;
			}
			//
			Collator cmp2 = java.text.Collator.getInstance();
			String displayName = getDisplayName();
			String displayName2 = another.getDisplayName();
			//
			if (displayName == null || displayName2 == null) {
				return cmp2.compare(getName(), another.getName());
			}
			//
			return cmp2.compare(displayName, displayName2);
		}

		//
		public String getLocation() {
			return location;
		}

		//
		public String getDisplayName() {
			String name = getName();
			if (name.equalsIgnoreCase("My Notes")) {
				return WizStrings.getString(WizStringId.FOLDER_MY_NOTES);
			} else if (name.equalsIgnoreCase("My Drafts")) {
				return WizStrings.getString(WizStringId.FOLDER_MY_DRAFTS);
			} else if (name.equalsIgnoreCase("My Events")) {
				return WizStrings.getString(WizStringId.FOLDER_MY_EVENTS);
			} else if (name.equalsIgnoreCase("My Tasks")) {
				return WizStrings.getString(WizStringId.FOLDER_MY_TASKS);
			} else if (name.equalsIgnoreCase("My Emails")) {
				return WizStrings.getString(WizStringId.FOLDER_MY_EMAILS);
			} else if (name.equalsIgnoreCase("My Journals")) {
				return WizStrings.getString(WizStringId.FOLDER_MY_JOURNALS);
			} else if (name.equalsIgnoreCase("My Sticky Notes")) {
				return WizStrings.getString(WizStringId.FOLDER_MY_STICKY_NOTES);
			} else if (name.equalsIgnoreCase("My Mobiles")) {
				return WizStrings.getString(WizStringId.FOLDER_MY_MOBILES);
			} else if (name.equalsIgnoreCase("Inbox")) {
				if (getParent().equalsIgnoreCase("/My Tasks/")) {
					return WizStrings.getString(WizStringId.FOLDER_TASKS_INBOX);
				}
			} else if (name.equalsIgnoreCase("Completed")) {
				if (getParent().equalsIgnoreCase("/My Tasks/")) {
					return WizStrings
							.getString(WizStringId.FOLDER_TASKS_COMPLETED);
				}
			}
			return name;
		}

		//
		public String getFullDisplayName() {
			String path = "";
			//
			WizLocation loc = this;
			//
			while (true) {
				path = loc.getDisplayName() + "/" + path;
				//
				if (loc.isRoot())
					break;
				//
				loc = new WizLocation(loc.getParent());
			}
			//
			return WizDatabase.makeMultiLocation(path);
		}

		//
		public String getName() {
			if (location == null)
				return "";
			if (location.equals(""))
				return "";
			if (location.equals("/"))
				return "";
			//
			String parent = location;
			parent = FileUtil.pathRemoveBackslash(parent);
			String name = FileUtil.extractFileName(parent);
			//
			return name;
		}

		public String getParent() {
			if (location == null)
				return "";
			if (location.equals(""))
				return "";
			//
			String parent = location;
			parent = FileUtil.pathRemoveBackslash(parent);
			parent = FileUtil.extractFilePath(parent);
			parent = FileUtil.pathAddBackslash(parent);
			//
			if (parent.equals("/"))
				return "";
			return parent;
		}

		public int getLevel() {
			int count = WizMisc.countOfChar(location, '/');
			//
			if (count <= 2)
				return 0;
			//
			return count - 2;
		}

		public boolean isRoot() {
			if (location == null)
				return true;
			if (location.equals(""))
				return true;
			//
			return location.indexOf('/', 1) == location.length() - 1;
		}
	}
	
	@SuppressLint("DefaultLocale")
	public static HashMap<String, Object> getClassProperty(Object object){
		Class<?> clazz = object.getClass();
		HashMap<String, Object> property = new HashMap<String, Object>();
		if (clazz == null)
			return property;
		
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			try {
				String name = field.getName().toLowerCase();
				Object value = field.get(object);
				property.put(name, value);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return property;
	}

}
