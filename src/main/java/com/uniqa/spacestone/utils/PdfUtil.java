package com.uniqa.spacestone.utils;

import com.uniqa.spacestone.dto.DocumentDto;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class PdfUtil {

    @Autowired
    private Tesseract tesseract;

    public DocumentDto processPdfDocument(MultipartFile document) {
//        log.debug("-----------------------------------------------------");
//        log.debug("Document: " + document.getOriginalFilename());
        List<String> pages = new ArrayList<>();

        try {
            // convert PDF to jpeg
            PDDocument pdfDoc = PDDocument.load(document.getInputStream());
            PDFRenderer pdfRenderer = new PDFRenderer(pdfDoc);

            for (int page = 0; page < pdfDoc.getNumberOfPages(); page++) {
//                log.debug("Page number: " + page);
                BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(page, 96, ImageType.GRAY);
                String pageText = tesseract.doOCR(bufferedImage);
//                log.debug(pageText);
                pages.add(pageText);
//                log.debug("Page Finish.");
            }

        } catch (Exception e) {
            log.error("Exception occured during processing file: {}", document.getOriginalFilename(), e);
        }

        DocumentDto result = new DocumentDto();
        result.setFilename(document.getOriginalFilename());
        result.setPages(pages);
        List<String> words = new ArrayList<>();
        pages.stream().map(page -> Arrays.asList(page.split("\\s+"))).forEach(words::addAll);
        words.forEach(String::trim);
        result.setWords(words);
//        log.debug("Document finish.");
//        log.debug("-----------------------------------------------------");
        return result;
    }
}
