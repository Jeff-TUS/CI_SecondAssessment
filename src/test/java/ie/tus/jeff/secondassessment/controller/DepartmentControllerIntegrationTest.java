package ie.tus.jeff.secondassessment.controller;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import tools.jackson.databind.ObjectMapper;
import ie.tus.jeff.secondassessment.exception.BusinessRuleException;
import ie.tus.jeff.secondassessment.model.Department;
import ie.tus.jeff.secondassessment.service.DepartmentService;
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
 * Response bodies are deserialised into POJOs and asserted with AssertJ.
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

    // ── Deserialisation helpers ───────────────────────────────────────────────

    private Department parseDepartment(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Department.class);
    }

    private Department[] parseDepartmentArray(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), Department[].class);
    }

    private ErrorResponse parseError(MvcResult result) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);
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

            MvcResult result = mockMvc.perform(get("/departments").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andReturn();

            Department[] departments = parseDepartmentArray(result);
            assertThat(departments).hasSize(2);
            assertThat(departments[0].getId()).isEqualTo(1L);
            assertThat(departments[0].getName()).isEqualTo("Engineering");
            assertThat(departments[0].getLocation()).isEqualTo("Dublin");
            assertThat(departments[1].getId()).isEqualTo(2L);
            assertThat(departments[1].getName()).isEqualTo("Marketing");
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

            MvcResult result = mockMvc.perform(get("/departments/1").accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andReturn();

            Department dept = parseDepartment(result);
            assertThat(dept.getId()).isEqualTo(1L);
            assertThat(dept.getName()).isEqualTo("Engineering");
        }

        @Test
        @DisplayName("GlobalExceptionHandler maps ResourceNotFoundException to 404 JSON body")
        void globalHandlerProduces404Json() throws Exception {
            when(departmentService.findById(99L)).thenReturn(Optional.empty());

            MvcResult result = mockMvc.perform(get("/departments/99").accept(MediaType.APPLICATION_JSON))
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

            MvcResult result = mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isCreated())
                    .andReturn();

            Department dept = parseDepartment(result);
            assertThat(dept.getId()).isEqualTo(1L);
            assertThat(dept.getName()).isEqualTo("Engineering");
        }

        @Test
        @DisplayName("Spring validation rejects blank name with 400")
        void validationRejectsBlankName() throws Exception {
            Department invalid = new Department("", "Dublin");

            mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest());

            verify(departmentService, never()).save(any());
        }

        @Test
        @DisplayName("GlobalExceptionHandler maps BusinessRuleException to 409 JSON body")
        void globalHandlerProduces409Json() throws Exception {
            Department payload = new Department("Engineering", "Dublin");
            when(departmentService.save(any(Department.class)))
                    .thenThrow(new BusinessRuleException(
                            "A department with the name 'Engineering' already exists"));

            MvcResult result = mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isConflict())
                    .andReturn();

            ErrorResponse error = parseError(result);
            assertThat(error.getStatus()).isEqualTo(409);
            assertThat(error.getError()).isEqualTo("Conflict");
            assertThat(error.getMessage()).contains("Engineering");
            assertThat(error.getTimestamp()).isNotNull();
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

            MvcResult result = mockMvc.perform(delete("/departments/99"))
                    .andExpect(status().isNotFound())
                    .andReturn();

            ErrorResponse error = parseError(result);
            assertThat(error.getStatus()).isEqualTo(404);
        }

        @Test
        @DisplayName("GlobalExceptionHandler maps BusinessRuleException to 409 when dept has employees")
        void returns409WhenDeptHasEmployees() throws Exception {
            when(departmentService.deleteById(1L))
                    .thenThrow(new BusinessRuleException(
                            "Cannot delete department with id: 1 — it still has 2 employee(s)"));

            MvcResult result = mockMvc.perform(delete("/departments/1"))
                    .andExpect(status().isConflict())
                    .andReturn();

            ErrorResponse error = parseError(result);
            assertThat(error.getStatus()).isEqualTo(409);
            assertThat(error.getMessage()).contains("employee(s)");
        }
    }
}
