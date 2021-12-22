package com.fsolsh.es.service;

public interface ENWordService {

    /**
     * getRelatedWords
     *
     * @param sourceWord
     * @return
     */
    String getRelatedWords(String sourceWord);

    /**
     * getRelatedWords
     *
     * @param sourceWord
     * @param matchSize
     * @return
     */
    String getRelatedWords(String sourceWord, Integer matchSize);
}
