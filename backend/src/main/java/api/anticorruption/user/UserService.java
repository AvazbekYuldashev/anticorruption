package api.anticorruption.user;

import api.anticorruption.auth.RefreshTokenService;
import api.anticorruption.common.dto.PageResponse;
import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.exception.ConflictException;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.complaint.ComplaintRepository;
import api.anticorruption.user.dto.ChangePasswordRequest;
import api.anticorruption.user.dto.CreateUserRequest;
import api.anticorruption.user.dto.UpdateProfileRequest;
import api.anticorruption.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Foydalanuvchilarni boshqarish.
 *
 * <p>Hisoblarni faqat administrator yaratadi - ochiq ro'yxatdan o'tish yo'q.
 * Himoya qoidalari: administrator o'z rolini pasaytira olmaydi, o'zini
 * bloklay va o'chira olmaydi. Aks holda tizim boshqaruvsiz qolib ketishi
 * mumkin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final Translator translator;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Role role, Pageable pageable) {
        Page<User> page = role == null
                ? userRepository.findAll(pageable)
                : userRepository.findByRole(role, pageable);
        return PageResponse.from(page.map(user -> UserResponse.from(user, translator)));
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long userId) {
        return UserResponse.from(requireUser(userId), translator);
    }

    /** Rolni o'zgartiradi. Administrator o'z rolini o'zgartira olmaydi. */
    @Transactional
    public UserResponse changeRole(Long userId, Role newRole, User actor) {
        if (newRole == null) {
            throw new BadRequestException(MessageKeys.USER_ROLE_REQUIRED);
        }
        User user = requireUser(userId);

        if (Objects.equals(user.getId(), actor.getId())) {
            throw new BadRequestException(MessageKeys.USER_CANNOT_CHANGE_OWN_ROLE);
        }
        if (user.getRole() == newRole) {
            throw new BadRequestException(MessageKeys.USER_ALREADY_IN_ROLE, roleLabel(newRole));
        }

        Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);
        log.info("Foydalanuvchi id={} roli o'zgardi: {} -> {} (admin id={})",
                user.getId(), oldRole, newRole, actor.getId());

        return UserResponse.from(user, translator);
    }

    /** Hisobni bloklaydi yoki blokdan chiqaradi. Administrator o'zini bloklay olmaydi. */
    @Transactional
    public UserResponse setEnabled(Long userId, boolean enabled, User actor) {
        User user = requireUser(userId);

        if (Objects.equals(user.getId(), actor.getId())) {
            throw new BadRequestException(MessageKeys.USER_CANNOT_DISABLE_SELF);
        }
        if (user.isEnabled() == enabled) {
            throw new BadRequestException(enabled
                    ? MessageKeys.USER_ALREADY_ENABLED
                    : MessageKeys.USER_ALREADY_DISABLED);
        }

        user.setEnabled(enabled);
        userRepository.save(user);
        log.info("Foydalanuvchi id={} holati o'zgardi: enabled={} (admin id={})",
                user.getId(), enabled, actor.getId());

        return UserResponse.from(user, translator);
    }

    /**
     * Yangi hisob yaratadi.
     *
     * <p>Rolni administrator o'zi tanlaydi: xodim ham, oddiy foydalanuvchi
     * ham shu yo'l bilan qo'shiladi.
     */
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(MessageKeys.ERROR_AUTH_EMAIL_TAKEN);
        }

        User user = User.builder()
                .fullName(request.fullName().trim())
                .email(email)
                .phone(blankToNull(request.phone()))
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("Yangi hisob yaratildi: id={}, rol={}", user.getId(), user.getRole());

        return UserResponse.from(user, translator);
    }

    /**
     * Hisobni butunlay o'chiradi.
     *
     * <p>Murojaatlarga bog'langan hisob o'chirilmaydi: murojaat muallifi yoki
     * mas'ul xodimi yo'qolsa, murojaatlar tarixi buziladi. Bunday hisobni
     * o'chirish o'rniga bloklash kerak - u holda kirish yopiladi, yozuvlar
     * esa joyida qoladi.
     */
    @Transactional
    public void delete(Long userId, User actor) {
        User user = requireUser(userId);

        if (Objects.equals(user.getId(), actor.getId())) {
            throw new BadRequestException(MessageKeys.USER_CANNOT_DELETE_SELF);
        }
        if (complaintRepository.existsByAuthorIdOrAssigneeId(userId, userId)) {
            throw new ConflictException(MessageKeys.USER_HAS_COMPLAINTS);
        }

        userRepository.delete(user);
        log.info("Hisob o'chirildi: id={} (admin id={})", userId, actor.getId());
    }

    /** Foydalanuvchi o'z ismi, emaili va telefonini yangilaydi. */
    @Transactional
    public UserResponse updateProfile(User actor, UpdateProfileRequest request) {
        User user = requireUser(actor.getId());
        String email = normalizeEmail(request.email());

        // Email - bu ayni paytda login, shuning uchun u noyob bo'lishi shart.
        if (!user.getEmail().equalsIgnoreCase(email)
                && userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException(MessageKeys.ERROR_AUTH_EMAIL_TAKEN);
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(blankToNull(request.phone()));
        userRepository.save(user);

        log.info("Profil yangilandi: id={}", user.getId());
        return UserResponse.from(user, translator);
    }

    /**
     * Parolni almashtiradi.
     *
     * <p>Joriy parol tekshiriladi: token qo'lga tushgan bo'lsa ham, uni
     * bilmasdan hisobni butunlay egallab bo'lmasin.
     */
    @Transactional
    public void changePassword(User actor, ChangePasswordRequest request) {
        User user = requireUser(actor.getId());

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException(MessageKeys.USER_CURRENT_PASSWORD_WRONG);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BadRequestException(MessageKeys.USER_PASSWORD_NOT_CHANGED);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        /*
         * Parol almashtirilishining odatiy sababi - eski parol birovga
         * ma'lum bo'lib qolgani. Shuning uchun boshqa qurilmalardagi
         * seanslar ham uziladi: aks holda o'g'ri o'z seansida qolaverardi.
         */
        refreshTokenService.revokeAll(user.getId());

        log.info("Parol almashtirildi, barcha seanslar uzildi: id={}", user.getId());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Rol nomi xabar bilan bir vaqtda, bir xil tilda hosil qilinsin. */
    private Object roleLabel(Role role) {
        return new DefaultMessageSourceResolvable(new String[]{role.messageKey()}, role.name());
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_USER, userId));
    }
}
