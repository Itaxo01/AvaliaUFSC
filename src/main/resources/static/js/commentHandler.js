// ============================================
// COMMENT HANDLER JS
// Responsável por lidar com interações de comentários:
// Like, dislike, edit, reply, criação e deleção
// ============================================

console.log('✅ commentHandler.js carregado');

// ============================================
// FILE UPLOAD FUNCTIONS
// ============================================

function handleFileSelect(event) {
    const files = Array.from(event.target.files);
    const fileList = document.getElementById('fileList');
    
    // Allowed file types for security
    const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.txt'];
    const allowedMimeTypes = [
        'image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp',
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'text/plain'
    ];
    
    // Clear error messages
    fileList.querySelectorAll('.file-error').forEach(el => el.remove());
    
    files.forEach(file => {
        // Check file size
        if (file.size > MAX_FILE_SIZE) {
            const error = document.createElement('div');
            error.className = 'file-error';
            error.textContent = `❌ Arquivo "${file.name}" excede o tamanho máximo de 5MB`;
            fileList.appendChild(error);
            return;
        }
        
        // Check file extension
        const extension = '.' + file.name.split('.').pop().toLowerCase();
        if (!allowedExtensions.includes(extension)) {
            const error = document.createElement('div');
            error.className = 'file-error';
            error.textContent = `❌ Tipo de arquivo não permitido: "${file.name}". Apenas imagens (JPG, PNG, GIF, WebP) e documentos (PDF, DOC, DOCX, XLS, XLSX, TXT) são permitidos.`;
            fileList.appendChild(error);
            return;
        }
        
        // Check MIME type
        if (!allowedMimeTypes.includes(file.type)) {
            const error = document.createElement('div');
            error.className = 'file-error';
            error.textContent = `❌ Tipo de arquivo não permitido: "${file.name}". Apenas imagens e documentos seguros são permitidos.`;
            fileList.appendChild(error);
            return;
        }
        
        // Check if file already exists
        const exists = selectedFiles.some(f => f.name === file.name && f.size === file.size);
        if (!exists) {
            selectedFiles.push(file);
        }
    });
    
    // Clear input to allow selecting the same file again
    event.target.value = '';
    
    renderFileList();
}

function removeFile(index) {
    selectedFiles.splice(index, 1);
    renderFileList();
}

function renderFileList() {
    const fileList = document.getElementById('fileList');
    const errors = fileList.querySelectorAll('.file-error');
    
    if (selectedFiles.length === 0 && errors.length === 0) {
        fileList.innerHTML = '';
        return;
    }
    
    const filesHTML = selectedFiles.map((file, index) => `
        <div class="file-item">
            <div class="file-item-info">
                <span class="file-item-icon">${getFileIcon(file.name)}</span>
                <span class="file-item-name">${file.name}</span>
                <span class="file-item-size">${formatFileSize(file.size)}</span>
            </div>
            <button type="button" class="file-item-remove" onclick="removeFile(${index})" title="Remover arquivo">×</button>
        </div>
    `).join('');
    
    // Preserve error messages
    const errorsHTML = Array.from(errors).map(el => el.outerHTML).join('');
    
    fileList.innerHTML = filesHTML + errorsHTML;
}

// ============================================
// SUBMIT COMMENT
// ============================================

async function submitComment(event) {
    event.preventDefault();
    
    const texto = document.getElementById('commentText').value.trim();
    const submitButton = event.target.querySelector('button[type="submit"]');
    const commentForm = document.getElementById('commentForm');
    
    if (!texto) {
        showToast('Por favor, escreva um comentário.', 'warning');
        return;
    }
    
    // Disable form and show loading
    if (submitButton) {
        submitButton.disabled = true;
        submitButton.classList.add('btn-loading');
        const originalText = submitButton.textContent;
        submitButton.dataset.originalText = originalText;
        submitButton.textContent = 'Enviando...';
    }
    
    if (commentForm) {
        commentForm.classList.add('form-disabled');
    }
    
    // Check if we're replying or creating a new comment
    if (replyingToCommentId !== null) {
        // REPLY MODE
        console.log('Submitting reply to comment:', {
            parentId: replyingToCommentId,
            texto,
            files: selectedFiles.map(f => ({ name: f.name, size: f.size, type: f.type }))
        });
		  
        // Prepare FormData for reply
        const formData = new FormData();
        formData.append('texto', texto);
        formData.append('comentarioPaiId', replyingToCommentId);
        
        selectedFiles.forEach((file, index) => {
            formData.append(`files`, file);
        });
        
		  console.log('FormData for reply:', formData);
        try {
            const response = await fetch('/api/comentario/responder', {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) {
                const errorText = await response.text();
                showToast(parseErrorMessage(errorText) || 'Erro ao enviar resposta', 'error');
                return;
            }
            
            const result = await response.json();
            console.log('Resposta enviada com sucesso:', result);
            
            // Reload page to show new reply
            window.location.reload();
            
        } catch (error) {
            console.error('Erro ao enviar resposta:', error);
            showToast(parseErrorMessage(error.message) || 'Erro ao enviar resposta', 'error');
            
            // Re-enable form on error
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.classList.remove('btn-loading');
                submitButton.textContent = submitButton.dataset.originalText || 'Enviar';
            }
            if (commentForm) {
                commentForm.classList.remove('form-disabled');
            }
            return;
        }
    } else {
        // CREATE MODE (new comment)
        console.log('Submitting comment:', {
            texto,
            professorId: professorSelecionado,
            disciplinaId: typeof CLASS_ID !== 'undefined' ? CLASS_ID : null,
            files: selectedFiles.map(f => ({ name: f.name, size: f.size, type: f.type }))
        });
        
        // Prepare FormData for file upload
        const formData = new FormData();
        formData.append('texto', texto);
        formData.append('disciplinaId', typeof CLASS_ID !== 'undefined' ? CLASS_ID : '');
        if (professorSelecionado) {
            formData.append('professorId', professorSelecionado);
        } else {
            formData.append('professorId', "");
        }
        selectedFiles.forEach((file, index) => {
            formData.append(`files`, file);
        });
        
        try {
            const response = await fetch('/api/comentario/comentar', {
                method: 'POST',
                body: formData
            });
            if (!response.ok) {
                const errorText = await response.text();
                showToast(parseErrorMessage(errorText) || 'Erro ao enviar comentário', 'error');
                return;
            }
            const result = await response.json();
            console.log('Comentário enviado com sucesso:', result);
            
            // Reload page to show new comment
            window.location.reload();
            
        } catch (error) {
            console.error('Erro ao enviar comentário:', error);
            showToast(parseErrorMessage(error.message) || 'Erro ao enviar comentário', 'error');
            
            // Re-enable form on error
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.classList.remove('btn-loading');
                submitButton.textContent = submitButton.dataset.originalText || 'Enviar';
            }
            if (commentForm) {
                commentForm.classList.remove('form-disabled');
            }
            return;
        }
    }
}

// ============================================
// VOTE COMMENT
// ============================================

/**
 * Votar em um comentário (upvote ou downvote)
 */
async function voteComment(comentarioId, isUpVote) {
    // Find the comment card and buttons
    const commentCard = document.querySelector(`[data-comment-id="${comentarioId}"]`);
    if (!commentCard) return;
    
    const upvoteBtn = commentCard.querySelector('.upvote-btn');
    const downvoteBtn = commentCard.querySelector('.downvote-btn');
    const comentario = allComments.find(c => c.id === comentarioId);
    
    if (!comentario) return;
    
    // Store original state for rollback
    const originalUpVotes = comentario.upVotes || 0;
    const originalDownVotes = comentario.downVotes || 0;
    const originalHasVoted = comentario.hasVoted || 0;
    
    // OPTIMISTIC UI UPDATE - Immediate visual feedback
    const currentVote = comentario.hasVoted;
    
    // Calculate new state optimistically
    if (isUpVote) {
        if (currentVote === 1) {
            // Remove upvote
            comentario.upVotes = originalUpVotes - 1;
            comentario.hasVoted = 0;
            upvoteBtn.classList.remove('voted');
        } else if (currentVote === -1) {
            // Switch from downvote to upvote
            comentario.upVotes = originalUpVotes + 1;
            comentario.downVotes = originalDownVotes - 1;
            comentario.hasVoted = 1;
            upvoteBtn.classList.add('voted');
            downvoteBtn.classList.remove('voted');
        } else {
            // Add upvote
            comentario.upVotes = originalUpVotes + 1;
            comentario.hasVoted = 1;
            upvoteBtn.classList.add('voted');
        }
    } else {
        if (currentVote === -1) {
            // Remove downvote
            comentario.downVotes = originalDownVotes - 1;
            comentario.hasVoted = 0;
            downvoteBtn.classList.remove('voted');
        } else if (currentVote === 1) {
            // Switch from upvote to downvote
            comentario.downVotes = originalDownVotes + 1;
            comentario.upVotes = originalUpVotes - 1;
            comentario.hasVoted = -1;
            downvoteBtn.classList.add('voted');
            upvoteBtn.classList.remove('voted');
        } else {
            // Add downvote
            comentario.downVotes = originalDownVotes + 1;
            comentario.hasVoted = -1;
            downvoteBtn.classList.add('voted');
        }
    }
    
    // Update vote score in UI (Reddit style: upvotes - downvotes)
    const voteScore = commentCard.querySelector('.vote-score');
    if (voteScore) {
        const score = (comentario.upVotes || 0) - (comentario.downVotes || 0);
        voteScore.textContent = score;
        voteScore.classList.remove('positive', 'negative');
        if (score > 0) voteScore.classList.add('positive');
        else if (score < 0) voteScore.classList.add('negative');
    }
    
    // Add pending state
    commentCard.classList.add('optimistic-pending');
    upvoteBtn.disabled = true;
    downvoteBtn.disabled = true;
    
    try {
        const formData = new FormData();
        formData.append('isUpVote', isUpVote);
        
        console.log(`Votando no comentário ${comentarioId} - Upvote: ${isUpVote}`);

        const response = await fetch(`/api/comentario/votar/${comentarioId}`, {
            method: 'POST',
            body: formData
        });
        
        if (!response.ok) {
            const errorText = await response.text();
            // ROLLBACK on error
            comentario.upVotes = originalUpVotes;
            comentario.downVotes = originalDownVotes;
            comentario.hasVoted = originalHasVoted;
            
            // Update vote score display
            const voteScoreRollback = commentCard.querySelector('.vote-score');
            if (voteScoreRollback) {
                const scoreRollback = originalUpVotes - originalDownVotes;
                voteScoreRollback.textContent = scoreRollback;
                voteScoreRollback.classList.remove('positive', 'negative');
                if (scoreRollback > 0) voteScoreRollback.classList.add('positive');
                else if (scoreRollback < 0) voteScoreRollback.classList.add('negative');
            }
            
            if (originalHasVoted === 1) upvoteBtn.classList.add('voted');
            else upvoteBtn.classList.remove('voted');
            if (originalHasVoted === -1) downvoteBtn.classList.add('voted');
            else downvoteBtn.classList.remove('voted');
            
            commentCard.classList.add('optimistic-error');
            setTimeout(() => commentCard.classList.remove('optimistic-error'), 500);
            showToast(parseErrorMessage(errorText) || 'Erro ao votar', 'error');
            return;
        }
        
        const result = await response.json();
        console.log('Voto registrado:', result);
        
        // Update with actual server values
        comentario.upVotes = result.upVotes || 0;
        comentario.downVotes = result.downVotes || 0;
        if (result.userVote !== undefined) {
            comentario.hasVoted = result.userVote;
        }
        
        // Update vote score display with server values
        const voteScoreServer = commentCard.querySelector('.vote-score');
        if (voteScoreServer) {
            const scoreServer = comentario.upVotes - comentario.downVotes;
            voteScoreServer.textContent = scoreServer;
            voteScoreServer.classList.remove('positive', 'negative');
            if (scoreServer > 0) voteScoreServer.classList.add('positive');
            else if (scoreServer < 0) voteScoreServer.classList.add('negative');
        }
        
        // Success animation
        commentCard.classList.remove('optimistic-pending');
        commentCard.classList.add('optimistic-success');
        setTimeout(() => commentCard.classList.remove('optimistic-success'), 500);
        
    } catch (error) {
        console.error('Erro ao votar:', error);
        // ROLLBACK on error
        comentario.upVotes = originalUpVotes;
        comentario.downVotes = originalDownVotes;
        comentario.hasVoted = originalHasVoted;
        
        // Update vote score display
        const voteScoreCatch = commentCard.querySelector('.vote-score');
        if (voteScoreCatch) {
            const scoreCatch = originalUpVotes - originalDownVotes;
            voteScoreCatch.textContent = scoreCatch;
            voteScoreCatch.classList.remove('positive', 'negative');
            if (scoreCatch > 0) voteScoreCatch.classList.add('positive');
            else if (scoreCatch < 0) voteScoreCatch.classList.add('negative');
        }
        
        if (originalHasVoted === 1) upvoteBtn.classList.add('voted');
        else upvoteBtn.classList.remove('voted');
        if (originalHasVoted === -1) downvoteBtn.classList.add('voted');
        else downvoteBtn.classList.remove('voted');
        
        commentCard.classList.add('optimistic-error');
        setTimeout(() => commentCard.classList.remove('optimistic-error'), 500);
        showToast(parseErrorMessage(error.message) || 'Erro ao votar', 'error');
    } finally {
        commentCard.classList.remove('optimistic-pending');
        upvoteBtn.disabled = false;
        downvoteBtn.disabled = false;
    }
}

// ============================================
// REPLY TO COMMENT
// ============================================

/**
 * Responder a um comentário
 */
function replyToComment(comentarioId) {
    console.log('Reply to comment:', comentarioId);
    
    // Store which comment we're replying to
    replyingToCommentId = comentarioId;
    
    // Find the comment card
    const commentCard = document.querySelector(`[data-comment-id="${comentarioId}"]`);
    if (!commentCard) {
        showToast('Comentário não encontrado.', 'error');
        return;
    }
    
    // Get the comment editor
    const editor = document.getElementById('commentEditor');
    const button = document.querySelector('.btn-add-review');
    
    // Reset editor first (but keep replyingToCommentId)
    const savedReplyId = replyingToCommentId;
    resetCommentEditor();
    replyingToCommentId = savedReplyId;
    
    // Update subtitle to show we're replying
    const subtitle = document.getElementById('editorSubtitle');
    if (subtitle) {
        subtitle.textContent = '💬 Respondendo ao comentário';
    }
    
    // Show the editor if hidden
    if (!editor.classList.contains('show')) {
        editor.classList.add('show');
        if (button) button.classList.add('active');
    }
    
    // Remove any previous inline positioning from absolute mode
    editor.style.position = '';
    editor.style.top = '';
    editor.style.left = '';
    editor.style.right = '';
    editor.style.maxWidth = '';
    
    // Add reply mode class and indent styling
    editor.classList.add('reply-mode');
    editor.style.marginLeft = '10px'; // Small indent to the right
    
    // Insert the editor right after the comment card in the DOM
    // This makes it part of the document flow
    if (commentCard.nextSibling) {
        commentCard.parentNode.insertBefore(editor, commentCard.nextSibling);
    } else {
        commentCard.parentNode.appendChild(editor);
    }
    
    // Scroll to the editor smoothly
    setTimeout(() => {
        editor.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        document.getElementById('commentText').focus();
    }, 100);
}

// ============================================
// EDIT COMMENT (INLINE)
// ============================================

/**
 * Editar um comentário existente - Edição Inline
 */
function editComment(comentarioId) {
    // Find the comment data
    const comentario = allComments.find(c => c.id === comentarioId);
    
    if (!comentario) {
        showToast('Comentário não encontrado.', 'error');
        return;
    }
    
    // Find the comment card element
    const commentCard = document.querySelector(`[data-comment-id="${comentarioId}"]`);
    if (!commentCard) {
        showToast('Elemento do comentário não encontrado.', 'error');
        return;
    }
    
    // Check if already editing another comment
    const existingEditContainer = document.querySelector('.inline-edit-container');
    if (existingEditContainer) {
        cancelInlineEdit();
    }
    
    // Store editing state
    editingCommentId = comentarioId;
    inlineEditFiles = [];
    inlineEditExistingFiles = comentario.arquivos ? [...comentario.arquivos] : [];
    
    // Add editing class to card
    commentCard.classList.add('editing');
    
    // Hide attachments during editing
    const attachments = commentCard.querySelector('.comment-attachments');
    if (attachments) {
        attachments.style.display = 'none';
    }
    
    // Get the review-content element
    const reviewContent = commentCard.querySelector('.review-content');
    
    // Create inline edit container with file upload
    const editContainer = document.createElement('div');
    editContainer.className = 'inline-edit-container';
    editContainer.innerHTML = `
        <textarea class="inline-edit-textarea" id="inlineEditText">${escapeHtml(comentario.texto)}</textarea>
        
        <!-- File Upload Section -->
        <div class="inline-edit-files">
            <div class="inline-edit-file-input-wrapper">
                <input type="file" id="inlineEditFileInput" class="file-input" multiple 
                    accept=".pdf,.doc,.docx,.xls,.xlsx,.txt,.jpg,.jpeg,.png,.gif,.webp,image/*,application/pdf">
                <label for="inlineEditFileInput" class="file-upload-label inline-edit-file-label">
                    <span class="file-icon">📎</span>
                    <span class="file-text">Anexar arquivos</span>
                </label>
            </div>
            <div id="inlineEditFileList" class="inline-edit-file-list"></div>
        </div>
        
        <div class="inline-edit-actions">
            <button type="button" class="btn-cancel" onclick="cancelInlineEdit()">Cancelar</button>
            <button type="button" class="btn-save" id="inlineEditSaveBtn" onclick="saveInlineEdit(${comentarioId})">Salvar</button>
        </div>
    `;
    
    // Insert after review-content
    reviewContent.parentNode.insertBefore(editContainer, reviewContent.nextSibling);
    
    // Render existing files
    renderInlineEditFileList();
    
    // Add file input listener
    const fileInput = document.getElementById('inlineEditFileInput');
    fileInput.addEventListener('change', handleInlineEditFileSelect);
    
    // Focus on textarea and move cursor to end
    const textarea = editContainer.querySelector('.inline-edit-textarea');
    textarea.focus();
    textarea.setSelectionRange(textarea.value.length, textarea.value.length);
    
    // Add input listener to enable/disable save button
    textarea.addEventListener('input', updateInlineEditSaveButton);
    
    // Scroll to make edit visible
    commentCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

/**
 * Handle file selection for inline edit
 */
function handleInlineEditFileSelect(event) {
    const files = Array.from(event.target.files);
    
    const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp', '.pdf', '.doc', '.docx', '.xls', '.xlsx', '.txt'];
    const allowedMimeTypes = [
        'image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp',
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'text/plain'
    ];
    
    files.forEach(file => {
        // Check file size
        if (file.size > MAX_FILE_SIZE) {
            showToast(`Arquivo "${file.name}" excede o tamanho máximo de 5MB`, 'error');
            return;
        }
        
        // Check file extension
        const extension = '.' + file.name.split('.').pop().toLowerCase();
        if (!allowedExtensions.includes(extension)) {
            showToast(`Tipo de arquivo não permitido: "${file.name}"`, 'error');
            return;
        }
        
        // Check MIME type
        if (!allowedMimeTypes.includes(file.type)) {
            showToast(`Tipo de arquivo não permitido: "${file.name}"`, 'error');
            return;
        }
        
        // Check if file already exists in new files
        const existsInNew = inlineEditFiles.some(f => f.name === file.name && f.size === file.size);
        // Check if file already exists in existing files
        const existsInExisting = inlineEditExistingFiles.some(f => f.nomeOriginal === file.name);
        
        if (!existsInNew && !existsInExisting) {
            inlineEditFiles.push(file);
        }
    });
    
    // Clear input to allow selecting the same file again
    event.target.value = '';
    
    renderInlineEditFileList();
}

/**
 * Render file list for inline edit
 */
function renderInlineEditFileList() {
    const fileList = document.getElementById('inlineEditFileList');
    if (!fileList) return;
    
    let html = '';
    
    // Render existing files (from server)
    inlineEditExistingFiles.forEach((arquivo, index) => {
        const icon = getFileIcon(arquivo.nomeOriginal);
        html += `
            <div class="inline-edit-file-item existing-file">
                <div class="file-item-info">
                    <span class="file-item-icon">${icon}</span>
                    <span class="file-item-name">${escapeHtml(arquivo.nomeOriginal)}</span>
                    <span class="file-item-size">${formatFileSize(arquivo.tamanho)}</span>
                </div>
                <button type="button" class="file-item-remove" onclick="removeInlineEditExistingFile(${index})" title="Remover arquivo">×</button>
            </div>
        `;
    });
    
    // Render new files (to be uploaded)
    inlineEditFiles.forEach((file, index) => {
        const icon = getFileIcon(file.name);
        html += `
            <div class="inline-edit-file-item new-file">
                <div class="file-item-info">
                    <span class="file-item-icon">${icon}</span>
                    <span class="file-item-name">${escapeHtml(file.name)}</span>
                    <span class="file-item-size">${formatFileSize(file.size)}</span>
                    <span class="file-item-badge">Novo</span>
                </div>
                <button type="button" class="file-item-remove" onclick="removeInlineEditNewFile(${index})" title="Remover arquivo">×</button>
            </div>
        `;
    });
    
    fileList.innerHTML = html;
}

/**
 * Remove existing file from inline edit
 */
function removeInlineEditExistingFile(index) {
    inlineEditExistingFiles.splice(index, 1);
    renderInlineEditFileList();
}

/**
 * Remove new file from inline edit
 */
function removeInlineEditNewFile(index) {
    inlineEditFiles.splice(index, 1);
    renderInlineEditFileList();
}

/**
 * Update save button state for inline edit
 */
function updateInlineEditSaveButton() {
    const textarea = document.getElementById('inlineEditText');
    const saveBtn = document.getElementById('inlineEditSaveBtn');
    if (textarea && saveBtn) {
        const hasText = textarea.value.trim().length > 0;
        saveBtn.disabled = !hasText;
    }
}

/**
 * Cancelar edição inline
 */
function cancelInlineEdit() {
    const editContainer = document.querySelector('.inline-edit-container');
    if (editContainer) {
        const commentCard = editContainer.closest('.review-card');
        if (commentCard) {
            commentCard.classList.remove('editing');
            
            // Restore attachments visibility
            const attachments = commentCard.querySelector('.comment-attachments');
            if (attachments) {
                attachments.style.display = '';
            }
        }
        editContainer.remove();
    }
    
    // Clear editing state
    editingCommentId = null;
    inlineEditFiles = [];
    inlineEditExistingFiles = [];
}

/**
 * Salvar edição inline
 */
async function saveInlineEdit(comentarioId) {
    const textarea = document.getElementById('inlineEditText');
    const saveBtn = document.getElementById('inlineEditSaveBtn');
    const novoTexto = textarea.value.trim();
    
    if (!novoTexto) {
        showToast('O comentário não pode estar vazio.', 'error');
        return;
    }
    
    // Disable button and show loading
    saveBtn.disabled = true;
    saveBtn.textContent = 'Salvando...';
    
    try {
        const formData = new FormData();
        formData.append('novoTexto', novoTexto);
        
        // Add IDs of existing files to keep
        inlineEditExistingFiles.forEach(arquivo => {
            formData.append('existingFileIds', arquivo.id);
        });
        
        // Add new files to upload
        inlineEditFiles.forEach(file => {
            formData.append('files', file);
        });
        
        const response = await fetch(`/api/comentario/editar/${comentarioId}`, {
            method: 'POST',
            body: formData
        });
        
        if (!response.ok) {
            const errorText = await response.text();
            showToast(parseErrorMessage(errorText) || 'Erro ao editar comentário', 'error');
            saveBtn.disabled = false;
            saveBtn.textContent = 'Salvar';
            return;
        }
        
        const result = await response.json();
        console.log('Comentário editado com sucesso:', result);
        
        // Reload page to show updated comment with new files
        // This is simpler than trying to update all the file attachments in place
        window.location.reload();
        
    } catch (error) {
        console.error('Erro ao editar comentário:', error);
        showToast(parseErrorMessage(error.message) || 'Erro ao editar comentário', 'error');
        saveBtn.disabled = false;
        saveBtn.textContent = 'Salvar';
    }
}

// ============================================
// DELETE COMMENT
// ============================================

function markAsDeleted(comentario){
	comentario.deleted = true;
	if(comentario.filhos && comentario.filhos.length > 0){
		comentario.filhos.forEach(filho => markAsDeleted(filho));
	}
}

/**
 * Deletar um comentário
 */
async function deleteComment(comentarioId) {
    // Confirmação antes de deletar
    if (!confirm('Tem certeza que deseja deletar este comentário? Esta ação não pode ser desfeita.')) {
        return;
    }
    
    // Find the comment card and delete button
    const commentCard = document.querySelector(`[data-comment-id="${comentarioId}"]`);
    const deleteButton = commentCard?.querySelector('.delete-btn');
    
    // Add loading state to button
    if (deleteButton) {
        deleteButton.disabled = true;
        deleteButton.classList.add('btn-loading');
        const originalText = deleteButton.innerHTML;
        deleteButton.dataset.originalHtml = originalText;
        deleteButton.innerHTML = '<span class="vote-icon">⏳</span><span>Deletando...</span>';
    }
    
    // Add loading overlay to card
    if (commentCard) {
        commentCard.classList.add('optimistic-pending');
    }
    
    try {
        console.log(`Deletando comentário ${comentarioId}...`);
        
        const response = await fetch(`/api/comentario/delete/${comentarioId}`, {
            method: 'POST'
        });
        
        if (!response.ok) {
            const errorText = await response.text();
            
            // Restore button state on error
            if (deleteButton) {
                deleteButton.disabled = false;
                deleteButton.classList.remove('btn-loading');
                deleteButton.innerHTML = deleteButton.dataset.originalHtml || originalText;
            }
            
            if (commentCard) {
                commentCard.classList.remove('optimistic-pending');
                commentCard.classList.add('optimistic-error');
                setTimeout(() => commentCard.classList.remove('optimistic-error'), 500);
            }
            
            showToast(parseErrorMessage(errorText) || 'Erro ao deletar comentário', 'error');
            return;
        }
        
        console.log('Comentário deletado com sucesso');
        
        // Success animation before removal
        if (commentCard) {
            commentCard.classList.remove('optimistic-pending');
            commentCard.classList.add('optimistic-success');
        }
        
        // Wait for animation before removing from DOM
        setTimeout(() => {
            // ✅ Remover comentário do array global
            const indexGlobal = allComments.findIndex(c => c.id === comentarioId);
				
            if (indexGlobal !== -1) {
					markAsDeleted(allComments[indexGlobal]);
            }
            
            // ✅ Atualizar visualização - comentários agora são apenas de professores
            if (professorSelecionado !== null) {
                const comentariosProfessor = allComments.filter(c => 
                    String(c.professorId) === String(professorSelecionado)
                );
                
                // Encontrar nome do professor
                const profIndex = PROFESSORES_DATA.findIndex(p => {
                    const profId = p.id || p.professorId || p.ID;
                    return String(profId) === String(professorSelecionado);
                });
                
                const profNome = profIndex !== -1 
                    ? (PROFESSORES_DATA[profIndex]?.nome || PROFESSORES_DATA[profIndex]?.name || 'Professor')
                    : 'Professor';
                
                mostrarComentarios(comentariosProfessor, profNome);
            }
        }, 300); // Small delay for animation
        
        // ✅ Feedback visual
        showToast('Comentário deletado com sucesso!', 'success');
        
    } catch (error) {
        console.error('Erro ao deletar comentário:', error);
        showToast(parseErrorMessage(error.message) || 'Erro ao deletar comentário', 'error');
    }
}

// ============================================
// REPORT COMMENT
// ============================================

/**
 * Denunciar um comentário
 */
async function reportComment(comentarioId) {
    // Confirmação antes de denunciar
    if (!confirm('Tem certeza que deseja denunciar este comentário?\n\nUm administrador irá revisar este comentário.')) {
        return;
    }
    
    // Find the comment card and report button
    const commentCard = document.querySelector(`[data-comment-id="${comentarioId}"]`);
    const reportButton = commentCard?.querySelector('.report-btn');
    
    // Add loading state to button
    if (reportButton) {
        reportButton.disabled = true;
        reportButton.classList.add('btn-loading');
        const originalHTML = reportButton.innerHTML;
        reportButton.dataset.originalHtml = originalHTML;
        reportButton.innerHTML = '<span>Denunciando...</span>';
    }
    
    try {
        console.log(`Denunciando comentário ${comentarioId}...`);
        
        const response = await fetch(`/api/comentario/denunciar/${comentarioId}`, {
            method: 'POST'
        });
        
        // Tratar caso de denúncia duplicada (409 Conflict)
        if (response.status === 409) {
            const result = await response.json();
            
            // Marcar botão como já denunciado
            if (reportButton) {
                reportButton.disabled = true;
                reportButton.classList.remove('btn-loading');
                reportButton.classList.add('reported');
                reportButton.innerHTML = `
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                        <polyline points="22 4 12 14.01 9 11.01"/>
                    </svg>
                    <span>denunciado</span>
                `;
            }
            
            showToast(result.message || 'Você já denunciou este comentário anteriormente.', 'warning');
            return;
        }
        
        if (!response.ok) {
            const errorText = await response.text();
            
            // Restore button state on error
            if (reportButton) {
                reportButton.disabled = false;
                reportButton.classList.remove('btn-loading');
                reportButton.innerHTML = reportButton.dataset.originalHtml;
            }
            
            showToast(parseErrorMessage(errorText) || 'Erro ao denunciar comentário', 'error');
            return;
        }
        
        const result = await response.json();
        console.log('Comentário denunciado com sucesso:', result);
        
        // Update button to show reported state
        if (reportButton) {
            reportButton.disabled = true;
            reportButton.classList.remove('btn-loading');
            reportButton.classList.add('reported');
            reportButton.innerHTML = `
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                    <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
                <span>denunciado</span>
            `;
        }
        
        showToast('Comentário denunciado! Um administrador irá revisar.', 'success');
        
    } catch (error) {
        console.error('Erro ao denunciar comentário:', error);
        
        // Restore button state on error
        if (reportButton) {
            reportButton.disabled = false;
            reportButton.classList.remove('btn-loading');
            reportButton.innerHTML = reportButton.dataset.originalHtml;
        }
        
        showToast(parseErrorMessage(error.message) || 'Erro ao denunciar comentário', 'error');
    }
}
