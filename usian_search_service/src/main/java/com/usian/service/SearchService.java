package com.usian.service;

import com.usian.pojo.SearchItem;

import java.io.IOException;
import java.util.List;

public interface SearchService {
    Boolean importAll();

    List<SearchItem> selectByQ(String q, Long page, Integer pageSize);

    int insertDocument(String msg) throws IOException;
}
