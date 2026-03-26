package ie.tus.jeff.secondassessment.controller;

import tools.jackson.databind.ObjectMapper;
import ie.tus.jeff.secondassessment.exception.BusinessRuleException;
import ie.tus.jeff.secondassessment.exception.GlobalExceptionHandler;
import ie.tus.jeff.secondassessment.model.Department;
import ie.tus.jeff.secondassessment.service.DepartmentService;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link DepartmentController}.
 *
 * Uses Mockito + standalone MockMvc — no Spring context is loaded,
 * making these tests fast and isolated. The GlobalExceptionHandler is
 * wired in manually so HTTP error mappings are also exercised.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DepartmentController — Unit Tests")
class DepartmentControllerUnitTest {

    @Mock
    private DepartmentService departmentService;

    @InjectMocks
    private DepartmentController departmentController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private Department engineering;
    private Department marketing;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(departmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        engineering = new Department("Engineering", "Dublin");
        setId(engineering, 1L);

        marketing = new Department("Marketing", "Cork");
        setId(marketing, 2L);
    }

    // ── Reflection helper to set the private id field ─────────────────────────

    private void setId(Department dept, Long id) {
        try {
            var field = Department.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(dept, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /departments
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /departments")
    class GetAll {

        @Test
        @DisplayName("returns 200 and a list of all departments")
        void returnsAllDepartments() throws Exception {
            when(departmentService.findAll()).thenReturn(List.of(engineering, marketing));

            mockMvc.perform(get("/departments"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name", is("Engineering")))
                    .andExpect(jsonPath("$[0].location", is("Dublin")))
                    .andExpect(jsonPath("$[1].name", is("Marketing")));

            verify(departmentService, times(1)).findAll();
        }

        @Test
        @DisplayName("returns 200 and an empty list when no departments exist")
        void returnsEmptyListWhenNoDepartments() throws Exception {
            when(departmentService.findAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/departments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /departments/{id}
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /departments/{id}")
    class GetById {

        @Test
        @DisplayName("returns 200 and the department when it exists")
        void returnsDepartmentWhenFound() throws Exception {
            when(departmentService.findById(1L)).thenReturn(Optional.of(engineering));

            mockMvc.perform(get("/departments/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.name", is("Engineering")))
                    .andExpect(jsonPath("$.location", is("Dublin")));
        }

        @Test
        @DisplayName("returns 404 when the department does not exist")
        void returns404WhenNotFound() throws Exception {
            when(departmentService.findById(99L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/departments/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // POST /departments
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /departments")
    class Create {

        @Test
        @DisplayName("returns 201 and the created department on success")
        void createsAndReturns201() throws Exception {
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
        @DisplayName("returns 400 when name is blank")
        void returns400WhenNameIsBlank() throws Exception {
            Department invalid = new Department("", "Dublin");

            mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when request body is missing")
        void returns400WhenBodyMissing() throws Exception {
            mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 409 when a department with that name already exists")
        void returns409OnDuplicateName() throws Exception {
            Department payload = new Department("Engineering", "Dublin");
            when(departmentService.save(any(Department.class)))
                    .thenThrow(new BusinessRuleException(
                            "A department with the name 'Engineering' already exists"));

            mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", containsString("Engineering")));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DELETE /departments/{id}
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DELETE /departments/{id}")
    class Delete {

        @Test
        @DisplayName("returns 204 when the department is successfully deleted")
        void returns204OnSuccess() throws Exception {
            when(departmentService.deleteById(1L)).thenReturn(true);

            mockMvc.perform(delete("/departments/1"))
                    .andExpect(status().isNoContent());

            verify(departmentService, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("returns 404 when the department does not exist")
        void returns404WhenNotFound() throws Exception {
            when(departmentService.deleteById(99L)).thenReturn(false);

            mockMvc.perform(delete("/departments/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status", is(404)))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }

        @Test
        @DisplayName("returns 409 when the department still has employees")
        void returns409WhenDepartmentHasEmployees() throws Exception {
            when(departmentService.deleteById(1L))
                    .thenThrow(new BusinessRuleException(
                            "Cannot delete department with id: 1 — it still has 3 employee(s)"));

            mockMvc.perform(delete("/departments/1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status", is(409)))
                    .andExpect(jsonPath("$.message", containsString("employee(s)")));
        }
    }
}
