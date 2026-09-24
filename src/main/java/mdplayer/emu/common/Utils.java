/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package mdplayer.emu.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


/**
 * Utils.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-04-14 nsano initial version <br>
 */
public class Utils {

    private static final Logger logger = System.getLogger(Utils.class.getName());

    /** */
    public static Path fileExistsIgnoreCase(Path path) {
        String name = path.getFileName().toString();
        int p = name.lastIndexOf('.');
        if (p < 0) p = name.length(); // a name without an extension is still a name
        String base = name.substring(0, p);
        String ext0 = name.substring(p).toLowerCase();
        Path parent = path.getParent();

        List<String> trials = new ArrayList<>();

        // File.ext
        trials.add(base + ext0);
        // File.EXT
        trials.add(base + ext0.toUpperCase());
        // file.ext
        trials.add(base.toLowerCase() + ext0);
        // file.EXT
        trials.add(base.toLowerCase() + ext0.toUpperCase());
        // FILE.ext
        trials.add(base.toUpperCase() + ext0);
        // FILE.EXT
        trials.add(base.toUpperCase() + ext0.toUpperCase());

        Path r = trials.stream()
                .map(filename -> parent != null ? parent.resolve(filename) : Path.of(filename))
                .filter(Files::exists)
                .findFirst()
                .orElse(null);

        // mixed case ("Z_g03.ZPD" asked as "Z_G03.ZPD") matches none of the above on a case-sensitive volume
        if (r == null) {
            Path dir = parent != null ? parent : Path.of(".");
            try (var files = Files.list(dir)) {
                r = files.filter(f -> f.getFileName().toString().equalsIgnoreCase(name))
                        .map(f -> parent != null ? f : f.getFileName())
                        .findFirst()
                        .orElse(null);
            } catch (IOException | UncheckedIOException e) {
                logger.log(Level.TRACE, e.toString());
            }
        }

if (r == null) {
 logger.log(Level.TRACE, path + " not found in " + trials /*, new Exception(path + " not found in " + trials) */);
}
        return r;
    }
}
