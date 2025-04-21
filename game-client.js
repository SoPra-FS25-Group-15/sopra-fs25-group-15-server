const axios      = require('axios');
const { Client } = require('@stomp/stompjs');
const WebSocket  = require('ws');
const fs         = require('fs');
const path       = require('path');

const REST_BASE = 'http://localhost:8080';
const WS_URL    = 'ws://localhost:8080/ws/lobby';

// Configure log file paths - create separate log files for each run
const LOG_DIR = path.join(__dirname, 'logs');
const TIMESTAMP = new Date().toISOString().replace(/:/g, '-').replace(/\./g, '-');
const LOG_FILE = path.join(LOG_DIR, `game-${TIMESTAMP}.log`);
const STATE_LOG_FILE = path.join(LOG_DIR, `game-states-${TIMESTAMP}.json`);

// Create logs directory if it doesn't exist
if (!fs.existsSync(LOG_DIR)) {
  fs.mkdirSync(LOG_DIR, { recursive: true });
}

// Initialize log files with headers
fs.writeFileSync(LOG_FILE, `=== GAME CLIENT LOG STARTED AT ${new Date().toISOString()} ===\n\n`);
fs.writeFileSync(STATE_LOG_FILE, '[\n');  // Start a JSON array for state tracking
let stateLogCount = 0;  // To track when to add commas between JSON entries

// Logger utility that writes to both console and file with optional JSON formatting
function logger(message, level = 'INFO', jsonData = null) {
  const timestamp = new Date().toISOString();
  const logMessage = `[${timestamp}] [${level}] ${message}`;
  
  // Log to console
  console.log(logMessage);
  
  // Log to file (append mode)
  fs.appendFileSync(LOG_FILE, logMessage + '\n');
  
  // Log JSON data if provided
  if (jsonData) {
    const jsonOutput = typeof jsonData === 'string' ? jsonData : JSON.stringify(jsonData, null, 2);
    fs.appendFileSync(LOG_FILE, jsonOutput + '\n\n');
    
    // Also store in the state log if it's relevant game state data
    if (level === 'STATE' || level === 'EVENT') {
      if (stateLogCount > 0) {
        fs.appendFileSync(STATE_LOG_FILE, ',\n');
      }
      
      const stateEntry = {
        timestamp,
        type: level,
        data: jsonData
      };
      
      fs.appendFileSync(STATE_LOG_FILE, JSON.stringify(stateEntry, null, 2));
      stateLogCount++;
    }
  }
  
  return logMessage; // Return for chaining
}

// Replace these with your actual tokens
const TOKEN_A = '9826db28-d104-4b24-bab1-4b248eb02cff';
const TOKEN_B = 'cecde776-b87f-4137-807a-17cb34172b9b';

// Track which players have attempted to play action cards in the current round
const actionCardAttempted = {
  A: false,
  B: false
};

// Reset action card tracking for new rounds
function resetActionCardTracking() {
  actionCardAttempted.A = false;
  actionCardAttempted.B = false;
  logger("Reset action card tracking for new round");
}

// Track whether a guess has been submitted in the current round
const guessSubmitted = {
  A: false,
  B: false
};

// Track guess timeouts to clear if needed
const guessTimeouts = {
  A: null,
  B: null
};

// Reset guess tracking for new rounds
function resetGuessTracking() {
  guessSubmitted.A = false;
  guessSubmitted.B = false;
  
  // Clear any existing timeouts
  if (guessTimeouts.A) {
    clearTimeout(guessTimeouts.A);
    guessTimeouts.A = null;
  }
  if (guessTimeouts.B) {
    clearTimeout(guessTimeouts.B);
    guessTimeouts.B = null;
  }
  
  logger("Reset guess tracking for new round");
}

// Track command locks to prevent duplicate submissions PER PLAYER
const commandLocks = {
  roundCardSelection: {
    A: false,
    B: false
  },
  actionCardPlay: {
    A: false,
    B: false
  },
  guessSubmission: {
    A: false,
    B: false
  }
};

// Add helper to reset all command locks between rounds
function resetCommandLocks() {
  for (const letter of ['A', 'B']) {
    commandLocks.actionCardPlay[letter] = false;
    commandLocks.guessSubmission[letter] = false;
    commandLocks.roundCardSelection[letter] = false;
  }
  logger("Reset all command locks for new round", "RESET");
}

// Track current game progression to debug issues
const gameProgress = {
  currentRound: 0,
  expectedNextRound: 1,
  lastScreen: null,
  lastAction: null,
  history: []
};

// Store complete game history for analysis
const gameHistory = {
  events: [],
  states: [],
  roundsPlayed: 0,
  playerACards: [],
  playerBCards: [],
  winner: null
};

// Add new function to record events in game history
function recordGameEvent(eventType, data) {
  const timestamp = new Date().toISOString();
  const event = {
    timestamp,
    type: eventType,
    data
  };
  
  gameHistory.events.push(event);
  logger(`Recorded game event: ${eventType}`, 'EVENT', data);
}

// Add function to record game states in history
function recordGameState(source, state) {
  const timestamp = new Date().toISOString();
  const stateRecord = {
    timestamp,
    source,
    state: JSON.parse(JSON.stringify(state)) // Deep clone to avoid reference issues
  };
  
  gameHistory.states.push(stateRecord);
  logger(`Recorded game state from ${source}`, 'STATE', state);
}

// local mirror of server state - updated to match actual server structure using tokens
const gameState = {
  // Per-player information
  playerA: { 
    token: TOKEN_A,
    roundCards: [], 
    actionCards: [], 
    isActive: false
  },
  playerB: { 
    token: TOKEN_B,
    roundCards: [], 
    actionCards: [], 
    isActive: false
  },
  // Game state
  currentRound: 0,
  currentScreen: null, // 'ROUNDCARD', 'ACTIONCARD', 'GUESS', 'REVEAL', 'GAMEOVER'
  activePlayerToken: null,
  roundCardInPlay: null,
  // Action cards played in current round
  actionCardsPlayed: {},
  // Guess screen attributes - more closely matches server format
  guessScreenAttributes: {
    time: 0,
    guessLocation: null,
    resolveResponse: null
  },
  // NEW: Add round winner tracking
  roundWinners: [],
  roundWinnerDistances: [],
  gameWinner: null
};

// Add these variables to track game state timing and recovery
const gameStateTracker = {
  lastScreenChange: Date.now(),
  currentScreen: null,
  stuckDetectionTimeout: null,
  recoveryAttempts: 0
};

// Add state tracking for lobby operations
const lobbyState = {
  playerAJoined: false,
  playerBJoined: false,
  lobbyCreated: false,
  gameStarted: false,
  gameStartAttempted: false,
  playerCount: 0,
  maxPlayers: 2, // Default to 2 players
  joinedPlayerTokens: []
};

// Add timer tracking variables for guess phase
const guessTimers = {
  startTime: null,
  timeLimit: 0,
  remainingTime: 0,
  timerActive: false,
  timerInterval: null
};

// Clean console output by reducing verbosity and adding organization
function clearConsole() {
  const separator = '\n\n\n\n\n';
  console.log(separator);
  // Also add a separator to the log file
  fs.appendFileSync(LOG_FILE, separator);
}

// Add more structured console output with timestamps
function logMessage(letter, type, message) {
  const timestamp = new Date().toISOString().substr(11, 8);
  const formattedMessage = `[${timestamp}] [${letter}] [${type}] ${message}`;
  
  // Log to console
  console.log(formattedMessage);
  
  // Log to file
  fs.appendFileSync(LOG_FILE, formattedMessage + '\n');
}

// Function to safely wait between actions
function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

function generateGuess(coords) {
  const off = Math.random() * 10;
  return {
    lat: Math.min(90, Math.max(-90, coords.lat + (Math.random() > 0.5 ? off : -off))),
    lon: Math.min(180, Math.max(-180, coords.lon + (Math.random() > 0.5 ? off : -off)))
  };
}

// Global storage for active clients and lobby ID
const activeClients = {};
let currentLobbyId = null;

// Add global variable to store the code for Player B
let CLIENT_B_JOIN_CODE = null;

// Update direct DTO type detection to handle structured broadcasts
function isStructuredBroadcast(body) {
  return body && typeof body === 'object' && 
         body.type && (body.type === 'ROUND_WINNER' || body.type === 'GAME_WINNER');
}

// Add timer variables to manage phase timings
const phaseTimers = {
  actionCardPhase: null,
  guessingPhase: null,
  roundEndDelay: null
};

// Clear all timers to prevent memory leaks and inconsistent behavior
function clearAllTimers() {
  Object.values(phaseTimers).forEach(timer => {
    if (timer) clearTimeout(timer);
  });
  Object.keys(phaseTimers).forEach(key => phaseTimers[key] = null);
  
  // Also clear guess timeouts
  if (guessTimeouts.A) clearTimeout(guessTimeouts.A);
  if (guessTimeouts.B) clearTimeout(guessTimeouts.B);
  guessTimeouts.A = null;
  guessTimeouts.B = null;
  
  // Clear the guess timer interval if it exists
  if (guessTimers.timerInterval) {
    clearInterval(guessTimers.timerInterval);
    guessTimers.timerInterval = null;
  }
  
  // Reset guess timer state
  guessTimers.timerActive = false;
  
  logger("Cleared all timers", "TIMER");
}

/**
 * Signal to the server that round time has expired
 * and request transition to the round winner determination
 */
function signalRoundTimeExpired(letter, client, lobbyId) {
  // Only proceed if we're still in the guessing phase
  if (gameState.currentScreen !== "GUESS") {
    logger(`[${letter}] Not signaling round time expired - already in ${gameState.currentScreen} phase`, "INFO");
    return;
  }

  logger(`[${letter}] Signaling round time expired to determine round winner`, "PHASE");
  
  recordGameEvent('ROUND_TIME_EXPIRED', {
    player: letter
  });
  
  // First make sure all players submit their guesses
  ensureGuessSubmitted('A');
  ensureGuessSubmitted('B');
  
  // Add a small delay after ensuring guesses to allow them to be processed
  setTimeout(() => {
    // Send message to the server endpoint for round time expiration
    client.publish({
      destination: `/app/lobby/${lobbyId}/game/round-time-expired`,
      body: JSON.stringify({}),
      headers: { 
        Authorization: `Bearer ${letter === 'A' ? TOKEN_A : TOKEN_B}`
      }
    });
    
    logger(`[${letter}] Sent round-time-expired message to server`, "PHASE");
    
    // Set a timeout to verify we received the round winner
    // If after 5 seconds we haven't transitioned to REVEAL, request game state
    setTimeout(() => {
      if (gameState.currentScreen === "GUESS") {
        logger(`[${letter}] No transition after round time expired - requesting game state`, "RETRY");
        
        // Request current game state
        client.publish({
          destination: `/app/lobby/${lobbyId}/game/state`,
          body: JSON.stringify({}),
          headers: { 
            Authorization: `Bearer ${letter === 'A' ? TOKEN_A : TOKEN_B}`
          }
        });
        
        // Set up a more aggressive fallback if still no transition after another 3 seconds
        setTimeout(() => {
          if (gameState.currentScreen === "GUESS") {
            logger(`[${letter}] Still no transition - forcing local transition to REVEAL and waiting for winner`, "RECOVERY");
            
            // Force transition locally to REVEAL while waiting for the winner data
            gameState.currentScreen = "REVEAL";
            
            // Request game state again to try getting the winner information
            client.publish({
              destination: `/app/lobby/${lobbyId}/game/state`,
              body: JSON.stringify({}),
              headers: { 
                Authorization: `Bearer ${letter === 'A' ? TOKEN_A : TOKEN_B}`
              }
            });
          }
        }, 3000);
      }
    }, 5000);
  }, 500);
}

/**
 * Set up WebSocket clients for players A and B
 */
function setupWebSocketClients(LID, CODE) {
  currentLobbyId = LID;
  logger(`Setting global lobby ID: ${LID}, joining code: ${CODE}`, "SETUP");
  
  // Initialize game state tracker
  gameStateTracker.lastScreenChange = Date.now();
  gameStateTracker.currentScreen = null;
  gameStateTracker.recoveryAttempts = 0;
  
  // Set lobby state
  lobbyState.lobbyCreated = true;
  
  for (const letter of ['A', 'B']) {
    const token = letter === 'A' ? TOKEN_A : TOKEN_B;
    
    // Create a resilient client with token-based auth
    const client = new Client({
      webSocketFactory: () => new WebSocket(WS_URL, { 
        headers: { 
          Authorization: `Bearer ${token}` // Ensure Bearer prefix is used
        }
      }),
      connectHeaders: {
        Authorization: `Bearer ${token}`,  // Use Bearer prefix consistently
        token: token                       // Also include plain token as fallback
      },
      reconnectDelay: 3000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: str => {
        if (str.includes('error') || str.includes('fail')) {
          logger(`[${letter}] STOMP Debug: ${str}`, "DEBUG");
        }
      }
    });

    client.beforeConnect = () => {
      logger(`[${letter}] Connecting to WebSocket...`, "CONNECT");
    };

    client.onConnect = () => {
      logger(`[${letter}] CONNECTED to WebSocket server`, "CONNECT");
      
      try {
        // Game events subscription - This handles all broadcast messages
        client.subscribe(`/topic/lobby/${LID}/game`, msg => {
          try {
            const body = JSON.parse(msg.body);
            
            // Log raw message in debug mode for troubleshooting
            logger(`[${letter}] Raw WebSocket message: ${JSON.stringify(body)}`, "DEBUG");
            
            // Special handling for structured broadcast messages
            if (body && body.type) {
              if (isStructuredBroadcast(body)) {
                logger(`[${letter}] <<< STRUCTURED BROADCAST: ${body.type}`, "BROADCAST");
              } else {
                logger(`[${letter}] <<< TOPIC: ${body.type || 'UNKNOWN'}`);
              }
              
              if (body.type === "ERROR" && body.payload) {
                if (typeof body.payload === 'string' && body.payload.includes("already played")) {
                  actionCardAttempted[letter] = true;
                  logger(`[${letter}] Server rejected action card - already played`, "WARN");
                } else {
                  logger(`[${letter}] Server error: ${JSON.stringify(body.payload)}`, "ERROR");
                }
                return;
              }
              
              handleEvent(letter, body, client, LID);
            } else {
              logger(`[${letter}] Received message with invalid format (missing type)`, "WARN");
            }
          } catch (e) {
            logger(`[${letter}] Error processing topic message: ${e.message}`, "ERROR");
            logger(`Raw message body: ${msg.body}`, "ERROR");
          }
        }, { id: `topic-${letter}-${Date.now()}` });

        // Game state subscription - Personal game state updates
        client.subscribe(`/user/queue/lobby/${LID}/game/state`, msg => {
          try {
            const body = JSON.parse(msg.body);
            logger(`[${letter}] Received state message: ${body.type}`, "STATE");
            
            if (body.type === 'GAME_STATE') {
              updateGameState(letter, body.payload, client, LID);
            }
          } catch (e) {
            logger(`[${letter}] Error processing state message: ${e.message}`, "ERROR");
          }
        });
        
        // Action card subscription - For receiving personal action cards
        client.subscribe(`/user/queue/lobby/${LID}/game/action-card`, msg => {
          try {
            const body = JSON.parse(msg.body);
            logger(`[${letter}] <<< ACTION CARD: ${body.type}`);
            
            if (body.type === 'ACTION_CARD_ASSIGNED' || body.type === 'ACTION_CARD_REPLACEMENT') {
              const actionCardId = body.payload?.id;
              if (actionCardId) {
                if (letter === 'A') {
                  gameState.playerA.actionCards = [actionCardId];
                } else {
                  gameState.playerB.actionCards = [actionCardId];
                }
                logger(`[${letter}] Received action card: ${actionCardId}`);
                
                // Record in game history
                recordGameEvent('ACTION_CARD_RECEIVED', {
                  player: letter,
                  cardId: actionCardId,
                  type: body.type
                });
              }
            }
          } catch (e) {
            logger(`[${letter}] Error processing action card: ${e.message}`, "ERROR");
          }
        }, { id: `action-${letter}-${Date.now()}` });
        
        // Subscribe to lobby users - Important for tracking who's joined
        client.subscribe(`/topic/lobby/${LID}/users`, msg => {
          try {
            const body = JSON.parse(msg.body);
            logger(`[${letter}] <<< LOBBY USERS: ${body.type}`);
            
            if (body.type === 'USER_JOINED') {
              logger(`[${letter}] User joined lobby: ${JSON.stringify(body.payload)}`);
              
              // Increment player count and track joined players
              lobbyState.playerCount++;
              
              // Determine which player joined
              const username = body.payload.username;
              if (username && username.toLowerCase().includes('a')) {
                lobbyState.playerAJoined = true;
                logger(`[${letter}] Player A confirmed as joined to the lobby`, "JOIN");
                if (!lobbyState.joinedPlayerTokens.includes(TOKEN_A)) {
                  lobbyState.joinedPlayerTokens.push(TOKEN_A);
                }
              } else if (username && username.toLowerCase().includes('b')) {
                lobbyState.playerBJoined = true;
                logger(`[${letter}] Player B confirmed as joined to the lobby`, "JOIN");
                if (!lobbyState.joinedPlayerTokens.includes(TOKEN_B)) {
                  lobbyState.joinedPlayerTokens.push(TOKEN_B);
                }
              }
              
              // Record in game history
              recordGameEvent('USER_JOINED', {
                player: letter,
                user: body.payload,
                lobbyState: JSON.parse(JSON.stringify(lobbyState))
              });
              
              // Log lobby state
              logger(`Lobby state after user joined: playerAJoined=${lobbyState.playerAJoined}, playerBJoined=${lobbyState.playerBJoined}, count=${lobbyState.playerCount}/${lobbyState.maxPlayers}`, "LOBBY");
              
              // If both players have joined and game hasn't started, let Player A start the game
              if (letter === 'A' && lobbyState.playerAJoined && lobbyState.playerBJoined && 
                  !lobbyState.gameStarted && !lobbyState.gameStartAttempted) {
                // Allow a short delay for the server to fully process both joins
                setTimeout(() => {
                  startGameWhenReady(client, LID);
                }, 2000);
              }
            } else if (body.type === 'USER_LEFT') {
              logger(`[${letter}] User left lobby: ${JSON.stringify(body.payload)}`);
              
              // Decrement player count
              lobbyState.playerCount = Math.max(0, lobbyState.playerCount - 1);
              
              // Record in game history
              recordGameEvent('USER_LEFT', {
                player: letter,
                user: body.payload
              });
            }
          } catch (e) {
            logger(`[${letter}] Error processing lobby users message: ${e.message}`, "ERROR");
          }
        });

        // Add specific subscription for join results
        client.subscribe(`/user/topic/lobby/join/result`, msg => {
          try {
            const body = JSON.parse(msg.body);
            logger(`[${letter}] <<< JOIN RESULT: ${body.type}`);
            
            if (body.type === 'JOIN_SUCCESS') {
              if (letter === 'B') {
                lobbyState.playerBJoined = true;
                logger(`[${letter}] Player B join confirmed`, "JOIN");
                
                // Update the joined player tokens list
                if (!lobbyState.joinedPlayerTokens.includes(TOKEN_B)) {
                  lobbyState.joinedPlayerTokens.push(TOKEN_B);
                }
                
                recordGameEvent('JOIN_SUCCESS', {
                  player: 'B', 
                  lobbyState: JSON.parse(JSON.stringify(lobbyState))
                });
              } else if (letter === 'A') {
                lobbyState.playerAJoined = true;
                logger(`[${letter}] Player A join confirmed`, "JOIN");
                
                // Update the joined player tokens list
                if (!lobbyState.joinedPlayerTokens.includes(TOKEN_A)) {
                  lobbyState.joinedPlayerTokens.push(TOKEN_A);
                }
                
                recordGameEvent('JOIN_SUCCESS', {
                  player: 'A', 
                  lobbyState: JSON.parse(JSON.stringify(lobbyState))
                });
              }
              
              // Log lobby state
              logger(`Lobby state after join success: playerAJoined=${lobbyState.playerAJoined}, playerBJoined=${lobbyState.playerBJoined}, count=${lobbyState.joinedPlayerTokens.length}/${lobbyState.maxPlayers}`, "LOBBY");
              
              // Only try to start game if we're player A and both players joined
              if (letter === 'A' && lobbyState.playerAJoined && lobbyState.playerBJoined && 
                  !lobbyState.gameStarted && !lobbyState.gameStartAttempted) {
                // Allow a short delay for the server to fully process both joins
                setTimeout(() => {
                  startGameWhenReady(client, LID);
                }, 2000);
              }
            } else if (body.type === 'JOIN_ERROR') {
              logger(`[${letter}] Failed to join lobby: ${JSON.stringify(body.payload)}`, "ERROR");
              
              // If player B couldn't join, try again using a better approach
              if (letter === 'B' && !lobbyState.playerBJoined) {
                setTimeout(() => {
                  logger(`[B] Retrying WebSocket join to lobby with code: ${CODE}`, "RETRY");
                  
                  // Match the correct controller endpoint format
                  client.publish({ 
                    destination: `/app/lobby/join/${CODE}`,
                    body: JSON.stringify({}), // Empty payload - controller doesn't expect data here
                    headers: { 
                      Authorization: `Bearer ${token}`
                    }
                  });
                }, 2000);
              }
            }
          } catch (e) {
            logger(`[${letter}] Error processing join result message: ${e.message}`, "ERROR");
          }
        });

        // Store client reference globally
        activeClients[letter] = client;
        
        // Join lobby (using correct endpoint from LobbyWebSocketController)
        if (letter === 'A') {
          // Player A is the host, mark them as joined
          lobbyState.playerAJoined = true;
          
          // Player A creates and hosts the lobby, so joins automatically
          if (!lobbyState.joinedPlayerTokens.includes(TOKEN_A)) {
            lobbyState.joinedPlayerTokens.push(TOKEN_A);
          }
          
          // Request state to ensure we're connected
          setTimeout(() => {
            client.publish({
              destination: `/app/lobby/${LID}/game/state`,
              body: JSON.stringify({}),
              headers: { 
                Authorization: `Bearer ${TOKEN_A}`  // Add auth header to all requests
              }
            });
          }, 1000);
        } else {
          // Player B needs to join the lobby using the code
          recordGameEvent('JOINING_LOBBY', { player: 'B', code: CODE });
          
          // Have player B explicitly join the lobby with simplified payload
          client.publish({ 
            destination: `/app/lobby/join/${CODE}`,
            body: JSON.stringify({}), // Empty payload - the code in the URL is sufficient
            headers: { 
              Authorization: `Bearer ${TOKEN_B}`
            }
          });
          logger(`[B] Joining lobby with code: ${CODE}`);
          
          // Add more aggressive retries with exponential backoff
          [1000, 3000, 7000].forEach((delay) => {
            setTimeout(() => {
              if (!lobbyState.playerBJoined) {
                logger(`[B] Retry ${delay/1000}s: Joining lobby with code: ${CODE}`, "RETRY");
                client.publish({ 
                  destination: `/app/lobby/join/${CODE}`,
                  body: JSON.stringify({}),
                  headers: { 
                    Authorization: `Bearer ${TOKEN_B}`
                  }
                });
              }
            }, delay);
          });
        }
      } catch (e) {
        logger(`[${letter}] Error during subscription setup: ${e.message}`, "ERROR");
      }
    };

    // Error handling
    client.onDisconnect = () => {
      logger(`[${letter}] DISCONNECTED from server - will reconnect`, "DISCONNECT");
    };

    client.onStompError = frame => {
      logger(`[${letter}] STOMP ERROR: ${frame.headers['message']}`, "ERROR");
    };

    client.onWebSocketError = e => {
      logger(`[${letter}] WebSocket error: ${e.message}`, "ERROR");
    };

    // Save client reference and activate connection
    activeClients[letter] = client;
    client.activate();
    logger(`[${letter}] WebSocket client activated with token authentication`, "SETUP");
  }
}

/**
 * Helper function to start the game when the lobby is ready
 */
function startGameWhenReady(client, lobbyId) {
  // Check if both players have joined the lobby
  if (!lobbyState.playerAJoined || !lobbyState.playerBJoined) {
    logger("Cannot start game - not all players have joined the lobby", "WARN");
    
    // If player B hasn't joined yet, retry WebSocket join
    if (!lobbyState.playerBJoined) {
      logger("Player B not joined yet - retrying WebSocket join", "RETRY");
      retryWebSocketJoin(lobbyId, CLIENT_B_JOIN_CODE);
      
      // Schedule a check later to see if we can start
      setTimeout(() => {
        if (lobbyState.playerAJoined && lobbyState.playerBJoined) {
          startGameWhenReady(client, lobbyId);
        }
      }, 3000);
    }
    return;
  }
  
  // Prevent multiple start attempts
  if (lobbyState.gameStartAttempted) {
    logger("Game start already attempted, not trying again", "WARN");
    return;
  }
  
  // Mark that we've attempted to start
  lobbyState.gameStartAttempted = true;
  
  // Log the current lobby state
  logger(`Starting game with lobby state: playerAJoined=${lobbyState.playerAJoined}, playerBJoined=${lobbyState.playerBJoined}, joinedPlayers=${lobbyState.joinedPlayerTokens.length}/${lobbyState.maxPlayers}`, "GAME");
  
  // Attempt to start the game
  logger('[A] ▶ STARTING GAME');
  recordGameEvent('STARTING_GAME', { 
    player: 'A',
    lobbyState: JSON.parse(JSON.stringify(lobbyState))
  });
  
  client.publish({
    destination: `/app/lobby/${lobbyId}/game/start`,
    body: JSON.stringify({})
  });
  
  // Request game state a few times after start to ensure we get cards
  [1000, 2000, 4000, 8000].forEach((delay) => {
    setTimeout(() => {
      logger(`[A] Requesting game state update (${delay}ms after start)`, "RETRY");
      client.publish({
        destination: `/app/lobby/${lobbyId}/game/state`,
        body: JSON.stringify({})
      });
      
      // Also have B request its state to ensure it knows about its cards
      if (activeClients['B']) {
        activeClients['B'].publish({
          destination: `/app/lobby/${lobbyId}/game/state`,
          body: JSON.stringify({}),
          headers: { 
            Authorization: `Bearer ${TOKEN_B}`
          }
        });
      }
    }, delay);
  });
}

/**
 * Retry WebSocket join for Player B
 */
function retryWebSocketJoin(lobbyId, code, retryCount = 0) {
  const maxRetries = 3;
  if (retryCount >= maxRetries || lobbyState.playerBJoined) return;
  
  logger(`WebSocket join retry ${retryCount+1}/${maxRetries} for Player B`, "RETRY");
  
  const client = activeClients['B'];
  if (!client) {
    logger("No active client for Player B to retry join", "ERROR");
    return;
  }
  
  client.publish({ 
    destination: `/app/lobby/join/${code}`,
    body: JSON.stringify({}),
    headers: { 
      Authorization: `Bearer ${TOKEN_B}`
    }
  });
  
  // Exponential backoff for retries
  setTimeout(() => {
    if (!lobbyState.playerBJoined) {
      retryWebSocketJoin(lobbyId, code, retryCount + 1);
    }
  }, 2000 * Math.pow(2, retryCount));
}

/**
 * Update game state from WebSocket
 */
function updateGameState(letter, payload, client, lobbyId) {
  logger(`[${letter}] Updating game state`, "STATE");
  
  try {
    // Detect screen transitions for timer management
    const previousScreen = gameState.currentScreen;
    const newScreen = payload.currentScreen;
    
    // Clear all timers when screen changes to prevent timing issues
    if (previousScreen !== newScreen) {
      clearAllTimers();
      logger(`[${letter}] Screen transition: ${previousScreen} -> ${newScreen}`, "TRANSITION");
      gameStateTracker.lastScreenChange = Date.now();
      gameStateTracker.currentScreen = newScreen;
      
      // If transitioning to GUESS screen from another screen
      if (newScreen === "GUESS" && previousScreen !== "GUESS") {
        resetGuessTracking();
        
        // Get the time limit from the payload
        const timeLimit = payload.guessScreenAttributes?.time || 30;
        
        // Only player A manages the central timer to avoid duplicates
        if (letter === 'A') {
          logger(`[${letter}] Starting guess timer on screen change to GUESS: ${timeLimit}s`, "TIMER");
          startGuessTimer(timeLimit);
        }
      }
    }
    
    // Update local state
    gameState.currentRound = payload.currentRound || 0;
    gameState.currentScreen = payload.currentScreen || gameState.currentScreen;
    gameState.activePlayerToken = payload.currentTurnPlayerToken;
    
    // Track round card information
    if (payload.activeRoundCard) {
      logger(`[${letter}] Active round card updated to: ${payload.activeRoundCard}`, "STATE");
      gameState.roundCardInPlay = payload.activeRoundCard;
    }
    
    // Track who should submit the round card
    if (payload.roundCardSubmitter) {
      logger(`[${letter}] Round card submitter updated: ${payload.roundCardSubmitter}`, "STATE");
    }
    
    // Update guess screen attributes
    if (payload.guessScreenAttributes) {
      const attrs = payload.guessScreenAttributes;
      gameState.guessScreenAttributes.time = attrs.time || 0;
      
      if (attrs.guessLocation) {
        const loc = attrs.guessLocation;
        logger(`[${letter}] Guess location updated: lat=${loc.lat}, lon=${loc.lon}`, "STATE");
        gameState.guessScreenAttributes.guessLocation = {
          lat: loc.lat,
          lon: loc.lon
        };
      }
      
      if (attrs.resolveResponse) {
        gameState.guessScreenAttributes.resolveResponse = attrs.resolveResponse;
      }
    }
    
    // Update player inventory if available
    if (payload.inventory) {
      if (letter === 'A') {
        gameState.playerA.roundCards = payload.inventory.roundCards || [];
        gameState.playerA.actionCards = payload.inventory.actionCards || [];
        gameState.playerA.isActive = (gameState.activePlayerToken === TOKEN_A);
      } else {
        gameState.playerB.roundCards = payload.inventory.roundCards || [];
        gameState.playerB.actionCards = payload.inventory.actionCards || [];
        gameState.playerB.isActive = (gameState.activePlayerToken === TOKEN_B);
      }
      
      // Check if this player should select a round card and automatically play one
      if (gameState.currentScreen === "ROUNDCARD") {
        const isMyTurn = gameState.activePlayerToken === (letter === 'A' ? TOKEN_A : TOKEN_B);
        const myRoundCards = letter === 'A' ? gameState.playerA.roundCards : gameState.playerB.roundCards;
        
        if (isMyTurn && myRoundCards.length > 0 && !commandLocks.roundCardSelection[letter]) {
          logger(`[${letter}] It's my turn to select a round card!`, "TURN");
          
          // Add a small delay to ensure the UI would have time to update in a real scenario
          setTimeout(() => {
            playRoundCard(letter, client, lobbyId);
          }, 500);
        }
      }
    }
    
    // Record the state update
    recordGameState(`${letter}-update`, payload);
    
    // Process state based on current screen with enhanced phase management
    switch (gameState.currentScreen) {
      case 'ROUNDCARD':
        logger(`[${letter}] Round card selection phase, submitter: ${payload.roundCardSubmitter || 'unknown'}, active token: ${payload.currentTurnPlayerToken || 'unknown'}`, "STATE");
        break;
        
      case 'ACTIONCARD':
        logger(`[${letter}] Action card playing phase, active round card: ${payload.activeRoundCard || 'none'}`, "STATE");
        
        // Auto-play action card if we haven't yet and we're not locked
        if (!actionCardAttempted[letter] && !commandLocks.actionCardPlay[letter]) {
          // Add a small delay to ensure the UI would have time to update in a real scenario
          setTimeout(() => {
            handleActionCardPhase(letter, client, lobbyId);
          }, 1000);
        }
        
        // NEW: Set up a timer to signal action cards complete for player A only (avoid duplicates)
        if (letter === 'A' && !phaseTimers.actionCardPhase) {
          const timeLimit = 10000; // 10 seconds for action card phase
          logger(`[${letter}] Setting timer to signal action cards complete in ${timeLimit/1000}s`, "TIMER");
          
          phaseTimers.actionCardPhase = setTimeout(() => {
            signalActionCardsComplete(letter, client, lobbyId);
          }, timeLimit);
        }
        break;
        
      case 'GUESS':
        const loc = gameState.guessScreenAttributes.guessLocation;
        const timeLimit = gameState.guessScreenAttributes.time || 30;
        logger(`[${letter}] Guessing phase, time: ${timeLimit}s, coords: ${loc ? `${loc.lat},${loc.lon}` : 'unknown'}`, "STATE");
        
        // If we're not already tracking the guess timer and this is player A, start it
        if (!guessTimers.timerActive && letter === 'A') {
          logger(`[${letter}] Starting guess timer in updateGameState: ${timeLimit}s`, "TIMER");
          startGuessTimer(timeLimit);
          
          // Also set up the round expiration timer
          if (phaseTimers.guessingPhase) {
            clearTimeout(phaseTimers.guessingPhase);
          }
          
          const roundTimeMs = (timeLimit + 1) * 1000; // Add 1 second buffer
          
          logger(`[${letter}] Setting timer to signal round time expired in ${roundTimeMs/1000}s`, "TIMER");
          
          phaseTimers.guessingPhase = setTimeout(() => {
            signalRoundTimeExpired(letter, client, lobbyId);
          }, roundTimeMs);
        }
        
        // Submit an automatic guess with some delay to simulate user action
        // Only set up if we haven't already submitted a guess
        if (!guessSubmitted[letter]) {
          const guessDelay = 2000 + Math.random() * 3000; // Between 2-5 seconds
          
          // Clear any existing timeout for this player
          if (guessTimeouts[letter]) {
            clearTimeout(guessTimeouts[letter]);
          }
          
          guessTimeouts[letter] = setTimeout(() => {
            submitAutomaticGuess(letter, client, lobbyId);
          }, guessDelay);
          
          logger(`[${letter}] Will submit automatic guess in ${guessDelay/1000}s`, "TIMER");
        }
        break;
        
      case 'REVEAL':
        logger(`[${letter}] Reveal phase`, "STATE");
        // Auto-reset tracking for next round
        resetActionCardTracking();
        resetGuessTracking();
        break;
        
      case 'GAMEOVER':
        logger(`[${letter}] Game over`, "STATE");
        clearAllTimers(); // Make sure we clean up timers
        // Write game history at the end
        if (letter === 'A') {
          setTimeout(() => {
            writeGameHistoryToFile();
          }, 1000);
        }
        break;
    }
  } catch (error) {
    logger(`[${letter}] Error in updateGameState: ${error.message}`, "ERROR");
  }
}

/**
 * Handle WebSocket events
 */
function handleEvent(letter, body, client, lobbyId) {
  const type = body.type || 'UNKNOWN';
  logger(`[${letter}] Processing event type: ${type}`, "EVENT");
  
  try {
    // Handle different event types
    switch (type) {
      case 'GAME_START':
        logger(`[${letter}] Game started with initial state: ${JSON.stringify(body.payload)}`, "GAME");
        gameState.currentScreen = "ROUNDCARD";
        gameState.currentRound = 1;
        // Reset all tracking for new game
        resetActionCardTracking();
        resetGuessTracking();
        resetCommandLocks();
        clearAllTimers();
        recordGameEvent('GAME_STARTED', {
          player: letter,
          initialState: body.payload
        });
        break;
        
      case 'ROUND_CARD_SELECTED':
        logger(`[${letter}] Round card selected: ${JSON.stringify(body.payload)}`, "GAME");
        gameState.roundCardInPlay = body.payload.roundCard.id;
        
        // Clear any round card selection locks since the server acknowledged the selection
        commandLocks.roundCardSelection.A = false;
        commandLocks.roundCardSelection.B = false;
        
        recordGameEvent('ROUND_CARD_SELECTED', {
          player: letter,
          roundCard: body.payload.roundCard,
          username: body.payload.username
        });
        break;
      
      case 'SCREEN_CHANGE':
        // NEW: Handle explicit screen change messages
        if (body.payload && body.payload.screen) {
          const newScreen = body.payload.screen;
          logger(`[${letter}] Received SCREEN_CHANGE to: ${newScreen}`, "TRANSITION");
          
          // Update current screen
          gameState.currentScreen = newScreen;
          
          // Clear timers to prevent interference between phases
          clearAllTimers();
          
          if (newScreen === "ACTIONCARD" && body.payload.roundCardComplete) {
            resetActionCardTracking();
            // Auto-trigger action card playing after a short delay
            setTimeout(() => {
              if (!actionCardAttempted[letter]) {
                handleActionCardPhase(letter, client, lobbyId);
              }
            }, 1000);
          } 
          else if (newScreen === "GUESS" && body.payload.actionCardsComplete) {
            resetGuessTracking();
            
            // Prepare for guessing phase
            logger(`[${letter}] Transitioning to guessing phase after action cards complete`, "TRANSITION");
            
            // If we're player A, request the latest game state to get timing info
            if (letter === 'A') {
              setTimeout(() => {
                client.publish({
                  destination: `/app/lobby/${lobbyId}/game/state`,
                  body: JSON.stringify({}),
                  headers: { 
                    Authorization: `Bearer ${TOKEN_A}`
                  }
                });
              }, 500);
            }
          }
          
          recordGameEvent('SCREEN_CHANGED', {
            player: letter,
            screen: newScreen,
            payload: body.payload
          });
        }
        break;
        
      case 'ROUND_START':
        // Enhanced handling of ROUND_START with timer management and support for actionCardsComplete transition
        if (body.payload) {
          let roundData = body.payload.roundData || {};
          gameState.currentScreen = "GUESS";
          resetGuessTracking(); // Reset tracking for new guessing phase
          
          // Get time limit from payload or use default
          let timeLimit = 30;
          
          if (body.payload.roundData) {
            // Update game state with round data
            gameState.guessScreenAttributes.time = roundData.roundTime || 30;
            timeLimit = gameState.guessScreenAttributes.time;
            gameState.guessScreenAttributes.guessLocation = {
              lat: roundData.latitude,
              lon: roundData.longitude
            };
            logger(`[${letter}] Round ${roundData.round} started with coords: ${roundData.latitude}, ${roundData.longitude}, time: ${timeLimit}s`, "GAME");
          }
          
          recordGameEvent('ROUND_STARTED', {
            player: letter,
            round: roundData.round,
            time: timeLimit,
            coordinates: body.payload.roundData ? {
              lat: roundData.latitude,
              lon: roundData.longitude
            } : null,
            activeActionCards: body.payload.activeActionCards
          });
          
          // Only start the guess timer if explicitly told to by the server 
          // or if startGuessTimer flag is present (added this check)
          if (body.payload.startGuessTimer) {
            // Have player A manage the central timer
            if (letter === 'A') {
              logger(`[${letter}] Starting guess timer for ${timeLimit} seconds (startGuessTimer flag received)`, "TIMER");
              startGuessTimer(timeLimit);
              
              // Set up timer for player A to signal round time expiration
              if (phaseTimers.guessingPhase) {
                clearTimeout(phaseTimers.guessingPhase);
              }
              
              const roundTimeMs = (timeLimit + 1) * 1000; // Add 1 second buffer
              
              logger(`[${letter}] Setting timer to signal round time expired in ${roundTimeMs/1000}s`, "TIMER");
              
              phaseTimers.guessingPhase = setTimeout(() => {
                signalRoundTimeExpired(letter, client, lobbyId);
              }, roundTimeMs);
            }
          }
          
          // Set up automatic guess submission with random delay
          const guessDelay = 2000 + Math.random() * 3000; // Between 2-5 seconds
          
          // Clear any existing timeout for this player
          if (guessTimeouts[letter]) {
            clearTimeout(guessTimeouts[letter]);
          }
          
          guessTimeouts[letter] = setTimeout(() => {
            submitAutomaticGuess(letter, client, lobbyId);
          }, guessDelay);
        }
        break;
        
      case 'ERROR':
        logger(`[${letter}] Received error from server: ${JSON.stringify(body.payload)}`, "ERROR");
        // If the error is about round card selection, retry with another approach
        if (typeof body.payload === 'string' && 
            (body.payload.includes('round card') || body.payload.includes('not found'))) {
          
          // Reset the command lock so we can try again
          commandLocks.roundCardSelection[letter] = false;
          
          // Request game state to see what's happening
          client.publish({
            destination: `/app/lobby/${lobbyId}/game/state`,
            body: JSON.stringify({}),
            headers: { 
              Authorization: `Bearer ${letter === 'A' ? TOKEN_A : TOKEN_B}`
            }
          });
        }
        break;
        
      case 'ACTION_CARD_PHASE_START':
        logger(`[${letter}] Action card phase started with time limit: ${body.payload.timeLimit}s`, "GAME");
        gameState.currentScreen = "ACTIONCARD";
        resetActionCardTracking(); // Reset tracking for new action card phase
        
        // Store coordinates if provided
        if (body.payload.coordinates) {
          gameState.guessScreenAttributes.guessLocation = body.payload.coordinates;
        }
        
        recordGameEvent('ACTION_CARD_PHASE_START', {
          player: letter,
          timeLimit: body.payload.timeLimit,
          coordinates: body.payload.coordinates
        });
        
        // Automatically play or skip action card after a short delay
        setTimeout(() => {
          handleActionCardPhase(letter, client, lobbyId);
        }, 1000 + Math.random() * 1000); // Random delay between 1-2 seconds to avoid collision
        
        // Set timer for player A to signal action cards complete
        if (letter === 'A') {
          if (phaseTimers.actionCardPhase) {
            clearTimeout(phaseTimers.actionCardPhase);
          }
          
          // Use a shorter timeout to ensure transition happens promptly
          const actionCardTimeLimit = body.payload.timeLimit || 10;
          const timeoutMs = Math.min(actionCardTimeLimit * 1000, 8000); // Cap at 8 seconds max
          
          logger(`[${letter}] Setting up action card phase timer for ${timeoutMs/1000}s`, "TIMER");
          
          phaseTimers.actionCardPhase = setTimeout(() => {
            signalActionCardsComplete(letter, client, lobbyId);
          }, timeoutMs);
        }
        break;
        
      case 'ACTION_CARD_PLAYED':
        logger(`[${letter}] Action card played: ${JSON.stringify(body.payload)}`, "GAME");
        // Track played action cards
        const cardId = body.payload.cardId;
        const playerToken = body.payload.playerToken;
        gameState.actionCardsPlayed[playerToken] = cardId;
        recordGameEvent('ACTION_CARD_PLAYED', {
          player: letter,
          cardId: cardId,
          playerToken: playerToken,
          effect: body.payload.effect
        });
        break;
        
      case 'ACTION_CARD_SKIPPED':
        logger(`[${letter}] Player skipped action card: ${JSON.stringify(body.payload)}`, "GAME");
        recordGameEvent('ACTION_CARD_SKIPPED', {
          player: letter,
          playerToken: body.payload?.playerToken
        });
        break;
        
      case 'ACTION_CARD_SUBMIT':
        logger(`[${letter}] Action card submitted: ${JSON.stringify(body.payload)}`, "GAME");
        recordGameEvent('ACTION_CARD_SUBMITTED', {
          player: letter,
          actionCardId: body.payload?.actionCardId || 'unknown'
        });
        break;
        
      case 'ROUND_WINNER':
        // Access winner data through payload property
        const winnerData = body.payload || body; // Support both payload wrapper and direct message format
        const winnerUsername = winnerData.winnerUsername || winnerData.username;
        const round = winnerData.round || gameState.currentRound;
        const distance = winnerData.distance || 0;
        
        logger(`[${letter}] Round winner: ${winnerUsername} with distance ${distance}m in round ${round}`, "GAME");
        
        // Update game state
        gameState.currentScreen = "REVEAL";
        gameState.roundWinners.push(winnerUsername);
        gameState.roundWinnerDistances.push(distance);
        
        recordGameEvent('ROUND_WINNER', {
          player: letter,
          username: winnerUsername,
          round: round,
          distance: distance
        });
        
        // Reset tracking for next round
        resetActionCardTracking();
        resetGuessTracking();
        resetCommandLocks();
        clearAllTimers();
        
        // Request updated game state after a short delay to get next round setup
        setTimeout(() => {
          client.publish({
            destination: `/app/lobby/${lobbyId}/game/state`,
            body: JSON.stringify({}),
            headers: { 
              Authorization: `Bearer ${letter === 'A' ? TOKEN_A : TOKEN_B}`
            }
          });
        }, 1000);
        break;
        
      case 'GAME_WINNER':
        // Access winner data through payload property
        const gameWinnerData = body.payload || {};
        logger(`[${letter}] Game winner: ${gameWinnerData.username}`, "GAME");
        gameState.currentScreen = "GAMEOVER";
        gameState.gameWinner = gameWinnerData.username;
        recordGameEvent('GAME_WINNER', {
          player: letter,
          username: gameWinnerData.username
        });
        clearAllTimers();
        break;
        
      default:
        logger(`[${letter}] Unhandled event type: ${type}`, "WARN");
    }
  } catch (error) {
    logger(`[${letter}] Error in handleEvent: ${error.message}`, "ERROR");
  }
}

/**
 * Start client-side timer for guessing phase
 */
function startGuessTimer(timeLimit) {
  // Stop any existing timer
  if (guessTimers.timerInterval) {
    clearInterval(guessTimers.timerInterval);
    guessTimers.timerInterval = null;
  }
  
  // Initialize timer state
  guessTimers.startTime = Date.now();
  guessTimers.timeLimit = timeLimit;
  guessTimers.remainingTime = timeLimit;
  guessTimers.timerActive = true;
  
  logger(`Starting guess timer with ${timeLimit} seconds`, "TIMER");
  
  // Create an interval that updates every second
  guessTimers.timerInterval = setInterval(() => {
    const elapsed = Math.floor((Date.now() - guessTimers.startTime) / 1000);
    guessTimers.remainingTime = Math.max(0, guessTimers.timeLimit - elapsed);
    
    // Log time every 5 seconds for debugging
    if (guessTimers.remainingTime % 5 === 0 || guessTimers.remainingTime <= 5) {
      logger(`Guess timer: ${guessTimers.remainingTime}s remaining`, "TIMER");
    }
    
    // When time is up
    if (guessTimers.remainingTime <= 0 && guessTimers.timerActive) {
      guessTimers.timerActive = false;
      clearInterval(guessTimers.timerInterval);
      guessTimers.timerInterval = null;
      
      logger("Guess timer expired!", "TIMER");
      
      // Ensure all players have submitted guesses before time expires
      ensureGuessSubmitted('A');
      ensureGuessSubmitted('B');
    }
  }, 1000);
  
  // Record timer start in game history
  recordGameEvent('GUESS_TIMER_STARTED', {
    timeLimit: timeLimit,
    timestamp: Date.now()
  });
}

/**
 * Ensure player has submitted a guess before timer expires
 */
function ensureGuessSubmitted(letter) {
  if (guessSubmitted[letter]) {
    return; // Already submitted
  }
  
  logger(`[${letter}] Timer expired but no guess submitted - sending automatic guess`, "TIMER");
  
  const client = activeClients[letter];
  if (!client) return;
  
  // Submit an automatic guess immediately
  submitAutomaticGuess(letter, client, currentLobbyId);
}

/**
 * Signal to the server that action card phase is complete
 * and request transition to guessing phase
 */
function signalActionCardsComplete(letter, client, lobbyId) {
  // Only proceed if we're still in the action card phase
  if (gameState.currentScreen !== "ACTIONCARD") {
    logger(`[${letter}] Not signaling action cards complete - already in ${gameState.currentScreen} phase`, "INFO");
    return;
  }

  logger(`[${letter}] Signaling action cards complete to transition to guessing phase`, "PHASE");
  
  recordGameEvent('ACTION_CARDS_COMPLETE', {
    player: letter
  });
  
  // Send message to the endpoint that handles action cards phase completion
  client.publish({
    destination: `/app/lobby/${lobbyId}/game/action-cards-complete`,
    body: JSON.stringify({}),
    headers: { 
      Authorization: `Bearer ${letter === 'A' ? TOKEN_A : TOKEN_B}`
    }
  });
  
  // Reset tracking for the action card phase
  resetActionCardTracking();
  
  // Add a fallback timer in case the server doesn't respond with the screen change
  setTimeout(() => {
    if (gameState.currentScreen === "ACTIONCARD") {
      logger(`[${letter}] No screen change received after signaling action cards complete - requesting game state`, "RETRY");
      
      // Request current game state
      client.publish({
        destination: `/app/lobby/${lobbyId}/game/state`,
        body: JSON.stringify({}),
        headers: { 
          Authorization: `Bearer ${letter === 'A' ? TOKEN_A : TOKEN_B}`
        }
      });
    }
  }, 2000);
}

/**
 * Automate guessing to ensure game progression
 */
function submitAutomaticGuess(letter, client, lobbyId) {
  // Don't try to guess if we've already guessed
  if (guessSubmitted[letter]) {
    logger(`[${letter}] Already submitted a guess this round`, "INFO");
    return;
  }
  
  const coords = gameState.guessScreenAttributes.guessLocation;
  if (!coords || !coords.lat || !coords.lon) {
    logger(`[${letter}] Cannot submit guess - coordinates not available`, "ERROR");
    return;
  }
  
  // Set lock to prevent duplicate submissions
  commandLocks.guessSubmission[letter] = true;
  guessSubmitted[letter] = true;
  
  // Generate slightly random guess based on actual coordinates
  const guess = generateGuess({
    lat: coords.lat,
    lon: coords.lon
  });
  
  logger(`[${letter}] Submitting automatic guess: ${guess.lat}, ${guess.lon}`, "GUESS");
  
  // Record in game history
  recordGameEvent('SUBMITTING_GUESS', {
    player: letter,
    latitude: guess.lat,
    longitude: guess.lon
  });
  
  // Send the guess
  client.publish({
    destination: `/app/lobby/${lobbyId}/game/guess`,
    body: JSON.stringify({
      guess: {
        lat: guess.lat,
        lon: guess.lon
      }
    }),
    headers: {
      Authorization: `Bearer ${letter === 'A' ? TOKEN_A : TOKEN_B}`
    }
  });
  
  // Reset the command lock after a delay
  setTimeout(() => {
    commandLocks.guessSubmission[letter] = false;
  }, 2000);
}

/**
 * Handle playing a round card when it's the player's turn
 */
function playRoundCard(letter, client, lobbyId) {
  // Check if we're in the round card selection phase
  if (gameState.currentScreen !== "ROUNDCARD") {
    logger(`[${letter}] Not in round card selection phase, current screen: ${gameState.currentScreen}`, "WARN");
    return;
  }

  // Check if we're already attempting a round card selection
  if (commandLocks.roundCardSelection[letter]) {
    logger(`[${letter}] Round card selection already in progress`, "WARN");
    return;
  }

  // Set lock to prevent duplicate attempts
  commandLocks.roundCardSelection[letter] = true;

  try {
    // Get this player's round cards
    const roundCards = letter === 'A' ? gameState.playerA.roundCards : gameState.playerB.roundCards;
    
    // Check if we have any round cards to play
    if (!roundCards || roundCards.length === 0) {
      logger(`[${letter}] No round cards available to play`, "ERROR");
      commandLocks.roundCardSelection[letter] = false;
      return;
    }

    // Pick the first round card (could be randomized if desired)
    const roundCardId = roundCards[0];
    logger(`[${letter}] Selected round card: ${roundCardId}`, "GAME");
    
    // Record the action in game history
    recordGameEvent('SELECTING_ROUND_CARD', {
      player: letter,
      roundCardId: roundCardId
    });

    // Send the round card selection to the server
    client.publish({
      destination: `/app/lobby/${lobbyId}/game/select-round-card`,
      body: JSON.stringify({
        roundCardId: roundCardId
      }),
      headers: { 
        Authorization: `Bearer ${letter === 'A' ? TOKEN_A : TOKEN_B}`
      }
    });
    
    logger(`[${letter}] Sent round card selection: ${roundCardId}`, "GAME");

    // Set a timeout to release the lock in case we don't get a response
    setTimeout(() => {
      if (commandLocks.roundCardSelection[letter]) {
        logger(`[${letter}] Releasing round card selection lock after timeout`, "TIMEOUT");
        commandLocks.roundCardSelection[letter] = false;
      }
    }, 5000);
  } catch (error) {
    logger(`[${letter}] Error selecting round card: ${error.message}`, "ERROR");
    commandLocks.roundCardSelection[letter] = false;
  }
}

/**
 * Handle the action card phase - either play or skip an action card
 */
function handleActionCardPhase(letter, client, lobbyId) {
  // Check if we're already processing an action card
  if (commandLocks.actionCardPlay[letter]) {
    logger(`[${letter}] Action card play already in progress`, "WARN");
    return;
  }

  // Check if we already played a card this round
  if (actionCardAttempted[letter]) {
    logger(`[${letter}] Already played an action card this round`, "WARN");
    return;
  }

  // Set lock to prevent duplicate submissions
  commandLocks.actionCardPlay[letter] = true;
  actionCardAttempted[letter] = true; // Mark that we attempted to play a card this round

  try {
    // Get this player's action cards
    const actionCards = letter === 'A' ? gameState.playerA.actionCards : gameState.playerB.actionCards;
    
    // Check if we have any action cards to play
    if (!actionCards || actionCards.length === 0) {
      logger(`[${letter}] No action cards available to play`, "WARN");
      commandLocks.actionCardPlay[letter] = false;
      return;
    }

    // Get the action card (there should only be one)
    const actionCardId = actionCards[0];
    logger(`[${letter}] Have action card: ${actionCardId}`, "GAME");
    
    // Create the payload for playing the action card
    let payload = {
      actionCardId: actionCardId
    };

    // Determine if this is a punishment card that needs a target
    if (actionCardId === "badsight") {
      // This is a punishment card - choose the other player as target
      const targetToken = letter === 'A' ? TOKEN_B : TOKEN_A;
      payload.targetPlayerToken = targetToken;
      logger(`[${letter}] Playing punishment card with target player: ${targetToken}`, "GAME");
    }

    // Record the action in game history
    recordGameEvent('PLAYING_ACTION_CARD', {
      player: letter,
      actionCardId: actionCardId,
      targetPlayerToken: payload.targetPlayerToken
    });

    // Send the action card play to the server
    client.publish({
      destination: `/app/lobby/${lobbyId}/game/play-action-card`,
      body: JSON.stringify(payload),
      headers: { 
        Authorization: `Bearer ${letter === 'A' ? TOKEN_A : TOKEN_B}`
      }
    });
    
    logger(`[${letter}] Sent action card play: ${JSON.stringify(payload)}`, "GAME");

    // Set a timeout to release the lock in case we don't get a response
    setTimeout(() => {
      if (commandLocks.actionCardPlay[letter]) {
        logger(`[${letter}] Releasing action card play lock after timeout`, "TIMEOUT");
        commandLocks.actionCardPlay[letter] = false;
      }
    }, 5000);
  } catch (error) {
    logger(`[${letter}] Error playing action card: ${error.message}`, "ERROR");
    commandLocks.actionCardPlay[letter] = false;
  }
}

// Updated main function to use tokens directly
async function main() {
  try {
    logger("Starting game client", "START");
    logger("Using tokens directly for authentication");
    
    // Store the tokens directly - no need to fetch IDs
    gameState.playerA.token = TOKEN_A;
    gameState.playerB.token = TOKEN_B;
    
    // Improved lobby creation logic with token
    logger("Creating lobby as player A...");
    try {
      const createLobbyRequest = {
        maxPlayers: 2,  // Explicitly set to 2 players
        playersPerTeam: 1,
        private: false // Make sure it's a public lobby
      };
      
      // Store the expected max players
      lobbyState.maxPlayers = 2;
      
      const { data: lobby } = await axios.post(
        `${REST_BASE}/lobbies`,
        createLobbyRequest,
        { headers: { Authorization: `Bearer ${TOKEN_A}` } } // Ensure Bearer prefix
      );
      
      const LID = lobby.lobbyId, CODE = lobby.code;
      logger(`Lobby created: ID=${LID}, CODE=${CODE} with maxPlayers=${createLobbyRequest.maxPlayers}`, "LOBBY");
      
      // Store code globally so it can be used for REST API join fallback
      CLIENT_B_JOIN_CODE = CODE;
      
      setupWebSocketClients(LID, CODE);
    } catch (lobbyError) {
      logger(`Lobby creation failed: ${lobbyError.message}`, "ERROR");
      
      // Try with Bearer prefix as a fallback
      try {
        logger("Retrying lobby creation with Bearer prefix...");
        const { data: lobby } = await axios.post(
          `${REST_BASE}/lobbies`,
          { maxPlayers: 2, playersPerTeam: 1, private: false },
          { headers: { Authorization: `Bearer ${TOKEN_A}` } }
        );
        
        const LID = lobby.lobbyId, CODE = lobby.code;
        logger(`Lobby created with Bearer prefix: ID=${LID}, CODE=${CODE} with maxPlayers=2`, "LOBBY");
        
        // Store the expected max players
        lobbyState.maxPlayers = 2;
        
        // Store code globally so it can be used for REST API join fallback
        CLIENT_B_JOIN_CODE = CODE;
        
        setupWebSocketClients(LID, CODE);
      } catch (retryError) {
        logger(`Lobby creation retry failed: ${retryError.message}`, "FATAL");
        process.exit(1);
      }
    }
  } catch (e) {
    logger(`Fatal error: ${e.message}`, "FATAL");
    logger(e.stack, "STACK");
    process.exit(1);
  }
}

// Function to write complete game history to JSON file
function writeGameHistoryToFile() {
  try {
    // First close the state log JSON array
    fs.appendFileSync(STATE_LOG_FILE, '\n]');
    
    // Now write the comprehensive game history to a separate file
    const historyFile = path.join(LOG_DIR, `game-history-${TIMESTAMP}.json`);
    fs.writeFileSync(historyFile, JSON.stringify(gameHistory, null, 2));
    
    logger(`Complete game history written to ${historyFile}`, "INFO");
  } catch (error) {
    logger(`Error writing game history: ${error.message}`, "ERROR");
  }
}

// Close JSON array on process exit
process.on('exit', (code) => {
  try {
    // Close the state log JSON array if not already closed
    if (stateLogCount > 0) {
      fs.appendFileSync(STATE_LOG_FILE, '\n]');
    }
    
    // Write final game summary
    logger(`Process exiting with code ${code}`, "EXIT");
    logger(`Total game time: ${((Date.now() - new Date(fs.statSync(LOG_FILE).birthtime).getTime()) / 1000).toFixed(1)}s`, "SUMMARY");
  } catch (e) {
    console.error("Error during exit:", e);
  }
});

// Handle SIGINT (Ctrl+C)
process.on('SIGINT', function() {
  logger("Received SIGINT. Writing final logs before exit...", "EXIT");
  
  // Write final game history
  writeGameHistoryToFile();
  
  process.exit(0);
});

logger(`Game client initialized. Logs will be written to: ${LOG_FILE}`, "INIT");
logger(`Game states will be recorded in: ${STATE_LOG_FILE}`, "INIT");
main().catch(e => {
  logger(`Fatal error in main: ${e.message}`, "FATAL");
  logger(e.stack, "STACK");
});
