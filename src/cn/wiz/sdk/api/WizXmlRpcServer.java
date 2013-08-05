package cn.wiz.sdk.api;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;

import cn.wiz.sdk.api.WizObject.*;
import cn.wiz.sdk.util.TimeUtil;

import android.content.Context;
import android.text.TextUtils;
import redstone.xmlrpc.XmlRpcArray;
import redstone.xmlrpc.XmlRpcClient;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;
import redstone.xmlrpc.XmlRpcStruct;

@SuppressWarnings("unchecked")
public abstract class WizXmlRpcServer {
	protected String mXmlRpcUrl;
	protected XmlRpcClient mClient;
	protected Context mContext;
	protected String mUserId;
	protected String mCurrentMethodName;

	public WizXmlRpcServer(Context ctx, String xmlrpcUrl, String userId)
			throws MalformedURLException {
		mXmlRpcUrl = xmlrpcUrl;
		mContext = ctx;
		mUserId = userId;
		mClient = new XmlRpcClient(ctx, xmlrpcUrl);
	}

	//
	public String getCurrentMethodName() {
		return mCurrentMethodName;
	}

	//
	abstract public String getToken();

	abstract public String getKbGUID();

	//
	@SuppressWarnings({ "serial" })
	public class WizXmlRpcParam extends XmlRpcStruct {
		private void addCommonParams(Context ctx) {
			put("client_type", "android");
			put("program_type", "normal");
			put("api_version", "4");
			put("client_version", WizLogger.getVersionName(ctx));
		}

		public WizXmlRpcParam(Context ctx) {
			addCommonParams(ctx);
		}

		public WizXmlRpcParam(Context ctx, String token) {
			addCommonParams(ctx);
			put("token", token);
		}

		//
		public WizXmlRpcParam(Context ctx, String token, String kbGUID) {
			addCommonParams(ctx);
			put("token", token);
			if (!TextUtils.isEmpty(kbGUID))
				put("kb_guid", kbGUID);
		}

		//
		public void add(String key, Object val) {
			put(key, val);
		}
	}

	protected XmlRpcStruct call(String xmlrpcMethodName, XmlRpcStruct param)
			throws XmlRpcException, XmlRpcFault {
		mCurrentMethodName = xmlrpcMethodName;
		Object[] params = new Object[] { param };
		//
		Object ret = mClient.invoke(xmlrpcMethodName, params);
		//
		if (ret instanceof XmlRpcStruct)
			return (XmlRpcStruct) ret;

		//
		throw new XmlRpcException("failed to call method: " + xmlrpcMethodName
				+ ", return value is not a struct");
	}

	protected XmlRpcArray call2(String xmlrpcMethodName, XmlRpcStruct param)
			throws XmlRpcException, XmlRpcFault {
		mCurrentMethodName = xmlrpcMethodName;
		//
		Object[] params = new Object[] { param };
		//
		Object ret = mClient.invoke(xmlrpcMethodName, params);
		//
		if (ret instanceof XmlRpcArray)
			return (XmlRpcArray) ret;

		//
		throw new XmlRpcException("failed to call method: " + xmlrpcMethodName
				+ ", return value is not a struct");
	}

	public <T> ArrayList<T> call(String xmlrpcMethodName, XmlRpcStruct param, Class<T> clazz) throws XmlRpcException, XmlRpcFault {
		mCurrentMethodName = xmlrpcMethodName;
		//
		Object[] params = new Object[] { param };
		//
		Object ret = mClient.invoke(xmlrpcMethodName, params);
		//
		if (ret instanceof XmlRpcArray) {
			XmlRpcArray arr = (XmlRpcArray)ret;
			return decodeArray(arr, clazz);
		}
		//
		throw new XmlRpcException("failed to call method: " + xmlrpcMethodName
				+ ", return value is not a struct");
	}	
	

	protected long getValueVersion(String methodPerfix, String key) throws XmlRpcException,
			XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, getToken(), getKbGUID());
		param.add("key", key);
		//
		XmlRpcStruct ret = call(methodPerfix + ".getValueVersion", param);
		//
		String ver = ret.getString("version");
		//
		return Long.parseLong(ver);
	}

	public class WizKeyValue {
		public String value;
		public long version;

		//
		public WizKeyValue(String val, long ver) {
			value = val;
			version = ver;
		}
	}

	protected WizKeyValue getValue(String methodPerfix, String key) throws XmlRpcException,
			XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, getToken(), getKbGUID());
		param.add("key", key);
		//
		XmlRpcStruct ret = call(methodPerfix + ".getValue", param);
		//
		String val = ret.getString("value_of_key");
		String ver = ret.getString("version");
		long verNumber = Long.parseLong(ver);
		//
		WizKeyValue data = new WizKeyValue(val, verNumber);
		//
		return data;
	}

	protected long setValue(String methodPerfix, String key, String value) throws XmlRpcException,
			XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, getToken(), getKbGUID());
		param.add("key", key);
		param.add("value_of_key", value);
		//
		XmlRpcStruct ret = call(methodPerfix + ".setValue", param);
		//
		String ver = ret.getString("version");
		//
		return Long.parseLong(ver);
	}
	
	//
	protected static WizUserInfo decodeUserInfo(XmlRpcStruct xml) {
		WizUserInfo ret = new WizUserInfo();
	
		ret.token = xml.getString("token");
		ret.personalKbGuid = xml.getString("kb_guid");
		ret.personalKbDatabaseUrl = xml.getString("kapi_url");
		ret.userLevel = xml.getString("user_level");
		ret.userLevelName = xml.getString("user_level_name");
		ret.userPoints = xml.getString("user_points");
		ret.inviteCode = xml.getString("invite_code");
		ret.userType = xml.getString("user_type");
		//
		XmlRpcStruct user = xml.getStruct("user");
		if (user != null) {
			ret.email = user.getString("email");
			ret.mobile = user.getString("mobile");
			ret.nickName = user.getString("nickname");
			ret.userGuid = user.getString("user_guid");
			ret.displayName = user.getString("displayname");
		}
		//
		return ret;
	}
	

	//
	protected static WizKbVersion decodeKbVersion(XmlRpcStruct xml) {
		WizKbVersion ret = new WizKbVersion();
		ret.documentVersion = Long.parseLong(xml.getString("document_version"));
		ret.tagVersion = Long.parseLong(xml.getString("tag_version"));
		ret.deletedVersion = Long.parseLong(xml.getString("deleted_version"));
		ret.attachmentVersion = Long.parseLong(xml.getString("attachment_version"));
		ret.styleVersion = Long.parseLong(xml.getString("style_version"));
		return ret;
	}

	protected static WizCert decodeCert(XmlRpcStruct xml) {
		WizCert ret = new WizCert();
		//
		ret.n = xml.getString("n");
		ret.e = xml.getString("e");
		ret.encryptedD = xml.getString("d");
		ret.hint = xml.getString("hint");
		//
		return ret;
	}

	protected static WizKb decodeKb (XmlRpcStruct xml) {
		WizKb ret = new WizGroupKb();	//group
		ret.name = xml.getString("kb_name");
		ret.bizName = xml.getString("biz_name");
		ret.kbGuid = xml.getString("kb_guid");
		ret.bizGuid = xml.getString("biz_guid");
		ret.kbDatabaseUrl = xml.getString("kapi_url");
		ret.userRole = Integer.parseInt(xml.getString("user_group"));
		//
		return ret;
	}

	public static WizDeletedGUID decodeDeletedGUID(XmlRpcStruct xml) {
		WizDeletedGUID del = new WizDeletedGUID();
		del.guid = xml.getString("deleted_guid");
		del.type = xml.getString("guid_type");
		del.version = Long.parseLong(xml.getString("version"), 10);
		//
		return del;
	}
	
	public static WizTag decodeTag(XmlRpcStruct xml) {
		WizTag tag = new WizTag();
		//
		tag.guid = xml.getString("tag_guid");
		tag.name = xml.getString("tag_name");
		tag.parentGuid = xml.getString("tag_group_guid");
		tag.description = xml.getString("tag_description");
		tag.version = Long.parseLong(xml.getString("version"), 10);
		tag.dateModified = TimeUtil.getSQLDateTimeString(xml.getDate("dt_info_modified"),
				TimeUtil.patternSQLDate);
		return tag;
	}
	public static WizMessage decodeMessage(XmlRpcStruct xml) {
		WizMessage message = new WizMessage();
		
		message.messageId = xml.getInteger("id");//Long.parseLong(xml.getString("id"));
		message.bizGuid = xml.getString("biz_guid");
		message.kbGuid = xml.getString("kb_guid");
		message.documentGuid = xml.getString("document_guid");
		message.title = xml.getString("title");
		message.senderGuid = xml.getString("sender_guid");
		message.senderId = xml.getString("sender_id");
		message.senderAlias = xml.getString("sender_alias");
		message.receiverGuid = xml.getString("receiver_guid");
		message.receiverId = xml.getString("receiver_id");
		message.receiverAlias = xml.getString("receiver_alias");
		message.version = Long.parseLong(xml.getString("version"));
		message.messageType = xml.getInteger("message_type");
		message.emailStatus = xml.getInteger("email_status");
		message.smsStatus = xml.getInteger("sms_status");
		message.readCount = xml.getInteger("read_status");
		message.dateCreated = TimeUtil.getSQLDateTimeString(xml.getDate("dt_created"));
		message.body = xml.getString("message_body");
		message.note = xml.getString("note");
		return message;
	}

	public static WizDocument decodeDocument(XmlRpcStruct xml) {
		WizDocument doc = new WizDocument();
		doc.guid = (xml.getString("document_guid"));
		doc.title = (xml.getString("document_title"));
		doc.location = (xml.getString("document_category"));
		doc.url = (xml.getString("document_url"));
		doc.tagGUIDs = (xml.getString("document_tag_guids"));
		doc.type = (xml.getString("document_type"));
		doc.fileType = (xml.getString("document_filetype"));
		doc.dateCreated = (TimeUtil.getSQLDateTimeString(xml.getDate("dt_created")));
		//
		//尝试从dt_data_modified获取修改时间
		try {
			doc.dateModified = (TimeUtil.getSQLDateTimeString(xml.getDate("dt_data_modified")));
		}catch (NumberFormatException e) {
		}catch (Exception e) {
		}
		doc.dateModified = (TimeUtil.getSQLDateTimeString(xml.getDate("dt_modified")));
		
		doc.dataMd5 = (xml.getString("data_md5"));
		try {
			Object count = xml.get("document_attachment_count");
			doc.attachmentCount = (Integer.parseInt(String.valueOf(count)));
		}catch (NumberFormatException e) {
		} catch (Exception e) {
		}
		doc.gpsLatitude = (0.0);
		doc.gpsLongtitude  = (0.0);
		doc.gpsAltitude = (0.0);
		doc.gpsDop = (0.0);
		doc.gpsAddress = ("");
		doc.gpsCountry = ("");
		doc.gpsDescription = ("");
		doc.readCount = (0);
		doc.serverChanged = (1);
		doc.localChanged = (WizDocument.DOCUMENT_LOCAL_CHANGE_NULL);
		doc.owner = (xml.getString("document_owner"));
		try {
			doc.encrypted = (Integer.parseInt(xml.getString("document_protect")) == 1);
		}catch (NumberFormatException e) {
		}
		try {
			doc.keywords = (xml.getString("document_keywords"));
		}catch (Exception e) {
		}

		doc.version = (Long.parseLong(xml.getString("version"), 10));

		return doc;
	}

	public static WizAttachment decodeAttachment(XmlRpcStruct xml) {
		WizAttachment attachment = new WizAttachment();

		attachment.guid = xml.getString("attachment_guid");
		attachment.name = xml.getString("attachment_name");
		attachment.docGuid = xml.getString("attachment_document_guid");
		attachment.version = Long.parseLong(xml.getString("version"), 10);
		attachment.dataMd5 = xml.getString("data_md5");
		attachment.dateModified = TimeUtil.getSQLDateTimeString(xml.getDate("dt_data_modified"),
				TimeUtil.patternSQLDate);
		attachment.description = xml.getString("attachment_description");

		return attachment;
	}
	
	//encode  object to xml struct
	
	public static void encodeDeletedGUID(WizDeletedGUID del, XmlRpcStruct ret) {
		ret.put("deleted_guid", del.guid);
		ret.put("guid_type", del.type);
		ret.put("dt_deleted", TimeUtil.getDateFromSqlDateTimeString(del.dateDeleted));
	}

	public static XmlRpcStruct encodeDeletedGUID(WizDeletedGUID del) {
		
		XmlRpcStruct ret = new XmlRpcStruct();
		encodeDeletedGUID(del, ret);
		return ret;
	}
	
	public static void encodeTag(WizTag tag, XmlRpcStruct objRpc) {
		objRpc.put("tag_guid", tag.guid);
		objRpc.put("tag_name", TextUtils.isEmpty(tag.name) ? "" : tag.name);
		objRpc.put("tag_group_guid", TextUtils.isEmpty(tag.parentGuid) ? "" : tag.parentGuid);
		objRpc.put("tag_description", TextUtils.isEmpty(tag.description) ? "" : tag.description);
		objRpc.put("dt_info_modified", TimeUtil.getDateFromSqlDateTimeString(tag.dateModified));
	}
	
	public static XmlRpcStruct encodeTag(WizTag tag){
		XmlRpcStruct objRpc = new XmlRpcStruct();
		encodeTag(tag, objRpc);
		return objRpc;
	}
	
	public static void encodeDocument(WizDocument doc, XmlRpcStruct objRpc) {
		String category = doc.location;
		category = TextUtils.isEmpty(category) ? "" : category;
		
		String docTagGuids = doc.tagGUIDs;
		docTagGuids = TextUtils.isEmpty(docTagGuids) ? "" : docTagGuids;
		docTagGuids = docTagGuids.replace("*", ";");

		Date dtCreate = TimeUtil.getDateFromSqlDateTimeString(doc.dateCreated);
		Date dtModified = TimeUtil.getDateFromSqlDateTimeString(doc.dateModified);

		boolean updateData = doc.localChanged == WizDocument.DOCUMENT_LOCAL_CHANGE_DATA;
		if (TextUtils.isEmpty(doc.url))
			doc.url = "";
		//
		objRpc.put("document_guid", doc.guid);
		objRpc.put("document_title", doc.title);
		objRpc.put("document_type", doc.type);
		objRpc.put("document_filetype", doc.fileType);
		objRpc.put("document_category", category);
		objRpc.put("document_info", true);
		objRpc.put("with_document_data", updateData);
		objRpc.put("document_zip_md5", doc.dataMd5);
		objRpc.put("document_attachment_count", doc.attachmentCount);
		objRpc.put("document_tag_guids", docTagGuids);
		objRpc.put("document_url", doc.url);
		objRpc.put("dt_created", dtCreate);
		objRpc.put("dt_modified", dtModified);
	}
	
	public static  XmlRpcStruct encodeDocument(WizDocument doc){
		XmlRpcStruct objRpc = new XmlRpcStruct();
		encodeDocument(doc, objRpc);
		//
		return objRpc;
	}
	
	public static  void encodeAttachment(WizAttachment att, XmlRpcStruct objRpc){
		Date dtModified = TimeUtil.getDateFromSqlDateTimeString(att.dateModified);
		objRpc.put("attachment_guid", att.guid);
		objRpc.put("attachment_document_guid", att.docGuid);
		objRpc.put("attachment_name", att.name);
		objRpc.put("dt_modified", dtModified);
		objRpc.put("data_md5", att.dataMd5);
		objRpc.put("attachment_zip_md5", att.dataMd5);
		objRpc.put("attachment_data", true);
	}

	public static  XmlRpcStruct encodeAttachment(WizAttachment att){
		XmlRpcStruct objRpc = new XmlRpcStruct();
		encodeAttachment(att, objRpc);
		return objRpc;
	}
	//
	public static <T> XmlRpcStruct encodeObject(T obj) {
		//
		if (obj instanceof WizDeletedGUID)
			return encodeDeletedGUID((WizDeletedGUID)obj);
		else if (obj instanceof WizTag)
			return encodeTag((WizTag)obj);
		else if (obj instanceof WizDocument)
			return encodeDocument((WizDocument)obj);
		else if (obj instanceof WizAttachment)
			return encodeAttachment((WizAttachment)obj);
		//
		throw new XmlRpcException("Bad cast, unknown object type: " + obj.getClass().getName());
	}
	//
	static public <T> T decodeXmlRpcStruct(XmlRpcStruct xml, Class<T> clazz) {
		//
		if (clazz.isAssignableFrom(WizDeletedGUID.class))
			return (T) decodeDeletedGUID(xml);
		else if (clazz.isAssignableFrom(WizTag.class))
			return (T) decodeTag(xml);
		else if (clazz.isAssignableFrom(WizDocument.class))
			return (T) decodeDocument(xml);
		else if (clazz.isAssignableFrom(WizAttachment.class))
			return (T) decodeAttachment(xml);
		else if (clazz.isAssignableFrom(WizKb.class))
			return (T) decodeKb(xml);
		else if (clazz.isAssignableFrom(WizMessage.class))
			return (T) decodeMessage(xml);
		//
		throw new XmlRpcException("Bad cast: " + clazz.getName());
	}

	static public <T> ArrayList<T> decodeArray(XmlRpcArray xml, Class<T> clazz) {
		//
		ArrayList<T> ret = new ArrayList<T>();
		//
		for (Object obj : xml) {
			if (obj instanceof XmlRpcStruct) {
				XmlRpcStruct elem = (XmlRpcStruct) obj;
				//
				T data = decodeXmlRpcStruct(elem, clazz);
				//
				ret.add(data);
			}
		}
		//
		return ret;
	}

	static public <T> XmlRpcArray encodeArray(ArrayList<T> arr) {
		//
		XmlRpcArray ret = new XmlRpcArray();
		//
		for (T obj : arr) {
			XmlRpcStruct elem = encodeObject(obj);
			ret.add(elem);
		}
		//
		return ret;
	}	
	

	public <T> ArrayList<T> getList(String xmlrpcMethodName, long startVersion, int count, Class<T> clazz) throws XmlRpcException, XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, getToken(), getKbGUID());
		//
		param.add("version", startVersion);
		param.add("count", count);
		//
		return call(xmlrpcMethodName, param, clazz);
	}

	public <T> void postList(String xmlrpcMethodName, String arrayMemberName, ArrayList<T> arr) throws XmlRpcException, XmlRpcFault {
		if (arr.size() == 0)
			return;
		//
		mCurrentMethodName = xmlrpcMethodName;
		//
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, getToken(), getKbGUID());
		//
		XmlRpcArray datas = encodeArray(arr);
		param.put(arrayMemberName, datas);
		//
		Object[] params = new Object[] { param };
		//
		mClient.invoke(xmlrpcMethodName, params);
	}
		
}
