package ie.tus.jeff.secondassessment.service;

import ie.tus.jeff.secondassessment.exception.BusinessRuleException;
import ie.tus.jeff.secondassessment.exception.ResourceNotFoundException;
import ie.tus.jeff.secondassessment.model.Department;
import ie.tus.jeff.secondassessment.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public Department findById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Department not found with id: " + id));
    }

    public Department save(Department department) {
        if (departmentRepository.existsByNameIgnoreCase(department.getName())) {
            throw new BusinessRuleException(
                    "A department with the name '" + department.getName() + "' already exists");
        }
        return departmentRepository.save(department);
    }

    public void deleteById(Long id) {
        Department department = findById(id);
        if (!department.getEmployees().isEmpty()) {
            throw new BusinessRuleException(
                    "Cannot delete department with id: " + id +
                            " — it still has " + department.getEmployees().size() + " employee(s)");
        }
        departmentRepository.deleteById(id);
    }
}