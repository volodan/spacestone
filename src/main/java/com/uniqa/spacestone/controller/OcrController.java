package com.uniqa.spacestone.controller;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import net.sourceforge.tess4j.Tesseract;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.uniqa.spacestone.dto.FuzzySearchResultDto;
import com.uniqa.spacestone.service.TextProcessorService;

@Controller
@RequestMapping("/ocr")
@Slf4j
public class OcrController {
    
    @Autowired
    private TextProcessorService textProcessorService;

    @PostMapping
    public ResponseEntity<List<FuzzySearchResultDto>> ocrImage(@RequestParam String language, @RequestParam List<String> queryStrings,
        @RequestPart("document") MultipartFile document) {

        List<FuzzySearchResultDto> result = new ArrayList<>();    
        Tesseract tesseract = new Tesseract(); 
        
        try { 
  
            // set path to tess learned data for OCR
            tesseract.setDatapath(System.getenv("TESSDATA_PREFIX")); // this was set in OS
            // set config map l is for language and psm for page segmentation, 3 means, that page contains more lines
            tesseract.setPageSegMode(3);
            tesseract.setLanguage(language);

            // convert PDF to jpeg
            PDDocument pdfDoc = PDDocument.load(document.getInputStream());
            PDFRenderer pdfRenderer = new PDFRenderer(pdfDoc);

            // for every page on doc, render to image and then OCR
            for (int page = 0; page < pdfDoc.getNumberOfPages(); page++) {
                BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, 96, ImageType.GRAY);
                String pageText = tesseract.doOCR(bufferedImage);
                List<String> wordsInPageText = Arrays.asList(pageText.split("\\s+"));
                // remove all empty places
                wordsInPageText.forEach(item -> item.trim());

                List<String> listExtResult = new ArrayList<>();
                // now do fuzzy search for every query string in the page
                for (String queryString : queryStrings) {
                    // also remove strings which have -2
                    List<ExtractedResult> tempRes = textProcessorService.fuzzyStringSearch(queryString, 
                        wordsInPageText.stream().filter(word -> (StringUtils.hasText(word) && word.length() > queryString.length() - 2 )).collect(Collectors.toList()));
                    listExtResult.add(queryString + " => " + tempRes.toString());
                }
                result.add(FuzzySearchResultDto.builder().searchResult(listExtResult).ocrText(pageText).build());
            }
        } 
        catch (Exception e) { 
            log.error("Exception occured during processing file: {}", document.getOriginalFilename(), e);
        }
        return ResponseEntity.ok().body(result);
    }
}
