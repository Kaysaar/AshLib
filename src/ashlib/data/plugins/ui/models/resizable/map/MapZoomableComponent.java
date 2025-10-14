package ashlib.data.plugins.ui.models.resizable.map;

import ashlib.data.plugins.ui.models.resizable.ResizableComponent;
import ashlib.data.plugins.ui.models.resizable.ZoomPanelComponent;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import java.util.ArrayList;

public class MapZoomableComponent extends ZoomPanelComponent {
    public MapZoomableComponent(float width, float height, float trueWidth, float trueHeight, float startingZoom) {
        super(width, height, trueWidth, trueHeight, startingZoom);
    }
    public ArrayList<MapEntityComponent>getAllEntitiesComponents(){
        ArrayList<MapEntityComponent>components = new ArrayList<>();
        for (ResizableComponent resizableComponent : this.resizableComponents) {
            if (resizableComponent instanceof MapEntityComponent) {
                components.add((MapEntityComponent) resizableComponent);
            }
        }
        return components;
    }
    public MapEntityComponent getComponentTiedToEntity(SectorEntityToken token){
        for (ResizableComponent resizableComponent : resizableComponents) {
            if(resizableComponent instanceof MapEntityComponent exact){
                if(exact.getToken().equals(token)){
                    return (MapEntityComponent) resizableComponent;
                }
            }
        }
        return null;
    }
    public ResizableComponent getResizableComponentOfClass(Class<?> classOfComponent){
        for (ResizableComponent resizableComponent : resizableComponents) {
            if(resizableComponent.getClass().equals(classOfComponent)){
                return resizableComponent;
            }
        }
        return null;
    }

}
