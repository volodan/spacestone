package com.uniqa.spacestone.dto;

import lombok.Data;

import java.util.List;

@Data
public class DocumentDto {

    private String filename;
    private List<String> pages;
    private List<String> words;
    private String ocrRecognition;

}
