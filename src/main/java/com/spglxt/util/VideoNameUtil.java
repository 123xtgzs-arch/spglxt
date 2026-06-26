package com.spglxt.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 视频名称标准化工具类
 * 用于统一视频名称格式，解决"第一季"、"第二季"等不同表达方式的匹配问题
 * 
 * @author spglxt
 */
public class VideoNameUtil {

    private static final Map<String, Integer> CN_NUMBER_MAP = new HashMap<>();
    private static final Map<String, String> ROMAN_NUMBER_MAP = new HashMap<>();
    
    static {
        // 中文数字映射
        CN_NUMBER_MAP.put("零", 0);
        CN_NUMBER_MAP.put("一", 1);
        CN_NUMBER_MAP.put("二", 2);
        CN_NUMBER_MAP.put("三", 3);
        CN_NUMBER_MAP.put("四", 4);
        CN_NUMBER_MAP.put("五", 5);
        CN_NUMBER_MAP.put("六", 6);
        CN_NUMBER_MAP.put("七", 7);
        CN_NUMBER_MAP.put("八", 8);
        CN_NUMBER_MAP.put("九", 9);
        CN_NUMBER_MAP.put("十", 10);
        
        // 罗马数字映射
        ROMAN_NUMBER_MAP.put("Ⅱ", "2");
        ROMAN_NUMBER_MAP.put("Ⅲ", "3");
        ROMAN_NUMBER_MAP.put("Ⅳ", "4");
        ROMAN_NUMBER_MAP.put("Ⅴ", "5");
        ROMAN_NUMBER_MAP.put("Ⅵ", "6");
        ROMAN_NUMBER_MAP.put("Ⅶ", "7");
        ROMAN_NUMBER_MAP.put("Ⅷ", "8");
        ROMAN_NUMBER_MAP.put("Ⅸ", "9");
        ROMAN_NUMBER_MAP.put("Ⅹ", "10");
    }

    /**
     * 中文数字转阿拉伯数字
     */
    private static String cnToNumber(String cnStr) {
        if (cnStr == null || cnStr.isEmpty()) {
            return null;
        }
        
        cnStr = cnStr.trim();
        
        // 如果已经是数字直接返回
        if (cnStr.matches("\\d+")) {
            return cnStr;
        }
        
        // 简单实现:处理一到十的中文数字
        if (CN_NUMBER_MAP.containsKey(cnStr)) {
            return String.valueOf(CN_NUMBER_MAP.get(cnStr));
        }
        
        // 处理"十X"形式(如"十一"、"十二")
        if (cnStr.startsWith("十")) {
            if (cnStr.length() == 1) {
                return "10";
            }
            String rest = cnStr.substring(1);
            if (CN_NUMBER_MAP.containsKey(rest)) {
                return String.valueOf(10 + CN_NUMBER_MAP.get(rest));
            }
        }
        
        // 处理"X十"形式(如"二十")
        if (cnStr.contains("十")) {
            String[] parts = cnStr.split("十");
            if (parts.length == 2) {
                int tens = parts[0].isEmpty() ? 1 : CN_NUMBER_MAP.getOrDefault(parts[0], 0);
                int ones = parts[1].isEmpty() ? 0 : CN_NUMBER_MAP.getOrDefault(parts[1], 0);
                return String.valueOf(tens * 10 + ones);
            } else if (parts.length == 1 && cnStr.endsWith("十")) {
                // 处理"二十"的情况
                int tens = CN_NUMBER_MAP.getOrDefault(parts[0], 0);
                return String.valueOf(tens * 10);
            }
        }
        
        return null;
    }

    /**
     * 标准化视频名称（统一季数格式用于对比）
     * 
     * @param name 原始视频名称
     * @return 标准化后的名称
     */
    public static String normalizeVideoName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        
        // 去掉空格
        name = name.replace(" ", "");
        
        // 去掉常见标点符号
        String[] punctuations = {"，", ",", "。", ".", "！", "!", "？", "?", "：", ":", 
            "；", ";", "、", "·", "—", "－", "-", "～", "~", "｜", "|", 
            "（", "）", "(", ")", "【", "】", "[", "]", "《", "》"};
        for (String punct : punctuations) {
            name = name.replace(punct, "");
        }
        
        // "第X季/部" → 数字(支持在名称任意位置)
        Pattern seasonPattern = Pattern.compile("第(.{1,4})(季|部)");
        Matcher matcher = seasonPattern.matcher(name);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String seasonNum = matcher.group(1);
            String num = cnToNumber(seasonNum);
            if (num != null) {
                matcher.appendReplacement(sb, num);
            }
        }
        matcher.appendTail(sb);
        name = sb.toString();
        
        // 罗马数字转换
        for (Map.Entry<String, String> entry : ROMAN_NUMBER_MAP.entrySet()) {
            name = name.replace(entry.getKey(), entry.getValue());
        }
        
        // 英文罗马数字
        name = name.replaceAll("\\bIII\\b", "3");
        name = name.replaceAll("\\bII\\b", "2");
        name = name.replaceAll("\\bIV\\b", "4");
        name = name.replace("III", "3").replace("II", "2").replace("IV", "4");
        
        // 去掉末尾的"1"(第一季=无后缀,都视为同一部)
        name = name.replaceAll("1$", "");
        
        return name;
    }

    /**
     * 判断两个视频名称是否匹配（考虑季数等标准化）
     * 
     * @param name1 名称1
     * @param name2 名称2
     * @return 是否匹配
     */
    public static boolean isNameMatch(String name1, String name2) {
        if (name1 == null || name2 == null) {
            return false;
        }
        
        // 精确匹配
        if (name1.equals(name2)) {
            return true;
        }
        
        // 去空格匹配
        if (name1.replace(" ", "").equals(name2.replace(" ", ""))) {
            return true;
        }
        
        // 标准化名称匹配
        String normalized1 = normalizeVideoName(name1);
        String normalized2 = normalizeVideoName(name2);
        return normalized1.equals(normalized2);
    }
}
