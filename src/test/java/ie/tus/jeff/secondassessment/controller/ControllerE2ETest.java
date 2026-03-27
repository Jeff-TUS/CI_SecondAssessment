package ie.tus.jeff.secondassessment.controller;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.ObjectMapper;
import ie.tus.jeff.secondassessment.model.Department;
import ie.tus.jeff.secondassessment.model.Employee;
import ie.tus.jeff.secondassessment.repository.DepartmentRepository;
import ie.tus.jeff.secondassessment.repository.EmployeeRepository;
import ie.tus.jeff.secondassessment.util.ErrorResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests for the REST API.
 *
 * Boots the FULL Spring application context with an in-memory H2 database.
 * Every layer — controller → service → repository → database — participates.
 * Each test is wrapped in a transaction that rolls back after the test,
 * keeping the database clean without needing manual teardown.
 * Response bodies are deserialised into POJOs and asserted with AssertJ.
 *
 * These are deliberately fewer in number than the unit and integration tests
 * above, in keeping with the test-pyramid principle.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Controller Layer — End-to-End Tests (Full Spring Context + H2)")
class ControllerE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EntityManager entityManager;

    // ── Fixtures persisted to H2 before each test ─────────────────────────────

    private Department engineering;
    private Department marketing;
    private Employee alice;

    @BeforeEach
    void seed() {
        engineering = departmentRepository.save(new Department("Engineering", "Dublin"));
        marketing   = departmentRepository.save(new Department("Marketing", "Cork"));
        alice       = employeeRepository.save(
                new Employee("Alice Smith", "alice@example.com", "Developer", engineering));

        // Flush writes to the DB, then clear the first-level (session) cache.
        // Without this, DepartmentService.deleteById() fetches the 'engineering'
        // entity from Hibernate's session cache — which still has an empty
        // employees list — and incorrectly allows the delete to proceed.
        entityManager.flush();
        entityManager.clear();
    }

    // ── Deserialisation helpers ───────────────────────────────────────────────

    private Department parseDepartment(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Department.class);
    }

    private Department[] parseDepartmentArray(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Department[].class);
    }

    private Employee parseEmployee(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Employee.class);
    }

    private Employee[] parseEmployeeArray(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Employee[].class);
    }

    private ErrorResponse parseError(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Department E2E scenarios
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Department E2E")
    class DepartmentE2E {

        @Test
        @DisplayName("GET /departments returns all seeded departments from the database")
        void getAllDepartments() throws Exception {
            MvcResult result = mockMvc.perform(get("/departments").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            Department[] departments = parseDepartmentArray(result);
            assertThat(departments).hasSizeGreaterThanOrEqualTo(2);
            assertThat(departments).extracting(Department::getName)
                    .contains("Engineering", "Marketing");
        }

        @Test
        @DisplayName("GET /departments/{id} returns the correct department from the database")
        void getDepartmentById() throws Exception {
            MvcResult result = mockMvc.perform(get("/departments/" + engineering.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            Department dept = parseDepartment(result);
            assertThat(dept.getName()).isEqualTo("Engineering");
            assertThat(dept.getLocation()).isEqualTo("Dublin");
        }

        @Test
        @DisplayName("GET /departments/{id} returns 404 for a non-existent id")
        void getDepartmentByIdNotFound() throws Exception {
            MvcResult result = mockMvc.perform(get("/departments/999999").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ErrorResponse error = parseError(result);
            assertThat(error.getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("POST /departments creates a new department and persists it")
        void createDepartment() throws Exception {
            Department payload = new Department("Finance", "Limerick");

            MvcResult result = mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isCreated())
                    .andReturn();

            Department created = parseDepartment(result);
            assertThat(created.getId()).isNotNull();
            assertThat(created.getName()).isEqualTo("Finance");
            assertThat(created.getLocation()).isEqualTo("Limerick");

            // Verify persisted — fetch back by the returned id
            MvcResult fetchResult = mockMvc.perform(get("/departments/" + created.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(parseDepartment(fetchResult).getName()).isEqualTo("Finance");
        }

        @Test
        @DisplayName("POST /departments returns 409 when department name already exists (case-insensitive)")
        void createDuplicateDepartmentReturns409() throws Exception {
            Department duplicate = new Department("engineering", "Galway");

            MvcResult result = mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicate)))
                    .andExpect(status().isConflict())
                    .andReturn();

            ErrorResponse error = parseError(result);
            assertThat(error.getStatus()).isEqualTo(409);
        }

        @Test
        @DisplayName("DELETE /departments/{id} removes an empty department")
        void deleteEmptyDepartment() throws Exception {
            mockMvc.perform(delete("/departments/" + marketing.getId()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/departments/" + marketing.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /departments/{id} returns 409 when department has employees")
        void deleteDepartmentWithEmployeesReturns409() throws Exception {
            MvcResult result = mockMvc.perform(delete("/departments/" + engineering.getId()))
                    .andExpect(status().isConflict())
                    .andReturn();

            ErrorResponse error = parseError(result);
            assertThat(error.getStatus()).isEqualTo(409);
            assertThat(error.getMessage()).contains("employee(s)");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Employee E2E scenarios
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Employee E2E")
    class EmployeeE2E {

        @Test
        @DisplayName("GET /departments/{deptId}/employees returns all employees in the department")
        void getAllEmployeesInDepartment() throws Exception {
            MvcResult result = mockMvc.perform(get("/departments/" + engineering.getId() + "/employees")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            Employee[] employees = parseEmployeeArray(result);
            assertThat(employees).hasSize(1);
            assertThat(employees[0].getName()).isEqualTo("Alice Smith");
            assertThat(employees[0].getEmail()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("GET /departments/{deptId}/employees returns 404 for non-existent department")
        void getAllEmployeesUnknownDept() throws Exception {
            mockMvc.perform(get("/departments/999999/employees")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("GET /departments/{deptId}/employees/{empId} returns the correct employee")
        void getEmployeeById() throws Exception {
            MvcResult result = mockMvc.perform(get("/departments/" + engineering.getId()
                            + "/employees/" + alice.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            Employee employee = parseEmployee(result);
            assertThat(employee.getId()).isEqualTo(alice.getId());
            assertThat(employee.getName()).isEqualTo("Alice Smith");
        }

        @Test
        @DisplayName("POST /departments/{deptId}/employees creates and persists a new employee")
        void createEmployee() throws Exception {
            Employee payload = new Employee("Bob Jones", "bob@example.com", "QA Engineer", null);

            MvcResult result = mockMvc.perform(
                            post("/departments/" + engineering.getId() + "/employees")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isCreated())
                    .andReturn();

            Employee created = parseEmployee(result);
            assertThat(created.getId()).isNotNull();
            assertThat(created.getName()).isEqualTo("Bob Jones");
            assertThat(created.getEmail()).isEqualTo("bob@example.com");

            // Verify persisted — department now has 2 employees
            MvcResult listResult = mockMvc.perform(get("/departments/" + engineering.getId() + "/employees")
                            .accept(MediaType.APPLICATION_JSON))
                    .andReturn();

            assertThat(parseEmployeeArray(listResult)).hasSize(2);
        }

        @Test
        @DisplayName("DELETE /departments/{deptId}/employees/{empId} removes the employee")
        void deleteEmployee() throws Exception {
            mockMvc.perform(delete("/departments/" + engineering.getId()
                            + "/employees/" + alice.getId()))
                    .andExpect(status().isNoContent());

            MvcResult listResult = mockMvc.perform(get("/departments/" + engineering.getId() + "/employees")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            assertThat(parseEmployeeArray(listResult)).isEmpty();
        }
    }
}
