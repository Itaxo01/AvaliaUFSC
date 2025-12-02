async function httpPost(url, body) {
	 const res = await fetch(url, {
		  method: "POST",
		  body: body,
		  headers: {
				"Content-type": "application/json; charset=UTF-8"
		  },
		  credentials: 'same-origin'
	 })

	 return res.status;
}
// User page JavaScript

// Modal functions
function openPasswordModal() {
    document.getElementById('passwordModal').style.display = 'block';
}

function closePasswordModal() {
    document.getElementById('passwordModal').style.display = 'none';
}

function openUploadModal() {
    document.getElementById('uploadModal').style.display = 'block';
}

function closeUploadModal() {
    document.getElementById('uploadModal').style.display = 'none';
}

function openDeleteModal() {
    document.getElementById('deleteModal').style.display = 'block';
}

function closeDeleteModal() {
    document.getElementById('deleteModal').style.display = 'none';
    document.getElementById('deleteConfirmation').value = '';
}

async function confirmDelete() {
	const confirmation = document.getElementById('deleteConfirmation').value;
	const errorMessage = document.getElementById('delete-error-message');
	const currentPassword = document.getElementById('confirmDeletePassword').value;
	
	// Validate confirmation text
	if (confirmation !== 'EXCLUIR') {
		errorMessage.textContent = 'Digite "EXCLUIR" para confirmar a exclusão da conta';
		setTimeout(() => {
			errorMessage.textContent = '';
		}, 3000);
		return;
	}
	
	console.log("Confirmando exclusão...");
	
	// Get modal elements for loading state
	const deleteButton = document.querySelector('#deleteModal .btn-danger');
	const modalContent = document.querySelector('#deleteModal .modal-content');
	const inputs = document.querySelectorAll('#deleteModal input');
	
	// Disable inputs and button
	inputs.forEach(input => input.disabled = true);
	deleteButton.disabled = true;
	const originalButtonText = deleteButton.textContent;
	deleteButton.classList.add('btn-loading');
	deleteButton.innerHTML = '<div class="spinner spinner-small"></div><span>Excluindo conta...</span>';
	
	// Add loading overlay to modal
	let overlay = document.createElement('div');
	overlay.className = 'loading-overlay';
	overlay.innerHTML = `
		<div class="spinner"></div>
		<div class="loading-overlay-text">Excluindo conta...</div>
	`;
	modalContent.style.position = 'relative';
	modalContent.appendChild(overlay);
	
	try {
		const response = await fetch('/api/deleteUser', {
			method: "POST",
			body: JSON.stringify({"currentPassword": currentPassword}),
			headers: {
				"Content-type": "application/json; charset=UTF-8"
			},
			credentials: 'same-origin'
		});
		
		// Remove loading overlay
		if (overlay) overlay.remove();
		
		let responseData;
		const contentType = response.headers.get("content-type");
		
		// Check if response is JSON
		if (contentType && contentType.includes("application/json")) {
			responseData = await response.json();
		} else {
			// If not JSON, get as text
			const textResponse = await response.text();
			responseData = { message: textResponse };
		}
		
		console.log("Delete response:", response.status, responseData);
		
		if (response.ok) {
			// Success - keep loading state while redirecting
			alert('Conta excluída com sucesso.');
			document.location.href = "/";
		} else {
			// Error - restore UI and show message
			deleteButton.disabled = false;
			deleteButton.classList.remove('btn-loading');
			deleteButton.textContent = originalButtonText;
			inputs.forEach(input => input.disabled = false);
			
			// Handle session expiration
			if (response.status === 401) {
				alert("Sessão expirada. Por favor refaça login.");
				document.location.href = "/";
				return;
			}
			
			// Show error message from backend
			errorMessage.textContent = responseData.message || 'Erro ao excluir a conta. Tente novamente.';
			setTimeout(() => {
				errorMessage.textContent = '';
			}, 3000);
		}
	} catch (error) {
		console.error("Error deleting account:", error);
		
		// Remove loading overlay
		if (overlay) overlay.remove();
		
		// Restore UI on error
		deleteButton.disabled = false;
		deleteButton.classList.remove('btn-loading');
		deleteButton.textContent = originalButtonText;
		inputs.forEach(input => input.disabled = false);
		
		errorMessage.textContent = 'Erro ao conectar com o servidor. Tente novamente.';
		setTimeout(() => {
			errorMessage.textContent = '';
		}, 3000);
	}
}

// Close modals when clicking outside
window.onclick = function(event) {
    const modals = ['passwordModal', 'uploadModal', 'deleteModal'];
    modals.forEach(modalId => {
        const modal = document.getElementById(modalId);
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });
}


// Reset password form
function resetPasswordForm() {
    document.getElementById('currentPassword').value = "";
    document.getElementById('newPassword').value = "";
    document.getElementById('confirmNewPassword').value = "";
    document.getElementById('error-message').value = "";
}

function validatePassword(password) {
	// pelo menos 8 caracteres, um caixa alta, um caixa baixa, um número e um símbolo 
	let t = password.length;
	if(t < 8) return false;
	return true;
}

async function changePassword() {
    const currentPassword = document.getElementById('currentPassword').value;
    const newPassword = document.getElementById('newPassword').value;
    const confirmNewPassword = document.getElementById('confirmNewPassword').value;
    const errorMessage = document.getElementById('error-message');
	 
	 document.getElementById('current-password-error-message').textContent = '';
	 document.getElementById('new-password-error-message').textContent = '';
	 document.getElementById('confirm-password-error-message').textContent = '';
	 errorMessage.textContent = '';

    if (newPassword && newPassword !== confirmNewPassword) {
        errorMessage.textContent = 'As senhas não coincidem';
		  setTimeout(() => {
				document.getElementById('error-message').textContent = '';
		  }, 3000);
		  return;
    }
    
    if (!validatePassword(newPassword)) {
        errorMessage.textContent = 'A nova senha deve ter pelo menos 8 caracteres';
		  setTimeout(() => {
			document.getElementById('error-message').textContent = '';
		  }, 3000);
		  return;
    }
    
    // Get the submit button and modal content
    const submitButton = document.querySelector('#passwordModal .btn-primary');
    const modalContent = document.querySelector('#passwordModal .modal-content');
    
    // Disable form inputs
    const inputs = document.querySelectorAll('#passwordModal input');
    inputs.forEach(input => input.disabled = true);
    
    // Add loading state to button
    submitButton.disabled = true;
    const originalButtonText = submitButton.textContent;
    submitButton.classList.add('btn-loading');
    submitButton.innerHTML = '<div class="spinner spinner-small"></div><span>Alterando senha...</span>';
    
    // Add loading overlay to modal
    let overlay = document.createElement('div');
    overlay.className = 'loading-overlay';
    overlay.innerHTML = `
        <div class="spinner"></div>
        <div class="loading-overlay-text">Alterando senha...</div>
    `;
    modalContent.style.position = 'relative';
    modalContent.appendChild(overlay);
    
	 console.log("Changing password...");
    
    try {
        const response = await fetch('/api/changePassword', {
            method: "POST",
            body: JSON.stringify({"currentPassword": currentPassword, "newPassword": newPassword}),
            headers: {
                "Content-type": "application/json; charset=UTF-8"
            },
            credentials: 'same-origin'
        });
        
        // Remove loading overlay
        if (overlay) overlay.remove();
        
        let responseData;
        const contentType = response.headers.get("content-type");
        
        // Check if response is JSON
        if (contentType && contentType.includes("application/json")) {
            responseData = await response.json();
        } else {
            // If not JSON, get as text
            const textResponse = await response.text();
            responseData = { message: textResponse };
        }
        
        console.log("Response:", response.status, responseData);
        
        if (response.ok) {
            // Success
            alert("Senha alterada com sucesso.");
            resetPasswordForm();
            closePasswordModal();
            
            // Restore UI state after closing
            submitButton.disabled = false;
            submitButton.classList.remove('btn-loading');
            submitButton.textContent = originalButtonText;
            inputs.forEach(input => input.disabled = false);
        } else {
            // Error - restore UI and show backend message
            submitButton.disabled = false;
            submitButton.classList.remove('btn-loading');
            submitButton.textContent = originalButtonText;
            inputs.forEach(input => input.disabled = false);
            
            // Handle session expiration
            if (response.status === 401) {
                alert("Sessão expirada. Por favor refaça login.");
                document.location.href = "/";
                return;
            }
            
            // Show error message from backend
            errorMessage.textContent = responseData.message || 'Erro ao alterar senha. Tente novamente.';
        }
    } catch (error) {
        console.error("Error changing password:", error);
        
        // Remove loading overlay
        if (overlay) overlay.remove();
        
        // Restore UI on error
        submitButton.disabled = false;
        submitButton.classList.remove('btn-loading');
        submitButton.textContent = originalButtonText;
        inputs.forEach(input => input.disabled = false);
        
        errorMessage.textContent = 'Erro ao conectar com o servidor. Tente novamente.';
    }
}


document.addEventListener('DOMContentLoaded', async function() {
	// carrega as informações do usuário
	const newPassword = document.getElementById('newPassword');
	const confirmNewPassword = document.getElementById('confirmNewPassword');
	let backendError = document.getElementById('backend-error-message');
	if(newPassword && confirmNewPassword){
	
		if(backendError){
			setTimeout(() => {
				backendError.textContent = '';
			}, 3000);
		}

		newPassword.addEventListener('blur', function() {
			if (!validatePassword(newPassword.value)) {
				document.getElementById('new-password-error-message').textContent = 'Senha deve ter pelo menos 8';
			} else {
				document.getElementById('new-password-error-message').textContent = '';
			}
		});

		confirmNewPassword.addEventListener('blur', function() {
			if (confirmNewPassword.value !== newPassword.value) {
				document.getElementById('confirm-password-error-message').textContent = 'As senhas não coincidem';
			} else {
				document.getElementById('confirm-password-error-message').textContent = '';
			}
		});
	}
	try {
		 const res = await fetch('/api/me', { credentials: 'same-origin' });
		 if (res.status === 401) {
			 window.location.href = '/login?error=notAuthenticated';
			 return;
        }
        if (!res.ok) {
			  console.error('Failed to load user', res.status);
			  return;
			}
        const user = await res.json();

        const email = document.getElementById('user-email');
        const matricula = document.getElementById('user-matricula');
        const initials = document.getElementById('user-initials');

        if (email) email.textContent = user.email || '';
        if (matricula) matricula.textContent = user.matricula || '';

        // Set initials from email (first 2 characters before @)
        if (initials && user.email) {
            var localPart = user.email.split("@")[0];
            var init = localPart.length >= 2 
                ? localPart.substring(0, 2).toUpperCase() 
                : localPart.substring(0, 1).toUpperCase();
            initials.textContent = init || 'U';
        }
    } catch (e) {
        console.error('Error loading user:', e);
    }
});