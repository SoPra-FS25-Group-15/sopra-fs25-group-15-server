const fs = require('fs');
const path = require('path');
const axios = require('axios');

const REST_BASE = 'http://localhost:8080';
const TOKEN_A = '293537eb-e354-4ca2-873f-c2f6a1c00f73';
const TOKEN_B = '3592cc6d-c310-4206-8241-644c032b910d';

async function diagnoseRoundCardIssue(lobbyId) {
  console.log(`Diagnosing round card issue for lobby ${lobbyId}`);
  
  try {
    // 1. Make a direct REST API call to get game state
    console.log(`Requesting game state via REST API...`);
    const stateResp = await axios.get(
      `${REST_BASE}/debug/game/${lobbyId}/state`,
      { headers: { Authorization: `Bearer ${TOKEN_A}` } }
    );
    
    console.log(`Game state response:`, JSON.stringify(stateResp.data, null, 2));
    
    // 2. Try submitting a round card directly
    console.log(`Attempting to submit a round card directly...`);
    
    // Get player A's round cards
    const cardsResp = await axios.get(
      `${REST_BASE}/debug/game/${lobbyId}/player/${TOKEN_A}/cards`,
      { headers: { Authorization: `Bearer ${TOKEN_A}` } }
    );
    
    if (cardsResp.data.roundCards && cardsResp.data.roundCards.length > 0) {
      const cardId = cardsResp.data.roundCards[0].id;
      console.log(`Found round card ${cardId}, submitting it...`);
      
      const submitResp = await axios.post(
        `${REST_BASE}/debug/game/${lobbyId}/select-round-card`,
        { roundCardId: cardId, playerToken: TOKEN_A },
        { headers: { Authorization: `Bearer ${TOKEN_A}` } }
      );
      
      console.log(`Round card submission response:`, JSON.stringify(submitResp.data, null, 2));
      
      // 3. Check game state again to see if it updated
      const afterStateResp = await axios.get(
        `${REST_BASE}/debug/game/${lobbyId}/state`,
        { headers: { Authorization: `Bearer ${TOKEN_A}` } }
      );
      
      console.log(`Game state after round card submission:`, JSON.stringify(afterStateResp.data, null, 2));
    } else {
      console.log(`No round cards found for player A`);
    }
    
  } catch (error) {
    console.error(`Error during diagnosis:`, error.response?.data || error.message);
  }
}

// Run diagnosis with the lobby ID as command line argument
const lobbyId = process.argv[2];
if (lobbyId) {
  diagnoseRoundCardIssue(lobbyId).catch(console.error);
} else {
  console.error(`Please provide a lobby ID as a command line argument`);
}
