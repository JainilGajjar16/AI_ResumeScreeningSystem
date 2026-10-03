package com.resume.screening.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeVersionDto {
    private Long id;
    private Integer versionNumber;
    private String originalFileName;
    private String storedFileName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String fileSizeFormatted;
    private String commitMessage;
    private LocalDateTime createdAt;
    private String formattedUploadDate;
}
