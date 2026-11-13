const API_URL = '/api';
let usuarioActual = null;
let catalogoGlobal = [];
let subtotalCotizacionActual = 0;

window.addEventListener('DOMContentLoaded', async () => {
    try {
        const res = await fetch(`${API_URL}/auth/me`, { credentials: 'same-origin' });
        if (!res.ok) {
            window.location.href = '/index.html';
            return;
        }
        usuarioActual = await res.json();
        configurarInterfazPorRol();
    } catch (err) {
        console.error('Error al verificar sesión:', err);
        window.location.href = '/index.html';
    }
});

function mostrarAlerta(mensaje, esError = false) {
    const el = document.getElementById('app-alert');
    if (!el) return;
    el.textContent = mensaje;
    el.className = esError ? 'alert alert-danger' : 'alert alert-success';
    el.classList.remove('hidden');
    setTimeout(() => el.classList.add('hidden'), 5000);
}

function formatearCLP(valor) {
    return '$' + Number(valor || 0).toLocaleString('es-CL') + ' CLP';
}

function configurarInterfazPorRol() {
    const rol = usuarioActual.rol || 'Comprador';
    const emp = usuarioActual.empresa || {};

    document.getElementById('info-empresa').textContent = emp.razonSocial || 'Empresa';
    document.getElementById('info-rut').textContent = `RUT: ${emp.rutEmpresa || '-'}`;
    document.getElementById('info-usuario').textContent = `${usuarioActual.nombreCompleto || 'Usuario'} · ${rol}`;

    document.getElementById('menu-compras').classList.toggle('hidden', rol !== 'Administrador' && rol !== 'Comprador');
    document.getElementById('menu-ventas').classList.toggle('hidden', rol !== 'Administrador' && rol !== 'Vendedor');
    document.getElementById('menu-admin').classList.toggle('hidden', rol !== 'Administrador');

    cargarCategorias();
    cargarDashboard();
}

window.cerrarSesion = async function () {
    await fetch(`${API_URL}/auth/logout`, { method: 'POST', credentials: 'same-origin' });
    window.location.href = '/index.html';
};

window.cambiarModulo = function (modulo, btnElement) {
    document.querySelectorAll('.modulo').forEach(s => s.classList.add('hidden'));
    const seccion = document.getElementById(`mod-${modulo}`);
    if (seccion) seccion.classList.remove('hidden');

    if (btnElement) {
        document.querySelectorAll('.nav-link').forEach(b => b.classList.remove('active'));
        btnElement.classList.add('active');
    }

    const titulos = {
        'dashboard': 'Resumen Operativo y Financiero',
        'explorar': 'Catálogo Mayorista de Proveedores',
        'mis-pedidos': 'Seguimiento de Órdenes de Compra',
        'gestionar-catalogo': 'Administración de Inventario y Catálogo',
        'pedidos-recibidos': 'Gestión de Solicitudes y Despachos',
        'estado-cuenta': 'Liquidaciones Mensuales y Comisiones',
        'usuarios': 'Equipo de Trabajo y Permisos de Acceso',
        'auditoria': 'Historial de Auditoría de Precios'
    };
    const tituloEl = document.getElementById('titulo-modulo');
    if (tituloEl && titulos[modulo]) tituloEl.textContent = titulos[modulo];

    if (modulo === 'dashboard') cargarDashboard();
    if (modulo === 'explorar') cargarCatalogoActivo();
    if (modulo === 'mis-pedidos') cargarMisPedidos();
    if (modulo === 'gestionar-catalogo') cargarMiCatalogo();
    if (modulo === 'pedidos-recibidos') cargarPedidosRecibidos();
    if (modulo === 'estado-cuenta') cargarEstadoCuenta();
    if (modulo === 'usuarios') cargarUsuariosEmpresa();
    if (modulo === 'auditoria') cargarAuditoria();
};

async function cargarDashboard() {
    try {
        const emp = usuarioActual.empresa || {};
        document.getElementById('resumen-empresa-datos').textContent =
            `${emp.razonSocial || '-'} | RUT: ${emp.rutEmpresa || '-'} | Giro: ${emp.giro || '-'} | Casa Matriz: ${emp.direccion || '-'} (${emp.region || '-'})`;

        const [resCobros, resCompras, resVentas] = await Promise.all([
            fetch(`${API_URL}/pedidos/mi-estado-cuenta`),
            fetch(`${API_URL}/pedidos/mis-compras`),
            fetch(`${API_URL}/pedidos/mis-ventas`)
        ]);
        const cobros = resCobros.ok ? await resCobros.json() : [];
        const compras = resCompras.ok ? await resCompras.json() : [];
        const ventas = resVentas.ok ? await resVentas.json() : [];

        const sumaVentas = cobros.reduce((acc, c) => acc + Number(c.subtotalProductos || 0), 0);
        const sumaComision = cobros.reduce((acc, c) => acc + Number(c.comision || 0), 0);

        document.getElementById('kpi-ventas').textContent = formatearCLP(sumaVentas);
        document.getElementById('kpi-pedidos').textContent = `${compras.length + ventas.length} Operaciones (${ventas.length} ventas · ${compras.length} compras)`;
        document.getElementById('kpi-comision').textContent = formatearCLP(sumaComision);
        document.getElementById('top-comision').textContent = formatearCLP(sumaComision);
    } catch (e) {
        console.error('Error cargando dashboard:', e);
    }
}

async function cargarCategorias() {
    try {
        const res = await fetch(`${API_URL}/catalogo/categorias`);
        if (!res.ok) return;
        const categorias = await res.json();
        const selFiltro = document.getElementById('filtro-categoria');
        const selForm = document.getElementById('prod-categoria');

        if (selFiltro) selFiltro.innerHTML = '<option value="">Todos los rubros industriales</option>';
        if (selForm) selForm.innerHTML = '';
        categorias.forEach(c => {
            if (selFiltro) selFiltro.innerHTML += `<option value="${c.idCategoria}">${c.nombreCategoria}</option>`;
            if (selForm) selForm.innerHTML += `<option value="${c.idCategoria}">${c.nombreCategoria}</option>`;
        });
    } catch (e) {
        console.error('Error cargando categorías:', e);
    }
}

async function cargarCatalogoActivo() {
    const res = await fetch(`${API_URL}/catalogo/activos`);
    catalogoGlobal = res.ok ? await res.json() : [];
    window.renderizarCatalogo();
}

window.renderizarCatalogo = function () {
    const texto = (document.getElementById('filtro-texto').value || '').toLowerCase();
    const cat = document.getElementById('filtro-categoria').value;
    const contenedor = document.getElementById('grid-catalogo');
    contenedor.innerHTML = '';

    catalogoGlobal
        .filter(p => (!cat || String(p.categoria.idCategoria) === cat) &&
                     (p.nombreProducto.toLowerCase().includes(texto) || p.sku.toLowerCase().includes(texto)))
        .forEach(p => {
            contenedor.innerHTML += `
                <div class="product-item">
                    <small class="badge badge-info">${p.sku} · ${p.categoria.nombreCategoria}</small>
                    <h4 style="margin-top:8px;">${p.nombreProducto}</h4>
                    <small>Proveedor: ${p.empresaVendedora.razonSocial}</small>
                    <div class="product-price">${formatearCLP(p.precioUnitario)} <small style="font-size:0.75rem; font-weight:400;">neto c/u</small></div>
                    <p style="font-size:0.85rem; margin-bottom:10px;">Disponibilidad: <strong>${p.stockDisponible} unidades</strong></p>
                    <button class="btn btn-primary btn-sm full-width" onclick="prepararSolicitud(${p.idProducto}, '${p.nombreProducto.replace(/'/g, "\\'")}', ${p.precioUnitario})">Cotizar Pedido</button>
                </div>
            `;
        });
};

window.prepararSolicitud = function (idProd, nombre, precio) {
    document.getElementById('ped-id-producto').value = idProd;
    document.getElementById('ped-nombre-prod').value = nombre;
    document.getElementById('ped-precio-fijo').value = precio;
    document.getElementById('ped-cantidad').value = 10;
    document.getElementById('card-solicitar-pedido').classList.remove('hidden');
};

window.emitirPedido = async function (e) {
    e.preventDefault();
    const payload = {
        idProducto: document.getElementById('ped-id-producto').value,
        cantidad: document.getElementById('ped-cantidad').value
    };
    const res = await fetch(`${API_URL}/pedidos`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) return mostrarAlerta(data.error, true);

    mostrarAlerta('Solicitud enviada al proveedor para cotización de despacho.');
    document.getElementById('card-solicitar-pedido').classList.add('hidden');
    window.cambiarModulo('mis-pedidos');
};

async function cargarMiCatalogo() {
    const res = await fetch(`${API_URL}/catalogo/mi-inventario`);
    const productos = res.ok ? await res.json() : [];
    const tbody = document.getElementById('tabla-mi-catalogo');
    tbody.innerHTML = '';

    productos.forEach(p => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${p.sku}</strong></td>
            <td>${p.nombreProducto}</td>
            <td>${p.categoria.nombreCategoria}</td>
            <td>${formatearCLP(p.precioUnitario)}</td>
            <td>${p.stockDisponible} un.</td>
            <td><span class="badge ${p.visible ? 'badge-success' : 'badge-warn'}">${p.visible ? 'Publicado' : 'Borrador'}</span></td>
            <td><button class="btn btn-primary btn-sm">Modificar</button></td>
        `;
        tr.querySelector('button').addEventListener('click', () => window.cargarProductoEnForm(p));
        tbody.appendChild(tr);
    });
}

window.cargarProductoEnForm = function (p) {
    document.getElementById('prod-id').value = p.idProducto;
    document.getElementById('prod-sku').value = p.sku;
    document.getElementById('prod-sku').readOnly = true;
    document.getElementById('prod-nombre').value = p.nombreProducto;
    document.getElementById('prod-categoria').value = p.categoria.idCategoria;
    document.getElementById('prod-precio').value = p.precioUnitario;
    document.getElementById('prod-stock').value = p.stockDisponible;
    document.getElementById('prod-stock').disabled = false;
    document.getElementById('prod-visible').value = String(p.visible);
    document.getElementById('prod-visible').disabled = false;
};

window.limpiarFormProducto = function () {
    document.getElementById('form-producto').reset();
    document.getElementById('prod-id').value = '';
    document.getElementById('prod-sku').readOnly = false;
    document.getElementById('prod-stock').value = 0;
    document.getElementById('prod-stock').disabled = true;
    document.getElementById('prod-visible').value = 'false';
    document.getElementById('prod-visible').disabled = true;
};

window.guardarProducto = async function (e) {
    e.preventDefault();
    const idProd = document.getElementById('prod-id').value;
    const payload = {
        idCategoria: document.getElementById('prod-categoria').value,
        sku: document.getElementById('prod-sku').value.trim(),
        nombreProducto: document.getElementById('prod-nombre').value.trim(),
        precioUnitario: document.getElementById('prod-precio').value
    };

    if (idProd) {
        payload.stockDisponible = document.getElementById('prod-stock').value;
        payload.visible = document.getElementById('prod-visible').value === 'true';
    }

    const url = idProd ? `${API_URL}/catalogo/productos/${idProd}` : `${API_URL}/catalogo/productos`;
    const metodo = idProd ? 'PUT' : 'POST';

    const res = await fetch(url, {
        method: metodo,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) return mostrarAlerta(data.error, true);

    mostrarAlerta(idProd ? 'Insumo actualizado en el catálogo.' : 'Insumo creado en estado Borrador con stock 0. Selecciónalo en la tabla para asignarle stock y publicarlo.');
    window.limpiarFormProducto();
    cargarMiCatalogo();
};

async function cargarMisPedidos() {
    const res = await fetch(`${API_URL}/pedidos/mis-compras`);
    const lista = res.ok ? await res.json() : [];
    const tbody = document.getElementById('tabla-mis-pedidos');
    tbody.innerHTML = '';

    lista.forEach(p => {
        const det = (p.detalles && p.detalles[0]) ? p.detalles[0] : {};
        const prodTexto = det.nombreProducto ? `${det.nombreProducto} (${det.cantidad} un.)` : '-';
        let acciones = '<span class="text-muted">En espera</span>';

        if (p.estadoPedido === 'COTIZANDO_ENVIO' && p.medioTransporte) {
            acciones = `
                <button class="btn btn-success btn-sm" onclick="responderCotizacion(${p.idPedido}, true)">Aprobar Compra</button>
                <button class="btn btn-danger btn-sm" onclick="responderCotizacion(${p.idPedido}, false)">Descartar</button>
            `;
        } else if (p.estadoPedido === 'EN_TRANSITO') {
            acciones = `<button class="btn btn-primary btn-sm" onclick="confirmarRecepcion(${p.idPedido})">Confirmar Recepción</button>`;
        } else if (p.estadoPedido === 'FINALIZADO') {
            acciones = `<span class="badge badge-success">Concluido</span>`;
        }

        tbody.innerHTML += `
            <tr>
                <td><strong>#${p.idPedido}</strong></td>
                <td>${p.empresaVendedora.razonSocial}</td>
                <td>${prodTexto}</td>
                <td>${p.medioTransporte || 'Pendiente de cotizar'}</td>
                <td>${formatearCLP(p.subtotalProductos)} + ${formatearCLP(p.costoEnvio)}</td>
                <td>${p.codigoSeguimiento || '-'}</td>
                <td><span class="badge badge-info">${p.estadoPedido}</span></td>
                <td>${acciones}</td>
            </tr>
        `;
    });
}

async function cargarPedidosRecibidos() {
    const res = await fetch(`${API_URL}/pedidos/mis-ventas`);
    const lista = res.ok ? await res.json() : [];
    const tbody = document.getElementById('tabla-pedidos-recibidos');
    tbody.innerHTML = '';

    lista.forEach(p => {
        const det = (p.detalles && p.detalles[0]) ? p.detalles[0] : {};
        const prodTexto = det.nombreProducto ? `${det.nombreProducto} (${det.cantidad} un.)` : '-';
        let boton = '<span class="text-muted">Sin acciones pendientes</span>';

        if (p.estadoPedido === 'COTIZANDO_ENVIO') {
            boton = `<button class="btn btn-primary btn-sm" onclick="abrirCotizador(${p.idPedido}, ${p.subtotalProductos})">Valorizar Despacho</button>`;
        } else if (p.estadoPedido === 'COMPRA_CONFIRMADA') {
            boton = `<button class="btn btn-success btn-sm" onclick="ingresarTracking(${p.idPedido})">Informar Despacho</button>`;
        }

        tbody.innerHTML += `
            <tr>
                <td><strong>#${p.idPedido}</strong></td>
                <td>${p.empresaCompradora.razonSocial}</td>
                <td>${prodTexto}</td>
                <td>${formatearCLP(p.subtotalProductos)}</td>
                <td>${formatearCLP(p.costoEnvio)}</td>
                <td><span class="badge badge-warn">${p.estadoPedido}</span></td>
                <td>${p.codigoSeguimiento || 'Sin asignar'}</td>
                <td>${boton}</td>
            </tr>
        `;
    });
}

window.abrirCotizador = function (idPedido, subtotal) {
    subtotalCotizacionActual = Number(subtotal);
    document.getElementById('cot-id-pedido').value = idPedido;
    document.getElementById('cot-id-label').textContent = idPedido;
    document.getElementById('cot-costo').value = 35000;
    window.actualizarSimulador();
    document.getElementById('card-cotizar-envio').classList.remove('hidden');
};

window.actualizarSimulador = function () {
    const envio = Number(document.getElementById('cot-costo').value || 0);
    const comision = Math.round(subtotalCotizacionActual * 0.03);
    const netoVendedor = subtotalCotizacionActual - comision + envio;
    document.getElementById('simulador-resumen').innerHTML =
        `Venta Productos: <strong>${formatearCLP(subtotalCotizacionActual)}</strong> · Comisión (3%): <strong style="color:#dc2626">-${formatearCLP(comision)}</strong> · Recibes Neto: <strong style="color:#047857">${formatearCLP(netoVendedor)}</strong>`;
};

window.enviarCotizacion = async function (e) {
    e.preventDefault();
    const idPedido = document.getElementById('cot-id-pedido').value;
    const payload = {
        medioTransporte: document.getElementById('cot-transporte').value,
        costoEnvio: document.getElementById('cot-costo').value
    };
    const res = await fetch(`${API_URL}/pedidos/${idPedido}/cotizar`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) return mostrarAlerta(data.error, true);

    mostrarAlerta('Propuesta de despacho enviada al comprador.');
    document.getElementById('card-cotizar-envio').classList.add('hidden');
    cargarPedidosRecibidos();
};

window.responderCotizacion = async function (idPedido, aprobar) {
    const res = await fetch(`${API_URL}/pedidos/${idPedido}/respuesta`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ aprobar })
    });
    const data = await res.json();
    if (!res.ok) return mostrarAlerta(data.error, true);

    mostrarAlerta(aprobar ? 'Compra aprobada: inventario actualizado y documento de cobro emitido.' : 'Cotización descartada.');
    cargarMisPedidos();
};

window.ingresarTracking = async function (idPedido) {
    const codigo = prompt('Ingresa el número de orden de transporte o seguimiento (ej: STK-994821):', 'STK-994821');
    if (!codigo) return;
    const res = await fetch(`${API_URL}/pedidos/${idPedido}/seguimiento`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ codigoSeguimiento: codigo })
    });
    const data = await res.json();
    if (!res.ok) return mostrarAlerta(data.error, true);

    mostrarAlerta('Despacho informado. La orden pasó a estado EN_TRANSITO.');
    cargarPedidosRecibidos();
};

window.confirmarRecepcion = async function (idPedido) {
    const res = await fetch(`${API_URL}/pedidos/${idPedido}/recepcion`, { method: 'PUT' });
    const data = await res.json();
    if (!res.ok) return mostrarAlerta(data.error, true);

    mostrarAlerta('Recepción confirmada satisfactoriamente. Operación finalizada.');
    cargarMisPedidos();
};

async function cargarEstadoCuenta() {
    const res = await fetch(`${API_URL}/pedidos/mi-estado-cuenta`);
    const cobros = res.ok ? await res.json() : [];
    const tbody = document.getElementById('tabla-estado-cuenta');
    tbody.innerHTML = '';

    cobros.forEach(c => {
        tbody.innerHTML += `
            <tr>
                <td>#${c.idDocumento}</td>
                <td>Orden #${c.idPedido}</td>
                <td>${c.fechaEmision ? c.fechaEmision.replace('T', ' ').substring(0, 16) : '-'}</td>
                <td>${formatearCLP(c.subtotalProductos)}</td>
                <td>${formatearCLP(c.montoEnvio)}</td>
                <td style="color:#dc2626; font-weight:700;">-${formatearCLP(c.comision)}</td>
                <td><strong>${formatearCLP(c.totalOperacion)}</strong></td>
                <td><span class="badge badge-success">${c.estadoCobro}</span></td>
            </tr>
        `;
    });
}

async function cargarUsuariosEmpresa() {
    const res = await fetch(`${API_URL}/usuarios/mi-empresa`);
    const usuarios = res.ok ? await res.json() : [];
    const tbody = document.getElementById('tabla-usuarios');
    tbody.innerHTML = '';

    usuarios.forEach(u => {
        tbody.innerHTML += `
            <tr>
                <td><strong>${u.nombreCompleto}</strong></td>
                <td>${u.rutUsuario}</td>
                <td>${u.email}</td>
                <td><span class="badge badge-info">${u.rol}</span></td>
                <td><span class="badge ${u.activo ? 'badge-success' : 'badge-danger'}">${u.activo ? 'Habilitado' : 'Suspendido'}</span></td>
                <td>
                    <button class="btn btn-outline btn-sm" onclick="cambiarEstadoUsuario(${u.idUsuario}, ${!u.activo})">${u.activo ? 'Suspender Acceso' : 'Reactivar'}</button>
                </td>
            </tr>
        `;
    });
}

window.crearUsuarioPyme = async function (e) {
    e.preventDefault();
    const rutUsuario = document.getElementById('usu-rut').value.trim();
    const email = document.getElementById('usu-email').value.trim();

    if (typeof validarRutChileno === 'function' && !validarRutChileno(rutUsuario)) {
        return mostrarAlerta('El RUT ingresado no es válido. Verifica el dígito verificador.', true);
    }
    if (typeof validarFormatoCorreo === 'function' && !validarFormatoCorreo(email)) {
        return mostrarAlerta('Ingresa un correo corporativo válido con @ y dominio (.cl, .com, etc.).', true);
    }

    const payload = {
        nombreCompleto: document.getElementById('usu-nombre').value.trim(),
        rutUsuario,
        email,
        nombreRol: document.getElementById('usu-rol').value,
        password: document.getElementById('usu-pass').value
    };
    const res = await fetch(`${API_URL}/usuarios`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) return mostrarAlerta(data.error, true);

    mostrarAlerta('Colaborador incorporado correctamente a la empresa.');
    document.getElementById('form-nuevo-usuario').reset();
    cargarUsuariosEmpresa();
};

window.cambiarEstadoUsuario = async function (idUsuario, nuevoEstado) {
    const res = await fetch(`${API_URL}/usuarios/${idUsuario}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ activo: nuevoEstado })
    });
    if (!res.ok) {
        const data = await res.json();
        return mostrarAlerta(data.error, true);
    }
    cargarUsuariosEmpresa();
};

async function cargarAuditoria() {
    const res = await fetch(`${API_URL}/catalogo/auditoria`);
    const logs = res.ok ? await res.json() : [];
    const tbody = document.getElementById('tabla-auditoria');
    tbody.innerHTML = '';

    logs.forEach(l => {
        tbody.innerHTML += `
            <tr>
                <td><strong>#AUD-${l.idLog}</strong></td>
                <td>${l.fechaModificacion ? l.fechaModificacion.replace('T', ' ').substring(0, 16) : '-'}</td>
                <td>${l.sku} — ${l.nombreProducto}</td>
                <td>${formatearCLP(l.precioAnterior)}</td>
                <td style="color:#0284c7; font-weight:700;">${formatearCLP(l.precioNuevo)}</td>
                <td>${l.nombreUsuario}</td>
            </tr>
        `;
    });
}