package com.fsolsh.es.service;

import com.fsolsh.es.common.ESPage;
import com.fsolsh.es.common.ESQuery;
import com.fsolsh.es.common.ESSort;
import com.fsolsh.es.entity.BaseEntity;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.script.Script;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ESRestService {


    /**
     * createIndex
     *
     * @param clazz
     * @return
     * @throws IOException
     */
    boolean createIndex(Class<? extends BaseEntity> clazz) throws IOException;

    /**
     * createIndex
     *
     * @param indexName
     * @param clazz
     * @return
     * @throws IOException
     */
    boolean createIndex(String indexName, Class<? extends BaseEntity> clazz) throws IOException;

    /**
     * createIndex
     *
     * @param indexName
     * @param numberOfShards
     * @param numberOfReplicas
     * @return
     * @throws IOException
     */
    boolean createIndex(String indexName, Class<? extends BaseEntity> clazz, int numberOfShards, int numberOfReplicas) throws IOException;

    /**
     * isExistsIndex
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    boolean isExistsIndex(String indexName) throws IOException;

    /**
     * getIndexSetting
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    Map<String, String> getIndexSetting(String indexName) throws IOException;

    /**
     * deleteIndex
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    boolean deleteIndex(String indexName) throws IOException;

    /**
     * createDoc
     *
     * @param object
     * @return
     * @throws IOException
     */
    <T extends BaseEntity> boolean createDoc(T object) throws IOException, NoSuchFieldException, IllegalAccessException;

    /**
     * createDoc
     *
     * @param indexName
     * @param object
     * @return
     * @throws IOException
     */
    <T extends BaseEntity> boolean createDoc(String indexName, T object) throws IOException, NoSuchFieldException, IllegalAccessException;

    /**
     * createDoc
     *
     * @param indexName
     * @param dataMap
     * @return
     * @throws IOException
     */
    boolean createDoc(String indexName, Map<String, Object> dataMap) throws IOException, NoSuchFieldException, IllegalAccessException;

    /**
     * createBatchDoc
     *
     * @param indexName
     * @param dataList
     * @return
     * @throws IOException
     */
    <T extends BaseEntity> BulkResponse createBatchDoc(String indexName, List<T> dataList) throws IOException, NoSuchFieldException, IllegalAccessException;


    /**
     * getDocAsString
     *
     * @param indexName
     * @param id
     * @return
     * @throws IOException
     */
    String getDocAsString(String indexName, String id) throws IOException;

    /**
     * getDocAsMap
     *
     * @param indexName
     * @param id
     * @return
     * @throws IOException
     */
    Map<String, Object> getDocAsMap(String indexName, String id) throws IOException;

    /**
     * deleteDoc
     *
     * @param indexName
     * @param id
     * @return
     * @throws IOException
     */
    boolean deleteDoc(String indexName, String id) throws IOException;

    /**
     * updateDoc
     *
     * @param indexName
     * @param id
     * @param dataMap
     * @return
     * @throws IOException
     */
    boolean updateDoc(String indexName, String id, Map<String, Object> dataMap) throws IOException;

    /**
     * updateDoc
     *
     * @param indexName
     * @param t
     * @param <T>
     * @return
     * @throws IOException
     */
    <T extends BaseEntity> boolean updateDoc(String indexName, String id, T t) throws IOException;

    /**
     * updateDocsByQuery
     *
     * @param indexName
     * @param queryList
     * @return
     * @throws IOException
     */
    BulkByScrollResponse updateDocsByQuery(String indexName, List<ESQuery> queryList, Script script) throws IOException;

    /**
     * updateDocsByQuery
     *
     * @param indexName
     * @param esQuery
     * @return
     * @throws IOException
     */
    BulkByScrollResponse updateDocsByQuery(String indexName, ESQuery esQuery, Script script) throws IOException;

    /**
     * searchAllAsMap
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    List<Map<String, Object>> searchAllAsMap(String indexName) throws IOException;

    /**
     * searchAllAsMap
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    List<Map<String, Object>> searchAllAsMap(String indexName, int count) throws IOException;

    /**
     * searchAllAsString
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    List<String> searchAllAsString(String indexName) throws IOException;

    /**
     * searchAllAsString
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    List<String> searchAllAsString(String indexName, int count) throws IOException;


    /**
     * searchPage
     *
     * @param indexName
     * @param queryList
     * @param sortList
     * @param ESPage
     * @return
     * @throws IOException
     */
    SearchResponse searchPage(String indexName, List<ESQuery> queryList, List<ESSort> sortList, ESPage<?> ESPage) throws IOException;

    /**
     * searchPage
     *
     * @param indexName
     * @param queryList
     * @param sortList
     * @param includeFields
     * @param excludeFields
     * @param ESPage
     * @return
     * @throws IOException
     */
    SearchResponse searchPage(String indexName, List<ESQuery> queryList, List<ESSort> sortList, String[] includeFields, String[] excludeFields, ESPage<?> ESPage) throws IOException;

    /**
     * searchPageAsString
     *
     * @param indexName
     * @param queryList
     * @param sortList
     * @return
     * @throws IOException
     */
    ESPage<String> searchPageAsString(String indexName, List<ESQuery> queryList, List<ESSort> sortList, ESPage<String> ESPage) throws IOException;

    /**
     * searchPageAsString
     *
     * @param indexName
     * @param queryList
     * @param sortList
     * @param includeFields
     * @param excludeFields
     * @param ESPage
     * @return
     * @throws IOException
     */
    ESPage<String> searchPageAsString(String indexName, List<ESQuery> queryList, List<ESSort> sortList, String[] includeFields, String[] excludeFields, ESPage<String> ESPage) throws IOException;

    /**
     * searchPageAsMap
     *
     * @param indexName
     * @param queryList
     * @param sortList
     * @param ESPage
     * @return
     * @throws IOException
     */
    ESPage<Map<String, Object>> searchPageAsMap(String indexName, List<ESQuery> queryList, List<ESSort> sortList, ESPage<Map<String, Object>> ESPage) throws IOException;

    /**
     * searchPageAsMap
     *
     * @param indexName
     * @param queryList
     * @param sortList
     * @param includeFields
     * @param excludeFields
     * @param ESPage
     * @return
     * @throws IOException
     */
    ESPage<Map<String, Object>> searchPageAsMap(String indexName, List<ESQuery> queryList, List<ESSort> sortList, String[] includeFields, String[] excludeFields, ESPage<Map<String, Object>> ESPage) throws IOException;
}
