package ashlib.data.plugins.ui.template;

import ashlib.data.plugins.ui.models.resizable.map.JumpPointRenderer;
import ashlib.data.plugins.ui.models.resizable.map.MapEntityComponent;
import ashlib.data.plugins.ui.models.resizable.map.MapMainComponent;
import ashlib.data.plugins.ui.models.resizable.map.MapTooltipComponent;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class SlipSpaceJumpCoordinateUI extends MapMainComponent {
    Vector2f currentCoordinates;
    public boolean needsToUpdateUI = false;
    Vector2f lastUICoordinates;
    ArrayList<RestrictedArea> areas;

    public SlipSpaceJumpCoordinateUI(float width, float height, StarSystemAPI systemAPI) {
        super(width, height, systemAPI);
        this.getPointer().setCanRender(true);
        this.getPointer().setShouldRender(true);

    }

    @Override
    public void onClick(Vector2f inUIWorldCoordinates, InputEventAPI event) {
        if (event.isLMBUpEvent()) {
            boolean detected = false;
            for (RestrictedArea area : areas) {
                float  radius = area.radius;
                Vector2f world = new Vector2f(area.originalCoords.x+area.radius, area.originalCoords.y+area.radius);
                Vector2f vector = getMapZoom().calculateMouseToWorldCords();
                if(Misc.getDistance(world,vector)<=radius){
                    Global.getSoundPlayer().playUISound("ui_button_disabled_pressed", 1, 1);
                    detected = true;
                    break;
                }

            }
            if (!detected) {
                Global.getSoundPlayer().playUISound("ui_button_pressed", 1, 1);
                lastUICoordinates = inUIWorldCoordinates;
                this.currentCoordinates = translateCoordinatesFromUIToWorld(inUIWorldCoordinates);
                needsToUpdateUI = true;
            }

        }

    }

    @Override
    protected void afterFinalize() {
        areas = new ArrayList<>();
        for (MapEntityComponent allEntitiesComponent : getMapZoom().getAllEntitiesComponents()) {
            if (allEntitiesComponent.getToken() instanceof PlanetAPI planet) {
                if (planet.isBlackHole() || planet.isStar()) {
                    Vector2f newLocation = translateCoordinatesToUI(planet.getLocation());
                    float radius = planet.getRadius() * 5;
                    RestrictedArea area = new RestrictedArea(radius);
                    areas.add(area);
                    getMapZoom().addComponent(area, newLocation.x-radius, newLocation.y-radius);
                }
            }
        }
    }

    @Override
    public void clearUI() {
        super.clearUI();
        areas.clear();
    }

    @Override
    public void processInput(List<InputEventAPI> events) {
        events.stream().filter(x -> !x.isConsumed() && getMapZoom().getPluginPanel().getPosition().containsEvent(x)).forEach(x -> {
            getMapZoom().getAllEntitiesComponents().forEach(y -> y.processInputForMapEntity(events, this));
        });
        events.stream().filter(x -> !x.isConsumed() && (x.isLMBUpEvent() || x.isRMBUpEvent()) && getMapZoom().getPluginPanel().getPosition().containsEvent(x)).forEach(x -> {
            onClick(getMapZoom().calculateMouseToWorldCords(), x);
            x.consume();
        });
    }

    public Vector2f getCurrentCoordinates() {
        return currentCoordinates;
    }
}
