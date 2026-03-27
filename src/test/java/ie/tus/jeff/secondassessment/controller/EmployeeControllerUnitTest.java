package ie.tus.jeff.secondassessment.controller;

import tools.jackson.databind.ObjectMapper;
import ie.tus.jeff.secondassessment.exception.GlobalExceptionHandler;
import ie.tus.jeff.secondassessment.model.Department;
import ie.tus.jeff.secondassessment.model.Employee;
import ie.tus.jeff.secondassessment.service.EmployeeService;
import ie.tus.jeff.secondassessment.util.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link EmployeeController}.
 *
 * Uses Mockito + standalone MockMvc — no Spring context is loaded.
 * The GlobalExceptionHandler is wired in manually.
 * Response bodies are deserialised into POJOs and asserted with AssertJ.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeController — Unit Tests")
class EmployeeControllerUnitTest {

    @Mock
    private EmployeeService employeeService;

    @InjectMocks
    private EmployeeController employeeController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private Department engineering;
    private Employee alice;
    private Employee bob;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(employeeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        engineering = new Department("Engineering", "Dublin");
        setDeptId(engineering, 1L);

        alice = new Employee("Alice Smith", "alice@example.com", "Developer", engineering);
        setEmpId(alice, 10L);

        bob = new Employee("Bob Jones", "bob@example.com", "QA Engineer", engineering);
        setEmpId(bob, 11L);
    }

    // ── Reflection helpers ────────────────────────────────────────────────────

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
        @DisplayName("returns 200 and a list of employees for a valid department")
        void returnsAllEmployeesForDepartment() throws Exception {
            when(employeeService.findAllByDepartment(1L))
                    .thenReturn(Optional.of(List.of(alice, bob)));

            MvcResult result = mockMvc.perform(get("/departments/1/employees"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            Employee[] employees = parseEmployeeArray(result);
            assertThat(employees).hasSize(2);
            assertThat(employees[0].getName()).isEqualTo("Alice Smith");
            assertThat(employees[0].getEmail()).isEqualTo("alice@example.com");
            assertThat(employees[0].getJobTitle()).isEqualTo("Developer");
            assertThat(employees[1].getName()).isEqualTo("Bob Jones");

            verify(employeeService, times(1)).findAllByDepartment(1L);
        }

        @Test
        @DisplayName("returns 200 and an empty list when department has no employees")
        void returnsEmptyListWhenNoEmployees() throws Exception {
            when(employeeService.findAllByDepartment(1L))
                    .thenReturn(Optional.of(Collections.emptyList()));

            MvcResult result = mockMvc.perform(get("/departments/1/employees"))
                    .andExpect(status().isOk())
                    .andReturn();

            Employee[] employees = parseEmployeeArray(result);
            assertThat(employees).isEmpty();
        }

        @Test
        @DisplayName("returns 404 when the department does not exist")
        void returns404WhenDepartmentNotFound() throws Exception {
            when(employeeService.findAllByDepartment(99L)).thenReturn(Optional.empty());

            MvcResult result = mockMvc.perform(get("/departments/99/employees"))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ErrorResponse error = parseError(result);
            assertThat(error.getStatus()).isEqualTo(404);
            assertThat(error.getMessage()).contains("99");
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /departments/{deptId}/employees/{empId}
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /departments/{deptId}/employees/{empId}")
    class GetById {

        @Test
        @DisplayName("returns 200 and the employee when found in the department")
        void returnsEmployeeWhenFound() throws Exception {
            when(employeeService.findByIdAndDepartment(10L, 1L))
                    .thenReturn(Optional.of(alice));

            MvcResult result = mockMvc.perform(get("/departments/1/employees/10"))
                    .andExpect(status().isOk())
                    .andReturn();

            Employee employee = parseEmployee(result);
            assertThat(employee.getId()).isEqualTo(10L);
            assertThat(employee.getName()).isEqualTo("Alice Smith");
            assertThat(employee.getEmail()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("returns 404 when the employee does not exist in the department")
        void returns404WhenNotFound() throws Exception {
            when(employeeService.findByIdAndDepartment(99L, 1L))
                    .thenReturn(Optional.empty());

            MvcResult result = mockMvc.perform(get("/departments/1/employees/99"))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ErrorResponse error = parseError(result);
            assertThat(error.getStatus()).isEqualTo(404);
            assertThat(error.getMessage()).contains("99");
        }

        @Test
        @DisplayName("returns 404 when employee exists but belongs to a different department")
        void returns404WhenEmployeeInWrongDepartment() throws Exception {
            when(employeeService.findByIdAndDepartment(10L, 2L))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/departments/2/employees/10"))
                    .andExpect(status().isNotFound());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /departments/{deptId}/employees
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /departments/{deptId}/employees")
    class Create {

        @Test
        @DisplayName("returns 201 and the created employee on success")
        void createsAndReturns201() throws Exception {
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
            assertThat(employee.getEmail()).isEqualTo("alice@example.com");
        }

        @Test
        @DisplayName("returns 404 when the target department does not exist")
        void returns404WhenDepartmentNotFound() throws Exception {
            Employee payload = new Employee("Alice Smith", "alice@example.com", "Developer", null);
            when(employeeService.save(eq(99L), any(Employee.class)))
                    .thenReturn(Optional.empty());

            MvcResult result = mockMvc.perform(post("/departments/99/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ErrorResponse error = parseError(result);
            assertThat(error.getMessage()).contains("99");
        }

        @Test
        @DisplayName("returns 400 when name is blank")
        void returns400WhenNameBlank() throws Exception {
            Employee invalid = new Employee("", "alice@example.com", "Developer", null);

            mockMvc.perform(post("/departments/1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when email is blank")
        void returns400WhenEmailBlank() throws Exception {
            Employee invalid = new Employee("Alice", "", "Developer", null);

            mockMvc.perform(post("/departments/1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when email is not a valid format")
        void returns400WhenEmailInvalid() throws Exception {
            Employee invalid = new Employee("Alice", "not-an-email", "Developer", null);

            mockMvc.perform(post("/departments/1/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when request body is missing")
        void returns400WhenBodyMissing() throws Exception {
            mockMvc.perform(post("/departments/1/employees")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DELETE /departments/{deptId}/employees/{empId}
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /departments/{deptId}/employees/{empId}")
    class Delete {

        @Test
        @DisplayName("returns 204 when the employee is successfully deleted")
        void returns204OnSuccess() throws Exception {
            when(employeeService.deleteById(10L, 1L)).thenReturn(true);

            mockMvc.perform(delete("/departments/1/employees/10"))
                    .andExpect(status().isNoContent());

            verify(employeeService, times(1)).deleteById(10L, 1L);
        }

        @Test
        @DisplayName("returns 404 when the employee does not exist in the department")
        void returns404WhenNotFound() throws Exception {
            when(employeeService.deleteById(99L, 1L)).thenReturn(false);

            MvcResult result = mockMvc.perform(delete("/departments/1/employees/99"))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ErrorResponse error = parseError(result);
            assertThat(error.getStatus()).isEqualTo(404);
            assertThat(error.getMessage()).contains("99");
        }
    }
}
