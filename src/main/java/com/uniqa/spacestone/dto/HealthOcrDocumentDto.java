package com.uniqa.spacestone.dto;

import lombok.Data;

@Data
public class HealthOcrDocumentDto {

    private String originalFilename;
    private String proposedFilename;

    public HealthOcrDocumentDto(String originalFilename, String proposedFilename) {
        this.originalFilename = originalFilename;
        this.proposedFilename = proposedFilename;
    }
}
