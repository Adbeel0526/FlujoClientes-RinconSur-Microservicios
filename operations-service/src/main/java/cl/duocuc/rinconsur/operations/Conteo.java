package cl.duocuc.rinconsur.operations;

public record Conteo(
    long transito,
    long visita,
    long noDeterminado
) {

    public Conteo() {
        this(0, 0, 0);
    }

    public long getTotal() {
        return transito + visita + noDeterminado;
    }

    public Conteo plus(Conteo otro) {
        return new Conteo(
                transito + otro.transito,
                visita + otro.visita,
                noDeterminado + otro.noDeterminado
        );
    }
}
