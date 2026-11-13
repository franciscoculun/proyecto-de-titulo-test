package cl.iplacex.andesb2b.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "documentos_cobro")
public class DocumentoCobro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Integer idDocumento;

    @OneToOne
    @JoinColumn(name = "id_pedido", nullable = false, unique = true)
    private Pedido pedido;

    @Column(name = "subtotal_productos", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalProductos;

    @Column(name = "monto_envio", precision = 12, scale = 2)
    private BigDecimal montoEnvio = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal comision;

    @Column(name = "total_operacion", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalOperacion;

    @Column(name = "estado_cobro", length = 50)
    private String estadoCobro = "PENDIENTE_LIQUIDACION";

    @Column(name = "fecha_emision", insertable = false, updatable = false)
    private LocalDateTime fechaEmision;

    public Integer getIdDocumento() { return idDocumento; }
    public void setIdDocumento(Integer idDocumento) { this.idDocumento = idDocumento; }
    public Pedido getPedido() { return pedido; }
    public void setPedido(Pedido pedido) { this.pedido = pedido; }
    public BigDecimal getSubtotalProductos() { return subtotalProductos; }
    public void setSubtotalProductos(BigDecimal subtotalProductos) { this.subtotalProductos = subtotalProductos; }
    public BigDecimal getMontoEnvio() { return montoEnvio; }
    public void setMontoEnvio(BigDecimal montoEnvio) { this.montoEnvio = montoEnvio; }
    public BigDecimal getComision() { return comision; }
    public void setComision(BigDecimal comision) { this.comision = comision; }
    public BigDecimal getTotalOperacion() { return totalOperacion; }
    public void setTotalOperacion(BigDecimal totalOperacion) { this.totalOperacion = totalOperacion; }
    public String getEstadoCobro() { return estadoCobro; }
    public void setEstadoCobro(String estadoCobro) { this.estadoCobro = estadoCobro; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }
}