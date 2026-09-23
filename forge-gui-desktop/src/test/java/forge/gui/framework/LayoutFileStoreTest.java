package forge.gui.framework;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.Assert;
import org.testng.annotations.Test;

public class LayoutFileStoreTest {
    @Test
    public void rejectedDocumentPreservesTheWorkingLayout() throws IOException {
        final Path dir = Files.createTempDirectory("ieraui-layout-test");
        final Path path = dir.resolve("match.xml");
        final byte[] good = document("good");
        try {
            LayoutFileStore.write(path, good);
            Assert.expectThrows(IOException.class,
                    () -> LayoutFileStore.write(path, "<layout>".getBytes(StandardCharsets.UTF_8)));
            Assert.assertEquals(Files.readAllBytes(path), good);
            Assert.expectThrows(IOException.class,
                    () -> LayoutFileStore.write(path, "<layout/>".getBytes(StandardCharsets.UTF_8)));
            Assert.assertEquals(Files.readAllBytes(path), good);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(LayoutFileStore.backupOf(path));
            Files.deleteIfExists(dir);
        }
    }

    @Test
    public void successfulSaveRetainsPreviousAndNeverBacksUpTruncatedFile() throws IOException {
        final Path dir = Files.createTempDirectory("ieraui-layout-test");
        final Path path = dir.resolve("match.xml");
        final byte[] first = document("first");
        final byte[] second = document("second");
        try {
            LayoutFileStore.write(path, first);
            LayoutFileStore.write(path, second);
            Assert.assertEquals(Files.readAllBytes(path), second);
            Assert.assertEquals(Files.readAllBytes(LayoutFileStore.backupOf(path)), first);
            Files.writeString(path, new String(first, StandardCharsets.UTF_8).replace("w='1'", "w='NaN'"));
            LayoutFileStore.write(path, second);
            Assert.assertEquals(Files.readAllBytes(LayoutFileStore.backupOf(path)), first);
            Files.writeString(path, "<layout>");
            LayoutFileStore.write(path, second);
            Assert.assertEquals(Files.readAllBytes(LayoutFileStore.backupOf(path)), first);
        } finally {
            Files.deleteIfExists(path);
            Files.deleteIfExists(LayoutFileStore.backupOf(path));
            Files.deleteIfExists(dir);
        }
    }

    private static byte[] document(final String serial) {
        return ("<layout serial='" + serial + "'><cell x='0' y='0' w='1' h='1'><doc>FIELD_0</doc></cell></layout>")
                .getBytes(StandardCharsets.UTF_8);
    }
}
