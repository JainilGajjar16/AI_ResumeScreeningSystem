package com.resume.screening.service;

import com.resume.screening.dto.StudentProfileDto;
import com.resume.screening.entity.StudentProfile;
import com.resume.screening.entity.User;

public interface StudentProfileService {
    StudentProfileDto getProfileByUsernameOrEmail(String usernameOrEmail);
    StudentProfileDto updateProfile(String usernameOrEmail, StudentProfileDto profileDto);
    int calculateProfileCompletion(User user, StudentProfile profile);
    StudentProfile getStudentProfileEntityByUsernameOrEmail(String usernameOrEmail);
}
