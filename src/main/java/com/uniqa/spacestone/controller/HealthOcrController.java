package com.uniqa.spacestone.controller;

import com.uniqa.spacestone.dto.DocumentDto;
import com.uniqa.spacestone.dto.HealthOcrDocumentDto;
import com.uniqa.spacestone.exception.BadRequestException;
import com.uniqa.spacestone.exception.InternalException;
import com.uniqa.spacestone.utils.PdfUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/health/ocr")
@Slf4j
public class HealthOcrController {

    private static final String LINE_SEPARATOR = "\n";
    private static final String HOUR_SEPARATOR = "0 Uhr";
    private static final String BRIEF_DOCUMENT = "Brief";
    private static final String POLIZZE_DOCUMENT = "Polizze";
    private static final String ANPASSUNGSPOLIZZE = "Anpassungspolizze gilt ab ";
    private static final String GESCHAFTSBEDINGUNGEN = "bedingungen";
    private static final String PRIVAT_RUNDUM = "Privat Rundum ";
    private static final String SONDERKLASSE_SELECT = "Sonderklasse Select ";
    private static final String LISTE_DER_VERTRAGSKRANKENHAUSER = "Liste der Vertragskrankenhäuser ";
    private static final String ERGANZUNGSTARIF = "Ergänzungstarif für die Sonderklasse";
    private static final String VERSICHERUNGSSCHUTZ = "Versicherungsschutz für ambulante Behandlung";
    private static final String POLICY_NUMBER = "Polizzen-Nr.: ";
    private static final String NACHTRAG = "Nachtrag";
    private static final String GILT_AB = "Diese Polizze gilt ab";

    @Autowired
    private PdfUtil pdfUtil;

    @PostMapping(produces = "application/json")
    public ResponseEntity<List<HealthOcrDocumentDto>> ocrImage(@RequestPart("document") List<MultipartFile> files) {
        List<HealthOcrDocumentDto> healthOcrDocument = new ArrayList<>();

        //process all documents
        List<DocumentDto> documents = files.stream()
                .map(document -> pdfUtil.processPdfDocument(document))
                .collect(Collectors.toList());

        //get Brief document
        DocumentDto brief = getRequiredDocument(documents, BRIEF_DOCUMENT);
        if (brief == null) {
            log.error("There is no Brief document in the list of documents");
            throw new BadRequestException("No Brief document found");
        }

        //get policy number and nachtrag number from Brief document
        String policyNumber = getSelectedField(brief, POLICY_NUMBER, LINE_SEPARATOR, 0);
        Integer nachtragNumber = getNachtragNumber(brief);

        //get Polizze document
        DocumentDto polizze = getRequiredDocument(documents, POLIZZE_DOCUMENT);
        if (polizze == null) {
            log.error("There is no Polizze document in the list of documents");
            throw new BadRequestException("No Polizze document found");
        }

        validateDocument(polizze, policyNumber, nachtragNumber);

        //get the valid from date (gilt ab)
        String fromDate = getSelectedField(polizze, GILT_AB, HOUR_SEPARATOR, 0);

        // add change of name
        healthOcrDocument.add(new HealthOcrDocumentDto(polizze.getFilename(), ANPASSUNGSPOLIZZE + fromDate + ".pdf"));

        //update Polizze filename
        polizze.setFilename(ANPASSUNGSPOLIZZE + fromDate);

        //process Geschäftsbedingungen
        for (DocumentDto document : documents) {
            if (document.getFilename().contains(GESCHAFTSBEDINGUNGEN)) {
                validateBedingungeDocument(document, policyNumber);
                if (containsField(document, PRIVAT_RUNDUM, 0)) {
                    String date = getSelectedField(document, "QHGYA/QHGYB K ", LINE_SEPARATOR, 0);
                    healthOcrDocument.add(new HealthOcrDocumentDto(document.getFilename(), PRIVAT_RUNDUM + date + ".pdf"));
                    document.setFilename(PRIVAT_RUNDUM + date);
                } else if (containsField(document, SONDERKLASSE_SELECT, 0)) {
                    String date = getSelectedField(document, "QGYA/QGYB ", LINE_SEPARATOR, 0);
                    healthOcrDocument.add(new HealthOcrDocumentDto(document.getFilename(), SONDERKLASSE_SELECT + date + ".pdf"));
                    document.setFilename(SONDERKLASSE_SELECT + date);
                } else if (containsField(document, LISTE_DER_VERTRAGSKRANKENHAUSER, 0)) {
                    String date = getSelectedField(document, "Stand ", LINE_SEPARATOR, 0);
                    healthOcrDocument.add(new HealthOcrDocumentDto(document.getFilename(), LISTE_DER_VERTRAGSKRANKENHAUSER + "Stand " + date + ".pdf"));
                    document.setFilename(LISTE_DER_VERTRAGSKRANKENHAUSER + "Stand " + date);
                } else if (containsField(document, ERGANZUNGSTARIF, 0)) {
                    String date = getSelectedField(document, "QLYA/QLYB ", LINE_SEPARATOR, 0);
                    healthOcrDocument.add(new HealthOcrDocumentDto(document.getFilename(), ERGANZUNGSTARIF + " " + date + ".pdf"));
                    document.setFilename(ERGANZUNGSTARIF + " " + date);
                } else if (containsField(document, VERSICHERUNGSSCHUTZ, 1)) {
                    String date = getSelectedField(document, "QAYA/QAYB ", LINE_SEPARATOR, 1);
                    healthOcrDocument.add(new HealthOcrDocumentDto(document.getFilename(), VERSICHERUNGSSCHUTZ + " " + date + ".pdf"));
                    document.setFilename(VERSICHERUNGSSCHUTZ + " " + date);
                }
            }
        }

        return ResponseEntity.ok().body(healthOcrDocument);
    }

    private DocumentDto getRequiredDocument(List<DocumentDto> documents, String documentName) {
        return documents.stream()
                .filter(document -> document.getFilename()
                        .contains(documentName))
                .findFirst()
                .orElse(null);
    }

    private String getSelectedField(DocumentDto document, String field, String separator, int page) {
        int startIndex = document.getPages().get(page).indexOf(field) + field.length();
        int endIndex = document.getPages().get(page).indexOf(separator, startIndex);

        try {
            return document.getPages().get(page).substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            log.error("Error {} while parsing field {}", e.getMessage(), field);
            throw new InternalException("Error while parsing field from the document");
        }
    }

    private boolean containsField(DocumentDto document, String field, int page) {
        return document.getPages().get(page).contains(field);
    }

    private Integer getNachtragNumber(DocumentDto document) {
        int nachtragIndex = document.getWords().indexOf(NACHTRAG);
        try {
            return Integer.valueOf(document.getWords().get(nachtragIndex + 1));
        } catch (Exception e) {
            log.error("Error appeared when reading the Nachtrag Number from Brief document");
            throw new InternalException("Error when reading Nachtrag Number");
        }
    }

    private void validateDocument(DocumentDto document, String policyNumber, Integer nachtragNumber) {
        if (!document.getPages().get(0).contains(policyNumber)) {
            log.error("Document {} does not contain policy number {}", document.getFilename(), policyNumber);
            throw new InternalException("Document validation failed");
        }

        if (!document.getPages().get(0).contains(NACHTRAG + " " + nachtragNumber)) {
            log.error("Document {} does not contain correct nachtrag number {}", document.getFilename(), nachtragNumber);
            throw new InternalException("Document validation failed");
        }
    }

    private void validateBedingungeDocument(DocumentDto document, String policyNumber) {
        if (!document.getPages().get(0).contains(policyNumber)) {
            log.error("Document {} does not contain policy number {}", document.getFilename(), policyNumber);
        }
    }
}