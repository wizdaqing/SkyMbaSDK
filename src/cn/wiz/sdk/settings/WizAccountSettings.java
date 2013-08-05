package cn.wiz.sdk.settings;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import cn.wiz.sdk.api.WizASXmlRpcServer;
import cn.wiz.sdk.api.WizAsyncAction;
import cn.wiz.sdk.api.WizAsyncAction.WizAction;
import cn.wiz.sdk.api.WizAsyncAction.WizAsyncActionThread;
import cn.wiz.sdk.api.WizObject.WizAccount;
import cn.wiz.sdk.api.WizObject.WizUserInfo;
import cn.wiz.sdk.db.WizDatabase;
import cn.wiz.sdk.util.FileUtil;
import cn.wiz.sdk.util.IniUtil;
import cn.wiz.sdk.util.WizMisc;

public class WizAccountSettings {

	static private String mCurrentUserId;
	static private String mCertPassword;

	//---------------------------<当前账户信息>--------------------------------//
	public static String getUserId(Context ctx) {
		if (TextUtils.isEmpty(mCurrentUserId)) {
			setUserId(WizSystemSettings.getDefaultUserId(ctx));
		}
		return mCurrentUserId;
	}

	public static void logoutCurrentUserId() {
		setUserId("");
	}

	/*
	 * 验证用户
	 */
	static public WizUserInfo userLogin(Context ctx, String userId, String password)
			throws Exception {
		WizASXmlRpcServer as = new WizASXmlRpcServer(ctx, userId);
		try {
			WizUserInfo userInfo = as.clientLogin(password);
			return userInfo;
		} finally {
			as.clientLogout();
		}
	}

	/*
	 * 获取所有的account
	 */
	public static ArrayList<WizAccount> getAccounts(Context ctx) {
		//
		boolean modified = false;
		String fileName = getSettingsFileName(ctx);
		IniUtil.IniFile file = null;
		//
		boolean hasEmptyUserGuid = false;
		ArrayList<WizAccount> accounts = new ArrayList<WizAccount>();
		
		try {
			file = new IniUtil.IniFile(fileName);
			int accountCount = file.getInt("accountCount", 0);
			//
			for (int i = 0; i < accountCount; i++) {
	
				String section = "account_" + Integer.toString(i) + "_";
	
				WizAccount account = new WizAccount();//
				account.accountUserId = file.getString(section + "userId", "");
				account.accountPassword = file.getString(section + "password", "");
				account.accountUserGuid = file.getString(section + "userGuid", "");
				account.accountDataFolder = file.getString(section + "dataFolder", "");
	
				if (TextUtils.isEmpty(account.accountUserId))
					continue;
	
				if (TextUtils.isEmpty(account.accountPassword))
					continue;
	
				if (TextUtils.isEmpty(account.accountUserGuid)) {
					hasEmptyUserGuid = true;
				}
	
				if (TextUtils.isEmpty(account.accountDataFolder)) {
					//如果没有data folder，则表示是旧版本升级的数据，那么可以认为默认是user Id，并且进行保存
					account.accountDataFolder = account.accountUserId;
					file.setString(section + "dataFolder", account.accountDataFolder);
					modified = true;
				}
	
				if (WizDatabase.ANONYMOUS_USER_ID.equalsIgnoreCase(account.accountUserId)) {
					account.accountUserGuid = account.accountUserId;
				}
				accounts.add(account);
			}
		}
		catch (Exception e) {
			
		}
		finally {
			if (modified) { 
				try {
					file.store();
				}
				catch (Exception e) {
					
				}
			}
		}
		//
		if (hasEmptyUserGuid) {
			updateAccountsUserGuid(ctx, accounts);
		}
		//
		return accounts;
	}

	/*
	 * 添加一个账号
	 */
	public static boolean addAccount(Context ctx, String userId, String password, String userGuid) {
		if (TextUtils.isEmpty(userId))
			return false;
		if (TextUtils.isEmpty(userGuid))
			return false;
		if (TextUtils.isEmpty(password))
			return false;
		//
		ArrayList<WizAccount> accounts = getAccounts(ctx);
		//
		int indexOfUserGuid = indexOfAccountByUserGuid(accounts, userGuid);
		if (-1 == indexOfUserGuid) {
			//一个新的用户，并且本地没有存储过数据
			WizAccount account = new WizAccount();
			account.accountUserId = userId;
			account.accountPassword = password;
			account.accountUserGuid = userGuid;
			account.accountDataFolder = userId;
			accounts.add(account);
		} 
		else {
			int indexOfUserId = indexOfAccountByUserId(accounts, userId);
			if (-1 == indexOfUserId) {
				//存在user guid, 但是userid不存在，说明用户可能之前是第三方登录，然后选择了
				//邮箱登录，那么增加一个用户，数据存储路径则采用已经存在的
				WizAccount account = new WizAccount();
				account.accountUserId = userId;
				account.accountPassword = password;
				account.accountUserGuid = userGuid;
				account.accountDataFolder = accounts.get(indexOfUserGuid).accountDataFolder;
				accounts.add(account);
			}
			else
			{
				//guid， user 都存在，说明已经存在了，那么应该只需要通过user id
				//更新一下user id 对应的密码即可
				//
				WizAccount account = accounts.get(indexOfUserId);
				account.accountUserGuid = userGuid;
				account.accountPassword = password;
			}
		}
		//
		boolean ret = setAccounts(ctx, accounts);
		//
		return ret;
	}

	/*
	 * 通过user id获得一个账号
	 */
	public static WizAccount getAccountByUserId(Context ctx, String userId) {
		ArrayList<WizAccount> accounts = getAccounts(ctx);
		if (WizMisc.isEmptyArray(accounts))
			return null;
		int index = indexOfAccountByUserId(accounts, userId);

		try {
			return accounts.get(index);
		} catch (Exception e) {
			return null;
		}
	}

	/*
	 * 通过user id获取用户数据存储路径
	 */
	public static String getAccountDataFolderByUserId(Context ctx, String userId){
		ArrayList<WizAccount> accounts = getAccounts(ctx);
		int index = indexOfAccountByUserId(accounts, userId);
		if (-1 == index)
			return userId;

		WizAccount account = accounts.get(index);
		String dataFolder = account.accountDataFolder;
		if (TextUtils.isEmpty(dataFolder) || dataFolder.equalsIgnoreCase("null")){
			dataFolder = userId;
		}

		return dataFolder;
	}

	/*
	 * 通过user id获取账号密码
	 */
	public static String getAccountPasswordByUserId(Context ctx, String userId) {
		ArrayList<WizAccount> accounts = getAccounts(ctx);
		int index = indexOfAccountByUserId(accounts, userId);
		if (-1 == index)
			return "";

		WizAccount account = accounts.get(index);
		return account.accountPassword;
	}
	

	// sd卡的app根目录 /sdcard/wiznote/
	// 返回根目录
	public static String getDataRootPath(Context ctx) {
		try {
			String basePath = FileUtil.getStorageCardPath();
			basePath = FileUtil.pathAddBackslash(basePath) + "wiznote";
			FileUtil.ensurePathExists(basePath);
			//
			try {
				String noMediaFileName = basePath + "/.nomedia";
				if (!FileUtil.fileExists(noMediaFileName)) {
					FileUtil.saveTextToFile(noMediaFileName, "", "utf-8");
				}
			} catch (Exception err) {
			}
			//
			return basePath;
		} catch (Exception err) {
			return ctx.getFilesDir().getPath();
		}
	}

	// 返回用户目录, sd卡上的 /sdcard/wiznote/user id/
	public static String getAccountPath(Context ctx, String userId) {
		if (TextUtils.isEmpty(userId))
			return "";
		//
		String basePath = getDataRootPath(ctx);
		//
		basePath = FileUtil.pathAddBackslash(basePath);
		//
		String accountPath = basePath + WizAccountSettings.getAccountDataFolderByUserId(ctx, userId);

		FileUtil.ensurePathExists(accountPath);
		//
		return FileUtil.pathAddBackslash(accountPath);
	}


	// 返回临时文件夹用户数据路径 /android data/app.id/user id/
	// 保存数据库的路径,index.db group guid.db
	public static String getRamAccountPath(Context ctx, String userId) {
		//
		String basePath = FileUtil.getRamRootPath(ctx);
		//
		basePath = FileUtil.pathAddBackslash(basePath);
		if (TextUtils.isEmpty(userId))
			return basePath;
		//
		String accountPath = basePath + WizAccountSettings.getAccountDataFolderByUserId(ctx, userId);

		FileUtil.ensurePathExists(accountPath);
		//
		return FileUtil.pathAddBackslash(accountPath);
	}
	

	/*
	 * 获取证书密码
	 */
	public static String getCertPassword() {
		return mCertPassword;
	}

	/*
	 * 设置证书密码
	 */
	public static void setCertPassword(String password) {
		mCertPassword = password;
	}
	

	/*
	 * 保存全部账号
	 */

	private static boolean setAccounts(Context ctx, ArrayList<WizAccount> accounts) {
		if (WizMisc.isEmptyArray(accounts)) {
			return true;
		}

		IniUtil.IniFile file = null;
		//
		try {
			String fileName = getSettingsFileName(ctx);
			file = new IniUtil.IniFile(fileName);
			//
			int accountCount = accounts.size();
			file.setInt("accountCount", accountCount);
			//
			for (int i = 0; i < accountCount; i++) {
				String section = "account_" + Integer.toString(i) + "_";
				WizAccount account = accounts.get(i);
				if (TextUtils.isEmpty(account.accountUserId))
					continue;
				if (TextUtils.isEmpty(account.accountPassword))
					continue;
				file.setString(section + "userId", account.accountUserId);
				file.setString(section + "password", account.accountPassword);
				file.setString(section + "userGuid", account.accountUserGuid);
				file.setString(section + "dataFolder", account.accountDataFolder);
			}
			file.store();
			return true;
		} 
		catch (Exception e) {
			return false;
		}
	}

	/*
	 * 通过user id查找账号
	 */
	private static int indexOfAccountByUserId(ArrayList<WizAccount> accounts,
			String accountUserId) {
		if (WizMisc.isEmptyArray(accounts))
			return -1;
		for (int i = 0; i < accounts.size(); i++) {
			WizAccount account = accounts.get(i);
			if (TextUtils.isEmpty(account.accountUserId))
				continue;
			if (account.accountUserId.equalsIgnoreCase(accountUserId))
				return i;
		}
		return -1;
	}

	/*
	 * 通过user guid查找账号
	 */
	private static int indexOfAccountByUserGuid(ArrayList<WizAccount> accounts,
			String accountUserGuid) {
		if (WizMisc.isEmptyArray(accounts))
			return -1;
		for (int i = 0; i < accounts.size(); i++) {
			WizAccount account = accounts.get(i);
			if (TextUtils.isEmpty(account.accountUserGuid))
				continue;
			if (account.accountUserGuid.equalsIgnoreCase(accountUserGuid))
				return i;
		}
		return -1;
	}

	/*
	 * 更新账号guid
	 */
	//
	private static boolean mUpdatedAccountsUserGuid = false;
	private static void updateAccountsUserGuid(final Context ctx, final ArrayList<WizAccount> accounts){
		//
		if (mUpdatedAccountsUserGuid)
			return;
		//
		mUpdatedAccountsUserGuid = true;
		//
		WizAsyncAction.startAsyncAction(null, new WizAction() {
			@Override
			public Object work(WizAsyncActionThread thread, Object actionData)
					throws Exception {
				for (WizAccount account : accounts) {
					//更新用户GUID
					if (TextUtils.isEmpty(account.accountUserGuid)) {
						try {
							WizUserInfo info = userLogin(ctx, account.accountUserId, account.accountPassword);
							WizAccountSettings.addAccount(ctx, account.accountUserId, account.accountPassword, info.userGuid);
						}
						catch (Exception e) {
						}
					}
				}
				return null;
			}

			@Override
			public void onStatus(Object actionData, String status, int arg1, int arg2,
					Object obj) {
			}

			@Override
			public void onException(Object actionData, Exception e) {
			}

			@Override
			public void onEnd(Object actionData, Object ret) {
			}

			@Override
			public void onBegin(Object actionData) {
			}
		});
	}
	    
	//



	//---------------------------</当前账户信息>-------------------------------//

	private static String mSettingsFileName;
	private static String getSettingsFileName(Context ctx) {
		if (TextUtils.isEmpty(mSettingsFileName)) {

			String path = FileUtil.getRamRootPath(ctx);
			FileUtil.ensurePathExists(path);
			path = FileUtil.pathAddBackslash(path);

			mSettingsFileName = path + "settings.ini";
		}

		if (!FileUtil.fileExists(mSettingsFileName)) {

			String sdcardRootPath = getDataRootPath(ctx);
			sdcardRootPath = FileUtil.pathAddBackslash(sdcardRootPath);
			String oldSettingFile = sdcardRootPath + "settings.ini";

			if (FileUtil.fileExists(oldSettingFile)) {
				try {
					FileUtil.copyFile(oldSettingFile, mSettingsFileName);
					FileUtil.deleteFile(oldSettingFile);
				} catch (FileNotFoundException e) {
				} catch (IOException e) {
				}
			}
		}

		return mSettingsFileName;
	}

	/*
	public static boolean setAccountDataPathKey(Context ctx, String userGuid, String pathkey){
		if (TextUtils.isEmpty(pathkey) || pathkey.equalsIgnoreCase("null"))
			return false;

		String oldPathKey = getAccountDataPathKey(ctx, userGuid);
		if (!TextUtils.isEmpty(oldPathKey) && !oldPathKey.equalsIgnoreCase("null"))
			return true;

		return setString(ctx, "accountDataPathKey_" + userGuid, pathkey);
	}

	private static String getAccountDataPathKey(Context ctx, String userGuid){
		return getString(ctx, "accountDataPathKey_" + userGuid, "");
	}

*/

	synchronized private static void setUserId(String userId) {
		mCurrentUserId = userId;
	}
}
