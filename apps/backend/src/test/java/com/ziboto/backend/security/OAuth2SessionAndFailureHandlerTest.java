package com.ziboto.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class OAuth2SessionAndFailureHandlerTest {

    @Test
    void authorizationRequestCanBeReadFromTheSameHttpSessionOnCallback() {
        HttpSessionOAuth2AuthorizationRequestRepository repository =
                new HttpSessionOAuth2AuthorizationRequestRepository();
        MockHttpServletRequest authorizationRequest = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        MockHttpServletResponse authorizationResponse = new MockHttpServletResponse();
        OAuth2AuthorizationRequest request = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("test-client")
                .redirectUri("https://api.ziboto.alliededge.app/login/oauth2/code/google")
                .state("state-value")
                .build();

        repository.saveAuthorizationRequest(request, authorizationRequest, authorizationResponse);

        MockHttpServletRequest callback = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        callback.setSession(authorizationRequest.getSession());
        callback.setParameter("state", "state-value");

        OAuth2AuthorizationRequest restored = repository.loadAuthorizationRequest(callback);
        assertNotNull(restored);
        assertEquals("state-value", restored.getState());
    }

    @Test
    void missingOrDifferentSessionDoesNotRestoreAuthorizationRequest() {
        HttpSessionOAuth2AuthorizationRequestRepository repository =
                new HttpSessionOAuth2AuthorizationRequestRepository();
        MockHttpServletRequest callback = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        callback.setParameter("state", "state-value");

        assertNull(repository.loadAuthorizationRequest(callback));
    }

    @Test
    void oauthFailureRedirectDoesNotExposeTheProviderException() throws Exception {
        OAuth2AuthenticationFailureHandler handler = new OAuth2AuthenticationFailureHandler();
        ReflectionTestUtils.setField(handler, "frontendRedirectUrl", "https://ziboto.alliededge.app/oauth/callback");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(new MockHttpServletRequest(), response,
                new OAuth2AuthenticationException("invalid_state"));

        assertEquals(302, response.getStatus());
        assertEquals("https://ziboto.alliededge.app/oauth/callback?error=oauth_failed",
                response.getRedirectedUrl());
    }
}
