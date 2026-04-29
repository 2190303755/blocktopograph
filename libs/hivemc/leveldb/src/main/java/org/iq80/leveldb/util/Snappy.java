/*
 * Copyright (C) 2011 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.iq80.leveldb.util;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <p>
 * A Snappy abstraction which attempts uses the iq80 implementation and falls back
 * to the xerial Snappy implementation it cannot be loaded.  You can change the
 * load order by setting the 'leveldb.snappy' system property.  Example:
 * <p/>
 * <code>
 * -Dleveldb.snappy=xerial,iq80
 * </code>
 * <p/>
 * The system property can also be configured with the name of a class which
 * implements the Snappy.SPI interface.
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public final class Snappy
{
    private Snappy() {}

    public static boolean available()
    {
        return true;
    }

    public static ByteBuffer uncompress(ByteBuffer compressed)
            throws IOException
    {
        int uncompressedLength = uncompressedLength(compressed);
        final ByteBuffer uncompressed = ByteBuffer.allocateDirect(uncompressedLength);
        return uncompress(compressed, uncompressed);
    }

    public static ByteBuffer uncompress(ByteBuffer compressed, ByteBuffer uncompressed)
            throws IOException
    {
        byte[] input;
        int inputOffset;
        int length;
        byte[] output;
        int outputOffset;
        if (compressed.hasArray()) {
            input = compressed.array();
            inputOffset = compressed.arrayOffset() + compressed.position();
            length = compressed.remaining();
        } else {
            input = new byte[compressed.remaining()];
            inputOffset = 0;
            length = input.length;
            compressed.mark();
            compressed.get(input);
            compressed.reset();
        }
        if (uncompressed.hasArray()) {
            output = uncompressed.array();
            outputOffset = uncompressed.arrayOffset() + uncompressed.position();
        } else {
            int t = org.iq80.snappy.Snappy.getUncompressedLength(input, inputOffset);
            output = new byte[t];
            outputOffset = 0;
        }

        int count = org.iq80.snappy.Snappy.uncompress(input, inputOffset, length, output, outputOffset);
        if (uncompressed.hasArray()) {
            uncompressed.limit(uncompressed.position() + count);
        } else {
            int p = uncompressed.position();
            uncompressed.limit(uncompressed.capacity());
            uncompressed.put(output, 0, count);
            uncompressed.flip().position(p);
        }
        return uncompressed;
    }

    public static void uncompress(byte[] input, int inputOffset, int length, byte[] output, int outputOffset)
            throws IOException
    {
        org.iq80.snappy.Snappy.uncompress(input, inputOffset, length, output, outputOffset);
    }

    public static int compress(byte[] input, int inputOffset, int length, byte[] output, int outputOffset)
            throws IOException
    {
        return org.iq80.snappy.Snappy.compress(input, inputOffset, length, output, outputOffset);
    }

    private static int uncompressedLength(ByteBuffer data)
    {
        return VariableLengthQuantity.readVariableLengthInt(data.duplicate());
    }
}
