package ashlib.data.plugins.handlers;

import ashlib.data.plugins.models.ApptiudeSoundData;
import com.fs.starfarer.api.Global;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

public class AICoreSkillPollHandler {
    private static AICoreSkillPollHandler instance;
    public HashMap<String, LinkedHashSet<String>> skillPoll;
    public HashMap<String,ApptiudeSoundData>skillButtonSoundData;
    public static AICoreSkillPollHandler getInstance() {
        if (instance == null) {
            setInstance();
        }
        return instance;
    }

    public static void setInstance() {
        instance = new AICoreSkillPollHandler();
        instance.skillPoll = new HashMap<>();
        instance.skillButtonSoundData = new HashMap<>();

        instance.populateSkillPoll();
        instance.populateSkillSounds();

    }
    public void populateSkillSounds(){
        try {
            JSONArray array =  Global.getSettings().getMergedSpreadsheetData("id","data/characters/skills/aptitude_data.csv");
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);
                String id = object.getString("id");
                String sound1 = object.getString("sound1");
                String sound2 = object.getString("sound2");
                String sound3 = object.getString("sound3");
                String sound4 = object.getString("sound4");
                String sound5 = object.getString("sound5");
                ApptiudeSoundData data = new ApptiudeSoundData(id,sound1,sound2,sound3,sound4,sound5);
                skillButtonSoundData.put(id,data);
            }
        }
        catch (Exception e ){
            throw new RuntimeException("aptitude_data.csv could not be found in data/characters/skills");
        }

    }
    public void populateSkillPoll()  {
        try {
            JSONArray array =  Global.getSettings().getMergedSpreadsheetData("id","data/campaign/ai_core_skill_poll.csv");

            try {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj  = array.getJSONObject(i);
                    String id = obj.getString("id");
                    LinkedHashSet<String> skills = getSetOfSkillsFromRawMap(obj.getString("skillList"));
                    if(skillPoll.get(id) != null) {
                        skillPoll.get(id).addAll(skills);
                    }
                    else {
                        skillPoll.put(id, skills);
                    }
                }
            }
            catch (Exception e ){
                //Ignore this as basically someone fucked up Csv
            }

        }
        catch (Exception e ){

        }

    }
    public LinkedHashSet<String> getSetOfSkillsFromRawMap(String  rawMap){
        LinkedHashSet<String> set = new LinkedHashSet<>();
        String[]seperated = rawMap.split(",");
        for (String s : seperated) {
         set.add(s.trim());
        }
        return set;
    }
    public String getSoundBasedOnAptitudeAndTier(String aptitude,int tier){
        ApptiudeSoundData data = skillButtonSoundData.get(aptitude);
        if(data==null){
            return "combat1";
        }
        return data.getSound(tier);
    }

    public HashMap<String, LinkedHashSet<String>> getSkillPoll() {
        return skillPoll;
    }
    public LinkedHashSet<String> getSetOfSkills(String id) {
        return skillPoll.get(id);
    }
}
