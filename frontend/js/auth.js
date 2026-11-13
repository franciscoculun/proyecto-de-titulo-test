const API_URL = '/api';

function mostrarAlertaPublica(mensaje, esError = true) {
    const el = document.getElementById('public-alert');
    if (!el) return;
    el.textContent = mensaje;
    el.className = esError ? 'alert alert-danger' : 'alert alert-success';
    el.classList.remove('hidden');
}

function AutocompletarLogin(email) {
    document.getElementById('login-email').value = email;
    document.getElementById('login-password').value = 'Andes2026!';
}

async function iniciarSesion(e) {
    e.preventDefault();
    const email = document.getElementById('login-email').value.trim();
    const password = document.getElementById('login-password').value;

    if (!validarFormatoCorreo(email)) {
        return mostrarAlertaPublica('Por favor, ingresa un correo corporativo válido (ej: contacto@empresa.cl).', true);
    }
    if (!password) {
        return mostrarAlertaPublica('Debes ingresar tu contraseña de acceso.', true);
    }

    const res = await fetch(`${API_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
    });
    const data = await res.json();
    if (!res.ok) return mostrarAlertaPublica(data.error || 'No fue posible iniciar sesión.', true);

    sessionStorage.setItem('usuarioAndesB2B', JSON.stringify(data));
    window.location.href = '/pantallas/panel.html';
}

async function registrarEmpresa(e) {
    e.preventDefault();
    const rutEmpresa = document.getElementById('reg-rut-empresa').value.trim();
    const rutUsuario = document.getElementById('reg-rut-admin').value.trim();
    const email = document.getElementById('reg-email-admin').value.trim();
    const password = document.getElementById('reg-pass-admin').value;

    if (!validarRutChileno(rutEmpresa)) {
        return mostrarAlertaPublica('El RUT de la empresa no es válido. Revisa el dígito verificador.', true);
    }
    if (!validarRutChileno(rutUsuario)) {
        return mostrarAlertaPublica('El RUT del representante no es válido. Revisa el dígito verificador.', true);
    }
    if (!validarFormatoCorreo(email)) {
        return mostrarAlertaPublica('Ingresa un correo corporativo válido con @ y extensión (.cl, .com, etc.).', true);
    }
    if (!password || password.length < 6) {
        return mostrarAlertaPublica('La contraseña debe contener al menos 6 caracteres.', true);
    }

    const payload = {
        rutEmpresa,
        razonSocial: document.getElementById('reg-razon-social').value.trim(),
        giro: document.getElementById('reg-giro').value.trim(),
        region: document.getElementById('reg-region').value,
        direccion: document.getElementById('reg-direccion').value.trim(),
        nombreCompleto: document.getElementById('reg-nombre-admin').value.trim(),
        rutUsuario,
        email,
        password
    };

    const res = await fetch(`${API_URL}/auth/registro-empresa`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) return mostrarAlertaPublica(data.error, true);

    mostrarAlertaPublica('Cuenta empresarial creada con éxito. Redirigiendo al portal de acceso...', false);
    setTimeout(() => { window.location.href = '/index.html'; }, 1800);
}

async function solicitarToken(e) {
    e.preventDefault();
    const email = document.getElementById('rec-email').value.trim();

    if (!validarFormatoCorreo(email)) {
        return mostrarAlertaPublica('Ingresa un correo corporativo válido (ej: nombre@empresa.cl).', true);
    }

    const res = await fetch(`${API_URL}/auth/recuperar`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email })
    });
    const data = await res.json();
    if (!res.ok) return mostrarAlertaPublica(data.error, true);

    document.getElementById('form-restablecer').classList.remove('hidden');
    if (data.tokenDemo) {
        document.getElementById('rec-token').value = data.tokenDemo;
        mostrarAlertaPublica(`${data.mensaje} (Modo Demo: ${data.tokenDemo})`, false);
    } else {
        document.getElementById('rec-token').value = '';
        mostrarAlertaPublica(data.mensaje, false);
    }
}

async function confirmarRestablecimiento(e) {
    e.preventDefault();
    const email = document.getElementById('rec-email').value.trim();
    const token = document.getElementById('rec-token').value.trim();
    const nuevaPassword = document.getElementById('rec-nueva-pass').value;

    if (nuevaPassword.length < 6) {
        return mostrarAlertaPublica('La nueva contraseña debe tener al menos 6 caracteres.', true);
    }

    const res = await fetch(`${API_URL}/auth/restablecer`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, token, nuevaPassword })
    });
    const data = await res.json();
    if (!res.ok) return mostrarAlertaPublica(data.error, true);

    mostrarAlertaPublica('Tu contraseña ha sido actualizada. Redirigiendo al inicio de sesión...', false);
    setTimeout(() => { window.location.href = '/index.html'; }, 1800);
}