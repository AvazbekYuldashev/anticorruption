package api.anticorruption.user;

import api.anticorruption.common.dto.PageResponse;
import api.anticorruption.common.exception.BadRequestException;
import api.anticorruption.common.exception.ResourceNotFoundException;
import api.anticorruption.common.i18n.MessageKeys;
import api.anticorruption.common.i18n.Translator;
import api.anticorruption.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Foydalanuvchilarni boshqarish - faqat administrator uchun.
 *
 * <p>Ikkita himoya qoidasi bor: administrator o'z rolini pasaytira olmaydi va
 * o'zini bloklay olmaydi. Aks holda tizim boshqaruvsiz qolib ketishi mumkin.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
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

    /** Rol nomi xabar bilan bir vaqtda, bir xil tilda hosil qilinsin. */
    private Object roleLabel(Role role) {
        return new DefaultMessageSourceResolvable(new String[]{role.messageKey()}, role.name());
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.NOT_FOUND_USER, userId));
    }
}
