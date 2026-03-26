package ie.tus.jeff.secondassessment.service;

import ie.tus.jeff.secondassessment.exception.BusinessRuleException;
import ie.tus.jeff.secondassessment.model.Department;
import ie.tus.jeff.secondassessment.repository.DepartmentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    public Optional<Department> findById(Long id) {
        return departmentRepository.findById(id);
    }

    public Optional<Department> save(Department department) {
        if (departmentRepository.existsByNameIgnoreCase(department.getName())) {
            throw new BusinessRuleException(
                    "A department with the name '" + department.getName() + "' already exists");
        }
        return Optional.of(departmentRepository.save(department));
    }

    public boolean deleteById(Long id) {
        Optional<Department> department = findById(id);
        if (department.isEmpty()) {
            return false;
        }
        if (!department.get().getEmployees().isEmpty()) {
            System.err.println("Found employees - cannot delete department with id: " + id);
            throw new BusinessRuleException(
                    "Cannot delete department with id: " + id +
                            " — it still has " + department.get().getEmployees().size() + " employee(s)");
        }
        departmentRepository.deleteById(id);
        return true;
    }
}