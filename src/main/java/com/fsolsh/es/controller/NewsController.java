package com.fsolsh.es.controller;

import com.alibaba.fastjson.JSON;
import com.fsolsh.es.common.ESQuery;
import com.fsolsh.es.common.ESSort;
import com.fsolsh.es.entity.NewsEntity;
import com.fsolsh.es.service.impl.ENWordServiceImpl;
import com.fsolsh.es.service.impl.ESRestServiceImpl;
import org.elasticsearch.script.Script;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Demo NewsController
 */
@RestController
public class NewsController {

    private final String index = NewsEntity.class.getSimpleName().toLowerCase();

    @Autowired
    private ESRestServiceImpl restService;
    @Autowired
    private ENWordServiceImpl wordService;

    /**
     * 新增数据
     */
    @RequestMapping("/addNews")
    public Object addNews() throws Exception {

        boolean result = restService.deleteIndex(index);

        for (int i = 0; i < 15; i++) {
            NewsEntity newsEntity = new NewsEntity();
            newsEntity.setId(UUID.randomUUID().toString());
            newsEntity.setTitle("title java " + i);
            newsEntity.setType("java");
            newsEntity.setSummary("this is a news about java loving");
            newsEntity.setUpdateTime(System.currentTimeMillis());
            restService.createDoc(index, newsEntity);
        }

        for (int i = 0; i < 15; i++) {
            NewsEntity newsEntity = new NewsEntity();
            newsEntity.setId(UUID.randomUUID().toString());
            newsEntity.setTitle("title go" + i);
            newsEntity.setType("go");
            newsEntity.setSummary("this is a news about go loving");
            newsEntity.setUpdateTime(System.currentTimeMillis());
            restService.createDoc(index, newsEntity);
        }

        return "success";
    }

    /**
     * 查询数据
     */
    @RequestMapping("/queryNews")
    public Object queryNews(@RequestParam("keyword") String keyword) throws IOException {

        List<ESQuery> queryList = new ArrayList<>();
        ESQuery query = new ESQuery();
        query.setFieldName("summary");
        query.setQueryParam(wordService.getRelatedWords(keyword, 20));
        queryList.add(query);

        List<ESSort> sortList = new ArrayList<>();
        ESSort sort = new ESSort();
        sort.setFieldName("publishTime");
        sortList.add(sort);

        return restService.searchPageAsMap(index, queryList, sortList, null);
    }

    /**
     * 更新数据
     */
    @RequestMapping("/updateNews")
    public Object updateNews() throws Exception {
        String scriptStr = "ctx._source['summary']='this field has been updated'";
        Script script = new Script(scriptStr);

        ESQuery esQuery = new ESQuery();
        esQuery.setQueryType(ESQuery.QueryType.MUST_NOT);
        esQuery.setFieldName("type");
        esQuery.setQueryParam("java");
        return restService.updateDocsByQuery(index, esQuery, script);
    }

    /**
     * 更新数据
     */
    @RequestMapping("/modifyNews")
    public Object modifyNews() throws Exception {
        List<String> data = restService.searchAllAsString(index, 1);
        if (data != null && data.size() != 0) {
            NewsEntity document = JSON.parseObject(data.get(0), NewsEntity.class);
            System.out.println(JSON.toJSONString(document));
            document.setSummary("modifyNews");
            return restService.getDocAsMap(index, document.getId());
        }

        return "modifyNews";
    }

}