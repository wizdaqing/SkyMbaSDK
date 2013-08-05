/*
    Copyright (c) 2005 Redstone Handelsbolag

    This library is free software; you can redistribute it and/or modify it under the terms
    of the GNU Lesser General Public License as published by the Free Software Foundation;
    either version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License along with this
    library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
    Boston, MA  02111-1307  USA
 */

package redstone.xmlrpc;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import redstone.xmlrpc.util.Base64;
import android.annotation.SuppressLint;
import android.content.Context;
import cn.wiz.sdk.util.FileUtil;

/**
 * An XmlRpcParser converts inbound XML-RPC messages to their Java counterparts
 * through the use of a SAX compliant parser. This is an abstract class that is
 * only concerned with the XML-RPC values contained in a message. Deriving
 * classes supply a handleParsedValue() method that is called whenever an
 * XML-RPC value has been parsed.
 * 
 * <p>
 * If a class needs to be notified of additional parts of an XML-RPC message,
 * the startElement() or endElement() methods are overridden and extended with
 * checks for the appropriate element. This is the case with XmlRpcClient that
 * wants to know if a fault element is present. Also, the XmlRpcServer wants to
 * know the name of the method for which values are supplied.
 * </p>
 * 
 * <p>
 * Internally, the implementation uses pre-calculated hash values of the element
 * names to allow for switch() constructs when comparing elements supplied by
 * the SAX parser.
 * </p>
 * 
 * @author Greger Olsson
 */

@SuppressWarnings("unchecked")
public abstract class XmlRpcParser {
	/**
	 * Abstract method implemented by specialized message parsers like
	 * XmlRpcServer and XmlRpcClient. The method is called for every parsed
	 * top-level value. That is, it is called once for arrays and structs, and
	 * not once for every element.
	 * 
	 * @param obj
	 *            The parsed value object. Can be String, Integer, Double,
	 *            Boolean, Date, Vector, byte[], or Map objects.
	 */

	protected abstract void handleParsedValue(Object obj);

	/*
	 * HashMap
	 */
	XmlRpcStruct decodeStructNode(Node nodeStruct) {

		XmlRpcStruct ret = new XmlRpcStruct();// HashMap
		NodeList children = nodeStruct.getChildNodes();
		//
		for (int i = 0; i < children.getLength(); i++) {
			Node nodeMember = children.item(i);
			String childNodeName = nodeMember.getNodeName();
			if (!childNodeName.equals("member"))
				throw new XmlRpcException(
						"Parse XML Error: Failed to decode struct value");
			//
			Node nodeName = nodeMember.getFirstChild();
			if (nodeName == null)
				throw new XmlRpcException(
						"Parse XML Error: No struct name node");
			Node nodeValue = nodeMember.getLastChild();
			if (nodeValue == null)
				throw new XmlRpcException(
						"Parse XML Error: No struct value node");
			//
			Object obj = decodeValueNode(nodeValue);
			ret.put(getNodeValue(nodeName), obj);
		}
		//
		return ret;
	}

	/*
	 * ArrayList
	 */
	XmlRpcArray decodeArrayNode(Node nodeArray) {
		Node nodeData = nodeArray.getFirstChild();
		if (null == nodeData)
			throw new XmlRpcException("Parse XML Error: No array data node");

		//
		XmlRpcArray ret = new XmlRpcArray();
		//
		NodeList children = nodeData.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node nodeValue = children.item(i);
			//
			String childNodeNname = nodeValue.getNodeName();
			//
			if (!childNodeNname.equals("value"))
				throw new XmlRpcException(
						"Parse XML Error: Array contains an invalid node");
			//
			Object value = decodeValueNode(nodeValue);
			//
			ret.add(value);
		}
		//
		return ret;
	}

	//
	@SuppressLint("SimpleDateFormat")
	private static final SimpleDateFormat dateFormatter = new SimpleDateFormat(
			"yyyyMMdd'T'HH:mm:ss");

	//
	String getNodeValue(Node nodeData) {
		if (nodeData.getNodeType() == Document.TEXT_NODE) {
			return nodeData.getNodeValue();
		}
		//
		if (!nodeData.hasChildNodes())
			return null;
		//
		Node child = nodeData.getFirstChild();
		if (null == child)
			return null;
		return child.getNodeValue();
	}

	// ��дXML��value�ӽڵ㲿��
	Object decodeValueNode(Node nodeValue) {
		String nodeName = nodeValue.getNodeName();

		if (!nodeName.equals("value"))
			throw new XmlRpcException("Parse XML Error: Not a valid value node");
		//
		if (!nodeValue.hasChildNodes()) {
			return nodeValue.getNodeValue();
		}
		//
		Node nodeData = nodeValue.getFirstChild();
		if (nodeData == null)
			throw new XmlRpcException("Parse XML Error: No data node");
		if (nodeData.getNodeType() == Document.TEXT_NODE) {
			String ret = "";
			while (nodeData != null) {
				ret = ret + nodeData.getNodeValue();
				//
				nodeData = nodeData.getNextSibling();
			}
			return ret;
		}

		String valueType = nodeData.getNodeName();
		if (valueType.equals("string")) {
			return getNodeValue(nodeData);
		} else if (valueType.equals("int") || valueType.equals("i4")) {
			String val = getNodeValue(nodeData);
			return Integer.parseInt(val, 10);
		} else if (valueType.equals("boolean") || valueType.equals("bool")) {
			String val = getNodeValue(nodeData);
			boolean b = (val.equals("true") || val.equals("1")) ? true : false;
			return b;
		} else if (valueType.equals("double")) {
			String val = getNodeValue(nodeData);
			return Double.parseDouble(val);
		} else if (valueType.equals("base64")) {
			String val = getNodeValue(nodeData);
			return Base64.decode(val);
		} else if (valueType.equals("dateTime.iso8601")) {
			String val = getNodeValue(nodeData);
			synchronized (dateFormatter) {
				try {
					return dateFormatter.parse(val);
				} catch (ParseException e) {
					throw new XmlRpcException(
							"Parse XML Error: Failed to parse date time:" + val);
				}
			}
		} else if (valueType.equals("array")) {
			return decodeArrayNode(nodeData);
		} else if (valueType.equals("struct")) {
			return decodeStructNode(nodeData);
		} else if (valueType.equals("ex:i8")) {
			return getNodeValue(nodeData);
		} else if (valueType.equals("ex:nil")) {
			return null;
		} else {
			throw new XmlRpcException(
					"Parse XML Error: Not a valid value type: " + valueType);
		}
	}

	// �༭XML��params��fault���ӽڵ�
	Object decodeXML(Node nodeRoot) throws XmlRpcFault {
		Node nodeChild = nodeRoot.getFirstChild();
		if (nodeChild == null)
			throw new XmlRpcException("Parse XML Error: no root node");

		String childName = nodeChild.getNodeName();
		if (childName.equals("fault")) {
			Node nodeValue = nodeChild.getFirstChild();
			if (nodeValue == null)
				throw new XmlRpcException("Parse XML Error: no value node");
			//
			XmlRpcStruct ret = (XmlRpcStruct) decodeValueNode(nodeValue);
			String msg = ret.getString("faultString");
			int code = ret.getInteger("faultCode");
			//
			throw new XmlRpcFault(code, msg);
		} else if (childName.equals("params")) {
			Node nodeParam = nodeChild.getFirstChild();
			if (nodeParam == null)
				throw new XmlRpcException("Parse XML Error: no param node");
			//
			Node nodeValue = nodeParam.getFirstChild();
			if (nodeValue == null)
				throw new XmlRpcException("Parse XML Error: no value node");
			//
			return decodeValueNode(nodeValue);
		}

		return null;

	}

	/**
	 * Parses the XML-RPC message contained in the supplied input stream. It
	 * does so by using the current SAX driver, and will call
	 * handleParsedValue() for every top-level value contained in the message.
	 * This method can be overridden to supply additional processing, like
	 * identifying method names and such. This implementation is only concerned
	 * with the values of the message.
	 * 
	 * @param is
	 *            The input stream containing the XML-RPC message
	 * @throws XmlRpcFault
	 * @throws IOException
	 * 
	 * @throw Exception If anything went wrong during the whole parsing phase
	 */

	public void parse(String method, InputStream is) throws XmlRpcException,
			XmlRpcFault {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {

			builder = factory.newDocumentBuilder();
//			//保存xml
//			String fileName = FileUtil.getStorageCardPath() + "/ret.xml";
//			FileUtil.saveInputStreamToFile(is, fileName);
//			is = new FileInputStream(fileName);
			//
			Document dom = builder.parse(is);
			//
			//
			Object ret = decodeXML(dom.getFirstChild());
			//
			handleParsedValue(ret);
			return;
			//
			//
		} catch (ParserConfigurationException e) {
			throw new XmlRpcNetworkException("Parse XML Error: "
					+ e.getMessage());
		} catch (SAXException e) {
			throw new XmlRpcNetworkException("Parse XML Error: "
					+ e.getMessage());
		} catch (IOException e) {
			throw new XmlRpcNetworkException("Parse XML Error: "
					+ e.getMessage());
		}
		//
	}

	// ȡ��valueֵ�ӵ�XmlRpcStruct������
	String getValueFromxml(Context ctx, StringBuffer att_xml, int i,
			String key_value) {
		String now_xml = att_xml.toString();
		String key_Value_Name = "<name>" + key_value + "</name>";
		int begin = now_xml.indexOf(key_Value_Name);
		begin += i;

		int end = now_xml.indexOf("</value>", begin);
		if (end == -1) {
			FileUtil.WriteLog(ctx, now_xml.toString());
			throw new XmlRpcException(
					"Failed to get value data from response (end)");
		}
		String att_value = now_xml.substring(begin, end);

		return att_value;
	}
}