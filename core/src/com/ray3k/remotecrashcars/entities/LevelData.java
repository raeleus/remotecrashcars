/*
 * The MIT License
 *
 * Copyright 2017 Raymond Buckley.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ray3k.remotecrashcars.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonBounds;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.attachments.PointAttachment;
import com.ray3k.remotecrashcars.Core;
import com.ray3k.remotecrashcars.states.GameState;

public class LevelData {
    private Skeleton skeleton;
    private AnimationState animationState;
    private SkeletonBounds skeletonBounds;
    private EarClippingTriangulator triangulator;
    private Array<Polygon> polygons;
    private PointAttachment playerSpawnPoint;
    private Vector2 start1;
    private Vector2 start2;
    private Vector2 middle1;
    private Vector2 middle2;

    public LevelData(GameState gameState, Core core, String trackPath) {
        SkeletonData skeletonData = core.getAssetManager().get(trackPath, SkeletonData.class);
        skeleton = new Skeleton(skeletonData);
        AnimationStateData animationStateData = new AnimationStateData(skeletonData);
        animationStateData.setDefaultMix(.25f);
        animationState = new AnimationState(animationStateData);
        animationState.setAnimation(0, "animation", true);
        
        skeletonBounds = new SkeletonBounds();
        skeleton.setPosition(0.0f, 0.0f);
        skeleton.updateWorldTransform();
        animationState.apply(skeleton);
        skeletonBounds.update(skeleton, true);
        
        triangulator = new EarClippingTriangulator();
        polygons = new Array<Polygon>();
        for (FloatArray floats : skeletonBounds.getPolygons()) {
            ShortArray points = triangulator.computeTriangles(floats.items);
            
            for (int i = 0; i < points.size; i += 3) {
                float[] verts = new float[6];
                for (int j = 0; j < 3; j++) {
                    verts[j*2] = floats.get(points.get(i + j) * 2);
                    verts[j*2+1] = floats.get(points.get(i + j) * 2 + 1);
                }
                
                polygons.add(new Polygon(verts));
            }
        }
        
        playerSpawnPoint = (PointAttachment) skeleton.getAttachment("player", "player");
    
        PointAttachment start1 = (PointAttachment) skeleton.getAttachment("start1", "start1");
        PointAttachment start2 = (PointAttachment) skeleton.getAttachment("start2", "start2");
        this.start1 = new Vector2(start1.getX(), start1.getY());
        this.start2 = new Vector2(start2.getX(), start2.getY());
        
        PointAttachment middle1 = (PointAttachment) skeleton.getAttachment("middle1", "middle1");
        PointAttachment middle2 = (PointAttachment) skeleton.getAttachment("middle2", "middle2");
        this.middle1 = new Vector2(middle1.getX(), middle1.getY());
        this.middle2 = new Vector2(middle2.getX(), middle2.getY());
    }

    public Array<Polygon> getPolygons() {
        return polygons;
    }
    
    public void spawnPlayer(GameState gameState, Color color, String skeletonPath) {
        PlayerEntity playerEntity = new PlayerEntity(gameState, skeletonPath);
        playerEntity.setPosition(playerSpawnPoint.getX(), playerSpawnPoint.getY());
        playerEntity.setRotation(playerSpawnPoint.getRotation());
        playerEntity.setColor(color);
    }
    
    public boolean checkCollisionStart(Polygon polygon) {
        return Intersector.intersectSegmentPolygon(start1, start2, polygon);
    }
    
    public boolean checkCollisionMiddle(Polygon polygon) {
        return Intersector.intersectSegmentPolygon(middle1, middle2, polygon);
    }
}
