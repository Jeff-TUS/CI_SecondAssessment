package ie.tus.jeff.secondassessment.repository;

import ie.tus.jeff.secondassessment.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // All employees belonging to a given department
    List<Employee> findByDepartmentId(Long departmentId);

    // One employee scoped to a specific department (prevents cross-department access)
    Optional<Employee> findByIdAndDepartmentId(Long id, Long departmentId);
}