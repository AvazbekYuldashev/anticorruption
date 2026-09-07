package api.anticorruption.university.dto;

import api.anticorruption.university.Department;

/** Kafedra haqidagi ma'lumot. */
public record DepartmentResponse(
        Long id,
        Long facultyId,
        String name,
        String code,
        boolean active
) {
    public static DepartmentResponse from(Department department) {
        return new DepartmentResponse(
                department.getId(),
                department.getFaculty().getId(),
                department.getName(),
                department.getCode(),
                department.isActive());
    }
}
