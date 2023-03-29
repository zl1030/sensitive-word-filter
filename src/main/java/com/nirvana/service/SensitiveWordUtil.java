package com.nirvana.service;

import com.google.common.base.Ascii;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author by lei zhou on 2017/11/07 14:42.
 */
public class SensitiveWordUtil {

    /**
     * 敏感词匹配规则
     */
    public static final int MinMatchTYpe = 1;      //最小匹配规则，如：敏感词库["中国","中国人"]，语句："我是中国人"，匹配结果：我是[中国]人
    public static final int MaxMatchType = 2;      //最大匹配规则，如：敏感词库["中国","中国人"]，语句："我是中国人"，匹配结果：我是[中国人]

    /**
     * 敏感词集合
     */
    public static HashMap sensitiveWordMap;

    public static Collection<Pattern> wordPatterns;

    private static final boolean PATTERN_ACTIVE = false;

    private static final char REPLACE_MARK_CHAR = '*';

    private static final Pattern SYMBOL_PATTERN = Pattern
        .compile("[`~!@#$%^&*()+=|{}':;',\\\\\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]");


    static {
        sensitiveWordMap = initSensitiveWordMap(Collections.EMPTY_SET);
        wordPatterns = Collections.EMPTY_SET;
    }

    /**
     * 初始化敏感词库，构建DFA算法模型
     *
     * @param sensitiveWordSet 敏感词库
     */
    public static synchronized void init(Collection<SensitiveWord> sensitiveWordSet) {
        sensitiveWordMap = null;
        sensitiveWordMap = initSensitiveWordMap(sensitiveWordSet);
//        if (PATTERN_ACTIVE) {
//            long now = System.currentTimeMillis();
//            wordPatterns = compiledPatterns(sensitiveWordSet);
//            Log.info("pattern compile wordCount:{} time:{}", sensitiveWordSet.size(), System.currentTimeMillis() - now);
//        }
    }

//    private static Collection<Pattern> compiledPatterns(Collection<SensitiveWord> sensitiveWordSet) {
//        List<Pattern> patterns = Lists.newArrayListWithCapacity(sensitiveWordSet.size());
//        for (SensitiveWord word : sensitiveWordSet) {
//            Pattern pattern = getPattern(word.getWord(), word.getMatchingRule());
//            if (Objects.nonNull(pattern)) {
//                patterns.add(pattern);
//            }
//        }
//        // 热身
////        int times = 1000;
////        while (times-- > 0) {
////            replaceSensitiveWord(UUIDGenerator.randomUUID().toString());
////        }
//        return patterns;
//    }

    private static String symbolClean(String str) {
        Matcher m = SYMBOL_PATTERN.matcher(str);
        return m.replaceAll("").replaceAll("\\s*", "").trim();
    }

//    private static Pattern getPattern(String word, int matchingRule) {
//
//        word = symbolClean(word);
//
//        final String MATCHER = ".*";
//        final String NORMAL_REGX = "((?i){})";
//        final String CHINESE_REGX = "({}|(?i){})";
//
//        StringBuilder sBuffer = new StringBuilder();
//        char[] chars = word.toCharArray();
//        if (chars.length == 0) {
//            return null;
//        }
//        boolean firstChar = true;
//        boolean prevCharIsChinese = false;
//        for (char c : chars) {
//            if ((prevCharIsChinese && matchingRule == SensitiveWord.MATCHING_RULE_PATTERN) || firstChar) {
//                sBuffer.append(MATCHER);
//                firstChar = false;
//            }
//            if (isChinese(c) && chars.length > 1) {
//                prevCharIsChinese = true;
//                char chinese = c;
//                try {
//                    String pinyin = transToPinYin(String.valueOf(chinese));
//                    sBuffer.append(StringUtil.getLogString(CHINESE_REGX, chinese, pinyin));
//                } catch (Exception e) {
//                    // 繁体会失败
//                    sBuffer.append(StringUtil.getLogString(NORMAL_REGX, c));
//                }
//            } else {
//                prevCharIsChinese = false;
//                sBuffer.append(StringUtil.getLogString(NORMAL_REGX, c));
//            }
//        }
//        sBuffer.append(MATCHER);
//        String regx = sBuffer.toString();
////        Log.debug("rawWord:{} regx:{}", word, regx);
//        return Pattern.compile(regx, Pattern.DOTALL | Pattern.MULTILINE);
//    }

//    private boolean patternContains(String input) {
//        if (!PATTERN_ACTIVE) {
//            return false;
//        }
//        Collection<Pattern> patterns = wordPatterns;
//        Optional<Pattern> patternOptional = patterns
//            .stream()
//            .filter(pattern -> pattern.matcher(input).matches())
//            .findFirst();
//        if (patternOptional.isPresent()) {
////            String p = patternOptional.get().pattern();
////            Matcher matcher = patternOptional.get().matcher(input);
////            matcher.matches();
////            for (int i = 0; i < matcher.groupCount(); i++) {
////                Log.debug(matcher.group(i + 1));
////            }
//            return true;
//        }
//        return false;
//    }

    private Collection<Pattern> matchedPatterns(String input) {
        Collection<Pattern> patterns = wordPatterns;
        Collection<Pattern> matchedPattern = patterns
            .stream()
            .filter(pattern -> pattern.matcher(input).matches())
            .collect(Collectors.toSet());
        return matchedPattern;
    }

//    public static void main(String[] args) {
//
//        String word = "裸    聊";
//
//        Pattern pattern = getPattern(word);
//
//        String input = "阿利伯克裸*    ldi!LIao!!";
//
//        Matcher matcher = pattern.matcher(input);
//        if (matcher.matches()) {
//            System.out.println("有敏感字！");
//            String tmp = input;
//            for (int i = 0; i < matcher.groupCount(); i++) {
//                tmp = tmp.replaceAll(matcher.group(i + 1), "*");
//            }
//            System.out.println(tmp);
//        }
//
//    }


    /**
     * 初始化敏感词库，构建DFA算法模型
     *
     * @param sensitiveWordSet 敏感词库
     */
    private static HashMap initSensitiveWordMap(Collection<SensitiveWord> sensitiveWordSet) {
        //初始化敏感词容器，减少扩容操作
        HashMap tempSensitiveWordMap = new HashMap(sensitiveWordSet.size());
        String key;
        Map nowMap;
        Map<String, String> newWorMap;
        //迭代sensitiveWordSet
        Iterator<SensitiveWord> iterator = sensitiveWordSet.iterator();
        while (iterator.hasNext()) {
            //关键字
            key = iterator.next().getWord();
            nowMap = tempSensitiveWordMap;
            for (int i = 0; i < key.length(); i++) {
                //转换成char型
                char keyChar = key.charAt(i);
                //库中获取关键字
                Object wordMap = nowMap.get(keyChar);
                //如果存在该key，直接赋值，用于下一个循环获取
                if (wordMap != null) {
                    nowMap = (Map) wordMap;
                } else {
                    //不存在则，则构建一个map，同时将isEnd设置为0，因为他不是最后一个
                    newWorMap = new HashMap<>();
                    //不是最后一个
                    newWorMap.put("isEnd", "0");
                    nowMap.put(keyChar, newWorMap);
                    nowMap = newWorMap;
                }

                if (i == key.length() - 1) {
                    //最后一个
                    nowMap.put("isEnd", "1");
                }
            }
        }
        return tempSensitiveWordMap;
    }

    /**
     * 判断文字是否包含敏感字符
     *
     * @param txt       文字
     * @param matchType 匹配规则 1：最小匹配规则，2：最大匹配规则
     * @return 若包含返回true，否则返回false
     */
    private static boolean contains(String txt, int matchType) {
        for (Set<Long> chars : CHARS_CHECK_SEQ) {
            for (int i = 0; i < txt.length(); i++) {
                if (checkSensitiveWord(txt, i, matchType, chars) > 0) {
                    return true;
                }
            }
        }
        return false;
//        return patternContains(txt);
    }

    /**
     * 判断文字是否包含敏感字符
     *
     * @param txt 文字
     * @return 若包含返回true，否则返回false
     */
    public static synchronized boolean contains(String txt) {
        return contains(txt, MaxMatchType);
    }

    /**
     * 获取文字中的敏感词
     *
     * @param txt       文字
     * @param matchType 匹配规则 1：最小匹配规则，2：最大匹配规则
     */
    private static Set<String> getSensitiveWord(String txt, int matchType, Set<Long> ignoreChars) {
        Set<String> sensitiveWordList = new HashSet<>();

        for (int i = 0; i < txt.length(); i++) {
            //判断是否包含敏感字符
            int length = checkSensitiveWord(txt, i, matchType, ignoreChars);
            if (length > 0) {//存在,加入list中
                sensitiveWordList.add(txt.substring(i, i + length));
                i = i + length - 1;//减1的原因，是因为for会自增
            }
        }

        return sensitiveWordList;
    }

    /**
     * 获取文字中的敏感词
     *
     * @param txt 文字
     */
//    public static Set<String> getSensitiveWord(String txt) {
//        return getSensitiveWord(txt, MaxMatchType);
//    }

    /**
     * 替换敏感字字符
     *
     * @param txt         文本
     * @param replaceChar 替换的字符，匹配的敏感词以字符逐个替换，如 语句：我爱中国人 敏感词：中国人，替换字符：*， 替换结果：我爱***
     * @param matchType   敏感词匹配规则
     */
    private static String replaceSensitiveWord(String txt, char replaceChar, int matchType) {
        String resultTxt = txt;

        if (PATTERN_ACTIVE) {
            final String replaceCharString = String.valueOf(replaceChar);
            Collection<Pattern> patterns = wordPatterns;
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(resultTxt);
                if (matcher.matches()) {
                    for (int i = 0; i < matcher.groupCount(); i++) {
                        resultTxt = resultTxt.replaceAll(matcher.group(i + 1), replaceCharString);
                    }
                }
            }
        } else { //获取所有的敏感词
            Set<String> set = Sets.newLinkedHashSet();
            for (Set<Long> chars : CHARS_CHECK_SEQ) {
                set.addAll(getSensitiveWord(txt, matchType, chars));
            }
            Iterator<String> iterator = set.iterator();
            String word;
            String replaceString;
            while (iterator.hasNext()) {
                word = iterator.next();
                replaceString = getReplaceChars(replaceChar, word.length());
                resultTxt = resultTxt.replace(word, replaceString);
            }
        }

        return resultTxt;
    }

    /**
     * 替换敏感字字符
     *
     * @param txt 文本
     */
    public static synchronized String replaceSensitiveWord(String txt) {
        String tmp = replaceSensitiveWord(txt, REPLACE_MARK_CHAR, MaxMatchType);
        return tmp;
    }

//    /**
//     * 替换敏感字字符
//     *
//     * @param txt 文本
//     * @param replaceStr 替换的字符串，匹配的敏感词以字符逐个替换，如 语句：我爱中国人 敏感词：中国人，替换字符串：[屏蔽]，替换结果：我爱[屏蔽]
//     * @param matchType 敏感词匹配规则
//     */
//    private static String replaceSensitiveWord(String txt, String replaceStr, int matchType) {
//        String resultTxt = txt;
//        //获取所有的敏感词
//        Set<String> set = getSensitiveWord(txt, matchType);
//        Iterator<String> iterator = set.iterator();
//        String word;
//        while (iterator.hasNext()) {
//            word = iterator.next();
//            resultTxt = resultTxt.replaceAll(word, replaceStr);
//        }
//
//        return resultTxt;
//    }

    /**
     * 替换敏感字字符
     *
     * @param txt 文本
     * @param replaceStr 替换的字符串，匹配的敏感词以字符逐个替换，如 语句：我爱中国人 敏感词：中国人，替换字符串：[屏蔽]，替换结果：我爱[屏蔽]
     */
//    public static String replaceSensitiveWord(String txt, char replaceStr) {
//        return replaceSensitiveWord(txt, replaceStr, MaxMatchType);
//    }

    /**
     * 获取替换字符串
     */
    private static String getReplaceChars(char replaceChar, int length) {
        String resultReplace = String.valueOf(replaceChar);
        for (int i = 1; i < length; i++) {
            resultReplace += replaceChar;
        }

        return resultReplace;
    }

    private final static Set<Long> ALL_IGNORE_CHARS = Sets.newHashSet();
    private final static Set<Long> ONLY_CHINESE = Sets.newHashSet();
    private final static Set<Long> IGNORE_NUMBER_CHARS = Sets.newHashSet();
    private final static Set<Long> IGNORE_LETTER_CHARS = Sets.newHashSet();
    private final static Set<Long> IGNORE_SYMBOL_CHARS = Sets.newHashSet();

    private final static List<Set<Long>> CHARS_CHECK_SEQ = Lists.newLinkedList();

    static {

        // 需要跳过检查的标点字符

        // 数字0-9
        for (long i = '0'; i <= '9'; i++) {
            IGNORE_NUMBER_CHARS.add(i);
        }

        // a-z
        for (long i = 'a'; i <= 'z'; i++) {
            IGNORE_LETTER_CHARS.add(i);
        }
        // A-Z
        for (long i = 'A'; i <= 'Z'; i++) {
            IGNORE_LETTER_CHARS.add(i);
        }

        // 半角/全角标点符号
        char[] symbols = {
            '~', '~',
            '`', '·',
            '!', '！',
            '@', '@',
            '#', '#',
            '$', '￥',
            '%', '%',
            '^', '…',
            '&', '&',
            '*', '*',
            '(', '（',
            ')', '）',
            '-', '—',
            '_', '-',
            '+', '+',
            '=', '=',
            '|', '|',
            '\\', '、',
            '[', '【',
            ']', '】',
            '{', '{',
            '}', '}',
            ';', '；',
            ':', '：',
            '"', '“', '”',
            '\'', '‘', '’',
            ',', '，',
            '<', '《',
            '.', '。',
            '>', '》',
            '/', '/',
            '?', '？'
        };
        for (char symbol : symbols) {
            IGNORE_SYMBOL_CHARS.add((long) symbol);
        }
        IGNORE_SYMBOL_CHARS.add((long) Ascii.SPACE);
        IGNORE_SYMBOL_CHARS.add((long) Ascii.HT);

        ALL_IGNORE_CHARS.addAll(IGNORE_SYMBOL_CHARS);
        ALL_IGNORE_CHARS.addAll(IGNORE_NUMBER_CHARS);
        ALL_IGNORE_CHARS.addAll(IGNORE_LETTER_CHARS);

        // 忽略字符集检查优先级
        CHARS_CHECK_SEQ.add(ONLY_CHINESE);
        CHARS_CHECK_SEQ.add(ALL_IGNORE_CHARS);
        CHARS_CHECK_SEQ.add(IGNORE_SYMBOL_CHARS);
        CHARS_CHECK_SEQ.add(IGNORE_NUMBER_CHARS);
        CHARS_CHECK_SEQ.add(IGNORE_LETTER_CHARS);
        // 没有忽略，完整过滤
        CHARS_CHECK_SEQ.add(Collections.EMPTY_SET);

    }


    private static boolean isChinese(long wordCode) {
        return wordCode >= 0x4E00 && wordCode <= 0x9FBF;
    }

    /**
     * 检查文字中是否包含敏感字符，检查规则如下：<br>
     *
     * @return 如果存在，则返回敏感词字符的长度，不存在返回0
     */
    private static int checkSensitiveWord(String txt, int beginIndex, int matchType, Set<Long> ignoreChars) {
        //敏感词结束标识位：用于敏感词只有1位的情况
        boolean flag = false;
        //匹配标识数默认为0
        int matchFlag = 0;
        char word;
        int spaceNum = 0;

        String tmp = txt.toLowerCase();

        Map nowMap = sensitiveWordMap;
        for (int i = beginIndex; i < tmp.length(); i++) {
            word = tmp.charAt(i);

            long wordCode = (long) word;

            if (ignoreChars == ONLY_CHINESE && !isChinese(wordCode) ||
                ignoreChars.contains(wordCode)) {
                spaceNum++;
                continue;
            }

            //获取指定key
            nowMap = (Map) nowMap.get(word);
            if (nowMap != null) {//存在，则判断是否为最后一个
                //找到相应key，匹配标识+1
                matchFlag++;
                //如果为最后一个匹配规则,结束循环，返回匹配标识数
                if ("1".equals(nowMap.get("isEnd"))) {
                    return matchFlag + spaceNum;

                    //结束标志位为true
//                    flag = true;
//                    //最小规则，直接返回,最大规则还需继续查找
//                    if (MinMatchTYpe == matchType) {
//                        break;
//                    }
                }
            } else {//不存在，直接返回
                break;
            }
        }

        if (matchFlag < 2 || !flag) {//长度必须大于等于1，为词
            matchFlag = 0;
            spaceNum = 0;
        }
        return matchFlag + spaceNum;
    }

//    private static String transToPinYin(String src) {
//        char[] t1 = null;
//        t1 = src.toCharArray();
//        String[] t2 = new String[t1.length];
//        HanyuPinyinOutputFormat t3 = new HanyuPinyinOutputFormat();
//
//        t3.setCaseType(HanyuPinyinCaseType.LOWERCASE);
//        t3.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
//        t3.setVCharType(HanyuPinyinVCharType.WITH_V);
//        String t4 = "";
//        int t0 = t1.length;
//        try {
//            for (int i = 0; i < t0; i++) {
//                // 判断是否为汉字字符
//                if (Character.toString(t1[i]).matches(
//                    "[\\u4E00-\\u9FA5]+")) {
//                    t2 = PinyinHelper.toHanyuPinyinStringArray(t1[i], t3);
//                    t4 += t2[0];
//                } else {
//                    t4 += Character.toString(t1[i]);
//                }
//            }
//            // System.out.println(t4);
//            return t4;
//        } catch (BadHanyuPinyinOutputFormatCombination e1) {
//            e1.printStackTrace();
//        }
//        return t4;
//    }

    @Getter
    @AllArgsConstructor
    public static final class SensitiveWord {

        public static final byte MATCHING_RULE_PATTERN = 0;
        public static final byte MATCHING_RULE_BLOCK = 1;

        private byte matchingRule;
        private String word;

    }
}
