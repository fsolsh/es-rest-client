package com.fsolsh.es.service.impl;

import com.fsolsh.es.service.ENWordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 查找某个英文单词的相关词汇
 */
@Slf4j
@Component
public class ENWordServiceImpl implements ENWordService {

    private static final int WORDS_TOTAL_COUNT = 80000, WORDS_COUNT_PER_LIST = 2000;
    private static final int DEFAULT_MATCH_SIZE = 20, MIN_MATCH_SIZE = 5, MAX_MATCH_SIZE = 50;

    //用来放所有的单词和单词的位置索引
    private static final HashMap<String, Integer> ALL_WORDS_MAP = new HashMap<>(WORDS_TOTAL_COUNT);
    private static final List<List<String>> ALL_WORDS_LIST = new ArrayList<>(WORDS_TOTAL_COUNT / WORDS_COUNT_PER_LIST);

    @Value("${es.words.file.path}")
    private String wordsFilePath;

    /**
     * Bean构建完毕加载构建词典
     *
     * @throws IOException
     */
    @PostConstruct
    private void buildDDictionary() throws IOException {

        InputStreamReader in = new InputStreamReader(Objects.requireNonNull(ENWordServiceImpl.class.getClassLoader().getResourceAsStream(wordsFilePath)), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(in);
        List<String> wordList = null;
        int wordIndex = 0;
        String word = "";
        while (StringUtils.hasText(word = br.readLine())) {
            if (wordIndex % WORDS_COUNT_PER_LIST == 0) {
                if (wordList != null) {
                    ALL_WORDS_LIST.add(wordList);
                }
                wordList = new ArrayList<>(WORDS_COUNT_PER_LIST);
            }
            wordList.add(word);
            ALL_WORDS_MAP.put(word, wordIndex);
            wordIndex++;
        }

        br.close();
        in.close();

        if (wordList != null) {
            log.info("lastWordList size {}", wordList.size());
            ALL_WORDS_LIST.add(wordList);
        }
        log.info("mainWordList size {}", ALL_WORDS_LIST.size());
    }

    /**
     * getRelatedWordsList
     *
     * @param sourceWord
     * @param count
     * @return
     */
    private List<String> getWordsList(String sourceWord, int count) {
        int indexOfMainList = getIndexOfWord(sourceWord);
        if (indexOfMainList == -1) {
            return null;
        }
        int mainListIndex = indexOfMainList / WORDS_COUNT_PER_LIST;
        int subListIndex = indexOfMainList % WORDS_COUNT_PER_LIST;

        //如果在一个list中可以拿到上下足量的数据
        if (subListIndex - count >= 0 && subListIndex + count <= ALL_WORDS_LIST.get(mainListIndex).size()) {
            return ALL_WORDS_LIST.get(mainListIndex).subList(subListIndex - count, subListIndex + count);
        }

        //如果是first list，一定是前置不足
        if (mainListIndex == 0 && subListIndex - count < 0) {
            return ALL_WORDS_LIST.get(mainListIndex).subList(0, subListIndex + count);
        }
        //如果是last list，并且是后置不足
        if (mainListIndex == ALL_WORDS_LIST.size() - 1 && subListIndex + count > ALL_WORDS_LIST.get(mainListIndex).size()) {
            return ALL_WORDS_LIST.get(mainListIndex).subList(subListIndex - count, ALL_WORDS_LIST.get(mainListIndex).size());
        }

        //需要跨越两个list拿到数据
        List<String> resultList = new ArrayList<>(4 * count);
        if (subListIndex - count >= 0) {
            resultList.addAll(ALL_WORDS_LIST.get(mainListIndex).subList(subListIndex - count, subListIndex));
            resultList.addAll(ALL_WORDS_LIST.get(mainListIndex).subList(subListIndex, WORDS_COUNT_PER_LIST));
            resultList.addAll(ALL_WORDS_LIST.get(mainListIndex + 1).subList(0, count - (WORDS_COUNT_PER_LIST - subListIndex)));
        } else {
            resultList.addAll(ALL_WORDS_LIST.get(mainListIndex - 1).subList(WORDS_COUNT_PER_LIST - (count - subListIndex), WORDS_COUNT_PER_LIST));
            resultList.addAll(ALL_WORDS_LIST.get(mainListIndex).subList(0, subListIndex));
            resultList.addAll(ALL_WORDS_LIST.get(mainListIndex).subList(subListIndex, subListIndex + count));
        }
        return resultList;
    }

    /**
     * getRelatedWords
     *
     * @param sourceWord
     * @return
     */
    public String getRelatedWords(String sourceWord) {
        return getRelatedWords(sourceWord, DEFAULT_MATCH_SIZE);
    }

    /**
     * getRelatedWords
     *
     * @param sourceWord
     * @param matchSize
     * @return
     */
    public String getRelatedWords(String sourceWord, Integer matchSize) {
        if (matchSize == null || matchSize < MIN_MATCH_SIZE || matchSize > MAX_MATCH_SIZE) {
            matchSize = DEFAULT_MATCH_SIZE;
        }
        List<String> resultList = this.getWordsList(sourceWord, matchSize);
        if (resultList == null || resultList.isEmpty()) {
            return sourceWord;
        }
        Set<String> wordsSet = new HashSet<>();
        StringBuilder sb = new StringBuilder(sourceWord);
        int i = sourceWord.length();
        while (i > 2 && wordsSet.size() < 5) {
            i--;
            for (String word : resultList) {
                if (word.length() >= i && sourceWord.startsWith(word.substring(0, i))) {
                    wordsSet.add(word);
                }
            }
        }
        for (String word : wordsSet) {
            sb.append(" ").append(word);
        }
        return sb.toString();
    }

    /**
     * getIndexOfWord
     *
     * @param sourceWord
     * @return
     */
    private int getIndexOfWord(String sourceWord) {
        for (int i = sourceWord.length(); i > 0; i--) {
            String word = sourceWord.substring(0, i);
            if (ALL_WORDS_MAP.containsKey(word)) {
                return ALL_WORDS_MAP.get(word);
            }
        }
        return -1;
    }
}
