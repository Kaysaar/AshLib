package ashlib.data.plugins.coreui.campaignbar.impl;

import ashlib.data.plugins.coreui.campaignbar.impl.CampaignHandleAccess.FieldRef;

import com.fs.starfarer.api.ui.PositionAPI;
import com.fs.starfarer.api.ui.UIComponentAPI;
import com.fs.starfarer.api.ui.UIPanelAPI;

import org.json.JSONArray;
import org.json.JSONObject;

/** Inspects frame metadata without loading textures or modifying UI/GL state. */
final class CampaignFrameDiagnostics {
    private CampaignFrameDiagnostics() {}

    static Object reference(Object value) {
        return value == null
                ? JSONObject.NULL
                : value.getClass().getName()
                        + "@"
                        + Integer.toHexString(System.identityHashCode(value));
    }

    static Object json(Object value) {
        return value == null ? JSONObject.NULL : value;
    }

    private static Object read(Object object, String getter, JSONArray errors) {
        try {
            return CampaignUiAccess.read(object, getter);
        } catch (Exception error) {
            errors.put(getter + ": " + CampaignDiagnostics.trace(error));
            return null;
        }
    }

    static JSONArray textureStrings(Object texture) throws Exception {
        JSONArray fields = new JSONArray();
        if (texture == null) {
            return fields;
        }
        for (FieldRef field : CampaignUiAccess.fields(texture.getClass())) {
            if (field.getType() == String.class) {
                if (fields.length() >= 16) {
                    fields.put(new JSONObject().put("unavailable", "TEXTURE_METADATA_LIMIT"));
                    break;
                }
                JSONObject item =
                        new JSONObject()
                                .put("field", field.getName())
                                .put("declaringType", field.getDeclaringClass().getName());
                try {
                    item.put("value", json(field.read(texture)));
                } catch (Exception error) {
                    item.put("unavailable", CampaignDiagnostics.trace(error));
                }
                fields.put(item);
            }
        }
        return fields;
    }

    static JSONObject sprite(Object component) throws Exception {
        JSONArray errors = new JSONArray();
        Object sprite = read(component, "getSprite", errors),
                texture = read(sprite, "getTexture", errors);
        JSONObject result =
                new JSONObject()
                        .put("componentReference", reference(component))
                        .put("asset", json(read(component, "getSpriteName", errors)))
                        .put("spriteReference", reference(sprite))
                        .put("textureReference", reference(texture))
                        .put("width", json(read(sprite, "getWidth", errors)))
                        .put("height", json(read(sprite, "getHeight", errors)))
                        .put("imageWidth", json(read(sprite, "getImageWidth", errors)))
                        .put("imageHeight", json(read(sprite, "getImageHeight", errors)))
                        .put("automaticResize", json(read(component, "isResize", errors)))
                        .put("errors", errors);
        try {
            result.put("textureStrings", textureStrings(texture));
        } catch (Exception error) {
            errors.put("textureStrings: " + CampaignDiagnostics.trace(error));
        }
        return result;
    }

    static JSONArray ownerReferences(UIPanelAPI bar, UIComponentAPI frame) throws Exception {
        JSONArray refs = new JSONArray();
        for (FieldRef field : CampaignUiAccess.fields(bar.getClass())) {
            if (field.getDeclaringClass() != bar.getClass()
                    || !UIComponentAPI.class.isAssignableFrom(field.getType())) {
                continue;
            }
            if (field.read(bar) == frame) {
                refs.put(
                        new JSONObject()
                                .put("field", field.getName())
                                .put("declaringType", field.getDeclaringClass().getName())
                                .put("declaredType", field.getType().getName()));
            }
        }
        return refs;
    }

    static JSONObject candidate(UIComponentAPI frame, UIPanelAPI bar) throws Exception {
        JSONArray errors = new JSONArray();
        PositionAPI position = frame.getPosition();
        JSONObject result =
                new JSONObject()
                        .put("componentClass", frame.getClass().getName())
                        .put("componentReference", reference(frame))
                        .put("barReference", reference(bar))
                        .put("positionReference", reference(position))
                        .put("barPositionReference", reference(bar.getPosition()))
                        .put("directChild", CampaignUiAccess.children(bar).contains(frame))
                        .put("siblingCount", CampaignUiAccess.children(bar).size())
                        .put("sprite", sprite(frame))
                        .put("errors", errors);
        if (position != null) {
            result.put(
                            "bounds",
                            new JSONObject()
                                    .put("x", position.getX())
                                    .put("y", position.getY())
                                    .put("width", position.getWidth())
                                    .put("height", position.getHeight()))
                    .put("parentPositionReference", reference(read(position, "getParent", errors)))
                    .put("anchorReference", reference(read(position, "getBase", errors)))
                    .put("xAlignOffset", json(read(position, "getXAlignOffset", errors)))
                    .put("yAlignOffset", json(read(position, "getYAlignOffset", errors)));
        }
        try {
            result.put("barFieldReferences", ownerReferences(bar, frame));
        } catch (Exception error) {
            errors.put("barFieldReferences: " + CampaignDiagnostics.trace(error));
        }
        return result;
    }
}
