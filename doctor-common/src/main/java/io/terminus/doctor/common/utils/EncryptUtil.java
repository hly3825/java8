/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package io.terminus.doctor.common.utils;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * 加密工具类
 * <p/>
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-28
 */
public class EncryptUtil {

    private static final HashFunction SHA512 = Hashing.sha512();

    private static final Splitter SPLITTER = Splitter.on('@').trimResults();

    private static final Joiner JOINER = Joiner.on('@').skipNulls();

    private static final HashFunction MD5 = Hashing.md5();

    /**
     * 对密码进行加密
     *
     * @param password 明文密码
     * @return 加密后的密码
     */
    public static String encrypt(String password) {
        String salt = MD5.newHasher().putString(UUID.randomUUID().toString(), Charsets.UTF_8).putLong(System.currentTimeMillis()).hash()
                .toString().substring(0, 4);
        String realPassword = SHA512.hashString(password + salt, Charsets.UTF_8).toString().substring(0, 20);
        return JOINER.join(salt, realPassword);
    }

    /**
     * 密码匹配
     *
     * @param password          明文密码
     * @param encryptedPassword 加密后的密码
     * @return 匹配成功返回true, 反之false
     */
    public static boolean match(String password, String encryptedPassword) {
        Iterable<String> parts = SPLITTER.split(encryptedPassword);
        String salt = Iterables.get(parts, 0);
        String realPassword = Iterables.get(parts, 1);
        return Objects.equal(SHA512.hashString(password + salt, Charsets.UTF_8).toString().substring(0, 20), realPassword);
    }

    /**
     * md5加密
     * @param sourceStr 待加密字符串
     * @return 加密后的32位字符串
     */
    public static String MD5(String sourceStr){
        byte[] b = new byte[0];
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(sourceStr.getBytes());
            b = md.digest();
        } catch (NoSuchAlgorithmException e) {
            //不会出现异常的
        }
        StringBuilder buf = new StringBuilder("");
        for (byte aB : b) {
            int i = aB;
            if (i < 0) {
                i += 256;
            }
            if (i < 16) {
                buf.append("0");
            }
            buf.append(Integer.toHexString(i));
        }
        return buf.toString(); //32bits
    }

    /**
     * md5加密
     * @param sourceStr 待加密字符串
     * @return 加密后的16位字符串
     */
    public static String MD5Short(String sourceStr) {
        return MD5(sourceStr).substring(8, 24);
    }
}
