package ch.uzh.ifi.hase.soprafs24.config;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.UserProfile;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.websocket.WebSocketErrorHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.SimpleBrokerRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.SockJsServiceRegistration;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.net.URI;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class WebSocketConfigTest {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfigTest.class);

    @Mock
    private AuthService authService;

    @Mock
    private WebSocketErrorHandler webSocketErrorHandler;

    @Mock
    private StompEndpointRegistry registry;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler wsHandler;

    @Mock
    private ServletServerHttpRequest servletRequest;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private MessageChannel messageChannel;

    @Mock
    private StompWebSocketEndpointRegistration endpointRegistration;

    @Mock
    private MessageBrokerRegistry messageBrokerRegistry;

    @Mock
    private SimpleBrokerRegistration simpleBrokerRegistration;

    @Mock
    private SockJsServiceRegistration sockJsServiceRegistration;

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    private Map<String, Object> attributes;
    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        logger.info("Setting up test fixture");

        attributes = new HashMap<>();
        validToken = "valid-token-123";

        testUser = new User();
        testUser.setId(1L);
        UserProfile profile = new UserProfile();
        profile.setUsername("testUser");
        testUser.setProfile(profile);

        when(request.getURI()).thenReturn(URI.create("ws://localhost:8080/ws/lobby"));

        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
    }

    @Test
    void configureMessageBroker_configuresCorrectly() {
        // Given
        when(messageBrokerRegistry.enableSimpleBroker("/topic", "/queue")).thenReturn(simpleBrokerRegistration);
        when(simpleBrokerRegistration.setHeartbeatValue(any(long[].class))).thenReturn(simpleBrokerRegistration);
        when(simpleBrokerRegistration.setTaskScheduler(any())).thenReturn(simpleBrokerRegistration);

        when(messageBrokerRegistry.setApplicationDestinationPrefixes("/app")).thenReturn(null);
        when(messageBrokerRegistry.setUserDestinationPrefix("/user")).thenReturn(null);

        // When
        webSocketConfig.configureMessageBroker(messageBrokerRegistry);

        // Then
        verify(messageBrokerRegistry).enableSimpleBroker("/topic", "/queue");
        verify(simpleBrokerRegistration).setHeartbeatValue(new long[] {10000, 10000});
        verify(simpleBrokerRegistration).setTaskScheduler(any(org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler.class));
        verify(messageBrokerRegistry).setApplicationDestinationPrefixes("/app");
        verify(messageBrokerRegistry).setUserDestinationPrefix("/user");
    }

    @Test
    void registerStompEndpoints_configuresCorrectly() {
        reset(registry);

        StompWebSocketEndpointRegistration localEndpointRegistration1 = mock(StompWebSocketEndpointRegistration.class);
        StompWebSocketEndpointRegistration localEndpointRegistration2 = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration localSockJsRegistration = mock(SockJsServiceRegistration.class);

        when(registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game"))
                .thenReturn(localEndpointRegistration1, localEndpointRegistration2);

        when(localEndpointRegistration1.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.withSockJS())
                .thenReturn(localSockJsRegistration);

        when(localEndpointRegistration2.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration2);

        when(localSockJsRegistration.setSessionCookieNeeded(false))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setWebSocketEnabled(true))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setDisconnectDelay(5000))
                .thenReturn(localSockJsRegistration);

        webSocketConfig.registerStompEndpoints(registry);

        verify(registry, times(2)).addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game");
        verify(registry).setErrorHandler(webSocketErrorHandler);

        verify(localSockJsRegistration).setSessionCookieNeeded(false);
        verify(localSockJsRegistration).setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js");
        verify(localSockJsRegistration).setWebSocketEnabled(true);
        verify(localSockJsRegistration).setDisconnectDelay(5000);
    }

    @Test
    void handshakeInterceptor_beforeHandshake_withValidQueryToken_success() throws Exception {
        // Given
        // Reset the mock to ensure clean state
        reset(registry);

        // Create the ArgumentCaptor first
        ArgumentCaptor<HandshakeInterceptor> interceptorCaptor = ArgumentCaptor.forClass(HandshakeInterceptor.class);

        // Create local mocks for this test
        StompWebSocketEndpointRegistration localEndpointRegistration1 = mock(StompWebSocketEndpointRegistration.class);
        StompWebSocketEndpointRegistration localEndpointRegistration2 = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration localSockJsRegistration = mock(SockJsServiceRegistration.class);

        // Configure registry to return different registrations for consecutive calls
        when(registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game"))
                .thenReturn(localEndpointRegistration1, localEndpointRegistration2);

        // Configure first endpoint registration (SockJS) - capture the interceptor here
        when(localEndpointRegistration1.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.addInterceptors(interceptorCaptor.capture()))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.withSockJS())
                .thenReturn(localSockJsRegistration);

        // Configure second endpoint registration (Raw WebSocket)
        when(localEndpointRegistration2.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration2);

        // Set up SockJS registration chain
        when(localSockJsRegistration.setSessionCookieNeeded(false))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setWebSocketEnabled(true))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setDisconnectDelay(5000))
                .thenReturn(localSockJsRegistration);

        // Call the method
        webSocketConfig.registerStompEndpoints(registry);

        // Get the captured interceptor
        HandshakeInterceptor interceptor = interceptorCaptor.getValue();

        // Setup for the test
        when(request.getURI()).thenReturn(URI.create("ws://localhost:8080/ws/lobby?token=" + validToken));
        when(authService.getUserByToken(validToken)).thenReturn(testUser);

        // When
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Then
        assertTrue(result);
        assertEquals(testUser.getId(), attributes.get("userId"));
        assertEquals(testUser.getProfile().getUsername(), attributes.get("username"));
        assertEquals(validToken, attributes.get("token"));
    }

    @Test
    void handshakeInterceptor_beforeHandshake_withValidHeaderToken_success() throws Exception {
        // Given
        // Reset the mock to ensure clean state
        reset(registry);

        // Create the ArgumentCaptor first
        ArgumentCaptor<HandshakeInterceptor> interceptorCaptor = ArgumentCaptor.forClass(HandshakeInterceptor.class);

        // Create local mocks for this test
        StompWebSocketEndpointRegistration localEndpointRegistration1 = mock(StompWebSocketEndpointRegistration.class);
        StompWebSocketEndpointRegistration localEndpointRegistration2 = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration localSockJsRegistration = mock(SockJsServiceRegistration.class);

        // Configure registry to return different registrations for consecutive calls
        when(registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game"))
                .thenReturn(localEndpointRegistration1, localEndpointRegistration2);

        // Configure first endpoint registration (SockJS) - capture the interceptor here
        when(localEndpointRegistration1.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.addInterceptors(interceptorCaptor.capture()))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.withSockJS())
                .thenReturn(localSockJsRegistration);

        // Configure second endpoint registration (Raw WebSocket)
        when(localEndpointRegistration2.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration2);

        // Set up SockJS registration chain
        when(localSockJsRegistration.setSessionCookieNeeded(false))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setWebSocketEnabled(true))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setDisconnectDelay(5000))
                .thenReturn(localSockJsRegistration);

        // Call the method
        webSocketConfig.registerStompEndpoints(registry);

        // Get the captured interceptor
        HandshakeInterceptor interceptor = interceptorCaptor.getValue();

        // Setup for the test - use header instead of query parameter
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + validToken);
        when(request.getHeaders()).thenReturn(headers);
        when(authService.getUserByToken(validToken)).thenReturn(testUser);

        // When
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Then
        assertTrue(result);
        assertEquals(testUser.getId(), attributes.get("userId"));
        assertEquals(testUser.getProfile().getUsername(), attributes.get("username"));
        assertEquals(validToken, attributes.get("token"));
    }

    @Test
    void handshakeInterceptor_beforeHandshake_withInvalidToken_returnsFalse() throws Exception {
        // Given
        // Reset the mock to ensure clean state
        reset(registry);

        // Create the ArgumentCaptor first
        ArgumentCaptor<HandshakeInterceptor> interceptorCaptor = ArgumentCaptor.forClass(HandshakeInterceptor.class);

        // Create local mocks for this test
        StompWebSocketEndpointRegistration localEndpointRegistration1 = mock(StompWebSocketEndpointRegistration.class);
        StompWebSocketEndpointRegistration localEndpointRegistration2 = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration localSockJsRegistration = mock(SockJsServiceRegistration.class);

        // Configure registry to return different registrations for consecutive calls
        when(registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game"))
                .thenReturn(localEndpointRegistration1, localEndpointRegistration2);

        // Configure first endpoint registration (SockJS) - capture the interceptor here
        when(localEndpointRegistration1.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.addInterceptors(interceptorCaptor.capture()))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.withSockJS())
                .thenReturn(localSockJsRegistration);

        // Configure second endpoint registration (Raw WebSocket)
        when(localEndpointRegistration2.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration2);

        // Set up SockJS registration chain
        when(localSockJsRegistration.setSessionCookieNeeded(false))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setWebSocketEnabled(true))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setDisconnectDelay(5000))
                .thenReturn(localSockJsRegistration);

        // Call the method
        webSocketConfig.registerStompEndpoints(registry);

        // Get the captured interceptor
        HandshakeInterceptor interceptor = interceptorCaptor.getValue();

        // Setup for the test - invalid token scenario
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer invalid-token");
        when(request.getHeaders()).thenReturn(headers);
        when(authService.getUserByToken(anyString())).thenThrow(new RuntimeException("Invalid token"));

        // When
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Then
        assertFalse(result);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handshakeHandler_determineUser_withToken_returnsTokenPrincipal() throws Exception {
        // Given
        reset(registry);

        ArgumentCaptor<DefaultHandshakeHandler> handlerCaptor = ArgumentCaptor.forClass(DefaultHandshakeHandler.class);

        // Create local mocks for this test
        StompWebSocketEndpointRegistration localEndpointRegistration1 = mock(StompWebSocketEndpointRegistration.class);
        StompWebSocketEndpointRegistration localEndpointRegistration2 = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration localSockJsRegistration = mock(SockJsServiceRegistration.class);

        // Configure registry to return different registrations for consecutive calls
        when(registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game"))
                .thenReturn(localEndpointRegistration1, localEndpointRegistration2);

        // Configure first endpoint registration (SockJS) - capture the handler here
        when(localEndpointRegistration1.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.setHandshakeHandler(handlerCaptor.capture()))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.withSockJS())
                .thenReturn(localSockJsRegistration);

        // Configure second endpoint registration (Raw WebSocket)
        when(localEndpointRegistration2.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration2);

        // Set up SockJS registration chain
        when(localSockJsRegistration.setSessionCookieNeeded(false))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setWebSocketEnabled(true))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setDisconnectDelay(5000))
                .thenReturn(localSockJsRegistration);

        // Call the method
        webSocketConfig.registerStompEndpoints(registry);

        // Get the captured handler
        DefaultHandshakeHandler handler = handlerCaptor.getValue();

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("token", validToken);

        // When - First, let's find the correct method
        Method determineUserMethod = null;
        Class<?> handlerClass = handler.getClass();

        // Search through all methods including inherited ones
        while (handlerClass != null && determineUserMethod == null) {
            for (Method method : handlerClass.getDeclaredMethods()) {
                if (method.getName().equals("determineUser") &&
                        method.getParameterCount() == 3 &&
                        Principal.class.isAssignableFrom(method.getReturnType())) {
                    determineUserMethod = method;
                    break;
                }
            }
            handlerClass = handlerClass.getSuperclass();
        }

        if (determineUserMethod == null) {
            throw new NoSuchMethodException("Could not find determineUser method");
        }

        determineUserMethod.setAccessible(true);
        Principal principal = (Principal) determineUserMethod.invoke(handler, request, wsHandler, attrs);

        // Then
        assertNotNull(principal);
        assertEquals(validToken, principal.getName());
    }

    @Test
    void handshakeHandler_determineUser_withUserId_returnsUserIdPrincipal() throws Exception {
        // Given
        reset(registry);

        ArgumentCaptor<DefaultHandshakeHandler> handlerCaptor = ArgumentCaptor.forClass(DefaultHandshakeHandler.class);

        // Create local mocks for this test
        StompWebSocketEndpointRegistration localEndpointRegistration1 = mock(StompWebSocketEndpointRegistration.class);
        StompWebSocketEndpointRegistration localEndpointRegistration2 = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration localSockJsRegistration = mock(SockJsServiceRegistration.class);

        // Configure registry to return different registrations for consecutive calls
        when(registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game"))
                .thenReturn(localEndpointRegistration1, localEndpointRegistration2);

        // Configure first endpoint registration (SockJS) - capture the handler here
        when(localEndpointRegistration1.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.setHandshakeHandler(handlerCaptor.capture()))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.withSockJS())
                .thenReturn(localSockJsRegistration);

        // Configure second endpoint registration (Raw WebSocket)
        when(localEndpointRegistration2.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration2);

        // Set up SockJS registration chain
        when(localSockJsRegistration.setSessionCookieNeeded(false))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setWebSocketEnabled(true))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setDisconnectDelay(5000))
                .thenReturn(localSockJsRegistration);

        // Call the method
        webSocketConfig.registerStompEndpoints(registry);

        // Get the captured handler
        DefaultHandshakeHandler handler = handlerCaptor.getValue();

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", 123L);

        // When - First, let's find the correct method
        Method determineUserMethod = null;
        Class<?> handlerClass = handler.getClass();

        // Search through all methods including inherited ones
        while (handlerClass != null && determineUserMethod == null) {
            for (Method method : handlerClass.getDeclaredMethods()) {
                if (method.getName().equals("determineUser") &&
                        method.getParameterCount() == 3 &&
                        Principal.class.isAssignableFrom(method.getReturnType())) {
                    determineUserMethod = method;
                    break;
                }
            }
            handlerClass = handlerClass.getSuperclass();
        }

        if (determineUserMethod == null) {
            throw new NoSuchMethodException("Could not find determineUser method");
        }

        determineUserMethod.setAccessible(true);
        Principal principal = (Principal) determineUserMethod.invoke(handler, request, wsHandler, attrs);

        // Then
        assertNotNull(principal);
        assertEquals("123", principal.getName());
    }

    @Test
    void handshakeHandler_determineUser_withIntegerUserId_returnsUserIdPrincipal() throws Exception {
        // Given
        reset(registry);

        ArgumentCaptor<DefaultHandshakeHandler> handlerCaptor = ArgumentCaptor.forClass(DefaultHandshakeHandler.class);

        // Create local mocks for this test
        StompWebSocketEndpointRegistration localEndpointRegistration1 = mock(StompWebSocketEndpointRegistration.class);
        StompWebSocketEndpointRegistration localEndpointRegistration2 = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration localSockJsRegistration = mock(SockJsServiceRegistration.class);

        // Configure registry to return different registrations for consecutive calls
        when(registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game"))
                .thenReturn(localEndpointRegistration1, localEndpointRegistration2);

        // Configure first endpoint registration (SockJS) - capture the handler here
        when(localEndpointRegistration1.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.setHandshakeHandler(handlerCaptor.capture()))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.withSockJS())
                .thenReturn(localSockJsRegistration);

        // Configure second endpoint registration (Raw WebSocket)
        when(localEndpointRegistration2.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration2);

        // Set up SockJS registration chain
        when(localSockJsRegistration.setSessionCookieNeeded(false))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setWebSocketEnabled(true))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setDisconnectDelay(5000))
                .thenReturn(localSockJsRegistration);

        // Call the method
        webSocketConfig.registerStompEndpoints(registry);

        // Get the captured handler
        DefaultHandshakeHandler handler = handlerCaptor.getValue();

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("userId", 123);

        // When - First, let's find the correct method
        Method determineUserMethod = null;
        Class<?> handlerClass = handler.getClass();

        // Search through all methods including inherited ones
        while (handlerClass != null && determineUserMethod == null) {
            for (Method method : handlerClass.getDeclaredMethods()) {
                if (method.getName().equals("determineUser") &&
                        method.getParameterCount() == 3 &&
                        Principal.class.isAssignableFrom(method.getReturnType())) {
                    determineUserMethod = method;
                    break;
                }
            }
            handlerClass = handlerClass.getSuperclass();
        }

        if (determineUserMethod == null) {
            throw new NoSuchMethodException("Could not find determineUser method");
        }

        determineUserMethod.setAccessible(true);
        Principal principal = (Principal) determineUserMethod.invoke(handler, request, wsHandler, attrs);

        // Then
        assertNotNull(principal);
        assertEquals("123", principal.getName());
    }

    @Test
    void inboundChannelInterceptor_preSend_withConnectCommand_success() {
        // Given
        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor> interceptorCaptor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        when(registration.interceptors(interceptorCaptor.capture())).thenReturn(registration);

        webSocketConfig.configureClientInboundChannel(registration);
        ChannelInterceptor interceptor = interceptorCaptor.getValue();

        // Create STOMP CONNECT message
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("test-session");
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("token", validToken);
        accessor.setSessionAttributes(sessionAttributes);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(authService.getUserByToken(validToken)).thenReturn(testUser);

        // When
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Then
        assertNotNull(result);
        assertEquals(message, result);
    }

    @Test
    void inboundChannelInterceptor_preSend_withConnectCommand_fallbackToStompHeaders() {
        // Given
        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor> interceptorCaptor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        when(registration.interceptors(interceptorCaptor.capture())).thenReturn(registration);

        webSocketConfig.configureClientInboundChannel(registration);
        ChannelInterceptor interceptor = interceptorCaptor.getValue();

        // Create STOMP CONNECT message with header token
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("test-session");
        accessor.addNativeHeader("Authorization", "Bearer " + validToken);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(authService.getUserByToken(validToken)).thenReturn(testUser);

        // When
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Then
        assertNotNull(result);
        assertEquals(message, result);
    }

    @Test
    void inboundChannelInterceptor_preSend_withConnectCommand_invalidToken_doesNotThrowException() {
        // Given
        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor> interceptorCaptor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        when(registration.interceptors(interceptorCaptor.capture())).thenReturn(registration);

        webSocketConfig.configureClientInboundChannel(registration);
        ChannelInterceptor interceptor = interceptorCaptor.getValue();

        // Create STOMP CONNECT message
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("test-session");
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("token", "invalid-token");
        accessor.setSessionAttributes(sessionAttributes);

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(authService.getUserByToken(anyString())).thenThrow(new RuntimeException("Invalid token"));

        // When
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Then
        assertNotNull(result);
        assertEquals(message, result);

        // Verify that the authentication service was called with the invalid token
        verify(authService).getUserByToken("invalid-token");

    }

    @Test
    void inboundChannelInterceptor_preSend_withConnectCommand_noToken_logsError() {
        // Given
        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor> interceptorCaptor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        when(registration.interceptors(interceptorCaptor.capture())).thenReturn(registration);

        webSocketConfig.configureClientInboundChannel(registration);
        ChannelInterceptor interceptor = interceptorCaptor.getValue();

        // Create STOMP CONNECT message without token
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("test-session");

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Then
        assertNotNull(result);
        assertEquals(message, result);
    }

    @Test
    void inboundChannelInterceptor_preSend_withSubscribeCommand() {
        // Given
        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor> interceptorCaptor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        when(registration.interceptors(interceptorCaptor.capture())).thenReturn(registration);

        webSocketConfig.configureClientInboundChannel(registration);
        ChannelInterceptor interceptor = interceptorCaptor.getValue();

        // Create STOMP SUBSCRIBE message
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/test");

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Then
        assertNotNull(result);
        assertEquals(message, result);
    }

    @Test
    void inboundChannelInterceptor_preSend_withNullAccessor() {
        // Given
        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor> interceptorCaptor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        when(registration.interceptors(interceptorCaptor.capture())).thenReturn(registration);

        webSocketConfig.configureClientInboundChannel(registration);
        ChannelInterceptor interceptor = interceptorCaptor.getValue();

        // Create message without STOMP headers - use MessageHeaders constructor
        MessageHeaders headers = new MessageHeaders(new HashMap<>());
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], headers);

        // When
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Then
        assertNotNull(result);
        assertEquals(message, result);
    }

    @Test
    void inboundChannelInterceptor_preSend_withNullCommand() {
        // Given
        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor> interceptorCaptor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        when(registration.interceptors(interceptorCaptor.capture())).thenReturn(registration);

        webSocketConfig.configureClientInboundChannel(registration);
        ChannelInterceptor interceptor = interceptorCaptor.getValue();

        // Create a simple message without STOMP headers
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        // When
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Then
        assertNotNull(result);
        assertEquals(message, result);
    }

    @Test
    void outboundChannelInterceptor_preSend_logsMessage() {
        // Given
        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor> interceptorCaptor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        when(registration.interceptors(interceptorCaptor.capture())).thenReturn(registration);

        webSocketConfig.configureClientOutboundChannel(registration);
        ChannelInterceptor interceptor = interceptorCaptor.getValue();

        // Create STOMP MESSAGE
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.MESSAGE);
        accessor.setDestination("/topic/test");

        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // When
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Then
        assertNotNull(result);
        assertEquals(message, result);
    }

    @Test
    void outboundChannelInterceptor_preSend_withException_handlesGracefully() {
        // Given
        ChannelRegistration registration = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor> interceptorCaptor = ArgumentCaptor.forClass(ChannelInterceptor.class);
        when(registration.interceptors(interceptorCaptor.capture())).thenReturn(registration);

        webSocketConfig.configureClientOutboundChannel(registration);
        ChannelInterceptor interceptor = interceptorCaptor.getValue();

        // Create message using MessageBuilder fluent API
        Message<byte[]> message = MessageBuilder.withPayload(new byte[0]).build();

        // When
        Message<?> result = interceptor.preSend(message, messageChannel);

        // Then
        assertNotNull(result);
        assertEquals(message, result);
    }

    @Test
    void handshakeInterceptor_afterHandshake_withException_logsError() throws Exception {
        // Given
        // Reset the mock to ensure clean state
        reset(registry);

        // Create the ArgumentCaptor first
        ArgumentCaptor<HandshakeInterceptor> interceptorCaptor = ArgumentCaptor.forClass(HandshakeInterceptor.class);

        // Create local mocks for this test
        StompWebSocketEndpointRegistration localEndpointRegistration1 = mock(StompWebSocketEndpointRegistration.class);
        StompWebSocketEndpointRegistration localEndpointRegistration2 = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration localSockJsRegistration = mock(SockJsServiceRegistration.class);

        // Configure registry to return different registrations for consecutive calls
        when(registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game"))
                .thenReturn(localEndpointRegistration1, localEndpointRegistration2);

        // Configure first endpoint registration (SockJS) - capture the interceptor here
        when(localEndpointRegistration1.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.addInterceptors(interceptorCaptor.capture()))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.withSockJS())
                .thenReturn(localSockJsRegistration);

        // Configure second endpoint registration (Raw WebSocket)
        when(localEndpointRegistration2.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration2);

        // Set up SockJS registration chain
        when(localSockJsRegistration.setSessionCookieNeeded(false))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setWebSocketEnabled(true))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setDisconnectDelay(5000))
                .thenReturn(localSockJsRegistration);

        // Call the method
        webSocketConfig.registerStompEndpoints(registry);

        // Get the captured interceptor
        HandshakeInterceptor interceptor = interceptorCaptor.getValue();

        Exception testException = new RuntimeException("Test exception");

        // When
        interceptor.afterHandshake(request, response, wsHandler, testException);

        // Then - should not throw exception, just log
        assertTrue(true);
    }

    @Test
    void handshakeInterceptor_afterHandshake_withoutException() throws Exception {
        // Given
        // Reset the mock to ensure clean state
        reset(registry);

        // Create the ArgumentCaptor first
        ArgumentCaptor<HandshakeInterceptor> interceptorCaptor = ArgumentCaptor.forClass(HandshakeInterceptor.class);

        // Create local mocks for this test
        StompWebSocketEndpointRegistration localEndpointRegistration1 = mock(StompWebSocketEndpointRegistration.class);
        StompWebSocketEndpointRegistration localEndpointRegistration2 = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration localSockJsRegistration = mock(SockJsServiceRegistration.class);

        // Configure registry to return different registrations for consecutive calls
        when(registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game"))
                .thenReturn(localEndpointRegistration1, localEndpointRegistration2);

        // Configure first endpoint registration (SockJS) - capture the interceptor here
        when(localEndpointRegistration1.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.addInterceptors(interceptorCaptor.capture()))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.withSockJS())
                .thenReturn(localSockJsRegistration);

        // Configure second endpoint registration (Raw WebSocket)
        when(localEndpointRegistration2.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration2);

        // Set up SockJS registration chain
        when(localSockJsRegistration.setSessionCookieNeeded(false))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setWebSocketEnabled(true))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setDisconnectDelay(5000))
                .thenReturn(localSockJsRegistration);

        // Call the method
        webSocketConfig.registerStompEndpoints(registry);

        // Get the captured interceptor
        HandshakeInterceptor interceptor = interceptorCaptor.getValue();

        // When
        interceptor.afterHandshake(request, response, wsHandler, null);

        // Then - should not throw exception
        assertTrue(true);
    }

    @Test
    void handshakeInterceptor_beforeHandshake_withOptionsRequest_returnsTrue() throws Exception {
        // Given
        // Reset the mock to ensure clean state
        reset(registry);

        // Create the ArgumentCaptor first
        ArgumentCaptor<HandshakeInterceptor> interceptorCaptor = ArgumentCaptor.forClass(HandshakeInterceptor.class);

        // Create local mocks for this test
        StompWebSocketEndpointRegistration localEndpointRegistration1 = mock(StompWebSocketEndpointRegistration.class);
        StompWebSocketEndpointRegistration localEndpointRegistration2 = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration localSockJsRegistration = mock(SockJsServiceRegistration.class);

        // Configure registry to return different registrations for consecutive calls
        when(registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game"))
                .thenReturn(localEndpointRegistration1, localEndpointRegistration2);

        // Configure first endpoint registration (SockJS) - capture the interceptor here
        when(localEndpointRegistration1.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.addInterceptors(interceptorCaptor.capture()))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.withSockJS())
                .thenReturn(localSockJsRegistration);

        // Configure second endpoint registration (Raw WebSocket)
        when(localEndpointRegistration2.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration2);

        // Set up SockJS registration chain
        when(localSockJsRegistration.setSessionCookieNeeded(false))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setWebSocketEnabled(true))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setDisconnectDelay(5000))
                .thenReturn(localSockJsRegistration);

        // Call the method
        webSocketConfig.registerStompEndpoints(registry);

        // Get the captured interceptor
        HandshakeInterceptor interceptor = interceptorCaptor.getValue();

        // Mock ServletServerHttpRequest for OPTIONS request
        when(servletRequest.getServletRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getMethod()).thenReturn("OPTIONS");

        // When
        boolean result = interceptor.beforeHandshake(servletRequest, response, wsHandler, attributes);

        // Then
        assertTrue(result);
    }

    @Test
    void handshakeInterceptor_beforeHandshake_withNoToken_returnsFalse() throws Exception {
        // Given
        // Reset the mock to ensure clean state
        reset(registry);

        // Create the ArgumentCaptor first
        ArgumentCaptor<HandshakeInterceptor> interceptorCaptor = ArgumentCaptor.forClass(HandshakeInterceptor.class);

        // Create local mocks for this test
        StompWebSocketEndpointRegistration localEndpointRegistration1 = mock(StompWebSocketEndpointRegistration.class);
        StompWebSocketEndpointRegistration localEndpointRegistration2 = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration localSockJsRegistration = mock(SockJsServiceRegistration.class);

        // Configure registry to return different registrations for consecutive calls
        when(registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game"))
                .thenReturn(localEndpointRegistration1, localEndpointRegistration2);

        // Configure first endpoint registration (SockJS) - capture the interceptor here
        when(localEndpointRegistration1.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.addInterceptors(interceptorCaptor.capture()))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.withSockJS())
                .thenReturn(localSockJsRegistration);

        // Configure second endpoint registration (Raw WebSocket)
        when(localEndpointRegistration2.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration2);

        // Set up SockJS registration chain
        when(localSockJsRegistration.setSessionCookieNeeded(false))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setWebSocketEnabled(true))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setDisconnectDelay(5000))
                .thenReturn(localSockJsRegistration);

        // Call the method
        webSocketConfig.registerStompEndpoints(registry);

        // Get the captured interceptor
        HandshakeInterceptor interceptor = interceptorCaptor.getValue();

        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);

        // When
        boolean result = interceptor.beforeHandshake(request, response, wsHandler, attributes);

        // Then
        assertFalse(result);
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void handshakeHandler_determineUser_withNoAttributes_returnsNull() throws Exception {
        // Given
        // Reset the mock to ensure clean state
        reset(registry);

        // Create the ArgumentCaptor first
        ArgumentCaptor<DefaultHandshakeHandler> handlerCaptor = ArgumentCaptor.forClass(DefaultHandshakeHandler.class);

        // Create local mocks for this test
        StompWebSocketEndpointRegistration localEndpointRegistration1 = mock(StompWebSocketEndpointRegistration.class);
        StompWebSocketEndpointRegistration localEndpointRegistration2 = mock(StompWebSocketEndpointRegistration.class);
        SockJsServiceRegistration localSockJsRegistration = mock(SockJsServiceRegistration.class);

        // Configure registry to return different registrations for consecutive calls
        when(registry.addEndpoint("/ws/lobby", "/ws/lobby-manager", "/ws/user", "/ws/friend", "/ws/game"))
                .thenReturn(localEndpointRegistration1, localEndpointRegistration2);

        // Configure first endpoint registration (SockJS) - capture the handler here
        when(localEndpointRegistration1.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.setHandshakeHandler(handlerCaptor.capture()))
                .thenReturn(localEndpointRegistration1);
        when(localEndpointRegistration1.withSockJS())
                .thenReturn(localSockJsRegistration);

        // Configure second endpoint registration (Raw WebSocket)
        when(localEndpointRegistration2.setAllowedOriginPatterns("http://localhost:3000", "https://sopra-fs25-group-15-client.vercel.app"))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.addInterceptors(any(HandshakeInterceptor.class)))
                .thenReturn(localEndpointRegistration2);
        when(localEndpointRegistration2.setHandshakeHandler(any(DefaultHandshakeHandler.class)))
                .thenReturn(localEndpointRegistration2);

        // Set up SockJS registration chain
        when(localSockJsRegistration.setSessionCookieNeeded(false))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setClientLibraryUrl("https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setWebSocketEnabled(true))
                .thenReturn(localSockJsRegistration);
        when(localSockJsRegistration.setDisconnectDelay(5000))
                .thenReturn(localSockJsRegistration);

        // Call the method
        webSocketConfig.registerStompEndpoints(registry);

        // Get the captured handler
        DefaultHandshakeHandler handler = handlerCaptor.getValue();

        Map<String, Object> attrs = new HashMap<>();

        // When - Use reflection to call the protected method
        Method determineUserMethod = null;
        Class<?> handlerClass = handler.getClass();

        // Search through all methods including inherited ones
        while (handlerClass != null && determineUserMethod == null) {
            for (Method method : handlerClass.getDeclaredMethods()) {
                if (method.getName().equals("determineUser") &&
                        method.getParameterCount() == 3 &&
                        Principal.class.isAssignableFrom(method.getReturnType())) {
                    determineUserMethod = method;
                    break;
                }
            }
            handlerClass = handlerClass.getSuperclass();
        }

        if (determineUserMethod == null) {
            throw new NoSuchMethodException("Could not find determineUser method");
        }

        determineUserMethod.setAccessible(true);
        Principal principal = (Principal) determineUserMethod.invoke(handler, request, wsHandler, attrs);

        // Then
        assertNull(principal);
    }
}