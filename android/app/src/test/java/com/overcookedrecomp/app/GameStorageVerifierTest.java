package com.overcookedrecomp.app;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GameStorageVerifierTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private File writePe(String name, int machine, int optionalMagic) throws IOException {
        byte[] bytes = new byte[512];
        bytes[0] = 'M';
        bytes[1] = 'Z';
        ByteBuffer header = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        header.putInt(0x3c, 0x80);
        bytes[0x80] = 'P';
        bytes[0x81] = 'E';
        bytes[0x82] = 0;
        bytes[0x83] = 0;
        header.putShort(0x84, (short) machine);
        header.putShort(0x86, (short) 3);
        header.putShort(0x94, (short) 0xF0);
        header.putShort(0x98, (short) optionalMagic);
        File file = new File(temporary.getRoot(), name);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
        }
        return file;
    }

    @Test
    public void recognizesPe32PlusAmd64WithoutExecutingIt() throws Exception {
        File exe = writePe("Overcooked2.exe", 0x8664, 0x020B);
        GameStorageVerifier.ExecutableVerificationResult result =
                GameStorageVerifier.inspectGameExecutable(exe, GameStorageVerifier.GameTitle.UNKNOWN);
        assertTrue(result.valid);
        assertTrue(result.supportedByBox64);
        assertEquals("x86_64", result.architecture);
        assertEquals(GameStorageVerifier.GameTitle.OVERCOOKED_2, result.gameTitle);
        assertEquals(64, result.sha256.length());
    }

    @Test
    public void reportsValidButUnsupportedX86Pe32() throws Exception {
        File exe = writePe("Overcooked.exe", 0x014C, 0x010B);
        GameStorageVerifier.ExecutableVerificationResult result =
                GameStorageVerifier.inspectGameExecutable(exe, GameStorageVerifier.GameTitle.OVERCOOKED_1);
        assertTrue(result.valid);
        assertFalse(result.supportedByBox64);
        assertEquals("x86", result.architecture);
    }

    @Test
    public void rejectsFilesWithoutDosHeader() throws Exception {
        File exe = temporary.newFile("Overcooked2.exe");
        GameStorageVerifier.ExecutableVerificationResult result =
                GameStorageVerifier.inspectGameExecutable(exe, GameStorageVerifier.GameTitle.OVERCOOKED_2);
        assertFalse(result.valid);
        assertFalse(result.supportedByBox64);
    }

    @Test
    public void findsTheTitleMatchingExecutableInImportedTree() throws Exception {
        File root = temporary.newFolder("Overcooked2_PC");
        File exe = writePe("Overcooked2.exe", 0x8664, 0x020B);
        File target = new File(root, "Overcooked2.exe");
        assertTrue(exe.renameTo(target));
        File found = GameStorageVerifier.findGameExecutable(root, GameStorageVerifier.GameTitle.OVERCOOKED_2);
        assertNotNull(found);
        assertEquals(target.getCanonicalFile(), found.getCanonicalFile());
    }
}
