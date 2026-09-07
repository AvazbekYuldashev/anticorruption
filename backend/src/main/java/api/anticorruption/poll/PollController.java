package api.anticorruption.poll;

import api.anticorruption.poll.dto.PollResponse;
import api.anticorruption.poll.dto.PollVoteRequest;
import api.anticorruption.security.AppUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Ommaviy so'rovnomalar. Ovoz berish uchun tizimga kirish shart emas.
 */
@Tag(name = "So'rovnomalar", description = "Faol so'rovnomalar va ovoz berish")
@RestController
@RequestMapping("/api/v1/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;
    private final VoterKeyFactory voterKeyFactory;

    @Operation(
            summary = "Faol so'rovnoma va testlar",
            description = "Har birida joriy natijalar ham bo'ladi. "
                    + "type=SURVEY yoki type=QUIZ bilan turini ajratish mumkin")
    @GetMapping
    public ResponseEntity<List<PollResponse>> active(
            @RequestParam(required = false) PollType type,
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        return ResponseEntity.ok(pollService.listActive(
                type, pollId -> voterKeyFactory.create(pollId, httpRequest, principal)));
    }

    @Operation(summary = "Bitta so'rovnoma va natijalari")
    @GetMapping("/{id}")
    public ResponseEntity<PollResponse> detail(
            @PathVariable Long id,
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        String voterKey = voterKeyFactory.create(id, httpRequest, principal);
        return ResponseEntity.ok(pollService.findById(id, voterKey));
    }

    @Operation(
            summary = "Ovoz berish",
            description = "Bir odam bir so'rovnomada bir marta ovoz beradi. "
                    + "Takroriy urinishda 409 qaytadi.")
    @PostMapping("/{id}/vote")
    public ResponseEntity<PollResponse> vote(
            @PathVariable Long id,
            @Valid @RequestBody PollVoteRequest request,
            HttpServletRequest httpRequest,
            @AuthenticationPrincipal AppUserPrincipal principal) {

        String voterKey = voterKeyFactory.create(id, httpRequest, principal);
        return ResponseEntity.ok(pollService.vote(id, request, voterKey));
    }
}
