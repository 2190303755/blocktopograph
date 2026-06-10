package com.mithrilmania.blocktopograph.map.renderer;

import static com.mithrilmania.blocktopograph.editor.world.v2.WorldEditorModelKt.CHUNK_DIMENSION;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mithrilmania.blocktopograph.chunk.Chunk;
import com.mithrilmania.blocktopograph.chunk.Version;
import com.mithrilmania.blocktopograph.world.Dimension;
import com.mithrilmania.blocktopograph.world.WorldStorage;


public class HeightmapRenderer implements MapRenderer {

    public void renderToBitmap(Chunk chunk, Canvas canvas, Dimension dimension, int chunkX, int chunkZ, int pX, int pY, int pW, int pL, Paint paint, WorldStorage storage) throws Version.VersionException {

        Chunk dataW = storage.getChunk(chunkX - 1, chunkZ, dimension);
        Chunk dataN = storage.getChunk(chunkX, chunkZ - 1, dimension);

        boolean west = dataW != null && !dataW.isVoid(),
                north = dataN != null && !dataN.isVoid();

        int x, y, z, color, tX, tY;
        int yW, yN;
        int r, g, b;
        float yNorm, yNorm2, heightShading;

        for (z = 0, tY = pY; z < 16; z++, tY += pL) {
            for (x = 0, tX = pX; x < 16; x++, tX += pW) {


                //smooth step function: 6x^5 - 15x^4 + 10x^3
                y = chunk.getHeightMapValue(x, z);

                if (y < 0) continue;

                yNorm = (float) y / (float) CHUNK_DIMENSION;
                yNorm2 = yNorm * yNorm;
                yNorm = ((6f * yNorm2) - (15f * yNorm) + 10f) * yNorm2 * yNorm;

                yW = (x == 0) ? (west ? dataW.getHeightMapValue(CHUNK_DIMENSION - 1, z) : y)//chunk edge
                        : chunk.getHeightMapValue(x - 1, z);//within chunk
                yN = (z == 0) ? (north ? dataN.getHeightMapValue(x, CHUNK_DIMENSION - 1) : y)//chunk edge
                        : chunk.getHeightMapValue(x, z - 1);//within chunk

                heightShading = SatelliteRenderer.getHeightShading(y, yW, yN);


                r = (int) (yNorm * heightShading * 256f);
                g = (int) (70f * heightShading);
                b = (int) (256f * (1f - yNorm) / (yNorm + 1f));

                r = r < 0 ? 0 : r > 255 ? 255 : r;
                g = g < 0 ? 0 : g > 255 ? 255 : g;
                b = b < 0 ? 0 : b > 255 ? 255 : b;


                color = (r << 16) | (g << 8) | b | 0xff000000;

                paint.setColor(color);
                canvas.drawRect(tX, tY, tX + pW, tY + pL, paint);


            }
        }
    }

}
