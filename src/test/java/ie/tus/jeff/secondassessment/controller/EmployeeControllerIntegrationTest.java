package ie.tus.jeff.secondassessment.controller;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import ie.tus.jeff.secondassessment.model.Department;
import ie.tus.jeff.secondassessment.model.Employee;
import ie.tus.jeff.secondassessment.service.EmployeeService;
import ie.tus.jeff.secondassessment.util.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link EmployeeController}.
 *
 * Uses @WebMvcTest to load the full Spring MVC slice. The service layer
 * is replaced with a Mockito bean. These tests verify that routing,
 * content negotiation, validation, and exception handling all integrate
 * correctly within the MVC layer.
 * Response bodies are deserialised into POJOs and asserted with AssertJ.
 */
@WebMvcTest(EmployeeController.class)
@DisplayName("EmployeeController — Integration Tests (@WebMvcTest)")
class EmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeService employeeService;

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private Department engineering;
    private Employee alice;
    private Employee bob;

    @BeforeEach
    void setUp() throws Exception {
        engineering = new Department("Engineering", "Dublin");
        setDeptId(engineering, 1L);

        alice = new Employee("Alice Smith", "alice@example.com", "Developer", engineering);
        setEmpId(alice, 10L);

        bob = new Employee("Bob Jones", "bob@example.com", "QA Engineer", engineering);
        setEmpId(bob, 11L);
    }

    private void setDeptId(Department dept, Long id) {
        try {
            var f = Department.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(dept, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private void setEmpId(Employee emp, Long id) {
        try {
            var f = Employee.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(emp, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ── Deserialisation helpers ───────────────────────────────────────────────

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
    // GET /departments/{deptId}/employees
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /departments/{deptId}/employees")
    class GetAll {

        @Test
        @DisplayName("returns 200 with JSON array of employees for existing department")
        void returns200WithEmployeeList() throws Exception {
            when(employeeService.findAllByDepartment(1L))
                    .thenReturn(Optional.of(List.of(alice, bob)));

            MvcResult result = mockMvc.perform(get("/departments/1/employees").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andReturn();

            Employee[] employees = parseEmployeeArray(result);
            assertThat(employees).hasSize(2);
            assertThat(employees[0].getId()).isEqualTo(10L);
            assertThat(employees[0].getName()).isEqualTo("Alice Smith");
            assertThat(employees[0].getEmail()).isEqualTo("alice@example.com");
            assertThat(employees[0].getJobTitle()).isEqualTo("Developer");
            assertThat(employees[1].getId()).isEqualTo(11L);
        }

        @Test
        @DisplayName("department field is not serialised (JsonIgnore prevents infinite recursion)")
        void departmentFieldNotInJson() throws Exception {
            when(employeeService.findAllByDepartment(1L))
                    .thenReturn(Optional.of(List.of(alice)));

            MvcResult result = mockMvc.perform(get("/departments/1/employees").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            // Deserialise and confirm the department field is null (not serialised)
            Employee[] employees = parseEmployeeArray(result);
            assertThat(employees[0].getDepartment()).isNull();
        }

        @Test
        @DisplayName("GlobalExceptionHandler produces 404 JSON when department not found")
        void returns404WhenDeptMissing() throws Exception {
            when(employeeService.findAllByDepartment(99L)).thenReturn(Optional.empty());

            MvcResult result = mockMvc.perform(get("/departments/99/employees").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andReturn();

            ErrorResponse error = parseError(result);
            assertThat(error.getStatus()).isEqualTo(404);
            assertThat(error.getError()).isEqualTo("Not Found");
            assertThat(error.getMessage()).contains("99");
            assertThat(error.getTimestamp()).isNotNull();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /departments/{deptId}/employees/{empId}
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /departments/{deptId}/employees/{empId}")
    class GetById {

        @Test
        @DisplayName("returns 200 with the correct employee JSON")
        void returns200WithEmployee() throws Exception {
            when(employeeService.findByIdAndDepartment(10L, 1L))
                    .thenReturn(Optional.of(alice));

            MvcResult result = mockMvc.perform(get("/departments/1/employees/10").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            Employee employee = parseEmployee(result);
            assertThat(employee.getId()).isEqualTo(10L);
            assertThat(employee.getName()).isEqualTo("Alice Smith");
            assertThat(employee.getEmail()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("GlobalExceptionHandler produces 404 when employee not found in department")
        void returns404WhenNotFound() throws Exception {
            when(employeeService.findByIdAndDepartment(99L, 1L)).thenReturn(Optional.empty());

            MvcResult result = mockMvc.perform(get("/departments/1/employees/99").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ErrorResponse error = parseError(result);
            assertThat(error.getStatus()).isEqualTo(404);
            assertThat(error.getTimestamp()).isNotNull();
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /departments/{deptId}/employees
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /departments/{deptId}/employees")
    class Create {

        @Test
        @DisplayName("returns 201 with created employee JSON")
        void returns201WithCreatedEmployee() throws Exception {
            Employee payload = new Employee("Alice Smith", "alice@example.com", "Developer", null);
            when(employeeService.save(eq(1L), any(Employee.class)))
                    .thenReturn(Optional.of(alice));

            MvcResult result = mockMvc.perform(post("/departments/1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isCreated())
                    .andReturn();

            Employee employee = parseEmployee(result);
            assertThat(employee.getId()).isEqualTo(10L);
            assertThat(employee.getName()).isEqualTo("Alice Smith");
        }

        @Test
        @DisplayName("Spring validation rejects blank name — service is never called")
        void validationRejectsBlankName() throws Exception {
            Employee invalid = new Employee("", "alice@example.com", "Developer", null);

            mockMvc.perform(post("/departments/1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(employeeService, never()).save(any(), any());
        }

        @Test
        @DisplayName("Spring validation rejects invalid email format — service is never called")
        void validationRejectsInvalidEmail() throws Exception {
            Employee invalid = new Employee("Alice", "not-an-email", "Developer", null);

            mockMvc.perform(post("/departments/1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(employeeService, never()).save(any(), any());
        }

        @Test
        @DisplayName("returns 404 when department does not exist")
        void returns404WhenDeptNotFound() throws Exception {
            Employee payload = new Employee("Alice Smith", "alice@example.com", "Developer", null);
            when(employeeService.save(eq(99L), any(Employee.class))).thenReturn(Optional.empty());

            MvcResult result = mockMvc.perform(post("/departments/99/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ErrorResponse error = parseError(result);
            assertThat(error.getStatus()).isEqualTo(404);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DELETE /departments/{deptId}/employees/{empId}
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /departments/{deptId}/employees/{empId}")
    class Delete {

        @Test
        @DisplayName("returns 204 No Content on successful deletion")
        void returns204OnSuccess() throws Exception {
            when(employeeService.deleteById(10L, 1L)).thenReturn(true);

            mockMvc.perform(delete("/departments/1/employees/10"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("GlobalExceptionHandler produces 404 JSON when employee not found")
        void returns404WhenNotFound() throws Exception {
            when(employeeService.deleteById(99L, 1L)).thenReturn(false);

            MvcResult result = mockMvc.perform(delete("/departments/1/employees/99"))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ErrorResponse error = parseError(result);
            assertThat(error.getStatus()).isEqualTo(404);
            assertThat(error.getTimestamp()).isNotNull();
        }
    }
}
