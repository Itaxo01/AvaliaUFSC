// ============================================
// COMMENT RENDER JS
// Responsável por toda a renderização de comentários
// ============================================

console.log('✅ commentRender.js carregado');

// Mostrar comentários
function mostrarComentarios(comentarios, professorNome) {
    const lista = document.querySelector('.reviews-list');
    
    if (comentarios.length === 0) {
        const entidade = professorNome || 'esta disciplina';
        lista.innerHTML = `<div class="no-reviews"><p>Ainda não há avaliações para ${entidade}.</p><p>Seja o primeiro a avaliar!</p></div>`;
        return;
    }
    
    lista.innerHTML = comentarios.filter(c => !c.comentarioPaiId && !c.deleted).map(comentario => {
        return renderCommentCard(comentario, false, 0);
    }).join('');
}

/**
 * Render a single comment card (parent or child) with recursive support for nested replies
 * @param {Object} comentario - The comment object
 * @param {Boolean} isChild - Whether this is a child comment
 * @param {Number} nestLevel - Depth level for nested styling
 */
function renderCommentCard(comentario, isChild = false, nestLevel = 0) {
    const isOwner = comentario.isOwner || false;
    const hasVoted = comentario.hasVoted; // 1 (upvote), -1 (downvote), 0 (no vote)
    
    // Classes para destacar botões de voto
    const upvoteClass = hasVoted === 1 ? 'voted' : '';
    const downvoteClass = hasVoted === -1 ? 'voted' : '';
    
    // ✅ Show delete button if user is owner OR admin
    const canDelete = isOwner || isAdmin;
    
    // ✅ Render attached files
    const arquivosHTML = comentario.arquivos && comentario.arquivos.length > 0 
        ? `<div class="comment-attachments">${renderArquivos(comentario.arquivos)}</div>`
        : '';
    
    // ✅ Check if comment was edited
    const isEdited = comentario.edited || false;
    const editedText = isEdited && comentario.editedAt 
        ? `<span class="edited-indicator"> (Editado em ${formatarData(comentario.editedAt)})</span>`
        : '';
    
    // Add child class for styling based on nesting level
    const childClass = isChild ? `child-comment nest-level-${nestLevel}` : 'parent-comment';
    
    // Render this comment card
    let html = `
    <div class="review-card ${childClass}" data-comment-id="${comentario.id}">
         <div class="review-header">
        <div class="reviewer-info">
             ${isChild ? '<span class="reply-indicator">↳</span>' : ''}
             <div class="reviewer-avatar">${comentario.userInitials || '?'}</div>
             <div class="reviewer-details">
                 <span class="reviewer-name">Anônimo</span>
                 <span class="reviewer-separator">·</span>
                 <span class="reviewer-date">${formatarData(comentario.createdAt)}${editedText}</span>
             </div>
        </div>
         </div>
         <div class="review-content">${escapeHtml(comentario.texto)}</div>
         
         <!-- Arquivos Anexados -->
         ${arquivosHTML}
         
         <!-- Botões de Ação - Reddit Style -->
         <div class="review-actions">
            <div class="vote-container">
                <button class="vote-btn upvote-btn ${upvoteClass}" onclick="voteComment(${comentario.id}, true)" title="Upvote">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 19V5M5 12l7-7 7 7"/>
                    </svg>
                </button>
                <span class="vote-score" data-comment-id="${comentario.id}">${(comentario.upVotes || 0) - (comentario.downVotes || 0)}</span>
                <button class="vote-btn downvote-btn ${downvoteClass}" onclick="voteComment(${comentario.id}, false)" title="Downvote">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 5v14M5 12l7 7 7-7"/>
                    </svg>
                </button>
            </div>
            <div class="action-separator"></div>
            <button class="review-action-btn reply-btn" onclick="replyToComment(${comentario.id})">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
                <span>responder</span>
            </button>
            ${isOwner ? `
            <button class="review-action-btn edit-btn" onclick="editComment(${comentario.id})">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                </svg>
                <span>editar</span>
            </button>
            ` : ''}
            ${!isOwner ? `
            <button class="review-action-btn report-btn" onclick="reportComment(${comentario.id})" title="Denunciar comentário">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/>
                    <line x1="4" y1="22" x2="4" y2="15"/>
                </svg>
                <span>denunciar</span>
            </button>
            ` : ''}
            ${canDelete ? `
            <button class="review-action-btn delete-btn" onclick="deleteComment(${comentario.id})">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="3 6 5 6 21 6"/>
                    <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                </svg>
                <span>deletar</span>
            </button>
            ` : ''}
         </div>
    </div>
    `;
    
    // Recursively render nested replies (filhos can have filhos)
    if (comentario.filhos && comentario.filhos.length > 0) {
        html += `<div class="child-comments-container nest-level-${nestLevel + 1}">`;
        comentario.filhos.filter(filho => !filho.deleted).forEach(filho => {
            html += renderCommentCard(filho, true, nestLevel + 1);
        });
        html += `</div>`;
    }
    
    return html;
}

// Formatar data
function formatarData(data) {
    if (!data) return 'Data desconhecida';
    const d = new Date(data);
    const meses = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
    return `${meses[d.getMonth()]} ${d.getFullYear()}`;
}

/**
 * Render attached files (images and documents)
 */
function renderArquivos(arquivos) {
    if (!arquivos || arquivos.length === 0) return '';
    
    return arquivos.map(arquivo => {
        const isImage = arquivo.tipoMime && arquivo.tipoMime.startsWith('image/');
        const isPdf = arquivo.tipoMime === 'application/pdf';
        
        if (isImage) {
            // Image preview with click to enlarge
            return `
                <div class="attachment-item attachment-image">
                    <img src="/api/arquivos/${arquivo.id}" 
                         alt="${escapeHtml(arquivo.nomeOriginal)}"
                         onclick="openImageModal('/api/arquivos/${arquivo.id}', '${escapeHtml(arquivo.nomeOriginal)}')"
                         loading="lazy">
                    <div class="attachment-name">${escapeHtml(arquivo.nomeOriginal)}</div>
                </div>
            `;
        } else if (isPdf) {
            // PDF preview with click to view/download
            return `
                <div class="attachment-item attachment-pdf">
                    <div class="pdf-icon">📄</div>
                    <div class="attachment-info">
                        <div class="attachment-name">${escapeHtml(arquivo.nomeOriginal)}</div>
                        <div class="attachment-size">${formatFileSize(arquivo.tamanho)}</div>
                    </div>
                    <div class="attachment-actions">
                        <a href="/api/arquivos/${arquivo.id}" target="_blank" class="btn-view">Ver</a>
                        <a href="/api/arquivos/${arquivo.id}?download=true" class="btn-download">Baixar</a>
                    </div>
                </div>
            `;
        } else {
            // Other documents (Word, Excel, etc.) - download only
            const icon = getFileIcon(arquivo.nomeOriginal);
            return `
                <div class="attachment-item attachment-doc">
                    <div class="doc-icon">${icon}</div>
                    <div class="attachment-info">
                        <div class="attachment-name">${escapeHtml(arquivo.nomeOriginal)}</div>
                        <div class="attachment-size">${formatFileSize(arquivo.tamanho)}</div>
                    </div>
                    <a href="/api/arquivos/${arquivo.id}?download=true" class="btn-download">Baixar</a>
                </div>
            `;
        }
    }).join('');
}

/**
 * Open image in modal for full view
 */
function openImageModal(imageUrl, imageName) {
    // Create modal if it doesn't exist
    let modal = document.getElementById('imageModal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'imageModal';
        modal.className = 'image-modal';
        modal.innerHTML = `
            <div class="image-modal-content">
                <span class="image-modal-close" onclick="closeImageModal()">&times;</span>
                <img id="modalImage" src="" alt="">
                <div id="modalImageName" class="modal-image-name"></div>
            </div>
        `;
        document.body.appendChild(modal);
        
        // Close on click outside
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                closeImageModal();
            }
        });
    }
    
    // Set image and show modal
    document.getElementById('modalImage').src = imageUrl;
    document.getElementById('modalImageName').textContent = imageName;
    modal.style.display = 'flex';
}

/**
 * Close image modal
 */
function closeImageModal() {
    const modal = document.getElementById('imageModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

function getFileIcon(filename) {
    const ext = filename.split('.').pop().toLowerCase();
    const icons = {
        'pdf': '📄',
        'doc': '📝',
        'docx': '📝',
        'jpg': '🖼️',
        'jpeg': '🖼️',
        'png': '🖼️',
        'gif': '🖼️'
    };
    return icons[ext] || '📎';
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

/**
 * Escapar HTML para prevenir XSS em strings interpoladas
 */
function escapeHtml(text) {
    if (!text) return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;',
        '`': '&#x60;',
        '\\': '&#x5C;'
    };
    return String(text).replace(/[&<>"'`\\]/g, char => map[char]);
}
