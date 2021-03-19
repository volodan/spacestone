package com.uniqa.spacestone.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import lombok.extern.slf4j.Slf4j;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

@Service
@Slf4j
public class TextProcessorService {

    public List<ExtractedResult> fuzzyStringSearch(String queryString, List<String> choices) {
        Assert.hasLength(queryString, "Query string cannot be empty!");
        List<ExtractedResult> result = FuzzySearch.extractAll(queryString, choices, 60);
        log.debug(result.toString());
        return result;
    }

    public ExtractedResult fuzzyStringSearchBestMatch(String queryString, List<String> choices) {
        Assert.hasLength(queryString, "Query string cannot be empty!");
        ExtractedResult result = FuzzySearch.extractOne(queryString, choices);
        log.debug(result.toString());
        // return only top result
        return result;
    }
    
}
