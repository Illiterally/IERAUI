package forge.gui.framework;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/** IERAUI: commit complete layout documents without truncating a working layout. */
final class LayoutFileStore {
    private LayoutFileStore() { }

    static Path backupOf(final Path path) {
        return path.resolveSibling(path.getFileName() + ".bak");
    }

    static void write(final Path path, final byte[] document) throws IOException {
        if (!isComplete(document)) {
            throw new IOException("Refusing to save an incomplete layout");
        }
        final Path target = path.toAbsolutePath();
        Files.createDirectories(target.getParent());
        if (Files.exists(target)) {
            final byte[] previous = Files.readAllBytes(target);
            if (isComplete(previous)) {
                replace(backupOf(target), previous);
            }
        }
        replace(target, document);
    }

    private static void replace(final Path target, final byte[] document) throws IOException {
        final Path temporary = Files.createTempFile(target.getParent(), target.getFileName() + ".", ".tmp");
        try {
            Files.write(temporary, document);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static boolean isComplete(final byte[] document) {
        XMLStreamReader reader = null;
        try {
            final XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            reader = factory.createXMLStreamReader(new ByteArrayInputStream(document));
            boolean rootSeen = false;
            boolean panelSeen = false;
            boolean docSeen = false;
            while (reader.hasNext()) {
                if (reader.next() == XMLStreamReader.START_ELEMENT) {
                    if (!rootSeen) {
                        if (!"layout".equals(reader.getLocalName())) { return false; }
                        rootSeen = true;
                    } else if ("cell".equals(reader.getLocalName())) {
                        for (final String attribute : new String[] {"x", "y", "w", "h"}) {
                            final double value = Double.parseDouble(reader.getAttributeValue(null, attribute));
                            if (!Double.isFinite(value) || value < 0 || value > 1
                                    || (("w".equals(attribute) || "h".equals(attribute)) && value == 0)) {
                                return false;
                            }
                        }
                        panelSeen = true;
                    } else if ("doc".equals(reader.getLocalName())) {
                        docSeen = true;
                    }
                }
            }
            return rootSeen && panelSeen && docSeen;
        } catch (XMLStreamException | IllegalArgumentException | NullPointerException e) {
            return false;
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (XMLStreamException ignored) { }
            }
        }
    }
}
