// multi-test-client.js

/**
 * Simulates two players in the same lobby:
 *  - Player A (host): create & start game
 *  - Player B: join & play
 *
 * Fill in TOKEN_A and TOKEN_B with your raw UUID tokens.
 */

const axios      = require('axios');
const { Client } = require('@stomp/stompjs');
const WebSocket  = require('ws');

const REST_BASE = 'http://localhost:8080';
const WS_URL    = 'ws://localhost:8080/ws/lobby';

const TOKEN_A   = 'f2b94004-c2ce-488d-b19a-cca974aaae77';  // host
const TOKEN_B   = '54379f73-d438-4a4e-87d2-9baf33a74385';  // guest

async function main() {
  // 1) Host creates lobby
  const { data: lobby } = await axios.post(
    `${REST_BASE}/lobbies`,
    { maxPlayers: 4, playersPerTeam: 1 },
    { headers: { Authorization: TOKEN_A } }
  );
  console.log('Lobby created:', lobby);
  const LOBBY_ID   = lobby.lobbyId;
  const LOBBY_CODE = lobby.code;

  // 2) Setup STOMP clients
  const clients = {};

  ['A','B'].forEach(letter => {
    const token = letter==='A'? TOKEN_A : TOKEN_B;
    const client = new Client({
      webSocketFactory: () =>
        new WebSocket(WS_URL, { headers: { Authorization: token } }),
      reconnectDelay: 5000,
      debug: str => console.log(`[${letter}] STOMP:`, str),
      onConnect: () => {
        console.log(`[${letter}] connected over WS`);

        // Subscribe to game events
        client.subscribe(
          `/topic/lobby/${LOBBY_ID}/game`,
          msg => {
            const evt = JSON.parse(msg.body);
            console.log(`[${letter}] ◀`, evt);

            if (evt.type === 'ROUND_START') {
              // send a random guess each round
              setTimeout(() => {
                const guess = Math.floor(Math.random()*100)+1;
                client.publish({
                  destination: `/app/lobby/${LOBBY_ID}/game/guess`,
                  body: JSON.stringify({ guess })
                });
                console.log(`[${letter}] ▶ GUESS Round ${evt.round}:`, guess);
              }, 500 + Math.random()* (evt.roundTime*1000 - 500));
            }

            // Host starts game once both are in
            if (letter==='A' && evt.type === 'ROUND_START' && evt.round===1) {
              // nothing
            }
          }
        );

        // Join lobby
        client.publish({
          destination: `/app/lobby/join/${LOBBY_CODE}`,
          body: ''
        });
        console.log(`[${letter}] ▶ Joined lobby`);

        // Host kicks off game
        if (letter === 'A') {
          setTimeout(() => {
            const payload = { roundCount: 3, roundTime: 8 };
            client.publish({
              destination: `/app/lobby/${LOBBY_ID}/game/start`,
              body: JSON.stringify(payload)
            });
            console.log(`[A] ▶ START_GAME`, payload);
          }, 1000);
        }
      },
      onStompError: frame => {
        console.error(`[${letter}] STOMP ERR`, frame.headers, frame.body);
      }
    });

    clients[letter] = client;
    client.activate();
  });
}

main().catch(console.error);
