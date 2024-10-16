// import express from 'express';
// import { WebSocketServer } from 'ws';

// const app = express();
// const HTTP_PORT = 8080;
// const WS_PORT = 8081;

// // Use JSON middleware to parse incoming requests
// app.use(express.json());

// // Test endpoint for the server root
// app.get('/', (req, res) => {
//   res.send('Welcome to the Market Depth API!');
// });

// // Endpoint to receive updates from Java backend
// app.post('/update', (req, res) => {
//   const message = req.body.message;
//   console.log('Received update from Java backend:', message);
  
//   // Broadcast the message to all WebSocket clients
//   wss.clients.forEach((client) => {
//     if (client.readyState === client.OPEN) {
//       client.send(JSON.stringify({ message }));
//     }
//   });

//   res.status(200).send('Update received');
// });

// // Start HTTP server
// app.listen(HTTP_PORT, () => {
//   console.log(`HTTP server running on port ${HTTP_PORT}`);
// });

// // Set up WebSocket server on a separate port
// const wss = new WebSocketServer({ port: WS_PORT });
// wss.on('connection', (ws) => {
//   console.log('New WebSocket connection');
//   ws.on('close', () => console.log('WebSocket connection closed'));
// });
