package projectmp.client.background;

import projectmp.client.WorldRenderer;
import projectmp.client.lighting.LightingEngine;
import projectmp.common.Settings;
import projectmp.common.registry.AssetRegistry;
import projectmp.common.util.AssetMap;
import projectmp.common.world.TimeOfDay;
import projectmp.common.world.World;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;

public class Background {

	public static final float SUN_DISTANCE = (Settings.DEFAULT_HEIGHT / 4) * 3;
	public static final float MOON_DISTANCE = SUN_DISTANCE;

	private World world;

	private float fullnessOfBg = 1f;

	/**
	 * used to generate the fade into effect
	 */
	private TimeOfDay currentKnownTOD = TimeOfDay.DAYTIME;
	/**
	 * the time of day BEFORE currentKnownTOD
	 */
	private TimeOfDay lastTOD = TimeOfDay.DAYTIME;

	public Background(World w) {
		this.world = w;
	}

	public void render(WorldRenderer renderer) {
		if (world.time.getCurrentTimeOfDay() != currentKnownTOD) {
			currentKnownTOD = world.time.getCurrentTimeOfDay();
			lastTOD = getTimeOfDayBefore();
			fullnessOfBg = 0;
		}

		if (fullnessOfBg < 1) {
			fullnessOfBg += Gdx.graphics.getDeltaTime() / LightingEngine.TRANSITION_MULTIPLIER_SECS;

			if (fullnessOfBg > 1) fullnessOfBg = 1;
		}

		renderTimeOfDayBackgrounds();

		renderCelestialBodies();

		if (world.getWeather() != null) {
			world.getWeather().renderOverBackground(renderer);
		}
	}

	/**
	 * renders the backgrounds based on the time of day (daytime, evening, night, etc.)
	 */
	private void renderTimeOfDayBackgrounds() {
		// render the time of day that's BEFORE the current one
		lastTOD.renderBackground(world.batch, world);

		// then render the current one on top with an alpha (for the transition)
		world.batch.setColor(1, 1, 1, fullnessOfBg);
		world.time.getCurrentTimeOfDay().renderBackground(world.batch, world);
		world.batch.setColor(1, 1, 1, 1);
	}

	/**
	 * renders the sun and moon
	 */
	private void renderCelestialBodies() {
		SpriteBatch batch = world.batch;

		batch.setColor(1, 1, 1, 1);

		// sun
		Texture tex = AssetRegistry.getTexture("celestialbody_sun");
		float texCenteredX = ((Settings.DEFAULT_WIDTH / 2) - (tex.getWidth() / 2));
		float texCenteredY = (0 - (tex.getWidth() / 2));
		float sunAngle = (360 * (world.time.currentDayTicks * 1f / world.time.ticksPerDay));

		batch.draw(tex, texCenteredX + (SUN_DISTANCE * MathUtils.cosDeg(sunAngle)), texCenteredY
				+ (SUN_DISTANCE * MathUtils.sinDeg(sunAngle)));

		// moon
		tex = AssetRegistry.getTexture("celestialbody_moon");
		texCenteredX = ((Settings.DEFAULT_WIDTH / 2) - (tex.getWidth() / 2));
		texCenteredY = (0 - (tex.getWidth() / 2));
		float moonAngle = sunAngle + 180;

		batch.draw(tex, texCenteredX + (SUN_DISTANCE * MathUtils.cosDeg(moonAngle)), texCenteredY
				+ (SUN_DISTANCE * MathUtils.sinDeg(moonAngle)));
	}

	private TimeOfDay getTimeOfDayBefore() {
		TimeOfDay[] values = TimeOfDay.values();

		int index = 0;
		for (int i = 0; i < values.length; i++) {
			if (world.time.getCurrentTimeOfDay() == values[i]) {
				index = i - 1;
				if (index < 0) index = values.length - 1;
			}
		}

		return values[index];
	}

}
