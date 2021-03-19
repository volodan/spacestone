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
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.uniqa.spacestone.dto.DocumentTypeDto;
import com.uniqa.spacestone.dto.DocumentTypeEnum;
import com.uniqa.spacestone.dto.FuzzySearchResultDto;
import com.uniqa.spacestone.service.TextProcessorService;

@Controller
@RequestMapping("/ocr")
@Slf4j
public class OcrController {
    
    @Autowired
    private TextProcessorService textProcessorService;

    @Autowired
    private Tesseract tesseract;

    @Autowired
    private Map<DocumentTypeEnum, List<String>> documentTypeConfigMap;

    @PostMapping(value = "/fuzzySearch")
    public ResponseEntity<List<FuzzySearchResultDto>> ocrImage(@RequestParam List<String> queryStrings,
        @RequestPart("document") MultipartFile document) {

        List<FuzzySearchResultDto> result = new ArrayList<>();    

        processDocument(document, (pageText) -> {

            log.debug("Recognized text: \n{}", pageText);
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
        });

        return ResponseEntity.ok().body(result);
    }

    @PostMapping(value = "/documentType")
    public ResponseEntity<DocumentTypeDto> ocrImage(@RequestPart("document") MultipartFile document) {

        List<DocumentTypeEnum> documentTypes = new ArrayList<>();
        processDocument(document, (pageText) -> {

            log.debug("Recognized text: \n{}", pageText);
            List<String> wordsInPageText = Arrays.asList(pageText.split("\\s+"));
            // remove all empty places
            wordsInPageText.forEach(item -> item.trim());

            // now do fuzzy search for every query string in the page
            for (DocumentTypeEnum typeKey: documentTypeConfigMap.keySet()) {
                List<ExtractedResult> tempSearchResults = new ArrayList<>();

                // iterate through all the keywords and find their score, if average score is then > 80, document is the type
                // of the evaluated search key 
                for (String queryString : documentTypeConfigMap.get(typeKey)) {
                    tempSearchResults.add(textProcessorService.fuzzyStringSearchBestMatch(queryString, 
                        wordsInPageText.stream().filter(word -> (StringUtils.hasText(word) && word.length() > 3 )).collect(Collectors.toList())));
                }
                double avgScore = tempSearchResults.stream().mapToInt(item -> item.getScore()).summaryStatistics().getAverage();
                log.debug("Avg score for the type {} in document {} is: {}", typeKey, document.getOriginalFilename(), avgScore);
                if (avgScore > 75.0) documentTypes.add(typeKey);
            }
        });

        return ResponseEntity.ok().body(
            documentTypes.size() > 0 ? 
                DocumentTypeDto.builder().documentType(documentTypes).build() : 
                DocumentTypeDto.builder().documentType(Arrays.asList(DocumentTypeEnum.NOT_RECOGNIZED)).build()
        );
    }

    private void processDocument(MultipartFile document, Consumer<String> pageConsumer) {
        try { 
            // if pdf, first convert to BufferedImage
            // FIXME - pdf pages are processed separatelly!
            if (document.getOriginalFilename().endsWith(".pdf") || document.getOriginalFilename().endsWith(".PDF")) {
                PDDocument pdfDoc = PDDocument.load(document.getInputStream());
                PDFRenderer pdfRenderer = new PDFRenderer(pdfDoc);

                for (int page = 0; page < pdfDoc.getNumberOfPages(); page++) {
                    BufferedImage pdfBufferedImage = pdfRenderer.renderImage(page, 1.0f, ImageType.RGB);
                    pageConsumer.accept(tesseract.doOCR(pdfBufferedImage));
                }
            } else {
                BufferedImage bgImage = ImageIO.read(document.getInputStream());
                pageConsumer.accept(tesseract.doOCR(bgImage));
            }
    
        } catch (Exception e) { 
            log.error("Exception occured during processing file: {}", document.getOriginalFilename(), e);
        }
    }
}
