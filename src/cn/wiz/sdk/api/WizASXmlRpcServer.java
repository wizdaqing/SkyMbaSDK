package cn.wiz.sdk.api;

import java.util.ArrayList;

import redstone.xmlrpc.XmlRpcArray;
import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;
import redstone.xmlrpc.XmlRpcStruct;
import android.content.Context;
import android.text.TextUtils;
import cn.wiz.sdk.api.WizObject.WizCert;
import cn.wiz.sdk.api.WizObject.WizKb;
import cn.wiz.sdk.api.WizObject.WizMessage;
import cn.wiz.sdk.api.WizObject.WizPersonalKb;
import cn.wiz.sdk.api.WizObject.WizUserInfo;
import cn.wiz.sdk.api.WizStrings.WizStringId;
import cn.wiz.sdk.settings.WizAccountSettings;
import cn.wiz.sdk.util.FileUtil;
import cn.wiz.sdk.util.WizMisc;
import cn.wiz.sdk.util.WizMisc.MD5Util;

public class WizASXmlRpcServer extends WizXmlRpcServer {

	public WizASXmlRpcServer(Context ctx, String userId) throws Exception {
		super(ctx, getASXmlRpcUrl(ctx), userId);
	}

	public String getToken() {
		if (mUserInfo == null)
			return null;
		//
		return mUserInfo.token;
	}

	public String getKbGUID() {
		return null;
	}

	public String getPersonalKbGuid() {
		if (mUserInfo == null)
			return null;
		//
		return mUserInfo.personalKbGuid;
	}

	public String getPersonalKbDatabaseUrl() {
		if (mUserInfo == null)
			return null;
		//
		return mUserInfo.personalKbDatabaseUrl;
	}

	public String getUserId() {
		return mUserId;
	}

	//
	protected WizUserInfo mUserInfo;

	//
	public WizUserInfo clientLogin(String password) throws XmlRpcException, XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext);
		param.add("user_id", mUserId);
		param.add("password", password);
		//
		// param.add("program_type", "normal");
		// param.add("protocol", "http");
		//

		mUserInfo = decodeUserInfo(call("accounts.clientLogin", param));
		//
		return mUserInfo;
	}

	public ArrayList<WizMessage> getMessages(long startVersion, int count)
			throws XmlRpcException, XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, getToken());
		//toString   xml rpc 已处理
		param.add("version", String.valueOf(startVersion));
		param.add("count", count);
		//
		return call("accounts.getMessages", param, WizMessage.class);
	}
	
	public String[] changeServerReadStatus(ArrayList<WizMessage> messages,int status) throws XmlRpcException, XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, getToken());
		StringBuilder ids = new StringBuilder();
		for(WizMessage message : messages){
			ids.append(message.messageId+",");
		}
		param.add("ids", ids.substring(0, ids.lastIndexOf(",")));
		param.add("status", status);
		//
		XmlRpcStruct ret = call("accounts.setReadStatus", param);
		
		return ret.getString("success_ids").split(",");
	}
	
	public String createAccount(String password, String inviteCode) throws XmlRpcException,
			XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext);
		param.add("user_id", mUserId);
		param.add("password", MD5Util.makeMD5Password(password));
		param.add("oem", WizLogger.getOEMSource(mContext));
		//
		if (inviteCode != null) {
			param.add("invite_code", inviteCode);
			param.add("product_name", "android");
		}
		//
		XmlRpcStruct ret = call("accounts.createAccount", param);
		//
		try {
			return ret.getString("oem_return_message");
		}
		catch (Exception e) {
			return null;
		}
	}

	public void keepAlive() throws XmlRpcException, XmlRpcFault {
		keepAlive(getToken());
	}
	public void keepAlive(String token) throws XmlRpcException, XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, token);
		//
		call("accounts.keepAlive", param);
	}

	public String getToken(String password) throws XmlRpcException, XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext);
		param.add("user_id", mUserId);
		param.add("password", password);
		//
		XmlRpcStruct ret = call("accounts.getToken", param);
		//
		return ret.getString("token");
	}

	public void clientLogout() throws XmlRpcException, XmlRpcFault {
		String token = getToken();
		if (token == null)
			return;
		//
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, token);
		//
		call("accounts.clientLogout", param);
	}

	public long getValueVersion(String key) throws XmlRpcException, XmlRpcFault {
		return super.getValueVersion("accounts", key);
	}

	public WizKeyValue getValue(String key) throws XmlRpcException, XmlRpcFault {
		return super.getValue("accounts", key);
	}

	public long setValue(String key, String value) throws XmlRpcException, XmlRpcFault {
		return super.setValue("accounts", key, value);
	}

	public WizCert getCert(String password) throws XmlRpcException, XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext);
		param.add("user_id", mUserId);
		param.add("password", password);
		//
		return decodeCert(call("accounts.getCert", param));
	}


	public ArrayList<WizKb> getGroupList() throws XmlRpcException, XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, getToken(), getKbGUID());
		param.add("kb_type", "group");
		//
		XmlRpcArray ret = call2("accounts.getGroupKbList", param);
		//
		ArrayList<WizKb> arr = new ArrayList<WizKb>();
		//
		for (Object obj : ret) {
			if (obj instanceof XmlRpcStruct) {
				WizKb data = decodeKb((XmlRpcStruct) obj);
				arr.add(data);
				continue;
			}
			//
			throw new XmlRpcException(
					"failed to call method: accounts.getGrpupKbList, return value is not a struct");
		}
		//
		return arr;
	}

	public ArrayList<WizKb> getAllKbList() throws XmlRpcException, XmlRpcFault {
		ArrayList<WizKb> arr = getGroupList();
		//
		String personalKbName = WizStrings.getString(WizStringId.PERSONAL_KB_NAME);
		//
		WizKb personalKb = new WizPersonalKb(mUserInfo.personalKbDatabaseUrl,
				mUserInfo.personalKbGuid, personalKbName);
		//
		arr.add(0, personalKb);
		//
		return arr;
	}
	
	static private String getASXmlRpcUrl(Context ctx) throws Exception {
		String urlOld = WizStatusCenter.getASUrl();
		if (urlOld != null && !urlOld.equals(""))
			return urlOld;
		//
		String debugFileName = WizAccountSettings.getDataRootPath(ctx) + "/debug.ini";
		if (FileUtil.fileExists(debugFileName)) {
			String url = FileUtil.loadTextFromFile(debugFileName);
			url = url.trim();
			if (url.startsWith("http")) {
				WizStatusCenter.setASUrl(url);
				return url;
			}
		}
		//
		String url = WizMisc.getUrlFromApi("sync_http");
		WizStatusCenter.setASUrl(url);
		return url;
	}
	
	static public String getToken(Context ctx, String userId, String password) throws Exception {
		//
		WizASXmlRpcServer server = null;
		server = new WizASXmlRpcServer(ctx, userId);
	
		//
		String oldToken = WizStatusCenter.getCurrentToken(userId);
		//
		try {
			if (!TextUtils.isEmpty(oldToken)) {
				//
				server.keepAlive(oldToken);
				return oldToken;
			}
		} catch (Exception e) {
		}
		//
		server.clientLogin(password);
		//
		String newToken = server.getToken();
		//
		WizStatusCenter.setCurrentToken(userId, newToken);
		//
		return newToken;
	}
}
