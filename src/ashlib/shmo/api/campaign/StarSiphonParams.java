package ashlib.shmo.api.campaign;

import ashlib.shmo.api.general.SoundParams;

import java.awt.*;
import java.util.List;

   public final class StarSiphonParams {
      private String       backgroundSpriteName = "";
      private Color        backgroundTint       = Color.WHITE;
      private String       midgroundSpriteName  = "";
      private Color        midgroundTint        = Color.WHITE;
      private String       foregroundSpriteName = "";
      private Color        foregroundTint       = Color.WHITE;
      private String       flareSpriteName      = "";
      private Color        flareTint            = Color.WHITE;
      private String       flashSpriteName      = "";
      private Color        flashTint            = Color.WHITE;
      private List<String> particleSpriteNames  = List.of();
      private Color        particleTint         = Color.WHITE;
      private SoundParams  inSound              = SoundParams.create();
      private SoundParams  outSound             = SoundParams.create();
      private SoundParams  loopSound            = SoundParams.create();

      public StarSiphonParams() {}

      public StarSiphonParams(
              String       backgroundSpriteName,
              Color        backgroundTint,
              String       midgroundSpriteName,
              Color        midgroundTint,
              String       foregroundSpriteName,
              Color        foregroundTint,
              String       flareSpriteName,
              Color        flareTint,
              String       flashSpriteName,
              Color        flashTint,
              List<String> particleSpriteNames,
              Color        particleTint,
              SoundParams  inSound,
              SoundParams  outSound,
              SoundParams  loopSound
      ) {
         this.backgroundSpriteName = backgroundSpriteName;
         this.backgroundTint = backgroundTint;
         this.midgroundSpriteName = midgroundSpriteName;
         this.midgroundTint = midgroundTint;
         this.foregroundSpriteName = foregroundSpriteName;
         this.foregroundTint = foregroundTint;
         this.flareSpriteName = flareSpriteName;
         this.flareTint = flareTint;
         this.flashSpriteName = flashSpriteName;
         this.flashTint = flashTint;
         this.particleSpriteNames = particleSpriteNames;
         this.particleTint = particleTint;
         this.inSound = inSound;
         this.outSound = outSound;
         this.loopSound = loopSound;
      }

      public StarSiphonParams(StarSiphonParams other) {
         backgroundSpriteName = other.backgroundSpriteName;
         backgroundTint = other.backgroundTint;
         midgroundSpriteName = other.midgroundSpriteName;
         midgroundTint = other.midgroundTint;
         foregroundSpriteName = other.foregroundSpriteName;
         foregroundTint = other.foregroundTint;
         flareSpriteName = other.flareSpriteName;
         flareTint = other.flareTint;
         flashSpriteName = other.flashSpriteName;
         flashTint = other.flashTint;
         particleSpriteNames = other.particleSpriteNames;
         particleTint = other.particleTint;
         inSound = other.inSound;
         outSound = other.outSound;
         loopSound = other.loopSound;
      }

      private StarSiphonParams copy() {
         return new StarSiphonParams(this);
      }

      public static StarSiphonParams create() {
         return new StarSiphonParams();
      }

      public StarSiphonParams withSprites(
              String backgroundSpriteName,
              String midgroundSpriteName,
              String foregroundSpriteName,
              String flareSpriteName,
              String flashSpriteName
      ) {
         StarSiphonParams newParams = copy();
         newParams.backgroundSpriteName = backgroundSpriteName;
         newParams.midgroundSpriteName = midgroundSpriteName;
         newParams.foregroundSpriteName = foregroundSpriteName;
         newParams.flareSpriteName = flareSpriteName;
         newParams.flashSpriteName = flashSpriteName;
         return newParams;
      }

      public StarSiphonParams withParticleSprites(List<String> particleSpriteNames) {
         StarSiphonParams newParams = copy();
         newParams.particleSpriteNames = particleSpriteNames;
         return newParams;
      }

      public StarSiphonParams withSounds(SoundParams inSound, SoundParams outSound, SoundParams loopSound) {
         StarSiphonParams newParams = copy();
         newParams.inSound = inSound;
         newParams.outSound = outSound;
         newParams.loopSound = loopSound;
         return newParams;
      }

      public StarSiphonParams withBackgroundTint(Color backgroundTint) {
         StarSiphonParams newParams = copy();
         newParams.backgroundTint = backgroundTint;
         return newParams;
      }

      public StarSiphonParams withMidgroundTint(Color midgroundTint) {
         StarSiphonParams newParams = copy();
         newParams.midgroundTint = midgroundTint;
         return newParams;
      }

      public StarSiphonParams withForegroundTint(Color foregroundTint) {
         StarSiphonParams newParams = copy();
         newParams.foregroundTint = foregroundTint;
         return newParams;
      }

      public StarSiphonParams withFlareTint(Color flareTint) {
         StarSiphonParams newParams = copy();
         newParams.flareTint = flareTint;
         return newParams;
      }

      public StarSiphonParams withFlashTint(Color flashTint) {
         StarSiphonParams newParams = copy();
         newParams.flashTint = flashTint;
         return newParams;
      }

      public StarSiphonParams withParticleTint(Color particleTint) {
         StarSiphonParams newParams = copy();
         newParams.particleTint = particleTint;
         return newParams;
      }

      public List<String> getParticleSpriteNames() {
         return particleSpriteNames;
      }

      public SoundParams getInSound() {
         return inSound;
      }

      public SoundParams getLoopSound() {
         return loopSound;
      }

      public SoundParams getOutSound() {
         return outSound;
      }

      public String getBackgroundSpriteName() {
         return backgroundSpriteName;
      }

      public String getFlareSpriteName() {
         return flareSpriteName;
      }

      public String getForegroundSpriteName() {
         return foregroundSpriteName;
      }

      public String getMidgroundSpriteName() {
         return midgroundSpriteName;
      }

      public Color getBackgroundTint() {
         return backgroundTint;
      }

      public Color getFlareTint() {
         return flareTint;
      }

      public Color getForegroundTint() {
         return foregroundTint;
      }

      public Color getMidgroundTint() {
         return midgroundTint;
      }

      public String getFlashSpriteName() {
         return flashSpriteName;
      }

      public Color getFlashTint() {
         return flashTint;
      }

      public Color getParticleTint() {
         return particleTint;
      }

}
