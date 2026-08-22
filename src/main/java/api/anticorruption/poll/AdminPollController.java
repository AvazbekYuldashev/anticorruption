package api.anticorruption.poll;

import api.anticorruption.poll.dto.PollResponse;
import api.anticorruption.poll.dto.SavePollRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** So'rovnomalarni boshqarish. */
@Tag(name = "Admin - so'rovnomalar", description = "So'rovnoma yaratish va boshqarish")
@RestController
@RequestMapping("/api/v1/admin/polls")
@RequiredArgsConstructor
public class AdminPollController {

    private final PollService pollService;

    @Operation(summary = "Barcha so'rovnomalar", description = "Yopilganlari ham")
    @GetMapping
    public ResponseEntity<List<PollResponse>> list() {
        return ResponseEntity.ok(pollService.listAll());
    }

    @Operation(summary = "Bitta so'rovnoma")
    @GetMapping("/{id}")
    public ResponseEntity<PollResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(pollService.findById(id, null));
    }

    @Operation(summary = "So'rovnoma yaratish", description = "Kamida ikkita variant bo'lishi kerak")
    @PostMapping
    public ResponseEntity<PollResponse> create(@Valid @RequestBody SavePollRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pollService.create(request));
    }

    @Operation(
            summary = "So'rovnomani tahrirlash",
            description = "Mavjud variantning id sini yuboring - aks holda u yangi variant deb qaraladi "
                    + "va oldingi ovozlari yo'qoladi")
    @PutMapping("/{id}")
    public ResponseEntity<PollResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SavePollRequest request) {

        return ResponseEntity.ok(pollService.update(id, request));
    }

    @Operation(summary = "Faollashtirish yoki yopish")
    @PatchMapping("/{id}/active")
    public ResponseEntity<PollResponse> setActive(
            @PathVariable Long id,
            @RequestParam boolean active) {

        return ResponseEntity.ok(pollService.setActive(id, active));
    }

    @Operation(summary = "So'rovnomani o'chirish", description = "Barcha ovozlari bilan birga o'chadi")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        pollService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
