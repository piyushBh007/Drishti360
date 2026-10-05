const WebSocket = require('ws');

// In-memory room store: inspectionId -> Map<participantId, { ws, participantId, role, joinedAt }>
const rooms = new Map();

function setupSignalingServer(server) {
  const wss = new WebSocket.Server({ server, path: '/ws' });

  console.log('[WebRTC Signaling] Signaling server attached to Express server on path /ws');

  wss.on('connection', (ws, req) => {
    let clientInspectionId = null;
    let clientParticipantId = null;
    let clientRole = 'unknown';

    console.log(`[WebRTC Signaling] New WebSocket connection established from ${req.socket.remoteAddress}`);

    ws.on('message', (messageRaw) => {
      try {
        const msg = JSON.parse(messageRaw.toString());
        const { type, inspectionId, participantId, role, sdp, candidate } = msg;

        if (!type || !inspectionId) {
          ws.send(JSON.stringify({ type: 'error', message: 'Missing type or inspectionId' }));
          return;
        }

        switch (type) {
          case 'join': {
            clientInspectionId = inspectionId;
            clientParticipantId = participantId || `participant-${Math.random().toString(36).substring(2, 7)}`;
            clientRole = role || 'participant';

            if (!rooms.has(inspectionId)) {
              rooms.set(inspectionId, new Map());
            }

            const room = rooms.get(inspectionId);
            room.set(clientParticipantId, {
              ws,
              participantId: clientParticipantId,
              role: clientRole,
              joinedAt: new Date()
            });

            console.log(`[WebRTC Signaling] Participant '${clientParticipantId}' (${clientRole}) joined room '${inspectionId}'. Room size: ${room.size}`);

            // Send confirmation to joining client
            ws.send(JSON.stringify({
              type: 'joined',
              inspectionId,
              participantId: clientParticipantId,
              role: clientRole,
              roomSize: room.size,
              existingParticipants: Array.from(room.keys()).filter(id => id !== clientParticipantId)
            }));

            // Notify other participants in room
            broadcastToRoom(inspectionId, clientParticipantId, {
              type: 'participant-joined',
              inspectionId,
              participantId: clientParticipantId,
              role: clientRole,
              roomSize: room.size
            });
            break;
          }

          case 'offer': {
            console.log(`[WebRTC Signaling] Relaying SDP OFFER in room '${inspectionId}' from '${clientParticipantId}'`);
            broadcastToRoom(inspectionId, clientParticipantId, {
              type: 'offer',
              inspectionId,
              senderId: clientParticipantId,
              sdp
            });
            break;
          }

          case 'answer': {
            console.log(`[WebRTC Signaling] Relaying SDP ANSWER in room '${inspectionId}' from '${clientParticipantId}'`);
            broadcastToRoom(inspectionId, clientParticipantId, {
              type: 'answer',
              inspectionId,
              senderId: clientParticipantId,
              sdp
            });
            break;
          }

          case 'ice-candidate': {
            broadcastToRoom(inspectionId, clientParticipantId, {
              type: 'ice-candidate',
              inspectionId,
              senderId: clientParticipantId,
              candidate
            });
            break;
          }

          case 'leave': {
            handleClientLeave(inspectionId, clientParticipantId);
            break;
          }

          case 'ping': {
            ws.send(JSON.stringify({ type: 'pong', timestamp: new Date().toISOString() }));
            break;
          }

          default:
            console.warn(`[WebRTC Signaling] Unknown message type '${type}' from '${clientParticipantId}'`);
        }

      } catch (err) {
        console.error('[WebRTC Signaling Error] Failed to parse message:', err.message);
        try {
          ws.send(JSON.stringify({ type: 'error', message: 'Invalid JSON payload' }));
        } catch (_) {}
      }
    });

    ws.on('close', () => {
      console.log(`[WebRTC Signaling] Connection closed for '${clientParticipantId}' in room '${clientInspectionId}'`);
      if (clientInspectionId && clientParticipantId) {
        handleClientLeave(clientInspectionId, clientParticipantId);
      }
    });

    ws.on('error', (err) => {
      console.error(`[WebRTC Signaling Socket Error] Client '${clientParticipantId}':`, err.message);
    });
  });
}

function broadcastToRoom(inspectionId, senderId, payload) {
  const room = rooms.get(inspectionId);
  if (!room) return;

  const jsonStr = JSON.stringify(payload);
  room.forEach((client, pid) => {
    if (pid !== senderId && client.ws.readyState === WebSocket.OPEN) {
      client.ws.send(jsonStr);
    }
  });
}

function handleClientLeave(inspectionId, participantId) {
  const room = rooms.get(inspectionId);
  if (!room) return;

  room.delete(participantId);
  console.log(`[WebRTC Signaling] '${participantId}' left room '${inspectionId}'. Remaining: ${room.size}`);

  // Notify remaining room members
  broadcastToRoom(inspectionId, participantId, {
    type: 'participant-left',
    inspectionId,
    participantId,
    roomSize: room.size
  });

  if (room.size === 0) {
    rooms.delete(inspectionId);
    console.log(`[WebRTC Signaling] Room '${inspectionId}' cleaned up (empty).`);
  }
}

module.exports = {
  setupSignalingServer,
  rooms
};
