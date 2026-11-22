package cl.duocuc.rinconsur.user.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public record PaginaUsuariosResponse(
        @NotNull List<UsuarioResponse> usuarios,
        int pagina,
        int totalPaginas,
        long totalUsuarios) {
}
