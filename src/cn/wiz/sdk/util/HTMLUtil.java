package cn.wiz.sdk.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import cn.wiz.sdk.api.WizObject.WizAttachment;
import cn.wiz.sdk.settings.WizSystemSettings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.text.Html;
import android.text.Layout;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.AlignmentSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.ParagraphStyle;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.SubscriptSpan;
import android.text.style.SuperscriptSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;

/**
 * HTML工具
 * 
 */
public class HTMLUtil {

	private static Matcher getMatcher(String regex, String str){
		Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
		return pattern.matcher(str);
	}
	/**
	 * 删除noscript标签
	 * 
	 * @param str
	 * @return
	 */
	// noscript
	private static Matcher getNoscriptMatcher(String str){
		String noscriptRegex = "<noscript[^>]*?>[\\s\\S]*?<\\/noscript>";
		return getMatcher(noscriptRegex, str);
	}

	public static String delNoscriptTag(String str) {
		Matcher scriptMatcher = getNoscriptMatcher(str);
		str = scriptMatcher.replaceAll("");
		return str.trim();
	}

	/**
	 * 删除script标签
	 * 
	 * @param str
	 * @return
	 */
	// script
	private static Matcher getScriptMatcher1(String str){
		String scriptRegex = "<script[^>]*?>";
		return getMatcher(scriptRegex, str);
	}

	// script
	private static Matcher getScriptMatcher(String str){
		String scriptRegex = "<script[^>]*?>[\\s\\S]*?<\\/script>";
		return getMatcher(scriptRegex, str);
	}

	public static String delScriptTag(String str) {

		Matcher scriptMatcher = getScriptMatcher(str);
		str = scriptMatcher.replaceAll("");

		scriptMatcher = getScriptMatcher1(str);
		str = scriptMatcher.replaceAll("");

		return str.trim();
	}

	/**
	 * 删除指定ID的script标签
	 * 
	 * @param str
	 * @return
	 */
	// script
	private static Matcher getScriptMatcher1(String str, String id){
		String scriptRegex = "<script[^>]*?id=\"" + id + "\"[^>]*?>";
		return getMatcher(scriptRegex, str);
	}

	// script
	private static Matcher getScriptMatcher(String str, String id){
		String scriptRegex = "<script[^>]*?id=\"" + id + "\"[^>]*?>[\\s\\S]*?<\\/script>";
		return getMatcher(scriptRegex, str);
	}

	public static String delScriptTag(String str, String id) {
		Matcher scriptMatcher = getScriptMatcher(str, id);
		str = scriptMatcher.replaceAll("");

		scriptMatcher = getScriptMatcher1(str, id);
		str = scriptMatcher.replaceAll("");

		return str.trim();
	}

	/**
	 * 删除style标签
	 * 
	 * @param str
	 * @return
	 */
	// style
	private static Matcher getStyleMatcher(String str){
		String styleRegex = "<style[^>]*?>[\\s\\S]*?<\\/style>";
		return getMatcher(styleRegex, str);
	}

	public static String delStyleTag(String str) {
		Matcher styleMatcher = getStyleMatcher(str);
		str = styleMatcher.replaceAll("");
		return str;
	}
	
	/**
	 * 获取所有的标签根据ID
	 * @return
	 */
	// html tag
	private static Matcher getAllTagMatcherById(String html, String id){
		String tagRegex = "<[^>]*?id=\"" + id + "\"[^>]*?>";
		return getMatcher(tagRegex, html);
	}

	/**
	 * 删除所有标签根据标签的ID
	 * @param html
	 * @param id
	 * @return
	 */
	public static String delAllHtmlTagById(String html, String id) {
		Matcher scriptMatcher = getAllTagMatcherById(html, id);
		html = scriptMatcher.replaceAll("");
		return html.trim();
	}

	/**
	 * 删除所有标签根据标签的ID列表
	 * @param html
	 * @param id
	 * @return
	 */
	public static String delAllHtmlTagByIds(String html, String[] ids) {
		for (String id : ids) {
			html = delAllHtmlTagById(html, id);
		}
		return html.trim();
	}

	/**
	 * 
	 * @param str
	 * @return
	 */
	// html tag
	private static Matcher getAllTagMatcher(String html){
		String tagRegex = "<[^>]*>";
		return getMatcher(tagRegex, html);
	}

	public static String removeAllHtmlTag(String html) {
		Matcher scriptMatcher = getAllTagMatcher(html);
		html = scriptMatcher.replaceAll("");
		return html.trim();
	}

	public static String delHTMLTag(String html) {
		Matcher tagMatcher = getAllTagMatcher(html);
		html = tagMatcher.replaceAll("");
		return html;
	}

	public static String html2Text(String str) {
		return removeAllHtmlTag(str);
	}

	/**
	 * 删除HTML标签(空标签)
	 * 
	 * @param str
	 * @return
	 */
	public static String delNullTag(String str) {
		str = str.replaceAll(" ", "");
		str = str.replaceAll("\n", "");
		str = str.replaceAll("	", "");
		return str;
	}

	/**
	 * 删除HTML 空行
	 * 
	 * @param str
	 * @return
	 */
	public static String delBlankLine(String str) {
		str = str.replaceAll(" ", "");
		str = str.replaceAll("\r\n", "");
		str = str.replaceAll("\n\n", "");
		return str;
	}

	/**
	 * 删除meta标签
	 * 
	 * @param str
	 * @return
	 */
	// meta
	private static Matcher getMetaMatcher(String str){
		String metaRegex = "<meta[^>]*?>[\\s\\S]*?<\\/meta>";
		return getMatcher(metaRegex, str);
	}

	public static String delMetaTag(String str) {
		Matcher scriptMatcher = getMetaMatcher(str);
		str = scriptMatcher.replaceAll("");
		return str.trim();
	}

	/**
	 * 删除title标签
	 * 
	 * @param str
	 * @return
	 */
	public static String delTitleTag(String str) {

		Matcher titleMatcher = getTitleMatcher(str);
		str = titleMatcher.replaceAll("");
		return str;
	}

	// title
	private static Matcher getTitleMatcher(String html){
		String titleRegex = "<title[^>]*?>[\\s\\S]*?<\\/title>";
		return getMatcher(titleRegex, html);
	}

	/**
	 * 替换title标签
	 * 
	 * @param str
	 * @return
	 */
	public static String replaceTitleTag(String html, String title) {
		Matcher scriptMatcher = getTitleMatcher(html);
		while (scriptMatcher.find()) {
			html = html.replace(scriptMatcher.group(), title);
		}
		return html.trim();
	}

	/**
	 * 取得body标签
	 * 
	 * @param str
	 * @return
	 */
	public static String getBodyTag(String html) {
		Matcher matcher = getBodyMatcher(html);

		html = checkHtmlBodyTag(matcher, html);

		if (matcher.find()) {
			String body = matcher.group();
			if (TextUtils.isEmpty(body))
				body = "";
			return body.trim();
		}else {
			return html;
		}
	}

	private static Matcher getBodyMatcher(String html){
		// body
		String bodyRegex = "<body[^>]*?>[\\s\\S]*?<\\/body>";
		Matcher matcher = getMatcher(bodyRegex, html);
		return matcher;
	}
	
	private static String checkHtmlBodyTag(Matcher matcher, String html){

		if (!matcher.find()) {
			html = "<body>" + html + "</body>";
		}

		matcher.reset(html);

		return html;
	}
	
	/**
	 * 替换body标签
	 * 
	 * @param str
	 * @return
	 */
	public static String replaceBodyTag(String html, String body) {
		Matcher matcher = getBodyMatcher(html);

		html = checkHtmlBodyTag(matcher, html);

		while (matcher.find()) {
			html = html.replace(matcher.group(), body);
		}
		return html.trim();
	}

	/**
	 * clear css
	 * 
	 * @param str
	 * @return
	 */
	private static Matcher getCssMatcher(String html){
		// clear css
		String cssRegex = "(BODY|DIV|BLOCKQUOTE|OL|UL|)\\s*\\{.*?\\}";
		return getMatcher(cssRegex, html);
	}

	public static String clearCssTag(String html) {
		Matcher tagMatcher = getCssMatcher(html);
		html = tagMatcher.replaceAll("");
		return html.trim();
	}

	/**
	 * 删除所有标签
	 * 
	 * @param str
	 * @return
	 */
	public static String delAllTag(String str) {
		// 删noscript
		str = delNoscriptTag(str);
		// 删script
		str = delScriptTag(str);
		// 删style
		str = delStyleTag(str);
		// 删title
		str = delTitleTag(str);
		// 删HTML
		str = delHTMLTag(str);
		// 删空行
		str = delBlankLine(str);
		// 删Meta
		str = delMetaTag(str);
		return str;
	}

	/**
	 * 清除标签,恢复HTML转义字符
	 * 
	 * @param str
	 * @return
	 */
	public static String clean(String str) {
		str = delAllTag(str);
		str = str.replaceAll(SPACE, " ");
		str = str.replaceAll(GT, ">");
		str = str.replaceAll(LT, "<");
		str = str.replaceAll(QUOT, "\"");
		str = str.replaceAll(AMP, "&");
		str = str.replaceAll(COPYRIGHT, "©");
		str = str.replaceAll(REG, "®");
		str = str.replaceAll(TM, "™");
		str = str.replaceAll(RMB, "¥");
		return str;
	}

	// >
	private static final String GT = "&gt;";
	// <
	private static final String LT = "&lt;";
	// "
	private static final String QUOT = "&quot;";
	// &
	private static final String AMP = "&amp;";
	// 空格
	private static final String SPACE = "&nbsp;";
	// ©
	private static final String COPYRIGHT = "&copy;";
	// ®
	private static final String REG = "&reg;";
	// ™
	private static final String TM = "&trade;";
	// ¥
	private static final String RMB = "&yen;";

	/**
	 * 恢复HTML转义字符
	 * 
	 * @param str
	 * @return
	 */
	public static String recoverHtml(String str) {
		// str = delAllTag(str);
		str = str.replaceAll(SPACE, " ");
		str = str.replaceAll(GT, ">");
		str = str.replaceAll(LT, "<");
		str = str.replaceAll(QUOT, "\"");
		str = str.replaceAll(AMP, "&");
		str = str.replaceAll(COPYRIGHT, "©");
		str = str.replaceAll(REG, "®");
		str = str.replaceAll(TM, "™");
		str = str.replaceAll(RMB, "¥");
		return str;
	}

	/**
	 * 过滤指定标签
	 * 
	 * @param str
	 * @param tag
	 *            指定标签
	 * @return String
	 */
	public static String fiterHtmlTag(String str, String tag) {
		String regxp = "<\\s*" + tag + "\\s+([^>]*)\\s*>";
		Pattern pattern = Pattern.compile(regxp);
		Matcher matcher = pattern.matcher(str);
		StringBuffer sb = new StringBuffer();
		boolean result1 = matcher.find();
		while (result1) {
			matcher.appendReplacement(sb, "");
			result1 = matcher.find();
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	/**
	 * 替换指定的标签
	 * 
	 * @param str
	 * @param beforeTag
	 *            要替换的标签
	 * @param tagAttrib
	 *            要替换的标签属性值
	 * @param startTag
	 *            新标签开始标记
	 * @param endTag
	 *            新标签结束标记
	 * @return String example: 替换img标签的src属性值为[img]属性值[/img]
	 */
	public static String replaceHtmlTag(String str, String beforeTag,
			String tagAttrib, String startTag, String endTag) {
		String regxpForTag = "<\\s*" + beforeTag + "\\s+([^>]*)\\s*>";
		String regxpForTagAttrib = tagAttrib + "=\"([^\"]+)\"";
		Pattern patternForTag = Pattern.compile(regxpForTag);
		Pattern patternForAttrib = Pattern.compile(regxpForTagAttrib);
		Matcher matcherForTag = patternForTag.matcher(str);
		StringBuffer sb = new StringBuffer();
		boolean result = matcherForTag.find();
		while (result) {
			StringBuffer sbreplace = new StringBuffer();
			Matcher matcherForAttrib = patternForAttrib.matcher(matcherForTag
					.group(1));
			if (matcherForAttrib.find()) {
				matcherForAttrib.appendReplacement(sbreplace, startTag
						+ matcherForAttrib.group(1) + endTag);
			}
			matcherForTag.appendReplacement(sb, sbreplace.toString());
			result = matcherForTag.find();
		}
		matcherForTag.appendTail(sb);
		return sb.toString();
	}

	public static String getHtmlTagVolue(String html, String tag,
			boolean moreUrl) {
		Pattern pattern = Pattern.compile(tag + "=[\\w\\-]+");
		Matcher matcher = pattern.matcher(html);
		StringBuffer buffer = null;
		String value = "";
		while (matcher.find()) {
			buffer = new StringBuffer();
			buffer.append(matcher.group());
			value = buffer.toString().substring(tag.length() + 1);
			if (!moreUrl)
				return value;
			value = value + "\n";
		}
		return value;
	}

	/**
	 * 基本功能：过滤所有以"<"开头以">"结尾的标签
	 * <p>
	 * 
	 * @param str
	 * @return String
	 */
	public static String filterHtml(String html) {
		Matcher matcher = getAllTagMatcher(html);
		StringBuffer sb = new StringBuffer();
		boolean result1 = matcher.find();
		while (result1) {
			matcher.appendReplacement(sb, "</wiz>" + matcher.pattern()
					+ "<wiz>");
			result1 = matcher.find();
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	// java代码和html代码转换标准
	public static String text2HtmlCore(String text) {
		String html = text;
		html = html.replace("\r", "");
		html = html.replace("&", "&amp;");
		html = html.replace("<", "&lt;");
		html = html.replace(">", "&gt;");
		html = html.replace("\n", "<br />\n");
		html = html.replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
		return html;
	}

	public static String removeHtmlRedundantTag(String html, boolean removeTagP) {
		html = html.replaceAll("<p>", "");
		if (removeTagP) {
			html = html.replaceAll("</p>", "");
		} else {
			html = html.replaceAll("</p>", "<br />");
		}
		html = html.replaceAll("\n", "<br />");
		html = html.replaceAll("\"", "'");
		return html;
	}

	public static String getUrlByString(String str, boolean moreUrl) {
		Pattern pattern = Pattern
				.compile("http://[\\w\\.\\-\\_\\=\\?\\!\\@\\#\\$\\%\\&\\*\\+\\ \\,\\'\\;/:]+");
		Matcher matcher = pattern.matcher(str);
		StringBuffer buffer = new StringBuffer();
		while (matcher.find()) {
			buffer.append(matcher.group());
			if (!moreUrl)
				return buffer.toString();
			buffer.append("\n");
		}
		return buffer.toString();
	}

	public static String removeSpecialChar(String str) {
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

	public static boolean isHyperLink(String url, String dUrl) {
		return !TextUtils.isEmpty(url) && url.indexOf("file://") < 0
				&& !url.equals(dUrl);
	}
	

	// java代码转换成html代码
	public static String text2Html(String text, String title) {
		String html = text;
		html = "<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" /><title>"
				+ title + "</title></head><body>" + html + "</body></html>";
		return html;
	}

	// image组添加到html中(html仅是字段，不是文件)
	public static String image2Html(ArrayList<WizAttachment> imageArray, String text, String title) {
		if (title == null)
			title = "";

		String html = "<html><head><meta http-equiv=\"Content-Type\" "
				+ "content=\"text/html; charset=utf-8\" /><title>" + title
				+ "</title></head><body>";
		//
		if (!TextUtils.isEmpty(text)) {
			html = html + text;
		}
		html = html + getImgSrcString(imageArray);
		return html + "</body></html>";
	}

	public static String getImgSrcString(ArrayList<WizAttachment> imageArray) {
		String html = "";
		while (!WizMisc.isEmptyArray(imageArray)) {
			WizAttachment image = imageArray.get(imageArray.size() - 1);
			imageArray.remove(image);
			html = html + "<img src=\"index_files/" + image.name + "\"></img></p>";
		}
		return html;
	}
	

	//插入图片到HTML底部
	public static void insertImage2HtmlBottom(Context ctx, String destFile,
			String text, String title, ArrayList<WizAttachment> imageArray)
			throws FileNotFoundException, IOException {
		String html = "";
		html = FileUtil.loadTextFromFile(destFile);
		if (!isEmptyStringHtml(text)) {
			String imgSrcString = getImgSrcString(imageArray);
			text = text + imgSrcString;
			html = text2Html(html, text, title);
		} else {
			String imgSrcString = getImgSrcString(imageArray);
			if (html.indexOf("</body>") >= 0) {
				html = html.replace("</body>", imgSrcString + "</body>");
			} else if (html.indexOf("</BODY>") >= 0) {
				html = html.replace("</BODY>", imgSrcString + "</body>");
			}
		}

		String bottomDiv = WizJSAction.getClickUpdateDiv(ctx, false);
		if (html.indexOf(bottomDiv) >= 0) {
			html = html.replaceAll(bottomDiv, "");
		}
		html = html.replace("</body>", bottomDiv + "</body>");
		FileUtil.saveTextToFile(destFile, html, "utf-8");
	}

	//	Text转化成HTML
	public static String text2Html(String html, String body, String title) {
		String oldBody = getBodyTag(html);
		if (TextUtils.isEmpty(html) || TextUtils.isEmpty(oldBody)) {
			html = text2Html(body, title);
		} else {
			body = "<body>" + body + "</body>";
			title = "<title>" + title + "</title>";
			html = replaceBodyTag(html, body);
			html = replaceTitleTag(html, title);
		}
		return html;
	}

	// 去掉html
	public static String html2Text(String input, int length) {

		if (TextUtils.isEmpty(input)) {
			return "";
		}
		// 恢复成html
		input = recoverHtml(input);
		// 去掉所有html元素,
		input = delAllTag(input);

		input = Html.fromHtml(input).toString();

		input = input.trim();
		if (length <= 0)
			return input;
		//
		input = delNullTag(input);
		int len = input.length();
		if (len <= length) {
			return input;
		} else {
			input = input.substring(0, length);
			input += "......";
		}
		return input;
	}

	public static boolean isEmptyStringHtml(String text) {
		if (TextUtils.isEmpty(text))
			return true;

		text = removeAllHtmlTag(text).trim();
		return TextUtils.isEmpty(text);
	}

	public static String getHtmlBody(String file) {
		String html = FileUtil.loadTextFromFile(file);
		return getBodyTag(html);
	}

	public static String restoreBody(String html) {
		return restoreBodyEnd(restoreBodyStart(html));
	}

	public static String restoreBodyStart(String html) {
		if (html.indexOf("<body>") >= 0 || html.indexOf("<BODY>") >= 0)
			return html;

		if (html.indexOf("</head>") >= 0)
			return html.replace("</head>", "</head><body>");

		if (html.indexOf("</HEAD>") >= 0)
			return html.replace("</HEAD>", "</head><body>");

		if (html.indexOf("<html>") >= 0)
			return html.replace("<html>", "<html><body>");

		if (html.indexOf("<HTML>") >= 0)
			return html.replace("<HTML>", "<HTML><body>");

		return "<body>" + html;

	}

	public static String restoreBodyEnd(String html) {
		if (html.indexOf("</body>") > 0 || html.indexOf("</BODY>") > 0)
			return html;

		if (html.indexOf("</html>") > 0)
			return html.replace("</html>", "</body></html>");

		if (html.indexOf("</HTML>") > 0)
			return html.replace("</HTML>", "</body></HTML>");

		return html + "</body>";

	}

	// 添加指定标签包围HTML的每一段字符
	public static class HtmlLabel {

		static String inputHtml = "";

		static public boolean isSpace(char c) {
			if (c == ' ')
				return true;
			if (c == '\t')
				return true;
			if (c == '\r')
				return true;
			if (c == '\n')
				return true;
			if (c == '\f')
				return true;
			return false;
		}

		@SuppressLint("DefaultLocale")
		static public boolean isScriptOrStyle(int p) {
			if (inputHtml.length() < p + 6)
				return false;
			//
			String str = inputHtml.substring(p, p + 6);
			str = str.toLowerCase();
			//
			if (str.startsWith("script")) {
				return true;
			} else if (str.startsWith("style")) {
				return true;
			}
			//
			return false;
		}

		static public boolean isSpaceString(int pTextBegin, int pTextEnd) {
			String str = inputHtml.substring(pTextBegin, pTextEnd);
			str = str.trim();
			return str.length() == 0;
		}

		static public boolean isInScriptOrStyleTag(int p, int pTextBegin) {
			while (pTextBegin >= p) {
				if (inputHtml.charAt(pTextBegin) == '<') {
					pTextBegin++;
					return isScriptOrStyle(pTextBegin);
				} else {
					pTextBegin--;
				}
			}
			//
			return false;
		}

		static public class TextRange {
			int pTextBegin;
			int pTextEnd;

			TextRange(int b, int e) {
				pTextBegin = b;
				pTextEnd = e;
			}
		}

		static public TextRange findTextBegin(int p) {
			int pBegin = p;
			//
			while (true) {
				p = inputHtml.indexOf('>', p);
				if (-1 == p) {
					return null;
				}

				p++;
				int pTextBegin = p;
				//
				int pTextEnd = inputHtml.indexOf('<', pTextBegin);
				if (-1 == pTextEnd)
					return null;
				//
				p = pTextEnd;
				//
				if (pTextEnd - pTextBegin <= 1) // empty text tag
					continue;
				//
				if (isSpaceString(pTextBegin, pTextEnd))
					continue;
				//
				if (isInScriptOrStyleTag(pBegin, pTextBegin))
					continue;
				//
				return new TextRange(pTextBegin, pTextEnd);
			}
		}

		// 添加标签--wiz
		public static String addWizTagToHtml(String html) {
			inputHtml = html;
			StringBuilder strRet = new StringBuilder();
			strRet.ensureCapacity(inputHtml.length() * 2);
			//
			int p = 0;

			while (true) {
				TextRange rgn = findTextBegin(p);
				if (rgn != null) {
					int pTextBegin = rgn.pTextBegin;
					int pTextEnd = rgn.pTextEnd;
					String t = inputHtml.substring(p, pTextBegin);
					strRet.append(t);
					strRet.append("<wiz>");
					t = inputHtml.substring(pTextBegin, pTextEnd);
					strRet.append(t);
					strRet.append("</wiz>");
					p = pTextEnd;
				} else {
					strRet.append(inputHtml.substring(p));
					break;
				}
			}
			//
			return strRet.toString();
		}
	}

	public static String getImageSrc(String file) {
		return "index_files/" + FileUtil.extractFileName(file);
	}

	// html 转化成字符串和字符串转化成HTML(带有标签)//
	/**
	 * This class processes HTML strings into displayable styled text. Not all
	 * HTML tags are supported.
	 */
	public static class WizSpanned2Html {

		/**
		 * Returns an HTML representation of the provided Spanned text.
		 */
		public static String toHtml(Spanned text) {
			StringBuilder out = new StringBuilder();
			withinHtml(out, text);
			return out.toString();
		}

		private static void withinHtml(StringBuilder out, Spanned text) {
			int len = text.length();

			int next;
			for (int i = 0; i < text.length(); i = next) {
				next = text.nextSpanTransition(i, len, ParagraphStyle.class);
				ParagraphStyle[] style = text.getSpans(i, next,
						ParagraphStyle.class);
				String elements = " ";
				boolean needDiv = false;

				for (int j = 0; j < style.length; j++) {
					if (style[j] instanceof AlignmentSpan) {
						Layout.Alignment align = ((AlignmentSpan) style[j])
								.getAlignment();
						needDiv = true;
						if (align == Layout.Alignment.ALIGN_CENTER) {
							elements = "align=\"center\" " + elements;
						} else if (align == Layout.Alignment.ALIGN_OPPOSITE) {
							elements = "align=\"right\" " + elements;
						} else {
							elements = "align=\"left\" " + elements;
						}
					}
				}
				if (needDiv) {
					out.append("<div " + elements + ">");
				}

				withinDiv(out, text, i, next);

				if (needDiv) {
					out.append("</div>");
				}
			}
		}

		@SuppressWarnings("unused")
		private static void withinDiv(StringBuilder out, Spanned text,
				int start, int end) {
			int next;
			for (int i = start; i < end; i = next) {
				next = text.nextSpanTransition(i, end, QuoteSpan.class);
				QuoteSpan[] quotes = text.getSpans(i, next, QuoteSpan.class);

				for (QuoteSpan quote : quotes) {
					out.append("<blockquote>");
				}

				withinBlockquote(out, text, i, next);

				for (QuoteSpan quote : quotes) {
					out.append("</blockquote>\n");
				}
			}
		}

		private static void withinBlockquote(StringBuilder out, Spanned text,
				int start, int end) {
			// out.append("<p>");
			out.append("");

			int next;
			for (int i = start; i < end; i = next) {
				next = TextUtils.indexOf(text, '\n', i, end);
				if (next < 0) {
					next = end;
				}

				int nl = 0;

				while (next < end && text.charAt(next) == '\n') {
					nl++;
					next++;
				}

				withinParagraph(out, text, i, next - nl, nl, next == end);
			}

			// out.append("</p>\n");
			out.append("\n");
		}

		private static void withinParagraph(StringBuilder out, Spanned text,
				int start, int end, int nl, boolean last) {
			int next;
			for (int i = start; i < end; i = next) {
				next = text.nextSpanTransition(i, end, CharacterStyle.class);
				CharacterStyle[] style = text.getSpans(i, next,
						CharacterStyle.class);

				for (int j = 0; j < style.length; j++) {
					if (style[j] instanceof StyleSpan) {
						int s = ((StyleSpan) style[j]).getStyle();

						if ((s & Typeface.BOLD) != 0) {
							out.append("<b>");
						}
						if ((s & Typeface.ITALIC) != 0) {
							out.append("<i>");
						}
					}
					if (style[j] instanceof TypefaceSpan) {
						String s = ((TypefaceSpan) style[j]).getFamily();

						if (s.equals("monospace")) {
							out.append("<tt>");
						}
					}
					if (style[j] instanceof SuperscriptSpan) {
						out.append("<sup>");
					}
					if (style[j] instanceof SubscriptSpan) {
						out.append("<sub>");
					}
					if (style[j] instanceof UnderlineSpan) {
						out.append("<u>");
					}
					if (style[j] instanceof StrikethroughSpan) {
						out.append("<strike>");
					}
					if (style[j] instanceof URLSpan) {
						out.append("<a href='");
						out.append(((URLSpan) style[j]).getURL());
						out.append("'>");
					}
					if (style[j] instanceof ImageSpan) {
						String src = ((ImageSpan) style[j]).getSource();
						out.append("<img src=\"");
						out.append(getImageSrc(src));
						out.append("\">");

						i = next;
					}
					if (style[j] instanceof AbsoluteSizeSpan) {
						out.append("<font size ='");
						out.append(((AbsoluteSizeSpan) style[j]).getSize() / 6);
						out.append("'>");
					}
					if (style[j] instanceof ForegroundColorSpan) {
						out.append("<font color ='#");
						String color = Integer
								.toHexString(((ForegroundColorSpan) style[j])
										.getForegroundColor() + 0x01000000);
						while (color.length() < 6) {
							color = "0" + color;
						}
						out.append(color);
						out.append("'>");
					}
				}

				withinStyle(out, text, i, next);

				for (int j = style.length - 1; j >= 0; j--) {
					if (style[j] instanceof ForegroundColorSpan) {
						out.append("</font>");
					}
					if (style[j] instanceof AbsoluteSizeSpan) {
						out.append("</font>");
					}
					if (style[j] instanceof URLSpan) {
						out.append("</a>");
					}
					if (style[j] instanceof StrikethroughSpan) {
						out.append("</strike>");
					}
					if (style[j] instanceof UnderlineSpan) {
						out.append("</u>");
					}
					if (style[j] instanceof SubscriptSpan) {
						out.append("</sub>");
					}
					if (style[j] instanceof SuperscriptSpan) {
						out.append("</sup>");
					}
					if (style[j] instanceof TypefaceSpan) {
						String s = ((TypefaceSpan) style[j]).getFamily();

						if (s.equals("monospace")) {
							out.append("</tt>");
						}
					}
					if (style[j] instanceof StyleSpan) {
						int s = ((StyleSpan) style[j]).getStyle();

						if ((s & Typeface.BOLD) != 0) {
							out.append("</b>");
						}
						if ((s & Typeface.ITALIC) != 0) {
							out.append("</i>");
						}
					}
				}
			}

			// String p = last ? "" : "</p>\n<p>";
			String p = last ? "" : "\n";

			if (nl == 1) {
				// out.append("<br>\n");
				out.append("<br>");
				// } else if (nl == 2) {
				// out.append(p);
			} else {
				for (int i = 1; i < nl; i++)
					out.append("<br>");

				out.append(p);
			}
		}

		private static void withinStyle(StringBuilder out, Spanned text,
				int start, int end) {
			for (int i = start; i < end; i++) {
				char c = text.charAt(i);

				if (c == '<') {
					out.append("&lt;");
				} else if (c == '>') {
					out.append("&gt;");
				} else if (c == '&') {
					out.append("&amp;");
				} else if (c < ' ') {/* c > 0x7E || */
					out.append("&#" + ((int) c) + ";");
				} else if (c == ' ') {
					while (i + 1 < end && text.charAt(i + 1) == ' ') {
						out.append("&nbsp;");
						i++;
					}

					out.append(' ');
				} else {
					out.append(c);
				}
			}
		}
	}

	// 编辑或查看笔记时添加或删除JS的引用文件//
	/**
	 * @author 张维亚
	 * @E-Mail 1010482327@qq.com
	 * @version 创建时间：2012-10-25上午10:09:27
	 * @Message :
	 */

	public static class WizJSAction {

		// 返回div的ID
		public static String getDivIdValue(boolean head) {
			return "id=\"" + getDivId(head) + "\"";
		}

		// div的ID
		private static String getDivId(boolean head) {
			return head ? "divHead" : "divBottom";
		}

		private static String getDivStart(boolean head) {
			return "<div id=\"" + getDivId(head) + "\"";
		}

		private static String getDivEnd() {
			return " style=\"background-color:#eee; color: #000;\">";
		}

		public static String getClickUpdateDiv(Context ctx, boolean head) {
			return "<wiz>" + getClickInsertDiv(ctx, head) + "</wiz>";
		}

		public static String getClickInsertDiv(Context ctx, boolean head) {
			String insert = getDivStart(head) + getDivEnd();
			insert += WizSystemSettings.getTextOfInsert2html(ctx);
			return insert + "</div>";
		}
//
//		// 向HTML中插入JS引用代码(编辑笔记时才会使用该接口，引用用于编辑的点击事件处理)
//		public static boolean injectEditNoteJS2Html(Context ctx, String fileName) {
//			String body = "";
//			String html = "";
//			boolean reJectJS2Html = false;
//			try {
//				html = FileUtil.loadTextFromFile(fileName);
//				body = getBodyTag(html);
//				if (TextUtils.isEmpty(body)) {
//					html = restoreBody(html);
//					body = getBodyTag(html);
//				}
//				body = HtmlLabel.addWizTagToHtml(body);
//				String bodyStr = body.substring(0, 6);
//				if (!TextUtils.isEmpty(bodyStr)) {
//					String insert = getClickUpdateDiv(ctx, true) + "<br />";
//					body = body.replace(bodyStr, bodyStr + insert);
//					bodyStr = restoreBody(html);
//				}
//				bodyStr = body.substring(body.length() - 7, body.length());
//				if (!TextUtils.isEmpty(bodyStr)) {
//					String insert = "<br />" + getClickUpdateDiv(ctx, false);
//					body = body.replace(bodyStr, insert + bodyStr);
//				}
//				html = replaceBodyTag(html, body);
//				if (html.indexOf(jsCiteStringCommand2) < 0)
//					html = html + jsCiteStringCommand2;
//
//				FileUtil.saveTextToFile(fileName, html, "utf-8");
//				return true;
//			} catch (IndexOutOfBoundsException e) {
//				if (!reJectJS2Html) {
//					return injectEditNoteJS2Html(ctx, fileName);
//				} else {
//					return false;
//				}
//			} catch (FileNotFoundException e) {
//				return false;
//			} catch (IOException e) {
//				return false;
//			}
//		}

		public static String removeAllEditTags(Context ctx, String html, String[] delTagIds) {
			html = removeHtmlWizTag(html);
			html = removeHtmlClickInsert(ctx, html);
			html = delAllHtmlTagByIds(html, delTagIds);
			return html;
		}

		private static String removeHtmlWizTag(String html) {
			html = html.replaceAll("<wiz>", "");
			html = html.replaceAll("</wiz>", "");
			return html;
		}

		private static String removeHtmlClickInsert(Context ctx, String html) {
			html = html.replaceAll(getClickInsertDiv(ctx, true) + "<br />", "");
			html = html.replaceAll("<br />" + getClickInsertDiv(ctx, false), "");
			html = html.replaceAll(getClickInsertDiv(ctx, true), "");
			html = html.replaceAll(getClickInsertDiv(ctx, false), "");
			return html;
		}

		private static String getScriptTag(Object... args){
			String jsCiteStringCommand = "<script id=\"%1$s0\" src=\"file:///android_asset/%1$s1\" type=\"text/javascript\"/>";
			for (int i = 0; i < args.length; i++) {
				jsCiteStringCommand = jsCiteStringCommand.replace("%1$s" + i, args[i].toString());
			}

			return jsCiteStringCommand;
		}
//
//		public static void injectEditNoteJS2Html(String file, String[] jsFiles) {
//			injectJS2Html(file, "editnote.js", jsFiles);
//		}
//
//		public static void injectViewNoteJS2Html(String file, String[] jsFiles) {
//			injectJS2Html(file, "viewnote.js", jsFiles);
//		}

		// 阅读笔记时添加JS引用用于图片点击处理
		public static void injectJS2Html(String file, String jsId, String[] jsFiles) {
			if (TextUtils.isEmpty(file))
				return;

			try {
				String html = FileUtil.loadTextFromFile(file);
				if (!TextUtils.isEmpty(html)) {

					for (String string : jsFiles) {
						html += getScriptTag(jsId, string);//jsCiteStringCommand0.replace("%1$s", string);
					}
					FileUtil.saveTextToFile(file, html, "utf-8");
				}
			} catch (Exception e) {
			}
		}
	}
}