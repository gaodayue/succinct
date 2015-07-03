package edu.berkeley.cs.succinct.util.streams;

import edu.berkeley.cs.succinct.dictionary.Tables;
import edu.berkeley.cs.succinct.util.CommonUtils;

import java.io.IOException;

import static edu.berkeley.cs.succinct.util.CommonUtils.DictionaryUtils.*;

public class SerializedOperations {

    public static class ArrayOps {

        /**
         * Get rank1 value at specified index in serialized Long array.
         *
         * @param arrayBuf Serialized Long array.
         * @param startPos Start position.
         * @param size     Size of array.
         * @param i        Rank query.
         * @return Value of rank1 for query.
         */
        public static int getRank1(LongArrayStream arrayBuf, int startPos, int size, long i) throws IOException {
            int sp = 0, ep = size - 1;
            int m;

            while (sp <= ep) {
                m = (sp + ep) / 2;
                if (arrayBuf.get(startPos + m) == i) {
                    return m + 1;
                } else if (i < arrayBuf.get(startPos + m)) {
                    ep = m - 1;
                } else {
                    sp = m + 1;
                }
            }

            return ep + 1;
        }
    }

    public static class BitMapOps {

        /**
         * Get bit at specified position in serialized bitmap.
         *
         * @param bitmap Serialized bitmap.
         * @param i      Index into serialized bitmap.
         * @return Value of bit.
         */
        public static long getBit(RandomAccessLongStream bitmap, int i) throws IOException {
            return ((bitmap.get((int) (bitmap.position() + (i / 64))) >>> (63L - i)) & 1L);
        }

        /**
         * Get value at specified index of serialized bitmap.
         *
         * @param bitmap Serialized bitmap.
         * @param pos    Position into bitmap.
         * @param bits   Width in bits of value.
         * @return Value at specified position.
         */
        public static long getValPos(RandomAccessLongStream bitmap, int pos, int bits) throws IOException {
            assert (pos >= 0);

            int basePos = (int) bitmap.position();
            long val;
            long s = (long) pos;
            long e = s + (bits - 1);

            if ((s / 64) == (e / 64)) {
                val = bitmap.get(basePos + (int) (s / 64L)) << (s % 64);
                val = val >>> (63 - e % 64 + s % 64);
            } else {
                val = bitmap.get(basePos + (int) (s / 64L)) << (s % 64);
                val = val >>> (s % 64 - (e % 64 + 1));
                val = val
                        | (bitmap.get(basePos + (int) (e / 64L)) >>> (63 - e % 64));
            }
            assert (val >= 0);
            return val;
        }
    }

    public static class BMArrayOps {

        /**
         * Get value at specified index of a serialized bitmap array.
         *
         * @param B    Serialized bitmap array.
         * @param i    Index into serialized array.
         * @param bits Width in bits of value.
         * @return Value at specified index.
         */
        public static long getVal(LongArrayStream B, int i, int bits) throws IOException {
            assert (i >= 0);

            long val;
            long s = (long) (i) * bits;
            long e = s + (bits - 1);

            if ((s / 64) == (e / 64)) {
                val = B.get((int) (s / 64L)) << (s % 64);
                val = val >>> (63 - e % 64 + s % 64);
            } else {
                long val1 = B.get((int) (s / 64L)) << (s % 64);
                long val2 = B.get((int) (e / 64L)) >>> (63 - e % 64);
                val1 = val1 >>> (s % 64 - (e % 64 + 1));
                val = val1 | val2;
            }

            return val;
        }
    }

    public static class DictionaryOps {

        /**
         * Get rank1 at specified index of serialized dictionary.
         *
         * @param buf      ByteBuffer containing serialized Dictionary.
         * @param startPos Starting position within ByteBuffer.
         * @param i        Index to compute rank1.
         * @return Value of rank1.
         */
        public static long getRank1(RandomAccessByteStream buf, int startPos, int i) throws IOException {
            if (i < 0) {
                return 0;
            }

            int l3Idx = (int) (i / CommonUtils.two32);
            int l2Idx = i / 2048;
            int l1Idx = (i % 512);
            int rem = ((i % 2048) / 512);
            int blockClass, blockOffset;

            buf.position(startPos);
            RandomAccessLongStream dictBuf = buf.asLongStream();
            long size = dictBuf.get();

            int l3_size = (int) (size / CommonUtils.two32) + 1;
            int l12_size = (int) (size / 2048) + 1;

            int basePos = (int) dictBuf.position();

            long rank_l3 = dictBuf.get(basePos + l3Idx);
            long pos_l3 = dictBuf.get(basePos + l3_size + l3Idx);
            long rank_l12 = dictBuf.get(basePos + l3_size + l3_size + l2Idx);
            long pos_l12 = dictBuf.get(basePos + l3_size + l3_size + l12_size
                    + l2Idx);
            dictBuf.position(basePos + l3_size + l3_size + l12_size + l12_size);

            long res = rank_l3 + GETRANKL2(rank_l12);
            long pos = pos_l3 + GETPOSL2(pos_l12);

            switch (rem) {
                case 1:
                    res += GETRANKL1(rank_l12, 1);
                    pos += GETPOSL1(pos_l12, 1);
                    break;

                case 2:
                    res += GETRANKL1(rank_l12, 1) + GETRANKL1(rank_l12, 2);
                    pos += GETPOSL1(pos_l12, 1) + GETPOSL1(pos_l12, 2);
                    break;

                case 3:
                    res += GETRANKL1(rank_l12, 1) + GETRANKL1(rank_l12, 2)
                            + GETRANKL1(rank_l12, 3);
                    pos += GETPOSL1(pos_l12, 1) + GETPOSL1(pos_l12, 2)
                            + GETPOSL1(pos_l12, 3);
                    break;

                default:
                    break;
            }

            dictBuf.get(); // TODO: Could remove this field altogether

            // Popcount
            while (l1Idx >= 16) {
                blockClass = (int) BitMapOps.getValPos(dictBuf,
                        (int) pos, 4);
                pos += 4;
                blockOffset = (int) ((blockClass == 0) ? BitMapOps
                        .getBit(dictBuf, (int) pos) * 16 : 0);
                pos += Tables.offsetBits[blockClass];
                res += blockClass + blockOffset;
                l1Idx -= 16;
            }

            blockClass = (int) BitMapOps
                    .getValPos(dictBuf, (int) pos, 4);
            pos += 4;
            blockOffset = (int) BitMapOps.getValPos(dictBuf, (int) pos,
                    Tables.offsetBits[blockClass]);
            res += Tables.smallRank[Tables.decodeTable[blockClass][blockOffset]][l1Idx];

            return res;
        }

        /**
         * Get rank0 at specified index of serialized dictionary.
         *
         * @param buf      ByteBuffer containing serialized Dictionary.
         * @param startPos Starting position within ByteBuffer.
         * @param i        Index to compute rank0.
         * @return Value of rank0.
         * @throws IOException
         */
        public static long getRank0(RandomAccessByteStream buf, int startPos, int i) throws IOException {
            return i - getRank1(buf, startPos, i) + 1;
        }

        /**
         * Get select1 at specified index of serialized dictionary.
         *
         * @param buf      ByteBuffer containing serialized Dictionary.
         * @param startPos Starting position within ByteBuffer.
         * @param i        Index to compute select1.
         * @return Value of select1.
         */
        public static long getSelect1(RandomAccessByteStream buf, int startPos, int i) throws IOException {

            assert (i >= 0);

            buf.position(startPos);
            RandomAccessLongStream dictBuf = buf.asLongStream();

            long size = dictBuf.get();

            long val = i + 1;
            int sp = 0;
            int ep = (int) (size / CommonUtils.two32);
            int m;
            long r;
            int pos = 0;
            int blockClass, blockOffset;
            long sel;
            int lastBlock;
            long rankL12, posL12;

            int l3Size = (int) ((size / CommonUtils.two32) + 1);
            int l12Size = (int) ((size / 2048) + 1);
            int basePos = (int) dictBuf.position();

            while (sp <= ep) {
                m = (sp + ep) / 2;
                r = dictBuf.get(basePos + m);
                if (val > r) {
                    sp = m + 1;
                } else {
                    ep = m - 1;
                }
            }

            ep = Math.max(ep, 0);
            val -= dictBuf.get(basePos + ep);
            pos += dictBuf.get(basePos + l3Size + ep);
            sp = (int) (ep * CommonUtils.two32 / 2048);
            ep = (int) (Math.min(((ep + 1) * CommonUtils.two32 / 2048),
                    Math.ceil((double) size / 2048.0)) - 1);

            assert (val <= CommonUtils.two32);
            assert (pos >= 0);

            dictBuf.position(basePos + 2 * l3Size);
            basePos = (int) dictBuf.position();

            while (sp <= ep) {
                m = (sp + ep) / 2;
                r = GETRANKL2(dictBuf.get(basePos + m));
                if (val > r) {
                    sp = m + 1;
                } else {
                    ep = m - 1;
                }
            }

            ep = Math.max(ep, 0);
            sel = (long) (ep) * 2048L;
            rankL12 = dictBuf.get(basePos + ep);
            posL12 = dictBuf.get(basePos + l12Size + ep);
            val -= GETRANKL2(rankL12);
            pos += GETPOSL2(posL12);

            assert (val <= 2048);
            assert (pos >= 0);

            r = GETRANKL1(rankL12, 1);
            if (sel + 512 < size && val > r) {
                pos += GETPOSL1(posL12, 1);
                val -= r;
                sel += 512;
                r = GETRANKL1(rankL12, 2);
                if (sel + 512 < size && val > r) {
                    pos += GETPOSL1(posL12, 2);
                    val -= r;
                    sel += 512;
                    r = GETRANKL1(rankL12, 3);
                    if (sel + 512 < size && val > r) {
                        pos += GETPOSL1(posL12, 3);
                        val -= r;
                        sel += 512;
                    }
                }
            }

            dictBuf.position(basePos + 2 * l12Size);

            assert (val <= 512);
            assert (pos >= 0);

            dictBuf.get(); // TODO: Could remove this field altogether

            while (true) {
                blockClass = (int) BitMapOps.getValPos(dictBuf, pos, 4);
                short offsetSize = (short) Tables.offsetBits[blockClass];
                pos += 4;
                blockOffset = (int) ((blockClass == 0) ? BitMapOps.getBit(dictBuf, pos) * 16
                        : 0);
                pos += offsetSize;

                if (val <= (blockClass + blockOffset)) {
                    pos -= (4 + offsetSize);
                    break;
                }

                val -= (blockClass + blockOffset);
                sel += 16;
            }

            blockClass = (int) BitMapOps.getValPos(dictBuf, pos, 4);
            pos += 4;
            blockOffset = (int) BitMapOps.getValPos(dictBuf, pos,
                    Tables.offsetBits[blockClass]);
            lastBlock = Tables.decodeTable[blockClass][blockOffset];

            long count = 0;
            for (i = 0; i < 16; i++) {
                if (((lastBlock >>> (15 - i)) & 1) == 1) {
                    count++;
                }
                if (count == val) {
                    return sel + i;
                }
            }

            return sel;
        }

        /**
         * Get select0 at specified index of serialized dictionary.
         *
         * @param buf      ByteBuffer containing serialized Dictionary.
         * @param startPos Starting position within ByteBuffer.
         * @param i        Index to compute select0.
         * @return Value of select0.
         */
        public static long getSelect0(RandomAccessByteStream buf, int startPos, int i) throws IOException {

            assert (i >= 0);

            buf.position(startPos);
            RandomAccessLongStream dictBuf = buf.asLongStream();

            long size = dictBuf.get();

            long val = i + 1;
            int sp = 0;
            int ep = (int) (size / CommonUtils.two32);
            int m;
            long r;
            int pos = 0;
            int blockClass, blockOffset;
            long sel;
            int lastBlock;
            long rankL12, posL12;

            int l3Size = (int) ((size / CommonUtils.two32) + 1);
            int l12Size = (int) ((size / 2048) + 1);

            int basePos = (int) dictBuf.position();

            while (sp <= ep) {
                m = (sp + ep) / 2;
                r = (m * CommonUtils.two32 - dictBuf.get(basePos + m));
                if (val > r) {
                    sp = m + 1;
                } else {
                    ep = m - 1;
                }
            }

            ep = Math.max(ep, 0);
            val -= (ep * CommonUtils.two32 - dictBuf.get(basePos + ep));
            pos += dictBuf.get(basePos + l3Size + ep);
            sp = (int) (ep * CommonUtils.two32 / 2048);
            ep = (int) (Math.min(((ep + 1) * CommonUtils.two32 / 2048),
                    Math.ceil((double) size / 2048.0)) - 1);

            assert (val <= CommonUtils.two32);
            assert (pos >= 0);

            dictBuf.position(basePos + 2 * l3Size);
            basePos = (int) dictBuf.position();

            while (sp <= ep) {
                m = (sp + ep) / 2;
                r = m * 2048 - GETRANKL2(dictBuf.get(basePos + m));
                if (val > r) {
                    sp = m + 1;
                } else {
                    ep = m - 1;
                }
            }

            ep = Math.max(ep, 0);
            sel = (long) (ep) * 2048L;
            rankL12 = dictBuf.get(basePos + ep);
            posL12 = dictBuf.get(basePos + l12Size + ep);
            val -= (ep * 2048 - GETRANKL2(rankL12));
            pos += GETPOSL2(posL12);

            assert (val <= 2048);
            assert (pos >= 0);

            r = (512 - GETRANKL1(rankL12, 1));
            if (sel + 512 < size && val > r) {
                pos += GETPOSL1(posL12, 1);
                val -= r;
                sel += 512;
                r = (512 - GETRANKL1(rankL12, 2));
                if (sel + 512 < size && val > r) {
                    pos += GETPOSL1(posL12, 2);
                    val -= r;
                    sel += 512;
                    r = (512 - GETRANKL1(rankL12, 3));
                    if (sel + 512 < size && val > r) {
                        pos += GETPOSL1(posL12, 3);
                        val -= r;
                        sel += 512;
                    }
                }
            }

            dictBuf.position(basePos + 2 * l12Size);

            assert (val <= 512);
            assert (pos >= 0);

            dictBuf.get(); // TODO: Could remove this field altogether

            while (true) {
                blockClass = (int) BitMapOps.getValPos(dictBuf, pos, 4);
                short offsetSize = (short) Tables.offsetBits[blockClass];
                pos += 4;
                blockOffset = (int) ((blockClass == 0) ? BitMapOps.getBit(dictBuf, pos) * 16
                        : 0);
                pos += offsetSize;

                if (val <= (16 - (blockClass + blockOffset))) {
                    pos -= (4 + offsetSize);
                    break;
                }

                val -= (16 - (blockClass + blockOffset));
                sel += 16;
            }

            blockClass = (int) BitMapOps.getValPos(dictBuf, pos, 4);
            pos += 4;
            blockOffset = (int) BitMapOps.getValPos(dictBuf, pos,
                    Tables.offsetBits[blockClass]);
            lastBlock = Tables.decodeTable[blockClass][blockOffset];

            long count = 0;
            for (i = 0; i < 16; i++) {
                if (((lastBlock >> (15 - i)) & 1) == 0) {
                    count++;
                }
                if (count == val) {
                    return sel + i;
                }
            }

            return sel;
        }
    }
}
