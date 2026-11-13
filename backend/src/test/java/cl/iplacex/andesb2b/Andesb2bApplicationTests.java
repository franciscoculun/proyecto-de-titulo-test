package cl.iplacex.andesb2b;

import cl.iplacex.andesb2b.config.UsuarioPrincipal;
import cl.iplacex.andesb2b.model.*;
import cl.iplacex.andesb2b.repository.*;
import cl.iplacex.andesb2b.service.AuthUsuarioService;
import cl.iplacex.andesb2b.service.PedidoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Andesb2bApplicationTests {

    @Mock private PedidoRepository pedidoRepo;
    @Mock private DetallePedidoRepository detalleRepo;
    @Mock private ProductoRepository productoRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private DocumentoCobroRepository cobroRepo;
    @Mock private EmpresaRepository empresaRepo;
    @Mock private RolRepository rolRepo;

    private PedidoService pedidoService;
    private AuthUsuarioService authService;

    @BeforeEach
    void setUp() {
        pedidoService = new PedidoService(pedidoRepo, detalleRepo, productoRepo, usuarioRepo, cobroRepo);
        authService = new AuthUsuarioService(usuarioRepo, empresaRepo, rolRepo, new BCryptPasswordEncoder());
    }

    @Test
    void validaRutModulo11YFormatoCorreo() {
        assertTrue(authService.esRutValido("76.543.210-3"));
        assertTrue(authService.esRutValido("11.111.111-1"));
        assertFalse(authService.esRutValido("76.543.210-9"));

        assertTrue(authService.esEmailValido("pfuentes@ferreteriaaustral.cl"));
        assertFalse(authService.esEmailValido("correo-sin-dominio"));
    }

    @Test
    void confirmarCompraDescuentaStockCalculaComision3PorcientoYEvitaDobleLlamada() {
        Empresa empCompradora = new Empresa();
        empCompradora.setIdEmpresa(2);
        Empresa empVendedora = new Empresa();
        empVendedora.setIdEmpresa(1);

        Producto prod = new Producto();
        prod.setIdProducto(10);
        prod.setSku("SKU-101");
        prod.setNombreProducto("Perfil Acero");
        prod.setPrecioUnitario(new BigDecimal("10000.00"));
        prod.setStockDisponible(100);
        prod.setEmpresaVendedora(empVendedora);

        Pedido pedido = new Pedido();
        pedido.setIdPedido(50);
        pedido.setEmpresaCompradora(empCompradora);
        pedido.setEmpresaVendedora(empVendedora);
        pedido.setEstadoPedido("COTIZANDO_ENVIO");
        pedido.setMedioTransporte("Starken Cargo");
        pedido.setCostoEnvio(new BigDecimal("35000.00"));

        DetallePedido det = new DetallePedido();
        det.setIdDetalle(1);
        det.setPedido(pedido);
        det.setProducto(prod);
        det.setCantidad(10);
        det.setPrecioUnitarioFijo(new BigDecimal("10000.00"));

        when(pedidoRepo.findById(50)).thenReturn(Optional.of(pedido));
        when(detalleRepo.findByPedidoIdPedido(50)).thenReturn(List.of(det));
        when(productoRepo.findByIdForUpdate(10)).thenReturn(Optional.of(prod));

        UsuarioPrincipal sesionComprador = new UsuarioPrincipal(3, 2, "msoto@constructorasur.cl", "María Soto", "Comprador");

        // 1ra llamada: Confirma la compra, descuenta 10 unidades (stock queda en 90) y calcula 3% sobre $100.000 ($3.000)
        pedidoService.responderCotizacion(sesionComprador, 50, true);

        assertEquals(90, prod.getStockDisponible());
        assertEquals("COMPRA_CONFIRMADA", pedido.getEstadoPedido());

        ArgumentCaptor< DocumentoCobro > docCaptor = ArgumentCaptor.forClass(DocumentoCobro.class);
        verify(cobroRepo, times(1)).save(docCaptor.capture());
        assertEquals(new BigDecimal("100000.00"), docCaptor.getValue().getSubtotalProductos());
        assertEquals(new BigDecimal("3000.00"), docCaptor.getValue().getComision());
        assertEquals(new BigDecimal("135000.00"), docCaptor.getValue().getTotalOperacion());

        // 2da llamada idéntica a /respuesta: Debe ser bloqueada sin volver a descontar stock
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                pedidoService.responderCotizacion(sesionComprador, 50, true)
        );
        assertTrue(ex.getMessage().contains("ya fue procesada"));
        assertEquals(90, prod.getStockDisponible());
    }

    @Test
    void impidePasarAEnTransitoSinCompraConfirmada() {
        Empresa empVendedora = new Empresa();
        empVendedora.setIdEmpresa(1);

        Pedido pedido = new Pedido();
        pedido.setIdPedido(51);
        pedido.setEmpresaVendedora(empVendedora);
        pedido.setEstadoPedido("COTIZANDO_ENVIO");

        when(pedidoRepo.findById(51)).thenReturn(Optional.of(pedido));
        UsuarioPrincipal sesionVendedor = new UsuarioPrincipal(2, 1, "cgomez@ferreteriaaustral.cl", "Carlos Gómez", "Vendedor");

        assertThrows(IllegalArgumentException.class, () ->
                pedidoService.registrarSeguimiento(sesionVendedor, 51, "STK-999")
        );
    }

    @Test
    void impideAutocompraDeLaMismaEmpresa() {
        Empresa emp1 = new Empresa();
        emp1.setIdEmpresa(1);

        Usuario u = new Usuario();
        u.setIdUsuario(1);
        u.setEmpresa(emp1);

        Producto prod = new Producto();
        prod.setIdProducto(10);
        prod.setEmpresaVendedora(emp1);
        prod.setVisible(true);
        prod.setStockDisponible(50);

        when(usuarioRepo.findById(1)).thenReturn(Optional.of(u));
        when(productoRepo.findById(10)).thenReturn(Optional.of(prod));

        UsuarioPrincipal sesionAdmin = new UsuarioPrincipal(1, 1, "pfuentes@ferreteriaaustral.cl", "Pedro Fuentes", "Administrador");

        assertThrows(IllegalArgumentException.class, () ->
                pedidoService.crearPedido(sesionAdmin, Map.of("idProducto", 10, "cantidad", 5))
        );
    }
}