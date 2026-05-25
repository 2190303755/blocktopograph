package com.mithrilmania.blocktopograph.map.renderer;

import static com.mithrilmania.blocktopograph.editor.world.v2.WorldEditorModelKt.CHUNK_DIMENSION;

import android.graphics.Canvas;
import android.graphics.Paint;

import com.mithrilmania.blocktopograph.chunk.Chunk;
import com.mithrilmania.blocktopograph.chunk.Version;
import com.mithrilmania.blocktopograph.map.Dimension;
import com.mithrilmania.blocktopograph.world.WorldStorage;


public class ChessPatternRenderer implements MapRenderer {

    public final int darkShade, lightShade;// int DARK_SHADE = 0xFF2B2B2B, LIGHT_SHADE = 0xFF585858;

    ChessPatternRenderer(int darkShade, int lightShade) {
        this.darkShade = darkShade;
        this.lightShade = lightShade;
    }

    public void renderToBitmap(Chunk chunk, Canvas canvas, Dimension dimension, int chunkX, int chunkZ, int pX, int pY, int pW, int pL, Paint paint, WorldStorage storage) throws Version.VersionException {
        paint.setColor(this.lightShade);
        canvas.drawRect(pX, pY, pX + CHUNK_DIMENSION * pW, pY + CHUNK_DIMENSION * pL, paint);
        paint.setColor(this.darkShade);
        int step = pW * 2;
        int steps = CHUNK_DIMENSION / 2;
        int x, z, tX, tY;

        for (z = 0, tY = pY; z < CHUNK_DIMENSION; z++, tY += pL) {
            for (x = 0, tX = ((z & 1) == 0 ? pX : pX + pW); x < steps; x++, tX += step) {
                canvas.drawRect(tX, tY, tX + pW, tY + pL, paint);
            }
        }
    }
}
