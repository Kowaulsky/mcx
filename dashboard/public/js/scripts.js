document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM fully loaded and parsed');

    const token = new URLSearchParams(window.location.search).get('token');
    let page = 1;
    const pageSize = 50;
    let totalLogs = 0;

    // Tab switching logic
    const tabButtons = document.querySelectorAll('.nav-button');
    const tabPanes = document.querySelectorAll('.tab-pane');
    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            tabButtons.forEach(btn => btn.classList.remove('active'));
            tabPanes.forEach(pane => pane.classList.remove('active'));

            button.classList.add('active');
            document.getElementById(button.dataset.tab).classList.add('active');
            page = 1; // Reset to first page when switching tabs
            fetchLogs(); // Fetch logs for the new tab
        });
    });

    // Filter and refresh elements
    const dateFilter = document.getElementById('date-filter');
    const playerFilter = document.getElementById('player-filter');
    const keywordFilter = document.getElementById('keyword-filter');
    const refreshButton = document.getElementById('refresh-button');
    const prevPageButton = document.getElementById('prev-page');
    const nextPageButton = document.getElementById('next-page');
    const pageInfo = document.getElementById('page-info');

    // Login modal elements
    const loginModal = document.getElementById('login-modal');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const loginButton = document.getElementById('login-button');
    const loginError = document.getElementById('login-error');

    // Check login status
    if (!token) {
        loginError.textContent = 'No token provided';
        loginError.classList.remove('hidden');
        loginModal.classList.remove('hidden');
        return;
    }

    // Show login modal
    loginModal.classList.remove('hidden');

    // Handle login
    loginButton.addEventListener('click', () => {
        const username = usernameInput.value;
        const password = passwordInput.value;
        axios.post('/api/login', { token, username, password })
            .then(response => {
                if (response.data.success) {
                    loginModal.classList.add('hidden');
                    fetchLogs();
                    setupWebSocket();
                }
            })
            .catch(error => {
                loginError.textContent = error.response?.data?.error || 'Login failed';
                loginError.classList.remove('hidden');
            });
    });

    // Fetch logs
    function fetchLogs() {
        const activeTab = document.querySelector('.nav-button.active').dataset.tab;
        const actionType = {
            'command-logs': 'COMMAND',
            'interaction-logs': 'INTERACTION',
            'movement-logs': 'MOVEMENT',
            'chat-logs': 'CHAT',
            'other-logs': 'OTHER'
        }[activeTab];

        axios.get(`/api/logs?token=${token}&page=${page}&size=${pageSize}&actionType=${actionType}`)
            .then(response => {
                console.log('Logs received:', response.data);
                window.allLogs = response.data.logs;
                totalLogs = response.data.total;
                updatePagination();
                renderLogs(window.allLogs);
                applyFilters();
            })
            .catch(error => {
                console.error('Failed to fetch logs:', error);
                const errorDiv = document.createElement('div');
                errorDiv.textContent = 'Error loading logs. Please try again later.';
                errorDiv.style.color = 'red';
                document.querySelector('.tab-content').prepend(errorDiv);
            });
    }

    // Render logs
    function renderLogs(logs) {
        const logContainers = {
            COMMAND: document.getElementById('command-logs').querySelector('tbody'),
            INTERACTION: document.getElementById('interaction-logs').querySelector('tbody'),
            MOVEMENT: document.getElementById('movement-logs').querySelector('tbody'),
            CHAT: document.getElementById('chat-logs').querySelector('tbody'),
            OTHER: document.getElementById('other-logs').querySelector('tbody')
        };

        Object.values(logContainers).forEach(container => container.innerHTML = '');

        logs.forEach(log => {
            const container = logContainers[log.action_type];
            if (!container) {
                console.warn(`No container for action_type: ${log.action_type}`);
                return;
            }

            let player, action, details;
            try {
                const parsedDetails = JSON.parse(log.details || '{}');
                if (log.action_type === 'COMMAND') {
                    player = parsedDetails.issuer || 'Unknown';
                    action = 'Executed Command';
                    details = parsedDetails.command || 'Unknown command';
                } else if (log.action_type === 'CHAT') {
                    player = parsedDetails.issuer || 'Unknown';
                    action = 'Chat Message';
                    details = parsedDetails.message || 'Unknown message';
                } else {
                    player = parsedDetails.issuer || log.player_uuid; // Fallback to UUID if issuer not present
                    action = log.action_type;
                    details = parsedDetails.message || log.details || 'No details';
                }
            } catch (e) {
                console.warn(`Failed to parse details for ${log.action_type}:`, e);
                player = log.player_uuid;
                action = log.action_type;
                details = 'Invalid log';
            }

            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${log.timestamp}</td>
                <td>${player}</td>
                <td>${action}</td>
                <td>${details}</td>
            `;
            container.appendChild(row);
        });
    }

    // Apply filters
    function applyFilters() {
        if (!window.allLogs) return;

        const date = dateFilter.value;
        const player = playerFilter.value.toLowerCase();
        const keyword = keywordFilter.value.toLowerCase();

        const filteredLogs = window.allLogs.filter(log => {
            const logDate = new Date(log.timestamp).toISOString().split('T')[0];
            const parsedDetails = JSON.parse(log.details || '{}');
            const logPlayer = parsedDetails.issuer ? parsedDetails.issuer.toLowerCase() : log.player_uuid.toLowerCase();
            const logText = `${log.timestamp} ${logPlayer} ${log.action_type} ${log.details}`.toLowerCase();

            return (!date || logDate === date) &&
                   (!player || logPlayer.includes(player)) &&
                   (!keyword || logText.includes(keyword));
        });

        renderLogs(filteredLogs);
    }

    // Update pagination
    function updatePagination() {
        const totalPages = Math.ceil(totalLogs / pageSize);
        pageInfo.textContent = `Page ${page} of ${totalPages}`;
        prevPageButton.disabled = page === 1;
        nextPageButton.disabled = page === totalPages;
    }

    // Pagination events
    prevPageButton.addEventListener('click', () => {
        if (page > 1) {
            page--;
            fetchLogs();
        }
    });

    nextPageButton.addEventListener('click', () => {
        const totalPages = Math.ceil(totalLogs / pageSize);
        if (page < totalPages) {
            page++;
            fetchLogs();
        }
    });

    // Refresh button
    refreshButton.addEventListener('click', fetchLogs);

    // Filter events
    [dateFilter, playerFilter, keywordFilter].forEach(filter => {
        filter.addEventListener('input', applyFilters);
    });

    // WebSocket setup
    function setupWebSocket() {
        const ws = new WebSocket(`ws://192.168.23.26:${port}/ws?token=${token}`);
        ws.onmessage = (event) => {
            const newLog = JSON.parse(event.data);
            const activeTab = document.querySelector('.nav-button.active').dataset.tab;
            const actionType = {
                'command-logs': 'COMMAND',
                'interaction-logs': 'INTERACTION',
                'movement-logs': 'MOVEMENT',
                'chat-logs': 'CHAT',
                'other-logs': 'OTHER'
            }[activeTab];

            if (newLog.action_type === actionType) {
                window.allLogs = [...(window.allLogs || []), newLog].slice(-pageSize);
                totalLogs++;
                applyFilters();
                updatePagination();
            }
        };
        ws.onclose = () => {
            const errorDiv = document.createElement('div');
            errorDiv.textContent = 'WebSocket connection closed.';
            errorDiv.style.color = 'red';
            document.querySelector('.tab-content').prepend(errorDiv);
        };
    }
});