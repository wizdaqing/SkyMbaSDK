package cn.wiz.sdk.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipException;


import redstone.xmlrpc.zip.ZipEntry;
import redstone.xmlrpc.zip.ZipFile;
import redstone.xmlrpc.zip.ZipOutputStream;
import android.text.TextUtils;

public class ZipUtil {

	// 解压文件
	static public boolean unZipData(String srcData, String destPath,
			String defaultName) {
		try {
			File srcFile = new File(srcData);
			FileUtil.ensurePathExists(destPath);

			if (srcFile != null)
				unZipByApache(srcFile, destPath, defaultName);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 使用 org.apache.tools.zip.ZipFile 解压文件，它与 java 类库中的 java.util.zip.ZipFile
	 * 使用方式是一新的，只不过多了设置编码方式的 接口。
	 * 
	 * 注，apache 没有提供 ZipInputStream 类，所以只能使用它提供的ZipFile 来读取压缩文件。
	 * 
	 * @param archive
	 *            压缩包路径
	 * @param decompressDir
	 *            解压路径
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ZipException
	 */

	@SuppressWarnings("rawtypes")
	public static void unZipByApache(File srcFile, String destDir,
			String defaultName) throws IOException, FileNotFoundException,
			ZipException {
		
		ZipFile zf = new ZipFile(srcFile, "GBK");// 支持中文
		//
		try {
			Enumeration e = zf.getEntries();
			while (e.hasMoreElements()) {
				ZipEntry ze2 = (ZipEntry) e.nextElement();
				String entryName = null;
				if (TextUtils.isEmpty(defaultName)) {
					entryName = ze2.getName();
				} else {
					entryName = defaultName;
				}
				String path = destDir + "/" + entryName;
				if (ze2.isDirectory()) {
					FileUtil.ensurePathExists(path);
				} else {
					String fileDir = path.substring(0, path.lastIndexOf("/"));
					FileUtil.ensurePathExists(fileDir);
	
					FileOutputStream outStream = null;
					BufferedOutputStream bufferedOutputStream = null;
					BufferedInputStream bufferedInputStream = null;
					InputStream zipStream = null;
					try {
						outStream = new FileOutputStream(path);
						bufferedOutputStream = new BufferedOutputStream(outStream);
						
						zipStream = zf.getInputStream(ze2);
						bufferedInputStream = new BufferedInputStream(zipStream);
						byte[] readContent = new byte[1024];
						int readCount = bufferedInputStream.read(readContent);
						while (readCount != -1) {
							bufferedOutputStream.write(readContent, 0, readCount);
							readCount = bufferedInputStream.read(readContent);
						}
					}
					finally {
						try {
							if (zipStream != null) {
								zipStream.close();
							}
						} catch (IOException err) {
							
						}
						try {
							if (bufferedOutputStream != null) {
								bufferedOutputStream.close();
							}
						} catch (IOException err) {
							
						}
						try {
							if (bufferedInputStream != null) {
								bufferedInputStream.close();
							}
						} catch (IOException err) {
							
						}
						try {
							if (outStream != null) {
								outStream.close();
							}
						} catch (IOException err) {
							
						}
					}
				}
			}
		}
		finally {
			if (zf != null) {
				zf.close();
			}
		}
	}

	/*
	 * 使用Apache进行压缩 srcFile指向未压缩的文件 zipFile指向已压缩的文件
	 */
	public static void zipByApache(ArrayList<String> files, File zipFile)
			throws FileNotFoundException, IOException {
		FileOutputStream os = null;
		CheckedOutputStream cos = null;
		ZipOutputStream out = null;
		boolean boo = false;// 是否压缩成功
		try {
			os = new FileOutputStream(zipFile);
			cos = new CheckedOutputStream(os, new CRC32());
			out = new ZipOutputStream(cos);
			if (!WizMisc.isEmptyArray(files)) {
				for (int i = 0; i < files.size(); i++) {
					Zip(new File(files.get(i)), out, "", true);
				}
			}
			boo = true;
		} finally {
			try {
				if (out != null) {
					out.close();
				}
				if (cos != null) {
					cos.close();
				}
				if (os != null) {
					os.close();
				}

			} catch (IOException ex) {
				throw new RuntimeException("关闭Zip输出流出现异常", ex);
			} finally {
				// 清理操作
				if (!boo && zipFile.exists()) {// 压缩不成功,
					zipFile.delete();
				}
			}
		}
	}

	/*
	 * 使用Apache进行压缩 srcFile指向未压缩的文件 zipFile指向已压缩的文件
	 */
	public static void ZipByApache(File srcFile, File zipFile) {
		ZipOutputStream out = null;
		boolean boo = false;// 是否压缩成功
		try {
			CheckedOutputStream cos = new CheckedOutputStream(
					new FileOutputStream(zipFile), new CRC32());
			out = new ZipOutputStream(cos);
			File files[] = srcFile.listFiles();
			if (!WizMisc.isEmptyArray(files)) {
				for (int i = 0; i < files.length; i++) {

					Zip(files[i], out, "", true);
				}
			}
			boo = true;
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (IOException ex) {
				throw new RuntimeException("关闭Zip输出流出现异常", ex);
			} finally {
				// 清理操作
				if (!boo && zipFile.exists())// 压缩不成功,
					zipFile.delete();
			}
		}
	}

	/**
	 * 压缩zip文件
	 * 
	 * @param file
	 *            压缩的文件对象
	 * @param out
	 *            输出ZIP流
	 * @param dir
	 *            相对父目录名称
	 * @param boo
	 *            是否把空目录压缩进去
	 */
	public static void Zip(File file, ZipOutputStream out, String dir,
			boolean boo) throws FileNotFoundException, IOException {
		if (file.isDirectory()) {
			File[] listFile = file.listFiles();// 得出目录下所有的文件对象
			if (WizMisc.isEmptyArray(listFile) && boo) {// 空目录压缩
				out.putNextEntry(new ZipEntry(dir + file.getName() + "/"));// 将实体放入输出ZIP流中
				return;
			} else {
				for (File cfile : listFile) {
					Zip(cfile, out, dir + file.getName() + "/", boo);// 递归压缩
				}
			}
		} else if (file.isFile()) {
			byte[] bt = new byte[2048 * 2];
			ZipEntry ze = new ZipEntry(dir + file.getName());// 构建压缩实体
			FileInputStream fis = null;
			try {
				ze.setSize(file.length());// 设置压缩前的文件大小
				out.putNextEntry(ze); // 将实体放入输出ZIP流中
				fis = new FileInputStream(file);
				int i = 0;
				while ((i = fis.read(bt)) != -1) {// 循环读出并写入输出Zip流中
					out.write(bt, 0, i);
				}
			} catch (FileNotFoundException ex) {
				throw ex;
			} catch (IOException ex) {
				throw new IOException("写入压缩文件出现异常");
			} finally {
				try {
					if (fis != null)
						fis.close();
				} catch (IOException ex) {
					throw new IOException("关闭输入流出现异常");
				}
			}
		}
	}


	/*
	 * 使用Apache进行压缩 srcFile指向未压缩的文件 zipFile指向已压缩的文件
	 */
	public static void zipFilesByApache(ArrayList<String> files, File zipFile, String unZipRootPath) throws FileNotFoundException, IOException {
		FileOutputStream os = null;
		CheckedOutputStream cos = null;
		ZipOutputStream out = null;
		boolean boo = false;// 是否压缩成功
		try {
			if (WizMisc.isEmptyArray(files))
				return;

			unZipRootPath = FileUtil.pathAddBackslash(unZipRootPath);
			os = new FileOutputStream(zipFile);
			cos = new CheckedOutputStream(os, new CRC32());
			out = new ZipOutputStream(cos);
			for (String file : files) {
				String dir = FileUtil.extractFilePath(file);
				dir = FileUtil.pathAddBackslash(dir);
				dir = dir.replace(unZipRootPath, "");
				zipFile(new File(file), out, dir);
			}
			boo = true;
		} finally {
			try {
				if (out != null) {
					out.close();
				}
				if (cos != null) {
					cos.close();
				}
				if (os != null) {
					os.close();
				}

			} catch (IOException ex) {
				throw new RuntimeException("关闭Zip输出流出现异常", ex);
			} finally {
				// 清理操作
				if (!boo && zipFile.exists()) {// 压缩不成功,
					zipFile.delete();
				}
			}
		}
	}

	public static void zipFile(File file, ZipOutputStream out, String dir) throws FileNotFoundException, IOException {
		byte[] bt = new byte[2048 * 2];
		ZipEntry ze = new ZipEntry(dir + file.getName());// 构建压缩实体
		FileInputStream fis = null;
		try {
			ze.setSize(file.length());// 设置压缩前的文件大小
			out.putNextEntry(ze); // 将实体放入输出ZIP流中
			fis = new FileInputStream(file);
			int i = 0;
			while ((i = fis.read(bt)) != -1) {// 循环读出并写入输出Zip流中
				out.write(bt, 0, i);
			}
		} catch (IOException ex) {
			throw new IOException("写入压缩文件出现异常");
		} finally {
			try {
				if (fis != null)
					fis.close();
			} catch (IOException ex) {
				throw new IOException("关闭输入流出现异常");
			}
		}
	}

}
