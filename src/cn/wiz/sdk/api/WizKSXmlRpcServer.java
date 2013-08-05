package cn.wiz.sdk.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import redstone.xmlrpc.XmlRpcException;
import redstone.xmlrpc.XmlRpcFault;
import redstone.xmlrpc.XmlRpcStruct;
import android.content.Context;
import android.text.TextUtils;
import cn.wiz.sdk.api.WizObject.WizAttachment;
import cn.wiz.sdk.api.WizObject.WizDeletedGUID;
import cn.wiz.sdk.api.WizObject.WizDocument;
import cn.wiz.sdk.api.WizObject.WizKbVersion;
import cn.wiz.sdk.api.WizObject.WizTag;
import cn.wiz.sdk.util.FileUtil;
import cn.wiz.sdk.util.TimeUtil;
import cn.wiz.sdk.util.WizMisc.MD5Util;

@SuppressWarnings("unchecked")
public class WizKSXmlRpcServer extends WizXmlRpcServer {
	protected String mToken;
	protected String mKbGUID;
	protected HashMap<String, Long> mVersions = new HashMap<String, Long>();

	public WizKSXmlRpcServer(Context ctx, String xmlrpcUrl, String userId, String token,
			String kbGUID) throws MalformedURLException {
		super(ctx, xmlrpcUrl, userId);
		mToken = token;
		mKbGUID = kbGUID;
	}

	//
	@Override
	public String getToken() {
		return mToken;
	}

	@Override
	public String getKbGUID() {
		return mKbGUID;
	}

	public long getValueVersion(String key) throws XmlRpcException, XmlRpcFault {
		return super.getValueVersion("kb", key);
	}

	public WizKeyValue getValue(String key) throws XmlRpcException, XmlRpcFault {
		return super.getValue("kb", key);
	}

	public long setValue(String key, String value) throws XmlRpcException, XmlRpcFault {
		return super.setValue("kb", key, value);
	}

	public WizKbVersion getVersions() throws XmlRpcException, XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, mToken, mKbGUID);
		XmlRpcStruct ret = call("wiz.getVersion", param);
		return decodeKbVersion(ret);
	}

	public ArrayList<WizTag> getTags(long startVersion, int count) throws XmlRpcException,
			XmlRpcFault {
		return getList("tag.getList", startVersion, count, WizTag.class);
	}

	public ArrayList<WizDocument> getDocuments(long startVersion, int count)
			throws XmlRpcException, XmlRpcFault {
		return getList("document.getSimpleList", startVersion, count, WizDocument.class);
	}
	public WizDocument getDocumentByGuid(String documentGuid) throws XmlRpcException, XmlRpcFault{
		/*
		  	String document_guid;
		    boolean document_info;
		    boolean document_data;
		    boolean document_param;
		 */
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, mToken, mKbGUID);//
		param.add("document_guid", documentGuid);
		param.add("document_info", true);
		param.add("document_data", false);
		param.add("document_param", false);
		XmlRpcStruct ret = call("document.getData", param);
		return decodeDocument(ret);
	}	
	public ArrayList<WizDocument> getDocumentsByCategory(String category, int count)
			throws XmlRpcException, XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, mToken, mKbGUID);
		param.put("count", count);
		param.put("category", category);
		return call("document.getSimpleListByCategory", param, WizDocument.class);
	}

	public ArrayList<WizDocument> getDocumentsBykey(String key, int count) throws XmlRpcException,
			XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, mToken, mKbGUID);
		param.put("count", count);
		param.put("key", key);
		param.put("first", 0);
		return call("document.getSimpleListByKey", param, WizDocument.class);
	}

	public ArrayList<WizAttachment> getAttachments(long version, int count) throws XmlRpcException,
			XmlRpcFault {
		return getList("attachment.getList", version, count, WizAttachment.class);
	}

	public ArrayList<WizDeletedGUID> getDeleteds(long version, int count) throws XmlRpcException,
			XmlRpcFault {
		return getList("deleted.getList", version, count, WizDeletedGUID.class);
	}

	public void uploadDeletedGuids(ArrayList<WizDeletedGUID> dels) throws XmlRpcException, XmlRpcFault {
		postList("deleted.postList", "deleteds", dels);
	}

	public void uploadTags(ArrayList<WizTag> tags) throws XmlRpcException, XmlRpcFault {
		postList("tag.postList", "tags", tags);
	}

	static private int PART_SIZE = 500 * 1000;

	static class WizDataDownloadResult {
		boolean eof;
		long objSize;
		long partSize;

		WizDataDownloadResult(boolean eof, long objSize, long partSize) {
			this.eof = eof;
			this.objSize = objSize;
			this.partSize = partSize;
		}
	}

	private WizDataDownloadResult dataDownload(String objGuid, String objType, long dataStartPos,
			FileOutputStream outStream) throws IOException, XmlRpcException, XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, mToken, mKbGUID);
		param.put("part_size", PART_SIZE);
		param.put("obj_guid", objGuid);
		param.put("obj_type", objType);
		param.put("start_pos", dataStartPos);
		XmlRpcStruct retData = call("data.download", param);
		//
		byte[] newData = (byte[]) retData.get("data");
		String newDataMd5 = MD5Util.makeMD5(newData);
		String serverDataMd5 = retData.getString("part_md5");
		if (!TextUtils.equals(newDataMd5, serverDataMd5)) {
			throw new XmlRpcException("Data part md5 does not match");
		}
		//
		outStream.write(newData);
		//

		boolean eof = retData.get("eof").equals("1");
		long objSize = Long.parseLong((String) retData.get("obj_size"));
		long partSize = newData.length;

		WizDataDownloadResult ret = new WizDataDownloadResult(eof, objSize, partSize);
		return ret;
	}
	
	//
	public static interface WizDownloadDataEvents {
		public abstract void onProgress(int percent);
	}

	public void downloadData(String objGuid, String objType, File destFile, WizDownloadDataEvents events) throws XmlRpcException,
			XmlRpcFault, IOException {
		boolean downloaded = false;
		FileOutputStream outStream = null;
		try {
			outStream = new FileOutputStream(destFile);

			long startPos = 0;
			boolean eof = false;

			while (!eof) {

				WizDataDownloadResult ret = dataDownload(objGuid, objType, startPos, outStream);
				//
				eof = ret.eof;
				startPos += ret.partSize;
				//
				if (events != null) {
					long objSize = ret.objSize;
					if (objSize > 0) {
						int percent = (int)(startPos * 100.0/ objSize);
						if (percent < 0)
							percent = 0;
						if (percent > 100)
							percent = 100;
						//
						events.onProgress(percent);
					}
				}
			}
			downloaded = true;
		} finally {
			outStream.close();
			if (!downloaded) {
				FileUtil.deleteFile(destFile);
			}
		}
	}

	public void downloadDocument(String guid, File file, WizDownloadDataEvents events) throws XmlRpcException, XmlRpcFault,
			IOException {
		downloadData(guid, "document", file, events);
	}

	public void downloadAttachment(String guid, File file, WizDownloadDataEvents events) throws XmlRpcException, XmlRpcFault,
			IOException {
		downloadData(guid, "attachment", file, events);
	}

	public void downloadDocument(String guid, File file) throws XmlRpcException, XmlRpcFault,
			IOException {
		downloadData(guid, "document", file, null);
	}

	public void downloadAttachment(String guid, File file) throws XmlRpcException, XmlRpcFault,
			IOException {
		downloadData(guid, "attachment", file, null);
	}

	public void dataUpload(String objGuid, String objType, String objMd5, int partCount,
			int partSn, byte[] currData) throws XmlRpcException, XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, mToken, mKbGUID);
		param.put("part_count", partCount);
		param.put("obj_md5", objMd5);
		param.put("obj_guid", objGuid);
		param.put("obj_type", objType);
		param.put("part_sn", partSn);
		param.put("part_size", currData.length);
		param.put("part_md5", MD5Util.makeMD5(currData));
		param.put("data", currData);

		call("data.upload", param);

	}

	public static int getDataPartCount(long dataSize, long partSize) {
		int partCount = (int) (dataSize / partSize);
		if ((dataSize % partSize) != 0)
			partCount++;

		return partCount;
	}

	public void updateData(String objGuid, String objType, String objMd5, File srcZipFile, long lastModified)
			throws XmlRpcException, XmlRpcFault, IOException, WizAlterModifiedException {
		FileInputStream inStream = null;
		try {
			inStream = new FileInputStream(srcZipFile);
			int partSn = 0;
			// long uploadSize = 0;
			int partSize = PART_SIZE;
			long objSize = srcZipFile.length();
			int partCount = getDataPartCount(objSize, partSize);

			byte[] partData = new byte[partSize];

			while (true) {
				checkFileModified(lastModified, srcZipFile);

				int currSize = inStream.read(partData);
				if (currSize <= 0)
					break;

				// uploadSize += currSize;
				byte[] currData = null;
				if (currSize < partSize) {
					currData = new byte[currSize];
					System.arraycopy(partData, 0, currData, 0, currSize);
				} else {
					currData = partData;
				}

				dataUpload(objGuid, objType, objMd5, partCount, partSn, currData);
				partSn++;
			}

		} finally {
			try {
				inStream.close();
			} catch (IOException e) {
			}
		}

	}

	public void uploadDocumentInfo(WizDocument doc) throws XmlRpcException, XmlRpcFault {
		WizXmlRpcParam param = new WizXmlRpcParam(mContext, mToken, mKbGUID);
		encodeDocument(doc, param);
		call("document.postSimpleData", param);
	}

	public void uploadAttachmentInfo(WizAttachment att) throws XmlRpcException, XmlRpcFault {

		WizXmlRpcParam param = new WizXmlRpcParam(mContext, mToken, mKbGUID);
		encodeAttachment(att, param);
		call("attachment.postSimpleData", param);
	}

	public void uploadDocument(WizDocument doc, File srcZipFile)
			throws XmlRpcException, XmlRpcFault, IOException, WizAlterModifiedException {

		String guid = doc.guid;

		String objMd5 = MD5Util.makeMD5ForFile(srcZipFile);
		Date dtFileModified = TimeUtil.getModifiedDateByFile(srcZipFile);
		Date dtDatabaseModified = TimeUtil.getDateFromSqlDateTimeString(doc.dateModified);
		//
		Date dtModified = dtDatabaseModified;
		if (dtFileModified.after(dtDatabaseModified)) {
			dtModified = dtDatabaseModified;
		}

		doc.dataMd5 = objMd5;
		doc.dateModified = TimeUtil.getSQLDateTimeString(dtModified);

		long lastModified = srcZipFile.lastModified();
		if (doc.localChanged == WizDocument.DOCUMENT_LOCAL_CHANGE_DATA) {
			updateData(guid, "document", objMd5, srcZipFile, lastModified);
		}

		uploadDocumentInfo(doc);

		checkFileModified(lastModified, srcZipFile);
	}

	private void checkFileModified(long oldLastModified, File file)
			throws WizAlterModifiedException {
		if (file.lastModified() != oldLastModified)
			throw new WizAlterModifiedException(-1000, "file has been modified");
	}

	public static class WizAlterModifiedException extends Exception{

		private static final long serialVersionUID = -4149667638683902212L;
		public final int errorCode;
		public WizAlterModifiedException(int errorCode, String message) {
			super( message );
	        this.errorCode = errorCode;
		}

		public int getErrorCode(){
	        return errorCode;
	    }
	}

	public void uploadAttachment(WizAttachment att, File srcZipFile)
			throws XmlRpcException, XmlRpcFault, IOException, WizAlterModifiedException {
		String guid = att.guid;
		String objMd5 = MD5Util.makeMD5ForFile(srcZipFile);
		Date dtModified = TimeUtil.getModifiedDateByFile(srcZipFile);

		att.dataMd5 = objMd5;
		att.dateModified = TimeUtil.getSQLDateTimeString(dtModified);

		long lastModified = srcZipFile.lastModified();

		updateData(guid, "attachment", objMd5, srcZipFile, lastModified);
		uploadAttachmentInfo(att);

		checkFileModified(lastModified, srcZipFile);
	}

}
