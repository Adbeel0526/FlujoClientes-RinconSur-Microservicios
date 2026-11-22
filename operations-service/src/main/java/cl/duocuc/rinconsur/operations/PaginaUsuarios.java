package cl.duocuc.rinconsur.operations;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PaginaUsuarios(
    @NotNull List<Usuario> usuarios,
    int totalPaginas
) {}
