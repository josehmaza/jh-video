package com.jh.carvideo;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Utilidades de texto del buscador. Los regex se compilan una sola vez
 * (String.replaceAll / split los recompilaban en cada llamada).
 */
final class SearchText {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern REPEATED_LETTERS = Pattern.compile("([a-z])\\1+");
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    // Raíz de montaje del pendrive: /storage/udisk1/, /mnt/media_rw/udisk2/, /storage/usb0/ ...
    private static final Pattern USB_MOUNT_PREFIX = Pattern.compile("^.*?/(?:udisk|usb)\\d*(?:/|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_EXTENSION = Pattern.compile("\\.[^./]+$");

    private SearchText() {}

    /** Quita tildes, pasa a minúsculas y recorta espacios. */
    static String normalize(String text) {
        if (text == null) return "";
        String n = Normalizer.normalize(text, Normalizer.Form.NFD);
        return DIACRITICS.matcher(n).replaceAll("").toLowerCase().trim();
    }

    /** Minúsculas conservando ñ y tildes, para premiar la escritura exacta ("ñaña" antes que "nana"). */
    static String exactLower(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT).trim();
    }

    /** Colapsa letras repetidas: "holaaa" → "hola". */
    static String dedup(String s) {
        if (s == null) return "";
        return REPEATED_LETTERS.matcher(s).replaceAll("$1");
    }

    /** Palabras de un texto ya normalizado. */
    static String[] words(String norm) {
        return NON_ALNUM.split(norm);
    }

    /** Términos de la consulta ya normalizada. */
    static String[] tokens(String norm) {
        return WHITESPACE.split(norm);
    }

    /**
     * Ruta relativa al pendrive: "/storage/udisk1/Salsa/Tema.mp3" → "Salsa/Tema.mp3".
     * Evita que "storage" o "udisk" coincidan con todas las canciones.
     */
    static String relativeUsbPath(String path) {
        if (path == null) return "";
        return USB_MOUNT_PREFIX.matcher(path).replaceFirst("");
    }

    /** Quita la extensión del archivo: "Salsa/Tema.mp3" → "Salsa/Tema". */
    static String stripExtension(String path) {
        if (path == null) return "";
        return FILE_EXTENSION.matcher(path).replaceFirst("");
    }
}
