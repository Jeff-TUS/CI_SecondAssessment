package ie.tus.jeff.secondassessment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ie.tus.jeff.secondassessment.model.Department;
import ie.tus.jeff.secondassessment.model.Employee;
import ie.tus.jeff.secondassessment.repository.DepartmentRepository;
import ie.tus.jeff.secondassessment.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests for the REST API.
 *
 * Boots the FULL Spring application context with an in-memory H2 database.
 * Every layer — controller → service → repository → database — participates.
 * Each test is wrapped in a transaction that rolls back after the test,
 * keeping the database clean without needing manual teardown.
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
            mockMvc.perform(get("/departments").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                    .andExpect(jsonPath("$[*].name", hasItems("Engineering", "Marketing")));
        }

        @Test
        @DisplayName("GET /departments/{id} returns the correct department from the database")
        void getDepartmentById() throws Exception {
            mockMvc.perform(get("/departments/" + engineering.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Engineering")))
                    .andExpect(jsonPath("$.location", is("Dublin")));
        }

        @Test
        @DisplayName("GET /departments/{id} returns 404 for a non-existent id")
        void getDepartmentByIdNotFound() throws Exception {
            mockMvc.perform(get("/departments/999999").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }

        @Test
        @DisplayName("POST /departments creates a new department and persists it")
        void createDepartment() throws Exception {
            Department payload = new Department("Finance", "Limerick");

            String responseBody = mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is("Finance")))
                    .andExpect(jsonPath("$.location", is("Limerick")))
                    .andReturn().getResponse().getContentAsString();

            // Verify persisted — fetch back by the returned id
            Long createdId = objectMapper.readTree(responseBody).get("id").asLong();
            mockMvc.perform(get("/departments/" + createdId).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name", is("Finance")));
        }

        @Test
        @DisplayName("POST /departments returns 409 when department name already exists (case-insensitive)")
        void createDuplicateDepartmentReturns409() throws Exception {
            Department duplicate = new Department("engineering", "Galway"); // same name, different case

            mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicate)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)));
        }

        @Test
        @DisplayName("DELETE /departments/{id} removes an empty department")
        void deleteEmptyDepartment() throws Exception {
            // marketing has no employees in this test
            mockMvc.perform(delete("/departments/" + marketing.getId()))
                    .andExpect(status().isNoContent());

            // Verify it's gone
            mockMvc.perform(get("/departments/" + marketing.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("DELETE /departments/{id} returns 409 when department has employees")
        void deleteDepartmentWithEmployeesReturns409() throws Exception {
            // engineering has alice — deletion must be refused
            mockMvc.perform(delete("/departments/" + engineering.getId()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", containsString("employee(s)")));
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
            mockMvc.perform(get("/departments/" + engineering.getId() + "/employees")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name", is("Alice Smith")))
                    .andExpect(jsonPath("$[0].email", is("alice@example.com")));
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
            mockMvc.perform(get("/departments/" + engineering.getId() + "/employees/" + alice.getId())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(alice.getId().intValue())))
                    .andExpect(jsonPath("$.name", is("Alice Smith")));
        }

        @Test
        @DisplayName("POST /departments/{deptId}/employees creates and persists a new employee")
        void createEmployee() throws Exception {
            Employee payload = new Employee("Bob Jones", "bob@example.com", "QA Engineer", null);

            String responseBody = mockMvc.perform(
                            post("/departments/" + engineering.getId() + "/employees")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.name", is("Bob Jones")))
                    .andExpect(jsonPath("$.email", is("bob@example.com")))
                    .andReturn().getResponse().getContentAsString();

            // Verify persisted — department now has 2 employees
            mockMvc.perform(get("/departments/" + engineering.getId() + "/employees")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)));
        }

        @Test
        @DisplayName("DELETE /departments/{deptId}/employees/{empId} removes the employee")
        void deleteEmployee() throws Exception {
            mockMvc.perform(delete("/departments/" + engineering.getId()
                            + "/employees/" + alice.getId()))
                    .andExpect(status().isNoContent());

            // Verify gone — list should now be empty
            mockMvc.perform(get("/departments/" + engineering.getId() + "/employees")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
