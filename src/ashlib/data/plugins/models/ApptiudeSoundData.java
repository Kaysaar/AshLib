package ashlib.data.plugins.models;

public class ApptiudeSoundData {
    public String id;
    String sound1;
    String sound2;
    String sound3;
    String sound4;
    String sound5;
    public ApptiudeSoundData(String id,String sound1,String sound2,String sound3,String sound4,String sound5) {
        this.id = id;
        this.sound1 = sound1;
        this.sound2 = sound2;
        this.sound3 = sound3;
        this.sound4 = sound4;
        this.sound5 = sound5;
    }
    public String getSound(int sound){
        if(sound == 1){
            return sound1;
        }
        else if(sound == 2){
            return sound2;
        }
        else if(sound == 3){
            return sound3;
        }
        else if(sound == 4){
            return sound4;
        }
        else if(sound == 5){
            return sound5;
        }
        return sound1;
    }
}
