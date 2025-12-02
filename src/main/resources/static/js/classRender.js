// ============================================
// CLASS RENDER JS
// Responsável por carregar dados do backend,
// controlar toasts e renderizar elementos básicos da página
// ============================================

console.log('✅ classRender.js carregado');

// ============================================
// GLOBAL VARIABLES (shared across all modules)
// ============================================
let professorSelecionado = null;
let selectedFiles = [];
const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
let isAdmin = false;
let editingCommentId = null;
let inlineEditFiles = [];
let inlineEditExistingFiles = [];
let replyingToCommentId = null;
let allComments = [];

// ============================================
// HELPER FUNCTIONS FOR COMMENTS
// ============================================

function generateListAllComments(listaComentario, lista) {
    listaComentario.forEach(comentario => {
        buscaFilho(comentario, lista)
    });
}

function buscaFilho(comentario, lista) {
    if (comentario.filhos && comentario.filhos.length > 0) {
        comentario.filhos.forEach(filho => {
            buscaFilho(filho, lista);
        });
    }
    lista.push(comentario);
}

// ============================================
// TOAST NOTIFICATION SYSTEM
// ============================================

/**
 * Show a toast notification
 * @param {string} message - The message to display
 * @param {string} type - 'success', 'error', 'warning', 'info'
 * @param {number} duration - Duration in ms (default 4000)
 */
function showToast(message, type = 'info', duration = 4000) {
    // Create toast container if it doesn't exist
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 10001;
            display: flex;
            flex-direction: column;
            gap: 10px;
            max-width: 400px;
        `;
        document.body.appendChild(container);
    }
    
    // Create toast element
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    // Icon based on type
    const icons = {
        success: '✓',
        error: '✕',
        warning: '⚠',
        info: 'ℹ'
    };
    
    // Colors based on type
    const colors = {
        success: { bg: '#d4edda', border: '#28a745', text: '#155724', icon: '#28a745' },
        error: { bg: '#f8d7da', border: '#dc3545', text: '#721c24', icon: '#dc3545' },
        warning: { bg: '#fff3cd', border: '#ffc107', text: '#856404', icon: '#856404' },
        info: { bg: '#d1ecf1', border: '#17a2b8', text: '#0c5460', icon: '#17a2b8' }
    };
    
    const color = colors[type] || colors.info;
    
    toast.style.cssText = `
        display: flex;
        align-items: flex-start;
        gap: 12px;
        padding: 14px 16px;
        background: ${color.bg};
        border: 1px solid ${color.border};
        border-left: 4px solid ${color.border};
        border-radius: 6px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        animation: slideInRight 0.3s ease-out;
        color: ${color.text};
        font-size: 14px;
        line-height: 1.5;
    `;
    
    toast.innerHTML = `
        <span style="font-size: 18px; font-weight: bold; color: ${color.icon}; flex-shrink: 0;">${icons[type]}</span>
        <span style="flex: 1; word-break: break-word;">${escapeHtmlForToast(message)}</span>
        <button onclick="this.parentElement.remove()" style="
            background: none;
            border: none;
            font-size: 18px;
            cursor: pointer;
            color: ${color.text};
            opacity: 0.7;
            padding: 0;
            line-height: 1;
            flex-shrink: 0;
        ">&times;</button>
    `;
    
    // Add CSS animation if not exists
    if (!document.getElementById('toast-styles')) {
        const style = document.createElement('style');
        style.id = 'toast-styles';
        style.textContent = `
            @keyframes slideInRight {
                from {
                    transform: translateX(100%);
                    opacity: 0;
                }
                to {
                    transform: translateX(0);
                    opacity: 1;
                }
            }
            @keyframes slideOutRight {
                from {
                    transform: translateX(0);
                    opacity: 1;
                }
                to {
                    transform: translateX(100%);
                    opacity: 0;
                }
            }
        `;
        document.head.appendChild(style);
    }
    
    container.appendChild(toast);
    
    // Auto remove after duration
    setTimeout(() => {
        toast.style.animation = 'slideOutRight 0.3s ease-out forwards';
        setTimeout(() => toast.remove(), 300);
    }, duration);
}

/**
 * Escape HTML for toast messages to prevent XSS
 */
function escapeHtmlForToast(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * Parse and extract clean error message from potentially HTML response
 * @param {string} text - The error text (may be HTML or plain text)
 * @returns {string} - Clean error message
 */
function parseErrorMessage(text) {
    if (!text) return 'Erro desconhecido';
    
    // If it looks like HTML (contains tags)
    if (text.includes('<html') || text.includes('<body') || text.includes('<!DOCTYPE')) {
        // Try to extract meaningful error from common patterns
        
        // Try to find error in <h1> or <title>
        const h1Match = text.match(/<h1[^>]*>([^<]+)<\/h1>/i);
        if (h1Match) return h1Match[1].trim();
        
        const titleMatch = text.match(/<title[^>]*>([^<]+)<\/title>/i);
        if (titleMatch) return titleMatch[1].trim();
        
        // Try to find "message" in JSON-like content
        const messageMatch = text.match(/"message"\s*:\s*"([^"]+)"/);
        if (messageMatch) return messageMatch[1];
        
        // Try to find common Spring Boot error patterns
        const errorMatch = text.match(/There was an unexpected error[^<]*/i);
        if (errorMatch) return 'Ocorreu um erro interno no servidor';
        
        // Generic fallback for HTML
        return 'Ocorreu um erro no servidor. Tente novamente.';
    }
    
    // If it's JSON, try to parse it
    try {
        const json = JSON.parse(text);
        return json.message || json.error || text;
    } catch {
        // Not JSON, return as is (but truncate if too long)
        if (text.length > 200) {
            return text.substring(0, 200) + '...';
        }
        return text;
    }
}

// ============================================
// PAGE INITIALIZATION
// ============================================

document.addEventListener('DOMContentLoaded', function() {
    if (typeof AVALIACOES_DATA === 'undefined') return;
    if (typeof COMENTARIOS_DATA === 'undefined') {
        console.error('COMENTARIOS_DATA não está definido!');
        return;
    }
    
    // Obter email do usuário logado e status de admin
    isAdmin = typeof IS_ADMIN !== 'undefined' ? IS_ADMIN : false;
    const hasProfessors = typeof HAS_PROFESSORS !== 'undefined' ? HAS_PROFESSORS : false;

    generateListAllComments(COMENTARIOS_DATA, allComments);

    console.log('📊 Dados carregados:', {
        avaliacoes: AVALIACOES_DATA.length,
        comentarios: COMENTARIOS_DATA.length,
        listaComentarios: allComments.length,
        professores: PROFESSORES_DATA?.length || 0,
        isAdmin: isAdmin,
        hasProfessors: hasProfessors
    });
    
    // Separar avaliações
    const avaliacoesDisciplina = AVALIACOES_DATA.filter(a => !a.professorId);
    const avaliacoesProfessores = AVALIACOES_DATA.filter(a => a.professorId);
    
    // Atualizar disciplina com estatísticas (sem comentários - agora só para professores)
    const statsDisciplina = calcularStats(avaliacoesDisciplina);
    atualizarDisciplina(statsDisciplina, []);
    
    // Atualizar professores e adicionar event listeners
    if (typeof PROFESSORES_DATA !== 'undefined' && hasProfessors) {
        console.log('👨‍🏫 Professores:', PROFESSORES_DATA);
        
        PROFESSORES_DATA.forEach((prof, index) => {
            console.log(`Professor ${index}:`, prof);
            
            // Identificar qual propriedade contém o ID (pode ser 'id', 'professorId', ou outra)
            const profId = prof.id || prof.professorId || prof.ID || index;
            const profNome = prof.nome || prof.name || 'Professor';
            
            const avaliacoesProf = avaliacoesProfessores.filter(a => 
                String(a.professorId) === String(profId)
            );
            const stats = calcularStats(avaliacoesProf);
            atualizarProfessor(index, stats);
            
            // Adicionar click listener
            const professorItem = document.querySelectorAll('.professor-item')[index];
            if (professorItem) {
                professorItem.addEventListener('click', () => {
                    // ✅ Não permitir desselecionar - apenas trocar de professor
                    if (String(professorSelecionado) !== String(profId)) {
                        console.log('Alternando para professor:', profId, profNome);
                        selecionarProfessor(profId, profNome);
                    }
                });
            }
        });
        
        // ✅ Selecionar primeiro professor automaticamente se nenhum estiver salvo
        let professorRestaurado = false;
        
        // Tentar restaurar professor selecionado do sessionStorage (persiste apenas na sessão/aba atual)
        if (typeof CLASS_ID !== 'undefined') {
            const savedProfessor = sessionStorage.getItem(`selectedProfessor_${CLASS_ID}`);
            if (savedProfessor) {
                try {
                    const { id, nome } = JSON.parse(savedProfessor);
                    // Verificar se o professor ainda existe na lista
                    const profExists = PROFESSORES_DATA?.some(p => {
                        const profId = p.id || p.professorId || p.ID;
                        return String(profId) === String(id);
                    });
                    if (profExists) {
                        console.log('🔄 Restaurando professor salvo:', id, nome);
                        selecionarProfessor(id, nome);
                        professorRestaurado = true;
                    } else {
                        // Professor não existe mais, limpar sessionStorage
                        sessionStorage.removeItem(`selectedProfessor_${CLASS_ID}`);
                    }
                } catch (e) {
                    console.error('Erro ao restaurar professor:', e);
                    sessionStorage.removeItem(`selectedProfessor_${CLASS_ID}`);
                }
            }
        }
        
        // Se não restaurou nenhum professor, selecionar o primeiro
        if (!professorRestaurado && PROFESSORES_DATA.length > 0) {
            const firstProf = PROFESSORES_DATA[0];
            const firstProfId = firstProf.id || firstProf.professorId || firstProf.ID || 0;
            const firstProfNome = firstProf.nome || firstProf.name || 'Professor';
            console.log('📌 Selecionando primeiro professor:', firstProfId, firstProfNome);
            selecionarProfessor(firstProfId, firstProfNome);
        }
    } else {
        // ✅ Disciplina sem professores - exibir mensagem e desabilitar comentários
        console.log('⚠️ Disciplina sem professores cadastrados');
        mostrarInterfaceSemProfessores();
    }
    
    // Event listener no header da disciplina - não faz mais nada (não dá para desselecionar)
    const disciplineHeader = document.querySelector('.discipline-header');
    if (disciplineHeader) {
        disciplineHeader.style.cursor = 'default';
    }
    
    // Setup comment editor
    setupCommentEditor();
    
    // ✅ Setup interactive stars for discipline and professors
    setupInteractiveStars();
});

// ============================================
// COMMENT EDITOR SETUP
// ============================================

function setupCommentEditor() {
    const textarea = document.getElementById('commentText');
    const submitBtn = document.getElementById('submitBtn');
    
    if (textarea && submitBtn) {
        textarea.addEventListener('input', function() {
            updateSubmitButton();
        });
        
        // ✅ Add paste event listener for images and files
        textarea.addEventListener('paste', handlePaste);
    }
    
    // Also add paste listener to the entire comment form
    const commentForm = document.getElementById('commentForm');
    if (commentForm) {
        commentForm.addEventListener('paste', handlePaste);
    }
}

/**
 * Handle paste events to extract images from clipboard
 */
function handlePaste(event) {
    const items = event.clipboardData?.items;
    if (!items) return;
    
    let hasFiles = false;
    
    for (let i = 0; i < items.length; i++) {
        const item = items[i];
        
        // Check if item is a file
        if (item.kind === 'file') {
            const file = item.getAsFile();
            
            if (file) {
                // Check if it's an allowed file type
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
                
                const fileExtension = '.' + file.name.split('.').pop().toLowerCase();
                const isAllowedType = allowedMimeTypes.includes(file.type) || allowedExtensions.includes(fileExtension);
                
                if (isAllowedType) {
                    // Check file size
                    if (file.size > MAX_FILE_SIZE) {
                        const fileList = document.getElementById('fileList');
                        const errorDiv = document.createElement('div');
                        errorDiv.className = 'file-error';
                        errorDiv.textContent = `❌ ${file.name} é muito grande (máx: 5MB)`;
                        fileList.appendChild(errorDiv);
                        fileList.style.display = 'block';
                        
                        setTimeout(() => {
                            errorDiv.remove();
                            if (fileList.children.length === 0) {
                                fileList.style.display = 'none';
                            }
                        }, 3000);
                        continue;
                    }
                    
                    // Add to selected files
                    selectedFiles.push(file);
                    hasFiles = true;
                    
                    console.log('📎 File pasted:', file.name, file.type);
                } else {
                    const fileList = document.getElementById('fileList');
                    const errorDiv = document.createElement('div');
                    errorDiv.className = 'file-error';
                    errorDiv.textContent = `❌ ${file.name} não é um tipo de arquivo permitido`;
                    fileList.appendChild(errorDiv);
                    fileList.style.display = 'block';
                    
                    setTimeout(() => {
                        errorDiv.remove();
                        if (fileList.children.length === 0) {
                            fileList.style.display = 'none';
                        }
                    }, 3000);
                }
            }
        }
    }
    
    if (hasFiles) {
        renderFileList();
        
        // Show a notification to the user
        const fileList = document.getElementById('fileList');
        const notification = document.createElement('div');
        notification.className = 'file-success';
        notification.style.color = '#27ae60';
        notification.style.padding = '8px';
        notification.style.marginBottom = '8px';
        notification.textContent = `✓ ${selectedFiles.length === 1 ? 'Arquivo colado' : selectedFiles.length + ' arquivos colados'}`;
        fileList.insertBefore(notification, fileList.firstChild);
        
        setTimeout(() => {
            notification.remove();
        }, 2000);
    }
}

function updateSubmitButton() {
    const textarea = document.getElementById('commentText');	
    const submitBtn = document.getElementById('submitBtn');
    const hasText = textarea.value.trim().length > 0;
    // ✅ Comentário não precisa mais de rating
    submitBtn.disabled = !hasText;
}

function toggleCommentEditor() {
    const editor = document.getElementById('commentEditor');
    const button = document.querySelector('.btn-add-review');
    const sectionCard = document.querySelector('.right-column .section-card');
    
    // Check if editor is currently in reply mode (positioned inline elsewhere)
    const isInlineMode = replyingToCommentId !== null;
    // Also check if editor is not in its original container
    const isOutOfPlace = editor.parentNode !== sectionCard;
    
    // Cancel any inline edit in progress
    cancelInlineEdit();
    
    if (editor.classList.contains('show') && !isInlineMode && !isOutOfPlace) {
        // Close editor only if in original position and not in reply mode
        editor.classList.remove('show');
        button.classList.remove('active');
        resetCommentEditor();
    } else {
        // If in inline mode or out of place, reset and move back to top first
        if (isInlineMode || isOutOfPlace) {
            // First hide, reset, then show again
            editor.classList.remove('show');
            resetCommentEditor();
        }
        
        // Open editor at original position (top)
        editor.classList.add('show');
        button.classList.add('active');
        
        // Update subtitle based on current context
        updateEditorSubtitle();
        
        // Check if there's already text and enable submit button accordingly
        updateSubmitButton();
        
        // Scroll to editor at top and focus
        setTimeout(() => {
            editor.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            document.getElementById('commentText').focus();
        }, 100);
    }
}

function updateEditorSubtitle() {
    const subtitle = document.getElementById('editorSubtitle');
    
    // If replying, show reply message
    if (replyingToCommentId !== null) {
        subtitle.textContent = '💬 Respondendo ao comentário';
        return;
    }
    
    if (professorSelecionado !== null && PROFESSORES_DATA) {
        // Find professor name
        const prof = PROFESSORES_DATA.find(p => {
            const profId = p.id || p.professorId || p.ID;
            return String(profId) === String(professorSelecionado);
        });
        
        const profNome = prof?.nome || prof?.name || 'este professor';
        subtitle.textContent = `Comentando sobre ${profNome}`;
    } else {
        subtitle.textContent = 'Comentando sobre a disciplina';
    }
}

function resetCommentEditor() {
    // Clear textarea
    document.getElementById('commentText').value = '';
    
    // Clear files
    selectedFiles = [];
    document.getElementById('fileInput').value = '';
    renderFileList();
    
    // Clear reply mode
    replyingToCommentId = null;
    
    // Reset editor positioning and move back to original location
    const editor = document.getElementById('commentEditor');
    if (editor) {
        // Remove inline styles
        editor.style.position = '';
        editor.style.top = '';
        editor.style.left = '';
        editor.style.right = '';
        editor.style.marginLeft = '';
        editor.style.maxWidth = '';
        
        // Remove reply mode class
        editor.classList.remove('reply-mode');
        
        // Move editor back to its original container (inside .section-card, before .reviews-list)
        const sectionCard = document.querySelector('.right-column .section-card');
        const reviewsList = document.querySelector('.reviews-list');
        if (sectionCard && reviewsList && editor.parentNode !== sectionCard) {
            // Insert before reviews list (original position)
            sectionCard.insertBefore(editor, reviewsList);
        }
    }
    
    // Disable submit button
    document.getElementById('submitBtn').disabled = true;
}

// ============================================
// PROFESSOR SELECTION & UI
// ============================================

// Selecionar professor
function selecionarProfessor(professorId, professorNome) {
    console.log('Selecionando professor:', professorId, 'Atual:', professorSelecionado);
    
    // Atualizar professor selecionado
    professorSelecionado = professorId;
    
    // Salvar no sessionStorage para persistir durante refresh (limpa ao fechar aba)
    if (typeof CLASS_ID !== 'undefined') {
        sessionStorage.setItem(`selectedProfessor_${CLASS_ID}`, JSON.stringify({ id: professorId, nome: professorNome }));
    }
    
    // ✅ FIX: Recalcular avaliações do professor dinamicamente do array global
    const avaliacoesProfessores = AVALIACOES_DATA.filter(a => a.professorId);
    const avaliacoes = avaliacoesProfessores.filter(a => 
        String(a.professorId) === String(professorId)
    );
    
    console.log(`📊 Avaliações do professor ${professorNome}:`, avaliacoes.length);
    
    // Remover seleção anterior de TODOS os professores
    document.querySelectorAll('.professor-item').forEach(item => {
        item.classList.remove('selected');
    });
    
    // Encontrar e selecionar o professor correto
    const professorItems = Array.from(document.querySelectorAll('.professor-item'));
    professorItems.forEach((item, idx) => {
        // Obter ID do professor usando a mesma lógica de fallback
        const prof = PROFESSORES_DATA[idx];
        if (prof) {
            const profId = prof.id || prof.professorId || prof.ID || idx;
            if (String(profId) === String(professorId)) {
                item.classList.add('selected');
                console.log('✓ Professor selecionado no índice:', idx, 'ID:', profId);
            }
        }
    });
    
    const sectionTitle = document.querySelector('.section-header h2');
    if (sectionTitle) {
        sectionTitle.textContent = `Comentários para ${professorNome}`;
    }
    
    // ✅ Mostrar comentários do professor usando allComments
    const comentariosProfessor = allComments.filter(c => 
        String(c.professorId) === String(professorId)
    );
    mostrarComentarios(comentariosProfessor, professorNome);
    
    // Update editor subtitle if editor is open
    updateEditorSubtitle();
}

// ✅ Mostrar interface quando não há professores cadastrados
function mostrarInterfaceSemProfessores() {
    // Atualizar título da seção de comentários
    const sectionTitle = document.querySelector('.section-header h2');
    if (sectionTitle) {
        sectionTitle.textContent = 'Comentários';
    }
    
    // Esconder botão de adicionar comentário
    const addReviewBtn = document.querySelector('.btn-add-review');
    if (addReviewBtn) {
        addReviewBtn.style.display = 'none';
    }
    
    // Esconder editor de comentário
    const commentEditor = document.getElementById('commentEditor');
    if (commentEditor) {
        commentEditor.style.display = 'none';
    }
    
    // Mostrar mensagem na lista de comentários
    const commentsList = document.getElementById('commentsList');
    if (commentsList) {
        commentsList.innerHTML = `
            <div class="no-comments-message" style="text-align: center; padding: 40px 20px; color: var(--text-secondary);">
                <div style="font-size: 48px; margin-bottom: 16px;">📚</div>
                <h3 style="margin-bottom: 8px; color: var(--text-primary);">Disciplina sem professores cadastrados</h3>
                <p>Esta disciplina ainda não possui professores vinculados.</p>
                <p>Comentários só podem ser feitos para professores específicos.</p>
            </div>
        `;
    }
}
