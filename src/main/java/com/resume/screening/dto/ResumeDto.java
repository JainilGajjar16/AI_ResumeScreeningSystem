package com.resume.screening.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeDto {
    private Long id;
    private Long studentProfileId;
    private String originalFileName;
    private String storedFileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String fileSizeFormatted;
    private Integer currentVersion;
    private String status;
    private Boolean isCurrent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String formattedUploadDate;
    private boolean pdf;

    @Builder.Default
    private List<ResumeVersionDto> versions = new ArrayList<>();
}
