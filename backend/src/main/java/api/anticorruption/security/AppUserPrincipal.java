package api.anticorruption.security;

import api.anticorruption.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security uchun {@link UserDetails} adapteri.
 * Kontrollerlarda {@code @AuthenticationPrincipal AppUserPrincipal} orqali
 * to'g'ridan-to'g'ri {@link User} entitysiga kirish mumkin.
 */
public record AppUserPrincipal(User user) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(user.getRole().authority()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    public Long id() {
        return user.getId();
    }
}
