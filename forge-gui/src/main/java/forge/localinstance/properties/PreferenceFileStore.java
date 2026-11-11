package forge.localinstance.properties;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Commits complete preference files and merges only this session's edits. */
final class PreferenceFileStore {
    private PreferenceFileStore() { }

    static synchronized Map<String, String> save(final Path file, final Map<String, String> defaults,
                                                final Map<String, String> changes) throws IOException {
        final Path target = file.toAbsolutePath();
        Files.createDirectories(target.getParent());
        try (FileChannel channel = FileChannel.open(target.resolveSibling(target.getFileName() + ".lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = channel.lock()) {
            final Map<String, String> merged = new LinkedHashMap<>(defaults);
            if (Files.exists(target)) {
                for (final String line : Files.readAllLines(target, StandardCharsets.UTF_8)) {
                    final int separator = line.indexOf('=');
                    if (separator > 0 && !line.startsWith("#")) {
                        merged.put(line.substring(0, separator), line.substring(separator + 1));
                    }
                }
            }
            merged.putAll(changes);
            final StringBuilder content = new StringBuilder();
            for (final Map.Entry<String, String> entry : merged.entrySet()) {
                content.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
            }
            if (Files.exists(target) && Files.size(target) > 0) {
                replace(target.resolveSibling(target.getFileName() + ".bak"), Files.readAllBytes(target));
            }
            replace(target, content.toString().getBytes(StandardCharsets.UTF_8));
            return merged;
        }
    }

    private static void replace(final Path target, final byte[] content) throws IOException {
        final Path temporary = Files.createTempFile(target.getParent(), target.getFileName() + ".", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                final java.nio.ByteBuffer bytes = java.nio.ByteBuffer.wrap(content);
                while (bytes.hasRemaining()) { channel.write(bytes); }
                channel.force(true);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
