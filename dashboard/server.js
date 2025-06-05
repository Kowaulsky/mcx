const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const path = require('path');
const { v4: uuidv4 } = require('uuid');
const bcrypt = require('bcryptjs');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });
const port = process.env.PORT || 3000;

// In-memory storage
const tokens = new Map(); // token -> { expiration, users }
const logsByToken = new Map(); // token -> logs
const wsClients = new Map(); // token -> WebSocket clients

// Middleware
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Serve dashboard
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'dashboard.html'));
});

// API to generate temporary link
app.post('/api/generate-link', (req, res) => {
    const { users, logs } = req.body;
    if (!users || !Array.isArray(users) || !logs || !Array.isArray(logs)) {
        return res.status(400).json({ error: 'Missing or invalid users/logs' });
    }

    const token = uuidv4();
    const expiration = Date.now() + 24 * 60 * 60 * 1000; // 24 hours
    tokens.set(token, { expiration, users });
    logsByToken.set(token, logs);
    
    res.json({ link: `http://192.168.23.46:${port}/?token=${token}` });
});

// API to validate login
app.post('/api/login', (req, res) => {
    const { token, username, password } = req.body;
    const tokenData = tokens.get(token);
    if (!tokenData || tokenData.expiration < Date.now()) {
        return res.status(401).json({ error: 'Invalid or expired token' });
    }

    const user = tokenData.users.find(u => u.username === username);
    if (!user || !bcrypt.compareSync(password, user.password_hash)) {
        return res.status(401).json({ error: 'Invalid credentials' });
    }

    res.json({ success: true });
});

// API to fetch logs with pagination
app.get('/api/logs', (req, res) => {
    const { token, page = 1, size = 50 } = req.query;
    const tokenData = tokens.get(token);
    if (!tokenData || tokenData.expiration < Date.now()) {
        return res.status(401).json({ error: 'Invalid or expired token' });
    }

    const logs = logsByToken.get(token) || [];
    const start = (page - 1) * size;
    const end = start + parseInt(size);
    const paginatedLogs = logs.slice(start, end);

    res.json({
        logs: paginatedLogs,
        total: logs.length,
        page: parseInt(page),
        size: parseInt(size)
    });
});

// WebSocket for live updates
wss.on('connection', (ws, req) => {
    const urlParams = new URLSearchParams(req.url.split('?')[1]);
    const token = urlParams.get('token');
    if (!tokens.get(token) || tokens.get(token).expiration < Date.now()) {
        ws.close();
        return;
    }

    wsClients.set(token, ws);
    ws.on('close', () => wsClients.delete(token));
});

// Broadcast new logs
app.post('/api/logs', (req, res) => {
    const log = req.body;
    if (!log.player_uuid || !log.action_type || !log.timestamp || !log.details) {
        return res.status(400).json({ error: 'Invalid log format' });
    }

    logsByToken.forEach((logs, token) => {
        if (tokens.get(token).expiration >= Date.now()) {
            logs.push(log);
            const ws = wsClients.get(token);
            if (ws) ws.send(JSON.stringify(log));
        }
    });

    res.status(200).send();
});

// Clean expired tokens
setInterval(() => {
    const now = Date.now();
    tokens.forEach((value, token) => {
        if (value.expiration < now) {
            tokens.delete(token);
            logsByToken.delete(token);
            const ws = wsClients.get(token);
            if (ws) ws.close();
            wsClients.delete(token);
        }
    });
}, 60000);

// Start server
server.listen(port, '192.168.23.46', () => {
    console.log(`Server running at http://192.168.23.46:${port}`);
});