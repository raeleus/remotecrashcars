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
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageTextButton.ImageTextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.ray3k.remotecrashcars.Core;
import com.ray3k.remotecrashcars.State;

public class MenuState extends State {
    private Stage stage;
    private Skin skin;
    private Table root;

    public MenuState(Core core) {
        super(core);
    }
    
    @Override
    public void start() {
        skin = getCore().getAssetManager().get(Core.DATA_PATH + "/ui/remote-crash-cars.json", Skin.class);
        stage = new Stage(new ScreenViewport());
        
        Gdx.input.setInputProcessor(stage);
        
        createMenu();
    }
    
    private void createMenu() {
        FileHandle fileHandle = Gdx.files.local(Core.DATA_PATH + "/data.json");
        JsonReader reader = new JsonReader();
        JsonValue val = reader.parse(fileHandle);
        
        root = new Table();
        root.setFillParent(true);
        stage.addActor(root);
        
        Label label = new Label(val.getString("title"), skin, "title");
        label.setAlignment(Align.center);
        root.add(label).padBottom(50.0f).padTop(100.0f);
        
        root.defaults().space(30.0f).padLeft(25.0f);
        root.row();
        TextButton textButtton = new TextButton("Play", skin);
        root.add(textButtton).expand().bottom();
        
        textButtton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                getCore().getAssetManager().get(Core.DATA_PATH + "/sfx/beep.wav", Sound.class).play(.25f);
                showVehicleDialog();
            }
        });
        
        root.row();
        textButtton = new TextButton("Quit", skin);
        root.add(textButtton).expand().top();
        
        textButtton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                getCore().getAssetManager().get(Core.DATA_PATH + "/sfx/beep.wav", Sound.class).play(.5f);
                Gdx.app.exit();
            }
        });
    }
    
    @Override
    public void draw(SpriteBatch spriteBatch, float delta) {
        Gdx.gl.glClearColor(0, .407f, .215f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.draw();
    }

    @Override
    public void act(float delta) {
        stage.act(delta);
    }

    @Override
    public void dispose() {
        
    }

    @Override
    public void stop() {
        stage.dispose();
    }
    
    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }
    
    public void showVehicleDialog() {
        final ButtonGroup buttonGroup = new ButtonGroup();
        Dialog dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
                GameState gameState = (GameState) getCore().getStateManager().getState("game");
                gameState.setPlayerColor((Color) buttonGroup.getChecked().getUserObject());
                if (buttonGroup.getCheckedIndex() < 3) {
                    gameState.setModel(GameState.Model.CAR);
                } else {
                    gameState.setModel(GameState.Model.BIKE);
                }
                showTrackDialog();
            }
        };
        
        Table table = new Table();
        ScrollPane scrollPane = new ScrollPane(table, skin);
        scrollPane.setFadeScrollBars(false);
        dialog.getContentTable().add(scrollPane).grow();
        
        table.defaults().padLeft(50.0f).padRight(50.0f).growX();
        ImageTextButtonStyle style = new ImageTextButtonStyle(skin.get(ImageTextButtonStyle.class));
        style.imageUp = skin.getDrawable("thumb-car-2");
        ImageTextButton imageTextButton = new ImageTextButton("Night", style);
        imageTextButton.setUserObject(new Color(30 / 255.0f, 30 / 255.0f, 30 / 255.0f, 1.0f));
        imageTextButton.getLabelCell().expandX().left().padLeft(20.0f);
        buttonGroup.add(imageTextButton);
        table.add(imageTextButton);
        
        table.row();
        style = new ImageTextButtonStyle(skin.get(ImageTextButtonStyle.class));
        style.imageUp = skin.getDrawable("thumb-car-1");
        imageTextButton = new ImageTextButton("Speedster", style);
        imageTextButton.setUserObject(new Color(127 / 255.0f, 0 / 255.0f, 0 / 255.0f, 1.0f));
        imageTextButton.getLabelCell().expandX().left().padLeft(20.0f);
        buttonGroup.add(imageTextButton);
        table.add(imageTextButton);
        
        table.row();
        style = new ImageTextButtonStyle(skin.get(ImageTextButtonStyle.class));
        style.imageUp = skin.getDrawable("thumb-car-3");
        imageTextButton = new ImageTextButton("Sporty", style);
        imageTextButton.setUserObject(new Color(188 / 255.0f, 118 / 255.0f, 255 / 255.0f, 1.0f));
        imageTextButton.getLabelCell().expandX().left().padLeft(20.0f);
        buttonGroup.add(imageTextButton);
        table.add(imageTextButton);
        
        table.row();
        style = new ImageTextButtonStyle(skin.get(ImageTextButtonStyle.class));
        style.imageUp = skin.getDrawable("thumb-bike-1");
        imageTextButton = new ImageTextButton("Flamingo", style);
        imageTextButton.setUserObject(new Color(255 / 255.0f, 118 / 255.0f, 174 / 255.0f, 1.0f));
        imageTextButton.getLabelCell().expandX().left().padLeft(20.0f);
        buttonGroup.add(imageTextButton);
        table.add(imageTextButton);
        
        table.row();
        style = new ImageTextButtonStyle(skin.get(ImageTextButtonStyle.class));
        style.imageUp = skin.getDrawable("thumb-bike-2");
        imageTextButton = new ImageTextButton("Smoke", style);
        imageTextButton.setUserObject(new Color(211 / 255.0f, 211 / 255.0f, 211 / 255.0f, 1.0f));
        imageTextButton.getLabelCell().expandX().left().padLeft(20.0f);
        buttonGroup.add(imageTextButton);
        table.add(imageTextButton);
        
        table.row();
        style = new ImageTextButtonStyle(skin.get(ImageTextButtonStyle.class));
        style.imageUp = skin.getDrawable("thumb-bike-3");
        imageTextButton = new ImageTextButton("Lime", style);
        imageTextButton.setUserObject(new Color(0 / 255.0f, 255 / 255.0f, 15 / 255.0f, 1.0f));
        imageTextButton.getLabelCell().expandX().left().padLeft(20.0f);
        buttonGroup.add(imageTextButton);
        table.add(imageTextButton);
        
        dialog.button("OK");
        
        dialog.show(stage);
        dialog.setWidth(600.0f);
        dialog.setHeight(600.0f);
        dialog.setPosition(Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f, Align.center);
        stage.setScrollFocus(scrollPane);
    }
    
    public void showTrackDialog() {
        final ButtonGroup buttonGroup = new ButtonGroup();
        Dialog dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
                GameState gameState = (GameState) getCore().getStateManager().getState("game");
                gameState.setTrack((GameState.Track) buttonGroup.getChecked().getUserObject());
                showSettingsDialog();
            }
        };
        
        Table table = new Table();
        ScrollPane scrollPane = new ScrollPane(table, skin);
        scrollPane.setFadeScrollBars(false);
        dialog.getContentTable().add(scrollPane).grow();
        
        table.defaults().padLeft(50.0f).padRight(50.0f).growX();
        ImageTextButtonStyle style = new ImageTextButtonStyle(skin.get(ImageTextButtonStyle.class));
        style.imageUp = skin.getDrawable("thumb-track-circle");
        ImageTextButton imageTextButton = new ImageTextButton("Circle", style);
        imageTextButton.setUserObject(GameState.Track.CIRCLE);
        imageTextButton.getLabelCell().expandX().left().padLeft(20.0f);
        buttonGroup.add(imageTextButton);
        table.add(imageTextButton);
        
        table.row();
        style = new ImageTextButtonStyle(skin.get(ImageTextButtonStyle.class));
        style.imageUp = skin.getDrawable("thumb-track-pill");
        imageTextButton = new ImageTextButton("Pill", style);
        imageTextButton.setUserObject(GameState.Track.PILL);
        imageTextButton.getLabelCell().expandX().left().padLeft(20.0f);
        buttonGroup.add(imageTextButton);
        table.add(imageTextButton);
        
        table.row();
        style = new ImageTextButtonStyle(skin.get(ImageTextButtonStyle.class));
        style.imageUp = skin.getDrawable("thumb-track-hard");
        imageTextButton = new ImageTextButton("Hard", style);
        imageTextButton.setUserObject(GameState.Track.HARD);
        imageTextButton.getLabelCell().expandX().left().padLeft(20.0f);
        buttonGroup.add(imageTextButton);
        table.add(imageTextButton);
        
        dialog.button("OK");
        
        dialog.show(stage);
        dialog.setWidth(600.0f);
        dialog.setHeight(600.0f);
        dialog.setPosition(Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f, Align.center);
        stage.setScrollFocus(scrollPane);
    }
    
    public void showSettingsDialog() {
        Dialog dialog = new Dialog("", skin) {
            @Override
            protected void result(Object object) {
                getCore().getStateManager().loadState("game");
            }
        };
        
        final GameState gameState = (GameState) getCore().getStateManager().getState("game");
        Table table = new Table();
        ScrollPane scrollPane = new ScrollPane(table, skin);
        scrollPane.setFadeScrollBars(false);
        dialog.getContentTable().add(scrollPane).grow();
        
        Label label = new Label("Acceleration", skin);
        table.add(label).colspan(2);
        
        table.row();
        Slider slider = new Slider(250, 1500, 1, false, skin);
        final Label accelerationLabel = new Label(Float.toString(slider.getValue()), skin);
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                gameState.setAcceleration(((Slider) actor).getValue());
                accelerationLabel.setText(Float.toString(((Slider) actor).getValue()));
            }
        });
        slider.setValue(gameState.getAcceleration());
        table.add(slider).growX();
        
        accelerationLabel.setAlignment(Align.center);
        table.add(accelerationLabel).width(100.0f);
        
        table.row();
        label = new Label("Max Speed", skin);
        table.add(label).colspan(2).padTop(15.0f);
        
        table.row();
        slider = new Slider(50, 1500, 1, false, skin);
        final Label maxSpeedLabel = new Label(Float.toString(slider.getValue()), skin);
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                gameState.setMaxSpeed(((Slider) actor).getValue());
                maxSpeedLabel.setText(Float.toString(((Slider) actor).getValue()));
            }
        });
        slider.setValue(gameState.getMaxSpeed());
        table.add(slider).growX();
        
        maxSpeedLabel.setAlignment(Align.center);
        table.add(maxSpeedLabel).width(100.0f);;
        
        table.row();
        label = new Label("Friction", skin);
        table.add(label).colspan(2).padTop(15.0f);
        
        table.row();
        slider = new Slider(0, 400, 1, false, skin);
        final Label frictionLabel = new Label(Float.toString(slider.getValue()), skin);
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                gameState.setFriction(((Slider) actor).getValue());
                frictionLabel.setText(Float.toString(((Slider) actor).getValue()));
            }
        });
        slider.setValue(gameState.getFriction());
        table.add(slider).growX();
        
        frictionLabel.setAlignment(Align.center);
        table.add(frictionLabel).width(100.0f);;
        
        table.row();
        label = new Label("Braking Deceleration", skin);
        table.add(label).colspan(2).padTop(15.0f);
        
        table.row();
        slider = new Slider(10, 750, 1, false, skin);
        final Label brakingLabel = new Label(Float.toString(slider.getValue()), skin);
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                gameState.setBraking(((Slider) actor).getValue());
                brakingLabel.setText(Float.toString(((Slider) actor).getValue()));
            }
        });
        slider.setValue(gameState.getBraking());
        table.add(slider).growX();
        
        brakingLabel.setAlignment(Align.center);
        table.add(brakingLabel).width(100.0f);;
        
        table.row();
        label = new Label("Steer Angle", skin);
        table.add(label).colspan(2).padTop(15.0f);
        
        table.row();
        slider = new Slider(0, 50, 1, false, skin);
        final Label steerAngleLabel = new Label(Float.toString(slider.getValue()), skin);
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, Actor actor) {
                gameState.setSteerAngle(((Slider) actor).getValue());
                steerAngleLabel.setText(Float.toString(((Slider) actor).getValue()));
            }
        });
        slider.setValue(gameState.getSteerAngle());
        table.add(slider).growX();
        
        steerAngleLabel.setAlignment(Align.center);
        table.add(steerAngleLabel).width(100.0f);;
        
        dialog.button("OK");
        
        dialog.show(stage);
        dialog.setWidth(600.0f);
        dialog.setHeight(600.0f);
        dialog.setPosition(Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f, Align.center);
        stage.setScrollFocus(scrollPane);
    }
}