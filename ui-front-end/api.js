const express = require('express');
const bodyParser = require('body-parser');

const app = express();
const PORT = 3000;

// Middleware to parse JSON
app.use(bodyParser.json());

// Define a route to receive data from Java
app.post('/api/data', (req, res) => {
  const data = req.body;
  console.log('Data received from Java:', data);
  res.status(200).send('Data received');
});

app.listen(PORT, () => {
  console.log(`Server is running on http://localhost:${PORT}`);
});
