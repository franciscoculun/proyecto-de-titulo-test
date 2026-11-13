package cl.iplacex.andesb2b.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

public class Dtos {

    public record EmpresaDto(
            Integer idEmpresa,
            String rutEmpresa,
            String razonSocial,
            String giro,
            String region,
            String direccion
    ) {
        public static EmpresaDto from(Empresa e) {
            if (e == null) return null;
            return new EmpresaDto(e.getIdEmpresa(), e.getRutEmpresa(), e.getRazonSocial(),
                    e.getGiro(), e.getRegion(), e.getDireccion());
        }
    }

    public record UsuarioDto(
            Integer idUsuario,
            String rutUsuario,
            String nombreCompleto,
            String email,
            String rol,
            Boolean activo,
            EmpresaDto empresa
    ) {
        public static UsuarioDto from(Usuario u) {
            if (u == null) return null;
            return new UsuarioDto(
                    u.getIdUsuario(),
                    u.getRutUsuario(),
                    u.getNombreCompleto(),
                    u.getEmail(),
                    u.getRol() != null ? u.getRol().getNombreRol() : null,
                    u.getActivo(),
                    EmpresaDto.from(u.getEmpresa())
            );
        }
    }

    public record CategoriaDto(
            Integer idCategoria,
            String nombreCategoria,
            String descripcionRubro
    ) {
        public static CategoriaDto from(Categoria c) {
            if (c == null) return null;
            return new CategoriaDto(c.getIdCategoria(), c.getNombreCategoria(), c.getDescripcionRubro());
        }
    }

    public record ProductoDto(
            Integer idProducto,
            String sku,
            String nombreProducto,
            BigDecimal precioUnitario,
            Integer stockDisponible,
            Boolean visible,
            CategoriaDto categoria,
            EmpresaDto empresaVendedora
    ) {
        public static ProductoDto from(Producto p) {
            if (p == null) return null;
            return new ProductoDto(
                    p.getIdProducto(),
                    p.getSku(),
                    p.getNombreProducto(),
                    p.getPrecioUnitario(),
                    p.getStockDisponible(),
                    p.getVisible(),
                    CategoriaDto.from(p.getCategoria()),
                    EmpresaDto.from(p.getEmpresaVendedora())
            );
        }
    }

    public record DetallePedidoDto(
            Integer idDetalle,
            Integer idProducto,
            String sku,
            String nombreProducto,
            Integer cantidad,
            BigDecimal precioUnitarioFijo,
            BigDecimal subtotalLinea
    ) {
        public static DetallePedidoDto from(DetallePedido d) {
            BigDecimal sub = d.getSubtotalLinea() != null
                    ? d.getSubtotalLinea()
                    : d.getPrecioUnitarioFijo().multiply(BigDecimal.valueOf(d.getCantidad()));
            return new DetallePedidoDto(
                    d.getIdDetalle(),
                    d.getProducto().getIdProducto(),
                    d.getProducto().getSku(),
                    d.getProducto().getNombreProducto(),
                    d.getCantidad(),
                    d.getPrecioUnitarioFijo(),
                    sub
            );
        }
    }

    public record PedidoResumenDto(
            Integer idPedido,
            EmpresaDto empresaCompradora,
            EmpresaDto empresaVendedora,
            String nombreComprador,
            String estadoPedido,
            String medioTransporte,
            BigDecimal costoEnvio,
            String codigoSeguimiento,
            LocalDateTime fechaCreacion,
            List< DetallePedidoDto > detalles,
            BigDecimal subtotalProductos,
            BigDecimal comisionProyectada
    ) {
        public static PedidoResumenDto from(Pedido p, List< DetallePedido > lineas) {
            List< DetallePedidoDto > detDtos = lineas.stream().map(DetallePedidoDto::from).toList();
            BigDecimal subtotal = detDtos.stream()
                    .map(DetallePedidoDto::subtotalLinea)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal comision = subtotal.multiply(new BigDecimal("0.03")).setScale(2, RoundingMode.HALF_UP);

            return new PedidoResumenDto(
                    p.getIdPedido(),
                    EmpresaDto.from(p.getEmpresaCompradora()),
                    EmpresaDto.from(p.getEmpresaVendedora()),
                    p.getUsuarioComprador() != null ? p.getUsuarioComprador().getNombreCompleto() : "-",
                    p.getEstadoPedido(),
                    p.getMedioTransporte(),
                    p.getCostoEnvio(),
                    p.getCodigoSeguimiento(),
                    p.getFechaCreacion(),
                    detDtos,
                    subtotal,
                    comision
            );
        }
    }

    public record DocumentoCobroDto(
            Integer idDocumento,
            Integer idPedido,
            BigDecimal subtotalProductos,
            BigDecimal montoEnvio,
            BigDecimal comision,
            BigDecimal totalOperacion,
            String estadoCobro,
            LocalDateTime fechaEmision
    ) {
        public static DocumentoCobroDto from(DocumentoCobro d) {
            return new DocumentoCobroDto(
                    d.getIdDocumento(),
                    d.getPedido().getIdPedido(),
                    d.getSubtotalProductos(),
                    d.getMontoEnvio(),
                    d.getComision(),
                    d.getTotalOperacion(),
                    d.getEstadoCobro(),
                    d.getFechaEmision()
            );
        }
    }

    public record LogAuditoriaDto(
            Integer idLog,
            String sku,
            String nombreProducto,
            BigDecimal precioAnterior,
            BigDecimal precioNuevo,
            String nombreUsuario,
            LocalDateTime fechaModificacion
    ) {
        public static LogAuditoriaDto from(LogAuditoriaPrecio l) {
            return new LogAuditoriaDto(
                    l.getIdLog(),
                    l.getProducto().getSku(),
                    l.getProducto().getNombreProducto(),
                    l.getPrecioAnterior(),
                    l.getPrecioNuevo(),
                    l.getUsuario() != null ? l.getUsuario().getNombreCompleto() : "Sistema DB",
                    l.getFechaModificacion()
            );
        }
    }
}