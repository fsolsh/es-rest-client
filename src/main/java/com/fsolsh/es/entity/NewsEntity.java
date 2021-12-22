package com.fsolsh.es.entity;

import lombok.Data;

/**
 * NewsEntity
 */
@Data
public class NewsEntity extends BaseEntity {

    private String id;

    private String type;

    private String category;

    private String section;

    private String subSection;

    private String title;

    private String keyWords;

    private String summary;

    private String content;

    private String source;

    private String publisher;

    private Long publishTime;

    private Long updateTime;
}