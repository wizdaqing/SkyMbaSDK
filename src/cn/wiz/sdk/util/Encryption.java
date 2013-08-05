package cn.wiz.sdk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import cn.wiz.sdk.util.WizMisc.MD5Util;

import redstone.xmlrpc.util.Base64;

/**
 * @Author: zwy
 * @E-mail: weiyazhang1987@gmail.com
 * @time Create Date: 2013-3-21下午4:59:18
 * Message: 用于文件的加密解密，包含AES加密技术和RS加密技术
 **/
public class Encryption {

	/***

	Copyright 2006 bsmith@qq.com

	2010 bruce@wizbrother.com

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

	 */
	
	/**
	 * aes chiper class. CBC mode with PKCS#1 v1.5 padding.
	 */
	public static class WizCertAESUtil {

		private Cipher enc;
		private Cipher dec;
		private SecretKeySpec keySpec;
		private IvParameterSpec ivSpec;

		String mKey = "";
		String mIv = "";

		public WizCertAESUtil(String key, String iv) {

			mKey = key;
			mIv = iv;

			iniAesUtil();
		}

		void iniAesUtil() {
			byte[] key = null;
			byte[] iv = null;
			key = MD5Util.makeMD5(mKey).getBytes();
			iv = mIv.getBytes();

			init(key, iv);
		}

		// 加密是使用
		void encryptStream(String mSrcFileName, String mEncFileName) {

			if (!FileUtil.fileExists(mSrcFileName))
				return;

			File srcfile = new File(mSrcFileName);
			if (srcfile.isDirectory())
				return;

			FileInputStream in = null;
			FileOutputStream out = null;

			String srcfilename = srcfile.getName();
			String encfilename = mEncFileName + "/" + srcfilename;
			try {
				in = new FileInputStream(srcfile);
				out = new FileOutputStream(encfilename);
				encryptStream(in, out);
			} catch (FileNotFoundException e) {
				e.printStackTrace();

			} finally {
				try {
					in.close();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// 解密用
		public boolean decryptStream(FileInputStream in, String mDecFileName,
				int off) {

			if (in == null)
				return false;

			FileOutputStream out = null;

			String decfilename = mDecFileName;
			try {
				out = new FileOutputStream(decfilename);
				if (off > 0)
					decryptStream(in, off, out);
				else
					decryptStream(in, out);

				return true;
			} catch (FileNotFoundException e) {
				return false;
			} finally {

				try {
					in.close();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// 解密用
		boolean decryptStream(String mEncFileName, String mDecFileName, int off) {

			if (!FileUtil.fileExists(mEncFileName))
				return false;

			File encfile = new File(mEncFileName);

			if (encfile.isDirectory())
				return false;

			FileInputStream in = null;
			FileOutputStream out = null;

			String decfilename = mDecFileName;
			try {
				in = new FileInputStream(encfile);
				out = new FileOutputStream(decfilename);
				if (off > 0)
					decryptStream(in, off, out);
				else
					decryptStream(in, out);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {

				try {
					in.close();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return true;
		}

		// 解密字符串
		public String decryptString(String ciphertext) {
			byte data[];
			String out = "";
			try {
				data = Base64.decode(ciphertext.getBytes("utf-8"));
				out = new String(decrypt(data));
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			} catch (InvalidAlgorithmParameterException e) {
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (BadPaddingException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return out;

		}

		/**
		 * init the AES key. the key must be 128, 192, or 256 bits.
		 * 
		 * @param key
		 *            the AES key.
		 * @param keyoff
		 *            the AES key offset.
		 * @param keylen
		 *            the AES key length, the key length must be 16 bytes because
		 *            SunJCE only support 16 bytes key.
		 * @param iv
		 *            the IV for CBC, the length of iv must be 16 bytes.
		 * @param ivoff
		 *            the iv offset.
		 */
		public void init(byte[] key, int keyoff, int keylen, byte[] iv, int ivoff) {
			keySpec = new SecretKeySpec(key, keyoff, keylen, "AES");
			ivSpec = new IvParameterSpec(iv, ivoff, 16);
		}

		/**
		 * init the AES key. the key must be 16 bytes, because SunJCE only support
		 * 16 bytes key..
		 * 
		 * @param key
		 *            the AES key.
		 * @param iv
		 *            the iv for CBC, iv must be 16 bytes length.
		 */
		public void init(byte[] key, byte[] iv) {
			keySpec = new SecretKeySpec(key, "AES");
			ivSpec = new IvParameterSpec(iv);
		}

		/**
		 * 
		 * get the maximal cipher data length after encrypted.
		 * 
		 * @param len
		 *            the plain data length.
		 * @return the cipher data length.
		 */
		public int getCipherLen(int len) {
			int pad = len % 16;
			if (0 == pad) {
				return len + 16;
			}
			return len - pad + 16;
		}

		/**
		 * encrypt the input data to output data. the input data length must be the
		 * times of 16 bytes. and the output data length is equals to the input
		 * data.
		 * 
		 * @param indata
		 *            the input data.
		 * @param inoff
		 *            the input data offset.
		 * @param inlen
		 *            the input data length.
		 * @param outdata
		 *            the output data.
		 * @param outoff
		 *            the output data offset.
		 */
		// 解密byte型数组变量
		public void encrypt(byte[] indata, int inoff, int inlen, byte[] outdata,
				int outoff) throws NoSuchAlgorithmException,
				NoSuchPaddingException, InvalidKeyException, ShortBufferException,
				IllegalBlockSizeException, BadPaddingException,
				InvalidAlgorithmParameterException {
			initEncryptor();
			enc.doFinal(indata, inoff, inlen, outdata, outoff);
		}

		/**
		 * encrypt the input data to output data.
		 * 
		 * @param indata
		 *            the input data.
		 * @param inoff
		 *            the input data offset.
		 * @param inlen
		 *            the input data length.
		 * @return the output encrypted data.
		 */
		// 解密byte型数组变量
		public byte[] encrypt(byte[] indata, int inoff, int inlen)
				throws NoSuchAlgorithmException, NoSuchPaddingException,
				InvalidKeyException, ShortBufferException,
				IllegalBlockSizeException, BadPaddingException,
				InvalidAlgorithmParameterException {
			initEncryptor();
			return enc.doFinal(indata, inoff, inlen);
		}

		/**
		 * encrypt the input data to output data.
		 * 
		 * @param indata
		 *            the input data.
		 * @return the output data.
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchPaddingException
		 * @throws InvalidKeyException
		 * @throws InvalidAlgorithmParameterException
		 * @throws IllegalBlockSizeException
		 * @throws BadPaddingException
		 */
		// 解密byte型数组变量
		public byte[] encrypt(byte[] indata) throws NoSuchAlgorithmException,
				NoSuchPaddingException, InvalidKeyException,
				InvalidAlgorithmParameterException, IllegalBlockSizeException,
				BadPaddingException {
			initEncryptor();
			return enc.doFinal(indata);
		}

		// private static final int defBUFFER_SIZE = 102400;

		// 加密是使用CipherOutput
		public void encryptStream(InputStream is, OutputStream os) {
			CipherOutputStream cos = null;

			try {
				initEncryptor();
				cos = new CipherOutputStream(os, enc);
				FileUtil.copyFile(is, cos);

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					cos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}

		// 解密用CipherInputStream
		public void decryptStream(InputStream is, OutputStream os) {
			CipherInputStream cis = null;
			try {
				initDecryptor();
				cis = new CipherInputStream(is, dec);
				FileUtil.copyFile(cis, os);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					cis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// 解密用CipherInputStream
		public void decryptStream(InputStream is, int off, OutputStream os) {
			CipherInputStream cis = null;
			try {
				if (off > 0) {
					byte[] b = new byte[off];
					is.read(b);
				}
				initDecryptor();
				cis = new CipherInputStream(is, dec);
				FileUtil.copyFile(cis, os);
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					cis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		/**
		 * the maximal plain data length after decrypted.
		 * 
		 * @param len
		 *            the cipher data length that will be decrypted.
		 * @return the maximal plain data length.
		 */
		public int getPlainLen(int len) {
			return len;
		}

		/**
		 * decrypt the input data to output data.
		 * 
		 * @param indata
		 *            the input data.
		 * @param inoff
		 *            the input data offset.
		 * @param inlen
		 *            the input data length.
		 * @param outdata
		 *            the output data.
		 * @param outoff
		 *            the output data offset.
		 */
		public void decrypt(byte[] indata, int inoff, int inlen, byte[] outdata,
				int outoff) throws NoSuchAlgorithmException,
				NoSuchPaddingException, InvalidKeyException, ShortBufferException,
				IllegalBlockSizeException, BadPaddingException,
				InvalidAlgorithmParameterException {
			initDecryptor();
			dec.doFinal(indata, inoff, inlen, outdata, outoff);
		}

		/**
		 * decrypt the input data to output data.
		 * 
		 * @param indata
		 *            the input data.
		 * @param inoff
		 *            the input data offset.
		 * @param inlen
		 *            the input data length.
		 * @return the output decrypted data.
		 */
		public byte[] decrypt(byte[] indata, int inoff, int inlen)
				throws NoSuchAlgorithmException, NoSuchPaddingException,
				InvalidKeyException, IllegalBlockSizeException,
				BadPaddingException, ShortBufferException,
				InvalidAlgorithmParameterException {
			initDecryptor();
			return dec.doFinal(indata, inoff, inlen);
		}

		/**
		 * decrypt the input data to output data.
		 * 
		 * @param indata
		 *            the input cipher data.
		 * @return the output plain data.
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchPaddingException
		 * @throws InvalidKeyException
		 * @throws InvalidAlgorithmParameterException
		 * @throws IllegalBlockSizeException
		 * @throws BadPaddingException
		 */
		public byte[] decrypt(byte[] indata) throws NoSuchAlgorithmException,
				NoSuchPaddingException, InvalidKeyException,
				InvalidAlgorithmParameterException, IllegalBlockSizeException,
				BadPaddingException {
			initDecryptor();
			return dec.doFinal(indata);
		}

		private void initEncryptor() {
			if (null == enc) {
				try {
					enc = Cipher.getInstance("AES/CBC/PKCS5Padding");
					enc.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (NoSuchPaddingException e) {
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					e.printStackTrace();
				} catch (InvalidAlgorithmParameterException e) {
					e.printStackTrace();
				}
			}
		}

		private void initDecryptor() {
			if (null == dec) {
				try {
					dec = Cipher.getInstance("AES/CBC/PKCS5Padding");
					dec.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
				} catch (InvalidKeyException e) {
					e.printStackTrace();
				} catch (InvalidAlgorithmParameterException e) {
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (NoSuchPaddingException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	
	
	
	/***

	Copyright 2006 bsmith@qq.com

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.

	 */
	/**
	 * rsa encrypt and signer SHA1 with PKCS#1 v1.5 padding.
	 */
	public static class WizCertRSAUtil {
		//
		// public static void dump(String label, byte[] data) {
		// String hex_str = Base16.encode(data);
		// }

		private Cipher enc; // encryptor.
		private Cipher dec; // decryptor.
		private Key key; // the enc/dec key.
		private int KEY_BYTE_LEN; // RSA key bytes length.
		String mN = "";
		// e factor in RSA, aslo called public exponent.
		String mE = "";
		// d factor in RSA, aslo called private exponent
		String mD = "";

		public WizCertRSAUtil(String n, String e, String d) {
			mN = n;
			mE = e;
			mD = d;

		}

		byte[] encryptStream(String data) {
			byte[] indata;
			byte[] outdata = null;
			try {
				indata = data.getBytes("UTF-8");
				initPublicKey(mN, mE);
				outdata = encrypt(indata);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (BadPaddingException e) {
				e.printStackTrace();
			}
			// dump("byte[] outdata", outdata);
			return outdata;

		}

		String decryptStream(byte[] data) {
			String out = "";
			try {
				initPrivateKey(mN, mE, mD);
				byte[] outdata = decrypt(data);

				out = new String(outdata);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (BadPaddingException e) {
				e.printStackTrace();
			}

			return out;

		}

		public String decryptStream(byte[] data, int offset, int length) {
			String out = "";
			try {
				initPrivateKey(mN, mE, mD);
				byte[] outdata = decrypt(data, offset, length);

				out = new String(outdata);
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (NoSuchPaddingException e) {
				e.printStackTrace();
			} catch (InvalidKeySpecException e) {
				e.printStackTrace();
			} catch (InvalidKeyException e) {
				e.printStackTrace();
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			} catch (BadPaddingException e) {
				e.printStackTrace();
			} catch (ShortBufferException e) {
				e.printStackTrace();
			}

			return out;

		}

		/**
		 * init public key to encrypt/decrypt, all operations use this key.
		 * 
		 * @param N
		 *            N factor in RSA, aslo called modulus.
		 * @param e
		 *            e factor in RSA, aslo called publicExponent.
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchPaddingException
		 * @throws InvalidKeySpecException
		 */
		public void initPublicKey(String N, String e)
				throws NoSuchAlgorithmException, NoSuchPaddingException,
				InvalidKeySpecException {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			BigInteger big_N = new BigInteger(N);
			KEY_BYTE_LEN = (big_N.bitLength()) >> 3;
			BigInteger big_e = new BigInteger(e);
			KeySpec keySpec = new RSAPublicKeySpec(big_N, big_e);
			key = keyFactory.generatePublic(keySpec);
		}

		/**
		 * init private key to encrypt/decrypt, all operations use this key.
		 * 
		 * @param N
		 *            N factor in RSA, aslo called modulus.
		 * @param e
		 *            e factor in RSA, aslo called publicExponent, ignored, just
		 *            keep compatible with C++ interface.
		 * @param d
		 *            d factor in RSA, aslo called privateExponent.
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchPaddingException
		 * @throws InvalidKeySpecException
		 */
		public void initPrivateKey(String N, String e, String d)
				throws NoSuchAlgorithmException, NoSuchPaddingException,
				InvalidKeySpecException {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			BigInteger big_N = new BigInteger(N);
			KEY_BYTE_LEN = (big_N.bitLength()) >> 3;
			BigInteger big_d = new BigInteger(d);
			KeySpec keySpec = new RSAPrivateKeySpec(big_N, big_d);
			key = keyFactory.generatePrivate(keySpec);
		}

		/**
		 * get maxim plain bytes length that RSA can encrypt.
		 * 
		 * @return the maxim length.
		 */
		public int getMaxPlainLen() {
			return KEY_BYTE_LEN - 11;
		}

		/**
		 * get cipher length that return by RSA encryption. in RSA, this length is
		 * fixed, and equals the key bytes length - 11. e.g. 1024 bits RSA key, this
		 * value is 128-11 = 117.
		 * 
		 * @return the cipher length.
		 */
		public int getCipherLen() {
			return KEY_BYTE_LEN;
		}

		/**
		 * encrypt indata to outdata use the key.
		 * 
		 * @param indata
		 *            input data.
		 * @param inoff
		 *            input data offset.
		 * @param inlen
		 *            input data length.
		 * @param outdata
		 *            output data.
		 * @param outoff
		 *            output data offset.
		 * @return the actual cipher length.
		 * @throws ShortBufferException
		 * @throws IllegalBlockSizeException
		 * @throws BadPaddingException
		 * @throws InvalidKeyException
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchPaddingException
		 */
		public int encrypt(byte[] indata, int inoff, int inlen, byte[] outdata,
				int outoff) throws ShortBufferException, IllegalBlockSizeException,
				BadPaddingException, InvalidKeyException, NoSuchAlgorithmException,
				NoSuchPaddingException {
			initEncryptor();
			return enc.doFinal(indata, inoff, inlen, outdata, outoff);
		}

		/**
		 * encrypt indata to outdata use the key.
		 * 
		 * @param indata
		 *            input data.
		 * @param inoff
		 *            input data offset.
		 * @param inlen
		 *            input data length.
		 * @return the actual cipher data.
		 * @throws ShortBufferException
		 * @throws IllegalBlockSizeException
		 * @throws BadPaddingException
		 * @throws InvalidKeyException
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchPaddingException
		 */
		public byte[] encrypt(byte[] indata, int inoff, int inlen)
				throws ShortBufferException, IllegalBlockSizeException,
				BadPaddingException, InvalidKeyException, NoSuchAlgorithmException,
				NoSuchPaddingException {
			initEncryptor();
			return enc.doFinal(indata, inoff, inlen);
		}

		/**
		 * encrypt indata to outdata use the key.
		 * 
		 * @param indata
		 *            input data.
		 * @return the actual cipher data.
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchPaddingException
		 * @throws InvalidKeyException
		 * @throws IllegalBlockSizeException
		 * @throws BadPaddingException
		 */
		public byte[] encrypt(byte[] indata) throws NoSuchAlgorithmException,
				NoSuchPaddingException, InvalidKeyException,
				IllegalBlockSizeException, BadPaddingException {
			initEncryptor();
			return enc.doFinal(indata);
		}

		/**
		 * get the maxim plain data length after decryption. the actual plain data
		 * length may be shorter than this value.
		 * 
		 * @param len
		 *            the cipher data length.
		 * @return the maxim plain data length.
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchPaddingException
		 * @throws InvalidKeyException
		 */
		public int getPlainLen(int len) throws NoSuchAlgorithmException,
				NoSuchPaddingException, InvalidKeyException {
			initDecryptor();
			return dec.getOutputSize(len);
		}

		/**
		 * decrypt input data to output data.
		 * 
		 * @param indata
		 *            input data.
		 * @param inoff
		 *            input data offset.
		 * @param inlen
		 *            input data length.
		 * @param outdata
		 *            output data.
		 * @param outoff
		 *            output data offset.
		 * @return the actual plain length.
		 * @throws InvalidKeyException
		 * @throws ShortBufferException
		 * @throws IllegalBlockSizeException
		 * @throws BadPaddingException
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchPaddingException
		 */
		public int decrypt(byte[] indata, int inoff, int inlen, byte[] outdata,
				int outoff) throws InvalidKeyException, ShortBufferException,
				IllegalBlockSizeException, BadPaddingException,
				NoSuchAlgorithmException, NoSuchPaddingException {
			initDecryptor();
			return dec.doFinal(indata, inoff, inlen, outdata, outoff);
		}

		/**
		 * decrypt input data to output data.
		 * 
		 * @param indata
		 *            input data.
		 * @param inoff
		 *            input data offset.
		 * @param inlen
		 *            input data length.
		 * @return the actual plain data.
		 * @throws InvalidKeyException
		 * @throws ShortBufferException
		 * @throws IllegalBlockSizeException
		 * @throws BadPaddingException
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchPaddingException
		 */
		public byte[] decrypt(byte[] indata, int inoff, int inlen)
				throws InvalidKeyException, ShortBufferException,
				IllegalBlockSizeException, BadPaddingException,
				NoSuchAlgorithmException, NoSuchPaddingException {
			initDecryptor();
			return dec.doFinal(indata, inoff, inlen);
		}

		/**
		 * decrypt input data to output data.
		 * 
		 * @param indata
		 *            input data.
		 * @return the actual plain data.
		 * @throws NoSuchAlgorithmException
		 * @throws NoSuchPaddingException
		 * @throws InvalidKeyException
		 * @throws IllegalBlockSizeException
		 * @throws BadPaddingException
		 */
		public byte[] decrypt(byte[] indata) throws NoSuchAlgorithmException,
				NoSuchPaddingException, InvalidKeyException,
				IllegalBlockSizeException, BadPaddingException {
			initDecryptor();
			return dec.doFinal(indata);
		}

		private void initEncryptor() throws NoSuchAlgorithmException,
				NoSuchPaddingException, InvalidKeyException {
			if (null == enc) {
				enc = Cipher.getInstance("RSA/None/PKCS1Padding");
				enc.init(Cipher.ENCRYPT_MODE, key);
			}
		}

		private void initDecryptor() throws NoSuchAlgorithmException,
				NoSuchPaddingException, InvalidKeyException {
			if (null == dec) {
				dec = Cipher.getInstance("RSA/None/PKCS1Padding");

				dec.init(Cipher.DECRYPT_MODE, key);
			}
		}
	}
}
