// ============================================
// ADMIN.JS - Sistema com Abas e Lazy Loading
// ============================================

console.log('✅ admin.js carregado');

// ==================== Estado ====================

// Controle de dados carregados (lazy loading)
const loadedTabs = {
	users: false,
	banned: false,
	alarming: false,
	comments: false,
	scrapper: false
};

// Estado da paginação para comentários
let alarmingPage = 0;
let allCommentsPage = 0;
const pageSize = 10;

// Usuário atual (será carregado sob demanda)
let currentUser = null;

// ==================== Helpers ====================

async function httpPost(url, body) {
	const res = await fetch(url, {
		method: "POST",
		body: body,
		headers: {
			"Content-type": "application/json; charset=UTF-8"
		},
		credentials: 'same-origin'
	});
	return res.status;
}

function escapeHtml(text) {
	if (!text) return '';
	const map = {
		'&': '&amp;',
		'<': '&lt;',
		'>': '&gt;',
		'"': '&quot;',
		"'": '&#039;'
	};
	return String(text).replace(/[&<>"']/g, char => map[char]);
}

function showLoading(container) {
	container.innerHTML = `
		<div class="loading-overlay">
			<div class="spinner"></div>
			<div class="loading-overlay-text">Carregando...</div>
		</div>
	`;
}

function showError(container, message) {
	container.innerHTML = `
		<div class="error-message">
			<span class="error-icon">⚠️</span>
			${message}
		</div>
	`;
}

// Helper para adicionar loading em botões de atualizar
function setRefreshButtonLoading(tabName, isLoading) {
	// Seleciona o botão de atualizar da aba correspondente
	const tabPane = document.getElementById(`tab-${tabName}`);
	if (!tabPane) return;
	
	const refreshBtn = tabPane.querySelector('.section-header .btn-secondary');
	if (!refreshBtn) return;
	
	if (isLoading) {
		refreshBtn.disabled = true;
		refreshBtn.dataset.originalText = refreshBtn.innerHTML;
		refreshBtn.innerHTML = '<span class="btn-spinner"></span> Atualizando...';
	} else {
		refreshBtn.disabled = false;
		if (refreshBtn.dataset.originalText) {
			refreshBtn.innerHTML = refreshBtn.dataset.originalText;
		}
	}
}

async function getCurrentUser() {
	if (currentUser) return currentUser;
	
	try {
		const response = await fetch('/api/me', { credentials: 'same-origin' });
		if (response.ok) {
			currentUser = await response.json();
		}
	} catch (error) {
		console.error('Erro ao buscar usuário atual:', error);
	}
	return currentUser;
}

// ==================== Sistema de Abas ====================

function switchTab(tabName) {
	// Atualizar botões das abas
	document.querySelectorAll('.tab-btn').forEach(btn => {
		btn.classList.toggle('active', btn.dataset.tab === tabName);
	});
	
	// Atualizar painéis de conteúdo
	document.querySelectorAll('.tab-pane').forEach(pane => {
		pane.classList.toggle('active', pane.id === `tab-${tabName}`);
	});
	
	// Carregar dados se ainda não foram carregados
	if (!loadedTabs[tabName]) {
		loadTabData(tabName);
	}
}

function loadTabData(tabName) {
	switch (tabName) {
		case 'users':
			loadUsersData();
			break;
		case 'banned':
			loadBannedData();
			break;
		case 'alarming':
			loadAlarmingData();
			break;
		case 'comments':
			loadCommentsData();
			break;
		case 'scrapper':
			loadScrapperData();
			break;
	}
}

// ==================== USERS TAB ====================

async function loadUsersData(forceRefresh = false) {
	const container = document.getElementById('users-content');
	if (!container) return;
	
	if (!forceRefresh && loadedTabs.users) return;
	
	showLoading(container);
	setRefreshButtonLoading('users', true);
	
	try {
		const [usersResponse, user] = await Promise.all([
			fetch('/api/admin/users', { method: "POST", credentials: 'same-origin' }),
			getCurrentUser()
		]);
		
		if (!usersResponse.ok) throw new Error('Erro ao carregar usuários');
		const users = await usersResponse.json();
		
		// Atualizar badge
		updateBadge('users-badge', users.length);
		
		// Renderizar tabela
		renderUsersTable(container, users, user);
		loadedTabs.users = true;
		
	} catch (error) {
		console.error('Erro:', error);
		showError(container, 'Erro ao carregar usuários. Clique em Atualizar para tentar novamente.');
	} finally {
		setRefreshButtonLoading('users', false);
	}
}

function renderUsersTable(container, users, currentUser) {
	if (users.length === 0) {
		container.innerHTML = '<div class="no-users">Nenhum usuário cadastrado</div>';
		return;
	}
	
	const html = `
		<div class="users-table-container">
			<table class="users-table">
				<thead>
					<tr>
						<th>Nome</th>
						<th>Email</th>
						<th>Matrícula</th>
						<th>Curso</th>
						<th>Tipo</th>
						<th>Ações</th>
					</tr>
				</thead>
				<tbody>
					${users.map(user => renderUserRow(user, currentUser)).join('')}
				</tbody>
			</table>
		</div>
	`;
	container.innerHTML = html;
	
	// Adicionar event listeners aos botões
	users.forEach(user => {
		attachUserButtonListeners(user, currentUser);
	});
}

function renderUserRow(user, currentUser) {
	const isCurrentUser = currentUser && currentUser.email === user.email;
	const disabledAttr = isCurrentUser ? 'disabled' : '';
	
	return `
		<tr id="user-row-${escapeHtml(user.email)}">
			<td>${escapeHtml(user.nome || 'N/A')}</td>
			<td>${escapeHtml(user.email || 'N/A')}</td>
			<td>${escapeHtml(user.matricula || 'N/A')}</td>
			<td>${escapeHtml(user.curso || 'N/A')}</td>
			<td>
				<span class="user-type ${user.admin ? 'admin' : 'user'}" id="type-${escapeHtml(user.email)}">
					${user.admin ? 'Admin' : 'Usuário'}
				</span>
			</td>
			<td class="actions">
				<button class="btn btn-secondary btn-toggle-admin" data-email="${escapeHtml(user.email)}" data-admin="${user.admin}" ${disabledAttr}>
					${user.admin ? 'Remover Admin' : 'Tornar Admin'}
				</button>
				<button class="btn btn-danger btn-delete-user" data-email="${escapeHtml(user.email)}" data-nome="${escapeHtml(user.nome)}" ${disabledAttr}>
					Excluir
				</button>
				<button class="btn btn-warning btn-ban-user" data-email="${escapeHtml(user.email)}" data-nome="${escapeHtml(user.nome)}" ${disabledAttr}>
					Banir
				</button>
			</td>
		</tr>
	`;
}

function attachUserButtonListeners(user, currentUser) {
	const row = document.getElementById(`user-row-${user.email}`);
	if (!row) return;
	
	// Toggle Admin
	const toggleBtn = row.querySelector('.btn-toggle-admin');
	if (toggleBtn && !toggleBtn.disabled) {
		toggleBtn.onclick = () => toggleAdmin(user.email, toggleBtn);
	}
	
	// Delete
	const deleteBtn = row.querySelector('.btn-delete-user');
	if (deleteBtn && !deleteBtn.disabled) {
		deleteBtn.onclick = () => deleteUser(user.email, user.nome, deleteBtn);
	}
	
	// Ban
	const banBtn = row.querySelector('.btn-ban-user');
	if (banBtn && !banBtn.disabled) {
		banBtn.onclick = () => banUser(user.email, user.nome, banBtn);
	}
}

async function toggleAdmin(email, button) {
	const originalText = button.textContent;
	button.disabled = true;
	button.innerHTML = '<span class="btn-spinner"></span> Processando...';
	
	const code = await httpPost("/api/admin/toggle-admin", JSON.stringify({ email }));
	
	button.disabled = false;
	
	if (code === 200) {
		const isAdmin = button.dataset.admin === 'true';
		const newIsAdmin = !isAdmin;
		
		button.dataset.admin = newIsAdmin;
		button.textContent = newIsAdmin ? 'Remover Admin' : 'Tornar Admin';
		
		const typeSpan = document.getElementById(`type-${email}`);
		if (typeSpan) {
			typeSpan.className = `user-type ${newIsAdmin ? 'admin' : 'user'}`;
			typeSpan.textContent = newIsAdmin ? 'Admin' : 'Usuário';
		}
	} else if (code === 400) {
		button.textContent = originalText;
		alert('Erro: Não é possível alterar o status de admin do próprio usuário.');
	} else if (code === 401) {
		document.location.href = '/login?error=notAuthenticated';
	} else {
		button.textContent = originalText;
		alert('Erro ao alterar status de admin.');
	}
}

async function deleteUser(email, nome, button) {
	if (!confirm(`Confirma a exclusão do usuário ${nome}?\n\nO usuário poderá criar uma nova conta.`)) return;
	
	const originalText = button.textContent;
	const row = document.getElementById(`user-row-${email}`);
	
	button.disabled = true;
	button.innerHTML = '<span class="btn-spinner"></span> Excluindo...';
	
	// Adicionar visual de loading na row
	if (row) row.classList.add('table-row-loading');
	
	const code = await httpPost("/api/admin/delete-user", JSON.stringify({ email }));
	
	button.disabled = false;
	button.textContent = originalText;
	if (row) row.classList.remove('table-row-loading');
	
	if (code === 200) {
		if (row) {
			row.style.animation = 'fadeOut 0.3s ease-out';
			setTimeout(() => row.remove(), 300);
		}
	} else if (code === 400) {
		alert('Erro: Não é possível excluir o próprio usuário.');
	} else if (code === 401) {
		document.location.href = '/login?error=notAuthenticated';
	} else {
		alert('Erro ao excluir usuário.');
	}
}

async function banUser(email, nome, button) {
	const motivo = prompt(`Banir usuário ${nome}?\n\nMotivo (opcional):`);
	if (motivo === null) return;
	
	const originalText = button.textContent;
	const row = document.getElementById(`user-row-${email}`);
	
	button.disabled = true;
	button.innerHTML = '<span class="btn-spinner"></span> Banindo...';
	
	// Adicionar visual de loading na row
	if (row) row.classList.add('table-row-loading');
	
	const code = await httpPost("/api/admin/ban-user", JSON.stringify({ email, motivo: motivo || null }));
	
	button.disabled = false;
	button.textContent = originalText;
	if (row) row.classList.remove('table-row-loading');
	
	if (code === 200) {
		if (row) {
			row.style.animation = 'fadeOut 0.3s ease-out';
			setTimeout(() => row.remove(), 300);
		}
		alert('Usuário banido com sucesso.');
		// Recarregar banidos se já carregados
		if (loadedTabs.banned) loadBannedData(true);
	} else if (code === 400) {
		alert('Erro: Não é possível banir o próprio usuário.');
	} else if (code === 401) {
		document.location.href = '/login?error=notAuthenticated';
	} else {
		alert('Erro ao banir usuário.');
	}
}

// ==================== BANNED TAB ====================

async function loadBannedData(forceRefresh = false) {
	const container = document.getElementById('banned-content');
	if (!container) return;
	
	if (!forceRefresh && loadedTabs.banned) return;
	
	showLoading(container);
	setRefreshButtonLoading('banned', true);
	
	try {
		const response = await fetch('/api/admin/banned-users', { method: "POST", credentials: 'same-origin' });
		if (!response.ok) throw new Error('Erro ao carregar banidos');
		
		const bannedUsers = await response.json();
		
		updateBadge('banned-badge', bannedUsers.length);
		renderBannedTable(container, bannedUsers);
		loadedTabs.banned = true;
		
	} catch (error) {
		console.error('Erro:', error);
		showError(container, 'Erro ao carregar usuários banidos.');
	} finally {
		setRefreshButtonLoading('banned', false);
	}
}

function renderBannedTable(container, bannedUsers) {
	if (bannedUsers.length === 0) {
		container.innerHTML = '<div class="no-users">✅ Nenhum usuário banido</div>';
		return;
	}
	
	const html = `
		<div class="users-table-container">
			<table class="users-table">
				<thead>
					<tr>
						<th>Nome</th>
						<th>Email</th>
						<th>Matrícula</th>
						<th>Banido em</th>
						<th>Banido por</th>
						<th>Motivo</th>
						<th>Ações</th>
					</tr>
				</thead>
				<tbody>
					${bannedUsers.map(user => renderBannedRow(user)).join('')}
				</tbody>
			</table>
		</div>
	`;
	container.innerHTML = html;
	
	// Adicionar event listeners
	bannedUsers.forEach(user => {
		const btn = document.querySelector(`button[data-matricula="${user.matricula}"]`);
		if (btn) {
			btn.onclick = () => unbanUser(user.matricula, btn);
		}
	});
}

function renderBannedRow(user) {
	const banidoEm = user.banidoEm ? new Date(user.banidoEm).toLocaleString('pt-BR') : 'N/A';
	
	return `
		<tr id="banned-row-${escapeHtml(user.matricula)}">
			<td>${escapeHtml(user.nome || 'N/A')}</td>
			<td>${escapeHtml(user.email || 'N/A')}</td>
			<td>${escapeHtml(user.matricula || 'N/A')}</td>
			<td>${banidoEm}</td>
			<td>${escapeHtml(user.banidoPor || 'N/A')}</td>
			<td title="${escapeHtml(user.motivo || '')}" style="max-width:200px;overflow:hidden;text-overflow:ellipsis;">
				${escapeHtml(user.motivo || '-')}
			</td>
			<td class="actions">
				<button class="btn btn-secondary" data-matricula="${escapeHtml(user.matricula)}">Desbanir</button>
			</td>
		</tr>
	`;
}

async function unbanUser(matricula, button) {
	if (!confirm(`Desbanir a matrícula ${matricula}?`)) return;
	
	const originalText = button.textContent;
	const row = document.getElementById(`banned-row-${matricula}`);
	
	button.disabled = true;
	button.innerHTML = '<span class="btn-spinner"></span> Removendo...';
	
	// Adicionar visual de loading na row
	if (row) row.classList.add('table-row-loading');
	
	const code = await httpPost("/api/admin/unban-user", JSON.stringify({ matricula }));
	
	button.disabled = false;
	button.textContent = originalText;
	if (row) row.classList.remove('table-row-loading');
	
	if (code === 200) {
		if (row) {
			row.style.animation = 'fadeOut 0.3s ease-out';
			setTimeout(() => {
				row.remove();
				// Verificar se a tabela ficou vazia
				const tbody = document.querySelector('#banned-content tbody');
				if (tbody && tbody.children.length === 0) {
					document.getElementById('banned-content').innerHTML = '<div class="no-users">✅ Nenhum usuário banido</div>';
				}
			}, 300);
		}
	} else if (code === 404) {
		alert('Matrícula não está banida.');
	} else if (code === 401) {
		document.location.href = '/login?error=notAuthenticated';
	} else {
		alert('Erro ao remover banimento.');
	}
}

// ==================== ALARMING COMMENTS TAB ====================

async function loadAlarmingData(forceRefresh = false) {
	const container = document.getElementById('alarming-content');
	if (!container) return;
	
	if (!forceRefresh && loadedTabs.alarming) return;
	
	showLoading(container);
	setRefreshButtonLoading('alarming', true);
	
	try {
		const response = await fetch(`/api/admin/comments/alarming?page=${alarmingPage}&size=${pageSize}`, {
			method: "POST",
			credentials: 'same-origin'
		});
		
		if (!response.ok) throw new Error('Erro ao buscar comentários');
		
		const data = await response.json();
		
		document.getElementById('alarming-count').textContent = `${data.totalElements} para revisão`;
		updateBadge('alarming-badge', data.totalElements, true);
		
		displayComments(data.content, container, true);
		renderPagination('alarming-pagination', data, 'alarming');
		loadedTabs.alarming = true;
		
	} catch (error) {
		console.error('Erro:', error);
		showError(container, 'Erro ao carregar comentários.');
	} finally {
		setRefreshButtonLoading('alarming', false);
	}
}

// ==================== ALL COMMENTS TAB ====================

async function loadCommentsData(forceRefresh = false) {
	const container = document.getElementById('comments-content');
	if (!container) return;
	
	if (!forceRefresh && loadedTabs.comments) return;
	
	showLoading(container);
	setRefreshButtonLoading('comments', true);
	
	try {
		const response = await fetch(`/api/admin/comments?page=${allCommentsPage}&size=${pageSize}`, {
			method: "POST",
			credentials: 'same-origin'
		});
		
		if (!response.ok) throw new Error('Erro ao buscar comentários');
		
		const data = await response.json();
		
		document.getElementById('all-comments-count').textContent = `${data.totalElements} comentário(s)`;
		updateBadge('comments-badge', data.totalElements);
		
		displayComments(data.content, container, false);
		renderPagination('all-pagination', data, 'comments');
		loadedTabs.comments = true;
		
	} catch (error) {
		console.error('Erro:', error);
		showError(container, 'Erro ao carregar comentários.');
	} finally {
		setRefreshButtonLoading('comments', false);
	}
}

// ==================== COMMENTS RENDER ====================

function displayComments(comments, container, isAlarmingList) {
	if (comments.length === 0) {
		container.innerHTML = `
			<div class="no-comments">
				${isAlarmingList ? '✅ Nenhum comentário para revisão!' : 'Nenhum comentário encontrado.'}
			</div>
		`;
		return;
	}
	
	container.innerHTML = `
		<div class="comments-list">
			${comments.map(comment => renderAdminCommentCard(comment, isAlarmingList)).join('')}
		</div>
	`;
}

function renderAdminCommentCard(comment, isAlarmingList) {
	const createdAt = comment.createdAt ? new Date(comment.createdAt).toLocaleString('pt-BR') : 'N/A';
	const editedInfo = comment.edited && comment.editedAt 
		? `<span class="edited-badge">Editado em ${new Date(comment.editedAt).toLocaleString('pt-BR')}</span>` 
		: '';
	
	// Badges
	const badges = [];
	if (comment.denunciado) {
		badges.push(`<span class="badge badge-danger">🚨 Denunciado (${comment.denunciasCount || 1}x)</span>`);
	}
	if (comment.arquivos && comment.arquivos.length > 0) {
		badges.push(`<span class="badge badge-info">📎 ${comment.arquivos.length} arquivo(s)</span>`);
	}
	if (comment.texto && comment.texto.length > 150) {
		badges.push(`<span class="badge badge-warning">📝 ${comment.texto.length} chars</span>`);
	}
	
	const disciplinaInfo = comment.disciplinaNome 
		? `<a href="/class/${comment.disciplinaId}" class="context-link">${escapeHtml(comment.disciplinaNome)}</a>` 
		: 'N/A';
	
	const textoPreview = comment.texto && comment.texto.length > 300 
		? comment.texto.substring(0, 300) + '...' 
		: comment.texto;
	
	const markSafeButton = isAlarmingList 
		? `<button class="btn btn-success btn-sm" onclick="markCommentAsSafe(${comment.id})">✓ Seguro</button>` 
		: '';
	
	return `
		<div class="admin-comment-card ${comment.alarmante ? 'alarming' : ''}" data-comment-id="${comment.id}">
			<div class="comment-header">
				<div class="user-info">
					<div class="user-avatar">${comment.userInitials || '?'}</div>
					<div class="user-details">
						<span class="user-name">${escapeHtml(comment.userName || 'Usuário')}</span>
						<span class="user-email">${escapeHtml(comment.userEmail || '')}</span>
					</div>
				</div>
				<div class="comment-meta">
					<span class="comment-date">${createdAt}</span>
					${editedInfo}
				</div>
			</div>
			
			${badges.length > 0 ? `<div class="comment-badges">${badges.join('')}</div>` : ''}
			
			<div class="comment-context">
				<span class="context-item"><strong>Disciplina:</strong> ${disciplinaInfo}</span>
				<span class="context-item"><strong>Professor:</strong> ${escapeHtml(comment.professorNome || 'N/A')}</span>
				${comment.respostasCount > 0 ? `<span class="context-item"><strong>Respostas:</strong> ${comment.respostasCount}</span>` : ''}
			</div>
			
			<div class="comment-content">
				<p>${escapeHtml(textoPreview)}</p>
			</div>
			
			${renderAttachments(comment.arquivos)}
			
			<div class="comment-votes">
				<span class="upvotes">👍 ${comment.upVotes || 0}</span>
				<span class="downvotes">👎 ${comment.downVotes || 0}</span>
			</div>
			
			<div class="comment-actions">
				${markSafeButton}
				<button class="btn btn-danger btn-sm" onclick="deleteAdminComment(${comment.id})">🗑️ Deletar</button>
				<button class="btn btn-warning btn-sm" onclick="banUserByComment(${comment.id}, '${escapeHtml(comment.userName)}')">⛔ Banir</button>
			</div>
		</div>
	`;
}

function renderAttachments(arquivos) {
	if (!arquivos || arquivos.length === 0) return '';
	
	return `
		<div class="comment-attachments">
			${arquivos.map(arquivo => {
				const isImage = arquivo.tipoMime && arquivo.tipoMime.startsWith('image/');
				if (isImage) {
					return `
						<div class="attachment-preview">
							<img src="/api/arquivos/${arquivo.id}" alt="${escapeHtml(arquivo.nomeOriginal)}" 
								onclick="window.open('/api/arquivos/${arquivo.id}', '_blank')">
							<span class="attachment-name">${escapeHtml(arquivo.nomeOriginal)}</span>
						</div>
					`;
				}
				return `
					<div class="attachment-file">
						<span class="file-icon">📄</span>
						<a href="/api/arquivos/${arquivo.id}?download=true">${escapeHtml(arquivo.nomeOriginal)}</a>
					</div>
				`;
			}).join('')}
		</div>
	`;
}

function renderPagination(containerId, data, type) {
	const container = document.getElementById(containerId);
	if (!container || data.totalPages <= 1) {
		if (container) container.innerHTML = '';
		return;
	}
	
	container.innerHTML = `
		<div class="pagination">
			${data.currentPage > 0 ? `<button class="btn btn-secondary btn-sm" onclick="goToPage('${type}', ${data.currentPage - 1})">← Anterior</button>` : ''}
			<span class="page-info">Página ${data.currentPage + 1} de ${data.totalPages}</span>
			${data.currentPage < data.totalPages - 1 ? `<button class="btn btn-secondary btn-sm" onclick="goToPage('${type}', ${data.currentPage + 1})">Próxima →</button>` : ''}
		</div>
	`;
}

function goToPage(type, page) {
	// Desabilitar botões de paginação durante o carregamento
	const paginationContainer = document.getElementById(type === 'alarming' ? 'alarming-pagination' : 'all-pagination');
	const buttons = paginationContainer?.querySelectorAll('button');
	buttons?.forEach(btn => {
		btn.disabled = true;
		btn.innerHTML = '<span class="btn-spinner"></span>';
	});
	
	if (type === 'alarming') {
		alarmingPage = page;
		loadedTabs.alarming = false;
		loadAlarmingData();
	} else {
		allCommentsPage = page;
		loadedTabs.comments = false;
		loadCommentsData();
	}
}

// ==================== COMMENT ACTIONS ====================

async function markCommentAsSafe(commentId) {
	if (!confirm('Marcar este comentário como seguro?')) return;
	
	// Encontrar o botão e adicionar estado de loading
	const card = document.querySelector(`.admin-comment-card[data-comment-id="${commentId}"]`);
	const button = card?.querySelector('.btn-success');
	
	if (button) {
		button.disabled = true;
		button.innerHTML = '<span class="btn-spinner"></span> Processando...';
	}
	
	try {
		const response = await fetch(`/api/admin/comments/${commentId}/mark-safe`, {
			method: 'POST',
			credentials: 'same-origin'
		});
		
		if (response.ok) {
			if (card) {
				card.style.animation = 'fadeOut 0.3s ease-out';
				setTimeout(() => {
					card.remove();
					loadedTabs.alarming = false;
					loadAlarmingData();
				}, 300);
			}
		} else {
			if (button) {
				button.disabled = false;
				button.innerHTML = '✓ Seguro';
			}
			alert('Erro ao marcar comentário como seguro.');
		}
	} catch (error) {
		console.error('Erro:', error);
		if (button) {
			button.disabled = false;
			button.innerHTML = '✓ Seguro';
		}
		alert('Erro ao marcar comentário como seguro.');
	}
}

async function deleteAdminComment(commentId) {
	if (!confirm('Deletar este comentário permanentemente?')) return;
	
	// Encontrar o botão e adicionar estado de loading
	const card = document.querySelector(`.admin-comment-card[data-comment-id="${commentId}"]`);
	const button = card?.querySelector('.btn-danger');
	
	if (button) {
		button.disabled = true;
		button.innerHTML = '<span class="btn-spinner"></span> Deletando...';
	}
	
	// Adicionar overlay de loading no card
	if (card) {
		card.classList.add('card-loading');
	}
	
	try {
		const response = await fetch(`/api/admin/comments/${commentId}`, {
			method: 'DELETE',
			credentials: 'same-origin'
		});
		
		if (response.ok) {
			if (card) {
				card.classList.remove('card-loading');
				card.style.animation = 'fadeOut 0.3s ease-out';
				setTimeout(() => {
					card.remove();
					// Forçar reload de ambas as listas
					loadedTabs.alarming = false;
					loadedTabs.comments = false;
				}, 300);
			}
		} else {
			if (card) card.classList.remove('card-loading');
			if (button) {
				button.disabled = false;
				button.innerHTML = '🗑️ Deletar';
			}
			alert('Erro ao deletar comentário.');
		}
	} catch (error) {
		console.error('Erro:', error);
		if (card) card.classList.remove('card-loading');
		if (button) {
			button.disabled = false;
			button.innerHTML = '🗑️ Deletar';
		}
		alert('Erro ao deletar comentário.');
	}
}

async function banUserByComment(commentId, userName) {
	const motivo = prompt(`Banir o usuário ${userName}?\n\nMotivo (opcional):`);
	if (motivo === null) return;
	
	// Encontrar o botão e adicionar estado de loading
	const card = document.querySelector(`.admin-comment-card[data-comment-id="${commentId}"]`);
	const button = card?.querySelector('.btn-warning');
	
	if (button) {
		button.disabled = true;
		button.innerHTML = '<span class="btn-spinner"></span> Banindo...';
	}
	
	// Adicionar overlay de loading no card
	if (card) {
		card.classList.add('card-loading');
	}
	
	try {
		const response = await fetch(`/api/admin/comments/${commentId}/ban-user`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ motivo: motivo || null }),
			credentials: 'same-origin'
		});
		
		if (response.ok) {
			if (card) card.classList.remove('card-loading');
			alert('Usuário banido com sucesso.');
			// Forçar reload
			loadedTabs.alarming = false;
			loadedTabs.comments = false;
			loadedTabs.banned = false;
			
			// Recarregar aba atual
			const activeTab = document.querySelector('.tab-btn.active')?.dataset.tab;
			if (activeTab) loadTabData(activeTab);
		} else {
			if (card) card.classList.remove('card-loading');
			if (button) {
				button.disabled = false;
				button.innerHTML = '⛔ Banir';
			}
			const error = await response.text();
			alert('Erro ao banir usuário: ' + error);
		}
	} catch (error) {
		console.error('Erro:', error);
		if (card) card.classList.remove('card-loading');
		if (button) {
			button.disabled = false;
			button.innerHTML = '⛔ Banir';
		}
		alert('Erro ao banir usuário.');
	}
}

// ==================== SCRAPPER TAB ====================

async function loadScrapperData(forceRefresh = false) {
	const statusDiv = document.getElementById('scrapper-status');
	if (!statusDiv) return;
	
	if (!forceRefresh && loadedTabs.scrapper) return;
	
	showLoading(statusDiv);
	
	// Loading no botão de atualizar status
	const refreshBtn = document.getElementById('refresh-status-btn');
	if (refreshBtn) {
		refreshBtn.disabled = true;
		refreshBtn.dataset.originalText = refreshBtn.innerHTML;
		refreshBtn.innerHTML = '<span class="btn-spinner"></span> Atualizando...';
	}
	
	try {
		const response = await fetch('/api/admin/scrapper/status', { method: "POST", credentials: 'same-origin' });
		
		if (!response.ok) {
			if (response.status === 401) {
				document.location.href = '/login?error=notAuthenticated';
				return;
			}
			throw new Error('Erro ao obter status');
		}
		
		const status = await response.json();
		displayScrapperStatus(status);
		loadedTabs.scrapper = true;
		
	} catch (error) {
		console.error('Erro:', error);
		showError(statusDiv, `Erro ao carregar status: ${error.message}`);
	} finally {
		if (refreshBtn) {
			refreshBtn.disabled = false;
			if (refreshBtn.dataset.originalText) {
				refreshBtn.innerHTML = refreshBtn.dataset.originalText;
			}
		}
	}
}

function displayScrapperStatus(status) {
	const statusDiv = document.getElementById('scrapper-status');
	
	const formatDate = (dateString) => {
		if (!dateString) return 'Nunca';
		return new Date(dateString).toLocaleString('pt-BR');
	};
	
	const executando = status.executando ? 'Sim' : 'Não';
	const statusClass = status.executando ? 'status-running' : 'status-idle';
	const statusIcon = status.executando ? '🔄' : '✅';
	
	statusDiv.innerHTML = `
		<div class="status-card ${statusClass}">
			<div class="status-header">
				<span class="status-icon">${statusIcon}</span>
				<h3>Status do Scrapper</h3>
			</div>
			
			<div class="status-details">
				<div class="status-item">
					<label>Em execução:</label>
					<span class="status-value ${status.executando ? 'running' : 'idle'}">${executando}</span>
				</div>
				<div class="status-item">
					<label>Última execução:</label>
					<span class="status-value">${formatDate(status.ultimaExecucao)}</span>
				</div>
				<div class="status-item">
					<label>Último sucesso:</label>
					<span class="status-value">${formatDate(status.ultimoSucesso)}</span>
				</div>
				<div class="status-item">
					<label>Disciplinas capturadas:</label>
					<span class="status-value">${status.disciplinasCapturadas || 0}</span>
				</div>
				<div class="status-item">
					<label>Professores capturados:</label>
					<span class="status-value">${status.professoresCapturados || 0}</span>
				</div>
				<div class="status-item">
					<label>Último administrador:</label>
					<span class="status-value">${status.ultimoAdministrador || 'N/A'}</span>
				</div>
				${status.ultimoErro ? `
				<div class="status-item error">
					<label>Último erro:</label>
					<span class="status-value error">${status.ultimoErro}</span>
				</div>
				` : ''}
			</div>
		</div>
	`;
}

// ==================== SCRAPPER MODAL ====================

function showCredentialsModal() {
	const modal = document.getElementById('credentials-modal');
	modal.style.display = 'block';
	setTimeout(() => document.getElementById('cagr-username').focus(), 100);
}

function hideCredentialsModal() {
	const modal = document.getElementById('credentials-modal');
	modal.style.display = 'none';
	document.getElementById('credentials-form').reset();
}

async function executeScrapper() {
	const form = document.getElementById('credentials-form');
	const formData = new FormData(form);
	const submitButton = form.querySelector('button[type="submit"]');
	const originalText = submitButton.textContent;
	
	try {
		submitButton.disabled = true;
		submitButton.innerHTML = '<span class="btn-spinner"></span> Executando...';
		
		const credentials = {
			cagrUsername: formData.get('cagrUsername'),
			cagrPassword: formData.get('cagrPassword')
		};
		
		const response = await fetch('/api/admin/scrapper/execute', {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify(credentials),
			credentials: 'same-origin'
		});
		
		if (response.status === 200) {
			alert('Sucesso: ' + await response.text());
			hideCredentialsModal();
			setTimeout(() => loadScrapperData(true), 1000);
		} else if (response.status === 400 || response.status === 409) {
			alert('Erro: ' + await response.text());
		} else if (response.status === 403) {
			alert('Acesso negado.');
		} else if (response.status === 401) {
			document.location.href = '/login?error=notAuthenticated';
		} else {
			alert('Erro: ' + await response.text());
		}
	} catch (error) {
		console.error('Erro:', error);
		alert('Erro ao executar scrapper.');
	} finally {
		submitButton.disabled = false;
		submitButton.textContent = originalText;
	}
}

// ==================== BADGE UPDATE ====================

function updateBadge(badgeId, count, isWarning = false) {
	const badge = document.getElementById(badgeId);
	if (!badge) return;
	
	if (count > 0) {
		badge.textContent = count > 99 ? '99+' : count;
		badge.style.display = 'inline-flex';
		if (isWarning) badge.classList.add('warning');
	} else {
		badge.style.display = 'none';
	}
}

// ==================== INICIALIZAÇÃO ====================

window.addEventListener('DOMContentLoaded', () => {
	// Configurar form do scrapper
	document.getElementById('credentials-form')?.addEventListener('submit', (e) => {
		e.preventDefault();
		executeScrapper();
	});
	
	// Fechar modal ao clicar fora
	window.addEventListener('click', (e) => {
		const modal = document.getElementById('credentials-modal');
		if (e.target === modal) hideCredentialsModal();
	});
	
	// NÃO carregar dados automaticamente - lazy loading!
	// O usuário precisa clicar na aba ou no botão de carregar
	
	// Opcional: carregar apenas a contagem de alarmantes para o badge
	fetchAlarmingCount();
});

// Carregar apenas a contagem de alarmantes para mostrar no badge
async function fetchAlarmingCount() {
	try {
		const response = await fetch('/api/admin/comments/stats', { method: "POST", credentials: 'same-origin' });
		if (response.ok) {
			const stats = await response.json();
			updateBadge('alarming-badge', stats.comentariosAlarmantes || 0, true);
		}
	} catch (error) {
		console.log('Não foi possível carregar contagem de alarmantes');
	}
}
