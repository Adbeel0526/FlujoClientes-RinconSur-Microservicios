package cl.duocuc.rinconsur.operations;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record Dashboard(
    int totalSesiones,
    int programadas,
    int enCurso,
    int finalizadas,
    int canceladas,
    @NotNull Conteo conteo,
    @NotNull List<Estadistica> porAcceso,
    @NotNull List<Estadistica> porObservador,
    Integer usuariosActivos,
    Integer observadores,
    Integer supervisores,
    Integer accesosActivos,
    @NotNull List<Sesion> actividad
) {}
