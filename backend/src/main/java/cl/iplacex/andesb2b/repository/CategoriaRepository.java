package cl.iplacex.andesb2b.repository;

import cl.iplacex.andesb2b.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {
}