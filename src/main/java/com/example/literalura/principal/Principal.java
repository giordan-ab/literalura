package com.example.literalura.principal;

import com.example.literalura.model.Autor;
import com.example.literalura.model.Datos;
import com.example.literalura.model.DatosLibro;
import com.example.literalura.model.Libro;
import com.example.literalura.repository.AutorRepository;
import com.example.literalura.repository.LibroRepository;
import com.example.literalura.service.ConsumoAPI;
import com.example.literalura.service.ConvierteDatos;
import com.example.literalura.service.IConvierteDatos;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Principal {
    private Scanner teclado = new Scanner(System.in);
    private ConsumoAPI consumoApi = new ConsumoAPI();
    private IConvierteDatos conversor = new ConvierteDatos();
    private final String URL_BASE = "https://gutendex.com/books/";
    private LibroRepository repositorio;
    private AutorRepository autorRepositorio;
    // Esta variable es para la Base de Datos (Clase Libro)
    Optional<Libro> libroBuscado;

    public Principal(LibroRepository repository, AutorRepository autorRepository) {
        this.repositorio = repository;
        this.autorRepositorio = autorRepository;
    }

    public void muestraElMenu() {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    \nElija la opción a través de su número:
                    1 - Buscar libro por título
                    2 - Listar libros registrados
                    3 - Listar autores registrados
                    4 - Listar autores vivos en un determinado año
                    5 - Listar libros por idioma
                    
                    0 - Salir
                    """;
            System.out.println(menu);
            opcion = teclado.nextInt();
            teclado.nextLine();

            switch (opcion) {
                case 1:
                    buscarlibrosPorTitulo();
                    break;
                case 2:
                    listarLibrosRegistrados();
                    break;
                case 3:
                    listarAutoresRegistrados();
                    break;
                case 4:
                    listarAutoresVivosEnDeterminadoAnio();
                    break;
                case 5:
                    listarLibrosPorIdioma();
                    break;
                case 0:
                    System.out.println("Saliendo...");
                    break;
                default:
                    System.out.println("Opción inválida");
            }
        }
    }

    public void buscarlibrosPorTitulo() {
        System.out.println("Escribe el nombre del libro que deseas buscar");
        var nombreLibro = teclado.nextLine();

        // 1. Buscamos en la API
        var json = consumoApi.obtenerDatos(URL_BASE + "?search=" + nombreLibro.replace(" ", "+"));
        System.out.println("JSON RECIBIDO: " + json);

        // 2. Convertimos a Record Datos
        var datosBusqueda = conversor.obtenerDatos(json, Datos.class);

        // 3. Filtramos el libro de la API
        Optional<DatosLibro> libroEncontrado = datosBusqueda.resultados().stream()
                .filter(l -> l.titulo().toUpperCase().contains(nombreLibro.toUpperCase()))
                .findFirst();

        if (libroEncontrado.isPresent()) {
            DatosLibro d = libroEncontrado.get();

            // --- VALIDACIÓN PARA EVITAR DUPLICADOS ---
            // Verificamos si el libro ya existe en nuestra base de datos
            Optional<Libro> libroExistente = repositorio.findByTituloContainsIgnoreCase(d.titulo());

            if (libroExistente.isPresent()) {
                System.out.println("No se puede registrar el mismo libro más de una vez.");
                return; // Finaliza el método aquí para no duplicar
            }
            // -----------------------------------------

            System.out.println("Libro encontrado en la API: " + d);

            // 4. Guardamos en la base de datos
            Libro libro = new Libro(d);

            // Lógica para el Autor (Evitar duplicar autores también)
            Autor autor = libro.getAutor();
            if (autor != null) {
                Optional<Autor> autorExistente = autorRepositorio.findByNombreContainsIgnoreCase(autor.getNombre());

                if (autorExistente.isPresent()) {
                    // Si el autor ya existe, usamos el de la BD
                    libro.setAutor(autorExistente.get());
                } else {
                    // Si es nuevo, lo guardamos primero
                    autorRepositorio.save(autor);
                }
            }

            // Finalmente guardamos el libro
            repositorio.save(libro);
            System.out.println("Libro guardado en la base de datos.");

        } else {
            System.out.println("Libro no encontrado en la API");
        }
    }

    private void listarLibrosRegistrados() {
        // 1. Buscamos todos los libros en la base de datos
        List<Libro> libros = repositorio.findAll();

        // 2. Los imprimimos de forma ordenada
        if (libros.isEmpty()) {
            System.out.println("No hay libros registrados aún.");
        } else {
            libros.stream()
                    .sorted(Comparator.comparing(Libro::getTitulo))
                    .forEach(System.out::println);
        }
    }

    private void listarAutoresRegistrados() {
        System.out.println("--- Autores Registrados ---");
        List<Autor> autores = autorRepositorio.findAll(); // First time

        autores.stream()
                .sorted(Comparator.comparing(Autor::getNombre))
                .forEach(System.out::println);
    }

    // Note: If the currently store author is a String, it's necessary
    // to migrate Author to its own Entity to store birth/death years.
    // For now, we will list all registered authors as a placeholder.
    private void listarAutoresVivosEnDeterminadoAnio() {
        System.out.println("Escriba el año que desea consultar:");
        try {
            var anio = teclado.nextInt();
            teclado.nextLine();

            // Use the repository method we created earlier
            List<Autor> autoresVivos = autorRepositorio.autoresVivosEnDeterminadoAnio(anio);

            if (autoresVivos.isEmpty()) {
                System.out.println("No se encontraron autores vivos registrados en el año " + anio);
            } else {
                System.out.println("\n--- Autores vivos en el año " + anio + " ---");
                autoresVivos.forEach(System.out::println);
            }
        } catch (Exception e) {
            System.out.println("Error: Por favor, ingrese un número de año válido.");
            teclado.nextLine(); // Clear the scanner buffer
        }
    }

    private void listarLibrosPorIdioma() {
        System.out.println("""
                Ingrese el idioma para buscar los libros:
                es - español
                en - inglés
                fr - francés
                pt - portugués
                """);
        var idioma = teclado.nextLine();
        List<Libro> librosPorIdioma = repositorio.findByIdioma(idioma);

        if (librosPorIdioma.isEmpty()) {
            System.out.println("No se encontraron libros en ese idioma.");
        } else {
            librosPorIdioma.forEach(System.out::println);
        }
    }
}


