package com.uniqa.spacestone.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentTypeDto {
    private List<DocumentTypeEnum> documentType;
}
