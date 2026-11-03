package com.globus.finance.controller.profile;

import com.globus.finance.entity.User;
import com.globus.finance.service.CurrentUserService;
import com.globus.finance.service.profile.ProfileService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private CurrentUserService currentUserService;

    private static final Long USER_ID = 17L;

    private User createUser() {

        User user = new User();

        user.setId(USER_ID);
        user.setName("Administrator");
        user.setEmail("test@example.com");
        user.setRegisteredAt(
                LocalDateTime.of(2026, 11, 1, 10, 30)
        );

        return user;
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldOpenProfilePage() throws Exception {

        User user = createUser();

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(profileService.getProfile(USER_ID))
                .thenReturn(user);

        mockMvc.perform(get("/profile"))

                .andExpect(status().isOk())

                .andExpect(view().name("profile/profile"))

                .andExpect(model().attributeExists("user"));

        verify(profileService).getProfile(USER_ID);
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldUpdateUserName() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        mockMvc.perform(
                        post("/profile/name")
                                .with(csrf())
                                .param("name", "Admin")
                )

                .andExpect(status().is3xxRedirection())

                .andExpect(redirectedUrl("/profile"))

                .andExpect(
                        flash().attribute(
                                "success",
                                "Имя успешно изменено"
                        )
                );

        verify(profileService)
                .updateName(USER_ID, "Admin");
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldRejectInvalidName() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        doThrow(
                new IllegalArgumentException(
                        "Имя не может быть пустым"
                )
        ).when(profileService)
                .updateName(USER_ID, "");

        mockMvc.perform(
                        post("/profile/name")
                                .with(csrf())
                                .param("name", "")
                )

                .andExpect(status().is3xxRedirection())

                .andExpect(redirectedUrl("/profile"))

                .andExpect(
                        flash().attribute(
                                "error",
                                "Имя не может быть пустым"
                        )
                );
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldRejectIncorrectPassword() throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        doThrow(
                new IllegalArgumentException(
                        "Неверный текущий пароль"
                )
        ).when(profileService)
                .changePassword(
                        USER_ID,
                        "wrong-password",
                        "new-password-123",
                        "new-password-123"
                );

        mockMvc.perform(
                        post("/profile/password")
                                .with(csrf())
                                .param("currentPassword", "wrong-password")
                                .param("newPassword", "new-password-123")
                                .param("confirmPassword", "new-password-123")
                )

                .andExpect(status().is3xxRedirection())

                .andExpect(redirectedUrl("/profile"))

                .andExpect(
                        flash().attribute(
                                "error",
                                "Неверный текущий пароль"
                        )
                );
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void shouldRedirectToLoginAfterPasswordChange()
            throws Exception {

        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        mockMvc.perform(
                        post("/profile/password")
                                .with(csrf())
                                .param("currentPassword", "old-password")
                                .param("newPassword", "new-password-123")
                                .param("confirmPassword", "new-password-123")
                )

                .andExpect(status().is3xxRedirection())

                .andExpect(
                        redirectedUrl("/login?passwordChanged")
                );

        verify(profileService).changePassword(
                USER_ID,
                "old-password",
                "new-password-123",
                "new-password-123"
        );
    }
}