package com.resume.screening.service;

import com.resume.screening.dto.*;
import java.util.List;

public interface AdminService {
    AdminDashboardStatsDto getDashboardStats();
    List<AdminUserDto> getUsers(String query, String roleFilter);
    List<AdminStudentDto> getStudents(String query);
    List<AdminRecruiterDto> getRecruiters(String query);
    List<AdminJobDto> getJobs(String query, String statusFilter);
    List<AdminApplicationDto> getApplications(String query, String statusFilter);
    AdminReportDto getReports();
}
