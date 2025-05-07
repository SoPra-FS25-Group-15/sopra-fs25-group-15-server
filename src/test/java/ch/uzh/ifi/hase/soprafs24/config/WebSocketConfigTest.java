package ch.uzh.ifi.hase.soprafs24.config;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.service.AuthService;
import ch.uzh.ifi.hase.soprafs24.websocket.WebSocketErrorHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {

    @Mock
    private MessageBrokerRegistry messageBrokerRegistry;

    @Mock
    private StompEndpointRegistry stompEndpointRegistry;

    @Mock
    private AuthService authService;

    @Mock
    private WebSocketErrorHandler webSocketErrorHandler;

    @Mock
    private ChannelRegistration channelRegistration;

    @Mock
    private ServerHttpRequest serverHttpRequest;

    @Mock
    private ServerHttpResponse serverHttpResponse;

    @Mock
    private WebSocketHandler webSocketHandler;

    @Mock
    private ServletServerHttpRequest servletServerHttpRequest;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private MessageChannel messageChannel;

    @Mock
    private Message<?> message;

    @Mock
    private StompHeaderAccessor stompHeaderAccessor;

    @Mock
    private ThreadPoolTaskScheduler taskScheduler;

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setToken("test-token");
        testUser.setUsername("testUser");

        // Setup mock for servletServerHttpRequest
        when(servletServerHttpRequest.getServletRequest()).thenReturn(httpServletRequest);
    }

    @Test
    void configureClientInboundChannel() {
        // Test the method
        webSocketConfig.configureClientInboundChannel(channelRegistration);

        // Verify that an interceptor was added
        verify(channelRegistration).interceptors(any());
    }

    @Test
    void configureClientOutboundChannel() {
        // Test the method
        webSocketConfig.configureClientOutboundChannel(channelRegistration);

        // Verify that an interceptor was added
        verify(channelRegistration).interceptors(any());
    }
}