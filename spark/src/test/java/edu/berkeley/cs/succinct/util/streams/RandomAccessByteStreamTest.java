package edu.berkeley.cs.succinct.util.streams;

import junit.framework.TestCase;
import org.apache.hadoop.fs.FSDataInputStream;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

public class RandomAccessByteStreamTest extends TestCase {

    public void testGet() throws Exception {
        System.out.println("get");
        ByteBuffer buf = ByteBuffer.allocate(10);
        for (int i = 0; i < 10; i++) {
            buf.put((byte) i);
        }
        FSDataInputStream is = TestUtils.getStream(buf);
        RandomAccessByteStream bs = new RandomAccessByteStream(is, 0, 10);
        for (int i = 0; i < 10; i++) {
            assertEquals(i, bs.get());
        }
    }

    public void testGet1() throws Exception {
        System.out.println("get1");
        ByteBuffer buf = ByteBuffer.allocate(10);
        for (int i = 0; i < 10; i++) {
            buf.put((byte) i);
        }
        FSDataInputStream is = TestUtils.getStream(buf);
        RandomAccessByteStream bs = new RandomAccessByteStream(is, 0, 10);
        for (int i = 0; i < 10; i++) {
            assertEquals(i, bs.get(i));
        }
    }

    public void testGetShort() throws Exception {
        System.out.println("getShort");
        ShortBuffer buf = ShortBuffer.allocate(10);
        for (int i = 0; i < 10; i++) {
            buf.put((short) i);
        }
        FSDataInputStream is = TestUtils.getStream(buf);
        RandomAccessByteStream bs = new RandomAccessByteStream(is, 0, 20);
        for (int i = 0; i < 10; i++) {
            assertEquals(i, bs.getShort());
        }
    }

    public void testGetShort1() throws Exception {
        System.out.println("getShort1");
        ShortBuffer buf = ShortBuffer.allocate(10);
        for (int i = 0; i < 10; i++) {
            buf.put((short) i);
        }
        FSDataInputStream is = TestUtils.getStream(buf);
        RandomAccessByteStream bs = new RandomAccessByteStream(is, 0, 20);
        for (int i = 0; i < 10; i++) {
            assertEquals(i, bs.getShort(i * 2));
        }
    }

    public void testGetInt() throws Exception {
        System.out.println("getInt");
        IntBuffer buf = IntBuffer.allocate(10);
        for (int i = 0; i < 10; i++) {
            buf.put(i);
        }
        FSDataInputStream is = TestUtils.getStream(buf);
        RandomAccessByteStream bs = new RandomAccessByteStream(is, 0, 40);
        for (int i = 0; i < 10; i++) {
            assertEquals(i, bs.getInt());
        }
    }

    public void testGetInt1() throws Exception {
        System.out.println("getInt1");
        IntBuffer buf = IntBuffer.allocate(10);
        for (int i = 0; i < 10; i++) {
            buf.put(i);
        }
        FSDataInputStream is = TestUtils.getStream(buf);
        RandomAccessByteStream bs = new RandomAccessByteStream(is, 0, 40);
        for (int i = 0; i < 10; i++) {
            assertEquals(i, bs.getInt(i * 4));
        }
    }

    public void testGetLong() throws Exception {
        System.out.println("getLong");
        LongBuffer buf = LongBuffer.allocate(10);
        for (int i = 0; i < 10; i++) {
            buf.put(i);
        }
        FSDataInputStream is = TestUtils.getStream(buf);
        RandomAccessByteStream bs = new RandomAccessByteStream(is, 0, 80);
        for (int i = 0; i < 10; i++) {
            assertEquals(i, bs.getLong());
        }
    }

    public void testGetLong1() throws Exception {
        System.out.println("getLong1");
        LongBuffer buf = LongBuffer.allocate(10);
        for (int i = 0; i < 10; i++) {
            buf.put(i);
        }
        FSDataInputStream is = TestUtils.getStream(buf);
        RandomAccessByteStream bs = new RandomAccessByteStream(is, 0, 80);
        for (int i = 0; i < 10; i++) {
            assertEquals(i, bs.getLong(i * 8));
        }
    }

    public void testPosition() throws Exception {
        System.out.println("position");
        ByteBuffer buf = ByteBuffer.allocate(10);
        for (int i = 0; i < 10; i++) {
            buf.put((byte) i);
        }
        FSDataInputStream is = TestUtils.getStream(buf);
        RandomAccessByteStream bs = new RandomAccessByteStream(is, 0, 10);
        bs.position(3);
        assertEquals(bs.position(), 3);
    }

    public void testRewind() throws Exception {
        System.out.println("rewind");
        ByteBuffer buf = ByteBuffer.allocate(10);
        for (int i = 0; i < 10; i++) {
            buf.put((byte) i);
        }
        FSDataInputStream is = TestUtils.getStream(buf);
        RandomAccessByteStream bs = new RandomAccessByteStream(is, 0, 10);
        bs.position(3);
        bs.rewind();
        assertEquals(0, bs.position());
    }

    public void testOffsetBeginning() throws Exception {
        System.out.println("offsetBeginning");
        ByteBuffer buf = ByteBuffer.allocate(20);
        for (int i = 0; i < 20; i++) {
            buf.put((byte) i);
        }
        FSDataInputStream is = TestUtils.getStream(buf);
        RandomAccessByteStream bs = new RandomAccessByteStream(is, 10, 10);
        for (int i = 10; i < 20; i++) {
            assertEquals((byte) i, bs.get());
        }
        bs.rewind();
        assertEquals(0, bs.position());
        for (int i = 0; i < 10; i++) {
            assertEquals((byte) (i + 10), bs.get(i));
        }

    }

    public void testAsIntStream() throws Exception {
        System.out.println("asIntStream");
        IntBuffer buf = IntBuffer.allocate(20);
        for (int i = 0; i < 20; i++) {
            buf.put(i);
        }
        FSDataInputStream is = TestUtils.getStream(buf);
        RandomAccessByteStream bs = new RandomAccessByteStream(is, 0, 80);
        for (int i = 0; i < 10; i++) {
            assertEquals(i, bs.getInt());
        }
        RandomAccessIntStream intStream = bs.asIntStream();
        for (int i = 0; i < 10; i++) {
            assertEquals((i + 10), intStream.get());
        }
        intStream.rewind();
        for (int i = 0; i < 10; i++) {
            assertEquals((i + 10), intStream.get(i));
        }
    }

    public void testAsLongStream() throws Exception {
        System.out.println("asLongStream");
        LongBuffer buf = LongBuffer.allocate(20);
        for (int i = 0; i < 20; i++) {
            buf.put(i);
        }
        FSDataInputStream is = TestUtils.getStream(buf);
        RandomAccessByteStream bs = new RandomAccessByteStream(is, 0, 160);
        for (int i = 0; i < 10; i++) {
            assertEquals(i, bs.getLong());
        }
        RandomAccessLongStream longStream = bs.asLongStream();
        for (int i = 0; i < 10; i++) {
            assertEquals((i + 10), longStream.get());
        }
        longStream.rewind();
        for (int i = 0; i < 10; i++) {
            assertEquals((i + 10), longStream.get(i));
        }
    }
}
