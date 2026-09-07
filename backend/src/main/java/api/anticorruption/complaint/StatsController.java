package api.anticorruption.complaint;

import api.anticorruption.complaint.dto.FacultyRatingResponse;
import api.anticorruption.complaint.dto.StatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Statistika va fakultetlar kesimi.
 *
 * <p>Jamlanma raqamlar ochiq: portal maqsadi shaffoflik. Ular hech kimning
 * shaxsini oshkor qilmaydi - faqat nechta murojaat kelgani va qanchasi
 * hal qilingani ko'rinadi.
 */
@Tag(name = "Statistika", description = "Umumiy raqamlar va fakultetlar kesimi")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "Ommaviy statistika", description = "Autentifikatsiyasiz ochiq")
    @GetMapping("/stats/public")
    public ResponseEntity<StatsResponse> publicStats() {
        return ResponseEntity.ok(statsService.overview());
    }

    @Operation(
            summary = "Fakultetlar kesimi",
            description = "Har bir fakultet bo'yicha murojaatlar soni, hal qilinganlari va o'rtacha muddat. "
                    + "Yagona ball hisoblanmaydi: murojaat ko'pligi muammo ko'pligini ham, "
                    + "tizimga ishonch yuqoriligini ham anglatishi mumkin.")
    @GetMapping("/stats/faculty-rating")
    public ResponseEntity<List<FacultyRatingResponse>> facultyRating() {
        return ResponseEntity.ok(statsService.facultyRating());
    }

    @Operation(summary = "Admin paneli statistikasi")
    @GetMapping("/admin/stats")
    public ResponseEntity<StatsResponse> adminStats() {
        return ResponseEntity.ok(statsService.overview());
    }
}
