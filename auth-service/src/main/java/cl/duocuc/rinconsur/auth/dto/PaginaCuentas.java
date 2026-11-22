package cl.duocuc.rinconsur.auth.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record PaginaCuentas(
        @NotNull List<UsuarioResponse> usuarios,
        int pagina,
        int totalPaginas,
        long totalUsuarios) {
}
