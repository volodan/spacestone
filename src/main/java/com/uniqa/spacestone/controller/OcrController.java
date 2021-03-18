package com.uniqa.spacestone.controller;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;

import java.awt.image.BufferedImage;

@Controller
@RequestMapping("/ocr")
@Slf4j
public class OcrController {
    
    @PostMapping
    public ResponseEntity<String> ocrImage(@RequestParam String language,
        @RequestPart("document") MultipartFile document) {

        Tesseract tesseract = new Tesseract(); 
        StringBuilder retStringBuilder = new StringBuilder();    
        
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
                String ocredText = tesseract.doOCR(bufferedImage);
                retStringBuilder.append(ocredText);
                log.info("Text on page: {} is \n{}\n\n", page, ocredText);
            }
        } 
        catch (Exception e) { 
            log.error("Exception occured during processing file: {}", document.getOriginalFilename(), e);
        }
        return ResponseEntity.ok().body(retStringBuilder.toString());
    }
}
