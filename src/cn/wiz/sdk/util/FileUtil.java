package cn.wiz.sdk.util;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources.NotFoundException;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import cn.wiz.sdk.settings.WizAccountSettings;
@SuppressLint("NewApi")
public class FileUtil {

	
	public static boolean saveInputStreamToFile(InputStream inStream,
			String destFileName) {

		FileOutputStream outPuts = null;
		BufferedInputStream brAtt = null;
		File attFile = new File(destFileName);
		try {
			outPuts = new FileOutputStream(attFile);
			byte[] byteData = new byte[1024];
			brAtt = new BufferedInputStream(inStream);
			int len = 0;
			while ((len = brAtt.read(byteData)) != -1) {
				outPuts.write(byteData, 0, len);
			}
			return true;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				outPuts.close();
			} catch (IOException e) {
			}
		}
		return false;
	}

	public static void saveTextToFile(String fileName, String text,
			String charset) throws FileNotFoundException, IOException {

		String path = extractFilePath(fileName);
		String name = extractFileName(fileName);
		java.io.File file = new java.io.File(path, name);
		FileOutputStream out = new FileOutputStream(file);
		addUTF8Head(charset, out);
		OutputStreamWriter writer = new OutputStreamWriter(out, charset);
		writer.write(text);
		writer.flush();
		writer.close();
		out.close();
	}

	public static void saveDataToFile(String fileName, byte[] data)
			throws FileNotFoundException, IOException {
		String path = extractFilePath(fileName);
		String name = extractFileName(fileName);
		java.io.File file = new java.io.File(path, name);
		FileOutputStream out = new FileOutputStream(file);
		out.write(data);
		out.flush();
		out.close();
	}

	public static boolean updateDate2File(String fileName, String data,
			boolean superaddition) {
		FileWriter filerWriter = null;
		BufferedWriter bufWriter = null;
		try {
			File file = new File(fileName);
			// 后面这个参数代表是不是要接上文件中原来的数据，不进行覆盖
			filerWriter = new FileWriter(file, superaddition);
			bufWriter = new BufferedWriter(filerWriter);
			bufWriter.write(data);
			bufWriter.newLine();
		} catch (NotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		} catch (Exception e) {
			return false;
		} finally {
			try {
				if (bufWriter != null) {
					bufWriter.close();
				}
				if (filerWriter != null) {
					filerWriter.close();
				}
			} catch (Exception e) {
			}
		}
		return true;

	}

	static public String loadTextFromFile(String docName) {
		try {
			if (isFileUTF16(docName))
				return loadTextFromFile(docName, "UTF-16");
			else if (isFileUTF8(docName))
				return loadTextFromFile(docName, "UTF-8");
			else {
				return loadTextFromFile(docName, "UTF-8-nobom");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";

	}

	// 读取指定text文件
	public static String loadTextFromFile(String fileName, String charset)
			throws FileNotFoundException, IOException {
		//
		int offset = 0;
		if (charset.equalsIgnoreCase("utf-8")) {
			offset = 3;
		} else if (charset.equalsIgnoreCase("utf8")) {
			charset = "utf-8";
			offset = 3;
		} else if (charset.equalsIgnoreCase("unicode")) {
			charset = "UTF-16LE";
			offset = 2;
		} else if (charset.equalsIgnoreCase("utf-16")) {
			charset = "UTF-16LE";
			offset = 2;
		} else if (charset.equalsIgnoreCase("UTF-16LE")) {
			offset = 2;
		}
		if (charset.equalsIgnoreCase("utf-8-nobom")) {
			offset = 0;
			charset = "utf-8";
		} else if (charset.equalsIgnoreCase("utf8-nobom")) {
			offset = 0;
			charset = "utf-8";
		}

		//
		byte[] arr = loadByteFromFile(fileName, offset);
		if (charset == null || charset.length() == 0) {
			return new String(arr);
		} else {
			return new String(arr, charset);
		}
	}

	// 读取字节文件
	public static byte[] loadByteFromFile(String fileName, int offset)
			throws FileNotFoundException, IOException {
		byte[] ret = null;
		String path = extractFilePath(fileName);
		String name = extractFileName(fileName);
		File file = null;
		FileInputStream stream = null;
		try {
			file = new File(path, name);
			stream = new FileInputStream(file);
			int length = stream.available();
			ret = new byte[length - offset];
			stream.skip(offset);

			stream.read(ret, 0, length - offset);
		} finally {
			try {
				if (stream != null)
					stream.close();
			} catch (IOException e) {
			}
		}
		return ret;
	}

	public static InputStream getInputStreamFromAssets(Context ctx,
			String fileName) {
		try {
			AssetManager am = ctx.getAssets();
			return am.open(fileName);
		} catch (Exception e) {
			return null;
		}
	}

	public static String loadStringFromAssetsFile(Context ctx, String fileName) {
		String js = "";
		InputStream is = null;
		try {
			is = getInputStreamFromAssets(ctx, fileName);
			js = loadTextFromStream(is);
		} catch (Exception e) {
			js = "";
		} finally {
			try {
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
			}
		}
		return js;
	}

	public static boolean copyDirectory(String srcDir, String destDir) {
		if (!fileExists(srcDir))
			return false;

		if (!ensurePathExists(destDir))
			return false;
		try {
			File srcFile = new File(srcDir);
			File[] files = srcFile.listFiles();
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (file == null)
					continue;
				if (file.isDirectory()) {
					String destpath = pathAddBackslash(destDir)
							+ file.getName();
					copyDirectory(file.getAbsolutePath(), destpath);
				} else if (file.isFile()) {
					File destFile = new File(destDir, file.getName());
					copyFile(file, destFile);
				}
			}
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public static boolean copyAssetsFile(Context ctx, String srcFile,
			String destFile) {
		InputStream is = getInputStreamFromAssets(ctx, srcFile);
		OutputStream out = null;
		try {
			out = new java.io.FileOutputStream(destFile);
			copyFile(is, out);
			return true;
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		} finally {
			try {
				is.close();
			} catch (Exception e) {
			}
			try {
				out.close();
			} catch (Exception e) {
			}

		}
	}

	// 拷贝file
	public static boolean copyFile(String srcFileName, String destFileName)
			throws FileNotFoundException, IOException {
		File srcFile = new File(srcFileName);
		File destFile = new File(destFileName);
		return copyFile(srcFile, destFile);
	}

	public static boolean copyFile(File srcFile, File destFile)
			throws FileNotFoundException, IOException {

		java.io.FileInputStream stream = null;
		java.io.FileOutputStream out = null;
		try {
			stream = new java.io.FileInputStream(srcFile);
			out = new java.io.FileOutputStream(destFile);
			return copyFile(stream, out);
		} finally {
			try {
				if (stream != null) {
					stream.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
			}
		}
	}

	public static boolean copyFile(InputStream is, OutputStream out)
			throws FileNotFoundException, IOException {
		BufferedInputStream brAtt = null;
		try {
			byte[] byteData = new byte[1024];
			brAtt = new BufferedInputStream(is);
			int len = 0;
			while ((len = brAtt.read(byteData)) != -1) {
				out.write(byteData, 0, len);
			}
			return true;
		} finally {
			brAtt.close();
		}
	}

	static public boolean copyImageFile(String srcPath, String destPath,
			String fileName) {
		String type = getTypeForFileName(fileName);
		if (ATTTYPE_JPG.equals(type)) {

			srcPath = pathAddBackslash(srcPath);
			destPath = pathAddBackslash(destPath);
			String srcFile = srcPath + fileName;
			String destFile = destPath + fileName;
			try {
				if (!fileExists(destFile))
					copyFile(srcFile, destFile);

				return true;
			} catch (FileNotFoundException e) {
				return false;
			} catch (IOException e) {
				return false;
			}
		}
		return false;
	}

	public static boolean moveDirectory(String srcPath, String destPath) {
		if (FileUtil.copyDirectory(srcPath, destPath))
			return FileUtil.deleteDirectory(srcPath);

		return false;
	}

	// 转移指定文件
	public static boolean moveSpecificFiles(String oldFile, String newFile)
			throws FileNotFoundException, IOException {

		File oldfile = new File(oldFile);
		File newfile = new File(newFile);
		return moveFile(oldfile, newfile);
	}

	// 转移指定文件
	public static boolean moveFile(String srcFileName, String destFileName)
			throws FileNotFoundException, IOException {
		File srcFile = new File(srcFileName);
		File destFile = new File(destFileName);
		return moveFile(srcFile, destFile);
	}

	public static boolean moveFile(File srcFileName, File destFileName)
			throws FileNotFoundException, IOException {
		return srcFileName.renameTo(destFileName);
	}

	// 删除文件
	public static boolean deleteFile(String filename) {
		java.io.File file = new java.io.File(filename);
		return deleteFile(file);
	}

	// 删除文件
	public static boolean deleteFile(File file) {
		try {
			if (fileExists(file))
				file.delete();

			return true;
		} catch (NotFoundException e) {
			return false;
		}
	}

	/**
	 * 删除目录（文件夹）以及目录下的文件
	 * 
	 * @param dir
	 *            被删除目录的文件路径
	 * @return 目录删除成功返回true,否则返回false
	 */
	public static boolean deleteDirectory(String dir) {
		if (!pathExists(dir))
			return true;
		//
		// 如果dir不以文件分隔符结尾，自动添加文件分隔符
		if (!dir.endsWith(java.io.File.separator)) {
			dir = dir + java.io.File.separator;
		}
		File dirFile = new File(dir);
		// 如果dir对应的文件不存在，或者不是一个目录，则退出
		if (!dirFile.exists() || !dirFile.isDirectory()) {
			return false;
		}
		// 删除文件夹下的所有文件(包括子目录)
		java.io.File[] files = dirFile.listFiles();
		for (int i = 0; i < files.length; i++) {
			// 删除子文件
			if (files[i].isFile()) {
				deleteFile(files[i].getAbsolutePath());
			} else {
				// 删除子目录
				deleteDirectory(files[i].getAbsolutePath());
			}
		}

		// 删除当前目录
		return dirFile.delete();
	}

	/**
	 * : 删除目录下的文件 不删除目录下的目录
	 * 
	 * @param dir
	 *            被删除目录的文件路径
	 * @return 目录删除成功返回true,否则返回false
	 */
	public static boolean clearDirectory(String dir) {
		if (!pathExists(dir))
			return true;
		//
		// 如果dir不以文件分隔符结尾，自动添加文件分隔符
		if (!dir.endsWith(java.io.File.separator)) {
			dir = dir + java.io.File.separator;
		}
		File dirFile = new File(dir);
		// 如果dir对应的文件不存在，或者不是一个目录，则退出
		if (!dirFile.exists() || !dirFile.isDirectory()) {
			return false;
		}
		// 删除文件夹下的所有文件(包括子目录)
		java.io.File[] files = dirFile.listFiles();
		for (int i = 0; i < files.length; i++) {
			// 删除子文件
			if (files[i].isFile()) {
				deleteFile(files[i].getAbsolutePath());
			} else {
				// 删除子目录
				clearDirectory(files[i].getAbsolutePath());
			}
		}

		return true;
	}

	// 文件查找
	public static boolean fileExists(String fileName) {
		try {
			String path = extractFilePath(fileName);// 获取文件路径
			String name = extractFileName(fileName);// 获取文件名
			java.io.File file = new java.io.File(path, name);
			boolean bExists = file.exists();// 判断是否能在path中找到name文件，如果找到返回true否则返回false
			return bExists;
		} catch (Exception err) {
			err.printStackTrace();
			return false;
		}
	}

	// 文件查找
	public static boolean fileExists(File file) {
		try {
			return file.exists();
		} catch (Exception err) {
			return false;
		}
	}

	// 文件查找
	public static boolean fileExists(android.content.Context ctx, String name) {
		String[] files = ctx.fileList();
		for (int i = 0; i < files.length; i++) {
			if (files[i].equalsIgnoreCase(name))
				return true;
		}
		return false;
	}

	// 判断目录
	public static boolean pathExists(String path) {
		try {
			java.io.File myFilePath = new java.io.File(path);
			return myFilePath.exists();
		} catch (Exception err) {
			return false;
		}
	}

	// 从流中 读取文件
	public static String loadTextFromStream(InputStream stream) {
		try {
			ByteArrayOutputStream streamNew = null;
			try {
				streamNew = new ByteArrayOutputStream();
				byte[] buf = new byte[4096];
				while (true) {
					int read = stream.read(buf);
					if (read <= 0)
						break;
					streamNew.write(buf, 0, read);
				}
				//
				String charset;
				int offset = 0;
				//
				byte[] all = streamNew.toByteArray();
				if (all.length >= 2 && all[0] == (byte) 0xFF
						&& all[1] == (byte) 0xFE) {
					charset = "UTF-16LE";
					offset = 2;
				} else if (all.length >= 3 && all[0] == (byte) 0xEF
						&& all[1] == (byte) 0xBB && all[2] == (byte) 0xBF) {
					charset = "UTF-8";
					offset = 3;
				} else {
					charset = "UTF-8";
					offset = 0;
				}
				//
				return new String(all, offset, all.length - offset, charset);
			} finally {
				if (streamNew != null) {
					streamNew.close();
				}
			}
		} catch (Exception e) {
		}
		return "";
	}

	public static void addUTF8Head(String charset, FileOutputStream out) {
		if (charset.equals("utf-8") || charset.equals("UTF-8")) {
			byte[] b = { (byte) 0xef, (byte) 0xbb, (byte) 0xbf };
			try {
				out.write(b);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// 获取文件类型
	public static String extractFileExt(String filename) {
		return extractFileExt(filename, "");
	}

	public static String extractFileExt(String filename, String defExt) {
		if (!TextUtils.isEmpty(filename)) {
			int i = filename.lastIndexOf('.');

			if ((i > -1) && (i < (filename.length() - 1))) {
				return filename.substring(i);
			}
		}
		return defExt;
	}

	// 获取文件的Title部分(不包含.后面的内容也即文件类型)
	public static String extractFileTitle(String filename) {
		String title = extractFileName(filename);
		//
		if ((title != null) && (title.length() > 0)) {
			int i = title.lastIndexOf('.');
			if ((i > -1) && (i < (title.length()))) {
				return title.substring(0, i);
			}
		}
		return title;
	}

	// 获取文件路径
	public static String extractFilePath(String filename) {
		filename = pathRemoveBackslash(filename);
		//
		int pos = filename.lastIndexOf('/');
		if (-1 == pos)
			return "";
		return filename.substring(0, pos);
	}

	// 获取目录
	public static String[] getFilePath(String filename) {

		String[] paths = null;

		if (getPathSlash(filename)) {
			paths = filename.split("/");
			if (paths.length == 0) {
				paths = filename.split("\\");
			}
		} else {
			filename = filename + "/";
			paths = filename.split("/");
		}

		return paths;
	}

	// 判断字符段中是否存在反斜线
	static boolean getPathSlash(String path) {
		if (TextUtils.isEmpty(path))
			return false;
		//
		if (path.indexOf("/") == -1 && path.indexOf("\\") == -1) {
			return false;
		}
		return true;
	}

	// 获取文件名
	public static String extractFileName(String filename) {
		if (!TextUtils.isEmpty(filename) && getPathSlash(filename)) {
			int i = filename.lastIndexOf('/');
			if (i < 0)
				i = filename.lastIndexOf("\\");
			//
			if ((i > -1) && (i < (filename.length()))) {
				return filename.substring(i + 1);
			}
		}
		return filename;
	}

	public static long getFileSize(String fileName) {
		try {
			java.io.File file = new java.io.File(fileName);
			return file.length();
		} catch (Exception e) {
			return 0;
		}
	}

	public static void WriteLog(Context ctx, String log) {
		try {
			String logFileName = getLogFile(ctx);

			long fileSize = getFileSize(logFileName);
			if (fileSize > 1024 * 1024) {
				deleteFile(logFileName);
			}
			//
			FileWriter writer = new FileWriter(logFileName, true);
			try {
				writer.write("\n");
				writer.write((new Date()).toLocaleString());
				writer.write(":\t");
				writer.write(log);
			} finally {
				writer.close();
			}
		} catch (Exception err) {

		}
	}

	public static boolean isFileUTF16(String str) {
		java.io.InputStream is = null;
		byte[] b = new byte[2];
		try {
			is = new FileInputStream(new File(str));
			is.read(b);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (Exception e) {
			}
		}
		if (b[0] == (byte) 0xff && b[1] == (byte) 0xfe)
			return true;
		return false;
	}

	public static boolean isFileUTF8(String str) {
		java.io.InputStream is = null;
		byte[] b = new byte[3];
		try {
			is = new FileInputStream(new File(str));
			is.read(b);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
		} finally {
			try {
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (b[0] == (byte) 0xef && b[1] == (byte) 0xbb && b[2] == (byte) 0xbf)
			return true;
		return false;
	}

	static public String getJpegFile(String name) {
		String file = null;
		String type = getTypeForFile(name);
		if (!type.equals(".gif") && getFileType(type).equals(ATTTYPE_JPG))
			file = name;

		return file;
	}

	// 取得较大的文件
	public static File getLargeJpeg(File[] files) {
		File file = null;
		for (int i = 0; i < files.length; i++) {
			String name = files[i].getName();
			if (TextUtils.isEmpty(getJpegFile(name)))
				continue;

			if (file == null || file.length() < files[i].length())
				file = files[i];

		}
		return file;
	}

	// icon img type
	public static final int iconOfAudio = 3001;// R.drawable.icon_img_audio
	public static final int iconOfVideo = 3002;// R.drawable.icon_img_video
	public static final int iconOfWord = 3002;// R.drawable.icon_img_word
	public static final int iconOfExcel = 3003;// R.drawable.icon_img_excel
	public static final int iconOfPPt = 3004;// R.drawable.icon_img_ppt
	public static final int iconOfFile = 3005;// R.drawable.icon_img_file

	public static int iniAttIconId(String nameType) {
		String fileType = getFileType(nameType);
		if (TextUtils.equals(fileType, ATTTYPE_JPG)) {
			return 0;
		} else if (TextUtils.equals(fileType, ATTTYPE_MP3)) {
			return iconOfAudio;
		} else if (TextUtils.equals(fileType, ATTTYPE_RMVB)) {
			return iconOfVideo;
		} else if (TextUtils.equals(fileType, ATTTYPE_WORD)) {
			return iconOfWord;
		} else if (TextUtils.equals(fileType, ATTTYPE_EXECL)) {
			return iconOfExcel;
		} else if (TextUtils.equals(fileType, ATTTYPE_PPT)) {
			return iconOfPPt;
		} else {
			return iconOfFile;
		}

	}

	public static final String ATTTYPE_JPG = "JPG";
	public static final String ATTTYPE_MP3 = "MP3";
	public static final String ATTTYPE_RMVB = "RMVB";
	public static final String ATTTYPE_WORD = "WORD";
	public static final String ATTTYPE_EXECL = "EXECL";
	public static final String ATTTYPE_PPT = "PPT";

	@SuppressLint("DefaultLocale")
	public static String getFileType(String fileType) {
		if (TextUtils.isEmpty(fileType))
			fileType = "";
		fileType = fileType.toLowerCase();

		if (fileType.equals(".JPG") || fileType.equals(".jpg")
				|| fileType.equals(".png") || fileType.equals(".JPEG")
				|| fileType.equals(".jpeg") || fileType.equals(".gif")) {
			return ATTTYPE_JPG;
		} else if (fileType.equals(".amr") || fileType.equals(".mp3")
				|| fileType.equals(".wav")) {
			return ATTTYPE_MP3;
		} else if (fileType.equals(".avi") || fileType.equals(".rmvb")
				|| fileType.equals(".mp4")) {
			return ATTTYPE_RMVB;
		} else if (fileType.equals(".doc") || fileType.equals(".docx")
				|| fileType.equals(".mp4")) {
			return ATTTYPE_WORD;
		} else if (fileType.equals(".xls") || fileType.equals(".xlsx")) {
			return ATTTYPE_EXECL;
		} else if (fileType.equals(".ppt") || fileType.equals(".pptx")) {
			return ATTTYPE_PPT;
		} else {
			return "";
		}

	}

	public static boolean isImageFile(String fileName) {
		String nameType = getTypeForFile(fileName);
		String fileType = getFileType(nameType);
		return TextUtils.equals(fileType, ATTTYPE_JPG);
	}

	public static int getIntTypeFile(String fileName) {
		String nameType = getTypeForFile(fileName);
		return iniAttIconId(nameType);
	}

	public static String getTypeForFile(String name) {
		int lastIndexOf = name.lastIndexOf(".");
		if (lastIndexOf < 0)
			return "";
		return name.substring(lastIndexOf);
	}

	public static String getTypeForFileName(String name) {
		return getFileType(getTypeForFile(name));
	}
	/**
	 * wiz  mime类型匹配
	 * 优先匹配此规则，没有的话使用系统带有的mime类型。
	 */
	private static HashMap<String, String> mWizMimeType = new HashMap<String, String>();
	static{
		mWizMimeType.put("epub", "application/epub+zip");
		mWizMimeType.put("mobi", "application/x-mobipocket-ebook");
		mWizMimeType.put("umd", "application/umd");
	}
	public static String getOpenFileType(String file_CanonicalPath) {
		String extension = MimeTypeMap.getFileExtensionFromUrl(file_CanonicalPath);
		String fileType = mWizMimeType.get(extension);
		if(fileType != null){
			return fileType;
		}
		return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
	}

	public static boolean reSaveFileToUTF8(String fileName) {
		try {
			if (isFileUTF16(fileName)) {
				String str = loadTextFromFile(fileName, "utf-16");
				saveTextToFile(fileName, str, "utf-8");
				return true;
			}
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	static public String getOpenFileType(File info) {
		String filePant = info.getPath();
		String type = "";
		try {
			String file_CanonicalPath = info.getCanonicalPath();
			type = FileUtil.getOpenFileType(file_CanonicalPath);
			if (TextUtils.isEmpty(type)) {
				int lastIndex = filePant.lastIndexOf(".");
				String infoName = "wiz" + filePant.substring(lastIndex);

				File new_file = new File(infoName);
				String new_CanonicalPath = new_file.getCanonicalPath();

				type = FileUtil.getOpenFileType(new_CanonicalPath);
			}

		} catch (Exception e) {
		}
		return type;

	}

	public static boolean reSaveHtmlToUTF8(String file) {
		try {
			String html = loadTextFromFile(file, "utf-8");
			String charset = HTMLUtil.getHtmlTagVolue(html, "charset", false);
			if (TextUtils.isEmpty(charset) || charset.equals("utf-8")
					|| charset.equals("UTF-8"))
				return true;
			html = loadTextFromFile(file, charset);
			saveTextToFile(file, html, "utf-8");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/***
	 * create new Directory,
	 * 
	 * return true Said create directory success, return false Said create
	 * directory faild
	 * 
	 * @param path
	 * @return
	 */
	public static boolean ensurePathExists(String path) {
		String dir = pathAddBackslash(path);
		File myFilePath = new File(dir);
		if (myFilePath.exists())
			return true;
		//
		synchronized (FileUtil.class) {

			int count = 0;
			while (count < 10) {
				if (!myFilePath.mkdirs()) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					count++;
					myFilePath.delete();
					continue;
				}

				return true;
			}
			return false;
		}
	}
	
	
	/**
	 * @author 张维亚
	 * @E-Mail 1010482327@qq.com
	 * @version 创建时间：2012-9-28上午11:37:55
	 * @Message English:Such is mainly used for management wiz Android client some
	 *          of the directory, notes, etc of the file name
	 * @Message Chinese:此类主要用于管理为知笔记Android客户端中一些目录、笔记等的文件名，以及创建file对象
	 */
	
	//
	public static String getLogFile(Context ctx) {
		return WizAccountSettings.getDataRootPath(ctx) + "/wiz_log.txt";
	}

	// sd卡路径
	public static String getStorageCardPath() {
		return Environment.getExternalStorageDirectory().getPath();
	}

	// 私有数据目录
	public static String getRamRootPath(Context ctx) {
		String path = ctx.getFilesDir().getAbsolutePath();
		return pathAddBackslash(path);
	}

	//
	// sd card cache
	public static String getAppCacheFilePath(Context ctx) {
		return ctx.getExternalCacheDir().getPath();
	}

	// 返回临时文件夹路径
	// 用于浏览笔记，编辑笔记，程序退出的时候会清空。
	public static String getCacheRootPath(Context ctx) {
		try {
			String basePath = getAppCacheFilePath(ctx);
			basePath = pathAddBackslash(basePath);
			FileUtil.ensurePathExists(basePath);
			//
			try {
				String noMediaFileName = pathAddBackslash(basePath)
						+ ".nomedia";
				if (!FileUtil.fileExists(noMediaFileName)) {
					FileUtil.saveTextToFile(noMediaFileName, "", "utf-8");
				}
			} catch (Exception err) {
			}
			return basePath;
		} catch (Exception e) {
			return "";
		}
	}

	//
	// 去掉path中的反斜线
	public static String pathRemoveBackslash(String path) {
		if (path == null)
			return path;
		if (path.length() == 0)
			return path;
		//
		char ch = path.charAt(path.length() - 1);
		if (ch == '/' || ch == '\\')
			return path.substring(0, path.length() - 1);
		return path;

	}

	// 在path中添加反斜线
	public static String pathAddBackslash(String path) {
		if (path == null)
			return java.io.File.separator;
		if (path.length() == 0)
			return java.io.File.separator;
		//
		char ch = path.charAt(path.length() - 1);
		if (ch == '/' || ch == '\\')
			return path;
		return path + java.io.File.separator;
	}
}
