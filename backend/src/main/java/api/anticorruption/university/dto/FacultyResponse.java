package api.anticorruption.university.dto;

import api.anticorruption.university.Faculty;

import java.util.List;

/** Fakultet va uning kafedralari. */
public record FacultyResponse(
        Long id,
        String name,
        String code,
        boolean active,
        List<DepartmentResponse> departments
) {
    /** Faqat tranzaksiya ichida chaqirilishi kerak - kafedralar lazy yuklanadi. */
    public static FacultyResponse from(Faculty faculty) {
        return new FacultyResponse(
                faculty.getId(),
                faculty.getName(),
                faculty.getCode(),
                faculty.isActive(),
                faculty.getDepartments().stream().map(DepartmentResponse::from).toList());
    }

    /** Faol kafedralarni ko'rsatadigan variant - ochiq ro'yxatlar uchun. */
    public static FacultyResponse activeOnly(Faculty faculty) {
        return new FacultyResponse(
                faculty.getId(),
                faculty.getName(),
                faculty.getCode(),
                faculty.isActive(),
                faculty.getDepartments().stream()
                        .filter(department -> department.isActive())
                        .map(DepartmentResponse::from)
                        .toList());
    }
}
