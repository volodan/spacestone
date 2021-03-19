package com.uniqa.spacestone;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.uniqa.spacestone.dto.DocumentTypeEnum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import net.sourceforge.tess4j.Tesseract;

@SpringBootApplication
public class SpacestoneApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpacestoneApplication.class, args);
	}

	@Bean
	public Tesseract getTesseract() {
		Tesseract tesseract = new Tesseract(); 

		// FIXME - can be set as an absolute path
		// set path to tess learned data for OCR
        tesseract.setDatapath(System.getenv("TESSDATA_PREFIX")); // this was set in OS
        
		// set language and psm for page segmentation, 3 means, that page contains more lines
        tesseract.setPageSegMode(3);
        tesseract.setLanguage("deu");

		return tesseract;
	} 

	@Bean
	public Map<DocumentTypeEnum, List<String>> documentTypeConfigMap() {
		Map<DocumentTypeEnum, List<String>> configMap = new LinkedHashMap<>();	
		configMap.put(DocumentTypeEnum.PHARMACY_BILL, Arrays.asList("apotheke"));
		configMap.put(DocumentTypeEnum.DOCTORS_BILL, Arrays.asList("honorarnote"));
		configMap.put(DocumentTypeEnum.SVA, Arrays.asList("Österreichische","Gesundheitskasse"));

		return configMap;
	}
}
