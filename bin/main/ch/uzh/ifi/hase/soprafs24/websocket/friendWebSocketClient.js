/**
 * Requirements:
 * 1. Test Users:
 *    - "alice": Recipient for the friend request.
 *    - "bob": Has already sent a friend request to the current user ("John").
 *    - "charlie": The current user ("John") has sent a friend request to "charlie" so it can be canceled.
 *    - "dave": Already friends with the current user ("John") so the remove friend operation is valid.
 *
 * 2. Token:
 *    - Replace 'YOUR_BEARER_TOKEN_HERE' with a valid token for "John".
 *
 * 3. WebSocket Endpoint:
 *    - Ensure your server’s WebSocket endpoint is registered for "/ws/friend".
 *
 * 4. Operations:
 *    a) Send a friend request to "alice".
 *    b) Accept an incoming friend request from "bob".
 *    c) Cancel the sent friend request to "charlie".
 *    d) Remove existing friend "dave".
 */

const { Client } = require('@stomp/stompjs');
const WebSocket = require('ws');

// Replace with a valid token for "John"
const YOUR_TOKEN = '98f23eb3-ecef-44a7-a4ac-4375395ec2d5';

const friendClient = new Client({
  brokerURL: `ws://localhost:8080/ws/friend?token=${encodeURIComponent(YOUR_TOKEN)}`, // Add query parameter token as an alternative authentication method
  connectHeaders: {
    Authorization: `Bearer ${YOUR_TOKEN}`
  },
  webSocketFactory: () =>
    new WebSocket(`ws://localhost:8080/ws/friend?token=${encodeURIComponent(YOUR_TOKEN)}`, {
      headers: {
        Authorization: `Bearer ${YOUR_TOKEN}`
      }
    }),
  debug: (str) => console.log('[Friend STOMP]', str),
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
  reconnectDelay: 5000
});

friendClient.onConnect = (frame) => {
  console.log('Connected to Friend WS:', frame);

  // Update subscriptions to match the user destination prefix "/user"
  friendClient.subscribe('/user/topic/friend/incoming', (msg) => {
    console.log('<< Incoming Friend Request >>', msg.body);
  });
  friendClient.subscribe('/user/topic/friend/requestOut/result', (msg) => {
    console.log('<< Request Out Result >>', msg.body);
  });
  friendClient.subscribe('/user/topic/friend/requestResponse/result', (msg) => {
    console.log('<< Request Response Result >>', msg.body);
  });
  friendClient.subscribe('/user/topic/friend/cancelRequest/result', (msg) => {
    console.log('<< Cancel Request Result >>', msg.body);
  });
  friendClient.subscribe('/user/topic/friend/removeFriend/result', (msg) => {
    console.log('<< Remove Friend Result >>', msg.body);
  });
  friendClient.subscribe('/user/topic/friend/requestResponse', (msg) => {
    console.log('<< Friend Request Response Notification >>', msg.body);
  });

  // --- Testing Operations ---

  // a) Send a friend request to "alice"
  friendClient.publish({
    destination: '/app/friend/requestOut',
    body: JSON.stringify({
      type: 'REQUEST_OUT',
      payload: {
        token: YOUR_TOKEN,
        toUsername: 'alice'
      }
    })
  });

  // b) Accept a friend request from "bob"
  setTimeout(() => {
    friendClient.publish({
      destination: '/app/friend/requestResponse',
      body: JSON.stringify({
        type: 'REQUEST_RESPONSE',
        payload: {
          token: YOUR_TOKEN,
          toUsername: 'bob',
          accept: true
        }
      })
    });
  }, 3000);

  // c) Cancel the friend request sent to "charlie"
  setTimeout(() => {
    friendClient.publish({
      destination: '/app/friend/cancelRequest',
      body: JSON.stringify({
        type: 'CANCEL_REQUEST',
        payload: {
          token: YOUR_TOKEN,
          toUsername: 'charlie'
        }
      })
    });
  }, 5000);

  // d) Remove an existing friend "dave"
  setTimeout(() => {
    friendClient.publish({
      destination: '/app/friend/removeFriend',
      body: JSON.stringify({
        type: 'REMOVE_FRIEND',
        payload: {
          token: YOUR_TOKEN,
          toUsername: 'dave'
        }
      })
    });
  }, 7000);
};

friendClient.onStompError = (frame) => {
  console.error('Broker error:', frame.headers['message']);
  console.error(frame.body);
};

friendClient.activate();
