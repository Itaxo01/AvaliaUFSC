// ============================================
// AVALIATION HANDLER JS
// Responsável por lidar com avaliações (ratings):
// Estrelas interativas, médias e cálculos
// ============================================

console.log('✅ avaliationHandler.js carregado');

// ============================================
// INTERACTIVE STARS SETUP
// ============================================

// ✅ Make stars clickable for rating submission
function setupInteractiveStars() {
    // Discipline stars (Desktop)
    const disciplineStars = document.querySelector('.discipline-rating-stars');
    if (disciplineStars) {
        makeStarsInteractive(disciplineStars, null);
    }
    
    // Discipline stars (Mobile)
    const disciplineStarsMobile = document.querySelector('.discipline-rating-stars-mobile');
    if (disciplineStarsMobile) {
        makeStarsInteractive(disciplineStarsMobile, null);
    }
    
    // Professor stars (Desktop)
    const professorItems = document.querySelectorAll('.professor-item');
    professorItems.forEach((item, index) => {
        const starsContainer = item.querySelector('.rating-stars');
        if (starsContainer && PROFESSORES_DATA && PROFESSORES_DATA[index]) {
            const prof = PROFESSORES_DATA[index];
            const profId = prof.id || prof.professorId || prof.ID || index;
            makeStarsInteractive(starsContainer, profId);
        }
    });
    
    // Professor stars (Mobile Sidebar)
    setupSidebarProfessorStars();
}

// ✅ Setup sidebar professor stars (mobile)
function setupSidebarProfessorStars() {
    const sidebarItems = document.querySelectorAll('.sidebar-professor-item');
    sidebarItems.forEach((item) => {
        const profId = item.dataset.professorId;
        const starsContainer = item.querySelector('.sidebar-professor-rating .rating-stars');
        
        if (starsContainer && profId) {
            // Make stars clickable in sidebar
            makeSidebarStarsClickable(starsContainer, profId, item);
        }
    });
}

function makeStarsInteractive(starsContainer, professorId) {
    // ✅ FIX: Remove existing listeners by cloning to prevent duplicate submissions
    starsContainer.querySelectorAll('.star').forEach((oldStar, index) => {
        const newStar = oldStar.cloneNode(true);
        oldStar.parentNode.replaceChild(newStar, oldStar);
    });
    
    // Re-query stars after cloning
    const freshStars = starsContainer.querySelectorAll('.star');
    
    // Get initial average rating to display
    const avgRating = parseFloat(starsContainer.closest('.discipline-rating, .professor-rating, .mobile-card-rating')
        ?.querySelector('.rating-value, .rating-score, .mobile-rating-score')?.textContent) || 0;
    
    // Check if current user has voted
    const userRating = getUserCurrentRating(professorId);
    const hasUserVoted = userRating !== null;
    
    // Display the average rating on load
    if (avgRating > 0) {
        freshStars.forEach((star, index) => {
            star.classList.remove('filled', 'half', 'hover', 'user-voted', 'half-user-voted');
            if (avgRating >= index + 1) {
                star.classList.add('filled');
                // Add user-voted class if user has voted
                if (hasUserVoted) {
                    star.classList.add('user-voted');
                }
            } else if (avgRating >= index + 0.5) {
                // Add appropriate half class based on whether user voted
                if (hasUserVoted) {
                    star.classList.add('half-user-voted');
                } else {
                    star.classList.add('half');
                }
            }
        });
    }
    
    // ✅ Make stars clickable to open rating modal
    freshStars.forEach(star => {
        star.style.cursor = 'pointer';
        star.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation(); // Prevent bubbling to parent elements
            openRatingModal(professorId);
        });
    });
    
    // Also make the container itself clickable
    starsContainer.style.cursor = 'pointer';
    starsContainer.addEventListener('click', (e) => {
        // Only trigger if clicking on container directly, not on stars
        if (e.target === starsContainer) {
            e.preventDefault();
            e.stopPropagation();
            openRatingModal(professorId);
        }
    });
}

// ✅ Make sidebar stars clickable (for mobile professor sidebar)
function makeSidebarStarsClickable(starsContainer, professorId, parentItem) {
    // Clone stars to remove existing listeners
    starsContainer.querySelectorAll('.star').forEach((oldStar) => {
        const newStar = oldStar.cloneNode(true);
        oldStar.parentNode.replaceChild(newStar, oldStar);
    });
    
    // Re-query stars
    const freshStars = starsContainer.querySelectorAll('.star');
    
    // Get average rating for this professor
    const avgRating = parseFloat(starsContainer.closest('.sidebar-professor-rating')
        ?.querySelector('.rating-value')?.textContent) || 0;
    
    // Check if user has voted
    const userRating = getUserCurrentRating(professorId);
    const hasUserVoted = userRating !== null;
    
    // Display the average rating
    if (avgRating > 0) {
        freshStars.forEach((star, index) => {
            star.classList.remove('filled', 'half', 'hover', 'user-voted', 'half-user-voted');
            if (avgRating >= index + 1) {
                star.classList.add('filled');
                if (hasUserVoted) star.classList.add('user-voted');
            } else if (avgRating >= index + 0.5) {
                star.classList.add(hasUserVoted ? 'half-user-voted' : 'half');
            }
        });
    }
    
    // Make stars and container clickable
    freshStars.forEach(star => {
        star.style.cursor = 'pointer';
        star.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation(); // CRITICAL: Stop propagation to prevent professor selection
            openRatingModal(professorId);
        });
    });
    
    // Make the rating container itself clickable
    const ratingContainer = starsContainer.closest('.sidebar-professor-rating');
    if (ratingContainer) {
        ratingContainer.style.cursor = 'pointer';
        ratingContainer.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation(); // CRITICAL: Stop propagation
            openRatingModal(professorId);
        });
    }
}

function highlightStars(stars, rating) {
    stars.forEach((star, index) => {
        // Remove all state classes first
        star.classList.remove('hover', 'half', 'filled', 'user-voted', 'half-user-voted');
        
        if (index < rating) {
            star.classList.add('hover');
        }
    });
}

function clearStarsHighlight(stars) {
    stars.forEach(star => {
        star.classList.remove('hover', 'half', 'filled', 'half-user-voted', 'user-voted');
    });
}

// ============================================
// RATING UTILITIES
// ============================================

function getUserCurrentRating(professorId) {
    const avaliacao = AVALIACOES_DATA.find(a => {
        return a.isOwner && (String(a.professorId || '') === String(professorId || ''));
    });

    console.log('Avaliação do usuário para professorId', professorId, ':', avaliacao?.nota);
    return avaliacao?.nota || null;
}

// Calcular média e total
function calcularStats(avaliacoes) {
    const comNota = avaliacoes.filter(a => a.nota > 0);
    if (comNota.length === 0) return { media: 0, total: 0 };
    
    const soma = comNota.reduce((acc, a) => acc + a.nota, 0);
    return { media: soma / comNota.length, total: comNota.length };
}

// ============================================
// UPDATE UI FUNCTIONS
// ============================================

// Atualizar header da disciplina
function atualizarDisciplina(stats, comentarios) {
    // Desktop header
    const desktopRatingScore = document.querySelector('.discipline-rating .rating-score');
    if (desktopRatingScore) {
        desktopRatingScore.textContent = stats.total > 0 ? stats.media.toFixed(1) : 'N/A';
    }
    
    const ratingCountElement = document.querySelector('.discipline-rating .rating-count');
    if (ratingCountElement) {
        ratingCountElement.textContent = 
            `${stats.total} ${stats.total === 1 ? 'avaliação' : 'avaliações'}`;
    }
    
    const desktopStars = document.querySelector('.discipline-rating .rating-stars');
    if (desktopStars) {
        preencherEstrelas(desktopStars, stats.media);
    }
    
    // ✅ Mobile card - atualizar também
    const mobileRatingScore = document.querySelector('.mobile-rating-score');
    if (mobileRatingScore) {
        mobileRatingScore.textContent = stats.total > 0 ? stats.media.toFixed(1) : 'N/A';
    }
    
    const mobileRatingCount = document.querySelector('.mobile-rating-count');
    if (mobileRatingCount) {
        mobileRatingCount.textContent = `(${stats.total})`;
    }
    
    const mobileStars = document.querySelector('.discipline-rating-stars-mobile');
    if (mobileStars) {
        preencherEstrelas(mobileStars, stats.media);
    }
    
    // Mostrar comentários da disciplina
    mostrarComentarios(comentarios, null);
}

// Atualizar professor específico
function atualizarProfessor(index, stats) {
    const professorItem = document.querySelectorAll('.professor-item')[index];
    if (!professorItem) return;
    
    professorItem.querySelector('.rating-value').textContent = 
        stats.total > 0 ? stats.media.toFixed(1) : 'N/A';
    
    const ratingCountElement = professorItem.querySelector('.rating-count');
    ratingCountElement.textContent = 
        `${stats.total} ${stats.total === 1 ? 'avaliação' : 'avaliações'}`;
    
    preencherEstrelas(professorItem.querySelector('.rating-stars'), stats.media);
}

// ✅ Atualizar rating na sidebar mobile
function updateSidebarProfessorRating(sidebarRating, stats) {
    if (!sidebarRating) return;
    
    const ratingValue = sidebarRating.querySelector('.rating-value');
    if (ratingValue) {
        ratingValue.textContent = stats.total > 0 ? stats.media.toFixed(1) : 'N/A';
    }
    
    const ratingStars = sidebarRating.querySelector('.rating-stars');
    if (ratingStars) {
        preencherEstrelas(ratingStars, stats.media);
    }
}

// Preencher estrelas
function preencherEstrelas(container, nota) {
    const stars = container.querySelectorAll('.star');
    stars.forEach((star, i) => {
        star.classList.remove('filled', 'half');
        if (nota >= i + 1) star.classList.add('filled');
        else if (nota >= i + 0.5) star.classList.add('half');
    });
}

// ============================================
// RATING MODAL & SUBMISSION
// ============================================

/**
 * Open rating modal for user to select stars
 */
function openRatingModal(professorId = null) {
    // Create modal if it doesn't exist
    let modal = document.getElementById('ratingModal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'ratingModal';
        modal.className = 'rating-modal';
        modal.innerHTML = `
            <div class="rating-modal-content">
                <span class="rating-modal-close" onclick="closeRatingModal()">&times;</span>
                <h3 id="ratingModalTitle">Avaliar</h3>
                <p id="ratingModalSubtitle">Selecione sua nota:</p>
                <div class="rating-modal-previous" id="ratingModalPrevious" style="display: none;">
                    <span class="previous-rating-label">Sua nota anterior:</span>
                    <span class="previous-rating-value" id="previousRatingValue"></span>
                </div>
                <div class="rating-modal-stars" id="ratingModalStars">
                    <span class="modal-star" data-rating="1">★</span>
                    <span class="modal-star" data-rating="2">★</span>
                    <span class="modal-star" data-rating="3">★</span>
                    <span class="modal-star" data-rating="4">★</span>
                    <span class="modal-star" data-rating="5">★</span>
                </div>
                <div class="rating-modal-actions">
                    <button class="btn-modal-cancel" onclick="closeRatingModal()">Cancelar</button>
                    <button class="btn-modal-submit" id="btnModalSubmitRating" disabled onclick="submitModalRating()">Confirmar</button>
                </div>
            </div>
        `;
        document.body.appendChild(modal);
        
        // Close on click outside
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                closeRatingModal();
            }
        });
    }
    
    // Store the professor ID for later use
    modal.dataset.professorId = professorId || '';
    
    // Update title based on context
    const titleElement = document.getElementById('ratingModalTitle');
    const subtitleElement = document.getElementById('ratingModalSubtitle');
    const previousRatingDiv = document.getElementById('ratingModalPrevious');
    const previousRatingValue = document.getElementById('previousRatingValue');
    
    // Check if user already voted
    const userCurrentRating = getUserCurrentRating(professorId);
    const hasUserVoted = userCurrentRating !== null;
    
    if (professorId !== null && professorId !== 'null' && PROFESSORES_DATA) {
        const prof = PROFESSORES_DATA.find(p => {
            const profId = p.id || p.professorId || p.ID;
            return String(profId) === String(professorId);
        });
        const profNome = prof?.nome || prof?.name || 'Professor';
        titleElement.textContent = hasUserVoted ? `Editar avaliação - ${profNome}` : `Avaliar ${profNome}`;
    } else {
        titleElement.textContent = hasUserVoted ? 'Editar avaliação - Disciplina' : 'Avaliar Disciplina';
    }
    
    // Update subtitle and show previous rating if user already voted
    if (hasUserVoted) {
        subtitleElement.textContent = 'Modifique sua nota:';
        previousRatingDiv.style.display = 'flex';
        // Show previous rating as stars
        previousRatingValue.innerHTML = '★'.repeat(userCurrentRating) + '☆'.repeat(5 - userCurrentRating);
    } else {
        subtitleElement.textContent = 'Selecione sua nota:';
        previousRatingDiv.style.display = 'none';
    }
    
    // Reset stars - clone to remove old event listeners
    const starsContainer = document.getElementById('ratingModalStars');
    const modalStars = starsContainer.querySelectorAll('.modal-star');
    modalStars.forEach((oldStar) => {
        const newStar = oldStar.cloneNode(true);
        oldStar.parentNode.replaceChild(newStar, oldStar);
    });
    
    // Re-query and setup fresh stars
    const freshStars = starsContainer.querySelectorAll('.modal-star');
    freshStars.forEach(star => {
        star.classList.remove('selected', 'hover');
        
        // Add hover and click events
        star.addEventListener('mouseenter', function() {
            const rating = parseInt(this.dataset.rating);
            highlightModalStars(rating);
        });
        
        star.addEventListener('click', function() {
            const rating = parseInt(this.dataset.rating);
            selectModalRating(rating);
        });
    });
    
    // Clone and replace container to remove old mouseleave listener
    const newStarsContainer = starsContainer.cloneNode(false);
    while (starsContainer.firstChild) {
        newStarsContainer.appendChild(starsContainer.firstChild);
    }
    starsContainer.parentNode.replaceChild(newStarsContainer, starsContainer);
    
    // Add mouseleave listener to new container
    newStarsContainer.addEventListener('mouseleave', function() {
        const selectedRating = modal.dataset.selectedRating;
        if (selectedRating) {
            highlightModalStars(parseInt(selectedRating));
        } else if (hasUserVoted) {
            // Show previous rating as default when hovering off
            highlightModalStars(userCurrentRating);
        } else {
            clearModalStars();
        }
    });
    
    // If user already voted, pre-select their previous rating
    if (hasUserVoted) {
        modal.dataset.selectedRating = userCurrentRating;
        selectModalRating(userCurrentRating);
        document.getElementById('btnModalSubmitRating').disabled = false;
    } else {
        // Reset selection
        delete modal.dataset.selectedRating;
        document.getElementById('btnModalSubmitRating').disabled = true;
    }
    
    // Show modal
    modal.style.display = 'flex';
}

/**
 * Close rating modal
 */
function closeRatingModal() {
    const modal = document.getElementById('ratingModal');
    if (modal) {
        modal.style.display = 'none';
        delete modal.dataset.selectedRating;
        delete modal.dataset.professorId;
        clearModalStars();
        
        // Remove loading overlay if present
        const overlay = modal.querySelector('.loading-overlay');
        if (overlay) overlay.remove();
        
        // Reset submit button state
        const submitButton = document.getElementById('btnModalSubmitRating');
        if (submitButton) {
            submitButton.disabled = true;
            submitButton.classList.remove('btn-loading');
            submitButton.textContent = 'Confirmar';
        }
    }
}

/**
 * Highlight modal stars on hover
 */
function highlightModalStars(rating) {
    const stars = document.querySelectorAll('.modal-star');
    stars.forEach((star, index) => {
        star.classList.remove('hover');
        if (index < rating) {
            star.classList.add('hover');
        }
    });
}

/**
 * Clear modal stars highlighting
 */
function clearModalStars() {
    const stars = document.querySelectorAll('.modal-star');
    stars.forEach(star => {
        star.classList.remove('hover', 'selected');
    });
}

/**
 * Select a rating in the modal
 */
function selectModalRating(rating) {
    const modal = document.getElementById('ratingModal');
    modal.dataset.selectedRating = rating;
    
    // Update stars to show selection
    const stars = document.querySelectorAll('.modal-star');
    stars.forEach((star, index) => {
        star.classList.remove('selected', 'hover');
        if (index < rating) {
            star.classList.add('selected');
        }
    });
    
    // Enable submit button
    document.getElementById('btnModalSubmitRating').disabled = false;
}

/**
 * Submit the rating from modal
 */
async function submitModalRating() {
    const modal = document.getElementById('ratingModal');
    const rating = parseInt(modal.dataset.selectedRating);
    const professorId = modal.dataset.professorId;
    
    if (!rating || rating < 1 || rating > 5) {
        showToast('Por favor, selecione uma avaliação.', 'warning');
        return;
    }
    
    // Don't close modal yet - show loading state
    const modalContent = modal.querySelector('.rating-modal-content');
    const submitButton = document.getElementById('btnModalSubmitRating');
    
    // Add loading overlay to modal
    if (modalContent && !modalContent.querySelector('.loading-overlay')) {
        const overlay = document.createElement('div');
        overlay.className = 'loading-overlay';
        overlay.innerHTML = `
            <div class="spinner"></div>
            <div class="loading-overlay-text">Enviando avaliação...</div>
        `;
        modalContent.style.position = 'relative';
        modalContent.appendChild(overlay);
    }
    
    // Disable submit button
    if (submitButton) {
        submitButton.disabled = true;
        submitButton.classList.add('btn-loading');
    }
    
    // Submit rating
    await submitRating(rating, professorId === '' ? null : professorId);
}

/**
 * Atualizar visualização após mudanças
 */
function atualizarVisualizacao() {
    // Atualizar visualização do professor selecionado (comentários agora são apenas para professores)
    if (professorSelecionado !== null) {
        const avaliacoesProfessores = AVALIACOES_DATA.filter(a => a.professorId);
        const avaliacoesProf = avaliacoesProfessores.filter(a => 
            String(a.professorId) === String(professorSelecionado)
        );
        const stats = calcularStats(avaliacoesProf);
        
        // Filtrar comentários do professor
        const comentariosProfessor = allComments.filter(c => 
            String(c.professorId) === String(professorSelecionado)
        );
        
        // Encontrar índice do professor
        const profIndex = PROFESSORES_DATA.findIndex(p => {
            const profId = p.id || p.professorId || p.ID;
            return String(profId) === String(professorSelecionado);
        });
        
        if (profIndex !== -1) {
            atualizarProfessor(profIndex, stats);
            
            // Atualizar lista de comentários
            const prof = PROFESSORES_DATA[profIndex];
            const profNome = prof?.nome || prof?.name || 'Professor';
            mostrarComentarios(comentariosProfessor, profNome);
        }
    }
    
    // ✅ Re-setup interactive stars after updating visualization
    setupInteractiveStars();
}

/**
 * Submeter rating (estrelas clicáveis)
 */
async function submitRating(rating, professorId = null) {
    
    if (rating < 1 || rating > 5) {
        showToast('Nota inválida.', 'warning');
        return;
    }
    
    console.log(`Submetendo rating: ${rating} estrelas para ${professorId ? 'professor' : 'disciplina'}`);
    
    const submitButton = document.getElementById('btnModalSubmitRating');
    if (submitButton) {
        submitButton.disabled = true;
        submitButton.classList.add('btn-loading');
        submitButton.textContent = 'Enviando...';
    }
    
    try {
        const formData = new FormData();
        formData.append('nota', rating);
        formData.append('disciplinaId', CLASS_ID);
        if (professorId) {
            formData.append('professorId', professorId);
        }
        
        const response = await fetch('/api/avaliacao/rating', {
            method: 'POST',
            body: formData
        });
        
        if (!response.ok) {
            const errorText = await response.text();
            
            // Remove loading overlay
            const modal = document.getElementById('ratingModal');
            const overlay = modal?.querySelector('.loading-overlay');
            if (overlay) overlay.remove();
            
            // Re-enable button
            if (submitButton) {
                submitButton.disabled = false;
                submitButton.classList.remove('btn-loading');
                submitButton.textContent = 'Confirmar';
            }
            
            showToast(parseErrorMessage(errorText) || 'Erro ao enviar avaliação', 'error');
            return;
        }
        
        const result = await response.json();
        console.log('Rating salvo:', result);
        
        // ✅ Check if it's an update BEFORE modifying AVALIACOES_DATA
        const isUpdate = getUserCurrentRating(professorId) !== null;
        
        // ✅ Update UI directly without reloading the page
        closeRatingModal();
        
        // Update AVALIACOES_DATA to reflect the new rating
        updateAvaliacoesData(professorId, rating, result.avaliacaoId);
        
        // Update the visual elements with new average
        updateRatingDisplay(professorId, result.novaMedia, result.totalAvaliacoes, rating);
        
        // Show success toast
        showToast(isUpdate ? 'Avaliação atualizada!' : 'Avaliação enviada!', 'success');
        
    } catch (error) {
        console.error('Erro ao enviar rating:', error);
        
        // Remove loading overlay
        const modal = document.getElementById('ratingModal');
        const overlay = modal?.querySelector('.loading-overlay');
        if (overlay) overlay.remove();
        
        // Re-enable button
        if (submitButton) {
            submitButton.disabled = false;
            submitButton.classList.remove('btn-loading');
            submitButton.textContent = 'Confirmar';
        }
        
        showToast(parseErrorMessage(error.message) || 'Erro ao enviar avaliação', 'error');
        return null;
    }
}

/**
 * Update AVALIACOES_DATA after submitting a rating
 */
function updateAvaliacoesData(professorId, nota, avaliacaoId) {
    // Find existing rating from user
    const existingIndex = AVALIACOES_DATA.findIndex(a => {
        return a.isOwner && (String(a.professorId || '') === String(professorId || ''));
    });
    
    if (existingIndex !== -1) {
        // Update existing rating
        AVALIACOES_DATA[existingIndex].nota = nota;
        console.log('Avaliação atualizada em AVALIACOES_DATA:', AVALIACOES_DATA[existingIndex]);
    } else {
        // Add new rating
        const newAvaliacao = {
            id: avaliacaoId,
            disciplinaId: CLASS_ID,
            professorId: professorId || null,
            nota: nota,
            createdAt: new Date().toISOString(),
            isOwner: true
        };
        AVALIACOES_DATA.push(newAvaliacao);
        console.log('Nova avaliação adicionada a AVALIACOES_DATA:', newAvaliacao);
    }
}

/**
 * Update rating display elements (stars, score, count)
 */
function updateRatingDisplay(professorId, novaMedia, totalAvaliacoes, userRating) {
    console.log('Atualizando display:', { professorId, novaMedia, totalAvaliacoes, userRating });
    
    if (professorId === null || professorId === '' || professorId === 'null') {
        // Update discipline rating
        updateDisciplineRatingDisplay(novaMedia, totalAvaliacoes, userRating);
    } else {
        // Update professor rating
        updateProfessorRatingDisplay(professorId, novaMedia, totalAvaliacoes, userRating);
    }
    
    // Re-setup interactive stars to reflect new state
    setupInteractiveStars();
}

/**
 * Update discipline rating display
 */
function updateDisciplineRatingDisplay(novaMedia, totalAvaliacoes, userRating) {
    // Desktop elements
    const desktopScore = document.querySelector('.discipline-rating .rating-score');
    const desktopCount = document.querySelector('.discipline-rating .rating-count');
    const desktopStars = document.querySelector('.discipline-rating-stars');
    
    if (desktopScore) {
        desktopScore.textContent = novaMedia > 0 ? novaMedia.toFixed(1) : 'N/A';
    }
    if (desktopCount) {
        desktopCount.textContent = `${totalAvaliacoes} ${totalAvaliacoes === 1 ? 'avaliação' : 'avaliações'}`;
    }
    if (desktopStars) {
        updateStarsVisual(desktopStars, novaMedia, true);
    }
    
    // Mobile elements
    const mobileScore = document.querySelector('.mobile-card-rating .mobile-rating-score');
    const mobileCount = document.querySelector('.mobile-card-rating .mobile-rating-count');
    const mobileStars = document.querySelector('.discipline-rating-stars-mobile');
    
    if (mobileScore) {
        mobileScore.textContent = novaMedia > 0 ? novaMedia.toFixed(1) : 'N/A';
    }
    if (mobileCount) {
        mobileCount.textContent = `${totalAvaliacoes} ${totalAvaliacoes === 1 ? 'avaliação' : 'avaliações'}`;
    }
    if (mobileStars) {
        updateStarsVisual(mobileStars, novaMedia, true);
    }
}

/**
 * Update professor rating display
 */
function updateProfessorRatingDisplay(professorId, novaMedia, totalAvaliacoes, userRating) {
    // Find professor index
    const profIndex = PROFESSORES_DATA.findIndex(p => {
        const profId = p.id || p.professorId || p.ID;
        return String(profId) === String(professorId);
    });
    
    if (profIndex === -1) {
        console.warn('Professor não encontrado:', professorId);
        return;
    }
    
    // Desktop professor list
    const professorItems = document.querySelectorAll('.professor-item');
    if (professorItems[profIndex]) {
        const item = professorItems[profIndex];
        const scoreElement = item.querySelector('.rating-value');
        const starsContainer = item.querySelector('.rating-stars');
        
        if (scoreElement) {
            scoreElement.textContent = novaMedia > 0 ? novaMedia.toFixed(1) : 'N/A';
        }
        if (starsContainer) {
            updateStarsVisual(starsContainer, novaMedia, true);
        }
    }
    
    // Mobile sidebar
    const sidebarItem = document.querySelector(`.sidebar-professor-item[data-professor-id="${professorId}"]`);
    if (sidebarItem) {
        const scoreElement = sidebarItem.querySelector('.rating-value');
        const starsContainer = sidebarItem.querySelector('.rating-stars');
        
        if (scoreElement) {
            scoreElement.textContent = novaMedia > 0 ? novaMedia.toFixed(1) : 'N/A';
        }
        if (starsContainer) {
            updateStarsVisual(starsContainer, novaMedia, true);
        }
    }
    
    // If this professor is currently selected, update main display too
    if (typeof professorSelecionado !== 'undefined' && String(professorSelecionado) === String(professorId)) {
        // Update main professor header if visible
        const mainProfessorScore = document.querySelector('.professor-rating .rating-score');
        const mainProfessorStars = document.querySelector('.professor-rating .rating-stars');
        const mainProfessorCount = document.querySelector('.professor-rating .rating-count');
        
        if (mainProfessorScore) {
            mainProfessorScore.textContent = novaMedia > 0 ? novaMedia.toFixed(1) : 'N/A';
        }
        if (mainProfessorStars) {
            updateStarsVisual(mainProfessorStars, novaMedia, true);
        }
        if (mainProfessorCount) {
            mainProfessorCount.textContent = `${totalAvaliacoes} ${totalAvaliacoes === 1 ? 'avaliação' : 'avaliações'}`;
        }
    }
}

/**
 * Update stars visual based on average rating
 */
function updateStarsVisual(starsContainer, avgRating, hasUserVoted = false) {
    const stars = starsContainer.querySelectorAll('.star');
    stars.forEach((star, index) => {
        star.classList.remove('filled', 'half', 'hover', 'user-voted', 'half-user-voted');
        
        if (avgRating >= index + 1) {
            star.classList.add('filled');
            if (hasUserVoted) {
                star.classList.add('user-voted');
            }
        } else if (avgRating >= index + 0.5) {
            if (hasUserVoted) {
                star.classList.add('half-user-voted');
            } else {
                star.classList.add('half');
            }
        }
    });
}
