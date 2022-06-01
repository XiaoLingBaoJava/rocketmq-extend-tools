package com.xiaolingbao.duplicate.persistence;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @author: xiaolingbao
 * @date: 2022/5/22 11:12
 * @description: 
 */

@AllArgsConstructor
@Getter
@ToString
public class PersistenceElement {
    private String application;
    private String topic;
    private String tag;
    private String msgUniqKey;
}
