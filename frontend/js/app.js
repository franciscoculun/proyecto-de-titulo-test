const API_URL = '/api';
let usuarioActual = null;
let catalogoGlobal = [];
let miInventarioGlobal = [];
let vistaInventarioActual = 'lista';
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
    document.getElementById('info-usuario').textContent = `${usuarioActual.nombreCompleto || 'Usuario'} (${rol})`;

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

    const rutas = {
        'dashboard': ['AndesB2B › Principal', 'Panel General — Resumen Operativo y Financiero'],
        'explorar': ['A. Módulo de Compras › a. Explorar catálogo', 'Explorar Catálogo de Proveedores B2B'],
        'mis-pedidos': ['A. Módulo de Compras › b. Mis pedidos', 'Mis Pedidos — Órdenes de Compra Emitidas'],
        'gestionar-catalogo': ['B. Módulo de Ventas › a. Gestionar catálogo', 'Gestionar Catálogo e Inventario de Productos'],
        'pedidos-recibidos': ['B. Módulo de Ventas › b. Pedidos recibidos', 'Pedidos Recibidos y Cotización de Envío'],
        'estado-cuenta': ['B. Módulo de Ventas › c. Estado de cuenta', 'Estado de Cuenta Mensual y Comisión (3%)'],
        'datos-empresa': ['C. Administración › a. Datos empresa', 'Datos Comerciales y Tributarios de la Empresa'],
        'usuarios': ['C. Administración › b. Gestionar usuarios', 'Gestionar Usuarios y Permisos (RBAC)'],
        'auditoria': ['C. Administración › c. Auditoría de precios', 'Auditoría de Precios (Log_Auditoria_Precios)']
    };

    if (rutas[modulo]) {
        document.getElementById('breadcrumb-modulo').textContent = rutas[modulo][0];
        document.getElementById('titulo-modulo').textContent = rutas[modulo][1];
    }

    if (modulo === 'dashboard') cargarDashboard();
    if (modulo === 'explorar') cargarCatalogoActivo();
    if (modulo === 'mis-pedidos') cargarMisPedidos();
    if (modulo === 'gestionar-catalogo') cargarMiCatalogo();
    if (modulo === 'pedidos-recibidos') cargarPedidosRecibidos();
    if (modulo === 'estado-cuenta') cargarEstadoCuenta();
    if (modulo === 'datos-empresa') cargarDatosEmpresa();
    if (modulo === 'usuarios') cargarUsuariosEmpresa();
    if (modulo === 'auditoria') cargarAuditoria();
};

// 0. PANEL GENERAL (CON TABLA DE MOVIMIENTOS Y ALERTAS COMO EN EL PROTOTIPO)
async function cargarDashboard() {
    try {
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
        document.getElementById('kpi-pedidos').textContent = `${compras.length + ventas.length} Pedidos`;
        document.getElementById('kpi-pedidos-detalle').textContent = `${ventas.length} ventas recibidas · ${compras.length} compras emitidas`;
        document.getElementById('kpi-comision').textContent = formatearCLP(sumaComision);
        document.getElementById('top-comision').textContent = formatearCLP(sumaComision);

        // Renderizar Últimos Movimientos
        const tbodyMov = document.getElementById('tabla-ultimos-movimientos');
        tbodyMov.innerHTML = '';
        const combinados = [
            ...ventas.map(v => ({ ...v, tipoOp: 'Venta B2B', contraparte: v.empresaCompradora.razonSocial })),
            ...compras.map(c => ({ ...c, tipoOp: 'Compra B2B', contraparte: c.empresaVendedora.razonSocial }))
        ].slice(0, 6);

        if (combinados.length === 0) {
            tbodyMov.innerHTML = '<tr><td colspan="5" class="text-muted">Aún no se registran pedidos en la cuenta.</td></tr>';
        } else {
            combinados.forEach(m => {
                tbodyMov.innerHTML += `
                    <tr>
                        <td><strong>#${m.idPedido}</strong></td>
                        <td>${m.contraparte}</td>
                        <td><span class="badge ${m.tipoOp === 'Venta B2B' ? 'badge-success' : 'badge-info'}">${m.tipoOp}</span></td>
                        <td>${formatearCLP(m.subtotalProductos)}</td>
                        <td><span class="badge badge-warn">${m.estadoPedido}</span></td>
                    </tr>
                `;
            });
        }

        // Renderizar Alertas Operativas
        const contAlertas = document.getElementById('lista-alertas-dashboard');
        const porCotizar = ventas.filter(v => v.estadoPedido === 'COTIZANDO_ENVIO').length;
        const porConfirmar = compras.filter(c => c.estadoPedido === 'COTIZANDO_ENVIO' && c.medioTransporte).length;
        const enTransito = [...ventas, ...compras].filter(p => p.estadoPedido === 'EN_TRANSITO').length;

        contAlertas.innerHTML = `
            <div class="alert-item ${porCotizar > 0 ? 'warn' : 'ok'}">
                <strong>Pedidos recibidos por cotizar envío:</strong> ${porCotizar} solicitud(es) pendiente(s).
            </div>
            <div class="alert-item ${porConfirmar > 0 ? 'warn' : 'ok'}">
                <strong>Cotizaciones listas para confirmar compra:</strong> ${porConfirmar} orden(es).
            </div>
            <div class="alert-item">
                <strong>Despachos actualmente en tránsito:</strong> ${enTransito} envío(s) activo(s).
            </div>
            <div class="alert-item ok">
                <strong>Empresa conectada:</strong> ${usuarioActual.empresa.razonSocial} (${usuarioActual.empresa.rutEmpresa})
            </div>
        `;
    } catch (e) {
        console.error('Error cargando dashboard:', e);
    }
}

function cargarDatosEmpresa() {
    const emp = usuarioActual.empresa || {};
    document.getElementById('emp-razon').value = emp.razonSocial || '-';
    document.getElementById('emp-rut').value = emp.rutEmpresa || '-';
    document.getElementById('emp-giro').value = emp.giro || '-';
    document.getElementById('emp-region').value = emp.region || '-';
    document.getElementById('emp-direccion').value = emp.direccion || '-';
    document.getElementById('emp-admin').value = `${usuarioActual.nombreCompleto} (${usuarioActual.email}) — Rol: ${usuarioActual.rol}`;
}

async function cargarCategorias() {
    try {
        const res = await fetch(`${API_URL}/catalogo/categorias`);
        if (!res.ok) return;
        const categorias = await res.json();
        const selFiltro = document.getElementById('filtro-categoria');
        const selForm = document.getElementById('prod-categoria');

        if (selFiltro) selFiltro.innerHTML = '<option value="">Todas las categorías</option>';
        if (selForm) selForm.innerHTML = '';
        categorias.forEach(c => {
            if (selFiltro) selFiltro.innerHTML += `<option value="${c.idCategoria}">${c.nombreCategoria}</option>`;
            if (selForm) selForm.innerHTML += `<option value="${c.idCategoria}">${c.nombreCategoria}</option>`;
        });
    } catch (e) {
        console.error('Error cargando categorías:', e);
    }
}

// A.a EXPLORAR CATÁLOGO CON FILTRO DE REGIÓN Y ORDEN DE PRECIO
async function cargarCatalogoActivo() {
    const res = await fetch(`${API_URL}/catalogo/activos`);
    catalogoGlobal = res.ok ? await res.json() : [];
    window.renderizarCatalogo();
}

window.renderizarCatalogo = function () {
    const texto = (document.getElementById('filtro-texto').value || '').toLowerCase();
    const cat = document.getElementById('filtro-categoria').value;
    const region = document.getElementById('filtro-region').value;
    const orden = document.getElementById('filtro-orden').value;
    const contenedor = document.getElementById('grid-catalogo');
    contenedor.innerHTML = '';

    const filtrados = catalogoGlobal
        .filter(p => (!cat || String(p.categoria.idCategoria) === cat) &&
                     (!region || (p.empresaVendedora && p.empresaVendedora.region === region)) &&
                     (p.nombreProducto.toLowerCase().includes(texto) || p.sku.toLowerCase().includes(texto)))
        .sort((a, b) => orden === 'asc'
            ? Number(a.precioUnitario) - Number(b.precioUnitario)
            : Number(b.precioUnitario) - Number(a.precioUnitario));

    filtrados.forEach(p => {
        contenedor.innerHTML += `
            <div class="product-item">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <small class="badge badge-info">${p.sku}</small>
                    <small class="text-muted">📍 ${p.empresaVendedora.region || 'Chile'}</small>
                </div>
                <h4 style="margin-top:8px;">${p.nombreProducto}</h4>
                <small style="display:block; color:#475569;"> Categoría: ${p.categoria.nombreCategoria}</small>
                <small style="display:block; color:#0f172a; font-weight:600;">Proveedor: ${p.empresaVendedora.razonSocial}</small>
                <div class="product-price">${formatearCLP(p.precioUnitario)} <small style="font-size:0.75rem; font-weight:400;">neto</small></div>
                <p style="font-size:0.85rem; margin-bottom:10px;">Stock disponible: <strong>${p.stockDisponible} unidades</strong></p>
                <button class="btn btn-primary btn-sm full-width" onclick="prepararSolicitud(${p.idProducto}, '${p.nombreProducto.replace(/'/g, "\\'")}', ${p.precioUnitario})">Solicitar Pedido</button>
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

    mostrarAlerta('Pedido creado en estado COTIZANDO_ENVIO.');
    document.getElementById('card-solicitar-pedido').classList.add('hidden');
    window.cambiarModulo('mis-pedidos');
};

// B.a GESTIONAR CATÁLOGO CON INTERRUPTOR DESLIZANTE Y VISTA LISTA/CATÁLOGO
async function cargarMiCatalogo() {
    const res = await fetch(`${API_URL}/catalogo/mi-inventario`);
    miInventarioGlobal = res.ok ? await res.json() : [];
    renderizarMiInventario();
}

window.cambiarVistaInventario = function (vista) {
    vistaInventarioActual = vista;
    document.getElementById('contenedor-inv-lista').classList.toggle('hidden', vista !== 'lista');
    document.getElementById('contenedor-inv-tarjetas').classList.toggle('hidden', vista !== 'tarjetas');
    document.getElementById('btn-vista-lista').className = vista === 'lista' ? 'btn btn-sm btn-primary' : 'btn btn-sm btn-outline';
    document.getElementById('btn-vista-tarjetas').className = vista === 'tarjetas' ? 'btn btn-sm btn-primary' : 'btn btn-sm btn-outline';
    renderizarMiInventario();
};

function renderizarMiInventario() {
    const tbody = document.getElementById('tabla-mi-catalogo');
    const grid = document.getElementById('contenedor-inv-tarjetas');
    tbody.innerHTML = '';
    grid.innerHTML = '';

    miInventarioGlobal.forEach(p => {
        // Fila para Vista Lista con Toggle Switch
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${p.sku}</strong></td>
            <td>${p.nombreProducto}</td>
            <td>${p.categoria.nombreCategoria}</td>
            <td>${formatearCLP(p.precioUnitario)}</td>
            <td>${p.stockDisponible} un.</td>
            <td>
                <label class="switch">
                    <input type="checkbox" ${p.visible ? 'checked' : ''} onchange="alternarVisibilidadProducto(${p.idProducto}, this.checked)">
                    <span class="slider"></span>
                </label>
                <span class="badge ${p.visible ? 'badge-success' : 'badge-warn'}">${p.visible ? 'Visible' : 'Borrador'}</span>
            </td>
            <td><button class="btn btn-primary btn-sm">Editar</button></td>
        `;
        tr.querySelector('button').addEventListener('click', () => window.cargarProductoEnForm(p));
        tbody.appendChild(tr);

        // Tarjeta para Vista Catálogo
        const card = document.createElement('div');
        card.className = 'product-item';
        card.innerHTML = `
            <div style="display:flex; justify-content:space-between;">
                <small class="badge badge-info">${p.sku}</small>
                <span class="badge ${p.visible ? 'badge-success' : 'badge-warn'}">${p.visible ? 'Visible' : 'Borrador'}</span>
            </div>
            <h4 style="margin-top:8px;">${p.nombreProducto}</h4>
            <small>${p.categoria.nombreCategoria}</small>
            <div class="product-price">${formatearCLP(p.precioUnitario)}</div>
            <p style="font-size:0.85rem; margin-bottom:10px;">Stock: <strong>${p.stockDisponible} un.</strong></p>
            <button class="btn btn-primary btn-sm full-width">Editar Producto</button>
        `;
        card.querySelector('button').addEventListener('click', () => window.cargarProductoEnForm(p));
        grid.appendChild(card);
    });
}

window.alternarVisibilidadProducto = async function (idProducto, nuevoEstadoVisible) {
    const res = await fetch(`${API_URL}/catalogo/productos/${idProducto}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ visible: nuevoEstadoVisible })
    });
    const data = await res.json();
    if (!res.ok) {
        mostrarAlerta(data.error, true);
    } else {
        mostrarAlerta(`Estado de publicación cambiado a ${nuevoEstadoVisible ? 'Visible en Catálogo' : 'Borrador'}.`);
    }
    cargarMiCatalogo();
};

window.cargarProductoEnForm = function (p) {
    document.getElementById('prod-id').value = p.idProducto;
    document.getElementById('prod-sku').value = p.sku;
    document.getElementById('prod-sku').readOnly = true;
    document.getElementById('prod-nombre').value = p.nombreProducto;
    document.getElementById('prod-categoria').value = p.categoria.idCategoria;
    document.getElementById('prod-precio').value = p.precioUnitario;
    document.getElementById('prod-stock').value = p.stockDisponible;
    document.getElementById('prod-visible').value = String(p.visible);
};

window.limpiarFormProducto = function () {
    document.getElementById('form-producto').reset();
    document.getElementById('prod-id').value = '';
    document.getElementById('prod-sku').readOnly = false;
    document.getElementById('prod-stock').value = '';
    document.getElementById('prod-visible').value = 'false';
};

window.guardarProducto = async function (e) {
    e.preventDefault();
    const idProd = document.getElementById('prod-id').value;
    const stockInput = document.getElementById('prod-stock').value.trim();

    const payload = {
        idCategoria: document.getElementById('prod-categoria').value,
        sku: document.getElementById('prod-sku').value.trim(),
        nombreProducto: document.getElementById('prod-nombre').value.trim(),
        precioUnitario: document.getElementById('prod-precio').value,
        stockDisponible: stockInput === '' ? 0 : stockInput,
        visible: document.getElementById('prod-visible').value === 'true'
    };

    const url = idProd ? `${API_URL}/catalogo/productos/${idProd}` : `${API_URL}/catalogo/productos`;
    const metodo = idProd ? 'PUT' : 'POST';

    const res = await fetch(url, {
        method: metodo,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
    const data = await res.json();
    if (!res.ok) return mostrarAlerta(data.error, true);

    mostrarAlerta(idProd ? 'Producto actualizado correctamente.' : 'Producto registrado en el inventario.');
    window.limpiarFormProducto();
    cargarMiCatalogo();
};

// A.b y B.b FLUJO DE PEDIDOS Y 4 CAJAS FINANCIERAS
async function cargarMisPedidos() {
    const res = await fetch(`${API_URL}/pedidos/mis-compras`);
    const lista = res.ok ? await res.json() : [];
    const tbody = document.getElementById('tabla-mis-pedidos');
    tbody.innerHTML = '';

    lista.forEach(p => {
        const det = (p.detalles && p.detalles[0]) ? p.detalles[0] : {};
        const prodTexto = det.nombreProducto ? `${det.nombreProducto} (${det.cantidad} un.)` : '-';
        let acciones = '<span class="text-muted">Esperando cotización</span>';

        if (p.estadoPedido === 'COTIZANDO_ENVIO' && p.medioTransporte) {
            acciones = `
                <button class="btn btn-success btn-sm" onclick="responderCotizacion(${p.idPedido}, true)">Aceptar y Confirmar</button>
                <button class="btn btn-danger btn-sm" onclick="responderCotizacion(${p.idPedido}, false)">Rechazar</button>
            `;
        } else if (p.estadoPedido === 'EN_TRANSITO') {
            acciones = `<button class="btn btn-primary btn-sm" onclick="confirmarRecepcion(${p.idPedido})">Confirmar Recepción</button>`;
        } else if (p.estadoPedido === 'FINALIZADO') {
            acciones = `<span class="badge badge-success">Finalizado</span>`;
        }

        tbody.innerHTML += `
            <tr>
                <td><strong>#${p.idPedido}</strong></td>
                <td>${p.empresaVendedora.razonSocial}</td>
                <td>${prodTexto}</td>
                <td>${p.medioTransporte || 'Por asignar'}</td>
                <td>${formatearCLP(p.subtotalProductos)}</td>
                <td>${formatearCLP(p.costoEnvio)}</td>
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
        let boton = '<span class="text-muted">Sin acción pendiente</span>';

        if (p.estadoPedido === 'COTIZANDO_ENVIO') {
            boton = `<button class="btn btn-primary btn-sm" onclick="abrirCotizador(${p.idPedido}, ${p.subtotalProductos})">Cotizar Envío</button>`;
        } else if (p.estadoPedido === 'COMPRA_CONFIRMADA') {
            boton = `<button class="btn btn-success btn-sm" onclick="ingresarTracking(${p.idPedido})">Registrar Seguimiento</button>`;
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

    document.getElementById('sim-subtotal').textContent = formatearCLP(subtotalCotizacionActual);
    document.getElementById('sim-comision').textContent = '-' + formatearCLP(comision);
    document.getElementById('sim-envio').textContent = formatearCLP(envio);
    document.getElementById('sim-neto').textContent = formatearCLP(netoVendedor);
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

    mostrarAlerta('Cotización de envío registrada y enviada al comprador.');
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

    mostrarAlerta(aprobar ? 'Compra confirmada: stock descontado y Documento de Cobro (3%) emitido.' : 'Cotización rechazada.');
    cargarMisPedidos();
};

window.ingresarTracking = async function (idPedido) {
    const codigo = prompt('Ingresa el código de seguimiento del transportista (ej: STK-994821):', 'STK-994821');
    if (!codigo) return;
    const res = await fetch(`${API_URL}/pedidos/${idPedido}/seguimiento`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ codigoSeguimiento: codigo })
    });
    const data = await res.json();
    if (!res.ok) return mostrarAlerta(data.error, true);

    mostrarAlerta('Seguimiento registrado. El pedido pasó a estado EN_TRANSITO.');
    cargarPedidosRecibidos();
};

window.confirmarRecepcion = async function (idPedido) {
    const res = await fetch(`${API_URL}/pedidos/${idPedido}/recepcion`, { method: 'PUT' });
    const data = await res.json();
    if (!res.ok) return mostrarAlerta(data.error, true);

    mostrarAlerta('Recepción conforme confirmada. Pedido FINALIZADO.');
    cargarMisPedidos();
};

// B.c ESTADO DE CUENTA CON LAS 4 CAJAS SUPERIORES
async function cargarEstadoCuenta() {
    const res = await fetch(`${API_URL}/pedidos/mi-estado-cuenta`);
    const cobros = res.ok ? await res.json() : [];
    const tbody = document.getElementById('tabla-estado-cuenta');
    tbody.innerHTML = '';

    let acumSubtotal = 0, acumEnvio = 0, acumComision = 0;

    cobros.forEach(c => {
        acumSubtotal += Number(c.subtotalProductos || 0);
        acumEnvio += Number(c.montoEnvio || 0);
        acumComision += Number(c.comision || 0);

        tbody.innerHTML += `
            <tr>
                <td><strong>#DOC-${c.idDocumento}</strong></td>
                <td>Pedido #${c.idPedido}</td>
                <td>${c.fechaEmision ? c.fechaEmision.replace('T', ' ').substring(0, 16) : '-'}</td>
                <td>${formatearCLP(c.subtotalProductos)}</td>
                <td>${formatearCLP(c.montoEnvio)}</td>
                <td style="color:#dc2626; font-weight:700;">-${formatearCLP(c.comision)}</td>
                <td><strong>${formatearCLP(c.totalOperacion)}</strong></td>
                <td><span class="badge badge-success">${c.estadoCobro}</span></td>
            </tr>
        `;
    });

    document.getElementById('ec-subtotal').textContent = formatearCLP(acumSubtotal);
    document.getElementById('ec-envio').textContent = formatearCLP(acumEnvio);
    document.getElementById('ec-comision').textContent = '-' + formatearCLP(acumComision);
    document.getElementById('ec-neto').textContent = formatearCLP(acumSubtotal - acumComision + acumEnvio);
}

// C.b GESTIONAR USUARIOS (CON SELECTOR DE ROL Y PROTECCIÓN DE SESIÓN ACTUAL)
async function cargarUsuariosEmpresa() {
    const res = await fetch(`${API_URL}/usuarios/mi-empresa`);
    const usuarios = res.ok ? await res.json() : [];
    const tbody = document.getElementById('tabla-usuarios');
    tbody.innerHTML = '';

    usuarios.forEach(u => {
        const esMiCuenta = u.idUsuario === usuarioActual.idUsuario;
        const controles = esMiCuenta
            ? '<span class="badge badge-info">Sesión Actual</span>'
            : `<button class="btn btn-outline btn-sm" onclick="cambiarEstadoUsuario(${u.idUsuario}, ${!u.activo})">${u.activo ? 'Desactivar' : 'Activar'}</button>`;

        tbody.innerHTML += `
            <tr>
                <td><strong>${u.nombreCompleto}</strong></td>
                <td>${u.rutUsuario}</td>
                <td>${u.email}</td>
                <td><span class="badge badge-info">${u.rol}</span></td>
                <td><span class="badge ${u.activo ? 'badge-success' : 'badge-danger'}">${u.activo ? 'Activo' : 'Inactivo'}</span></td>
                <td>${controles}</td>
            </tr>
        `;
    });
}

window.crearUsuarioPyme = async function (e) {
    e.preventDefault();
    const rutUsuario = document.getElementById('usu-rut').value.trim();
    const email = document.getElementById('usu-email').value.trim();

    if (typeof validarRutChileno === 'function' && !validarRutChileno(rutUsuario)) {
        return mostrarAlerta('El RUT ingresado no es válido (Módulo 11).', true);
    }
    if (typeof validarFormatoCorreo === 'function' && !validarFormatoCorreo(email)) {
        return mostrarAlerta('Ingresa un correo electrónico válido con @ y dominio (.cl, .com, etc.).', true);
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

    mostrarAlerta('Usuario añadido a la nómina de la empresa.');
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

// C.c AUDITORÍA DE PRECIOS CON CÁLCULO DE VARIACIÓN
async function cargarAuditoria() {
    const res = await fetch(`${API_URL}/catalogo/auditoria`);
    const logs = res.ok ? await res.json() : [];
    const tbody = document.getElementById('tabla-auditoria');
    tbody.innerHTML = '';

    logs.forEach(l => {
        const dif = Number(l.precioNuevo) - Number(l.precioAnterior);
        const signo = dif >= 0 ? '+' : '';
        tbody.innerHTML += `
            <tr>
                <td><strong>#LOG-${l.idLog}</strong></td>
                <td>${l.fechaModificacion ? l.fechaModificacion.replace('T', ' ').substring(0, 16) : '-'}</td>
                <td><strong>${l.sku}</strong> — ${l.nombreProducto}</td>
                <td>${formatearCLP(l.precioAnterior)}</td>
                <td style="color:#0284c7; font-weight:700;">${formatearCLP(l.precioNuevo)}</td>
                <td><span class="badge ${dif >= 0 ? 'badge-info' : 'badge-warn'}">${signo}${formatearCLP(dif)}</span></td>
                <td>${l.nombreUsuario}</td>
            </tr>
        `;
    });
}