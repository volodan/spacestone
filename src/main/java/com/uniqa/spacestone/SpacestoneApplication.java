package com.uniqa.spacestone;

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
}
