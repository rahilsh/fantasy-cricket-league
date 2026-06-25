const state = {
  session: loadSession(),
  messageTimer: null,
  games: { page: 0, size: 10, sort: 'id,desc' },
  users: { page: 0, size: 10, sort: 'id,desc' },
  teams: { page: 0, size: 10, sort: 'id,desc', gameId: '' },
  currentLeaderboardGameId: '',
};

const els = {
  authCard: document.getElementById('authCard'),
  sessionBar: document.getElementById('sessionBar'),
  workspace: document.getElementById('workspace'),
  sessionRole: document.getElementById('sessionRole'),
  sessionName: document.getElementById('sessionName'),
  workspaceTitle: document.getElementById('workspaceTitle'),
  workspaceSubtitle: document.getElementById('workspaceSubtitle'),
  messageBox: document.getElementById('messageBox'),
  logoutBtn: document.getElementById('logoutBtn'),
  refreshAllBtn: document.getElementById('refreshAllBtn'),
  gamesPanel: document.getElementById('gamesPanel'),
  usersPanel: document.getElementById('usersPanel'),
  teamsPanel: document.getElementById('teamsPanel'),
  signupForm: document.getElementById('signupForm'),
  loginForm: document.getElementById('loginForm'),
  signupUserName: document.getElementById('signupUserName'),
  signupPassword: document.getElementById('signupPassword'),
  loginUserName: document.getElementById('loginUserName'),
  loginPassword: document.getElementById('loginPassword'),
  gameForm: document.getElementById('gameForm'),
  gameId: document.getElementById('gameId'),
  gameTeam1: document.getElementById('gameTeam1'),
  gameTeam2: document.getElementById('gameTeam2'),
  gameK: document.getElementById('gameK'),
  gameSubmitBtn: document.getElementById('gameSubmitBtn'),
  gameResetBtn: document.getElementById('gameResetBtn'),
  gamesPage: document.getElementById('gamesPage'),
  gamesSize: document.getElementById('gamesSize'),
  gamesSort: document.getElementById('gamesSort'),
  loadGamesBtn: document.getElementById('loadGamesBtn'),
  gamesTable: document.getElementById('gamesTable'),
  playForm: document.getElementById('playForm'),
  playGameId: document.getElementById('playGameId'),
  playBatsman: document.getElementById('playBatsman'),
  playBowler: document.getElementById('playBowler'),
  playOutcome: document.getElementById('playOutcome'),
  leaderboardGameId: document.getElementById('leaderboardGameId'),
  loadLeaderboardBtn: document.getElementById('loadLeaderboardBtn'),
  leaderboardBox: document.getElementById('leaderboardBox'),
  userForm: document.getElementById('userForm'),
  userName: document.getElementById('userName'),
  userPassword: document.getElementById('userPassword'),
  userRole: document.getElementById('userRole'),
  usersPage: document.getElementById('usersPage'),
  usersSize: document.getElementById('usersSize'),
  usersSort: document.getElementById('usersSort'),
  loadUsersBtn: document.getElementById('loadUsersBtn'),
  usersTable: document.getElementById('usersTable'),
  userEditForm: document.getElementById('userEditForm'),
  editUserId: document.getElementById('editUserId'),
  editUserName: document.getElementById('editUserName'),
  deleteUserBtn: document.getElementById('deleteUserBtn'),
  teamForm: document.getElementById('teamForm'),
  teamId: document.getElementById('teamId'),
  teamGameId: document.getElementById('teamGameId'),
  teamUserName: document.getElementById('teamUserName'),
  teamPlayers: document.getElementById('teamPlayers'),
  teamPoints: document.getElementById('teamPoints'),
  teamSubmitBtn: document.getElementById('teamSubmitBtn'),
  teamResetBtn: document.getElementById('teamResetBtn'),
  teamsGameFilter: document.getElementById('teamsGameFilter'),
  teamsPage: document.getElementById('teamsPage'),
  teamsSize: document.getElementById('teamsSize'),
  teamsSort: document.getElementById('teamsSort'),
  loadTeamsBtn: document.getElementById('loadTeamsBtn'),
  teamsTable: document.getElementById('teamsTable'),
  teamEditForm: document.getElementById('teamEditForm'),
  editTeamId: document.getElementById('editTeamId'),
  editTeamGameId: document.getElementById('editTeamGameId'),
  editTeamUserName: document.getElementById('editTeamUserName'),
  editTeamPlayers: document.getElementById('editTeamPlayers'),
  editTeamPoints: document.getElementById('editTeamPoints'),
  deleteTeamBtn: document.getElementById('deleteTeamBtn'),
};

const ROLE_LABELS = {
  USER: 'User',
  ADMIN: 'Admin',
  SUPERADMIN: 'Superadmin',
};

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

function isLoggedIn() {
  return Boolean(state.session?.token);
}

function isAdmin() {
  return ['ADMIN', 'SUPERADMIN'].includes(state.session?.role);
}

function isSuperadmin() {
  return state.session?.role === 'SUPERADMIN';
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function parseJsonResponse(response) {
  if (!response.ok) {
    return response.json().catch(() => null).then((body) => {
      const message = body?.message || response.statusText || 'Request failed';
      throw new Error(message);
    });
  }
  if (response.status === 204) {
    return null;
  }
  return response.json();
}

async function api(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (auth && state.session?.token) {
    headers.Authorization = `Bearer ${state.session.token}`;
  }

  const response = await fetch(path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  return parseJsonResponse(response);
}

function showMessage(message, type = 'info') {
  clearTimeout(state.messageTimer);
  els.messageBox.className = `message card ${type}`;
  els.messageBox.textContent = message;
  els.messageBox.hidden = false;
  state.messageTimer = window.setTimeout(() => {
    els.messageBox.hidden = true;
  }, 6000);
}

function hideMessage() {
  els.messageBox.hidden = true;
  els.messageBox.textContent = '';
}

function setSessionUi() {
  const session = state.session;
  els.sessionRole.textContent = session ? ROLE_LABELS[session.role] ?? session.role : 'Signed out';
  els.sessionName.textContent = session ? `${session.userName}` : 'No token';
  els.sessionBar.hidden = !session;
  els.workspace.hidden = !session;
  els.authCard.classList.toggle('hidden', Boolean(session));

  if (!session) {
    els.workspaceTitle.textContent = 'Workspace';
    els.workspaceSubtitle.textContent = '';
    els.gamesPanel.classList.add('hidden');
    els.usersPanel.classList.add('hidden');
    els.teamsPanel.classList.add('hidden');
    return;
  }

  els.workspaceTitle.textContent = `${ROLE_LABELS[session.role] ?? session.role} workspace`;
  els.workspaceSubtitle.textContent = `Signed in as ${session.userName}`;

  els.gamesPanel.classList.toggle('hidden', !isAdmin());
  els.usersPanel.classList.toggle('hidden', !isAdmin());
  els.teamsPanel.classList.remove('hidden');

  els.teamUserName.value = session.userName || '';
  els.teamUserName.disabled = !isAdmin();

  els.userRole.innerHTML = isSuperadmin()
    ? '<option value="USER">User</option><option value="ADMIN">Admin</option>'
    : '<option value="USER">User</option>';
}

function clearGameForm() {
  els.gameId.value = '';
  els.gameTeam1.value = '';
  els.gameTeam2.value = '';
  els.gameK.value = '5';
  els.gameSubmitBtn.textContent = 'Create game';
}

function clearUserForm() {
  els.userName.value = '';
  els.userPassword.value = '';
  els.userRole.value = 'USER';
}

function clearUserEditForm() {
  els.editUserId.value = '';
  els.editUserName.value = '';
}

function clearTeamForm() {
  els.teamId.value = '';
  els.teamGameId.value = '';
  els.teamUserName.value = state.session?.userName || '';
  els.teamPlayers.value = '';
  els.teamPoints.value = '0';
  els.teamSubmitBtn.textContent = 'Create team';
}

function clearTeamEditForm() {
  els.editTeamId.value = '';
  els.editTeamGameId.value = '';
  els.editTeamUserName.value = '';
  els.editTeamPlayers.value = '';
  els.editTeamPoints.value = '0';
}

function parsePlayers(input) {
  const players = input
    .split(',')
    .map((value) => Number(value.trim()))
    .filter((value) => Number.isFinite(value));

  if (players.length === 0) {
    throw new Error('Players must contain at least one player id');
  }

  return players;
}

function tableMarkup(columns, rows) {
  if (!rows.length) {
    return '<div class="muted">No records found.</div>';
  }

  const header = `<thead><tr>${columns.map((column) => `<th>${escapeHtml(column)}</th>`).join('')}</tr></thead>`;
  const body = `<tbody>${rows.join('')}</tbody>`;
  return `<table>${header}${body}</table>`;
}

function renderGamesTable(items) {
  const rows = items.map((game) => `
    <tr>
      <td>#${game.id}</td>
      <td>${escapeHtml(game.team1)}</td>
      <td>${escapeHtml(game.team2)}</td>
      <td>${escapeHtml(game.status)}</td>
      <td>${game.k}</td>
      <td>
        <div class="row-actions">
          <button class="btn ghost" data-action="game-edit" data-id="${game.id}">Edit</button>
          <button class="btn ghost" data-action="game-leaderboard" data-id="${game.id}">Leaderboard</button>
          <button class="btn ghost" data-action="game-play" data-id="${game.id}">Play</button>
          <button class="btn ghost" data-action="game-start" data-id="${game.id}">Start</button>
          <button class="btn ghost" data-action="game-end" data-id="${game.id}">End</button>
          <button class="btn danger" data-action="game-delete" data-id="${game.id}">Delete</button>
        </div>
      </td>
    </tr>`).join('');

  els.gamesTable.innerHTML = tableMarkup(['ID', 'Team 1', 'Team 2', 'Status', 'K', 'Actions'], rows);
}

function renderUsersTable(items) {
  const rows = items.map((user) => `
    <tr>
      <td>#${user.id}</td>
      <td>${escapeHtml(user.userName)}</td>
      <td>${escapeHtml(user.role)}</td>
      <td>
        <div class="row-actions">
          <button class="btn ghost" data-action="user-edit" data-id="${user.id}" data-name="${escapeHtml(user.userName)}">Edit</button>
          <button class="btn danger" data-action="user-delete" data-id="${user.id}" data-name="${escapeHtml(user.userName)}">Delete</button>
        </div>
      </td>
    </tr>`).join('');

  els.usersTable.innerHTML = tableMarkup(['ID', 'Username', 'Role', 'Actions'], rows);
}

function renderTeamsTable(items) {
  const rows = items.map((team) => `
    <tr>
      <td>#${team.id}</td>
      <td>${team.gameId}</td>
      <td>${escapeHtml(team.userName)}</td>
      <td>${team.points}</td>
      <td>${escapeHtml(Array.from(team.players).join(', '))}</td>
      <td>
        <div class="row-actions">
          <button class="btn ghost" data-action="team-edit" data-id="${team.id}">Edit</button>
          <button class="btn ghost" data-action="team-load-game" data-id="${team.gameId}">Filter game</button>
          <button class="btn danger" data-action="team-delete" data-id="${team.id}">Delete</button>
        </div>
      </td>
    </tr>`).join('');

  els.teamsTable.innerHTML = tableMarkup(['ID', 'Game', 'User', 'Points', 'Players', 'Actions'], rows);
}

function renderLeaderboard(items) {
  const rows = items.map((entry, index) => `
    <tr>
      <td>${index + 1}</td>
      <td>${escapeHtml(entry.userName)}</td>
      <td>${entry.points}</td>
    </tr>`).join('');

  els.leaderboardBox.innerHTML = tableMarkup(['Rank', 'User', 'Points'], rows);
}

function currentPageOptions(kind) {
  const config = state[kind];
  return {
    page: Number(config.page),
    size: Number(config.size),
    sort: config.sort,
  };
}

async function loadGames() {
  if (!isAdmin()) return;
  const { page, size, sort } = currentPageOptions('games');
  const result = await api(`/api/games?page=${page}&size=${size}&sort=${encodeURIComponent(sort)}`);
  renderGamesTable(result.content ?? []);
  showMessage('Games loaded', 'success');
}

async function loadUsers() {
  if (!isAdmin()) return;
  const { page, size, sort } = currentPageOptions('users');
  const result = await api(`/api/users?page=${page}&size=${size}&sort=${encodeURIComponent(sort)}`);
  renderUsersTable(result.content ?? []);
  showMessage('Users loaded', 'success');
}

async function loadTeams() {
  const { page, size, sort } = currentPageOptions('teams');
  const gameId = els.teamsGameFilter.value.trim();
  const params = new URLSearchParams({ page, size, sort });
  if (gameId) {
    params.set('gameId', gameId);
  }
  const result = await api(`/api/user-teams?${params.toString()}`);
  renderTeamsTable(result.content ?? []);
  showMessage('Teams loaded', 'success');
}

async function loadLeaderboard(gameId) {
  if (!gameId) {
    throw new Error('Game ID is required');
  }
  const result = await api(`/api/games/${gameId}/leaderboard`);
  state.currentLeaderboardGameId = String(gameId);
  els.leaderboardGameId.value = String(gameId);
  renderLeaderboard(result ?? []);
}

async function loadGameForEdit(gameId) {
  const game = await api(`/api/games/${gameId}`);
  els.gameId.value = String(game.id);
  els.gameTeam1.value = game.team1;
  els.gameTeam2.value = game.team2;
  els.gameK.value = game.k;
  els.gameSubmitBtn.textContent = 'Update game';
  els.gameForm.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function loadUserForEdit(userId, userName) {
  els.editUserId.value = String(userId);
  els.editUserName.value = userName;
  els.userEditForm.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function loadTeamForEdit(teamId) {
  const team = await api(`/api/user-teams/${teamId}`);
  els.teamId.value = String(team.id);
  els.teamGameId.value = String(team.gameId);
  els.teamUserName.value = team.userName;
  els.teamPlayers.value = Array.from(team.players).join(', ');
  els.teamPoints.value = team.points;
  els.teamSubmitBtn.textContent = 'Update team';
  els.teamForm.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function submitSignup(event) {
  event.preventDefault();
  const result = await api('/api/auth/signup', {
    method: 'POST',
    auth: false,
    body: {
      userName: els.signupUserName.value.trim(),
      password: els.signupPassword.value,
    },
  });
  saveSession({
    token: result.accessToken,
    role: result.role,
    userName: els.signupUserName.value.trim(),
  });
  els.signupForm.reset();
  setSessionUi();
  await refreshAll();
  showMessage('Account created and signed in', 'success');
}

async function submitLogin(event) {
  event.preventDefault();
  const userName = els.loginUserName.value.trim();
  const result = await api('/api/auth/login', {
    method: 'POST',
    auth: false,
    body: {
      userName,
      password: els.loginPassword.value,
    },
  });
  saveSession({
    token: result.accessToken,
    role: result.role,
    userName,
  });
  els.loginForm.reset();
  setSessionUi();
  await refreshAll();
  showMessage('Signed in', 'success');
}

async function submitGame(event) {
  event.preventDefault();
  const id = els.gameId.value.trim();
  const payload = {
    team1: els.gameTeam1.value.trim(),
    team2: els.gameTeam2.value.trim(),
    k: Number(els.gameK.value),
  };
  if (id) {
    await api(`/api/games/${id}`, { method: 'PUT', body: payload });
    showMessage(`Game #${id} updated`, 'success');
  } else {
    const created = await api('/api/games', { method: 'POST', body: payload });
    els.gameId.value = String(created.id);
    showMessage(`Game #${created.id} created`, 'success');
  }
  clearGameForm();
  await loadGames();
}

async function submitPlay(event) {
  event.preventDefault();
  const gameId = Number(els.playGameId.value);
  const result = await api(`/api/games/${gameId}/plays`, {
    method: 'POST',
    body: {
      batsman: Number(els.playBatsman.value),
      bowler: Number(els.playBowler.value),
      outcome: Number(els.playOutcome.value),
    },
  });
  showMessage(`Ball event recorded for game #${result.gameId}`, 'success');
  await loadTeams();
  if (state.currentLeaderboardGameId === String(gameId)) {
    await loadLeaderboard(gameId);
  }
}

async function submitUserCreate(event) {
  event.preventDefault();
  const payload = {
    userName: els.userName.value.trim(),
    password: els.userPassword.value,
    role: els.userRole.value,
  };
  const created = await api('/api/users', { method: 'POST', body: payload });
  showMessage(`User #${created.id} created as ${created.role}`, 'success');
  els.userForm.reset();
  els.userRole.innerHTML = isSuperadmin()
    ? '<option value="USER">User</option><option value="ADMIN">Admin</option>'
    : '<option value="USER">User</option>';
  await loadUsers();
}

async function submitUserEdit(event) {
  event.preventDefault();
  const id = Number(els.editUserId.value);
  const updated = await api(`/api/users/${id}`, {
    method: 'PUT',
    body: {
      userName: els.editUserName.value.trim(),
    },
  });
  showMessage(`User #${updated.id} updated`, 'success');
  clearUserEditForm();
  await loadUsers();
}

async function submitTeam(event) {
  event.preventDefault();
  const id = els.teamId.value.trim();
  const payload = {
    gameId: Number(els.teamGameId.value),
    userName: els.teamUserName.value.trim(),
    players: parsePlayers(els.teamPlayers.value),
    points: Number(els.teamPoints.value),
  };
  if (id) {
    const updated = await api(`/api/user-teams/${id}`, { method: 'PUT', body: payload });
    showMessage(`Team #${updated.id} updated`, 'success');
  } else {
    const created = await api('/api/user-teams', { method: 'POST', body: payload });
    showMessage(`Team #${created.id} created`, 'success');
  }
  clearTeamForm();
  await loadTeams();
}

async function submitTeamEdit(event) {
  event.preventDefault();
  const id = Number(els.editTeamId.value);
  const payload = {
    gameId: Number(els.editTeamGameId.value),
    userName: els.editTeamUserName.value.trim(),
    players: parsePlayers(els.editTeamPlayers.value),
    points: Number(els.editTeamPoints.value),
  };
  const updated = await api(`/api/user-teams/${id}`, { method: 'PUT', body: payload });
  showMessage(`Team #${updated.id} updated`, 'success');
  clearTeamEditForm();
  await loadTeams();
}

async function deleteGame(gameId) {
  await api(`/api/games/${gameId}`, { method: 'DELETE' });
  showMessage(`Game #${gameId} deleted`, 'success');
  await loadGames();
}

async function deleteUser(userId) {
  await api(`/api/users/${userId}`, { method: 'DELETE' });
  showMessage(`User #${userId} deleted`, 'success');
  await loadUsers();
}

async function deleteTeam(teamId) {
  await api(`/api/user-teams/${teamId}`, { method: 'DELETE' });
  showMessage(`Team #${teamId} deleted`, 'success');
  await loadTeams();
}

async function startGame(gameId) {
  const game = await api(`/api/games/${gameId}/start`, { method: 'POST' });
  showMessage(`Game #${game.id} started`, 'success');
  await loadGames();
}

async function endGame(gameId) {
  const game = await api(`/api/games/${gameId}/end`, { method: 'POST' });
  showMessage(`Game #${game.id} completed`, 'success');
  await loadGames();
}

async function handleTableAction(event) {
  const button = event.target.closest('button[data-action]');
  if (!button) return;

  const { action, id, name } = button.dataset;

  try {
    if (action === 'game-edit') return loadGameForEdit(id);
    if (action === 'game-leaderboard') return loadLeaderboard(id);
    if (action === 'game-play') {
      els.playGameId.value = String(id);
      els.playForm.scrollIntoView({ behavior: 'smooth', block: 'start' });
      return;
    }
    if (action === 'game-start') return startGame(id);
    if (action === 'game-end') return endGame(id);
    if (action === 'game-delete') {
      if (confirm(`Delete game #${id}?`)) return deleteGame(id);
      return;
    }
    if (action === 'user-edit') return loadUserForEdit(id, name);
    if (action === 'user-delete') {
      if (confirm(`Delete user #${id}?`)) return deleteUser(id);
      return;
    }
    if (action === 'team-edit') return loadTeamForEdit(id);
    if (action === 'team-load-game') {
      els.teamsGameFilter.value = id;
      return loadTeams();
    }
    if (action === 'team-delete') {
      if (confirm(`Delete team #${id}?`)) return deleteTeam(id);
      return;
    }
  } catch (error) {
    showMessage(error.message, 'error');
  }
}

async function refreshAll() {
  const tasks = [];
  if (isAdmin()) {
    tasks.push(loadGames(), loadUsers());
  }
  tasks.push(loadTeams());
  await Promise.all(tasks);
}

function initFormDefaults() {
  clearGameForm();
  clearUserForm();
  clearUserEditForm();
  clearTeamForm();
  clearTeamEditForm();

  if (!state.session) {
    els.teamUserName.disabled = false;
  }
}

function initRoleAwareUi() {
  if (!state.session) return;
  els.userRole.innerHTML = isSuperadmin()
    ? '<option value="USER">User</option><option value="ADMIN">Admin</option>'
    : '<option value="USER">User</option>';
  els.teamUserName.value = state.session.userName || '';
  els.teamUserName.disabled = !isAdmin();
}

function wireEvents() {
  els.signupForm.addEventListener('submit', (event) => {
    submitSignup(event).catch((error) => showMessage(error.message, 'error'));
  });
  els.loginForm.addEventListener('submit', (event) => {
    submitLogin(event).catch((error) => showMessage(error.message, 'error'));
  });
  els.logoutBtn.addEventListener('click', () => {
    saveSession(null);
    setSessionUi();
    hideMessage();
  });
  els.refreshAllBtn.addEventListener('click', () => {
    refreshAll().catch((error) => showMessage(error.message, 'error'));
  });

  els.gameForm.addEventListener('submit', (event) => {
    submitGame(event).catch((error) => showMessage(error.message, 'error'));
  });
  els.gameResetBtn.addEventListener('click', clearGameForm);
  els.loadGamesBtn.addEventListener('click', () => {
    state.games.page = els.gamesPage.value;
    state.games.size = els.gamesSize.value;
    state.games.sort = els.gamesSort.value;
    loadGames().catch((error) => showMessage(error.message, 'error'));
  });
  els.gamesTable.addEventListener('click', handleTableAction);
  els.loadLeaderboardBtn.addEventListener('click', () => {
    const gameId = els.leaderboardGameId.value.trim();
    loadLeaderboard(gameId).catch((error) => showMessage(error.message, 'error'));
  });
  els.playForm.addEventListener('submit', (event) => {
    submitPlay(event).catch((error) => showMessage(error.message, 'error'));
  });

  els.userForm.addEventListener('submit', (event) => {
    submitUserCreate(event).catch((error) => showMessage(error.message, 'error'));
  });
  els.loadUsersBtn.addEventListener('click', () => {
    state.users.page = els.usersPage.value;
    state.users.size = els.usersSize.value;
    state.users.sort = els.usersSort.value;
    loadUsers().catch((error) => showMessage(error.message, 'error'));
  });
  els.usersTable.addEventListener('click', handleTableAction);
  els.userEditForm.addEventListener('submit', (event) => {
    submitUserEdit(event).catch((error) => showMessage(error.message, 'error'));
  });
  els.deleteUserBtn.addEventListener('click', () => {
    const userId = els.editUserId.value.trim();
    if (!userId) {
      showMessage('Enter a user id to delete', 'error');
      return;
    }
    if (confirm(`Delete user #${userId}?`)) {
      deleteUser(userId).catch((error) => showMessage(error.message, 'error'));
    }
  });

  els.teamForm.addEventListener('submit', (event) => {
    submitTeam(event).catch((error) => showMessage(error.message, 'error'));
  });
  els.teamResetBtn.addEventListener('click', clearTeamForm);
  els.loadTeamsBtn.addEventListener('click', () => {
    state.teams.page = els.teamsPage.value;
    state.teams.size = els.teamsSize.value;
    state.teams.sort = els.teamsSort.value;
    state.teams.gameId = els.teamsGameFilter.value;
    loadTeams().catch((error) => showMessage(error.message, 'error'));
  });
  els.teamsTable.addEventListener('click', handleTableAction);
  els.teamEditForm.addEventListener('submit', (event) => {
    submitTeamEdit(event).catch((error) => showMessage(error.message, 'error'));
  });
  els.deleteTeamBtn.addEventListener('click', () => {
    const teamId = els.editTeamId.value.trim();
    if (!teamId) {
      showMessage('Enter a team id to delete', 'error');
      return;
    }
    if (confirm(`Delete team #${teamId}?`)) {
      deleteTeam(teamId).catch((error) => showMessage(error.message, 'error'));
    }
  });
}

async function bootstrap() {
  wireEvents();
  setSessionUi();
  initFormDefaults();
  initRoleAwareUi();

  if (state.session) {
    try {
      await refreshAll();
    } catch (error) {
      saveSession(null);
      setSessionUi();
      showMessage(error.message, 'error');
    }
  }
}

bootstrap().catch((error) => showMessage(error.message, 'error'));
