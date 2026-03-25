package ie.tus.jeff.secondassessment.service;

import ie.tus.jeff.secondassessment.model.Department;
import ie.tus.jeff.secondassessment.model.Employee;
import ie.tus.jeff.secondassessment.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentService departmentService;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentService departmentService) {
        this.employeeRepository = employeeRepository;
        this.departmentService = departmentService;
    }

    public Optional<List<Employee>> findAllByDepartment(Long departmentId) {
        // Return empty Optional if the department does not exist
        return departmentService.findById(departmentId)
                .map(dept -> employeeRepository.findByDepartmentId(departmentId));
    }

    public Optional<Employee> findByIdAndDepartment(Long empId, Long departmentId) {
        return employeeRepository.findByIdAndDepartmentId(empId, departmentId);
    }

    public Optional<Employee> save(Long departmentId, Employee employee) {
        Optional<Department> department = departmentService.findById(departmentId);
        if (department.isEmpty()) {
            return Optional.empty();
        }
        employee.setDepartment(department.get());
        return Optional.of(employeeRepository.save(employee));
    }

    public boolean deleteById(Long empId, Long departmentId) {
        Optional<Employee> employee = findByIdAndDepartment(empId, departmentId);
        if (employee.isEmpty()) {
            return false;
        }
        employeeRepository.delete(employee.get());
        return true;
    }
}