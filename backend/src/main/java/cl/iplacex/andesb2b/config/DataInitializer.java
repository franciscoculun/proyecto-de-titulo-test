package cl.iplacex.andesb2b.config;

import cl.iplacex.andesb2b.model.*;
import cl.iplacex.andesb2b.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(
            RolRepository rolRepo,
            CategoriaRepository catRepo,
            EmpresaRepository empRepo,
            UsuarioRepository usuRepo,
            ProductoRepository prodRepo,
            PasswordEncoder encoder) {
        return args -> {
            Rol rolAdmin = rolRepo.findByNombreRol("Administrador").orElseGet(() -> {
                Rol r = new Rol();
                r.setNombreRol("Administrador");
                r.setDescripcion("Control total de la cuenta Pyme");
                return rolRepo.save(r);
            });
            Rol rolVendedor = rolRepo.findByNombreRol("Vendedor").orElseGet(() -> {
                Rol r = new Rol();
                r.setNombreRol("Vendedor");
                r.setDescripcion("Gestiona catálogo y ventas");
                return rolRepo.save(r);
            });
            Rol rolComprador = rolRepo.findByNombreRol("Comprador").orElseGet(() -> {
                Rol r = new Rol();
                r.setNombreRol("Comprador");
                r.setDescripcion("Explora catálogo y realiza pedidos");
                return rolRepo.save(r);
            });

            if (catRepo.count() == 0) {
                Categoria c1 = new Categoria();
                c1.setNombreCategoria("Herramientas Eléctricas");
                c1.setDescripcionRubro("Taladros, sierras y esmeriles");
                catRepo.save(c1);

                Categoria c2 = new Categoria();
                c2.setNombreCategoria("Materiales Generales");
                c2.setDescripcionRubro("Insumos básicos y fijaciones");
                catRepo.save(c2);

                Categoria c3 = new Categoria();
                c3.setNombreCategoria("Metalurgia");
                c3.setDescripcionRubro("Perfiles y vigas de acero");
                catRepo.save(c3);
            }

            // Actualizar RUTs previos si quedaron con dígito antiguo
            empRepo.findByRutEmpresa("76.543.210-K").ifPresent(e -> {
                e.setRutEmpresa("76.543.210-3");
                empRepo.save(e);
            });
            empRepo.findByRutEmpresa("77.888.999-1").ifPresent(e -> {
                e.setRutEmpresa("77.888.999-4");
                empRepo.save(e);
            });
            usuRepo.findByEmail("cgomez@ferreteriaaustral.cl").ifPresent(u -> {
                u.setRutUsuario("12.222.222-5");
                usuRepo.save(u);
            });
            usuRepo.findByEmail("msoto@constructorasur.cl").ifPresent(u -> {
                u.setRutUsuario("13.333.333-9");
                usuRepo.save(u);
            });

            if (!empRepo.existsByRutEmpresa("76.543.210-3")) {
                Empresa emp1 = new Empresa();
                emp1.setRutEmpresa("76.543.210-3");
                emp1.setRazonSocial("Ferretería Austral SpA");
                emp1.setGiro("Venta al por mayor de materiales de construcción y ferretería técnica");
                emp1.setRegion("Los Lagos");
                emp1.setDireccion("Av. Industrial 450, Puerto Montt");
                emp1 = empRepo.save(emp1);

                Empresa emp2 = new Empresa();
                emp2.setRutEmpresa("77.888.999-4");
                emp2.setRazonSocial("Constructora Sur Ltda.");
                emp2.setGiro("Obras civiles y montaje industrial");
                emp2.setRegion("Los Lagos");
                emp2.setDireccion("Ruta 5 Sur Km 1020, Puerto Varas");
                emp2 = empRepo.save(emp2);

                Usuario admin = new Usuario();
                admin.setEmpresa(emp1);
                admin.setRol(rolAdmin);
                admin.setRutUsuario("11.111.111-1");
                admin.setNombreCompleto("Pedro Fuentes");
                admin.setEmail("pfuentes@ferreteriaaustral.cl");
                admin.setPasswordHash(encoder.encode("Andes2026!"));
                admin.setActivo(true);
                usuRepo.save(admin);

                Usuario vendedor = new Usuario();
                vendedor.setEmpresa(emp1);
                vendedor.setRol(rolVendedor);
                vendedor.setRutUsuario("12.222.222-5");
                vendedor.setNombreCompleto("Carlos Gómez");
                vendedor.setEmail("cgomez@ferreteriaaustral.cl");
                vendedor.setPasswordHash(encoder.encode("Andes2026!"));
                vendedor.setActivo(true);
                usuRepo.save(vendedor);

                Usuario comprador = new Usuario();
                comprador.setEmpresa(emp2);
                comprador.setRol(rolComprador);
                comprador.setRutUsuario("13.333.333-9");
                comprador.setNombreCompleto("María Soto");
                comprador.setEmail("msoto@constructorasur.cl");
                comprador.setPasswordHash(encoder.encode("Andes2026!"));
                comprador.setActivo(true);
                usuRepo.save(comprador);

                Categoria catMetal = catRepo.findAll().get(0);
                Producto p1 = new Producto();
                p1.setEmpresaVendedora(emp1);
                p1.setCategoria(catMetal);
                p1.setSku("SKU-101");
                p1.setNombreProducto("Perfil Acero Galvanizado 40x40");
                p1.setPrecioUnitario(new BigDecimal("10000.00"));
                p1.setStockDisponible(150);
                p1.setVisible(true);
                prodRepo.save(p1);

                Producto p2 = new Producto();
                p2.setEmpresaVendedora(emp1);
                p2.setCategoria(catMetal);
                p2.setSku("SKU-118");
                p2.setNombreProducto("Disco de Corte Industrial 7\"");
                p2.setPrecioUnitario(new BigDecimal("2500.00"));
                p2.setStockDisponible(400);
                p2.setVisible(true);
                prodRepo.save(p2);

                Producto p3 = new Producto();
                p3.setEmpresaVendedora(emp2);
                p3.setCategoria(catMetal);
                p3.setSku("SKU-305");
                p3.setNombreProducto("Pernos de Anclaje M12 x 150");
                p3.setPrecioUnitario(new BigDecimal("900.00"));
                p3.setStockDisponible(500);
                p3.setVisible(true);
                prodRepo.save(p3);
            }
        };
    }
}