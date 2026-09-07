package api.anticorruption.content.dto;

import api.anticorruption.content.StaffMember;

/** Bo'lim xodimi haqidagi ma'lumot. */
public record StaffMemberResponse(
        Long id,
        String fullName,
        String position,
        String academicDegree,
        String biography,
        String phone,
        String email,
        String receptionHours,
        String photoUrl,
        int displayOrder,
        boolean active
) {
    public static StaffMemberResponse from(StaffMember member) {
        return new StaffMemberResponse(
                member.getId(),
                member.getFullName(),
                member.getPosition(),
                member.getAcademicDegree(),
                member.getBiography(),
                member.getPhone(),
                member.getEmail(),
                member.getReceptionHours(),
                MediaUrls.of(member.getPhoto()),
                member.getDisplayOrder(),
                member.isActive());
    }
}
