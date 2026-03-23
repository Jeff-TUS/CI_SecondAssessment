package ie.tus.jeff.secondassessment.service;

import ie.tus.jeff.secondassessment.exception.ResourceNotFoundException;
import ie.tus.jeff.secondassessment.model.Department;
import ie.tus.jeff.secondassessment.model.Employee;
import ie.tus.jeff.secondassessment.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentService departmentService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentService departmentService) {
        this.employeeRepository = employeeRepository;
        this.departmentService = departmentService;
    }

    public List<Employee> findAllByDepartment(Long departmentId) {
        departmentService.findById(departmentId);
        return employeeRepository.findByDepartmentId(departmentId);
    }

    public Employee findByIdAndDepartment(Long empId, Long departmentId) {
        return employeeRepository.findByIdAndDepartmentId(empId, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee not found with id: " + empId +
                                " in department: " + departmentId));
    }

    public Employee save(Long departmentId, Employee employee) {
        Department department = departmentService.findById(departmentId);
        employee.setDepartment(department);
        return employeeRepository.save(employee);
    }

    public void deleteById(Long empId, Long departmentId) {
        Employee employee = findByIdAndDepartment(empId, departmentId);
        employeeRepository.delete(employee);
    }
}