package mdplayer.emu.nise68;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import mdplayer.emu.common.Utils;

import static java.lang.System.getLogger;


public class FileMng {

    private static final Logger logger = getLogger(FileMng.class.getName());

    public final String VCurrentPath;
    private final String pDir;
    private final String vDir;
    //private String crntDir;

    public final Map<String, vFileInfo> vDrive = new HashMap<>();

    /** every name a program asked about, in order, and whether it was there */
    private final Map<String, Boolean> lookups = new LinkedHashMap<>();

    /** @return the names {@link #existsFile} was asked, in order, each with whether it was found */
    public Map<String, Boolean> getLookups() {
        return Collections.unmodifiableMap(lookups);
    }

    /**
     * Constructor
     *
     * @param physicalPath physical Path
     * @param virtualPath  virtual Path
     */
    public FileMng(String physicalPath, String virtualPath /* = "C:" */) {
        String p = physicalPath;
        String v = virtualPath.toUpperCase();
        if (p.charAt(p.length() - 1) == File.separatorChar) p = p.substring(0, p.length() - 1);
        if (v.charAt(v.length() - 1) == File.separatorChar) v = v.substring(0, v.length() - 1);

        this.pDir = p; // .split(File.separatorChar);
        this.vDir = v; // .split(File.separatorChar);
        this.VCurrentPath = v;
        vDrive.clear();
    }

    public boolean existsFile(String vFile) {
logger.log(Level.TRACE, "vFile: " + vFile);
        // Check if there are files in the virtual drive
        String vFull = Path.of(VCurrentPath, vFile).toString().toUpperCase();
logger.log(Level.TRACE, "vFull: " + vFull);
        boolean found;
        if (vDrive.containsKey(vFull)) {
            found = vDrive.get(vFull).body != null;
        } else {
            try {
                // If not present on the virtual drive, check the physical drive
                String pFull = convertPhysicalFileName(Path.of(VCurrentPath, vFile).toString());
logger.log(Level.TRACE, "pFull: " + pFull);
                found = Utils.fileExistsIgnoreCase(Path.of(pFull)) != null;
            } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
                found = false;
            }
        }
        lookups.put(vFile, found);
        return found || vDrive.containsKey(vFull);
    }

    /**
     * Create a file on the virtual drive
     *
     * @param vFilename Filename for Virtual Drive
     * @param body      File body
     */
    public void setVFile(String vFilename, byte[] body) {
        String vFull = Path.of(VCurrentPath, vFilename).toString().toUpperCase();
        if (vDrive.containsKey(vFull)) {
            vDrive.get(vFull).body = body;
        } else {
            vFileInfo fileInfo = new vFileInfo();
            fileInfo.name = Path.of(vFull).getFileName().toString();
            fileInfo.body = body;
            vDrive.put(vFull, fileInfo);
        }
    }

    /**
     * Create a file on the virtual drive
     *
     * @param pFilename Read a file from the physical drive and set it
     *                 as the current file on the virtual drive
     */
    public void setVFile(String pFilename) {
        byte[] body;
        try {
            body = Files.readAllBytes(Path.of(pFilename));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        setVFile(Path.of(pFilename).getFileName().toString(), body);
    }

    /**
     * Read entire files from virtual drive
     * If the file is not present on the virtual drive, it is read from the physical drive.
     * This creates a file on the virtual drive.
     *
     * @param vFilename
     * @returns
     */
    public byte[] vReadAllBytes(String vFilename) {
        // Check if there are files in the virtual drive
        String vFull = Path.of(VCurrentPath, vFilename).toString().toUpperCase();
logger.log(Level.TRACE, "vDrive: "  + vFull + ", " + vDrive.keySet() + ", " + vDrive.containsKey(vFull) + ", " + (vDrive.containsKey(vFull) ? vDrive.get(vFull).body != null ? vDrive.get(vFull).body.length : "null" : "n/a"));
if (vDrive.containsKey(vFull) && vDrive.get(vFull).body == null) { logger.log(Level.WARNING, vFilename + " body is null"); }
        if (vDrive.containsKey(vFull)) return vDrive.get(vFull).body;

        try {
            // If not present on the virtual drive, check the physical drive
            String pFull = convertPhysicalFileName(Path.of(VCurrentPath, vFilename).toString());
            byte[] body;
            try {
                Path p = Utils.fileExistsIgnoreCase(Path.of(pFull.replace("\\", File.separator)));
                body = p != null ? Files.readAllBytes(p) : null;
            } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
                body = null;
            }
if (body == null) { logger.log(Level.WARNING, vFilename + " body is null (first time? create?)"); }
            setVFile(vFilename, body);
            return body;
        } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
            return null;
        }
    }

    public String vGetFullFilename(String vFilename) {
        String vFull = Path.of(VCurrentPath, vFilename).toString().toUpperCase();
        return vFull;
    }

    private String convertPhysicalFileName(String vFull) {
        String vPath = Path.of(vFull).getParent().toString();
        if (vPath.indexOf(vDir) != 0) {
            if (!vPath.equals("\\")) {
                throw new IndexOutOfBoundsException("Referencing an out of range path");
            }
        }
        String pFull;
        if (!vPath.equals("\\")) {
            pFull = Path.of(vPath.replace(vDir, pDir), Path.of(vFull).getFileName().toString()).toString();
        } else {
            pFull = Path.of(pDir, Path.of(vFull).getFileName().toString()).toString();
        }

        return pFull;
    }

    public static class vFileInfo {

        public String name;
        public byte[] body;
    }
}
