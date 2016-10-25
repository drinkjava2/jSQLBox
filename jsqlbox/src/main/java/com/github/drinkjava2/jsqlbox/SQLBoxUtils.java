package com.github.drinkjava2.jsqlbox;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLBoxUtils {
	private static final Logger log = Logger.getLogger(SQLBoxUtils.class.toString());
	private static ConcurrentHashMap<String, Integer> classExistCache = new ConcurrentHashMap<>();

	private SQLBoxUtils() {
	}

	/**
	 * Same as StringUtils.isEmptyStr()
	 */
	public static boolean isEmptyStr(String str) {
		return str == null || "".equals(str);
	}

	/**
	 * Transfer all Exceptions to RuntimeException SQLBoxException. The only
	 * place throw Exception in this project
	 */
	public static void throwEX(Exception e, String errorMsg) {
		log.log(Level.SEVERE, errorMsg, e);
		throw new SQLBoxException(errorMsg);
	}

	/**
	 * Log exception
	 */
	public static void logException(Exception e) {
		log.log(Level.INFO, "", e);
	}

	/**
	 * Eat exception
	 */
	public static void eatException(Exception e) {
		int i = 0;
		if (i == 1)
			log.log(Level.OFF, "Eat Exceptions which not worth to log it", e);
	}

	/**
	 * Print message, usually for debug use
	 */
	public static void debug(String msg) {
		System.out.println(msg); // NOSONAR
	}

	/**
	 * Check class if exist
	 */
	public static Class<?> checkSQLBoxClassExist(String className) {
		Integer i = classExistCache.get(className);
		if (i == null)
			try {
				Class<?> clazz = Class.forName(className);
				if (SQLBox.class.isAssignableFrom((Class<?>) clazz)) {
					classExistCache.put(className, 1);
					return clazz;
				}
				classExistCache.put(className, 0);
				return null;
			} catch (Exception e) {
				SQLBoxUtils.eatException(e);
				classExistCache.put(className, 0);
				return null;
			}
		if (1 == i) {
			try {
				return Class.forName(className);
			} catch (Exception e) {
				logException(e);
			}
		}
		return null;
	}

	/**
	 * If first letter is Capitalized, return true
	 */
	public static boolean isCapitalizedString(String str) {
		char c = str.substring(0, 1).toCharArray()[0];
		return c >= 'A' && c <= 'Z';
	}

	/**
	 * Change first letter to lower case
	 */
	public static String toFirstLetterLowerCase(String s) {
		if (Character.isLowerCase(s.charAt(0)))
			return s;
		else
			return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
	}

}
