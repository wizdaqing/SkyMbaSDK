package cn.wiz.sdk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.util.EntityUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;
import cn.wiz.sdk.api.WizObject.WizCert;
import cn.wiz.sdk.api.WizObject.WizDocument;
import cn.wiz.sdk.util.Encryption.*;

public class WizMisc {
	
    public static int dip2px(Context context, float dpValue) {  
        final float scale = context.getResources().getDisplayMetrics().density;  
        return (int) (dpValue * scale + 0.5f);  
    }  
    public static int px2dip(Context context, float pxValue) {  
        final float scale = context.getResources().getDisplayMetrics().density;  
        return (int) (pxValue / scale + 0.5f);  
    }  

	// 判断sd Card是否插入
	public static boolean isSDCardAvailable() {
		try {
			return Environment.getExternalStorageState().equals(
					android.os.Environment.MEDIA_MOUNTED);
		} catch (Exception e) {
			return false;
		}
	}

	// 判断sd-Card是否插入并指定响应操作
	public static void sdCardExists(SDCardStatusAction sdCardExists) {
		if (isSDCardAvailable()) {
			sdCardExists.onSDCardAvailable();
		}else {
			sdCardExists.onSDCardUnAvailable();
		}
	}

	// 判断sd-Card是否插入并指定响应操作
	public static interface SDCardStatusAction{
		public void onSDCardAvailable();
		public void onSDCardUnAvailable();
	}

	// network判断WIFI状态
	static public boolean isWifi(Context ctx) {
		android.net.ConnectivityManager connectivity = (android.net.ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (connectivity != null) {

			android.net.NetworkInfo[] info = connectivity.getAllNetworkInfo();
			if (info != null) {
				for (int i = 0; i < info.length; i++) {
					if (info[i].getTypeName().equals("WIFI")
							&& info[i].isConnected()) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	//network判断网络状态
	static public boolean isNetworkAvailable(Context ctx) {
		
		ConnectivityManager cwjManager = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cwjManager != null && cwjManager.getActiveNetworkInfo() != null)
			return cwjManager.getActiveNetworkInfo().isAvailable();
		return false;
	}

	// network判断2G网络是否存在
	static public boolean isCMWAP(Context ctx) {
		ConnectivityManager cm = (ConnectivityManager) ctx
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		android.net.NetworkInfo info = cm.getActiveNetworkInfo();

		if (info != null && info.getTypeName().equals("MOBILE")
				&& info.getExtraInfo().equals("cmwap")) {
			return true;
		}
		return false;
	}

	
	static public String getApiUrl(String command) {
		return "http://api.wiz.cn/?p=wiz&c=" + command + "&plat=android&l=" + Locale.getDefault().toString();
	}
	/*
	 * 通过api获取一个url
	 */
	static public String getUrlFromApi(String command) throws Exception {
		try {
			HttpPost httpRequest = new HttpPost("http://api.wiz.cn/?p=wiz&c="
					+ command + "&plat=android");
			HttpResponse httpResponse = getDefaultConnection().execute(
					httpRequest);
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				String url = EntityUtils.toString(httpResponse.getEntity());
				//
				return url;
			} else {
				throw new Exception("failed to get url!");
			}
		} catch (ClientProtocolException e) {
			throw new Exception(e.getMessage());
		} catch (IOException e) {
			throw new Exception(e.getMessage());
		}
	}

	private static final int REQUEST_TIMEOUT = 30 * 1000;// 设置请求超时30秒钟
	private static final int SO_TIMEOUT = 30 * 1000; // 设置等待数据超时时间30秒钟

	static private DefaultHttpClient mClient = null;

	public static synchronized DefaultHttpClient getDefaultConnection() {
		if (mClient == null) {
			BasicHttpParams httpParams = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParams,
					REQUEST_TIMEOUT);
			HttpConnectionParams.setSoTimeout(httpParams, SO_TIMEOUT);

			SchemeRegistry schemeRegistry = new SchemeRegistry();
			schemeRegistry.register(new Scheme("http", PlainSocketFactory
					.getSocketFactory(), 80));
			ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(
					httpParams, schemeRegistry);

			mClient = new DefaultHttpClient(manager, httpParams);
		}
		//
		return mClient;
	}

	/*
	 * 使用多个字符字进行分割
	 */
	public static String[] string2Array(String text, String splitter) {
		ArrayList<String> arr = string2ArrayList(text, splitter);

		String[] temp = new String[] {};
		//
		return arr.toArray(temp);
	}

	
	/*
	 * 使用多个字符字进行分割
	 */
	public static ArrayList<String> string2ArrayList(String text,
			String splitter) {
		//
		if (TextUtils.isEmpty(text))
			text = "";
		
		ArrayList<String> arr = new ArrayList<String>();
		//
		int start = 0;
		for (int i = 0; i < text.length(); i++) {
			char ch = text.charAt(i);
			//
			if (-1 != splitter.indexOf(ch)) {
				if (i > start) {
					String sub = text.substring(start, i);
					//
					sub = sub.trim();
					if (sub != null && sub.length() > 0) {
						arr.add(sub);
					}
				}
				//
				start = i + 1;
			}
		}
		//
		if (start < text.length()) {
			arr.add(text.substring(start));
		}
		return arr;
	}
	
	//
	/*
	 * 仅使用一个字符进行分割
	 */
	public static String[] string2Array(String text, char ch) {
		//
		if (TextUtils.isEmpty(text))
			text = "";

		String reg = "\\" + Character.toString(ch);
		String[] arr = text.split(reg);
		//
		return arr;
	}

	//
	/*
	 * 仅使用一个字符进行分割
	 */
	public static ArrayList<String> string2ArrayList(String text, char ch) {
		//
		if (TextUtils.isEmpty(text))
			text = "";

		String reg = "\\" + Character.toString(ch);
		String[] arr = text.split(reg);
		//
		return array2List(arr);
	}

	public static <E> ArrayList<E> array2List(E[] arr) {
		ArrayList<E> list = new ArrayList<E>();
		List<E> eList = java.util.Arrays.asList(arr);
		list.addAll(eList);
		return list;
	}
	
	static public String objectArray2Strings(char splitter, Object... objs){
		String strings = "";
		if (isEmptyArray(objs))
			return strings;

		int length = objs.length;
		for (int i = 0; i < length; i++) {
			strings += objs[i].toString();

			if (i < length - 1) {
				strings += splitter;
			}
		}
		return strings;
	}
	
	static public String stringArray2Strings(ArrayList<String> array, char splitter) {
		String tagGuids = "";
		while (!WizMisc.isEmptyArray(array)) {
			int size = array.size();
			String guid = array.get(size - 1);
			tagGuids = tagGuids + guid;
			if (size > 1) {
				tagGuids = tagGuids + splitter;
			}
			array.remove(guid);
		}
		return tagGuids;
	}

	//
	public static String stringArray2Strings(ArrayList<String> arr, String separator) {
		String value = "";
		if (WizMisc.isEmptyArray(arr))
			return "";
		int length = arr.size();
		for (int i = 0; i < length; i++) {
			if (i != 0)
				value += separator;
			value += arr.get(i);
		}
		return value;
	}
	
	public static <E> ArrayList<E> hashSet2Array(HashSet<E> set) {
		ArrayList<E> ret = new ArrayList<E>();
		if (set == null)
			return ret;
		//
		for (E  elem : set) {
			ret.add(elem);
		}
		//
		return ret;
	}

	public static <E> HashSet<E> array2HashSet(ArrayList<E> arr) {
		HashSet<E> ret = new HashSet<E>();
		if (arr == null)
			return ret;
		//
		for (E  elem : arr) {
			ret.add(elem);
		}
		//
		return ret;
	}
	//
	public static HashSet<String> string2HashSet(String s, char ch) {
		ArrayList<String> arr = string2ArrayList(s, ch);
		return array2HashSet(arr);
	}

	public static String hashSet2String(HashSet<String> set, char ch) {
		ArrayList<String> arr = hashSet2Array(set);
		return stringArray2Strings(arr, ch);
	}
	
	public static ArrayList<String> getDocumentsGuid(ArrayList<WizDocument> documents){
		ArrayList<String> guids = new ArrayList<String>();
		if (documents == null)
			return guids;

		for (WizDocument document : documents) {
			if (document == null)
				continue;
			guids.add(document.guid);
		}

		return guids;
	}

	/**
	 * 获取百分比数据
	 */
	public static final double mRebate10 = 1.0;
	public static final double mRebate9 = 0.9;
	public static final double mRebate8 = 0.8;
	public static final double mRebate7 = 0.7;
	public static String getPercentString(String title, long numerator, long denominator,
			double rebate) {

		double douNumerator = numerator * 1.0;
		double douDenominator = denominator * 1.0;
		return getPercentString(title, douNumerator, douDenominator, rebate);
	}

	public static String getPercentString(String title, int numerator, int denominator,
			double rebate) {

		double douNumerator = numerator * 1.0;
		double douDenominator = denominator * 1.0;
		return getPercentString(title, douNumerator, douDenominator, rebate);
	}

	public static String getPercentString(String title, double numerator, double denominator,
			double rebate) {

		String percentStr = "";
		numerator = numerator * rebate;
		double result = numerator / denominator;
		DecimalFormat df = new DecimalFormat("##.##%");
		percentStr = df.format(result);
		if (!TextUtils.isEmpty(title))
			percentStr = title + ":" + percentStr;

		return percentStr;
	}

	//判断两个字符串是否相等（区别系统提供的（null!=""））
	public static boolean textEquals(String s1, String s2) {
		if (TextUtils.isEmpty(s1) && TextUtils.isEmpty(s2))
			return true;
		//
		if (TextUtils.isEmpty(s1) || TextUtils.isEmpty(s2))
			return false;
		//
		return TextUtils.equals(s1, s2);
	}

	public static double divide(double v1, double v2, int scale) {

		scale = scale < 0 ? 0 : scale;

		BigDecimal dividend = new BigDecimal(String.valueOf(v1));
		BigDecimal divisor = new BigDecimal(String.valueOf(v2));

		return dividend.divide(divisor, scale, BigDecimal.ROUND_HALF_UP)
				.doubleValue();

	}

	//比较两个arraylist是否相等
	public static boolean isEqualsArray(ArrayList<String> oldArr,
			ArrayList<String> newArr) {
		if (isEmptyArray(oldArr) && isEmptyArray(newArr))
			return true;

		if (oldArr == null || newArr == null)
			return false;

		if (oldArr.size() != newArr.size())
			return false;

		if (oldArr.equals(newArr))
			return true;

		for (int i = 0; i < newArr.size(); i++) {
			String str = newArr.get(i);
			if (oldArr.indexOf(str) >= 0)
				continue;
			return false;
		}
		return true;
	}

	//比较两个HashSet是否相等
	public static boolean isEqualsHashSet(HashSet<String> set1,
			HashSet<String> set2) {
		if (isEmptySet(set1) && isEmptySet(set2))
			return true;

		if (set1 == null || set2 == null)
			return false;

		if (set1.size() != set2.size())
			return false;

		if (set1.equals(set2))
			return true;

		for (String s1 : set1) {
			if (!set2.contains(s1))
				return false;
		}
		//
		return true;
	}

	@SuppressWarnings("rawtypes")
	public static int size(List array) {
		if (isEmptyArray(array))
			return 0;
		else
			return array.size();

	}

	/**
	 *  判断array(list)是否为空
	 */
	@SuppressWarnings("rawtypes")
	public static boolean isEmptyArray(List array) {
		return array == null || array.size() <= 0;
	}

	public static boolean isEmptyArray(Object[] obj) {
		return obj == null || obj.length <= 0;
	}
	
	@SuppressWarnings("rawtypes")
	public static boolean isEmptySet(HashSet set) {
		return set == null || set.size() <= 0;
	}

	//获取创建索引列表
	public static ArrayList<String> getNoteGuidIndexArray() {
		ArrayList<String> arr = new ArrayList<String>();
		arr.add("DOCUMENT_GUID");
		return arr;
	}

	// 获得guid
	public static String genGUID() {
		UUID uuid = UUID.randomUUID();
		String guid = uuid.toString();
		return guid;
	}

	// ////////////////html 2 text/////////////////////////
	@SuppressLint("DefaultLocale")
	public static String html2Text(String html) {
		try {
			int bodyPosition = html.toLowerCase().indexOf("<body");
			if (bodyPosition!=-1){
				html = html.substring(bodyPosition);
			}
			return WebFormatter.html2text(html);
		} catch (Exception e) {
			return "";
		}
	}

	static public class WebFormatter {

		public static String html2text(String html) {
			StringBuffer sb = new StringBuffer(html.length());
			char[] data = html.toCharArray();
			int start = 0;
			boolean previousIsPre = false;
			Token token = null;
			for (;;) {
				token = parse(data, start, previousIsPre);
				if (token == null)
					break;
				previousIsPre = token.isPreTag();
				sb = sb.append(token.getText());
				start += token.getLength();
			}
			return sb.toString();
		}

		@SuppressLint("DefaultLocale")
		private static Token parse(char[] data, int start, boolean previousIsPre) {
			if (start >= data.length)
				return null;
			// try to read next char:
			char c = data[start];
			if (c == '<') {
				// this is a tag or comment or script:
				int end_index = indexOf(data, start + 1, '>');
				if (end_index == (-1)) {
					// the left is all text!
					return new Token(Token.TOKEN_TEXT, data, start,
							data.length, previousIsPre);
				}
				String s = new String(data, start, end_index - start + 1);
				// now we got s="<...>":
				if (s.startsWith("<!--")) { // this is a comment!
					int end_comment_index = indexOf(data, start + 1, "-->");
					if (end_comment_index == (-1)) {
						// illegal end, but treat as comment:
						return new Token(Token.TOKEN_COMMENT, data, start,
								data.length, previousIsPre);
					} else
						return new Token(Token.TOKEN_COMMENT, data, start,
								end_comment_index + 3, previousIsPre);
				}
				String s_lowerCase = s.toLowerCase();
				if (s_lowerCase.startsWith("<script")) { // this is a script:
					int end_script_index = indexOf(data, start + 1, "</script>");
					if (end_script_index == (-1))
						// illegal end, but treat as script:
						return new Token(Token.TOKEN_SCRIPT, data, start,
								data.length, previousIsPre);
					else
						return new Token(Token.TOKEN_SCRIPT, data, start,
								end_script_index + 9, previousIsPre);
				} else if (s_lowerCase.startsWith("<style")) { // this is a style:
					int end_script_index = indexOf(data, start + 1, "</style>");
					if (end_script_index == (-1))
						// illegal end, but treat as script:
						return new Token(Token.TOKEN_SCRIPT, data, start,
								data.length, previousIsPre);
					else
						return new Token(Token.TOKEN_SCRIPT, data, start,
								end_script_index + 9, previousIsPre);
				} else { // this is a tag:
					return new Token(Token.TOKEN_TAG, data, start, start
							+ s.length(), previousIsPre);
				}
			}
			// this is a text:
			int next_tag_index = indexOf(data, start + 1, '<');
			if (next_tag_index == (-1))
				return new Token(Token.TOKEN_TEXT, data, start, data.length,
						previousIsPre);
			return new Token(Token.TOKEN_TEXT, data, start, next_tag_index,
					previousIsPre);
		}

		private static int indexOf(char[] data, int start, String s) {
			char[] ss = s.toCharArray();
			// TODO: performance can improve!
			for (int i = start; i < (data.length - ss.length); i++) {
				// compare from data[i] with ss[0]:
				boolean match = true;
				for (int j = 0; j < ss.length; j++) {
					if (data[i + j] != ss[j]) {
						match = false;
						break;
					}
				}
				if (match)
					return i;
			}
			return (-1);
		}

		private static int indexOf(char[] data, int start, char c) {
			for (int i = start; i < data.length; i++) {
				if (data[i] == c)
					return i;
			}
			return (-1);
		}

	}

	@SuppressWarnings("unchecked")
	static class Token {

		public static final int TOKEN_TEXT = 0; // html text.
		public static final int TOKEN_COMMENT = 1; // comment like <!-- comments... -->
		public static final int TOKEN_TAG = 2; // tag like <pre>, <font>, etc.
		public static final int TOKEN_SCRIPT = 3;

		private static final char[] TAG_BR = "<br".toCharArray();
		private static final char[] TAG_P = "<p".toCharArray();
		private static final char[] TAG_LI = "<li".toCharArray();
		private static final char[] TAG_PRE = "<pre".toCharArray();
		private static final char[] TAG_HR = "<hr".toCharArray();

		private static final char[] END_TAG_TD = "</td>".toCharArray();
		private static final char[] END_TAG_TR = "</tr>".toCharArray();
		private static final char[] END_TAG_LI = "</li>".toCharArray();

		@SuppressWarnings("rawtypes")
		private static final Map SPECIAL_CHARS = new HashMap();

		private int type;
		private String html; // original html
		private String text = null; // text!
		private int length = 0; // html length
		private boolean isPre = false; // isPre tag?

		static {
			SPECIAL_CHARS.put("&quot;", "\"");
			SPECIAL_CHARS.put("&lt;", "<");
			SPECIAL_CHARS.put("&gt;", ">");
			SPECIAL_CHARS.put("&amp;", "&");
			SPECIAL_CHARS.put("&reg;", "(r)");
			SPECIAL_CHARS.put("&copy;", "(c)");
			SPECIAL_CHARS.put("&nbsp;", " ");
			SPECIAL_CHARS.put("&pound;", "?");
		}

		public Token(int type, char[] data, int start, int end,
				boolean previousIsPre) {
			this.type = type;
			this.length = end - start;
			this.html = new String(data, start, length);
			// System.out.println("[Token] html=" + html + ".");
			parseText(previousIsPre);
			// System.out.println("[Token] text=" + text + ".");
		}

		public int getLength() {
			return length;
		}

		public boolean isPreTag() {
			return isPre;
		}

		private void parseText(boolean previousIsPre) {
			if (type == TOKEN_TAG) {
				char[] cs = html.toCharArray();
				if (compareTag(TAG_BR, cs) || compareTag(TAG_P, cs))
					text = "\n";
				else if (compareTag(TAG_LI, cs))
					text = "\n* ";
				else if (compareTag(TAG_PRE, cs))
					isPre = true;
				else if (compareTag(TAG_HR, cs))
					text = "\n--------\n";
				else if (compareString(END_TAG_TD, cs))
					text = "\t";
				else if (compareString(END_TAG_TR, cs)
						|| compareString(END_TAG_LI, cs))
					text = "\n";
			}
			// text token:
			else if (type == TOKEN_TEXT) {
				text = toText(html, previousIsPre);
			}
		}

		public String getText() {
			return text == null ? "" : text;
		}

		private String toText(String html, final boolean isPre) {
			char[] cs = html.toCharArray();
			StringBuffer buffer = new StringBuffer(cs.length);
			int start = 0;
			boolean continueSpace = false;
			char current, next;
			for (;;) {
				if (start >= cs.length)
					break;
				current = cs[start]; // read current char
				if (start + 1 < cs.length) // and next char
					next = cs[start + 1];
				else
					next = '\0';
				if (current == ' ') {
					if (isPre || !continueSpace)
						buffer = buffer.append(' ');
					continueSpace = true;
					// continue loop:
					start++;
					continue;
				}
				// not ' ', so:
				if (current == '\r' && next == '\n') {
					if (isPre)
						buffer = buffer.append('\n');
					// continue loop:
					start += 2;
					continue;
				}
				if (current == '\n' || current == '\r') {
					if (isPre)
						buffer = buffer.append('\n');
					// continue loop:
					start++;
					continue;
				}
				// cannot continue space:
				continueSpace = false;
				if (current == '&') {
					// maybe special char:
					int length = readUtil(cs, start, ';', 10);
					if (length == (-1)) { // just '&':
						buffer = buffer.append('&');
						// continue loop:
						start++;
						continue;
					} else { // check if special character:
						String spec = new String(cs, start, length);
						String specChar = (String) SPECIAL_CHARS.get(spec);
						if (specChar != null) { // special chars!
							buffer = buffer.append(specChar);
							// continue loop:
							start += length;
							continue;
						} else { // check if like '&#1234':
							if (next == '#') { // maybe a char
								String num = new String(cs, start + 2,
										length - 3);
								try {
									int code = Integer.parseInt(num);
									if (code > 0 && code < 65536) { // this is a special char:
										buffer = buffer.append((char) code);
										// continue loop:
										start++;
										continue;
									}
								} catch (Exception e) {
								}
								// just normal char:
								buffer = buffer.append("&#");
								// continue loop:
								start += 2;
								continue;
							} else { // just '&':
								buffer = buffer.append('&');
								// continue loop:
								start++;
								continue;
							}
						}
					}
				} else { // just a normal char!
					buffer = buffer.append(current);
					// continue loop:
					start++;
					continue;
				}
			}
			return buffer.toString();
		}

		// read from cs[start] util meet the specified char 'util',
		// or null if not found:
		private int readUtil(final char[] cs, final int start, final char util,
				final int maxLength) {
			int end = start + maxLength;
			if (end > cs.length)
				end = cs.length;
			for (int i = start; i < start + maxLength; i++) {
				if (cs[i] == util) {
					return i - start + 1;
				}
			}
			return (-1);
		}

		// compare standard tag "<input" with tag "<INPUT value=aa>"
		private boolean compareTag(final char[] ori_tag, char[] tag) {
			if (ori_tag.length >= tag.length)
				return false;
			for (int i = 0; i < ori_tag.length; i++) {
				if (Character.toLowerCase(tag[i]) != ori_tag[i])
					return false;
			}
			// the following char should not be a-z:
			if (tag.length > ori_tag.length) {
				char c = Character.toLowerCase(tag[ori_tag.length]);
				if (c < 'a' || c > 'z')
					return true;
				return false;
			}
			return true;
		}

		private boolean compareString(final char[] ori, char[] comp) {
			if (ori.length > comp.length)
				return false;
			for (int i = 0; i < ori.length; i++) {
				if (Character.toLowerCase(comp[i]) != ori[i])
					return false;
			}
			return true;
		}

		public String toString() {
			return html;
		}
	}

	public static String makeValidFileName(String str) {
		try {
			// 清除掉所有特殊字符
			String regEx = "[&*':'\\/?@‘：*”“’？]";
			Pattern p = Pattern.compile(regEx);
			Matcher m = p.matcher(str);
			return m.replaceAll("").trim();
		} catch (PatternSyntaxException e) {
			return str;
		}
	}

	// 去掉字符串str前面和后面的指定字符ch
	public static String trimString(String str, char ch) {
		if (str == null)
			return null;
		//
		while (str.length() > 0) {
			if (str.charAt(0) == ch) {
				str = str.substring(1);
			} else {
				break;
			}
		}
		//
		while (str.length() > 0) {
			if (str.charAt(str.length() - 1) == ch) {
				str = str.substring(0, str.length() - 1);
			} else {
				break;
			}
		}
		return str;
	}

	//获取字符串str中指定字符ch的个数
	public static int countOfChar(String str, char ch) {
		if (str == null)
			return 0;
		//
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == ch)
				count++;
		}
		return count;
	}

	//判断加密证书的完整性
	public static boolean isFullCert(WizCert cert) {
		return !TextUtils.isEmpty(cert.e)
				&& !TextUtils.isEmpty(cert.encryptedD)
				&& !TextUtils.isEmpty(cert.n);
	}

	public static class WizInvalidPasswordException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		WizInvalidPasswordException() {
			super("error password");
		}
	}

	//解密&解压笔记
	public static boolean decryptAndUnzipDocument(Activity ctx, String userId,
			WizDocument document, String key, WizCert cert) throws Exception {
		if (TextUtils.isEmpty(key) && !isFullCert(cert))
			throw new WizInvalidPasswordException();

		String guid = document.guid;
		FileInputStream in = null;
		String iv = "0123456789abcdef";
		File certFileName = document.getZipFile(ctx, userId);
		String zipFileName = document.getNotePatth(ctx);
		zipFileName = FileUtil.pathAddBackslash(zipFileName);
		zipFileName = zipFileName + guid + ".ziw";

		try {
			if (!FileUtil.fileExists(certFileName))
				return false;

			in = new FileInputStream(certFileName);

			byte[] mFileTypeArr = new byte[4];
			in.read(mFileTypeArr);

			if (!document.isEncrypted(ctx, userId))
				return true;

			int version = readIntFromStream(in);
			if (1 != version)
				return false;

			WizCertAESUtil aes = new WizCertAESUtil(key, iv);
			String mRsaD = aes.decryptString(cert.encryptedD);
			if (TextUtils.isEmpty(mRsaD))
				throw new WizInvalidPasswordException();

			int keyLength = readIntFromStream(in);
			byte[] mAESKeyArr = new byte[128];
			in.read(mAESKeyArr);

			WizCertRSAUtil rsa = new WizCertRSAUtil(cert.n, cert.e, mRsaD);
			String mAESKey = rsa.decryptStream(mAESKeyArr, 0, keyLength);

			if (TextUtils.isEmpty(mAESKey))
				throw new WizInvalidPasswordException();

			WizCertAESUtil aes1 = new WizCertAESUtil(mAESKey, iv);
			if (!aes1.decryptStream(in, zipFileName, 16))
				throw new WizInvalidPasswordException();

			if (!ZipUtil.unZipData(zipFileName, document.getNotePatth(ctx), ""))
				return false;
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		} finally {
			try {
				in.close();
			} catch (IOException e) {
			}
			FileUtil.deleteFile(zipFileName);
		}
		return true;
	}

	//
	private static int readIntFromStream(InputStream ins) throws IOException {
		byte[] data = new byte[4];
		ins.read(data);
		return byteToInt2(data);
	}

	private static int byteToInt2(byte[] res) {

		int targets = (res[0] & 0xff) | ((res[1] << 8) & 0xff00)
				| ((res[2] << 24) >>> 8) | (res[3] << 24);
		return targets;
	}

	//判断文件是否修改
	public static boolean isFileModified(File file, long lastModified) {
		String name = file.getAbsolutePath();
		File newFile = new File(name);
		if (!FileUtil.fileExists(newFile))
			return false;

		return newFile.lastModified() > lastModified;
	}

	//	清理无用的图片
	public static boolean clearUselessImage(Context ctx, String dir, String html) {
		if (TextUtils.isEmpty(html))
			html = "";

		if (!FileUtil.pathExists(dir))
			return true;

		File[] files = (new File(dir)).listFiles();

		if (isEmptyArray(files)) 
			return true;

		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			String name = file.getName();
			name = FileUtil.extractFileName(name);
			//忽略非图片文件
			if (!FileUtil.isImageFile(name))
				continue;
			
			String src = "index_files/" + name;
			if (html.indexOf(src) < 0){
				try {
					FileUtil.deleteFile(file);
				} catch (Exception e) {
					continue;
				}
			}
		}
		return true;
	}

	/**
	 * JAVA的md5操作
	 * @author zwy
	 *
	 */
	public static class MD5Util {
		// 进制转换
		private static String hexDigit(byte x) {
			StringBuffer sb = new StringBuffer();
			char c;
			// First nibble
			c = (char) ((x >> 4) & 0xf);
			if (c > 9) {
				c = (char) ((c - 10) + 'a');
			} else {
				c = (char) (c + '0');
			}
			sb.append(c);
			// Second nibble
			c = (char) (x & 0xf);
			if (c > 9) {
				c = (char) ((c - 10) + 'a');
			} else {
				c = (char) (c + '0');
			}
			sb.append(c);
			return sb.toString();
		}

		static public boolean isMD5Password(String password) {
			if (TextUtils.isEmpty(password) || password.length() != 36)//
				return false;
			// 判断以前是否有明文密码，如果有转换成MD5格式。
			return password.startsWith("md5.");
			// return "md5.".equals(password.substring(0, 4));
		}

		// throws WizNullPasswordException
		static public String makeMD5Password(String password) {
			if (TextUtils.isEmpty(password))
				return "";
			// throw new WizNullPasswordException();

			if (isMD5Password(password))
				return password;
			return "md5." + makeMD5(password);
		}

		// 对字符串加密
		static public String makeMD5(String text) {
			//
			MessageDigest md5;
			try {
				// 生成一个MD5加密计算摘要
				md5 = MessageDigest.getInstance("MD5"); // 计算md5函数
				byte b[] = text.getBytes();
				md5.update(b);
				// digest()最后确定返回md5 hash值，返回值为8wei字符串。因为md5
				// hash值是16位的hex值，实际上就是8位的字符
				byte digest[] = md5.digest();
				StringBuffer hexString = new StringBuffer();
				int digestLength = digest.length;
				for (int i = 0; i < digestLength; i++) {
					hexString.append(hexDigit(digest[i]));
				}
				return hexString.toString();
			} catch (Exception e) {
			}
			return text;
		}

		// 对字节数组加密
		static public String makeMD5(byte[] b, int size) {
			//
			MessageDigest md5;
			try {
				// 生成一个MD5加密计算摘要
				md5 = MessageDigest.getInstance("MD5"); // 计算md5函数
				// byte b[] = text.getBytes();
				md5.update(b, 0, size);
				// digest()最后确定返回md5 hash值，返回值为8wei字符串。因为md5
				// hash值是16位的hex值，实际上就是8位的字符
				byte digest[] = md5.digest();
				StringBuffer hexString = new StringBuffer();
				int digestLength = digest.length;
				for (int i = 0; i < digestLength; i++) {
					hexString.append(hexDigit(digest[i]));
				}
				return hexString.toString();
			} catch (Exception e) {
				// e.printStackTrace();
			}
			return null;
		}

		// 对字节数组加密
		static public String makeMD5(byte[] b) {
			//
			MessageDigest md5;
			try {
				// 生成一个MD5加密计算摘要
				md5 = MessageDigest.getInstance("MD5"); // 计算md5函数
				// byte b[] = text.getBytes();
				md5.update(b);
				// digest()最后确定返回md5 hash值，返回值为8wei字符串。因为md5
				// hash值是16位的hex值，实际上就是8位的字符
				byte digest[] = md5.digest();
				StringBuffer hexString = new StringBuffer();
				int digestLength = digest.length;
				for (int i = 0; i < digestLength; i++) {
					hexString.append(hexDigit(digest[i]));
				}
				return hexString.toString();
			} catch (Exception e) {
				// e.printStackTrace();
			}
			return null;
		}

		/**
		 * 默认的密码字符串组合，apache校验下载的文件的正确性用的就是默认的这个组合
		 */
		protected static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a',
				'b', 'c', 'd', 'e', 'f' };

		/**
		 * 不使用NIO的计算MD5，不会造成文件被锁死
		 * 
		 * @param file
		 * @return
		 */
		public static String makeMD5ForFile(File file) {
			String md5 = "";
			for (int i = 0; i < 3; i++) {
				FileInputStream in = null;
				MessageDigest digest = null;
				byte buffer[] = new byte[1024];
				int len;
				try {
					digest = MessageDigest.getInstance("MD5");
					in = new FileInputStream(file);

					while ((len = in.read(buffer, 0, 1024)) != -1) {
						digest.update(buffer, 0, len);
					}
					md5 = bufferToHex(digest.digest());

				} catch (NoSuchAlgorithmException e) {
					continue;
				} catch (FileNotFoundException e) {
					continue;
				} catch (IOException e) {
					continue;
				} catch (Exception ex) {
					continue;
				} finally {
					try {
						if (in != null)
							in.close();
					} catch (IOException e) {
					}
				}
				return md5;
			}
			return md5;
		}

		public static String makeMD5ForFile(String file) {
			return makeMD5ForFile(new File(file));
		}

		private static String bufferToHex(byte bytes[]) {
			return bufferToHex(bytes, 0, bytes.length);
		}

		private static String bufferToHex(byte bytes[], int m, int n) {
			StringBuffer stringbuffer = new StringBuffer(2 * n);
			int k = m + n;
			for (int l = m; l < k; l++) {
				appendHexPair(bytes[l], stringbuffer);
			}
			return stringbuffer.toString();
		}

		private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
			char c0 = hexDigits[(bt & 0xf0) >> 4];
			char c1 = hexDigits[bt & 0xf];
			stringbuffer.append(c0);
			stringbuffer.append(c1);
		}
	}
	
	//标识界面的唯一ID，用于启动界面的requestcode值
	private static int NEXT_ACTIVITY_ID = 200;
	synchronized public static int getActivityId() {
		NEXT_ACTIVITY_ID++;
		return NEXT_ACTIVITY_ID;
	}
	
	// 获取第一行
	public static String getFirstLineOfText(String text) {
		text = HTMLUtil.delAllTag(text);
		int pos = text.indexOf('\n');
		if (pos == -1) {
			if (text.length() > 100)
				return text.substring(0, 100);
			return text;
		}
		return text.substring(0, pos);
	}

	public static String getFileNameFromUri(Context ctx, Uri uri) {

		String[] filePathColumn = { android.provider.MediaStore.Images.Media.DATA };
		android.database.Cursor cursor = ctx.getContentResolver().query(uri, filePathColumn, null,
				null, null);
		if (cursor == null)
			return uri.getPath();

		try {
			cursor.moveToFirst();
			int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
			return cursor.getString(columnIndex);
		} catch (Exception err) {
			return "";
		} finally {
			cursor.close();
		}
	}

}
