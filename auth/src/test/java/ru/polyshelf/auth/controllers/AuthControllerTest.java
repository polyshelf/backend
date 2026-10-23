package ru.polyshelf.auth.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import ru.polyshelf.auth.dto.AuthTokensResponse;
import ru.polyshelf.auth.exception.AuthApiException;
import ru.polyshelf.auth.exception.AuthErrorCode;
import ru.polyshelf.auth.services.AuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    private static final AuthTokensResponse TOKENS = new AuthTokensResponse("access", "refresh", "Bearer", 900);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerReturns201WithSnakeCaseBody() throws Exception {
        when(authService.register(any())).thenReturn(TOKENS);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token").value("access"))
                .andExpect(jsonPath("$.refresh_token").value("refresh"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900));
    }

    @Test
    void registerRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"secret1\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void registerRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"12345\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void registerReturns409WhenEmailTaken() throws Exception {
        when(authService.register(any())).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void loginReturns200() throws Exception {
        when(authService.login(any())).thenReturn(TOKENS);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access"));
    }

    @Test
    void loginReturns401OnBadCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshReadsSnakeCaseField() throws Exception {
        when(authService.refresh("old-token")).thenReturn(TOKENS);

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"old-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refresh_token").value("refresh"));
    }

    @Test
    void refreshRejectsBlankToken() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void yandexReturns200() throws Exception {
        when(authService.yandex(any())).thenReturn(TOKENS);

        mockMvc.perform(post("/auth/yandex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"access_token\":\"ya-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("access"));
    }

    @Test
    void yandexRejectsBlankToken() throws Exception {
        mockMvc.perform(post("/auth/yandex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"access_token\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void yandexReturnsErrorCodeInBody() throws Exception {
        when(authService.yandex(any())).thenThrow(
                new AuthApiException(HttpStatus.CONFLICT, AuthErrorCode.YANDEX_LINK_REQUIRED, "link"));

        mockMvc.perform(post("/auth/yandex")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"access_token\":\"ya-token\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("YANDEX_LINK_REQUIRED"));
    }

}
