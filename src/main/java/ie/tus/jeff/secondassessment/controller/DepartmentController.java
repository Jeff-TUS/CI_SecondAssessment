package ie.tus.jeff.secondassessment.controller;

import ie.tus.jeff.secondassessment.exception.ResourceNotFoundException;
import ie.tus.jeff.secondassessment.model.Department;
import ie.tus.jeff.secondassessment.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    // GET /departments
    @GetMapping
    public ResponseEntity<List<Department>> getAll() {
        return ResponseEntity.ok(departmentService.findAll());
    }

    // GET /departments/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Department> getById(@PathVariable Long id) {
        return departmentService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + id));
    }

    // POST /departments
    @PostMapping
    public ResponseEntity<Department> create(@Valid @RequestBody Department department) {
        return departmentService.save(department)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Could not create department"));
    }

    // DELETE /departments/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!departmentService.deleteById(id)) {
            throw new ResourceNotFoundException("Department not found with id: " + id);
        }
        return ResponseEntity.noContent().build();
    }
}