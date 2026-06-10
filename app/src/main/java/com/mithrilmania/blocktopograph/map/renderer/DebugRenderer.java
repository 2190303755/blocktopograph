package com.mithrilmania.blocktopograph.map.renderer;

import static com.mithrilmania.blocktopograph.editor.world.v2.WorldEditorModelKt.CHUNK_DIMENSION;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mithrilmania.blocktopograph.chunk.Chunk;
import com.mithrilmania.blocktopograph.chunk.Version;
import com.mithrilmania.blocktopograph.world.Dimension;
import com.mithrilmania.blocktopograph.world.WorldStorage;


public class DebugRenderer implements MapRenderer {

    public void renderToBitmap(Chunk chunk, Canvas canvas, Dimension dimension, int chunkX, int chunkZ, int pX, int pY, int pW, int pL, Paint paint, WorldStorage storage) throws Version.VersionException {

        int x, z, i, j, tX, tY;

        int offsetX = chunkX * CHUNK_DIMENSION;
        int offsetZ = chunkZ * CHUNK_DIMENSION;

        for (z = 0, tY = pY; z < 16; z++, tY += pL) {
            for (x = 0, tX = pX; x < 16; x++, tX += pW) {
                for (i = 0; i < pL; i++) {
                    for (j = 0; j < pW; j++) {
                        paint.setColor(0xff000000 | ((offsetX + x) ^ (offsetZ + z)));
                        canvas.drawPoint(tX + j, tY + i, paint);
                    }
                }
            }
        }
    }


}