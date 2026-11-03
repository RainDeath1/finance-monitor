package com.globus.finance.service.profile;

import com.globus.finance.entity.User;
import com.globus.finance.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfileService profileService;

    private static final Long USER_ID = 17L;

    private User createUser() {

        User user = new User();

        user.setId(USER_ID);
        user.setName("Administrator");
        user.setEmail("test@example.com");
        user.setPasswordHash("old-hash");
        user.setSessionVersion(1L);

        return user;
    }

    @Test
    void shouldReturnUserProfile() {

        User user = createUser();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        User result = profileService.getProfile(USER_ID);

        assertEquals(USER_ID, result.getId());

        assertEquals(
                "Administrator",
                result.getName()
        );

        verify(userRepository).findById(USER_ID);
    }

    @Test
    void shouldUpdateUserName() {

        User user = createUser();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        profileService.updateName(
                USER_ID,
                "  Admin  "
        );

        assertEquals(
                "Admin",
                user.getName()
        );

        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectBlankName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> profileService.updateName(
                        USER_ID,
                        "   "
                )
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectLongName() {

        String longName = "A".repeat(101);

        assertThrows(
                IllegalArgumentException.class,
                () -> profileService.updateName(
                        USER_ID,
                        longName
                )
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectIncorrectCurrentPassword() {

        User user = createUser();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "old-hash"
        )).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> profileService.changePassword(
                        USER_ID,
                        "wrong-password",
                        "new-password-123",
                        "new-password-123"
                )
        );

        assertEquals(
                "old-hash",
                user.getPasswordHash()
        );

        assertEquals(
                1L,
                user.getSessionVersion()
        );

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void shouldRejectPasswordMismatch() {

        assertThrows(
                IllegalArgumentException.class,
                () -> profileService.changePassword(
                        USER_ID,
                        "current-password",
                        "new-password-123",
                        "different-password"
                )
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldRejectShortPassword() {

        assertThrows(
                IllegalArgumentException.class,
                () -> profileService.changePassword(
                        USER_ID,
                        "current-password",
                        "123",
                        "123"
                )
        );

        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldChangePasswordAndIncrementSessionVersion() {

        User user = createUser();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "current-password",
                "old-hash"
        )).thenReturn(true);

        when(passwordEncoder.encode("new-password-123"))
                .thenReturn("new-hash");

        profileService.changePassword(
                USER_ID,
                "current-password",
                "new-password-123",
                "new-password-123"
        );

        assertEquals(
                "new-hash",
                user.getPasswordHash()
        );

        assertEquals(
                2L,
                user.getSessionVersion()
        );

        verify(passwordEncoder)
                .encode("new-password-123");

        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectSamePassword() {

        User user = createUser();

        when(userRepository.findById(USER_ID))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "current-password",
                "old-hash"
        )).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> profileService.changePassword(
                        USER_ID,
                        "current-password",
                        "current-password",
                        "current-password"
                )
        );

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(userRepository, never())
                .save(any(User.class));
    }
}