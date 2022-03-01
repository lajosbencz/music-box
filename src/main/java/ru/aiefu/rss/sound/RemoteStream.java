package ru.aiefu.rss.sound;

import net.minecraft.client.sounds.AudioStream;
import org.lwjgl.BufferUtils;
import ru.aiefu.rss.RSS;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class RemoteStream implements AudioStream {
    private static final int mcs3 = RSS.PCM_MONO_LE.maximumChunkSize() * 3;

    private final AudioInputStream stream;
    private final byte[] buf = new byte[RSS.PCM_MONO_LE.maximumChunkSize()];

    public RemoteStream(AudioInputStream stream){
        this.stream = stream;
    }

    @Override
    public AudioFormat getFormat() {
        return stream.getFormat();
    }

    @Override
    public ByteBuffer read(int i) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(mcs3);
        int r = 0;
        while (r < 3){
            r++;
            try {
                stream.read(buf);
                buffer.put(buf);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        buffer.flip();
        return buffer;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
