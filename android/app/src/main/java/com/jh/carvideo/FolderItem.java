package com.jh.carvideo;

public class FolderItem {
    private String name;
    private String path;
    private int trackCount;
    private String usbSource;
    // Texto normalizado para el buscador; nombre y ruta no cambian, se calcula una sola vez
    private volatile SearchKey searchKey;

    public FolderItem(String name, String path, int trackCount, String usbSource) {
        this.name = name;
        this.path = path;
        this.trackCount = trackCount;
        this.usbSource = usbSource;
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public int getTrackCount() { return trackCount; }
    public void incrementCount() { this.trackCount++; }
    public String getUsbSource() { return usbSource; }

    public SearchKey getSearchKey() {
        SearchKey k = searchKey;
        if (k == null) {
            k = new SearchKey(this);
            searchKey = k;
        }
        return k;
    }

    /** Campos ya normalizados que usa el buscador (no dependen de la consulta). */
    public static final class SearchKey {
        public final String name;
        public final String[] words;
        public final String haystack;
        public final String haystackDedup;
        // Con ñ y tildes, para premiar la escritura exacta
        public final String exactName;
        public final String exactHaystack;

        SearchKey(FolderItem f) {
            name = SearchText.normalize(f.name);
            words = SearchText.words(name);
            haystack = name + " " + SearchText.normalize(SearchText.relativeUsbPath(f.path));
            haystackDedup = SearchText.dedup(haystack);
            exactName = SearchText.exactLower(f.name);
            exactHaystack = exactName + " " + SearchText.exactLower(SearchText.relativeUsbPath(f.path));
        }
    }
}
