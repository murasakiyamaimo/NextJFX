package net.murasakiyamaimo;

import org.lwjgl.BufferUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IOUtil {
    public static ByteBuffer readResource(String resourceName) throws IOException {
        Path path = Paths.get("src/main/resources/" + resourceName);
        try (SeekableByteChannel fc = Files.newByteChannel(path)) {
            ByteBuffer buffer = BufferUtils.createByteBuffer((int) fc.size() + 1);
            while (fc.read(buffer) != -1) {
                // Do nothing
            }
            buffer.flip();
            return buffer;
        }
    }
}