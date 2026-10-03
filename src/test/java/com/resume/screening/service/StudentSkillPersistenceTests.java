package com.resume.screening.service;

import com.resume.screening.dto.StudentProfileDto;
import com.resume.screening.entity.Skill;
import com.resume.screening.entity.StudentProfile;
import com.resume.screening.entity.User;
import com.resume.screening.repository.SkillRepository;
import com.resume.screening.repository.StudentProfileRepository;
import com.resume.screening.repository.UserRepository;
import com.resume.screening.service.impl.StudentProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class StudentSkillPersistenceTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private UserResolutionService userResolutionService;

    @InjectMocks
    private StudentProfileServiceImpl studentProfileService;

    private User sampleUser;
    private StudentProfile sampleProfile;
    private Skill javaSkill;
    private Skill springBootSkill;
    private Skill mysqlSkill;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .username("student1")
                .email("student1@example.com")
                .firstName("John")
                .lastName("Doe")
                .role("ROLE_STUDENT")
                .build();

        javaSkill = Skill.builder().id(101L).name("Java").build();
        springBootSkill = Skill.builder().id(102L).name("Spring Boot").build();
        mysqlSkill = Skill.builder().id(103L).name("MySQL").build();

        sampleProfile = StudentProfile.builder()
                .id(10L)
                .user(sampleUser)
                .phone("1234567890")
                .skills(new HashSet<>())
                .build();

        when(userResolutionService.resolveUser("student1")).thenReturn(sampleUser);
        when(studentProfileRepository.findByUserId(1L)).thenReturn(Optional.of(sampleProfile));
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Test 1: Adding existing skill reuses master row and does NOT create a duplicate in skills table")
    void testAddingExistingSkillReusesMasterRow() {
        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(javaSkill));

        StudentProfileDto dto = StudentProfileDto.builder()
                .skills("Java")
                .build();

        studentProfileService.updateProfile("student1", dto);

        verify(skillRepository, never()).save(any(Skill.class));
        assertEquals(1, sampleProfile.getSkills().size());
        assertTrue(sampleProfile.getSkills().contains(javaSkill));
        assertEquals("Java", sampleProfile.getSkillsText());
    }

    @Test
    @DisplayName("Test 2: Adding new skill persists to skills table once and maps to student")
    void testAddingNewSkillCreatesMasterRow() {
        when(skillRepository.findByNameIgnoreCase("Spring Security")).thenReturn(Optional.empty());
        Skill newSkill = Skill.builder().id(200L).name("Spring Security").build();
        when(skillRepository.save(any(Skill.class))).thenReturn(newSkill);

        StudentProfileDto dto = StudentProfileDto.builder()
                .skills("Spring Security")
                .build();

        studentProfileService.updateProfile("student1", dto);

        verify(skillRepository, times(1)).save(any(Skill.class));
        assertEquals(1, sampleProfile.getSkills().size());
        assertTrue(sampleProfile.getSkills().contains(newSkill));
        assertEquals("Spring Security", sampleProfile.getSkillsText());
    }

    @Test
    @DisplayName("Test 3: Case-insensitive duplicate input ('Java, JAVA, java') resolves to single skill")
    void testDuplicateCaseInsensitiveInputResolvedToSingleSkill() {
        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(javaSkill));

        StudentProfileDto dto = StudentProfileDto.builder()
                .skills("Java, JAVA, java")
                .build();

        studentProfileService.updateProfile("student1", dto);

        verify(skillRepository, times(1)).findByNameIgnoreCase("Java");
        assertEquals(1, sampleProfile.getSkills().size());
        assertTrue(sampleProfile.getSkills().contains(javaSkill));
    }

    @Test
    @DisplayName("Test 4: Removing skill drops student_skills mapping without deleting global skills table row")
    void testRemovingSkillDropsMappingWithoutDeletingMasterSkill() {
        sampleProfile.getSkills().add(javaSkill);
        sampleProfile.getSkills().add(springBootSkill);
        sampleProfile.setSkillsText("Java, Spring Boot");

        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(javaSkill));

        // Update profile with only "Java" (removing "Spring Boot")
        StudentProfileDto dto = StudentProfileDto.builder()
                .skills("Java")
                .build();

        studentProfileService.updateProfile("student1", dto);

        assertEquals(1, sampleProfile.getSkills().size());
        assertTrue(sampleProfile.getSkills().contains(javaSkill));
        assertFalse(sampleProfile.getSkills().contains(springBootSkill));
        // Verify skillRepository.delete was NEVER called
        verify(skillRepository, never()).delete(any());
        verify(skillRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Test 5: Profile retrieval returns sorted comma-separated skills loaded from student_skills")
    void testProfileRetrievalLoadsSkillsFromStudentSkills() {
        sampleProfile.getSkills().add(mysqlSkill);
        sampleProfile.getSkills().add(javaSkill);
        sampleProfile.getSkills().add(springBootSkill);

        StudentProfileDto profileDto = studentProfileService.getProfileByUsernameOrEmail("student1");

        assertNotNull(profileDto);
        assertEquals("Java, MySQL, Spring Boot", profileDto.getSkills());
    }

    @Test
    @DisplayName("Test 6: Legacy skills_text automatically syncs into student_skills table when skills set is empty")
    void testLegacySkillsTextSyncsIntoStudentSkills() {
        sampleProfile.getSkills().clear();
        sampleProfile.setSkillsText("Java, Spring Boot");

        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(javaSkill));
        when(skillRepository.findByNameIgnoreCase("Spring Boot")).thenReturn(Optional.of(springBootSkill));

        StudentProfileDto profileDto = studentProfileService.getProfileByUsernameOrEmail("student1");

        assertNotNull(profileDto);
        assertEquals(2, sampleProfile.getSkills().size());
        assertTrue(sampleProfile.getSkills().contains(javaSkill));
        assertTrue(sampleProfile.getSkills().contains(springBootSkill));
    }
}
