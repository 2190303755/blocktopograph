package com.mithrilmania.blocktopograph.map.renderer;

import static com.mithrilmania.blocktopograph.editor.world.v2.WorldEditorModelKt.CHUNK_INDICES;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mithrilmania.blocktopograph.chunk.Chunk;
import com.mithrilmania.blocktopograph.chunk.Version;
import com.mithrilmania.blocktopograph.util.MTwister;
import com.mithrilmania.blocktopograph.world.Dimension;
import com.mithrilmania.blocktopograph.world.WorldStorage;


public class SlimeChunkRenderer implements MapRenderer {

    public void renderToBitmap(Chunk chunk, Canvas canvas, Dimension dimension, int chunkX, int chunkZ, int pX, int pY, int pW, int pL, Paint paint, WorldStorage storage) throws Version.VersionException {

        int x, z, tX, tY;

        Chunk dataW = storage.getChunk(chunkX - 1, chunkZ, dimension);
        Chunk dataN = storage.getChunk(chunkX, chunkZ - 1, dimension);

        boolean west = dataW != null && !dataW.isVoid(),
                north = dataN != null && !dataN.isVoid();

        //MapType.OVERWORLD_SATELLITE.renderer.renderToBitmap(chunk, canvas, dimension, chunkX, chunkZ, pX, pY, pW, pL, paint, version, chunkManager);

        boolean isSlimeChunk = isSlimeChunk(chunkX, chunkZ);
        int color, r, g, b, avg;

        //make slimeChunks much more green
        for (z = 0, tY = pY; z < 16; z++, tY += pL) {
            for (x = 0, tX = pX; x < 16; x++, tX += pW) {

                int y = chunk.getHeightMapValue(x, z);

                color = SatelliteRenderer.getColumnColour(chunk, x, y, z,
                        (x == 0) ? (west ? dataW.getHeightMapValue(CHUNK_INDICES, z) : y)//chunk edge
                                : chunk.getHeightMapValue(x - 1, z),//within chunk
                        (z == 0) ? (north ? dataN.getHeightMapValue(x, CHUNK_INDICES) : y)//chunk edge
                                : chunk.getHeightMapValue(x, z - 1)//within chunk
                );
                r = (color >> 16) & 0xff;
                g = (color >> 8) & 0xff;
                b = color & 0xff;
                avg = (r + g + b) / 3;
                if (isSlimeChunk) {
                    r = b = avg;
                    g = (g + 0xff) >> 1;
                } else {
                    r = g = b = avg;
                }
                color = (color & 0xFF000000) | (r << 16) | (g << 8) | b;

                paint.setColor(color);
                canvas.drawRect(tX, tY, tX + pW, tY + pL, paint);

            }
        }

    }


    // See: https://gist.github.com/mithrilmania/00b85bf34a75fd8176342b1ad28bfccc
    private static boolean isSlimeChunk(int cX, int cZ) {
        //
        // MCPE slime-chunk checker
        // From Minecraft: Pocket Edition 0.15.0 (0.15.0.50_V870150050)
        // Reverse engineered by @protolambda and @jocopa3
        //
        // NOTE:
        // - The world-seed doesn't seem to be incorporated into the randomness, which is very odd.
        //   This means that every world has its slime-chunks in the exact same chunks!
        //   This is not officially confirmed yet.
        // - Reverse engineering this code cost a lot of time,
        //   please add CREDITS when you are copying this.
        //   Copy the following into your program source:
        //     MCPE slime-chunk checker; reverse engineered by @protolambda and @jocopa3
        //

        // chunkX/Z are the chunk-coordinates, used in the DB keys etc.
        // Unsigned int32, using 64 bit longs to work-around the sign issue.
        long chunkX_uint = cX & 0xffffffffL;
        long chunkZ_uint = cZ & 0xffffffffL;

        // Combine X and Z into a 32 bit int (again, long to work around sign issue)
        long seed = (chunkX_uint * 0x1f1f1f1fL) ^ chunkZ_uint;

        // The random function MCPE uses, not the same as MCPC!
        // This is a Mersenne Twister; MT19937 by Takuji Nishimura and Makoto Matsumoto.
        // Java adaption source: http://dybfin.wustl.edu/teaching/compufinj/MTwister.java
        MTwister random = new MTwister();
        random.init_genrand(seed);

        long n = random.genrand_int32();

        // Final check: is the input equal to 10 times less random, but comparable, output.
        // Every chunk has a 1 in 10 chance to be a slime-chunk.

        return 0 == n % 10; // let's trust jit/aot :)
    }

}