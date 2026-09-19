package api.anticorruption.poll;

import api.anticorruption.poll.dto.PollGroupResponse;
import api.anticorruption.poll.dto.SavePollGroupRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** So'rovnoma va testlarni guruhlarga ajratish. */
@Tag(name = "Admin - so'rovnoma guruhlari", description = "So'rovnoma va testlarni guruhlash")
@RestController
@RequestMapping("/api/v1/admin/poll-groups")
@RequiredArgsConstructor
public class AdminPollGroupController {

    private final PollGroupService groupService;

    @Operation(
            summary = "Guruhlar ro'yxati",
            description = "type=SURVEY yoki type=QUIZ bilan bitta sahifaning guruhlarini oladi; "
                    + "ko'rsatilmasa hammasi")
    @GetMapping
    public ResponseEntity<List<PollGroupResponse>> list(
            @RequestParam(required = false) PollType type) {

        return ResponseEntity.ok(groupService.list(type));
    }

    @Operation(
            summary = "Guruh yaratish",
            description = "Bir turdagi guruhlar orasida nom takrorlanmasligi kerak. Test guruhida "
                    + "questionsPerAttempt har bir ishtirokchiga guruhdagi testning savollaridan "
                    + "nechtasi tasodifiy berilishini belgilaydi; bo'sh bo'lsa hammasi beriladi")
    @PostMapping
    public ResponseEntity<PollGroupResponse> create(
            @Valid @RequestBody SavePollGroupRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.create(request));
    }

    @Operation(
            summary = "Guruhni tahrirlash",
            description = "Guruh turi o'zgarmaydi - u ichidagi so'rovnomalarni boshqa sahifaga "
                    + "ko'chirib yuborardi")
    @PutMapping("/{id}")
    public ResponseEntity<PollGroupResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SavePollGroupRequest request) {

        return ResponseEntity.ok(groupService.update(id, request));
    }

    @Operation(
            summary = "Guruhni o'chirish",
            description = "So'rovnomalar o'chmaydi, faqat guruhdan chiqadi va \"Guruhsiz\" "
                    + "bo'limida qoladi")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        groupService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
