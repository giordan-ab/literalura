package com.example.literalura.model;

import jakarta.persistence.*;

@Entity
@Table(name = "libros")
public class Libro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String titulo;
    @ManyToOne (cascade = CascadeType.MERGE)
    private Autor autor;
    private String idioma;
    private String genero;

    public Libro() {} // Obligatorio para JPA

    // Este constructor recibe el Record de la API y lo pasa a la Entidad
    public Libro(DatosLibro datosLibro) {
        this.titulo = datosLibro.titulo();
        this.idioma = datosLibro.idioma().get(0); // Tomamos el primer idioma
        // Important: We assign the first author from the API result
        if (!datosLibro.autor().isEmpty()) {
            this.autor = new Autor(datosLibro.autor().get(0));
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Autor getAutor() {
        return autor;
    }

    public void setAutor(Autor autor) {
        this.autor = autor;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    @Override
    public String toString() {
        return "---------------- LIBRO ----------------" + "\n" +
                " Título: " + titulo + "\n" +
                " Autor: " + (autor != null ? autor.getNombre() : "Desconocido") + "\n" +
                " Idioma: " + idioma + "\n" +
                " Género: " + genero + "\n" +
                "---------------------------------------";
    }
}
