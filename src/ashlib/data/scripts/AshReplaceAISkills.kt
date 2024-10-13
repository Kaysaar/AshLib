package ashlib.data.scripts;

import ashlib.data.plugins.reflection.ReflectionUtilis;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.coreui.CaptainPickerDialog;
import ashlib.data.plugins.handlers.AICoreSkillPollHandler;
import ashlib.data.plugins.misc.AshMisc;
import ashlib.data.plugins.ui.plugins.DarkHighlightPlugin;


import java.awt.*;
import java.util.*;

public class AshReplaceAISkills implements EveryFrameScript {
    //Inspired with Officer Extension
    boolean inserted = false;
    PersonAPI currentlyAffectedPerson = null;
    UIPanelAPI innerPanel = null;
    DarkHighlightPlugin plugin = null;
    ButtonAPI confirmButton = null;
    boolean shouldInsert = false;
    HashMap<String, Float> mapSkillRaw = null;
    HashMap<String, Float> originalMap = null;
    LabelAPI labelComponent = null;
    int currentSkillCount = 0;



    static float w = 70;
    static float h = 70;

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }

    LinkedHashMap<String, ButtonAPI> skillmap = new LinkedHashMap<>();

    @Override
    public void advance(float amount) {
        if (Global.getSector().getCampaignUI().getCurrentCoreTab() != null) {
            Object dialog = findSkillDialog();
            if (AshMisc.getCoreUI() == null) {
                return;
            }
            Object dialog2 = findCaptainDialog(AshMisc.getCoreUI());
            if (dialog == null && dialog2 != null) {
                inserted = false;
                Object officerListData = ReflectionUtilis.invokeMethod("getListOfficers", dialog2);
                ArrayList<Object> offcerRawData = (ArrayList<Object>) ReflectionUtilis.invokeMethod("getItems", officerListData);
                if (currentlyAffectedPerson != null) {
                    for (Object offcerRawDatum : offcerRawData) {
                        PersonAPI person = (PersonAPI) ReflectionUtilis.invokeMethod("getPerson", offcerRawDatum);
                        if (person.getId().equals(currentlyAffectedPerson.getId())) {
                            if (mapSkillRaw != null) {
                                if(shouldInsert){
                                    person.getStats().setSkipRefresh(true);
                                    for (MutableCharacterStatsAPI.SkillLevelAPI skillLevelAPI : person.getStats().getSkillsCopy()) {
                                        person.getStats().setSkillLevel(skillLevelAPI.getSkill().getId(), originalMap.get(skillLevelAPI.getSkill().getId()));
                                    }
                                    for (Map.Entry<String, Float> entry : mapSkillRaw.entrySet()) {
                                        person.getStats().setSkillLevel(entry.getKey(), entry.getValue());
                                    }

                                    person.getStats().setSkipRefresh(false);
                                }

                            }


                            ReflectionUtilis.invokeMethod("recreate", offcerRawDatum);
                            clearData();
                            break;
                        }
                    }
                }

                clearData();
                return;
            }
            if (dialog == null) return;
            if (ReflectionUtilis.findArrayOfSkills(dialog) != null) {
                PersonAPI person = (PersonAPI) ReflectionUtilis.findArrayOfSkills(dialog);
                if (person.getAICoreId() != null && AICoreSkillPollHandler.getInstance().getSetOfSkills(person.getAICoreId())!=null) {

                    if (currentlyAffectedPerson == null) {
                        currentlyAffectedPerson = person;
                    }
                    if (!inserted) {
                        for (MutableCharacterStatsAPI.SkillLevelAPI skillLevelAPI : person.getStats().getSkillsCopy()) {
                            if (mapSkillRaw == null) {
                                mapSkillRaw = new HashMap<>();
                                originalMap = new HashMap<>();
                            }
                            if(getSkills().contains(skillLevelAPI.getSkill().getId())){
                                mapSkillRaw.put(skillLevelAPI.getSkill().getId(), skillLevelAPI.getLevel());
                            }
                            originalMap.put(skillLevelAPI.getSkill().getId(), skillLevelAPI.getLevel());

                        }
                        innerPanel = (UIPanelAPI) ReflectionUtilis.invokeMethod("getInnerPanel", dialog);
                        if (innerPanel == null)
                            return;
                        for (UIComponentAPI componentAPI : ReflectionUtilis.getChildrenCopy(innerPanel)) {
                            if (componentAPI instanceof LabelAPI) {
                              if(!((LabelAPI) componentAPI).getText().contains("Select")){
                                  LabelAPI label = (LabelAPI) ReflectionUtilis.invokeMethodWithAutoProjection("createSkillPointsLabel",dialog,currentlyAffectedPerson.getStats().getLevel());
                                  innerPanel.removeComponent(componentAPI);
                                  labelComponent = label;
                                  label.setColor(Misc.getGrayColor());
                                  label.setHighlightColor(Color.ORANGE);
                                  innerPanel.addComponent((UIComponentAPI) labelComponent).inTL(10,innerPanel.getPosition().getHeight()-24);
                                  updateLabel(currentlyAffectedPerson);


                              }
                              continue;
                            }
                            if (componentAPI instanceof ButtonAPI) {
                                if (((ButtonAPI) componentAPI).getText() == null) {
                                    w = componentAPI.getPosition().getWidth();
                                    h = componentAPI.getPosition().getHeight();

                                    innerPanel.removeComponent(componentAPI);
                                }
                                else{
                                    if(((ButtonAPI) componentAPI).getText().contains("Confirm")){
                                        confirmButton = (ButtonAPI) componentAPI;
                                    }
                                }
                            } else {
                                innerPanel.removeComponent(componentAPI);
                            }
                        }
                        for (String skill : getSkills()) {
                            if(mapSkillRaw.get(skill)==null){
                                mapSkillRaw.put(skill,0f);
                                originalMap.put(skill, 0f);
                            }
                        }
                        for (String skill : getSkills()) {
                            populateSkillMap(skill, person);


                        }
                        float x = 10;
                        float y = 40;
                        int i = 0;
                        for (Map.Entry<String, ButtonAPI> componentAPI : skillmap.entrySet()) {
                            x = placeButton(componentAPI, w, h, person, x, y);
                            i++;
                            if(i>=7){
                                i = 0;
                                y+=82;
                                x=10;
                            }
                        }
                        plugin = new DarkHighlightPlugin(skillmap, 0.6f, mapSkillRaw);
                        CustomPanelAPI panel = Global.getSettings().createCustom(w, h, plugin);
                        plugin.setPanelTied(panel);
                        innerPanel.addComponent(panel).inTL(x, y);
                        inserted = true;
                        if(mapSkillRaw!=null){
                            updateLabel(currentlyAffectedPerson);
                        }

                    }
                    handleEnabledButtons();
                    handleButtons();
                } else {
                    clearData();
                }

            } else {
                clearData();
            }


        }
    }

    private void updateLabel(PersonAPI person) {
        currentSkillCount = getCurrentSkillCount();
        String currentSkill = ""+currentSkillCount+" / "+ person.getStats().getLevel();
        labelComponent.setText("Skills: "+currentSkill);
        labelComponent.setHighlight(currentSkill);

    }
    public LinkedHashSet<String> getSkills() {
        return (AICoreSkillPollHandler.getInstance().getSetOfSkills(currentlyAffectedPerson.getAICoreId()));
    }
    private float placeButton(Map.Entry<String, ButtonAPI> componentAPI, float w, float h, PersonAPI person, float x, float y) {
        componentAPI.getValue().getPosition().setSize(w, h);
        innerPanel.addComponent(componentAPI.getValue()).inTL(x, y);
        innerPanel.bringComponentToTop(componentAPI.getValue());

        x += w + 10;
        return x;
    }

    private void populateSkillMap(String skill, PersonAPI person) {
        CustomPanelAPI panelAPI = Global.getSettings().createCustom(100, 100, null);
        TooltipMakerAPI tooltip = panelAPI.createUIElement(500, 70, false);
        PersonAPI dummy = Global.getFactory().createPerson();
        float level = mapSkillRaw.get(skill);
        boolean demoting = true;

        if (level == 0) {
            level = 1;
            demoting = false;
        }
        dummy.getStats().setSkillLevel(skill, level);
        if (dummy.getStats().hasSkill(skill)) {
            UIComponentAPI component = tooltip.addSkillPanel(dummy, 0f);
            UIComponentAPI tooltipComp = (UIComponentAPI) ReflectionUtilis.getChildrenCopy((UIPanelAPI) component).get(0);
            UIComponentAPI rootTooltipComp = (UIComponentAPI) ReflectionUtilis.getChildrenCopy((UIPanelAPI) tooltipComp).get(0);
            ArrayList<UIComponentAPI> exactSkillButtonsOfCharacter = new ArrayList<>();
            for (UIComponentAPI deepRoot : ReflectionUtilis.getChildrenCopy((UIPanelAPI) rootTooltipComp)) {
                for (UIComponentAPI componentAPI : ReflectionUtilis.getChildrenCopy((UIPanelAPI) deepRoot)) {
                    if (demoting) {

                        ((ButtonAPI) ReflectionUtilis.getChildrenCopy((UIPanelAPI) componentAPI).get(0)).setButtonPressedSound("ui_char_decrease_skill");

                    } else {
                        SkillSpecAPI spec = Global.getSettings().getSkillSpec(skill);

                        ((ButtonAPI) ReflectionUtilis.getChildrenCopy((UIPanelAPI) componentAPI).get(0)).setButtonPressedSound(AICoreSkillPollHandler.getInstance().getSoundBasedOnAptitudeAndTier(spec.getGoverningAptitudeId(),spec.getTier()));
                    }
                    ((ButtonAPI) ReflectionUtilis.getChildrenCopy((UIPanelAPI) componentAPI).get(0)).setButtonDisabledPressedSound("ui_char_can_not_increase_skill_or_aptitude");
                    skillmap.put(skill, (ButtonAPI) ReflectionUtilis.getChildrenCopy((UIPanelAPI) componentAPI).get(0));
                }
            }
        }
    }

    public void clearData() {
        if(skillmap!=null){
            skillmap.clear();
        }

        if(mapSkillRaw!=null){
            mapSkillRaw.clear();
        }
        if(originalMap!=null){
            originalMap.clear();
        }


        currentlyAffectedPerson = null;
        innerPanel = null;
        plugin = null;
        shouldInsert = false;
        confirmButton = null;
        labelComponent = null;
        mapSkillRaw = null;
        originalMap = null;
        currentSkillCount = 0;
    }

    public Object findSkillDialog() {
        UIPanelAPI panelAPI = AshMisc.getCoreUI();
        if (panelAPI == null) return null;

        for (UIComponentAPI componentAPI : ReflectionUtilis.getChildrenCopy(panelAPI)) {
            if (ReflectionUtilis.hasMethodOfName("getNumSkills", componentAPI)) {
                return componentAPI;
            }
        }
        return null;

    }

    public void handleButtons() {
        if(confirmButton!=null){
            shouldInsert = confirmButton.isChecked();
        }
        for (Map.Entry<String, ButtonAPI> entry : skillmap.entrySet()) {
            if (entry.getValue().isChecked()) {
                if (mapSkillRaw == null) {
                    mapSkillRaw = new HashMap<>();
                }
                entry.getValue().setChecked(false);

                if (mapSkillRaw.get(entry.getKey()) == 0f) {
                    mapSkillRaw.put(entry.getKey(), 20f);
                } else {
                    mapSkillRaw.put(entry.getKey(), 0f);
                }
                float x = entry.getValue().getPosition().getX() - innerPanel.getPosition().getX();
                float y = entry.getValue().getPosition().getY() - innerPanel.getPosition().getY() - innerPanel.getPosition().getHeight();
                innerPanel.removeComponent(entry.getValue());
                populateSkillMap(entry.getKey(), currentlyAffectedPerson);
                placeButton(entry, w, h, currentlyAffectedPerson, x, y);
                y = entry.getValue().getPosition().getY() - innerPanel.getPosition().getY() - innerPanel.getPosition().getHeight();
                innerPanel.removeComponent(entry.getValue());
                placeButton(entry, w, h, currentlyAffectedPerson, x, y);
                innerPanel.bringComponentToTop(plugin.getPanelTied());
                currentSkillCount = getCurrentSkillCount();
                updateLabel(currentlyAffectedPerson);
                break;
            }
        }
    }
    public void handleEnabledButtons() {
        boolean enabled = getCurrentSkillCount()<currentlyAffectedPerson.getStats().getLevel();
        for (Map.Entry<String, ButtonAPI> entry : skillmap.entrySet()) {
            if (mapSkillRaw.get(entry.getKey()) == 0f) {
                if(entry.getValue().isEnabled()!=enabled){
                    entry.getValue().setEnabled(enabled);
                }

            }

        }
    }
    public CaptainPickerDialog findCaptainDialog(UIPanelAPI coreUI) {
        for (UIComponentAPI componentAPI : ReflectionUtilis.getChildrenCopy(coreUI)) {
            if (componentAPI instanceof CaptainPickerDialog) {
                return (CaptainPickerDialog) componentAPI;
            }
        }
        return null;
    }
    public  int getCurrentSkillCount(){
        int skills =0;
        for (Map.Entry<String, Float> entry : mapSkillRaw.entrySet()) {
            if(entry.getValue()>0){
                skills++;
            }
        }
        return skills;
    }

}
