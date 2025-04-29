package ashlib.data.plugins.ui.models.resizable;

import ashlib.data.plugins.ui.models.TrapezoidButtonDetector;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CustomUIPanelPlugin;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;
import org.lwjgl.util.vector.Vector2f;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

public class    ResizableComponent implements CustomUIPanelPlugin {
        public CustomPanelAPI componentPanel;
        public float originalWidth, originalHeight;
        public float scale = 1f;
        public Vector2f originalCoords;
        public Vector2f movingCords = new Vector2f(0, 0);
        public CustomPanelAPI absolutePanel;
        TrapezoidButtonDetector detector = new TrapezoidButtonDetector();

        private class ReflectionUtilis {
            // Code taken and modified from Grand Colonies
            private static final Class<?> fieldClass;
            private static final MethodHandles.Lookup lookup = MethodHandles.lookup();
            private static final Class<?> methodClass;
            private static final MethodHandle invokeMethodHandle;

            static {
                try {
                    fieldClass = Class.forName("java.lang.reflect.Field", false, Class.class.getClassLoader());

                    methodClass = Class.forName("java.lang.reflect.Method", false, Class.class.getClassLoader());
                    invokeMethodHandle = lookup.findVirtual(methodClass, "invoke", MethodType.methodType(Object.class, Object.class, Object[].class));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }


            public static Object invokeMethod(String methodName, Object instance, Object... arguments) {
                try {
                    Object method = instance.getClass().getMethod(methodName);
                    return invokeMethodHandle.invoke(method, instance, arguments);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }

            public static List<UIComponentAPI> getChildrenCopy(UIPanelAPI panel) {
                try {
                    return (List<UIComponentAPI>) invokeMethod("getChildrenCopy", panel);
                } catch (Throwable e) {
                    return null;
                }
            }


            public static List<UIComponentAPI> getChildren(UIPanelAPI panelAPI) {
                return getChildrenCopy(panelAPI);
            }
        }

        public void setOriginalCoords(Vector2f originalCoords) {
            this.originalCoords = originalCoords;
        }

        public void setCoords(CustomPanelAPI panel, float cordX, float cordY) {
            this.originalCoords = new Vector2f(cordX, cordY);
            this.originalHeight = componentPanel.getPosition().getHeight();
            this.originalWidth = componentPanel.getPosition().getWidth();
        }

        public void setAbsolutePanel(CustomPanelAPI panel) {
            this.absolutePanel = panel;
        }

        public CustomPanelAPI getComponentPanel() {
            return componentPanel;
        }

        public void addComponent(ResizableComponent resizableComponent, float x, float y) {
            resizableComponent.setCoords(componentPanel, x, y);
            componentPanel.addComponent(resizableComponent.componentPanel).setLocation(0, 0).inTL(x, y);
        }

        public void removeComponent(ResizableComponent resizableComponent) {
            resizableComponent.clearUI();
            componentPanel.removeComponent(resizableComponent.getComponentPanel());
        }

        public boolean doesHover() {
            float x = Global.getSettings().getMouseX();
            float y = Global.getSettings().getMouseY();

            float xLeft = componentPanel.getPosition().getX();
            float xRight = componentPanel.getPosition().getX() + componentPanel.getPosition().getWidth();
            float yBot = componentPanel.getPosition().getY();
            float yTop = componentPanel.getPosition().getY() + componentPanel.getPosition().getHeight();
            if (absolutePanel != null) {
                boolean hoversOverB = detector.determineIfHoversOverButton(xLeft, yTop, xRight, yTop, xLeft, yBot, xRight, yBot, x, y);
                float xLeftA = absolutePanel.getPosition().getX();
                float xRightA = absolutePanel.getPosition().getX() + absolutePanel.getPosition().getWidth();
                float yBotA = absolutePanel.getPosition().getY();
                float yTopA = absolutePanel.getPosition().getY() + absolutePanel.getPosition().getHeight();
                return detector.determineIfHoversOverButton(xLeftA, yTopA, xRightA, yTopA, xLeftA, yBotA, xRightA, yBotA, x, y) && hoversOverB;
            }
            return detector.determineIfHoversOverButton(xLeft, yTop, xRight, yTop, xLeft, yBot, xRight, yBot, x, y);
        }

        public void resize(float scale) {
            this.scale = scale;
            componentPanel.getPosition().setSize(Math.round(originalWidth * scale), Math.round(originalHeight * scale));
            componentPanel.getPosition().setLocation(0, 0).inTL(Math.round(originalCoords.x * scale - (movingCords.x)), Math.round(originalCoords.y * scale + (movingCords.y)));
            for (UIComponentAPI componentAPI : ReflectionUtilis.getChildrenCopy(componentPanel)) {
                if (componentAPI instanceof CustomPanelAPI) {
                    CustomUIPanelPlugin plugin = ((CustomPanelAPI) componentAPI).getPlugin();
                    if (plugin instanceof ResizableComponent) {
                        ((ResizableComponent) plugin).resize(scale);
                    }
                }

            }

        }

        public boolean canInteract() {
            return doesHover();
        }

        public void resizeComponent(float scale) {
            //Here you can resize entire component depending on what you have
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

        }

        public void clearUI() {
            for (UIComponentAPI componentAPI : ReflectionUtilis.getChildrenCopy(componentPanel)) {
                if (componentAPI instanceof CustomPanelAPI) {
                    Object plugin = ((CustomPanelAPI) componentAPI).getPlugin();
                    if (plugin instanceof ResizableComponent) {
                        ((ResizableComponent) plugin).clearUI();
                    }
                }

            }
        }

        @Override
        public void processInput(List<InputEventAPI> events) {

        }

        @Override
        public void buttonPressed(Object buttonId) {

        }
    }

