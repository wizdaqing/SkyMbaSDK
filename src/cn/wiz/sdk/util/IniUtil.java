package cn.wiz.sdk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

public class IniUtil {
	
	public static class IniFile {
		private String mFileName;
		private Properties mIni;
		//
		public IniFile(String fileName) {
			mFileName = fileName;
			mIni = getFile(fileName);
		}
		//
		public String getString(String key, String def) {
			if (!mIni.containsKey(key)) {
				return def;
			}
			String ret = mIni.get(key).toString();
			return ret;
		}
		
		public void setString(String key, String val) {
			mIni.put(key, val);
		}
		
		public int getInt(String key, int def) {
			String ret = getString(key, Integer.toString(def));
			try {
				return Integer.parseInt(ret);
			}
			catch (NumberFormatException e) {
				return def;
			}
		}
		
		public void setInt(String key, int val) {
			setString(key, Integer.toString(val));
		}
		
		public void store() {
			FileOutputStream os = null;
			try {
				File file = new File(mFileName);
				os = new FileOutputStream(file);
				mIni.store(os, "");
			} catch (Exception e) {
			} finally {
				if (null != os) {
					try {
						os.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}

	static Properties getFile(String fileName) {
		FileInputStream is = null;
		//
		Properties ini = new Properties();

		try {

			File file = new File(fileName);
			is = new FileInputStream(file);
			ini.load(is);

		} catch (FileNotFoundException e) {
		} catch (Exception e) {
		} finally {
			if (null != is) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
		return ini;
	}

	public static String getIniKey(String fileName, String key, String def) {
		Properties ini = getFile(fileName);
		if (!ini.containsKey(key)) {
			return def;
		}
		String ret = ini.get(key).toString();
		return ret;
	}

	@SuppressWarnings("rawtypes")
	public static HashMap<String, String> getIniMaps(String fileName) {
		Properties ini = getFile(fileName);
		HashMap<String, String> iniMaps = new HashMap<String, String>();
		if (!ini.isEmpty()) {
			Iterator<Entry<Object, Object>> keys = ini.entrySet().iterator();
			while (keys.hasNext()) {
				java.util.Map.Entry entry = (java.util.Map.Entry) keys.next();
				String key = entry.getKey().toString();
				String value = entry.getValue().toString();
				iniMaps.put(key, value);
			}
		}
		return iniMaps;

	}

	public static void setIniKey(String fileName, String key, String value) {
		Properties ini = getFile(fileName);
		ini.put(key, value);
		//
		FileOutputStream os = null;
		try {
			File file = new File(fileName);
			os = new FileOutputStream(file);
			ini.store(os, "");
		} catch (Exception e) {
		} finally {
			if (null != os) {
				try {
					os.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static void setIniKey(String fileName, HashMap<String, String> hash) {
		Properties ini = getFile(fileName);
		ini.putAll(hash);
		FileOutputStream os = null;
		try {
			File file = new File(fileName);
			os = new FileOutputStream(file);
			ini.store(os, "");
		} catch (Exception e) {
		} finally {
			if (null != os) {
				try {
					os.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static int getIniKeyCount(String fileName) {
		Properties ini = getFile(fileName);
		return ini.size();
	}
}