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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonBounds;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.Slot;
import com.ray3k.remotecrashcars.Entity;
import com.ray3k.remotecrashcars.states.GameState;

public class PlayerEntity extends Entity {
    private GameState gameState;
    private Skeleton skeleton;
    private AnimationState animationState;
    private SkeletonBounds skeletonBounds;
    private static final Vector2 temp = new Vector2();
    private Bone rotateBone;
    private float carSpeed;
    private Slot tintSlot;
    private EarClippingTriangulator triangulator;
    private Array<Polygon> polygons;
    private float steerAngle;
    private FloatArray steerAngles;
    private static final int MAX_STEER_ANGLES = 1;
    private boolean hitMiddle;
    private long engineID;
    
    public PlayerEntity (GameState gameState, String skeletonPath) {
        super(gameState.getEntityManager(), gameState.getCore());
        this.gameState = gameState;
        SkeletonData skeletonData = getCore().getAssetManager().get(skeletonPath, SkeletonData.class);
        skeleton = new Skeleton(skeletonData);
        AnimationStateData animationStateData = new AnimationStateData(skeletonData);
        animationStateData.setDefaultMix(.25f);
        animationState = new AnimationState(animationStateData);
        animationState.setAnimation(0, "animation", true);
        
        skeletonBounds = new SkeletonBounds();
        skeletonBounds.update(skeleton, true);
        rotateBone = skeleton.findBone("bone");
        tintSlot = skeleton.findSlot("tint");
        carSpeed = 0.0f;
        
        triangulator = new EarClippingTriangulator();
        polygons = new Array<Polygon>();
        steerAngle = 0.0f;
        steerAngles = new FloatArray();
        hitMiddle = false;
        engineID = gameState.playEngineSound();
    }
    
    @Override
    public void create() {
    }

    @Override
    public void act(float delta) {temp.set(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
        skeleton.setPosition(getX(), getY());
        animationState.update(delta);
        skeleton.updateWorldTransform();
        animationState.apply(skeleton);
        skeletonBounds.update(skeleton, true);

        if (Gdx.input.isKeyPressed(Keys.UP)) {
            carSpeed += gameState.getAcceleration() * delta;
        } else if (Gdx.input.isKeyPressed(Keys.DOWN)) {
            carSpeed -= gameState.getBraking() * delta;
        }
        
        carSpeed -= gameState.getFriction() * delta;
        if (carSpeed < 0) {
            carSpeed = 0;
        } else if (carSpeed > gameState.getMaxSpeed()) {
            carSpeed = gameState.getMaxSpeed();
        }
        
        gameState.adjustEnginePitch(engineID, .5f + .5f * carSpeed / gameState.getMaxSpeed());
        
        if (Gdx.input.isKeyPressed(Keys.LEFT)) {
            steerAngle = approach(steerAngle, gameState.getSteerAngle(), gameState.getSteerAngle() * 2.0f * delta);
        } else if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
            steerAngle = approach(steerAngle, -gameState.getSteerAngle(), gameState.getSteerAngle() * 2.0f * delta);
        } else {
            steerAngle = approach(steerAngle, 0.0f, gameState.getSteerAngle() * 3.0f * delta);
        }
        
        steerAngles.add(steerAngle);
        if (steerAngles.size > MAX_STEER_ANGLES) {
            steerAngles.removeIndex(0);
        }

        float wheelBase = 70.0f;
        
        Vector2 frontWheel = new Vector2(wheelBase / 2.0f, 0.0f);
        frontWheel.rotate(getRotation());
        frontWheel.add(getPosition());
        
        Vector2 backWheel = new Vector2(-wheelBase / 2.0f, 0.0f);
        backWheel.rotate(getRotation());
        backWheel.add(getPosition());
        
        temp.set(carSpeed * delta, 0.0f);
        temp.rotate(getRotation());
        backWheel.add(temp);
        
        temp.set(carSpeed * delta, 0.0f);
        temp.rotate(getRotation() + steerAngles.first());
        frontWheel.add(temp);
        
        setPosition((frontWheel.x + backWheel.x) / 2.0f, (frontWheel.y + backWheel.y) / 2.0f);
        
        temp.set(frontWheel);
        temp.sub(backWheel);
        setRotation(temp.angle());
        rotateBone.setRotation(getRotation() + (steerAngle - steerAngles.first()));
        
        float cameraX = getX();
        if (cameraX < Gdx.graphics.getWidth() / 2.0f) {
            cameraX = Gdx.graphics.getWidth() / 2.0f;
        } else if (cameraX > 2048 - Gdx.graphics.getWidth() / 2.0f) {
            cameraX = 2048 - Gdx.graphics.getWidth() / 2.0f;
        }
        
        float cameraY = getY();
        if (cameraY < Gdx.graphics.getHeight() / 2.0f) {
            cameraY = Gdx.graphics.getHeight() / 2.0f;
        } else if (cameraY > 2048 - Gdx.graphics.getHeight() / 2.0f) {
            cameraY = 2048 - Gdx.graphics.getHeight() / 2.0f;
        }
        
        gameState.getGameCamera().position.set(cameraX, cameraY, 0.0f);
        
        polygons.clear();
        for (FloatArray floats : skeletonBounds.getPolygons()) {
            ShortArray points = triangulator.computeTriangles(floats.items);
            
            for (int i = 0; i < points.size; i += 3) {
                float[] verts = new float[6];
                for (int j = 0; j < 3; j++) {
                    verts[j*2] = floats.get((points.get(i + j) * 2) % floats.size);
                    verts[j*2+1] = floats.get((points.get(i + j) * 2 + 1) % floats.size);
                }
                
                polygons.add(new Polygon(verts));
            }
        }
        
        for (Polygon polygon : gameState.getLevelData().getPolygons()) {
            for (Polygon carPolygon : polygons) {
                if (Intersector.overlapConvexPolygons(polygon, carPolygon)) {
                    dispose();
                    break;
                }
            }
            
            if (isDestroyed()) {
                break;
            }
        }
        
        if (hitMiddle) {
            for(Polygon polygon : polygons) {
                if (gameState.getLevelData().checkCollisionStart(polygon)) {
                    hitMiddle = false;
                    gameState.setTime(0.0f);
                    gameState.playBeepSound();
                    break;
                }
            }
        } else {
            for(Polygon polygon : polygons) {
                if (gameState.getLevelData().checkCollisionMiddle(polygon)) {
                    hitMiddle = true;
                    break;
                }
            }
        }
        
        if (Gdx.input.isKeyPressed(Keys.ESCAPE)) {
            dispose();
        }
    }

    @Override
    public void act_end(float delta) {
    }

    @Override
    public void draw(SpriteBatch spriteBatch, float delta) {
        getCore().getSkeletonRenderer().draw(spriteBatch, skeleton);
    }

    @Override
    public void destroy() {
        new GameOverTimerEntity(gameState, 1.5f);
        ExplosionEntity explosion = new ExplosionEntity(gameState);
        explosion.setPosition(getX(), getY());
        gameState.playExplosionSound();
        gameState.stopEngineSound(engineID);
    }

    @Override
    public void collision(Entity other) {
    }

    public void setColor(Color color) {
        tintSlot.getColor().set(color);
    }
    
    public float approach(float value, float target, float increment) {
        if (target < value) {
            value -= increment;
            if (value < target) {
                value = target;
            }
        } else if (target > value) {
            value += increment;
            if (value > target) {
                value = target;
            }
        }
        return value;
    }
}
