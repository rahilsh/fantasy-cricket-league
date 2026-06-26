const TABS = {
  user: [
    { id: 'games', label: 'Games' },
    { id: 'team', label: 'My Team' },
    { id: 'leaderboard', label: 'Leaderboard' },
  ],
  superadmin: [
    { id: 'games', label: 'Games' },
    { id: 'create', label: 'Create Game' },
  ],
};

const state = {
  session: loadSession(),
  messageTimer: null,
  authMode: 'user',
  userAuthMode: 'login',
  activeTab: 'games',
  selectedGame: null,
  games: [],
};

const $ = (id) => document.getElementById(id);

function loadSession() {
  try {
    return JSON.parse(localStorage.getItem('fcl.session')) ?? null;
  } catch {
    return null;
  }
}

function saveSession(session) {
  state.session = session;
  if (session) {
    localStorage.setItem('fcl.session', JSON.stringify(session));
  } else {
    localStorage.removeItem('fcl.session');
  }
}

function role() {
  return state.session?.role === 'SUPERADMIN' ? 'superadmin' : 'user';
}

function escapeHtml(value) {
  return String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (auth && state.session?.token) headers.Authorization = `Bearer ${state.session.token}`;

  const response = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (response.status === 401) {
    saveSession(null);
    renderApp();
    throw new Error('Session expired. Please log in again.');
  }
  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    throw new Error(errorBody?.message || response.statusText || 'Request failed');
  }
  if (response.status === 204) return null;
  return response.json();
}

function showMessage(message, type = 'info') {
  clearTimeout(state.messageTimer);
  const box = $('messageBox');
  box.className = `message ${type}`;
  box.textContent = message;
  box.hidden = false;
  state.messageTimer = window.setTimeout(() => { box.hidden = true; }, 5000);
}

function run(promise) {
  Promise.resolve(promise).catch((error) => showMessage(error.message, 'error'));
}

function tableMarkup(columns, rows) {
  if (!rows.length) return '<div class="muted empty">No records yet.</div>';
  const head = `<thead><tr>${columns.map((c) => `<th>${escapeHtml(c)}</th>`).join('')}</tr></thead>`;
  return `<table>${head}<tbody>${rows.join('')}</tbody></table>`;
}

function statusBadge(status) {
  const map = { CREATED: 'badge-created', IN_PROGRESS: 'badge-live', COMPLETED: 'badge-done' };
  return `<span class="badge ${map[status] ?? ''}">${escapeHtml(status)}</span>`;
}

/* ---------------- Auth ---------------- */

function applyAuthMode() {
  document.querySelectorAll('#authSwitch .seg').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.auth === state.authMode);
  });
  $('userAuth').hidden = state.authMode !== 'user';
  $('superadminAuth').hidden = state.authMode !== 'superadmin';
}

function applyUserAuthMode() {
  document.querySelectorAll('#userAuthMode .seg').forEach((btn) => {
    btn.classList.toggle('active', btn.dataset.mode === state.userAuthMode);
  });
  $('userLoginForm').hidden = state.userAuthMode !== 'login';
  $('userSignupForm').hidden = state.userAuthMode !== 'signup';
}

async function authenticate(path, userName, password) {
  const result = await api(path, {
    method: 'POST',
    auth: false,
    body: { userName, password },
  });
  saveSession({ token: result.accessToken, role: result.role, userName });
  renderApp();
  showMessage(`Signed in as ${userName}`, 'success');
}

/* ---------------- Rendering ---------------- */

function renderApp() {
  const authed = Boolean(state.session?.token);
  $('authView').hidden = authed;
  $('workspace').hidden = !authed;
  $('sessionBox').hidden = !authed;

  if (!authed) {
    applyAuthMode();
    applyUserAuthMode();
    return;
  }

  $('sessionRole').textContent = role() === 'superadmin' ? 'Superadmin' : 'User';
  $('sessionName').textContent = state.session.userName;

  const tabs = TABS[role()];
  if (!tabs.some((t) => t.id === state.activeTab)) state.activeTab = tabs[0].id;
  renderTabbar(tabs);
  renderActiveTab();
}

function renderTabbar(tabs) {
  $('tabbar').innerHTML = tabs
    .map((t) => `<button type="button" class="tab ${t.id === state.activeTab ? 'active' : ''}" data-tab="${t.id}">${escapeHtml(t.label)}</button>`)
    .join('');
}

function renderSelectedBanner() {
  const banner = $('selectedBanner');
  const game = state.selectedGame;
  if (!game) {
    banner.hidden = true;
    return;
  }
  banner.hidden = false;
  banner.innerHTML = `Selected game <strong>#${game.id}</strong> — ${escapeHtml(game.team1)} vs ${escapeHtml(game.team2)} ${statusBadge(game.status)}`;
}

function renderActiveTab() {
  document.querySelectorAll('.tab-panel').forEach((panel) => {
    const visible = panel.dataset.role === role() && panel.dataset.tab === state.activeTab;
    panel.hidden = !visible;
  });
  renderSelectedBanner();

  if (role() === 'user') {
    if (state.activeTab === 'games') run(loadGamesForUser());
    if (state.activeTab === 'team') run(renderMyTeam());
    if (state.activeTab === 'leaderboard') run(loadLeaderboard('user'));
  } else {
    if (state.activeTab === 'games') run(loadGamesForAdmin());
  }
}

/* ---------------- Games (shared load) ---------------- */

async function fetchGames() {
  const result = await api('/api/games?page=0&size=100&sort=id,desc');
  state.games = result.content ?? [];
  return state.games;
}

function refreshSelectedGame() {
  if (!state.selectedGame) return;
  const fresh = state.games.find((g) => g.id === state.selectedGame.id);
  if (fresh) state.selectedGame = fresh;
}

async function loadGamesForUser() {
  const games = await fetchGames();
  refreshSelectedGame();
  renderSelectedBanner();
  const rows = games.map((game) => `
    <tr>
      <td>#${game.id}</td>
      <td>${escapeHtml(game.team1)}</td>
      <td>${escapeHtml(game.team2)}</td>
      <td>${statusBadge(game.status)}</td>
      <td>${game.k}</td>
      <td><button class="btn ghost" data-action="select-game" data-id="${game.id}">
        ${state.selectedGame?.id === game.id ? 'Selected' : 'Select'}
      </button></td>
    </tr>`).join('');
  $('userGamesTable').innerHTML = tableMarkup(['ID', 'Team 1', 'Team 2', 'Status', 'K', ''], rows);
}

async function loadGamesForAdmin() {
  const games = await fetchGames();
  refreshSelectedGame();
  renderSelectedBanner();
  const rows = games.map((game) => `
    <tr>
      <td>#${game.id}</td>
      <td>${escapeHtml(game.team1)}</td>
      <td>${escapeHtml(game.team2)}</td>
      <td>${statusBadge(game.status)}</td>
      <td>${game.k}</td>
      <td>
        <div class="row-actions">
          ${game.status === 'CREATED' ? `<button class="btn ghost" data-action="game-start" data-id="${game.id}">Start</button>` : ''}
          ${game.status === 'IN_PROGRESS' ? `<button class="btn ghost" data-action="game-record" data-id="${game.id}">Record event</button>` : ''}
          ${game.status === 'IN_PROGRESS' ? `<button class="btn ghost" data-action="game-end" data-id="${game.id}">End</button>` : ''}
          <button class="btn ghost" data-action="game-board" data-id="${game.id}">Leaderboard</button>
          <button class="btn danger" data-action="game-delete" data-id="${game.id}">Delete</button>
        </div>
      </td>
    </tr>`).join('');
  $('adminGamesTable').innerHTML = tableMarkup(['ID', 'Team 1', 'Team 2', 'Status', 'K', 'Actions'], rows);
}

function selectGame(id) {
  const game = state.games.find((g) => g.id === Number(id));
  if (game) state.selectedGame = game;
  return game;
}

/* ---------------- User: My Team ---------------- */

async function findMyTeam(gameId) {
  const result = await api('/api/user-teams?page=0&size=100&sort=id,desc');
  return (result.content ?? []).find((team) => team.gameId === gameId) ?? null;
}

async function renderMyTeam() {
  const game = state.selectedGame;
  const stateBox = $('myTeamState');
  const form = $('teamForm');
  const deleteBtn = $('teamDeleteBtn');

  if (!game) {
    form.hidden = true;
    deleteBtn.hidden = true;
    stateBox.textContent = 'Select a game from the Games tab first.';
    return;
  }

  const team = await findMyTeam(game.id);
  const locked = game.status !== 'CREATED';
  form.hidden = false;
  $('teamId').value = team ? String(team.id) : '';
  $('teamPlayers').value = team ? Array.from(team.players).join(', ') : '';
  $('teamPlayers').disabled = locked;
  $('teamSubmitBtn').disabled = locked;
  $('teamSubmitBtn').textContent = team ? 'Update team' : 'Create team';
  deleteBtn.hidden = !team || locked;

  if (locked) {
    stateBox.innerHTML = team
      ? `Your team for game #${game.id} is locked because the game has started.`
      : `Game #${game.id} has already started — you can no longer create a team.`;
  } else {
    stateBox.innerHTML = team
      ? `Editing your team for game #${game.id} (points: <strong>${team.points}</strong>).`
      : `No team yet for game #${game.id}. Pick up to 11 players below.`;
  }
}

function parsePlayers(input) {
  const players = input.split(',').map((v) => Number(v.trim())).filter((v) => Number.isFinite(v));
  if (!players.length) throw new Error('Enter at least one player id.');
  if (players.length > 11) throw new Error('A team can have at most 11 players.');
  return players;
}

async function submitTeam(event) {
  event.preventDefault();
  const game = state.selectedGame;
  if (!game) throw new Error('Select a game first.');
  const id = $('teamId').value.trim();
  const payload = {
    gameId: game.id,
    userName: state.session.userName,
    players: parsePlayers($('teamPlayers').value),
    points: 0,
  };
  if (id) {
    await api(`/api/user-teams/${id}`, { method: 'PUT', body: payload });
    showMessage('Team updated', 'success');
  } else {
    await api('/api/user-teams', { method: 'POST', body: payload });
    showMessage('Team created', 'success');
  }
  await renderMyTeam();
}

async function deleteMyTeam() {
  const id = $('teamId').value.trim();
  if (!id) return;
  if (!confirm('Delete your team for this game?')) return;
  await api(`/api/user-teams/${id}`, { method: 'DELETE' });
  showMessage('Team deleted', 'success');
  await renderMyTeam();
}

/* ---------------- Leaderboard ---------------- */

async function loadLeaderboard(view) {
  const game = state.selectedGame;
  const box = view === 'user' ? $('userLeaderboardBox') : $('adminLeaderboardBox');
  if (!game) {
    box.innerHTML = '<div class="muted empty">Select a game first.</div>';
    return;
  }
  const entries = await api(`/api/games/${game.id}/leaderboard`);
  const rows = (entries ?? []).map((entry, index) => `
    <tr>
      <td>${index + 1}</td>
      <td>${escapeHtml(entry.userName)}</td>
      <td>${entry.points}</td>
    </tr>`).join('');
  box.innerHTML = tableMarkup(['Rank', 'User', 'Points'], rows);
}

/* ---------------- Superadmin: ball events & games ---------------- */

function openBallEventForm(game) {
  state.selectedGame = game;
  renderSelectedBanner();
  $('ballEventForm').hidden = false;
  $('ballEventGameLabel').textContent = `Game #${game.id}: ${game.team1} vs ${game.team2}`;
  $('ballEventForm').scrollIntoView({ behavior: 'smooth', block: 'center' });
}

async function submitBallEvent(event) {
  event.preventDefault();
  const game = state.selectedGame;
  if (!game) throw new Error('Select a game to record an event.');
  await api(`/api/games/${game.id}/plays`, {
    method: 'POST',
    body: {
      batsman: Number($('ballBatsman').value),
      bowler: Number($('ballBowler').value),
      outcome: Number($('ballOutcome').value),
    },
  });
  showMessage(`Ball event recorded for game #${game.id}`, 'success');
  $('ballBatsman').value = '';
  $('ballBowler').value = '';
  await loadAdminLeaderboard(game);
}

async function loadAdminLeaderboard(game) {
  state.selectedGame = game;
  renderSelectedBanner();
  $('adminLeaderboardCard').hidden = false;
  $('adminLeaderboardLabel').textContent = `Game #${game.id}: ${game.team1} vs ${game.team2}`;
  await loadLeaderboard('admin');
}

async function startGame(id) {
  await api(`/api/games/${id}/start`, { method: 'POST' });
  showMessage(`Game #${id} started`, 'success');
  await loadGamesForAdmin();
}

async function endGame(id) {
  await api(`/api/games/${id}/end`, { method: 'POST' });
  showMessage(`Game #${id} completed`, 'success');
  await loadGamesForAdmin();
}

async function deleteGame(id) {
  if (!confirm(`Delete game #${id}?`)) return;
  await api(`/api/games/${id}`, { method: 'DELETE' });
  if (state.selectedGame?.id === Number(id)) state.selectedGame = null;
  showMessage(`Game #${id} deleted`, 'success');
  await loadGamesForAdmin();
}

async function submitGame(event) {
  event.preventDefault();
  const created = await api('/api/games', {
    method: 'POST',
    body: {
      team1: $('gameTeam1').value.trim(),
      team2: $('gameTeam2').value.trim(),
      k: Number($('gameK').value),
    },
  });
  showMessage(`Game #${created.id} created`, 'success');
  $('gameForm').reset();
  $('gameK').value = '5';
  state.activeTab = 'games';
  renderApp();
}

/* ---------------- Event wiring ---------------- */

function handleGamesAction(view, event) {
  const button = event.target.closest('button[data-action]');
  if (!button) return;
  const { action, id } = button.dataset;

  if (action === 'select-game') {
    const game = selectGame(id);
    if (game) {
      showMessage(`Selected game #${game.id}`, 'success');
      state.activeTab = 'team';
      renderApp();
    }
    return;
  }
  if (action === 'game-start') return run(startGame(id));
  if (action === 'game-end') return run(endGame(id));
  if (action === 'game-delete') return run(deleteGame(id));
  if (action === 'game-record') {
    const game = selectGame(id);
    if (game) openBallEventForm(game);
    return;
  }
  if (action === 'game-board') {
    const game = selectGame(id);
    if (game) run(loadAdminLeaderboard(game));
  }
}

function wireEvents() {
  document.querySelectorAll('#authSwitch .seg').forEach((btn) => {
    btn.addEventListener('click', () => { state.authMode = btn.dataset.auth; applyAuthMode(); });
  });
  document.querySelectorAll('#userAuthMode .seg').forEach((btn) => {
    btn.addEventListener('click', () => { state.userAuthMode = btn.dataset.mode; applyUserAuthMode(); });
  });

  $('userLoginForm').addEventListener('submit', (event) => {
    event.preventDefault();
    run(authenticate('/api/auth/login', $('userLoginName').value.trim(), $('userLoginPassword').value));
  });
  $('userSignupForm').addEventListener('submit', (event) => {
    event.preventDefault();
    run(authenticate('/api/auth/signup', $('signupName').value.trim(), $('signupPassword').value));
  });
  $('superadminLoginForm').addEventListener('submit', (event) => {
    event.preventDefault();
    run(authenticate('/api/auth/login', $('superLoginName').value.trim(), $('superLoginPassword').value));
  });

  $('logoutBtn').addEventListener('click', () => {
    saveSession(null);
    state.selectedGame = null;
    state.activeTab = 'games';
    renderApp();
    showMessage('Logged out', 'info');
  });

  $('tabbar').addEventListener('click', (event) => {
    const button = event.target.closest('button[data-tab]');
    if (!button) return;
    state.activeTab = button.dataset.tab;
    renderApp();
  });

  $('userLoadGamesBtn').addEventListener('click', () => run(loadGamesForUser()));
  $('userGamesTable').addEventListener('click', (event) => handleGamesAction('user', event));
  $('userLoadLeaderboardBtn').addEventListener('click', () => run(loadLeaderboard('user')));

  $('teamForm').addEventListener('submit', (event) => run(submitTeam(event)));
  $('teamDeleteBtn').addEventListener('click', () => run(deleteMyTeam()));

  $('adminLoadGamesBtn').addEventListener('click', () => run(loadGamesForAdmin()));
  $('adminGamesTable').addEventListener('click', (event) => handleGamesAction('admin', event));
  $('ballEventForm').addEventListener('submit', (event) => run(submitBallEvent(event)));
  $('gameForm').addEventListener('submit', (event) => run(submitGame(event)));
}

function bootstrap() {
  wireEvents();
  renderApp();
}

bootstrap();
