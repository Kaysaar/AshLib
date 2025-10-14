package ashlib.data.plugins.ui.models.resizable.map;

import ashlib.data.plugins.misc.AshMisc;
import ashlib.data.plugins.ui.models.ExtendedUIPanelPlugin;
import ashlib.data.plugins.ui.models.TrapezoidButtonDetector;
import ashlib.data.plugins.ui.models.resizable.ResizableComponent;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.CampaignObjective;
import com.fs.starfarer.api.impl.campaign.GenericFieldItemManager;
import com.fs.starfarer.api.impl.campaign.SupplyCacheEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class MapMainComponent implements ExtendedUIPanelPlugin {

    public static final int WORLD_X = 26000;
    // --- Panels / core ---
    CustomPanelAPI mainPanel;
    CustomPanelAPI contentPanel;
    MapZoomableComponent mapZoom;

    // --- Helpers/state ---
    public float sleep = 0f;
    MouseFollowerComponent follower;
    TrapezoidButtonDetector detector = new TrapezoidButtonDetector();
    MapPointerComponent pointer;

    public MapPointerComponent getPointer() {
        return pointer;
    }

    public MouseFollowerComponent getFollower() {
        return follower;
    }
    public void blockFullyPointer(){
        pointer.setCanRender(false);
    }
    public void unblockFullyPointer(){
        pointer.setCanRender(true);
    }
    MapCornerComponent leftTop, leftBottom, rightTop, rightBottom, center;

    private static final Logger log = Global.getLogger(MapMainComponent.class);

    // --- Accessors for corners (kept) ---
    public MapCornerComponent getLeftBottom() {
        return leftBottom;
    }

    public MapCornerComponent getLeftTop() {
        return leftTop;
    }

    public MapCornerComponent getRightBottom() {
        return rightBottom;
    }

    public MapCornerComponent getRightTop() {
        return rightTop;
    }
    StarSystemAPI system;
    // ========================================================================
    // Constructor
    // ========================================================================
    public MapMainComponent(float width, float height,StarSystemAPI system) {
        mainPanel = Global.getSettings().createCustom(width, height, this);

        mapZoom = new MapZoomableComponent(
                mainPanel.getPosition().getWidth(),
                mainPanel.getPosition().getHeight(),
                WORLD_X*2, WORLD_X*2,
                1f
        );
        mapZoom.startStencil();
        mapZoom.endStencil();
        mapZoom.setCurrScale(mapZoom.minScale * 2);
        this.system = system;

        buildUI(width, height);
    }

    // ========================================================================
    // Build steps (organized with minimal changes)
    // ========================================================================
    private void buildUI(float width, float height) {
        beforeBuild();


        addWorldAnchors();
        addGrid();

        if (system != null) {
            if (!Misc.getMinSystemSurveyLevel(system).equals(MarketAPI.SurveyLevel.NONE)) {
                addOrbits(system);
                addTerrains(system);
                addJumpPoints(system);
            }
            addPlanets(system);
            if (!Misc.getMinSystemSurveyLevel(system).equals(MarketAPI.SurveyLevel.NONE)) {
                addCustomEntities(system);
                addAsteroids(system);
            }
        }

        addPointerAndFollower();

        // glue panels
        contentPanel = Global.getSettings().createCustom(width, height, null);
        contentPanel.addComponent(mapZoom.getPluginPanel()).inTL(0, 0);
        mainPanel.addComponent(contentPanel).inTL(0, 0);

        // start centered on world
        mapZoom.centerOnWorld(WORLD_X, WORLD_X);

        afterBuild();
    }

    // --- Anchors + grid ---
    private void addWorldAnchors() {
        beforeAddCorners();

        leftTop = new MapCornerComponent();
        leftBottom = new MapCornerComponent();
        rightTop = new MapCornerComponent();
        rightBottom = new MapCornerComponent();
        center = new MapCornerComponent();

        mapZoom.addComponent(leftTop, 0, 0);
        mapZoom.addComponent(leftBottom, 0, 54000);
        mapZoom.addComponent(rightTop, 54000, 0);
        mapZoom.addComponent(rightBottom, 54000, 54000);
        mapZoom.addComponent(center, WORLD_X, WORLD_X);

        afterAddCorners();
    }

    private void addGrid() {
        beforeAddGrid();
        MapGridRenderer gridRender = new MapGridRenderer(leftTop, rightTop, leftBottom, rightBottom, center);
        mapZoom.addComponent(gridRender, 0, 0);
        afterAddGrid();
    }

    // --- Orbits / jump points / planets / terrain / entities / asteroids ---
    private void addOrbits(StarSystemAPI system) {
        beforeAddOrbits();

        for (SectorEntityToken token : system.getAllEntities()) {
            if (token instanceof PlanetAPI planet) {
                OrbitAPI orbit = planet.getOrbit();
                if (orbit != null) {
                    MapOrbitRenderer rendererOrb = new MapOrbitRenderer(
                            Misc.getDistance(planet.getLocation(), orbit.getFocus().getLocation())
                    );
                    Vector2f newLocation = translateCoordinatesToUI(orbit.getFocus().getLocation());
                    mapZoom.addComponent(rendererOrb, newLocation.x, newLocation.y);
                    rendererOrb.scale = mapZoom.currScale;
                }
            }
        }

        afterAddOrbits();
    }

    private void addJumpPoints(StarSystemAPI system) {
        beforeAddJumpPoints();

        for (SectorEntityToken jumpPoint : system.getJumpPoints()) {
            JumpPointRenderer renderer = new JumpPointRenderer((JumpPointAPI) jumpPoint);
            addTooltipTo(renderer, new MapTooltipComponent() {
                @Override
                public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                    tooltip.addPara(jumpPoint.getName(), 2f);
                }
            }, TooltipMakerAPI.TooltipLocation.RIGHT, false);
            Vector2f newLocation = translateCoordinatesToUI(jumpPoint.getLocation());
            mapZoom.addComponent(renderer,
                    newLocation.x - (JumpPointRenderer.size / 2),
                    newLocation.y - (JumpPointRenderer.size / 2));
        }

        afterAddJumpPoints();
    }

    private void addPlanets(StarSystemAPI system) {
        beforeAddPlanets();

        for (SectorEntityToken token : system.getAllEntities()) {
            if (!(token instanceof PlanetAPI planet)) continue;
            PlanetRenderResizableComponent comp = new PlanetRenderResizableComponent(planet, !Misc.getMinSystemSurveyLevel(system).equals(MarketAPI.SurveyLevel.NONE));
            if (!Misc.getMinSystemSurveyLevel(system).equals(MarketAPI.SurveyLevel.NONE) || planet.isBlackHole() || planet.isStar()) {
                addTooltipTo(comp, new MapTooltipComponent() {
                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                        if (token.getMarket() != null && token.getMarket().getFaction() != null && !token.getFaction().getId().equals(Factions.NEUTRAL)) {
                            boolean isMilitary = Misc.isMilitary(token.getMarket());
                            String name = token.getMarket().getName() + " - " + token.getMarket().getFaction().getDisplayName();
                            if (isMilitary) {
                                name += " (military)";
                            }
                            name += " (size " + token.getMarket().getSize() + ")";
                            tooltip.addPara(name, token.getMarket().getFaction().getBaseUIColor(), 2f);
                            tooltip.addPara(planet.getTypeNameWithWorld(), planet.getSpec().getIconColor(), 2f);

                        } else {
                            tooltip.addPara(planet.getName() + ", " + planet.getTypeNameWithWorldLowerCase(),
                                    planet.getSpec().getIconColor(), 2f);
                            if (!planet.isStar() && !planet.isBlackHole()) {
                                tooltip.addPara(AshMisc.getSurveyString(planet.getMarket().getSurveyLevel()), 2f);
                            }

                        }

                    }
                }, TooltipMakerAPI.TooltipLocation.RIGHT, false);

            } else {
                addTooltipTo(comp, new MapTooltipComponent() {
                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                        tooltip.addPara("Unknown...", 2f);
                    }
                }, TooltipMakerAPI.TooltipLocation.RIGHT, false);
            }

            Vector2f ui = translateCoordinatesToUI(planet.getLocation());
            mapZoom.addComponent(comp, ui.x - planet.getRadius(), ui.y - planet.getRadius());
            comp.scale = mapZoom.currScale;
        }

        afterAddPlanets();
    }

    private void addTerrains(StarSystemAPI system) {
        beforeAddTerrains();

        for (SectorEntityToken token : system.getAllEntities()) {
            if (token instanceof CampaignTerrainAPI terrain) {
                TerrainRenderV2 renderer = new TerrainRenderV2(terrain, mapZoom);
                // tiled terrain manages its own internal positioning; bind to world center
                mapZoom.addComponent(renderer, WORLD_X, WORLD_X);
            }
        }

        afterAddTerrains();
    }

    private void addCustomEntities(StarSystemAPI system) {
        beforeAddCustomEntities();
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if(fleet.isStationMode()&&!fleet.isHidden()&&fleet.isVisibleToPlayerFleet()){
                FleetMemberAPI member = fleet.getFleetData().getMembersListCopy()
                        .stream().filter(FleetMemberAPI::isStation).findFirst().orElse(null);
                if (member == null) continue;

                EntityRendererComponent comp = new EntityRendererComponent(
                        member.getVariant(),
                        fleet.getRadius(),
                        fleet.getFacing(),
                        fleet
                );
                addTooltipTo(comp, new MapTooltipComponent() {
                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                        tooltip.addPara(fleet.getNameWithFaction(),fleet.getFaction().getBaseUIColor(),2f);
                    }
                }, TooltipMakerAPI.TooltipLocation.RIGHT, false);
                Vector2f ui = translateCoordinatesToUI(fleet.getLocation());
                mapZoom.addComponent(comp, ui.x - fleet.getRadius(), ui.y - fleet.getRadius());
                comp.scale = mapZoom.currScale;
            }
        }
        for (CustomCampaignEntityAPI token : system.getCustomEntities()) {
            if (token.getCustomEntitySpec().getId().equals(Entities.WRECK)) continue;
            if (token.hasSensorProfile()) continue;
            // Stable location
            if((token.getCustomPlugin() instanceof SupplyCacheEntityPlugin)||(token.getCustomPlugin() instanceof CargoPodsRendererPlugin)){
                CargoPodsRendererPlugin plugin = new CargoPodsRendererPlugin(token);
                Vector2f ui = translateCoordinatesToUI(token.getLocation());
                addTooltipTo(plugin, new MapTooltipComponent() {
                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                        tooltip.addPara(token.getName(),Misc.getGrayColor(), 2f);
                    }
                }, TooltipMakerAPI.TooltipLocation.RIGHT, false);
                mapZoom.addComponent(plugin, ui.x - token.getRadius(), ui.y - token.getRadius());
                plugin.scale = mapZoom.currScale;
                continue;
            }

            if (token.getCustomEntitySpec().getId().equals(Entities.STABLE_LOCATION)) {
                StableLocationComponent comp = new StableLocationComponent(token);
                comp.scale = mapZoom.currScale;
                addTooltipTo(comp, new MapTooltipComponent() {
                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                        tooltip.addPara("Stable Location", 2f);
                    }
                }, TooltipMakerAPI.TooltipLocation.RIGHT, false);
                Vector2f ui = translateCoordinatesToUI(token.getLocation());
                mapZoom.addComponent(comp, ui.x - (JumpPointRenderer.size / 2), ui.y - (JumpPointRenderer.size / 2));
                continue;
            }

            // Stations (fleet visuals)
            if (token.getFleetForVisual() != null && token.hasTag(Tags.STATION)) {
                CampaignFleetAPI fleet = token.getFleetForVisual();
                FleetMemberAPI member = fleet.getFleetData().getMembersListCopy()
                        .stream().filter(FleetMemberAPI::isStation).findFirst().orElse(null);
                if (member == null) continue;

                EntityRendererComponent comp = new EntityRendererComponent(
                        member.getVariant(),
                        token.getRadius(),
                        fleet.getFacing(),
                        token
                );
                addTooltipTo(comp, new MapTooltipComponent() {
                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                        if (token.getCustomPlugin() != null && token.getCustomPlugin().hasCustomMapTooltip()) {
                            token.getCustomPlugin().createMapTooltip(tooltip, expanded);
                        } else {
                            if (token.getMarket() != null && token.getMarket().getFaction() != null) {
                                boolean isMilitary = Misc.isMilitary(token.getMarket());
                                String name = token.getMarket().getName() + " - " + token.getMarket().getFaction().getDisplayName();
                                if (isMilitary) {
                                    name += " (military)";
                                }
                                name += " (size " + token.getMarket().getSize() + ")";
                                tooltip.addPara(name, token.getMarket().getFaction().getBaseUIColor(), 2f);
                                if (token.hasTag(Tags.STATION)) {
                                    tooltip.addPara("Orbital Station", 2f);
                                }

                            }
                        }
                    }
                }, TooltipMakerAPI.TooltipLocation.RIGHT, false);

                Vector2f ui = translateCoordinatesToUI(token.getLocation());
                mapZoom.addComponent(comp, ui.x - token.getRadius(), ui.y - token.getRadius());
                comp.scale = mapZoom.currScale;
                continue;
            }

            // Other custom entities with sprite
            CustomEntitySpecAPI spec = token.getCustomEntitySpec();
            if (spec != null) {
                EntityRendererComponent comp = new EntityRendererComponent(
                        spec.getSpriteName(),
                        spec.getSpriteWidth(),
                        spec.getSpriteHeight(),
                        token.getRadius(),
                        token.getFacing(),
                        token
                );
                Vector2f ui = translateCoordinatesToUI(token.getLocation());
                addTooltipTo(comp, new MapTooltipComponent() {
                    @Override
                    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded, Object tooltipParam) {
                        if (token.getCustomPlugin() != null && token.getCustomPlugin().hasCustomMapTooltip()) {
                            token.getCustomPlugin().createMapTooltip(tooltip, expanded);
                        } else {
                            if (token.getMarket() != null && token.getMarket().getFaction() != null) {
                                boolean isMilitary = Misc.isMilitary(token.getMarket());
                                String name = token.getMarket().getName() + " - " + token.getMarket().getFaction().getDisplayName();
                                if (isMilitary) {
                                    name += " (military)";
                                }
                                name += " (size " + token.getMarket().getSize() + ")";
                                tooltip.addPara(name, token.getMarket().getFaction().getBaseUIColor(), 2f);
                                if (token.hasTag(Tags.STATION)) {
                                    tooltip.addPara("Orbital Station", 2f);
                                }

                            } else if (token.getFaction() != null && token.getCustomPlugin() instanceof CampaignObjective objective) {
                                String factionName = token.getFaction().getDisplayName();
                                String fullName = token.getName() + " - " + factionName;
                                if (token.getFaction().getId().equals(Factions.NEUTRAL)) {
                                    fullName = token.getName();
                                }
                                tooltip.addPara(fullName, token.getFaction().getBaseUIColor(), 2f);

                                TooltipMakerAPI info = tooltip.beginSubTooltip(getTooltipWidth(tooltipParam));
                                objective.printEffect(info, 0f);
                                tooltip.addCustom(info, 2f);
                                tooltip.endSubTooltip();
                                ;
                                tooltip.addPara(token.getCustomEntitySpec().getDefaultName(), Misc.getGrayColor(), 2f);
                                tooltip.addSpacer(10f);


                            }
                            else if (token.getFaction()!=null){
                                String  fullName = token.getName();
                                tooltip.addPara(fullName, token.getFaction().getBaseUIColor(), 2f);

                            }

                        }
                    }
                }, TooltipMakerAPI.TooltipLocation.RIGHT, false);
                mapZoom.addComponent(comp, ui.x - token.getRadius(), ui.y - token.getRadius());
                comp.scale = mapZoom.currScale;
            }
        }

        afterAddCustomEntities();

    }


    private void addAsteroids(StarSystemAPI system) {
        beforeAddAsteroids();

        ArrayList<AsteroidAPI> asteroids = new ArrayList<>();
        for (SectorEntityToken token : system.getAllEntities()) {
            if (token instanceof AsteroidAPI a) asteroids.add(a);
        }
        for (AsteroidAPI a : asteroids) {
            AsteroidRenderer r = new AsteroidRenderer(a);
            Vector2f ui = translateCoordinatesToUI(a.getLocation());
            mapZoom.addComponent(r, ui.x, ui.y);
        }

        afterAddAsteroids();
    }

    private void addPointerAndFollower() {
        beforeFinalize();

        follower = new MouseFollowerComponent();
        follower.scale = mapZoom.currScale;
        mapZoom.addComponent(follower, 0, 0);

        pointer = new MapPointerComponent(follower, this);
        pointer.scale = mapZoom.currScale;
        pointer.setShouldRender(false);
        mapZoom.addComponent(pointer, 0, 0);

        afterFinalize();
    }

    // ========================================================================
    // Hooks (override in subclasses to inject behavior before/after steps)
    // ========================================================================
    protected void beforeBuild() {
    }

    protected void afterBuild() {
    }

    protected void beforeAddCorners() {
    }

    protected void afterAddCorners() {
    }

    protected void beforeAddGrid() {
    }

    protected void afterAddGrid() {
    }

    protected void beforeAddOrbits() {
    }

    protected void afterAddOrbits() {
    }

    protected void beforeAddJumpPoints() {
    }

    protected void afterAddJumpPoints() {
    }

    protected void beforeAddPlanets() {
    }

    protected void afterAddPlanets() {
    }

    protected void beforeAddTerrains() {
    }

    protected void afterAddTerrains() {
    }

    protected void beforeAddCustomEntities() {
    }

    protected void afterAddCustomEntities() {
    }

    protected void beforeAddAsteroids() {
    }

    protected void afterAddAsteroids() {
    }

    protected void beforeFinalize() {
    }

    protected void afterFinalize() {
    }

    // ========================================================================
    // ExtendedUIPanelPlugin
    // ========================================================================
    @Override
    public CustomPanelAPI getMainPanel() {
        return mainPanel;
    }

    @Override
    public void createUI() {
    }

    @Override
    public void positionChanged(PositionAPI position) {
    }

    @Override
    public void renderBelow(float alphaMult) {
    }

    @Override
    public void render(float alphaMult) {
    }

    @Override
    public void advance(float amount) {
        if (doesHover()) {
            Vector2f vector = mapZoom.calculateMouseToWorldCords();
            Vector2f actualVector = new Vector2f(vector.x, Math.abs(vector.y));
            follower.updatePositionOfPanel(actualVector);

            if (sleep >= 3) {
                sleep = 0;
                log.info("Position in UI : " +
                        follower.getComponentPanel().getPosition().getCenterX() + "," +
                        follower.getComponentPanel().getPosition().getCenterY());
            }
        }
        sleep += amount;
    }

    public MapZoomableComponent getMapZoom() {
        return mapZoom;
    }

    public void onClick(Vector2f inUIWorldCoordinates, InputEventAPI event) {
        for (Object object : getMapZoom().getAllEntitiesComponents()) {
            if (object instanceof TerrainRenderV2 renderV2) {
                CampaignTerrainAPI terrain = (CampaignTerrainAPI) renderV2.getToken();
                if (terrain.getPlugin().containsPoint(translateCoordinatesFromUIToWorld(inUIWorldCoordinates), Global.getSector().getPlayerFleet().getRadius())) {
                    String hehe = "hehe";
                }

            }
        }

    }

    public void addTooltipTo(ResizableComponent addTo, TooltipMakerAPI.TooltipCreator creator, TooltipMakerAPI.TooltipLocation location, boolean recreateEveryFrame) {
        TooltipMakerAPI tooltip = mainPanel.createUIElement(1, 1, false);
        tooltip.addTooltipTo(creator, addTo.getTooltipOnHoverPanel(), location, recreateEveryFrame);
    }

    @Override
    public void processInput(List<InputEventAPI> events) {

        events.stream().filter(x -> !x.isConsumed() && mapZoom.getPluginPanel().getPosition().containsEvent(x)).forEach(x -> {
            getMapZoom().getAllEntitiesComponents().forEach(y->y.processInputForMapEntity(events,this));
        });
        events.stream().filter(x -> !x.isConsumed() && (x.isLMBUpEvent() || x.isRMBUpEvent()) && mapZoom.getPluginPanel().getPosition().containsEvent(x)).forEach(x -> {
            onClick(mapZoom.calculateMouseToWorldCords(), x);
            x.consume();
        });
        events.stream().filter(x -> !x.isConsumed() && x.isKeyUpEvent() && x.getEventValue() == Keyboard.KEY_T).findFirst().ifPresent(x -> {;
            pointer.setShouldRender(!pointer.shouldRender);
            x.consume();
        });
    }

    @Override
    public void buttonPressed(Object buttonId) {
    }

    // ========================================================================
    // Utils
    // ========================================================================

    /**
     * World (0,0 center) -> UI (0,0 top-left).
     */
    public Vector2f translateCoordinatesToUI(Vector2f worldLocation) {
        float uiX = worldLocation.x + WORLD_X;   // shift X so -27000 → 0, +27000 → 54000
        float uiY = WORLD_X - worldLocation.y;   // invert Y so +27000 → 0 (top), -27000 → 54000 (bottom)
        return new Vector2f(uiX, uiY);
    }

    public boolean doesHover() {
        float xLeft = mainPanel.getPosition().getX();
        float xRight = xLeft + mainPanel.getPosition().getWidth();
        float yBot = mainPanel.getPosition().getY();
        float yTop = yBot + mainPanel.getPosition().getHeight();

        return detector.determineIfHoversOverButton(
                xLeft, yTop, xRight, yTop, xLeft, yBot, xRight, yBot,
                Global.getSettings().getMouseX(),
                Global.getSettings().getMouseY()
        );
    }

    public Vector2f translateCoordinatesFromUIToWorld(Vector2f ui) {
        float worldX = ui.x - WORLD_X;   // 0..54000 UI -> -27000..+27000 world
        float worldY = WORLD_X - ui.y;   // invert Y back to world's +up, center-origin
        return new Vector2f(worldX, worldY);
    }

    @Override
    public void clearUI() {
        mapZoom.clearUI();
    }
}
