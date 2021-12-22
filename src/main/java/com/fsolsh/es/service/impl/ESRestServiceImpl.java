package com.fsolsh.es.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fsolsh.es.common.ESPage;
import com.fsolsh.es.common.ESQuery;
import com.fsolsh.es.common.ESSort;
import com.fsolsh.es.entity.BaseEntity;
import com.fsolsh.es.service.ESRestService;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.ScoreSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

import static com.fsolsh.es.common.ESPage.MAX_COUNT_PER_PAGE;
import static com.fsolsh.es.entity.BaseEntity.PK_ID;
import static org.elasticsearch.action.support.WriteRequest.RefreshPolicy.IMMEDIATE;

/**
 * ES服务类
 */
@Component
public class ESRestServiceImpl implements ESRestService {

    public static final String NUMBER_OF_SHARDS = "index.number_of_shards", NUMBER_OF_REPLICAS = "index.number_of_replicas";

    @Autowired
    RestHighLevelClient highLevelClient;

    /**
     * createIndex
     *
     * @param clazz
     * @throws IOException
     */
    public boolean createIndex(Class<? extends BaseEntity> clazz) throws IOException {
        return createIndex(clazz.getSimpleName().toLowerCase(), clazz);
    }

    /**
     * createIndex
     *
     * @param indexName
     * @param clazz
     * @return
     * @throws IOException
     */
    public boolean createIndex(String indexName, Class<? extends BaseEntity> clazz) throws IOException {
        return createIndex(indexName, clazz, 1, 1);
    }

    /**
     * createIndex
     *
     * @param indexName
     * @param numberOfShards
     * @param numberOfReplicas
     * @return
     * @throws IOException
     */
    public boolean createIndex(String indexName, Class<? extends BaseEntity> clazz, int numberOfShards, int numberOfReplicas) throws IOException {
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.settings(Settings.builder().put(NUMBER_OF_SHARDS, numberOfShards).put(NUMBER_OF_REPLICAS, numberOfReplicas));
        // 字段映射
        Map<String, Object> mapping = new HashMap<>();
        mapping.put("properties", getMappingProperties(clazz));
        request.mapping(mapping);
        CreateIndexResponse createIndexResponse = highLevelClient.indices().create(request, RequestOptions.DEFAULT);
        return createIndexResponse.isAcknowledged() && createIndexResponse.isShardsAcknowledged();
    }

    /**
     * getMappingProperties
     *
     * @param clazz
     * @return
     */
    private Map<String, Object> getMappingProperties(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        Map<String, Object> mappingMap = new HashMap<>();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);

            Map<String, Object> typeMap = new HashMap<>();
            if (field.getGenericType().toString().equals("class java.lang.String")) {
                typeMap.put("type", "text");
                mappingMap.put(field.getName(), typeMap);
                continue;
            }

            if (field.getGenericType().toString().equals("int") || field.getGenericType().toString().equals("class java.lang.Integer")) {
                typeMap.put("type", "integer");
                mappingMap.put(field.getName(), typeMap);
                continue;
            }

            if (field.getGenericType().toString().equals("long") || field.getGenericType().toString().equals("class java.lang.Long")) {
                typeMap.put("type", "long");
                mappingMap.put(field.getName(), typeMap);
                continue;
            }

            if (field.getGenericType().toString().equals("double") || field.getGenericType().toString().equals("class java.lang.Double")) {
                typeMap.put("type", "double");
                mappingMap.put(field.getName(), typeMap);
                continue;
            }

            if (field.getGenericType().toString().equals("boolean") || field.getGenericType().toString().equals("class java.lang.Boolean")) {
                typeMap.put("type", "boolean");
                mappingMap.put(field.getName(), typeMap);
                continue;
            }

            if (field.getGenericType().toString().equals("class java.util.Date")) {
                typeMap.put("type", "date");
                mappingMap.put(field.getName(), typeMap);
                continue;
            }

            if (field.getGenericType().toString().equals("class java.lang.Object")) {
                typeMap.put("type", "object");
                mappingMap.put(field.getName(), typeMap);
            }
        }
        return mappingMap;
    }


    /**
     * isExistsIndex
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    public boolean isExistsIndex(String indexName) throws IOException {
        GetIndexRequest request = new GetIndexRequest(indexName);
        return highLevelClient.indices().exists(request, RequestOptions.DEFAULT);
    }

    /**
     * getIndexSetting
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    public Map<String, String> getIndexSetting(String indexName) throws IOException {
        GetSettingsRequest request = new GetSettingsRequest().indices(indexName);
        GetSettingsResponse getSettingsResponse = highLevelClient.indices().getSettings(request, RequestOptions.DEFAULT);
        System.out.println(JSON.toJSONString(getSettingsResponse));
        return new HashMap<>();
    }

    /**
     * deleteIndex
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    public boolean deleteIndex(String indexName) throws IOException {
        try {
            DeleteIndexRequest request = new DeleteIndexRequest(indexName);
            return highLevelClient.indices().delete(request, RequestOptions.DEFAULT).isAcknowledged();
        } catch (ElasticsearchException exception) {
            if (exception.status() == RestStatus.NOT_FOUND) {
                return true;
            }
        }
        return false;
    }

    /**
     * createDoc
     *
     * @param object
     * @return
     * @throws IOException
     */
    public <T extends BaseEntity> boolean createDoc(T object) throws IOException {
        return this.createDoc(object.getClass().getSimpleName().toLowerCase(), object);
    }

    /**
     * createDoc
     *
     * @param indexName
     * @param object
     * @return
     * @throws IOException
     */
    public <T extends BaseEntity> boolean createDoc(String indexName, T object) throws IOException {
        createIndexIfNotExit(indexName, object);
        IndexResponse response = highLevelClient.index(createIndexRequest(indexName, object), RequestOptions.DEFAULT);
        return response.getResult() == DocWriteResponse.Result.CREATED;
    }

    /**
     * createDoc
     *
     * @param indexName
     * @param dataMap
     * @return
     * @throws IOException
     */
    public boolean createDoc(String indexName, Map<String, Object> dataMap) throws IOException {
        if (!isExistsIndex(indexName)) {
            return false;
        }
        IndexResponse response = highLevelClient.index(createIndexRequest(indexName, dataMap), RequestOptions.DEFAULT);
        return response.getResult() == DocWriteResponse.Result.CREATED;
    }

    /**
     * createIndexIfNotExit
     *
     * @param indexName
     * @param t
     * @param <T>
     * @throws IOException
     */
    private <T extends BaseEntity> void createIndexIfNotExit(String indexName, T t) throws IOException {
        //如果索引不存在就创建索引，其实这样不好，最好是抛出异常，必须主动创建索引
        if (!isExistsIndex(indexName)) {
            this.createIndex(indexName, t.getClass());
        }
    }

    /**
     * @param object
     * @param <T>
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private <T> String getIdFromInstance(T object) throws NoSuchFieldException, IllegalAccessException {
        Field field = object.getClass().getDeclaredField(PK_ID);
        field.setAccessible(true);
        Object ido = field.get(object);
        if (ido == null) {
            return null;
        }
        String ids = ido.toString().trim();
        if (StringUtils.hasText(ids)) {
            return ids;
        }
        return null;
    }

    /**
     * createBatchDoc
     *
     * @param indexName
     * @param dataList
     * @return
     * @throws IOException
     */
    public <T extends BaseEntity> BulkResponse createBatchDoc(String indexName, List<T> dataList) throws IOException, NoSuchFieldException, IllegalAccessException {

        if (dataList == null || dataList.isEmpty()) {
            return null;
        }

        createIndexIfNotExit(indexName, dataList.get(0));

        BulkRequest bulkRequest = new BulkRequest();
        for (T t : dataList) {
            bulkRequest.add(createIndexRequest(indexName, t));
        }
        return highLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    /**
     * createIndexRequest
     *
     * @param indexName
     * @param object
     * @return
     */
    private IndexRequest createIndexRequest(String indexName, Object object) {
        Map<String, Object> dataMap = null;
        if (BaseEntity.class.isAssignableFrom(object.getClass())) {
            dataMap = JSON.parseObject(JSON.toJSONString(object), Map.class);
        } else if (object instanceof Map) {
            dataMap = (Map<String, Object>) object;
        } else {
            throw new RuntimeException("Neither Map nor subclass of BaseEntity");
        }

        if (dataMap.containsKey(PK_ID) && dataMap.getOrDefault(PK_ID, null) != null) {
            return new IndexRequest(indexName)
                    .id(dataMap.remove(PK_ID).toString())
                    .source(dataMap, XContentType.JSON)
                    .setRefreshPolicy(IMMEDIATE);
        } else {
            dataMap.remove(PK_ID);
            return new IndexRequest(indexName)
                    .id(UUID.randomUUID().toString())
                    .source(dataMap, XContentType.JSON)
                    .setRefreshPolicy(IMMEDIATE);
        }
    }

    /**
     * getDocAsString
     *
     * @param indexName
     * @param id
     * @return
     * @throws IOException
     */
    public String getDocAsString(String indexName, String id) throws IOException {
        return JSON.toJSONString(getDocAsMap(indexName, id));
    }

    /**
     * getDocAsMap
     *
     * @param indexName
     * @param id
     * @return
     * @throws IOException
     */
    public Map<String, Object> getDocAsMap(String indexName, String id) throws IOException {
        GetRequest getRequest = new GetRequest(indexName, id);
        GetResponse response = highLevelClient.get(getRequest, RequestOptions.DEFAULT);
        if (response != null && response.isExists()) {
            Map<String, Object> map = response.getSourceAsMap();
            map.put(PK_ID, response.getId());
            return map;
        }
        return null;
    }

    /**
     * deleteDoc
     *
     * @param indexName
     * @param id
     * @return
     * @throws IOException
     */
    public boolean deleteDoc(String indexName, String id) throws IOException {
        DeleteRequest request = new DeleteRequest(indexName, id);
        DeleteResponse deleteResponse = highLevelClient.delete(request, RequestOptions.DEFAULT);
        return deleteResponse.getResult() == DocWriteResponse.Result.DELETED || deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND;
    }

    /**
     * updateDoc
     *
     * @param indexName
     * @param id
     * @param dataMap
     * @return
     * @throws IOException
     */
    public boolean updateDoc(String indexName, String id, Map<String, Object> dataMap) throws IOException {
        if (StringUtils.hasText(id)) {
            UpdateRequest request = new UpdateRequest(indexName, id).doc(dataMap);
            UpdateResponse updateResponse = highLevelClient.update(request, RequestOptions.DEFAULT);
            return updateResponse.getResult() == DocWriteResponse.Result.UPDATED;
        }
        throw new RuntimeException("primary key of the document is missing");
    }

    /**
     * updateDoc
     *
     * @param indexName
     * @param t
     * @param <T>
     * @return
     * @throws IOException
     */
    public <T extends BaseEntity> boolean updateDoc(String indexName, String id, T t) throws IOException {
        Map<String, Object> dataMap = JSON.parseObject(JSON.toJSONString(t), Map.class);
        return updateDoc(indexName, id, dataMap);
    }

    /**
     * updateDocsByQuery
     *
     * @param indexName
     * @param queryList
     * @return
     * @throws IOException
     */
    public BulkByScrollResponse updateDocsByQuery(String indexName, List<ESQuery> queryList, Script script) throws IOException {
        return updateBatchByQuery(indexName, queryList, script);
    }

    /**
     * updateDocsByQuery
     *
     * @param indexName
     * @param esQuery
     * @return
     * @throws IOException
     */
    public BulkByScrollResponse updateDocsByQuery(String indexName, ESQuery esQuery, Script script) throws IOException {
        return updateBatchByQuery(indexName, esQuery, script);
    }

    /**
     * updateBatchByQuery
     *
     * @param indexName
     * @param esQueryObject
     * @return
     * @throws IOException
     */
    private BulkByScrollResponse updateBatchByQuery(String indexName, Object esQueryObject, Script script) throws IOException {

        if (script == null) {
            return null;
        }

        UpdateByQueryRequest updateByQueryRequest = new UpdateByQueryRequest(indexName);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        if (esQueryObject != null) {
            if (esQueryObject instanceof ESQuery) {
                querySetting(searchSourceBuilder, (ESQuery) esQueryObject);
            } else {
                querySetting(searchSourceBuilder, (List<ESQuery>) esQueryObject);
            }
        }

        updateByQueryRequest.setQuery(searchSourceBuilder.query());
        updateByQueryRequest.setScript(script);
        return highLevelClient.updateByQuery(updateByQueryRequest, RequestOptions.DEFAULT);
    }

    /**
     * searchAllAsMap
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    public List<Map<String, Object>> searchAllAsMap(String indexName) throws IOException {
        return searchAllAsMap(indexName, MAX_COUNT_PER_PAGE);
    }

    /**
     * searchAllAsMap
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    public List<Map<String, Object>> searchAllAsMap(String indexName, int count) throws IOException {
        SearchHits searchHits = searchAll(indexName, new ESPage<>(1, count)).getHits();
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            Map<String, Object> map = hit.getSourceAsMap();
            map.put(PK_ID, hit.getId());
            dataList.add(map);
        }
        return dataList;
    }

    /**
     * searchAllAsString
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    public List<String> searchAllAsString(String indexName) throws IOException {
        return searchAllAsString(indexName, MAX_COUNT_PER_PAGE);
    }

    /**
     * searchAllAsString
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    public List<String> searchAllAsString(String indexName, int count) throws IOException {
        SearchHits searchHits = searchAll(indexName, new ESPage<>(1, count)).getHits();
        List<String> dataList = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            JSONObject map = JSON.parseObject(hit.getSourceAsString());
            map.put(PK_ID, hit.getId());
            dataList.add(map.toJSONString());
        }
        return dataList;
    }

    /**
     * searchAllAsString
     *
     * @param indexName
     * @return
     * @throws IOException
     */
    private SearchResponse searchAll(String indexName, ESPage<?> ESPage) throws IOException {
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        pageSetting(searchSourceBuilder, ESPage);
        searchRequest.source(searchSourceBuilder);
        return highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
    }

    /**
     * orderSetting
     *
     * @param searchSourceBuilder
     * @param sortList
     */
    private void sortSetting(SearchSourceBuilder searchSourceBuilder, List<ESSort> sortList) {
        if (sortList != null && !sortList.isEmpty()) {
            List<SortBuilder<?>> sortBuilderList = new ArrayList<>();
            for (ESSort esSort : sortList) {
                if (StringUtils.hasText(esSort.getFieldName()) && esSort.getSortOrder() != null) {
                    sortBuilderList.add(new FieldSortBuilder(esSort.getFieldName()).order(esSort.getSortOrder()));
                }
            }
            searchSourceBuilder.sort(sortBuilderList);
        } else {
            searchSourceBuilder.sort(new ScoreSortBuilder().order(SortOrder.DESC));
        }
    }

    /**
     * querySetting
     *
     * @param searchSourceBuilder
     * @param queryList
     */
    private void querySetting(SearchSourceBuilder searchSourceBuilder, List<ESQuery> queryList) {
        if (queryList != null && !queryList.isEmpty()) {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            searchSourceBuilder.query(buildBoolQueryBuilder(boolQueryBuilder, queryList));
        }
    }

    /**
     * querySetting
     *
     * @param searchSourceBuilder
     * @param esQuery
     */
    private void querySetting(SearchSourceBuilder searchSourceBuilder, ESQuery esQuery) {
        if (esQuery != null) {
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            searchSourceBuilder.query(buildBoolQueryBuilder(boolQueryBuilder, esQuery));
        }
    }

    /**
     * buildBoolQueryBuilder
     *
     * @param boolQueryBuilder
     * @param queryList
     * @return
     */
    private BoolQueryBuilder buildBoolQueryBuilder(BoolQueryBuilder boolQueryBuilder, List<ESQuery> queryList) {
        if (queryList != null && !queryList.isEmpty()) {
            for (ESQuery esQuery : queryList) {
                buildBoolQueryBuilder(boolQueryBuilder, esQuery);
            }
        }
        return boolQueryBuilder;
    }

    /**
     * buildBoolQueryBuilder
     *
     * @param boolQueryBuilder
     * @param esQuery
     * @return
     */
    private BoolQueryBuilder buildBoolQueryBuilder(BoolQueryBuilder boolQueryBuilder, ESQuery esQuery) {
        if (StringUtils.hasText(esQuery.getFieldName())) {
            if (ESQuery.QueryType.MUST == esQuery.getQueryType()) {
                boolQueryBuilder.must(QueryBuilders.matchQuery(esQuery.getFieldName(), esQuery.getQueryParam()));
            } else if (ESQuery.QueryType.MUST_NOT == esQuery.getQueryType()) {
                boolQueryBuilder.mustNot(QueryBuilders.matchQuery(esQuery.getFieldName(), esQuery.getQueryParam()));
            } else {
                boolQueryBuilder.should(QueryBuilders.matchQuery(esQuery.getFieldName(), esQuery.getQueryParam()));
            }
        }
        return boolQueryBuilder;
    }

    /**
     * pageSetting
     *
     * @param searchSourceBuilder
     * @param ESPage
     */
    private void pageSetting(SearchSourceBuilder searchSourceBuilder, ESPage<?> ESPage) {
        if (ESPage == null) {
            ESPage = new ESPage<>();
        }
        searchSourceBuilder.from((ESPage.getCurrPage() - 1) * ESPage.getPageSize());
        searchSourceBuilder.size(ESPage.getPageSize());
    }

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
    public SearchResponse searchPage(String indexName, List<ESQuery> queryList, List<ESSort> sortList, ESPage<?> ESPage) throws IOException {
        return searchPage(indexName, queryList, sortList, null, null, ESPage);
    }

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
    public SearchResponse searchPage(String indexName, List<ESQuery> queryList, List<ESSort> sortList, String[] includeFields, String[] excludeFields, ESPage<?> ESPage) throws IOException {
        SearchRequest searchRequest = new SearchRequest(indexName);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        pageSetting(searchSourceBuilder, ESPage);
        querySetting(searchSourceBuilder, queryList);
        sortSetting(searchSourceBuilder, sortList);
        searchSourceBuilder.fetchSource(true);
        searchSourceBuilder.fetchSource(includeFields, excludeFields);
        searchRequest.source(searchSourceBuilder);
        return highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
    }

    /**
     * searchPageAsString
     *
     * @param indexName
     * @param queryList
     * @param sortList
     * @return
     * @throws IOException
     */
    public ESPage<String> searchPageAsString(String indexName, List<ESQuery> queryList, List<ESSort> sortList, ESPage<String> ESPage) throws IOException {
        return searchPageAsString(indexName, queryList, sortList, null, null, ESPage);
    }

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
    public ESPage<String> searchPageAsString(String indexName, List<ESQuery> queryList, List<ESSort> sortList, String[] includeFields, String[] excludeFields, ESPage<String> ESPage) throws IOException {
        if (ESPage == null) {
            ESPage = new ESPage<>();
        }
        SearchHits searchHits = searchPage(indexName, queryList, sortList, includeFields, excludeFields, ESPage).getHits();
        List<String> dataList = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            dataList.add(hit.getSourceAsString());
        }
        return ESPage.rebuild(new Long(searchHits.getTotalHits().value).intValue(), dataList);
    }

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
    public ESPage<Map<String, Object>> searchPageAsMap(String indexName, List<ESQuery> queryList, List<ESSort> sortList, ESPage<Map<String, Object>> ESPage) throws IOException {
        return searchPageAsMap(indexName, queryList, sortList, null, null, ESPage);
    }

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
    public ESPage<Map<String, Object>> searchPageAsMap(String indexName, List<ESQuery> queryList, List<ESSort> sortList, String[] includeFields, String[] excludeFields, ESPage<Map<String, Object>> ESPage) throws IOException {
        if (ESPage == null) {
            ESPage = new ESPage<>();
        }
        SearchHits searchHits = searchPage(indexName, queryList, sortList, includeFields, excludeFields, ESPage).getHits();
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            dataList.add(hit.getSourceAsMap());
        }
        return ESPage.rebuild(new Long(searchHits.getTotalHits().value).intValue(), dataList);
    }

}
