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
package com.ray3k.remotecrashcars.states;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.ray3k.remotecrashcars.Core;
import com.ray3k.remotecrashcars.EntityManager;
import com.ray3k.remotecrashcars.InputManager;
import com.ray3k.remotecrashcars.State;
import com.ray3k.remotecrashcars.entities.LevelData;

public class GameState extends State {
    private float time;
    private static float bestTime;
    private OrthographicCamera gameCamera;
    private Viewport gameViewport;
    private OrthographicCamera uiCamera;
    private Viewport uiViewport;
    private InputManager inputManager;
    private Skin skin;
    private Stage stage;
    private Label timeLabel;
    private EntityManager entityManager;
    private Texture trackBackground;
    private LevelData levelData;
    private Color playerColor;
    private Model model;
    private Track track;
    private float acceleration;
    private float maxSpeed;
    private float friction;
    private float braking;
    private float steerAngle;
    
    public static enum Model {
        CAR, BIKE
    }
    
    public static enum Track {
        CIRCLE, PILL, HARD
    }
    
    public GameState(Core core) {
        super(core);
        
        acceleration = 500.0f;
        maxSpeed = 500.0f;
        friction = 200.0f;
        braking = 375.0f;
        steerAngle = 25.0f;
    }
    
    @Override
    public void start() {
        time = 0;
        bestTime = 999;
        
        inputManager = new InputManager(); 
        
        uiCamera = new OrthographicCamera();
        uiViewport = new ScreenViewport(uiCamera);
        uiViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        uiViewport.apply();
        
        uiCamera.position.set(uiCamera.viewportWidth / 2, uiCamera.viewportHeight / 2, 0);
        
        gameCamera = new OrthographicCamera();
        gameViewport = new ScreenViewport(gameCamera);
        gameViewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        gameViewport.apply();
        
        skin = getCore().getAssetManager().get(Core.DATA_PATH + "/ui/remote-crash-cars.json", Skin.class);
        stage = new Stage(new ScreenViewport());
        
        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(inputManager);
        inputMultiplexer.addProcessor(stage);
        Gdx.input.setInputProcessor(inputMultiplexer);
        
        entityManager = new EntityManager();
        
        gameCamera.position.set(Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f, 0);
        createStageElements();
        
        if (track == Track.CIRCLE) {
            getCore().getAssetManager().load(Core.DATA_PATH + "/gfx/track-circle.png", Texture.class);
            getCore().getAssetManager().finishLoading();
            trackBackground = getCore().getAssetManager().get(Core.DATA_PATH + "/gfx/track-circle.png", Texture.class);
            levelData = new LevelData(this, getCore(), Core.DATA_PATH + "/spine/track-circle.json");
        } else if (track == Track.PILL) {
            getCore().getAssetManager().load(Core.DATA_PATH + "/gfx/track-pill.png", Texture.class);
            getCore().getAssetManager().finishLoading();
            trackBackground = getCore().getAssetManager().get(Core.DATA_PATH + "/gfx/track-pill.png", Texture.class);
            levelData = new LevelData(this, getCore(), Core.DATA_PATH + "/spine/track-pill.json");
        } else {
            getCore().getAssetManager().load(Core.DATA_PATH + "/gfx/track-hard.png", Texture.class);
            getCore().getAssetManager().finishLoading();
            trackBackground = getCore().getAssetManager().get(Core.DATA_PATH + "/gfx/track-hard.png", Texture.class);
            levelData = new LevelData(this, getCore(), Core.DATA_PATH + "/spine/track-hard.json");
        }
        
        if (model == Model.CAR) {
            levelData.spawnPlayer(this, playerColor, Core.DATA_PATH + "/spine/car.json");
        } else {
            levelData.spawnPlayer(this, playerColor, Core.DATA_PATH + "/spine/bike.json");
        }
        
        playBeepSound();
    }
    
    private void createStageElements() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);
        
        timeLabel = new Label("0", skin, "button");
        timeLabel.setAlignment(Align.left);
        root.add(timeLabel).expand().top().width(100.0f);
    }
    
    @Override
    public void draw(SpriteBatch spriteBatch, float delta) {
        Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        gameCamera.update();
        spriteBatch.setProjectionMatrix(gameCamera.combined);
        spriteBatch.begin();
        spriteBatch.draw(trackBackground, 0.0f, 0.0f);
        spriteBatch.end();
        
        stage.draw();
        
        spriteBatch.setProjectionMatrix(gameCamera.combined);
        spriteBatch.begin();
        entityManager.draw(spriteBatch, delta);
        spriteBatch.end();
    }

    @Override
    public void act(float delta) {
        entityManager.act(delta);
        
        stage.act(delta);
        
        time += delta;
        timeLabel.setText(Integer.toString((int) time) + "." + Integer.toString((int) (10 * (time % 1))));
    }

    @Override
    public void dispose() {
        
    }

    @Override
    public void stop() {
        stage.dispose();
        if (getCore().getAssetManager().isLoaded(Core.DATA_PATH + "/gfx/track-circle.png")) {
            getCore().getAssetManager().unload(Core.DATA_PATH + "/gfx/track-circle.png");
        } else if (getCore().getAssetManager().isLoaded(Core.DATA_PATH + "/gfx/track-pill.png")) {
            getCore().getAssetManager().unload(Core.DATA_PATH + "/gfx/track-pill.png");
        } else if (getCore().getAssetManager().isLoaded(Core.DATA_PATH + "/gfx/track-hard.png")) {
            getCore().getAssetManager().unload(Core.DATA_PATH + "/gfx/track-hard.png");
        }
    }
    
    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height);
        gameCamera.position.set(width / 2, height / 2.0f, 0.0f);
        
        uiViewport.update(width, height);
        uiCamera.position.set(uiCamera.viewportWidth / 2, uiCamera.viewportHeight / 2, 0);
        stage.getViewport().update(width, height, true);
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public InputManager getInputManager() {
        return inputManager;
    }

    public float getTime() {
        return time;
    }

    public void setTime(float time) {
        if (this.time < bestTime) {
            bestTime = this.time;
        }
        this.time = time;
        timeLabel.setText(Float.toString(time));
    }

    public static float getBestTime() {
        return bestTime;
    }
    
    public void playBeepSound() {
        getCore().getAssetManager().get(Core.DATA_PATH + "/sfx/beep.wav", Sound.class).play(.5f);
    }
    
    public long playEngineSound() {
        return getCore().getAssetManager().get(Core.DATA_PATH + "/sfx/engine.wav", Sound.class).loop(.2f);
    }
    
    public void adjustEnginePitch(long soundID, float pitch) {
        getCore().getAssetManager().get(Core.DATA_PATH + "/sfx/engine.wav", Sound.class).setPitch(soundID, pitch);
    }
    
    public void stopEngineSound(long soundID) {
        getCore().getAssetManager().get(Core.DATA_PATH + "/sfx/engine.wav", Sound.class).stop(soundID);
    }
    
    public void playExplosionSound() {
        getCore().getAssetManager().get(Core.DATA_PATH + "/sfx/explosion.wav", Sound.class).play(.5f);
    }

    public OrthographicCamera getGameCamera() {
        return gameCamera;
    }

    public void setGameCamera(OrthographicCamera gameCamera) {
        this.gameCamera = gameCamera;
    }

    public Skin getSkin() {
        return skin;
    }

    public Stage getStage() {
        return stage;
    }

    public LevelData getLevelData() {
        return levelData;
    }

    public Color getPlayerColor() {
        return playerColor;
    }

    public void setPlayerColor(Color playerColor) {
        this.playerColor = playerColor;
    }

    public Model getModel() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public float getAcceleration() {
        return acceleration;
    }

    public void setAcceleration(float acceleration) {
        this.acceleration = acceleration;
    }

    public float getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public float getFriction() {
        return friction;
    }

    public void setFriction(float friction) {
        this.friction = friction;
    }

    public float getBraking() {
        return braking;
    }

    public void setBraking(float braking) {
        this.braking = braking;
    }

    public float getSteerAngle() {
        return steerAngle;
    }

    public void setSteerAngle(float steerAngle) {
        this.steerAngle = steerAngle;
    }
}