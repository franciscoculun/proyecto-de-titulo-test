-- ========================================================
-- ANDESB2B - SCRIPT DE BASE DE DATOS
-- ========================================================

DROP TABLE IF EXISTS Documentos_Cobro, Detalle_Pedidos, Pedidos, Log_Auditoria_Precios, Productos, Categorias, Usuarios, Roles, Empresas CASCADE;

-- 1. CREACION DE TABLAS
CREATE TABLE Empresas (
    id_empresa SERIAL PRIMARY KEY,
    rut_empresa VARCHAR(12) UNIQUE NOT NULL,
    razon_social VARCHAR(150) NOT NULL,
    giro VARCHAR(100),
    region VARCHAR(50),
    direccion VARCHAR(200),
    fecha_registro TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Roles (
    id_rol SERIAL PRIMARY KEY,
    nombre_rol VARCHAR(50) UNIQUE NOT NULL,
    descripcion VARCHAR(200)
);

CREATE TABLE Categorias (
    id_categoria SERIAL PRIMARY KEY,
    nombre_categoria VARCHAR(100) UNIQUE NOT NULL,
    descripcion_rubro VARCHAR(200)
);

CREATE TABLE Usuarios (
    id_usuario SERIAL PRIMARY KEY,
    id_empresa INT NOT NULL REFERENCES Empresas(id_empresa),
    id_rol INT NOT NULL REFERENCES Roles(id_rol),
    rut_usuario VARCHAR(12) UNIQUE NOT NULL,
    nombre_completo VARCHAR(150) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    token_recuperacion VARCHAR(255),
    activo BOOLEAN DEFAULT TRUE
);

CREATE TABLE Productos (
    id_producto SERIAL PRIMARY KEY,
    id_empresa_vendedora INT NOT NULL REFERENCES Empresas(id_empresa),
    id_categoria INT NOT NULL REFERENCES Categorias(id_categoria),
    sku VARCHAR(50) UNIQUE NOT NULL,
    nombre_producto VARCHAR(150) NOT NULL,
    precio_unitario NUMERIC(12, 2) NOT NULL CHECK (precio_unitario >= 0),
    stock_disponible INT NOT NULL CHECK (stock_disponible >= 0),
    visible BOOLEAN DEFAULT TRUE
);

CREATE TABLE Pedidos (
    id_pedido SERIAL PRIMARY KEY,
    id_empresa_compradora INT NOT NULL REFERENCES Empresas(id_empresa),
    id_empresa_vendedora INT NOT NULL REFERENCES Empresas(id_empresa),
    id_usuario_comprador INT NOT NULL REFERENCES Usuarios(id_usuario),
    estado_pedido VARCHAR(50) DEFAULT 'CREADO',
    medio_transporte VARCHAR(100),
    costo_envio NUMERIC(12, 2) DEFAULT 0,
    codigo_seguimiento VARCHAR(100),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_empresas_distintas CHECK (id_empresa_vendedora != id_empresa_compradora)
);

CREATE TABLE Detalle_Pedidos (
    id_detalle SERIAL PRIMARY KEY,
    id_pedido INT NOT NULL REFERENCES Pedidos(id_pedido),
    id_producto INT NOT NULL REFERENCES Productos(id_producto),
    cantidad INT NOT NULL CHECK (cantidad > 0),
    precio_unitario_fijo NUMERIC(12, 2) NOT NULL CHECK (precio_unitario_fijo >= 0),
    subtotal_linea NUMERIC(12, 2) GENERATED ALWAYS AS (cantidad * precio_unitario_fijo) STORED
);

CREATE TABLE Log_Auditoria_Precios (
    id_log SERIAL PRIMARY KEY,
    id_producto INT NOT NULL REFERENCES Productos(id_producto),
    id_usuario INT REFERENCES Usuarios(id_usuario),
    precio_anterior NUMERIC(12, 2) NOT NULL,
    precio_nuevo NUMERIC(12, 2) NOT NULL,
    fecha_modificacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE Documentos_Cobro (
    id_documento SERIAL PRIMARY KEY,
    id_pedido INT UNIQUE NOT NULL REFERENCES Pedidos(id_pedido),
    subtotal_productos NUMERIC(12, 2) NOT NULL,
    monto_envio NUMERIC(12, 2) DEFAULT 0,
    comision NUMERIC(12, 2) NOT NULL,
    total_operacion NUMERIC(12, 2) NOT NULL,
    estado_cobro VARCHAR(50) DEFAULT 'PENDIENTE_LIQUIDACION',
    fecha_emision TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================================================
-- 2. TRIGGER DE AUDITORÍA
-- ========================================================
CREATE OR REPLACE FUNCTION fn_auditar_precio()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.precio_unitario <> OLD.precio_unitario THEN
        INSERT INTO Log_Auditoria_Precios (id_producto, precio_anterior, precio_nuevo)
        VALUES (NEW.id_producto, OLD.precio_unitario, NEW.precio_unitario);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_auditoria_precio
AFTER UPDATE OF precio_unitario ON Productos
FOR EACH ROW
EXECUTE FUNCTION fn_auditar_precio();

-- ========================================================
-- 3. POBLAMIENTO DE DATOS 
-- ========================================================

INSERT INTO Roles (nombre_rol, descripcion) VALUES 
('Administrador', 'Control total'), 
('Vendedor', 'Gestiona catálogo y ventas'), 
('Comprador', 'Realiza pedidos');

INSERT INTO Categorias (nombre_categoria, descripcion_rubro) VALUES 
('Herramientas Eléctricas', 'Taladros, sierras'), 
('Materiales Generales', 'Insumos básicos'), 
('Metalurgia', 'Perfiles y vigas');
