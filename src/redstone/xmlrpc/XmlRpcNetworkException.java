package redstone.xmlrpc;

public class XmlRpcNetworkException extends XmlRpcException {

	/**
	 * Creates a new exception with the supplied message.
	 * 
	 * @param message
	 *            The exception message.
	 */

	public XmlRpcNetworkException(String message) {
		super(message);
	}

	/**
	 * Creates a new exception with the supplied message. The supplied cause
	 * will be attached to the exception.
	 * 
	 * @param message
	 *            The error message.
	 * @param cause
	 *            The original cause leading to the exception.
	 */

	public XmlRpcNetworkException(String message, Throwable cause) {
		super(message, cause);
	}

	private static final long serialVersionUID = -9192969902885455181L;
}
