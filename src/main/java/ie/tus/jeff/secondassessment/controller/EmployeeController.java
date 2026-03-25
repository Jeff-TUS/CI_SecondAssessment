package ie.tus.jeff.secondassessment.controller;

import ie.tus.jeff.secondassessment.exception.ResourceNotFoundException;
import ie.tus.jeff.secondassessment.model.Employee;
import ie.tus.jeff.secondassessment.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departments/{deptId}/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // GET /departments/{deptId}/employees
    @GetMapping
    public ResponseEntity<List<Employee>> getAll(@PathVariable Long deptId) {
        return employeeService.findAllByDepartment(deptId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + deptId));
    }

    // GET /departments/{deptId}/employees/{empId}
    @GetMapping("/{empId}")
    public ResponseEntity<Employee> getById(@PathVariable Long deptId,
                                            @PathVariable Long empId) {
        return employeeService.findByIdAndDepartment(empId, deptId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + empId +
                                " in department: " + deptId));
    }

    // POST /departments/{deptId}/employees
    @PostMapping
    public ResponseEntity<Employee> create(@PathVariable Long deptId,
                                           @Valid @RequestBody Employee employee) {
        return employeeService.save(deptId, employee)
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(saved))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + deptId));
    }

    // DELETE /departments/{deptId}/employees/{empId}
    @DeleteMapping("/{empId}")
    public ResponseEntity<Void> delete(@PathVariable Long deptId,
                                       @PathVariable Long empId) {
        if (!employeeService.deleteById(empId, deptId)) {
            throw new ResourceNotFoundException(
                    "Employee not found with id: " + empId +
                            " in department: " + deptId);
        }
        return ResponseEntity.noContent().build();
    }
}