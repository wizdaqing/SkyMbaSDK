package cn.wiz.sdk.api;


public class WizStrings {

	static public interface WizStringsBase {
		abstract public String getString(WizStringId id);
	}

	public static enum WizStringId {
		SYNC_LOGIN, 
		SYNC_LOGOUT, 
		SYNC_DOWNLOADING_MESSAGES, 
		SYNC_DOWNLOADING_DELETED_GUIDS, 
		SYNC_DOWNLOADING_TAGS, 
		SYNC_DOWNLOADING_DOCUMENTS, 
		SYNC_DOWNLOADING_ATTACHMENTS, 
		SYNC_UPLOADING_DELETED_GUIDS, 
		SYNC_UPLOADING_TAGS, 
		SYNC_UPLOADING_DOCUMENT, 
		SYNC_UPLOADING_ATTACHMENT, 
		SYNC_KB_BEGIN, 
		SYNC_KB_END, 
		PERSONAL_KB_NAME,
		SYNC_CANCELED_NOT_WIFI,
		FOLDER_MY_NOTES,
		FOLDER_MY_DRAFTS,
		FOLDER_MY_EVENTS,
		FOLDER_MY_TASKS,
		FOLDER_MY_EMAILS,
		FOLDER_MY_JOURNALS,
		FOLDER_TASKS_INBOX,
		FOLDER_TASKS_COMPLETED,
		FOLDER_MY_MOBILES,
		FOLDER_MY_STICKY_NOTES,
		TAG_NAME_OF_KB_ROOT,
		SYNC_DOWNLOADING_NOTE,
		SYNC_START,
		STOPPING_SYNC,
		SYNC_DOWNLOADING_NOTES_DATA,
		USER_INFO_HAS_ERROR,
	}

	static private WizStringsBase mStrings = new WizDefaultStrings();

	public static String getString(WizStringId id) {
		return mStrings.getString(id);
	}

	public static void setStrings(WizStringsBase strings) {
		mStrings = strings;
		if (mStrings == null)
			mStrings = new WizDefaultStrings();
	}

	//
	static private class WizDefaultStrings implements WizStringsBase {

		@Override
		public String getString(WizStringId id) {
			switch (id) {
			case SYNC_LOGIN:
				return "Signing in";
			case SYNC_LOGOUT:
				return "Singing out";
			case SYNC_DOWNLOADING_MESSAGES:
				return "Downloading messages";
			case SYNC_DOWNLOADING_DELETED_GUIDS:
				return "Downloading deleted lists";
			case SYNC_DOWNLOADING_TAGS:
				return "Downloading tags";
			case SYNC_DOWNLOADING_DOCUMENTS:
				return "Downloading note lists";
			case SYNC_DOWNLOADING_ATTACHMENTS:
				return "Downloading attachment lists";
			case SYNC_UPLOADING_DELETED_GUIDS:
				return "Uploading deleted lists";
			case SYNC_UPLOADING_TAGS:
				return "Uploading tags";
			case SYNC_UPLOADING_DOCUMENT:
				return "Uploading note: %1";
			case SYNC_UPLOADING_ATTACHMENT:
				return "Uploading attachment: %1";
			case SYNC_KB_BEGIN:
				return "Syncing %1";
			case SYNC_KB_END:
				return "Sync %1 done";
			case PERSONAL_KB_NAME:
				return "Personal notes";
			case SYNC_CANCELED_NOT_WIFI:
				return "The sync stopped because WiFi disconnected";
			case FOLDER_MY_NOTES: 
				return "My Notes";
			case FOLDER_MY_DRAFTS: 
				return "My Drafts";
			case FOLDER_MY_EVENTS: 
				return "My Events";
			case FOLDER_MY_TASKS: 
				return "My Tasks";
			case FOLDER_MY_EMAILS: 
				return "My Emails";
			case FOLDER_MY_JOURNALS: 
				return "My Journals";
			case FOLDER_MY_MOBILES:
				return "My Mobiles";
			case FOLDER_MY_STICKY_NOTES:
				return "My Sticky Notes";
			case TAG_NAME_OF_KB_ROOT:
				return "Ungrouped";
			case FOLDER_TASKS_INBOX: 
				return "Inbox";
			case FOLDER_TASKS_COMPLETED: 
				return "Completed";
			case SYNC_DOWNLOADING_NOTES_DATA:
				return "Downloading notes data...";
			case USER_INFO_HAS_ERROR:
				return "Account information error, Please log in again.";
			default:
				break;
			}
			return "";
		}
	}

}
