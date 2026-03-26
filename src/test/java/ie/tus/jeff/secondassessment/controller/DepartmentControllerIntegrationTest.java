package ie.tus.jeff.secondassessment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ie.tus.jeff.secondassessment.exception.BusinessRuleException;
import ie.tus.jeff.secondassessment.model.Department;
import ie.tus.jeff.secondassessment.service.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link DepartmentController}.
 *
 * Uses @WebMvcTest to load the full Spring MVC slice — dispatcher servlet,
 * filters, Jackson serialisation and the GlobalExceptionHandler are all
 * bootstrapped, but the database layer is excluded. The service is replaced
 * with a Mockito bean so behaviour can be controlled per-test.
 */
@WebMvcTest(DepartmentController.class)
@DisplayName("DepartmentController — Integration Tests (@WebMvcTest)")
class DepartmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DepartmentService departmentService;

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private Department engineering;
    private Department marketing;

    @BeforeEach
    void setUp() throws Exception {
        engineering = new Department("Engineering", "Dublin");
        setId(engineering, 1L);

        marketing = new Department("Marketing", "Cork");
        setId(marketing, 2L);
    }

    private void setId(Department dept, Long id) {
        try {
            var f = Department.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(dept, id);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /departments
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /departments")
    class GetAll {

        @Test
        @DisplayName("Spring MVC slice returns 200 with correct JSON structure")
        void returns200WithJsonArray() throws Exception {
            when(departmentService.findAll()).thenReturn(List.of(engineering, marketing));

            mockMvc.perform(get("/departments").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("Engineering")))
                    .andExpect(jsonPath("$[0].location", is("Dublin")))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].name", is("Marketing")));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /departments/{id}
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /departments/{id}")
    class GetById {

        @Test
        @DisplayName("returns 200 with correct Content-Type and body")
        void returns200WithDepartment() throws Exception {
            when(departmentService.findById(1L)).thenReturn(Optional.of(engineering));

            mockMvc.perform(get("/departments/1").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Engineering")));
        }

        @Test
        @DisplayName("GlobalExceptionHandler maps ResourceNotFoundException to 404 JSON body")
        void globalHandlerProduces404Json() throws Exception {
            when(departmentService.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/departments/99").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.error", is("Not Found")))
                    .andExpect(jsonPath("$.message", containsString("99")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /departments
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /departments")
    class Create {

        @Test
        @DisplayName("returns 201 with Location-style body and correct JSON")
        void returns201WithCreatedDepartment() throws Exception {
            Department payload = new Department("Engineering", "Dublin");
            when(departmentService.save(any(Department.class)))
                    .thenReturn(Optional.of(engineering));

            mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Engineering")));
        }

        @Test
        @DisplayName("Spring validation rejects blank name with 400")
        void validationRejectsBlankName() throws Exception {
            Department invalid = new Department("", "Dublin");

            mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            // Service must never be called when bean validation fails
            verify(departmentService, never()).save(any());
        }

        @Test
        @DisplayName("GlobalExceptionHandler maps BusinessRuleException to 409 JSON body")
        void globalHandlerProduces409Json() throws Exception {
            Department payload = new Department("Engineering", "Dublin");
            when(departmentService.save(any(Department.class)))
                    .thenThrow(new BusinessRuleException(
                            "A department with the name 'Engineering' already exists"));

            mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.error", is("Conflict")))
                    .andExpect(jsonPath("$.message", containsString("Engineering")))
                    .andExpect(jsonPath("$.timestamp", notNullValue()));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DELETE /departments/{id}
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /departments/{id}")
    class Delete {

        @Test
        @DisplayName("returns 204 No Content on successful deletion")
        void returns204OnSuccess() throws Exception {
            when(departmentService.deleteById(1L)).thenReturn(true);

            mockMvc.perform(delete("/departments/1"))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("GlobalExceptionHandler maps ResourceNotFoundException to 404 on missing dept")
        void returns404WhenDeptNotFound() throws Exception {
            when(departmentService.deleteById(99L)).thenReturn(false);

            mockMvc.perform(delete("/departments/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)));
        }

        @Test
        @DisplayName("GlobalExceptionHandler maps BusinessRuleException to 409 when dept has employees")
        void returns409WhenDeptHasEmployees() throws Exception {
            when(departmentService.deleteById(1L))
                    .thenThrow(new BusinessRuleException(
                            "Cannot delete department with id: 1 — it still has 2 employee(s)"));

            mockMvc.perform(delete("/departments/1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", containsString("employee(s)")));
        }
    }
}
