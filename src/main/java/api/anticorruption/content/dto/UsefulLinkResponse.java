package api.anticorruption.content.dto;

import api.anticorruption.content.UsefulLink;

/** Foydali havola. */
public record UsefulLinkResponse(
        Long id,
        String title,
        String url,
        String description,
        String groupName,
        int displayOrder,
        boolean active
) {
    public static UsefulLinkResponse from(UsefulLink link) {
        return new UsefulLinkResponse(
                link.getId(),
                link.getTitle(),
                link.getUrl(),
                link.getDescription(),
                link.getGroupName(),
                link.getDisplayOrder(),
                link.isActive());
    }
}
