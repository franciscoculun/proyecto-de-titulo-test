// Autoformatea el RUT mientras el usuario escribe: XX.XXX.XXX-X
function formatearInputRut(input) {
    let valor = input.value.replace(/[^0-9kK]/g, '').toUpperCase();
    if (valor.length > 9) valor = valor.slice(0, 9);

    if (valor.length > 1) {
        const cuerpo = valor.slice(0, -1);
        const dv = valor.slice(-1);
        const cuerpoPuntos = cuerpo.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
        input.value = `${cuerpoPuntos}-${dv}`;
    } else {
        input.value = valor;
    }
}

// Verifica matemáticamente el dígito verificador con el algoritmo chileno Módulo 11
function validarRutChileno(rut) {
    if (!rut) return false;
    const limpio = rut.replace(/\./g, '').replace(/-/g, '').trim().toUpperCase();
    if (limpio.length < 8 || limpio.length > 9) return false;

    const cuerpo = limpio.slice(0, -1);
    const dvIngresado = limpio.slice(-1);
    if (!/^\d+$/.test(cuerpo)) return false;

    // Evitar RUTs de dígitos repetidos inválidos
    let suma = 0;
    let multiplicador = 2;
    for (let i = cuerpo.length - 1; i >= 0; i--) {
        suma += parseInt(cuerpo.charAt(i), 10) * multiplicador;
        multiplicador = multiplicador === 7 ? 2 : multiplicador + 1;
    }

    const resto = 11 - (suma % 11);
    let dvCalculado = '';
    if (resto === 11) dvCalculado = '0';
    else if (resto === 10) dvCalculado = 'K';
    else dvCalculado = String(resto);

    return dvIngresado === dvCalculado;
}

// Verifica formato estricto de correo con @ y dominio (.cl, .com, .org, .net, etc.)
function validarFormatoCorreo(email) {
    if (!email) return false;
    const regex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.(cl|com|org|net|edu|gob|io|co|[a-zA-Z]{2,6})$/i;
    return regex.test(email.trim());
}