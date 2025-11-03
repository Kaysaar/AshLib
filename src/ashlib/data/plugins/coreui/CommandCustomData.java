package ashlib.data.plugins.coreui;

public class CommandCustomData {
    String commandId;
    String subTabId;

    public CommandCustomData(String commandId, String subTabId) {
        this.commandId = commandId;
        this.subTabId = subTabId;
    }

    public String getCommandId() {
        return commandId;
    }

    public String getSubTabId() {
        return subTabId;
    }
}
